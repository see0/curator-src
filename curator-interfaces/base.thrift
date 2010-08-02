/**
 * Base thrift interface for the Cognitive Computation Group Services.
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
 * Span
 * Span covers a portion of text.
 **/
struct Span {
  /**  start index of span in the raw text (inclusive). */
  1: required i32 start,
  /**  end index of span in the raw text (exclusive). */
  2: required i32 end,
  /** label for span. */  
  3: optional string label,
  /** score of span. */
  4: optional double score,
  /** source of span. */
  5: optional Source source,
  /** an additional attribute assoicated with this span. */
  6: optional string attribute,
  /** index of the text (in the multirecord) to which this span references. */
  7: optional i32 multiIndex,
}

/**
 * Labeling.
 * A labeling of text.  
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
 * Clustering.
 * A clustering of labels for the text.  Each cluster is represented 
 * as a Labeling which in turn will have labels (list<Span>) 
 * representing each item in the cluster.
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
 * Node.
 * Nodes store their children.  Referenced as index into list<Node> in
 * the containing struct.
 * Here the link between Node can be labeled.
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
 * Tree
 * Trees are a set of connected nodes with a top node.
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
 * Forest
 * Forest is a set of trees.
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
 * Relation
 * Relations are between two spans.
 */
struct Relation {
  1: required i32 start,
  2: required i32 end,
  3: optional string label,
  4: optional Source source,
  5: optional double score,
}

/**
 * View
 * A View is the most general data structure.  Spans and their relations.
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
  