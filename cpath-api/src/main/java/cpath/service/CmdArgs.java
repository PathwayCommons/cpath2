package cpath.service;

/**
 * cPath2 web service command arguments.
 * 
 * @author rodche
 *
 */
public enum CmdArgs {
	uri("an identifier, usually a BioPAX element URI (default); multiple values per query are supported"),
    q("query string (full-text search)"),
    page("search results page number (>=0)"),
	type("a BioPAX class name"),
	kind("graph query type"),
	format("output format name"),
	organism("filter by organism (e.g., taxonomy ID)"),
	datasource("filter by data source"),
	source("graph query source URI"),
	target("graph query destination URI"),
    limit("graph query search distance limit"),
    biopax("a BioPAX OWL to convert"),
    path("a BioPAX property path expression (like xPath)"),
    direction("graph query parameter 'direction'"),
	pattern("pattern - simple interaction type or inference rule name"),
	user("e.g., client name, email, or app name to use in the access log")
    //TODO alg("a user-defined BioPAX data analysis (code) to run."),
	;
	
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
