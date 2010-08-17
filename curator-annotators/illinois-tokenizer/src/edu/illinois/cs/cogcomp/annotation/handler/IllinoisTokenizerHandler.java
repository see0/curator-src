package edu.illinois.cs.cogcomp.annotation.handler;

import java.util.ArrayList;
import java.util.List;

import org.apache.thrift.TException;

import LBJ2.nlp.Sentence;
import LBJ2.nlp.SentenceSplitter;
import LBJ2.nlp.Word;
import LBJ2.parse.LinkedVector;
import edu.illinois.cs.cogcomp.thrift.curator.Record;
import edu.illinois.cs.cogcomp.thrift.labeler.MultiLabeler;
import edu.illinois.cs.cogcomp.thrift.base.AnnotationFailedException;
import edu.illinois.cs.cogcomp.thrift.base.Labeling;
import edu.illinois.cs.cogcomp.thrift.base.Span;

/**
 * Thrift Service for Illinois Tokenizer (built into Learning Based Java).
 * Implements the MultiLabeler service where first Labeling are the sentence
 * boundaries and the second Labeling the token boundaries.
 * 
 * @author James Clarke
 * 
 */
public class IllinoisTokenizerHandler implements MultiLabeler.Iface {

	public List<Labeling> labelRecord(Record record)
			throws AnnotationFailedException, TException {
		SentenceSplitter splitter = new SentenceSplitter(record.getRawText()
				.split("\n"));

		List<Span> tokens = new ArrayList<Span>();
		List<Span> sentences = new ArrayList<Span>();
		for (Sentence s : splitter.splitAll()) {
			LinkedVector words = s.wordSplit();
			for (int i = 0; i < words.size(); i++) {
				Word word = (Word) words.get(i);
				Span token = new Span();
				token.setStart(word.start);
				token.setEnding(word.end + 1);
				tokens.add(token);
			}
			Span span = new Span();
			span.setStart(s.start);
			span.setEnding(s.end + 1);
			sentences.add(span);
		}

		List<Labeling> result = new ArrayList<Labeling>();
		Labeling tokenLabeling = new Labeling();
		tokenLabeling.setLabels(tokens);
		tokenLabeling.setSource(getSourceIdentifier());
		Labeling sentenceLabeling = new Labeling();
		sentenceLabeling.setLabels(sentences);
		sentenceLabeling.setSource(getSourceIdentifier());
		result.add(sentenceLabeling);
		result.add(tokenLabeling);
		return result;
	}

	public boolean ping() throws TException {
		return true;
	}

	public String getName() throws TException {
		return "Illinois Tokenizer";
	}

	public String getVersion() throws TException {
		return "0.2";
	}

	public String getSourceIdentifier() throws TException {
		return "illinoistokenizer-" + getVersion();
	}

}
