/**
 * Thrift interface for a parser.
 *
 * thrift -r --gen <lang> parser.thrift 
 * James Clarke <clarkeje@gmail.com>
 **/

include "base.thrift"
include "curator.thrift"

namespace java edu.illinois.cs.cogcomp.thrift.parser
namespace cpp  cogcomp.thrift.parser
namespace py cogcomp.parser
namespace perl cogcomp.Parser
namespace php parser

/**
 * Parser service.
 **/
service Parser extends base.BaseService {

  /**
   * Parses the Record.
   * record - the record
   * result - a base.Forest for the input.
   **/
  base.Forest parseRecord(1:curator.Record record) throws (1:base.AnnotationFailedException ex),
}

/**
 * Two Parser service which returns a ParsePair (useful for parsers that return 
 * phrase-structure trees and dependency trees.
 **/
service MultiParser extends base.BaseService {
  /**
   * Parses the Record.
   * record - the record
   * result - a ParsePair for the input.
   **/
  list<base.Forest> parseRecord(1:curator.Record record) throws (1:base.AnnotationFailedException ex),
}
