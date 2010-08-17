package edu.illinois.cs.cogcomp.annotation.handler;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;

import edu.illinois.cs.cogcomp.thrift.base.AnnotationFailedException;
import edu.illinois.cs.cogcomp.thrift.base.Labeling;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.thrift.curator.Record;
import edu.illinois.cs.cogcomp.thrift.labeler.Labeler;

public class IllinoisPOSHandlerTest {
	private Labeler.Iface handler;
	
	@Before
	public void setUp() throws Exception {
		handler = new IllinoisPOSHandler();
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
	public void testSimpleSentence() throws AnnotationFailedException, TException {
		String text = "Bob saw Jim leave the car in the parking lot.";
		Record r = new Record();
		r.setRawText(text);
		r.setWhitespaced(false);
		int[] sentenceOffsets = {0, 45};
		List<Span> sentences = buildSpans(sentenceOffsets);
		Labeling slabeling = new Labeling(sentences);
		int[] tokenOffsets = { 0, 3, 4, 7, 8, 11, 12, 17, 18, 21, 22, 25, 26,
				28, 29, 32, 33, 40, 41, 44, 44, 45};
		List<Span> tokens = buildSpans(tokenOffsets);
		Labeling tlabeling = new Labeling(tokens);
		
		r.setLabelViews(new HashMap<String, Labeling>());
		r.getLabelViews().put("sentences", slabeling);
		r.getLabelViews().put("tokens", tlabeling);
		
		Labeling labeling = handler.labelRecord(r);
		String[] posTags = {"NNP", "VBD", "NNP", "VBP", "DT", "NN", "IN", "DT", "NN", "NN", "."};
		int index = 0;
		for (Span tag : labeling.getLabels()) {
			System.out.println(text.subSequence(tag.getStart(), tag.getEnding())+ "\t"+tag.getLabel());
			assertEquals(posTags[index], tag.getLabel());
			index++;
		}
	}
	
	@Test
	public void testQuotes() throws AnnotationFailedException, TException {
		String text = "Bob said ``hello''.";
		Record r = new Record();
		r.setRawText(text);
		r.setWhitespaced(false);
		int[] sentenceOffsets = {0, 19};
		List<Span> sentences = buildSpans(sentenceOffsets);
		Labeling slabeling = new Labeling(sentences);
		int[] tokenOffsets = { 0, 3, 4, 8, 9, 11, 11, 16, 16, 18, 18, 19};
		List<Span> tokens = buildSpans(tokenOffsets);
		Labeling tlabeling = new Labeling(tokens);
		
		r.setLabelViews(new HashMap<String, Labeling>());
		r.getLabelViews().put("sentences", slabeling);
		r.getLabelViews().put("tokens", tlabeling);
		
		Labeling labeling = handler.labelRecord(r);
		String[] posTags = {"NNP", "VBD", "``", "UH", "''", "."};
		int index = 0;
		for (Span tag : labeling.getLabels()) {
			System.out.println(text.subSequence(tag.getStart(), tag.getEnding())+ "\t"+tag.getLabel());
			assertEquals(posTags[index], tag.getLabel());
			index++;
		}
	}
}
