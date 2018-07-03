package cpath.service;

/**
 * cPath2 web service command arguments.
 */
public enum CmdArgs {
  uri("known BioPAX entity URI or standard identifier (e.g., gene symbol); multiple values are supported (array)"),
  q("query string (full-text search supports Lucene query syntax)"),
  page("full-text search query results page number (>=0)"),
  type("a BioPAX class name"),
  kind("graph query type"),
  format("output format name"),
  organism("filter by organism, e.g., taxonomy ID (recommended) or name; array"),
  datasource("filter by data source (name, id or uri; array)"),
  source("graph query source URI(s); array"),
  target("graph query destination URI(s); array"),
  limit("graph query search distance limit"),
  path("string expression, like 'Entity/xref:PublicationXref/id' - connected by '/' and ':' " +
    "BioPAX types and properties - a path to reach specific model elements through given ones"),
  direction("graph query parameter 'direction'"),

  //optional parameters for sub-model extraction and conversion to other format algorithms -
  pattern("when format is SIF or TXT - SIF type (pattern) name(s) to apply (can be array)"),
  user("client's name, email, or app (for the service access log and usage reporting)"),
  subpw("'true' or 'false' (default); for the 'get' and 'graph' queries; " +
    " whether to skip traversing into sub-pathways of pathways in the result sub-model"),;

  private final String info;

  public String getInfo() {
    return info;
  }

  CmdArgs(String info) {
    this.info = info;
  }

  @Override
  public String toString() {
    return name().toLowerCase();
  }
}
