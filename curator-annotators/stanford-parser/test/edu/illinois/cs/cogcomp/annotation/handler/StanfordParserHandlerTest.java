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

}
