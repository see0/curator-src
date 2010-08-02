/**
 * Thrift interface for a labeler.
 *
 * thrift -r --gen <lang> tagger.thrift 
 * James Clarke <clarkeje@gmail.com>
 **/

include "base.thrift"
include "curator.thrift"

namespace java edu.illinois.cs.cogcomp.thrift.labeler
namespace cpp  cogcomp.thrift.labeler
namespace py cogcomp.labeler
namespace perl cogcomp.Labeler
namespace php tagger

/**
 * Tagging service.
 **/
service Labeler extends base.BaseService {
  /**
   * Labels a given record.
   * record - the record
   * result - list of base.Labeling for the input, one per sentence.
   **/
  base.Labeling labelRecord(1:curator.Record record) throws (1:base.AnnotationFailedException ex),

}

/**
 * Two Taggerg service.
 * Useful for things that tag the text twice. i.e.,
 **/
service MultiLabeler extends base.BaseService {
  /**
   * Labels a given record.
   * record - the record
   * result - list of base.Labeling for the input, one per sentence.
   **/
  list<base.Labeling> labelRecord(1:curator.Record record) throws (1:base.AnnotationFailedException ex),

}