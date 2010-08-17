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

public class IllinoisChunkerHandlerTest {
	private Labeler.Iface handler;

	@Before
	public void setUp() throws Exception {
		handler = new IllinoisChunkerHandler();
	}

	private List<Span> buildSpans(int[] offsets) {
		List<Span> spans = new ArrayList<Span>();
		int index = 0;
		while (index < offsets.length) {
			Span span = new Span(offsets[index], offsets[index + 1]);
			spans.add(span);
			index = index + 2;
		}
		return spans;
	}
	
	private List<Span> buildSpans(int[] offsets, String[] labels) {
		List<Span> spans = new ArrayList<Span>();
		int index = 0;
		int index2 = 0;
		while (index < offsets.length) {
			Span span = new Span(offsets[index], offsets[index + 1]);
			span.setLabel(labels[index2]);
			spans.add(span);
			index = index + 2;
			index2++;
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

		String[] posTags = {"NNP", "VBD", "NNP", "VBP", "DT", "NN", "IN", "DT", "NN", "NN", "."};
		List<Span> pos = buildSpans(tokenOffsets, posTags);
		Labeling plabeling = new Labeling(pos);
		r.setLabelViews(new HashMap<String, Labeling>());
		r.getLabelViews().put("sentences", slabeling);
		r.getLabelViews().put("tokens", tlabeling);
		r.getLabelViews().put("pos", plabeling);
		
		Labeling labeling = handler.labelRecord(r);

		int[] chunkOffsets = {0,3,4,7,8,11,12,17,18,25,26,28,29,44};
		String[] chunkLabels = {"NP", "VP", "NP", "VP", "NP", "PP", "NP"};
		int index = 0;
		int index2 = 0;
		for (Span tag : labeling.getLabels()) {
			System.out.println(text.subSequence(tag.getStart(), tag.getEnding())+ "\t"+tag.getLabel());
			assertEquals(chunkOffsets[index], tag.getStart());
			index++;
			assertEquals(chunkOffsets[index], tag.getEnding());
			index++;
			assertEquals(chunkLabels[index2], tag.getLabel());
			index2++;
		}
	}

}
