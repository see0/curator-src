include "base.thrift"

namespace java edu.illinois.cs.cogcomp.thrift.curator
namespace cpp cogcomp.thrift.curator
namespace py cogcomp.curator
namespace perl Cogcomp.curator
namespace php curator


struct Record {
  /** how to identify this record. */
   1: required string identifier,
   2: required string rawText,
   3: required map<string, base.Labeling> labelViews,
   4: required map<string, base.Clustering> clusterViews,
   5: required map<string, base.Forest> parseViews,
   6: required map<string, base.View> views,
   7: required bool whitespaced,
}

struct MultiRecord {
  1: required string identifier,
  2: required list<string> records,
  3: required map<string, base.Labeling> labelViews,
  4: required map<string, base.Clustering> clusterViews,
  5: required map<string, base.Forest> parseViews,
  6: required map<string, base.View> views,
}

service Curator extends base.BaseService {
  bool isCacheAvailable(),

  Record getRecord(1:string text) throws (1:base.ServiceUnavailableException ex, 2:base.AnnotationFailedException ex2),

  Record getRecordById(1:string identifier) throws (1:base.ServiceUnavailableException ex, 2:base.AnnotationFailedException ex2),

  Record provide(1:string view_name, 2:string text, 3:bool forceUpdate)
  throws (1:base.ServiceUnavailableException suex, 2:base.AnnotationFailedException afex),

  Record wsprovide(1:string view_name, 2:list<string> sentences, 3:bool forceUpdate)
  throws (1:base.ServiceUnavailableException suex, 2:base.AnnotationFailedException afex),

  Record wsgetRecord(1:list<string> sentences) throws (1:base.ServiceUnavailableException ex, 2:base.AnnotationFailedException ex2),

  void storeRecord(1:Record record) throws (1:base.ServiceSecurityException ssex), 

  MultiRecord getMultiRecord(1:list<string> texts) throws (1:base.ServiceUnavailableException ex, 2:base.AnnotationFailedException ex2),

  MultiRecord provideMulti(1:string view_name, 2:string text, 3:bool forceUpdate)
  throws (1:base.ServiceUnavailableException suex, 2:base.AnnotationFailedException afex),

  void storeMultiRecord(1:MultiRecord record) throws (1:base.ServiceSecurityException ssex), 

  map<string,string> describeAnnotations(),
}
