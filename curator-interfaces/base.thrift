/**
 * Base thrift interface for the Curator data structures.
 * All services should include this file and inherit from the BaseService
 * and reuse as many of the data structures as possible.
 *
 *  thrift --gen <lang> base.thrift
 *
 * James Clarke <clarkeje@gmail.com>
 **/

namespace java edu.illinois.cs.cogcomp.thrift.base
namespace cpp cogcomp.thrift.base
namespace py cogcomp.base
namespace perl Cogcomp.Base
namespace php base

typedef string Text
typedef string Source

/**
 * Span covers a portion of text.
 *
 * <code>start</code> - start index of span in the raw text (inclusive).<br/>
 * <code>ending</code> - end index of span in the raw text (exclusive).<br/>
 * <code>label</code> - label for span.<br/>
 * <code>score</code> - score of span.<br/>
 * <code>source</code> - the source annotator of this span.<br/>
 * <code>attributes</code> - any additional attributes assoicated with this span. a map of attribute_name => value.<br/>
 * <code>multiIndex</code> - if associated with a MultiRecord which Record object is this span for.
 **/
struct Span {
  /**  start index of span in the raw text (inclusive). */
  1: required i32 start,
  /**  ending index of span in the raw text (exclusive). */
  2: required i32 ending,
  /** label for span. */  
  3: optional string label,
  /** score of span. */
  4: optional double score,
  /** source of span. */
  5: optional Source source,
  /** any additional attributes assoicated with this span. */
  6: optional map<string, string> attributes,
  /** index of the text (in the multirecord) to which this span references. */
  7: optional i32 multiIndex,
}

/**
 * A labeling of text.  Really a list of Spans.
 *
 * <code>labels</code> - the labels for this labeling. Each label represented as a Span.<br/>
 * <code>source</code> - the source annotator this labeling came from.<br/>
 * <code>score</code> - the score for this labeling.<br/>
 * <code>rawText</code> - the raw text for this labeling (if null then consult the labeling's parent's rawText field, i.e., the Record's)
 */
struct Labeling {
  /**  the labels as spans. */
  1: required list<Span> labels,
  /**  the source of this labeling came from. */
  2: optional Source source,
  /** score for this labeling. */
  3: optional double score,
  /** the raw text for this labeling (if null then consult the labeling's parent's rawText field)*/
  4: optional string rawText,
}

/**
 * A clustering of labels for the text.  Each cluster is represented 
 * as a Labeling which in turn will have labels (list<Span>) 
 * representing each item in the cluster.
 *
 * <code>clusters</code> - the clusters for the this clustering. Each cluster represented as a Labeling.<br/>
 * <code>source</code> - the source annotator this clustering came from.<br/>
 * <code>score</code> - the score for this clustering.<br/>
 * <code>rawText</code> - the raw text for this clustering (if null then consult the labeling's parent's rawText field, i.e., the Record's)
 */
struct Clustering {
  /** the clusters, each cluster is a Labeling. */
  1: required list<Labeling> clusters,
  /** the source of this Clustering */
  2: optional Source source,
  /** score for this clustering */
  3: optional double score,
  /**  the raw text for this clustering (if null then consult the clustering's parent's rawText field)*/
  4: optional string rawText,
}


/**
 * Nodes store their children.  Referenced as index into list<Node> in
 * the containing struct.
 * Here the link between Node can be labeled.
 * 
 * <code>label</code> - the label for this Node.<br/>
 * <code>span</code> - the span this node covers.<br/>
 * <code>children</code> -  the children of the node represented as a map of <child index, edge label>. Empty string implies no label. The index is the index of the node in the parent data structure (i.e., the tree's nodes).<br/>
 * <code>source</code> - the source annotator this node came from.<br/>
 * <code>score</code> - the score for this node.<br/>
 */
struct Node {
  /** the label of the node. */
  1: required string label,
  /** the span this node covers. */
  2: optional Span span,
  /** the children of the node represented as a map of <child index, edge label>. Empty string implies no label. */
  3: optional map<i32, string> children,
  /** source of the node . */
  4: optional Source source,
  /** the score for this node. */
  5: optional double score,
}

/**
 * Trees are a set of connected nodes with a top node.
 *
 * <code>nodes</code> - the list of labeled nodes. <br/>
 * <code>top</code> - the index in nodes of the top node. <br/>
 * <code>source</code> - the source annotator this Tree came from.<br/>
 * <code>score</code> - the score for this Tree.
 */
struct Tree {
  /** list of labeled nodes. */
  1: required list<Node> nodes,
  /** the  index of top/root node in nodes. */
  2: required i32 top,
  /** the source of this tree. */
  3: optional Source source,
  /** the score of this tree. */
  4: optional double score,
}

/**
 * Forest is a set of trees.
 *
 * <code>trees</code> - the trees for the this forest.<br/>
 * <code>source</code> - the source annotator this forest came from.<br/>
 * <code>score</code> - the score for this forest.<br/>
 * <code>rawText</code> - the raw text for this forest (if null then consult the labeling's parent's rawText field, i.e., the Record's)
 */
struct Forest {
  /** the trees in this Forest */
  1: required list<Tree> trees,
  /** the raw text for this Forest (if null then consult the tree's parent's rawText field) */
  2: optional string rawText,
  /** the source of this Forest */
  3: optional Source source,
}


/**
 * Relations are between two spans.
 *
 * <code>start</code> - the index of the span that starts this relation.<br/>
 * <code>ending</code> - the index of the span that ends this relation.<br/>
 * <code>label</code> - the label for this relation.<br/>
 * <code>source</code> - where this relation came from.<br/>
 * <code>score</code> - the score for this relation.<br/>
 */
struct Relation {
  1: required i32 start,
  2: required i32 ending,
  3: optional string label,
  4: optional Source source,
  5: optional double score,
}

/**
 * A View is the most general data structure.  Spans and their relations.
 *
 * <code>spans</code> - the spans of for this view.<br/>
 * <code>relations</code> - the relations of this view.<br/>
 * <code>source</code> - the source annotator this view came from.<br/>
 * <code>score</code> - the score for this view.<br/>
 * <code>rawText</code> - the raw text for this view (if null then consult the labeling's parent's rawText field, i.e., the Record's)
 */
struct View {
  1: required list<Span> spans,
  2: required list<Relation> relations,
  3: optional string rawText,
  4: optional Source source,
  5: optional double score,
}

exception ServiceUnavailableException {
  1: string reason,
}

exception ServiceSecurityException {
  1: string reason,
}

exception AnnotationFailedException {
  1: string reason,
}

/**
 * Base Service
 */
service BaseService {
/**
 * Ping? Pong! return True!
 */
 bool ping(),
/**
 * Return the name of this service.
 */
 string getName(),
/**
 * Return the version of this service.
 */
 string getVersion(),

/**
 * Return the an identifier to be used in source fields.  This should be of the form <shortname>-<version> 
 * where <shortname> contains no spaces or hypens and <version> is parsable into a number (double, int etc). 
 * i.e.,. server-1.0.
 *
 * Any datastructures returned must have the source field populated with the result of this function.
 */
 string getSourceIdentifier(),
}
  