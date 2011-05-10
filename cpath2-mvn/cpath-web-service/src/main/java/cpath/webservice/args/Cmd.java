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
	SEARCH("Full-text search. " +
           "Returns the list of identifiers that can be used with the 'get' command " +
           "This command has four parameters.",
           "http://awabi.cbio.mskcc.org/cpath2/search?q=brca*",
           "Ordered list of IDs to records within pathway commons that match search criteria.",
			new CmdArgs[]{q, type, organism, datasource}),
	GET("Gets a BioPAX element or sub-model " +
        "by ID(s).  This command has two parameters.",
        "http://awabi.cbio.mskcc.org/cpath2/get?uri=urn:miriam:uniprot:P38398",
        "BioPAX by default, other formats as specified by the format parameter.  See the <a href=\"#valid_output_parameter\">valid values for format parameter</a> below.",
        new CmdArgs[]{uri, format}),
	GRAPH("Executes an advanced graph query on the data within pathway commons. " +
          "Returns a sub-model as the result. This command has four parameters.",
          "http://awabi.cbio.mskcc.org/cpath2/graph?kind=neighborhood&source=HTTP:%2F%2FWWW.REACTOME.ORG%2FBIOPAX%23BRCA2__NUCLEOPLASM__1_9606",
          "BioPAX by default, other formats as specified by the format parameter.  See the <a href=\"#valid_output_parameter\">valid values for format parameter</a> below.",
          new CmdArgs[]{kind, source, dest, format})
        ;
    /* should we expose this method?
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
