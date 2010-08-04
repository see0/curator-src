package edu.illinois.cs.cogcomp.annotation.handler;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import LBJ2.nlp.Word;
import LBJ2.nlp.seg.Token;
import edu.illinois.cs.cogcomp.lbj.chunk.Chunker;
import edu.illinois.cs.cogcomp.thrift.base.AnnotationFailedException;
import edu.illinois.cs.cogcomp.thrift.base.Labeling;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.thrift.curator.Record;
import edu.illinois.cs.cogcomp.thrift.labeler.Labeler;


/**
 * Wraps the Illinois Chunker (Shallow Parser) into a Labeler.Iface
 * @author James Clarke
 *
 */
public class IllinoisChunkerHandler implements Labeler.Iface {
	private final Logger logger = LoggerFactory.getLogger(IllinoisChunkerHandler.class);
	private Chunker tagger = new Chunker();
	private String posfield = "pos";
	private String tokensfield = "tokens";
	private String sentencesfield = "sentences";

	public IllinoisChunkerHandler() {
		this("configs/chunker.properties");
	}
	
	public IllinoisChunkerHandler(String configFilename) {
		if (configFilename.trim().equals("")) {
			configFilename = "configs/chunker.properties";
		}
		Properties config = new Properties();
		try {
            FileInputStream in = new FileInputStream(configFilename);
            config.load(new BufferedInputStream(in));
            in.close();
        } catch (IOException e) {
			logger.warn("Error reading configuration file. {}", e);
        }
		tokensfield = config.getProperty("tokens.field", "tokens");
		sentencesfield = config.getProperty("sentences.field", "sentences");
		posfield = config.getProperty("pos.field", "sentences");
	}
	
	public Labeling labelRecord(Record record) throws AnnotationFailedException,
			TException {
		if (!record.getLabelViews().containsKey(tokensfield) && !record.getLabelViews().containsKey(sentencesfield)) {
			throw new TException("Record must be tokenized and sentence split first");
		}
		if (!record.getLabelViews().containsKey(posfield)) {
			throw new TException("Record must be POS tagged.");
		}
		long startTime = System.currentTimeMillis();
		
		List<Span> tags = record.getLabelViews().get(posfield).getLabels();
		String rawText = record.getRawText();

		List<Token> lbjTokens = recordToLBJTokensPos(record);
		Labeling labeling = new Labeling();

		List<Span> labels = new ArrayList<Span>();
		
		Span label = null;
		String clabel = "";
		Span previous = null;
		int tcounter = 0;
		for (int i = 0; i < lbjTokens.size(); i++) {
			Token lbjtoken = lbjTokens.get(i);
			Span current = tags.get(tcounter);
			tagger.discreteValue(lbjtoken);
			logger.debug("{} {}", lbjtoken.toString(), lbjtoken.type);
			if (lbjtoken.type.charAt(0) == 'I') {
				if (!clabel.equals(lbjtoken.type.substring(2))) {
					logger.warn("Inside a chunk when I shouldn't be!");
					logger.warn(rawText);
				}
			} else if ((lbjtoken.type.charAt(0) == 'B' || lbjtoken.type.charAt(0) == 'O') && label !=null) {
				label.setEnd(previous.getEnd());
				labels.add(label);
				label = null;
			}
			if (lbjtoken.type.charAt(0) == 'B') {
				label = new Span();
				label.setStart(current.getStart());
				clabel = lbjtoken.type.substring(2);
				label.setLabel(clabel);
			}
			previous = current;
			tcounter++;
		}
		if (label != null) {
			label.setEnd(previous.getEnd());
			labels.add(label);
		}
		labeling.setLabels(labels);
    	labeling.setSource(getSourceIdentifier());

		long endTime = System.currentTimeMillis();
		logger.info("Tagged input in {}ms", endTime-startTime);
		return labeling;
	}

	public String getName() throws TException {
		return "Illinois Chunker";
	}

	public String getVersion() throws TException {
		return "0.3";
	}

	public boolean ping() throws TException {
		return true;
	}

	public String getSourceIdentifier() throws TException {
		return "illinoischunker-"+getVersion();
	}
	
	/**
	 * Converts a Record to LBJ Tokens with POS information.
	 * 
	 * @param record
	 * @return
	 */
	private List<Token> recordToLBJTokensPos(Record record) {
		List<Token> lbjTokens = new LinkedList<Token>();
		int j = 0;
		List<Span> tags = record.getLabelViews().get(posfield).getLabels();
		String rawText = record.getRawText();
		for (Span sentence : record.getLabelViews().get(sentencesfield).getLabels()) {
			Word wprevious = null;
			Token tprevious = null;
			boolean opendblquote = true;
			Span tag = null;
			do {
				tag = tags.get(j);
				Word wcurrent;
				String token = rawText.substring(tag.getStart(), tag.getEnd());
				if (token.equals("\"")) {
					token = opendblquote ? "``" : "''";
					opendblquote = !opendblquote;
				} else if (token.equals("(")) {
					token = "-LRB-";
				} else if (token.equals(")")) {
					token = "-RRB-";
				} else if (token.equals("{")) {
					token = "-LCB-";
				} else if (token.equals("}")) {
					token = "-RCB-";
				} else if (token.equals("[")) {
					token = "-LSB-";
				} else if (token.equals("]")) {
					token = "-RSB-";
				}
				wcurrent = new Word(token, wprevious);
				wcurrent.partOfSpeech = tag.getLabel();
				Token tcurrent = new Token(wcurrent, tprevious, "");
				lbjTokens.add(tcurrent);
				if (tprevious != null) {
					tprevious.next = tcurrent;
				}
				wprevious = wcurrent;
				tprevious = tcurrent;
				j++;
			} while (tag.getEnd() < sentence.getEnd());

		}
		return lbjTokens;
	}

}
