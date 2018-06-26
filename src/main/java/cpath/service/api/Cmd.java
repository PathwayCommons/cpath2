package cpath.service.api;

import static cpath.service.api.CmdArgs.*;


/**
 * cPath2 web service commands.
 * 
 * @author rodche
 */
public enum Cmd {
	SEARCH("Full-text search for BioPAX objects. It returns the ordered list of search hits, " +
		"which are simplified description of BioPAX elements " +
		"matching the query and passing all filters. A hit's uri (same as the corresponding BioPAX " +
		"object's RDF ID) can be used with other webservice commands to " +
		"extract the corresponding sub-model to BioPAX or another supported format. ",
		"/search?q=brca*&organism=9606",
		"Search Response that lists Search Hits - XML (default) or JSON (when called as '/search.json?')",
		q, page, type, organism, datasource, user),
	GET("Gets a BioPAX element or sub-model by ID(s).",
        "/get?uri=http://identifiers.org/uniprot/P38398",
        "BioPAX by default, other formats as specified by the format parameter.",
        uri, format, user, pattern, subpw, layout),
	GRAPH("Executes an advanced graph query on the data within pathway commons. " +
        "Returns a sub-model as the result. This command can have the following parameters.",
        "/graph?kind=neighborhood&source=URI1&source=URI2&...",
        "BioPAX by default, other formats as specified by the format parameter.",
        kind, source, target, format, limit, direction, organism, datasource, user, pattern, subpw, layout),
    TOP_PATHWAYS("Gets Top Pathways. This command accepts optional filter by organism and by datasource values",
    	"/top_pathways",
        "Search Response - XML (JSON, when called as '/top_pathways.json?') contains the list of all top pathways.", 
        organism, datasource, q, user),
    TRAVERSE("Gets data property values (or elements's URIs) at the end of the property path.",
    	"/traverse?uri=http://identifiers.org/uniprot/P38398&path=ProteinReference/organism/displayName",
    	"Traverse Response - XML (or JSON, when called as '/traverse.json?').", 
    	path, uri, user)
    ;
	
	private final CmdArgs[] args; //Array is better for use in json/jsp than List/Set
	private final String info;
    private final String example;
    private final String output;
	
	public CmdArgs[] getArgs() {
		return args;
	}
	
	public String getInfo() {
		return info;
	}

    public String getExample() {
        return example;
    }
    
    public String getOutput() {
        return output;
    }
	
	Cmd(String info, String example, String output, CmdArgs... args) {
		this.info = info;
        this.example = example;
        this.output = output;
		this.args = args;
	}

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
