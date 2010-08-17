package edu.illinois.cs.cogcomp.annotation.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import LBJ2.classify.Classifier;
import LBJ2.parse.LinkedVector;
import edu.illinois.cs.cogcomp.NerTagger.LbjTagger.BracketFileManager;
import edu.illinois.cs.cogcomp.NerTagger.LbjTagger.NETester;
import edu.illinois.cs.cogcomp.NerTagger.LbjTagger.NEWord;
import edu.illinois.cs.cogcomp.NerTagger.LbjTagger.Parameters;
import edu.illinois.cs.cogcomp.NerTagger.lbj.FeaturesLevel1;
import edu.illinois.cs.cogcomp.NerTagger.lbj.FeaturesLevel2;
import edu.illinois.cs.cogcomp.NerTagger.lbj.NETaggerLevel1;
import edu.illinois.cs.cogcomp.NerTagger.lbj.NETaggerLevel2;
import edu.illinois.cs.cogcomp.thrift.base.Labeling;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.thrift.curator.Record;
import edu.illinois.cs.cogcomp.thrift.labeler.Labeler;


/**
 * @author James Clarke
 * 
 */
public class IllinoisNERHandler implements Labeler.Iface {
	private final Logger logger = LoggerFactory.getLogger(IllinoisNERHandler.class);
	private NETaggerLevel1 t1;
	private NETaggerLevel2 t2;

	public IllinoisNERHandler() {
		this("configs/ner.config");
	}
	
	public IllinoisNERHandler(String params) {
		Parameters.readConfigAndLoadExternalData(params);
		Parameters.forceNewSentenceOnLineBreaks = false;
		NETaggerLevel1 tagger1 = (NETaggerLevel1) Classifier.binaryRead(String
				.format("%s.level1", Parameters.pathToModelFile));
		NETaggerLevel2 tagger2 = (NETaggerLevel2) Classifier.binaryRead(String
				.format("%s.level2", Parameters.pathToModelFile));
		setTaggers(tagger1, tagger2);
	}

	public IllinoisNERHandler(NETaggerLevel1 tag1, NETaggerLevel2 tag2) {
		setTaggers(tag1, tag2);
	}

	public void setTaggers(NETaggerLevel1 tag1, NETaggerLevel2 tag2) {
		t1 = tag1;
		t2 = tag2;
	}

	/**
	 * Performs that call to the LBJ NER library.
	 * 
	 * @param input
	 * @param nl2sent
	 *            convert new lines to sentences?
	 * @return
	 */
	private synchronized List<LinkedVector> performNER(String input,
			boolean nl2sent) {
		long startTime = System.currentTimeMillis();
		logger.debug("Performing NER (nl2sent: {}) on:", nl2sent);
		logger.debug(input);
		Parameters.forceNewSentenceOnLineBreaks = nl2sent;
		if (input.trim().equals("")) {
			return new ArrayList<LinkedVector>();
		}
		Vector<LinkedVector> data = BracketFileManager.parseText(input);
		NETester.annotateBothLevels(data, t1, t2);
		boolean debug = false;
		if (debug) {
			System.out
					.println("---------------    Active features: --------------");
			for (Iterator<String> iter = Parameters.featuresToUse.keySet()
					.iterator(); iter.hasNext(); System.out
					.println(iter.next()))
				;
			System.out
					.println("-------- Sentence splitting details (each sentence is a new line)---------");
			for (int i = 0; i < data.size(); i++) {
				System.out.println("\t");
				for (int j = 0; j < data.elementAt(i).size(); j++)
					System.out.print(((NEWord) data.elementAt(i).get(j)).form
							+ " ");
				System.out.println("");
			}
			System.out
					.println("\n\n------------  Level1 features report  ----------------\n\n");
			FeaturesLevel1 feats1 = new FeaturesLevel1();
			for (int i = 0; i < data.size(); i++) {
				for (int j = 0; j < data.elementAt(i).size(); j++)
					System.out
							.println("\t"
									+ ((NEWord) data.elementAt(i).get(j)).form
									+ " "
									+ feats1.classify((NEWord) data
											.elementAt(i).get(j)));
			}
			if (t2 != null
					&& (Parameters.featuresToUse.containsKey("PatternFeatures") || Parameters.featuresToUse
							.containsKey("PredictionsLevel1"))) {
				System.out
						.println("\n\n---------  Level2 features report  -----------\n\n");
				FeaturesLevel2 feats2 = new FeaturesLevel2();
				for (int i = 0; i < data.size(); i++) {
					for (int j = 0; j < data.elementAt(i).size(); j++)
						System.out.println("\t"
								+ ((NEWord) data.elementAt(i).get(j)).form
								+ " "
								+ feats2.classify((NEWord) data.elementAt(i)
										.get(j)));
				}
			}
		}

		long endTime = System.currentTimeMillis();
		long time = endTime - startTime;
		logger.debug("Performed NER in {}ms", time);
		return data;
	}

	/**
	 * Converts BILOU to BIO format.
	 * 
	 * @param prediction
	 * @return
	 */
	public String bilou2bio(String prediction) {
		String replacement = null;
		if (Parameters.taggingScheme.equalsIgnoreCase(Parameters.BILOU)) {
			if (prediction.startsWith("U-"))
				replacement = "B";
			if (prediction.startsWith("L-"))
				replacement = "L";
			if (replacement != null)
				prediction = String.format("%s-%s", replacement, prediction
						.substring(2));
		}
		return prediction;
	}

	public Labeling performNer(String input, boolean nl2sent) throws TException {
		List<LinkedVector> data = performNER(input, nl2sent);
		List<Integer> quoteLocs = findQuotationLocations(input);
		List<Span> labels = new ArrayList<Span>();

		// track the location we have reached in the input
		int location = 0;

		// each LinkedVector in data corresponds to a sentence.
		for (int i = 0; i < data.size(); i++) {
			LinkedVector vector = data.get(i);
			boolean open = false;

			// the span for this entity
			Span span = null;

			// lets cache the predictions and words
			String[] predictions = new String[vector.size()];
			String[] words = new String[vector.size()];
			for (int j = 0; j < vector.size(); j++) {
				predictions[j] = bilou2bio(((NEWord) vector.get(j)).neTypeLevel2);
				words[j] = ((NEWord) vector.get(j)).form;
			}

			for (int j = 0; j < vector.size(); j++) {

				// the current word (NER's interpretation)
				String word = words[j];
				// this int[] will store start loc of word in 0th index and end
				// in 1st index.
				int[] startend = findStartEndForSpan(input, location, word,
						quoteLocs);

				if (predictions[j].startsWith("B-")
						|| (j > 0 && predictions[j].startsWith("I-") && (!predictions[j - 1]
								.endsWith(predictions[j].substring(2))))) {
					span = new Span();
					span.setStart(startend[0]);
					location = startend[1];
					span.setLabel(predictions[j].substring(2));
					open = true;
				}

				if (open) {
					boolean close = false;
					if (j == vector.size() - 1) {
						close = true;
					} else {
						if (predictions[j + 1].startsWith("B-"))
							close = true;
						if (predictions[j + 1].equals("O"))
							close = true;
						if (predictions[j + 1].indexOf('-') > -1
								&& (!predictions[j].endsWith(predictions[j + 1]
										.substring(2))))
							close = true;
					}
					if (close) {
						span.setEnding(startend[1]);
						location = startend[1];
						labels.add(span);
						open = false;
					}
				}
			}

		}
		Labeling labeling = new Labeling();
		labeling.setSource(getSourceIdentifier());
		labeling.setLabels(labels);
		return labeling;
	}

	/**
	 * Finds that start and end indices in the input of the span corresponding
	 * to the word. The location is a pointer to how far we have processed the
	 * input.
	 * 
	 * @param input
	 * @param location
	 * @param word
	 * @param quoteLocs
	 * @return
	 */
	private int[] findStartEndForSpan(String input, int location, String word,
			List<Integer> quoteLocs) {
		int[] startend = null;

		if (word.equals("\"")) {
			// double quote is a special case because it could have been a
			// double tick before
			// inputAsNer is how NER viewed the input (we replicate the
			// important transforms
			// ner makes, this is very fragile!)
			StringBuffer inputAsNer = new StringBuffer();
			inputAsNer.append(input.substring(0, location));
			// translate double ticks to double quote in the original input
			inputAsNer.append(input.substring(location).replace("``", "\"")
					.replace("''", "\""));
			// find start end for the word in the input as ner
			startend = findSpan(location, inputAsNer.toString(),
					word);
			if (quoteLocs.contains(startend[0])) {
				// if the double quote was original translated we should move
				// the end pointer one
				startend[1]++;
			}
		} else {
			startend = findSpan(location, input, word);
		}
		return startend;
	}

	/**
	 * Finds where double tick quotation marks are and returns their start
	 * locations in the string NER will use.
	 * 
	 * NER performs preprocessing internally on the input string to turn double
	 * ticks to the double quote character, we need to be able to recover the
	 * double tick locations in the original to make sure our spans are
	 * consistent.
	 * 
	 * If input is: He said, ``This is great'', but he's "hip". NER will modify
	 * it to: He said, "This is great", but he's "hip". so we need to know that
	 * locations <9, 23> are locations in the NER string that should be double
	 * ticks.
	 * 
	 * @param input
	 *            rawText input not modified by NER
	 * @return list of integer locations that double quote should be translated
	 *         to double tick
	 */
	private List<Integer> findQuotationLocations(String input) {
		List<Integer> quoteLocs = new ArrayList<Integer>();
		if (input.contains("``") || input.contains("''")) {
			int from = 0;
			int index;
			int counter = 0;
			while ((index = input.indexOf("``", from)) != -1) {
				quoteLocs.add(index - counter);
				counter++;
				from = index + 2;
			}
			while ((index = input.indexOf("''", from)) != -1) {
				quoteLocs.add(index - counter);
				counter++;
				from = index + 2;
			}
			Collections.sort(quoteLocs);
		}
		return quoteLocs;
	}

	public boolean ping() {
		logger.debug("PONG!");
		return true;
	}

	public Labeling labelRecord(Record record) throws TException {
		Labeling ner = performNer(record.getRawText(), false);
		return ner;
	}

	public String getName() throws TException {
		return "Illinois Named Entity Recognizer";
	}

	public String getVersion() throws TException {
		return "0.1";
	}

	public String getSourceIdentifier() throws TException {
		return "illinoisner-" + getVersion();
	}

	/**
	 * Finds the span (as start and end indices) where the word occurs in the
	 * rawText starting at from.
	 * 
	 * @param from
	 * @param rawText
	 * @param word
	 * @return
	 */
	private static int[] findSpan(int from, String rawText, String word) {
		int start = rawText.indexOf(word, from);
		int end = start + word.length();
		return new int[] { start, end };
	}
}
