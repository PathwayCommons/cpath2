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

package cpath.mapper.internal;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cpath.common.FetcherHTTPClient;

/**
 * @author rodche
 *
 */
public class SimpleUniprotClient {
	public static final String QUERYBASE = "http://www.uniprot.org/uniprot/?format=tab&";
	public static final Pattern PATTERN = 
		Pattern.compile("^([A-N,R-Z][0-9][A-Z][A-Z, 0-9][A-Z, 0-9][0-9])|([O,P,Q][0-9][A-Z, 0-9][A-Z, 0-9][A-Z, 0-9][0-9])$");
	
	private FetcherHTTPClient client;
	   
	public SimpleUniprotClient(FetcherHTTPClient fetcherHTTPClient) {
		this.client = fetcherHTTPClient;
	}
	   
    /**
     * 
     * @param id - UniProt accession number (any)
     * @return primary id (accession)
     */
    public String getPrimaryId(String id) {
    	String accession = null;
        String query = QUERYBASE + "columns=id&query=accession:" + id;
        
        try {
        	InputStream inputStream = client.getDataFromServiceAsStream(query);
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            // primary ID always appears first in the returned table
            StringBuffer sb = new StringBuffer();
            String line;
            while((line = in.readLine()) != null) {
            	sb.append(line);
            }
            String resultTxt = sb.toString();
            Matcher matcher = PATTERN.matcher(resultTxt);
            if(matcher.find()) {
            	accession = matcher.group();
            } else {
            	throw new IllegalArgumentException("UniProt returned no results for " + id);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Cannot get result from " + query, ex);
        }
        
        return accession;
    }
	
}
