package edu.illinois.cs.cogcomp.annotation.handler;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;

import edu.illinois.cs.cogcomp.thrift.base.AnnotationFailedException;
import edu.illinois.cs.cogcomp.thrift.base.Labeling;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.thrift.curator.Record;
import edu.illinois.cs.cogcomp.thrift.labeler.MultiLabeler;

public class IllinoisTokenizerHandlerTest {
	private MultiLabeler.Iface handler;

	@Before
	public void setUp() throws Exception {
		handler = new IllinoisTokenizerHandler();

	}

	@Test
	public void testSimpleSentences() throws AnnotationFailedException, TException {
		String text = "Bob saw Jim leave the car in the parking lot. He didn't comment on it, even though he wanted to!";
		Record r = new Record();
		r.setRawText(text);
		r.setWhitespaced(false);
		List<Labeling> labeling = handler.labelRecord(r);

		int[] sentenceOffsets = { 0, 45, 46, 96 };
		int index = 0;
		for (Span sentence : labeling.get(0).getLabels()) {
			System.out.println(text.substring(sentence.getStart(),
					sentence.getEnding()));
			assertEquals(sentenceOffsets[index], sentence.getStart());
			index++;
			assertEquals(sentenceOffsets[index], sentence.getEnding());
			index++;
		}
		index = 0;
		int[] tokenOffsets = { 0, 3, 4, 7, 8, 11, 12, 17, 18, 21, 22, 25, 26,
				28, 29, 32, 33, 40, 41, 44, 44, 45, 46, 48, 49, 52, 52, 55, 56,
				63, 64, 66, 67, 69, 69, 70, 71, 75, 76, 82, 83, 85, 86, 92, 93,
				95, 95, 96 };
		for (Span token : labeling.get(1).getLabels()) {
			System.out.println(text.substring(token.getStart(),
					token.getEnding()));
			assertEquals(tokenOffsets[index], token.getStart());
			index++;
			assertEquals(tokenOffsets[index], token.getEnding());
			index++;
		}
	}

}
