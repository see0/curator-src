package edu.illinois.cs.cogcomp.annotation.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;

import edu.illinois.cs.cogcomp.thrift.base.AnnotationFailedException;
import edu.illinois.cs.cogcomp.thrift.base.Forest;
import edu.illinois.cs.cogcomp.thrift.base.Labeling;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.thrift.curator.Record;
import edu.illinois.cs.cogcomp.thrift.parser.MultiParser;

public class StanfordParserHandlerTest {

	private MultiParser.Iface handler;
	@Before
	public void setUp() throws Exception {
		handler = new StanfordParserHandler();
	}

	private List<Span> buildSpans(int[] offsets) {
		List<Span> spans = new ArrayList<Span>();
		int index = 0;
		while (index < offsets.length) {
			Span span = new Span(offsets[index], offsets[index+1]);
			spans.add(span);
			index = index + 2;
		}
		return spans;
	}
	
	@Test
	public void testParseRecord() throws AnnotationFailedException, TException {
		String text = "My dog also likes eating bananas.";
		
		int[] sentenceOffsets = {0, 33};
		int[] tokenOffsets = {0,2,3,6,7,11,12,17,18,24,25,32,32,33};
		List<Span> sentences = buildSpans(sentenceOffsets);
		Labeling slabeling = new Labeling(sentences);
		List<Span> tokens = buildSpans(tokenOffsets);
		Labeling tlabeling = new Labeling(tokens);
		
		Record r = new Record();
		r.setRawText(text);
		r.setLabelViews(new HashMap<String, Labeling>());
		r.getLabelViews().put("sentences", slabeling);
		r.getLabelViews().put("tokens", tlabeling);
		
		List<Forest> forests = handler.parseRecord(r);
		//TODO: Make this into a assert and hope it all works
		System.out.println(forests);
	}
	
	@Test
	public void testRegression1() throws AnnotationFailedException, TException {
		String text = "The investigation focused on the hangman's cell, where 141 French prisoners-of-war were said to have been executed, as well at the 18th-century Master Ropemaker's House.";
		int[] sentenceOffsets = {0,169};
		int[] tokenOffsets = {0,3,4,17,18,25,26,28,29,32,33,40,40,42,43,47,47,48,49,54,55,58,59,65,66,82,83,87,88,92,93,95,96,100,101,105,106,114,114,115,116,118,119,123,124,126,127,130,131,143,144,150,151,160,160,162,163,168,168,169};
		List<Span> sentences = buildSpans(sentenceOffsets);
		Labeling slabeling = new Labeling(sentences);
		List<Span> tokens = buildSpans(tokenOffsets);
		Labeling tlabeling = new Labeling(tokens);
		
		Record r = new Record();
		r.setRawText(text);
		r.setLabelViews(new HashMap<String, Labeling>());
		r.getLabelViews().put("sentences", slabeling);
		r.getLabelViews().put("tokens", tlabeling);
		
		List<Forest> forests = handler.parseRecord(r);
		//TODO: Make this into a assert and hope it all works
		System.out.println(forests);
	}
	
	@Test
	public void testRegression2() throws AnnotationFailedException, TException {
		String text = "-LRB- In a stock-index arbitrage sell program , traders buy or sell big baskets of stocks and offset the trade in futures to lock in a price difference . -RRB-";
		int[] sentenceOffsets = {0,159};
		int[] tokenOffsets = {0,1,1,4,4,5,6,8,9,10,11,22,23,32,33,37,38,45,46,47,48,55,56,59,60,62,63,67,68,71,72,79,80,82,83,89,90,93,94,100,101,104,105,110,111,113,114,121,122,124,125,129,130,132,133,134,135,140,141,151,152,153,154,155,155,158,158,159};
		List<Span> sentences = buildSpans(sentenceOffsets);
		Labeling slabeling = new Labeling(sentences);
		List<Span> tokens = buildSpans(tokenOffsets);
		Labeling tlabeling = new Labeling(tokens);
		
		Record r = new Record();
		r.setRawText(text);
		r.setLabelViews(new HashMap<String, Labeling>());
		r.getLabelViews().put("sentences", slabeling);
		r.getLabelViews().put("tokens", tlabeling);
		
		List<Forest> forests = handler.parseRecord(r);
		//TODO: Make this into a assert and hope it all works
		System.out.println(forests);
	}

}
