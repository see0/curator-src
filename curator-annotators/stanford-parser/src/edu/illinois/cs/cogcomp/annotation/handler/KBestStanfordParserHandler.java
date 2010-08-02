package edu.illinois.cs.cogcomp.annotation.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.illinois.cs.cogcomp.thrift.base.AnnotationFailedException;
import edu.illinois.cs.cogcomp.thrift.base.Forest;
import edu.illinois.cs.cogcomp.thrift.base.Node;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.thrift.base.Tree;
import edu.illinois.cs.cogcomp.thrift.curator.Record;
import edu.illinois.cs.cogcomp.thrift.parser.KBestParser;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.TreeFactory;
import edu.stanford.nlp.util.ScoredObject;

public class KBestStanfordParserHandler implements KBestParser.Iface {
	private final Logger logger = LoggerFactory
			.getLogger(KBestStanfordParserHandler.class);
	private final LexicalizedParser parser;
	private static final String VERSION = "0.1";
	private String sentencesfield = "sentences";
	private String tokensfield = "tokens";
	private boolean useTokens = true;

	public KBestStanfordParserHandler() {
		Configuration config = null;
		try {
			config = new PropertiesConfiguration("configs/stanford.properties");
		} catch (ConfigurationException e) {
			logger.warn("Error reading configuration file. {}", e);
		}
		String data = config.getString("stanford.data",
				"data/englishPCFG.ser.gz");
		parser = new LexicalizedParser(data);
		parser.setOptionFlags(new String[] { "-retainTmpSubcategories" });
	}

	private Node generateNode(edu.stanford.nlp.trees.Tree parse, Tree tree,
			int offset) throws TException {
		if (!tree.isSetNodes()) {
			tree.setNodes(new ArrayList<Node>());
		}
		List<Node> nodes = tree.getNodes();
		Node node = new Node();

		node.setLabel(parse.value());
		for (edu.stanford.nlp.trees.Tree pt : parse.getChildrenAsList()) {
			if (!node.isSetChildren()) {
				node.setChildren(new TreeMap<Integer, String>());
			}
			if (pt.isLeaf()) {
				continue;
			} else {
				Node child = generateNode(pt, tree, offset);
				nodes.add(child);
				node.getChildren().put(nodes.size() - 1, "");
			}
		}
		Span span = new Span();
		List<Word> words = parse.yield();
		span.setStart(words.get(0).beginPosition() + offset);
		span.setEnd(words.get(words.size() - 1).endPosition() + offset);
		node.setSpan(span);
		return node;
	}

	public List<Forest> parseRecord(Record record, int k)
			throws AnnotationFailedException, TException {
		String rawText = record.getRawText();
		// the semantics here is going to be different from other parsers
		// each forest will contain the k best trees for a sentence
		// rather than each forest containing the kth best tree for the
		// sentences
		List<Forest> forests = new ArrayList<Forest>();
		for (Span sentence : record.getLabelViews().get(sentencesfield)
				.getLabels()) {
			Forest parseForest = new Forest();
			parseForest.setSource(getSourceIdentifier());
			// int offset = sentence.getStart();
			int offset = 0;
			Object input = null;
			String rawsent = rawText.substring(sentence.getStart(),
					sentence.getEnd());
			if (useTokens) {
				List<Word> s = new ArrayList<Word>();
				for (Span t : record.getLabelViews().get(tokensfield)
						.getLabels()) {
					if (t.getStart() >= sentence.getStart()
							&& t.getEnd() <= sentence.getEnd()) {
						s.add(new Word(rawText.substring(t.getStart(),
								t.getEnd()), t.getStart(), t.getEnd()));
					}
				}
				input = s;
			} else {
				input = rawsent;
			}
			List<ScoredObject<edu.stanford.nlp.trees.Tree>> kParses = parseK(
					input, k);
			for (ScoredObject<edu.stanford.nlp.trees.Tree> so : kParses) {
				if (so.object().numChildren() > 1) {
					logger.warn("More than one child in the top Tree.\n{}",
							rawText);
				}
				edu.stanford.nlp.trees.Tree pt = so.object().firstChild();
				Tree tree = new Tree();
				tree.setScore(so.score());
				Node top = generateNode(pt, tree, offset);
				tree.getNodes().add(top);
				tree.setTop(tree.getNodes().size() - 1);
				if (!parseForest.isSetTrees()) {
					parseForest.setTrees(new ArrayList<Tree>());
				}
				parseForest.getTrees().add(tree);
			}
			forests.add(parseForest);
		}
		return forests;
	}

	private synchronized List<ScoredObject<edu.stanford.nlp.trees.Tree>> parseK(
			Object input, int k) {
		long startTime = System.currentTimeMillis();
		boolean parsed = false;
		if (input instanceof List) {
			List<? extends HasWord> words = (List<? extends HasWord>) input;
			parsed = parser.parse(words);
		} else if (input instanceof String) {
			parsed = parser.parse((String) input);
		}
		if (parsed) {
			long endTime = System.currentTimeMillis();
			logger.info("Parsed input in {}ms", endTime - startTime);
			return parser.getKBestPCFGParses(k);
		}
		// if can't parse or exception, fall through
		// this was taken from the LexicalizedParser source
		List<? extends HasWord> lst = null;
		if (input instanceof List) {
			lst = (List<? extends HasWord>) input;
		} else if (input instanceof String) {
			DocumentPreprocessor dp = new DocumentPreprocessor(
					parser.getOp().tlpParams.treebankLanguagePack()
							.getTokenizerFactory());
			lst = dp.getWordsFromString((String) input);
		}
		TreeFactory lstf = new LabeledScoredTreeFactory();
		List<edu.stanford.nlp.trees.Tree> lst2 = new ArrayList<edu.stanford.nlp.trees.Tree>();
		for (Object obj : lst) {
			String s = obj.toString();
			edu.stanford.nlp.trees.Tree t = lstf.newLeaf(s);
			edu.stanford.nlp.trees.Tree t2 = lstf.newTreeNode("X",
					Collections.singletonList(t));
			lst2.add(t2);
		}
		List<ScoredObject<edu.stanford.nlp.trees.Tree>> result = new ArrayList<ScoredObject<edu.stanford.nlp.trees.Tree>>();
		result.add(new ScoredObject<edu.stanford.nlp.trees.Tree>(lstf
				.newTreeNode("X", lst2), 0.0));
		long endTime = System.currentTimeMillis();
		logger.info("Parsed input in {}ms", endTime - startTime);
		return result;
	}

	public String getName() throws TException {
		return "Stanford K-Best Parser";
	}

	public String getSourceIdentifier() throws TException {
		return "stanford-kbest-" + getVersion();
	}

	public String getVersion() throws TException {
		return VERSION;
	}

	public boolean ping() throws TException {
		return true;
	}

}
