package edu.illinois.cs.cogcomp.curator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.illinois.cs.cogcomp.thrift.base.Labeling;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.util.StringUtil;

/**
* A simple whitespace tokenizer
* 
* @author James Clarke
* 
*/

public class Whitespacer {

	public static Labeling tokenize(List<String> sentences) {
		Labeling labeling = new Labeling();
		List<Span> tokens = new ArrayList<Span>();
		int offset = 0;
		for (String sentence : sentences) {
			Matcher m = Pattern.compile("\\S+").matcher(sentence);
			while (m.find()) {
				int start = m.start() + offset;
				int end = m.end() + offset;
				Span token = new Span(start, end);
				tokens.add(token);
			}
			offset += sentence.length() + 1;
		}
		labeling.setLabels(tokens);
		labeling.setSource(getSourceIdentifier());
		return labeling;
	}
	
	public static String getSourceIdentifier() {
		return "whitespacetokenizer-0.1";
	}
	
	public static Labeling sentences(List<String> sentences) {
		Labeling labeling = new Labeling();
		List<Span> sents = new ArrayList<Span>();
		int offset = 0;
		for (String sentence : sentences) {
			int start = offset;
			int end = sentence.length() + offset;
			Span sent = new Span(start, end);
			sents.add(sent);
			offset += sentence.length() + 1;
		}
		labeling.setLabels(sents);
		labeling.setSource(getSourceIdentifier());
		return labeling;
	}
	
	public static void main(String args[]) {
		List<String> sentences = new ArrayList<String>();
		sentences.add("This is a tokenized sentence .");
		sentences.add("Here is another !");
		sentences.add("And one more .");
		String rawText = StringUtil.join(sentences, " ");
		System.out.println(rawText);
		Labeling s = sentences(sentences);
		Labeling t = tokenize(sentences);
		for (Span span : t.getLabels()) {
			System.out.println(rawText.substring(span.getStart(), span.getEnding()));
		}
		for (Span span : s.getLabels()) {
			System.out.println(rawText.substring(span.getStart(), span.getEnding()));
		}
	}
}
