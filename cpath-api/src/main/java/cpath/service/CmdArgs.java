package cpath.service;

/**
 * cPath2 web service command arguments.
 * 
 * @author rodche
 *
 */
public enum CmdArgs {
	uri("an identifier, usually a BioPAX element URI (default); multiple values per query are supported (array)"),
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
	//optional parameters for sub-model extraction and conversion, etc. algorithms
	pattern("when format is SIF or TXT - SIF type (pattern) name(s) to apply (can be array)"),
	user("client's name, email, or app (for the service access log and usage reporting)"),
	subpw("true (default) or false; for the 'get by URI' queries only; " +
			"- whether to skip sub-pathways when exporting a BioPAX sub-model to another format"),//TODO
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
