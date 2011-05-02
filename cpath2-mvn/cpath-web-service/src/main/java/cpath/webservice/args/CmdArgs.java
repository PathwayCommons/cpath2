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

/**
 * cPath2 web service command arguments.
 * 
 * @author rodche
 *
 */
public enum CmdArgs {
	uri("a URI, BioPAX element's ID (multiple 'uri=' per query are allowed). " +
		"Note: normally (in a client app), you should not encode URIs, but, " +
		"when for some reason you're using a browser to access the web service," +
		"you should encode, e.g., HTTP://WWW.REACTOME.ORG/BIOPAX#BRCA2__NUCLEOPLASM__1_9606" +
		" becomes HTTP:%2F%2FWWW.REACTOME.ORG%2FBIOPAX%23BRCA2__NUCLEOPLASM__1_9606"),
	q("a query string"),
	type("a BioPAX class"),
	kind("advanced (graph) query type"),
	format("output format (converting from BioPAX)"),
	organism("filter by organism (multiple 'organism=' are allowed)"),
	datasource("filter by data sources (multiple 'datasource=' are allowed)"),
	source("graph query source URI (multiple 'source=' are allowed)"),
	dest("graph query destination URI (multiple 'dest=' are allowed)"),
	biopax("BioPAX OWL to convert"),
	alg("a user-defined algorithm to run (a java Some.class using Paxtools core API)"), //TODO future (graph query extention point, plugins...)
	;
	
	private final String info;
	
	public String getInfo() {
		return info;
	}

	private CmdArgs(String info) {
		this.info = info;
	}
}
