package edu.illinois.cs.cogcomp.annotation.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.illinois.cs.cogcomp.thrift.base.Forest;
import edu.illinois.cs.cogcomp.thrift.base.Node;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.thrift.base.Tree;
import edu.illinois.cs.cogcomp.thrift.curator.Record;
import edu.illinois.cs.cogcomp.thrift.parser.MultiParser;
import edu.stanford.nlp.ling.CoreAnnotations.CopyAnnotation;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.TreeGraphNode;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.Pair;

/**
 * Stanford Parser Handler
 * 
 * Implements TwoParser.Iface to provide phrase structure parse trees and
 * dependency trees for a text or record.
 * 
 * @author James Clarke
 * 
 */
public class StanfordParserHandler implements MultiParser.Iface {
	private final Logger logger = LoggerFactory
			.getLogger(StanfordParserHandler.class);
	private final LexicalizedParser parser;
	private static final String VERSION = "0.6";
	private String sentencesfield = "sentences";
	private String tokensfield = "tokens";
	private boolean useTokens = true;
	
	public StanfordParserHandler() {
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

	public List<Forest> parseRecord(Record record) throws TException {
		String rawText = record.getRawText();

		Forest parseForest = new Forest();
		parseForest.setSource(getSourceIdentifier());
		Forest depForest = new Forest();
		depForest.setSource(getSourceIdentifier());
		for (Span sentence : record.getLabelViews().get(sentencesfield).getLabels()) {
			//int offset = sentence.getStart();
			int offset = 0;
			Object input = null;
			String rawsent = rawText.substring(sentence.getStart(), sentence.getEnd());
			if (useTokens) {
				List<Word> s = new ArrayList<Word>();
				for (Span t : record.getLabelViews().get(tokensfield).getLabels()) {
					if (t.getStart() >= sentence.getStart() && t.getEnd() <= sentence.getEnd()) {
						s.add(new Word(rawText.substring(t.getStart(), t.getEnd()), t.getStart(), t.getEnd()));
					}
				}
				input = s;
			} else {
				input = rawsent;
			}
			edu.stanford.nlp.trees.Tree parse = parse(input);
			for (edu.stanford.nlp.trees.Tree pt : parse.getChildrenAsList()) {
				Tree tree = new Tree();
				Node top = generateNode(pt, tree, offset);
				tree.getNodes().add(top);
				tree.setTop(tree.getNodes().size() - 1);
				if (!parseForest.isSetTrees()) {
					parseForest.setTrees(new ArrayList<Tree>());
				}
				parseForest.getTrees().add(tree);
			}
			// dependency stuff
			List<Tree> depTree = parseToDependencyTree(parse, offset, rawsent);
			if (depTree == null) {
				logger.error("Error creating dependency tree for: {}", rawsent);
			} else {
				if (!depForest.isSetTrees()) {
					depForest.setTrees(new ArrayList<Tree>());
				}
				depForest.getTrees().addAll(depTree);
			}
		}
		List<Forest> result = new ArrayList<Forest>();
		result.add(parseForest);
		result.add(depForest);
		return result;
	}

	public String getName() throws TException {
		return "Stanford Parser";
	}

	public String getVersion() throws TException {
		return VERSION;
	}

	public boolean ping() throws TException {
		return true;
	}

	private synchronized edu.stanford.nlp.trees.Tree parse(Object text) {
		long startTime = System.currentTimeMillis();
		edu.stanford.nlp.trees.Tree result = (edu.stanford.nlp.trees.Tree) parser
				.apply(text);
		long endTime = System.currentTimeMillis();
		logger.info("Parsed input in {}ms", endTime - startTime);
		return result;
	}

	private synchronized List<Tree> parseToDependencyTree(
			edu.stanford.nlp.trees.Tree parse, int offset, String input)
			throws TException {
		Sentence<Word> sentence = parse.yield();
		TreebankLanguagePack tlp = new PennTreebankLanguagePack();
		GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
		GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
		Collection<TypedDependency> tdl = gs.typedDependenciesCollapsedTree();
		// position in sentence, Node and position in nodes
		Map<Integer, Pair<Node, Integer>> mainNodeMap = new HashMap<Integer, Pair<Node, Integer>>();
		// will store any copy nodes
		Map<Integer, Pair<Node, Integer>> copyNodeMap = new HashMap<Integer, Pair<Node, Integer>>();

		List<Node> nodes = new ArrayList<Node>();
		Set<Integer> nodesWithHeads = new HashSet<Integer>();
		
		
		//THIS IS CODE TO WORK AROUND TWO HEADS PROBLEM IN STANFORD 1.6.1
		//maps td.dep().index() to => tdl
//		Map<Integer, TypedDependency> heads = new HashMap<Integer, TypedDependency>();
//		int bcount = -1;
//		for (TypedDependency td : tdl) {
//			if ((td.dep().label().get(CopyAnnotation.class) != null
//					&& td.dep().label().get(CopyAnnotation.class)) || (td.gov().label().get(CopyAnnotation.class) != null
//					&& td.gov().label().get(CopyAnnotation.class))) {
//				//special case for copies
//				heads.put(bcount, td);
//				bcount--;
//			} else if (!heads.containsKey(td.dep().index())) {
//				heads.put(td.dep().index(), td);
//			} else if (td.reln().toString().equals("pobj")){
//				//we don't want to add pobj
//				logger.warn("Removing dependency: {}", td);
//				continue;
//			} else {
//				TypedDependency td2 = heads.get(td.dep().index());
//				if (td2.reln().toString().equals("pobj")) {
//					//replace with current dep
//					logger.warn("Removing dependency: {}", td2);
//					heads.put(td.dep().index(), td);
//				} else if (td2.equals(td)) {
//					//case when stanford parser produces duplicate deps
//					logger.warn("Removing depdendency: {}", td);
//				} else {
//					logger.error("FOUND WORD WITH TWO HEADS!!!");
//					logger.error("{} and {}", td, td2);
//					logger.error("Input: {}", input);
//					logger.error("Parse: {}", parse.toString());
//					logger.error("Dependencies: {}", tdl);
//					return null;					
//				}
//			}
//		}
//		tdl = heads.values();
		//END WORK AROUND!!

		//we will bind this nodeMap to the correct one as we build
		Map<Integer, Pair<Node, Integer>> nodeMap;
		Set<TypedDependency> seen = new HashSet<TypedDependency>();
		Set<TreeGraphNode> hasHeads = new HashSet<TreeGraphNode>();
		for (TypedDependency td : tdl) {
			logger.debug("{} duplicate? {}", td, seen.contains(td));
			logger.debug("has heads: {}", hasHeads.contains(td.dep()));
			//work around for duplicate dependencies
			if (seen.contains(td)) {
				logger.warn("Duplicate dependencies found for sentence:");
				logger.warn("{}", input);
				continue;
			}
			seen.add(td);
			
			//work around for words with multiple heads (we only take the first head we encounter)
			if (hasHeads.contains(td.dep())) {
				logger.warn("Non-tree dependency structure found for sentence:");
				logger.warn("{}", input);
				continue;
			}
			hasHeads.add(td.dep());
			
			int hpos = td.gov().index() - 1;
			int dpos = td.dep().index() - 1;
			
			boolean hcopy = td.gov().label().get(CopyAnnotation.class) != null
					&& td.gov().label().get(CopyAnnotation.class);
			boolean dcopy = td.dep().label().get(CopyAnnotation.class) != null
					&& td.dep().label().get(CopyAnnotation.class);

			if (hpos == dpos) {
				logger.debug("hcopy: {}", hcopy);
				logger.debug("dcopy: {}", dcopy);
			}
				
			int depNodePos;
			Node headNode;
			Node depNode;
			if (hcopy) {
				nodeMap = copyNodeMap;
			} else {
				nodeMap = mainNodeMap;
			}
			if (nodeMap.containsKey(hpos)) {
				headNode = nodeMap.get(hpos).first;
	
			} else {
				headNode = new Node();
				headNode.setLabel("dependency node");
				Span headSpan = wordToSpan(sentence.get(hpos), offset);
				if (hcopy) {
					headSpan.setAttribute("copy");
				}
				headNode.setSpan(headSpan);
				nodes.add(headNode);
				nodeMap.put(hpos, new Pair<Node, Integer>(headNode, nodes
						.size() - 1));
			}
			
			if (dcopy) {
				nodeMap = copyNodeMap;
			} else {
				nodeMap = mainNodeMap;
			}
			if (nodeMap.containsKey(dpos)) {
				Pair<Node, Integer> pair = nodeMap.get(dpos);
				depNode = pair.first;
				depNodePos = pair.second;
			} else {
				depNode = new Node();
				depNode.setLabel("dependency node");
				Span dependentSpan = wordToSpan(sentence.get(dpos), offset);
				if (dcopy) {
					dependentSpan.setAttribute("copy");
				}
				depNode.setSpan(dependentSpan);
				nodes.add(depNode);
				nodeMap.put(dpos, new Pair<Node, Integer>(depNode,
						nodes.size() - 1));
				depNodePos = nodes.size() - 1;
			}

			if (!headNode.isSetChildren()) {
				headNode.setChildren(new HashMap<Integer, String>());
			}
			headNode.getChildren().put(depNodePos, td.reln().toString());
			nodesWithHeads.add(depNodePos);
		}

		Set<Integer> headNodes = new HashSet<Integer>();

		for (int i = 0; i < nodes.size(); i++) {
			if (nodesWithHeads.contains(i)) {
				continue;
			}
			headNodes.add(i);
		}
		List<Tree> trees = new ArrayList<Tree>();
		for (Integer head : headNodes) {
			try {
				Tree tree = extractTree(nodes, head);
				trees.add(tree);
			} catch (StackOverflowError e) {
				logger.error("getting stack overflow errors!!!!");
				logger.error("Input: {}", input);
				logger.error("Parse: {}", parse.toString());
				logger.error("Dependencies: {}", tdl);
				return null;
			}
		}
		// if (trees.isEmpty()) {
		// logger.error("Trees empty");
		// logger.error("Parse: {}", parse.toString());
		// logger.error("Dependencies: {}", tdl);
		// return null;
		// }
		return trees;
	}

	private Tree extractTree(List<Node> allNodes, int headindex)
			throws TException {
		List<Node> nodes = new ArrayList<Node>();
		Node head = extractNode(allNodes, nodes, headindex);
		nodes.add(head);
		Tree tree = new Tree();
		tree.setNodes(nodes);
		tree.setTop(nodes.size() - 1);
		return tree;
	}

	private Node extractNode(List<Node> allNodes, List<Node> nodes, int index) {
		Node current = allNodes.get(index);
		if (!current.isSetChildren()) {
			return current;
		}
		Map<Integer, String> children = new HashMap<Integer, String>();
		for (int childindex : current.getChildren().keySet()) {
			nodes.add(extractNode(allNodes, nodes, childindex));
			children.put(nodes.size() - 1, current.getChildren()
					.get(childindex));
		}
		current.setChildren(children);
		return current;
	}

	private Span wordToSpan(Word word, int offset) throws TException {
		Span span = new Span();
		span.setStart(word.beginPosition() + offset);
		span.setEnd(word.endPosition() + offset);
		return span;
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

	public String getSourceIdentifier() throws TException {
		return "stanfordparser-" + getVersion();
	}
}
