/**
 ** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** both UofT and MSKCC have no obligations to provide maintenance, 
 ** support, updates, enhancements or modifications.  In no event shall
 ** UofT or MSKCC be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** UofT or MSKCC have been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this software; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA;
 ** or find it at http://www.fsf.org/ or http://www.gnu.org.
 **/

package cpath.webservice.args;

import static cpath.webservice.args.CmdArgs.*;


/**
 * cPath2 web service commands.
 * 
 * @author rodche
 *
 */
public enum Cmd {
	SEARCH("Full-text search. This command has four parameters. " +
           "It returns the ordered (by match score) list of BioPAX element URIs " +
           "that matched the query and satisfied the filters. Such " +
           "URI (RDF ID) can be used with other " +
           "webservice commands to extract the corresponding sub-model to BioPAX " +
           "or another supported format.",
           "/search?q=brca*", //URL prefix shouldn't be specified here (it depends on actual server configuration)!
           "Plain text list (one URI per line)",
			new CmdArgs[]{q, page, type, organism, datasource, process}),
	FIND("An alternative full-text search, similar to '/search'but returns XML/JSON. " +
			"It accepts four parameters " +
			"and returns the ordered list of search 'hits', i.e., " +
			"objects describing the corresponding BioPAX entity and utility class elements " +
			"that matched the query and passed filters. A hit's uri (same as the corresponding BioPAX " +
			"object's RDF ID) can be used with other webservice commands to " +
			"extract the corresponding sub-model to BioPAX or another supported format. " +
			"There is also a special variant of " +
			"the command - '/entity/find', which is to find such BioPAX " +
			"Entity class (only) objects that themselves - or have children that " +
			"satisfy the search query and filters.",
			"/find?q=brca*", //URL prefix shouldn't be specified here (it depends on actual server configuration)!
			"Search response - as XML (default) or JSON (" +
			"when called using '/find.json' or '/entity/find.json')",
			new CmdArgs[]{q, page, type, organism, datasource, process}),			
	GET("Gets a BioPAX element or sub-model " +
        "by ID(s).  This command has two parameters.",
        "/get?uri=urn:miriam:uniprot:P38398",
        "BioPAX by default, other formats as specified by the format parameter.  " +
        "See the <a href=\"#valid_output_parameter\">valid values for format parameter</a> below.",
        new CmdArgs[]{uri, format}),
	GRAPH("Executes an advanced graph query on the data within pathway commons. " +
          "Returns a sub-model as the result. This command has four parameters.",
          "/graph?kind=neighborhood&source=HTTP:%2F%2FWWW.REACTOME.ORG%2FBIOPAX%23BRCA2__NUCLEOPLASM__1_9606",
          "BioPAX by default, other formats as specified by the format parameter. " +
          "See the <a href=\"#valid_output_parameter\">valid values for format parameter</a> below.",
          new CmdArgs[]{kind, source, target, format, limit})
        ;
    /* TODO should we expose "/convert" command?
	CONVERT("Converts from BioPAX to simple formats.  This command has two parameters",
			new CmdArgs[]{biopax, format}),
	;
    */
	
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
	
	private Cmd(String info, String example, String output, CmdArgs... args) {
		this.info = info;
        this.example = example;
        this.output = output;
		this.args = args;
	}
}
