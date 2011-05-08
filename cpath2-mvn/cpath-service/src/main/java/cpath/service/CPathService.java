/**
 ** Copyright (c) 2010 Memorial Sloan-Kettering Cancer Center (MSKCC)
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

package cpath.service;

import java.util.Map;

import org.biopax.paxtools.model.BioPAXElement;

import cpath.dao.PaxtoolsDAO;
import cpath.dao.filters.SearchFilter;


/**
 * CPath^2 Service is a mid-tier or adapter between DAOs and web controllers. 
 * 
 * Can be used in a console application or integration tests (web container is not required!)
 * 
 * This is to implement several query methods 
 * that accept valid parameters and beans, handle exceptions,
 * and return results packed in the HashMap with predefined keys.
 * 
 * TODO Enables "second query" use case -
 * when no data found in the pathway data (main) storage,
 * it will hit Warehouse to find general information (about 
 * CVs, small molecules and proteins)
 * 
 * @author rodche
 *
 */
public interface CPathService {

	/**
	 * Enumeration: map keys for the cPath^2 service results 
	 */
	public static enum ResultMapKey {
		MODEL, // key to BioPAX (PaxTools) Model, if any is returned
		ELEMENT, // a BioPAXElement (detached from DAO)
		DATA, // key to query results (to be treated by the caller), e.g., id-list, image, etc.
		ERROR, // key to error string or object (e.g., toString() will be used to get message)
		COUNT, // key to "records" count, e.g. items in the ID-list or no. of BioPAX elements
		MISC; // key to, e.g., lucene search statistics, etc.
	}
	
	/**
	 * Enumeration of cPath service output formats
	 * 
	 * @author rodche
	 *
	 */
	public static enum OutputFormat {
        BIOPAX("BioPAX RDF/XML Format"),
		BINARY_SIF("Simple Binary Interactions Format"),
        EXTENDED_BINARY_SIF("Extended Simple Binary Interactions Format"),
		GSEA("Gene Set Expression Analysis Format")
		;
        
        private final String info;
        
        public String getInfo() {
			return info;
		}
        
        private OutputFormat(String info) {
			this.info = info;
		}
	}

	
	public static enum GraphQueryDirection
	{
		// Directions
		UPSTREAM,
		DOWNSTREAM,
		BOTHSTREAM,
	}
	
	
	public static enum GraphQueryLimit
	{
		NORMAL,
		SHORTEST_PLUS_K
	}
	
	
	//--- Graph queries ---------------------------------------------------------------------------|

	/**
	 * Gets the BioPAX element by id 
	 * (first-level object props are initialized),
	 * converts to the required output format (if possible), 
	 * and returns as map.
	 * @param format
	 * @param id the list of URIs to fetch
	 * 
	 * @see ResultMapKey
	 * 
	 * @return
	 */
	Map<ResultMapKey, Object> fetch(OutputFormat format, String... id);

	
	/**
	 * Full-text search for the BioPAX elements. 
	 * Returns the map result that contains the list of identifiers,
	 * count, and supplementary information.
	 * 
	 * @see ResultMapKey
	 * 
	 * @param queryStr if null or empty, list/count all elements (of the class)
	 * @param biopaxClasses
	 * @param organisms an array of the taxonomy ids
	 * @param dataSources an array of pathway data provider names
	 * @return
	 * 
	 * @see PaxtoolsDAO#find(String, Class[], SearchFilter[]...)
	 */
	Map<ResultMapKey, Object> find(String queryStr, 
			Class<? extends BioPAXElement>[] biopaxClasses, String[] organisms,
			String... dataSources);


	/**
	 * Generates the BioPAX validation report for the pathway data provider
	 * (report will be associated with 'DATA' key in the returned map).
	 * 
	 * @param metadataIdentifier
	 * @return
	 */
	Map<ResultMapKey, Object> getValidationReport(String metadataIdentifier);
	

	/**
	 * Runs a neighborhood query using the given parameters.
	 *
	 * @param format output format
	 * @param source IDs of seed of neighborhood
	 * @param limit search limit (integer value)
	 * @param direction flag 
	 * @return the neighborhood
	 */
	Map<ResultMapKey, Object> getNeighborhood(OutputFormat format, 
			String[] source, Integer limit, GraphQueryDirection direction);

	/**
	 * Runs a paths-between query from the given sources to the given targets.
	 *
	 * @param format output format
	 * @param source IDs of source molecules
	 * @param target IDs of target molecules
	 * @param limit search limit (integer value)
	 * @param limitType {@link GraphQueryLimit}
	 * @return paths between
	 */
	Map<ResultMapKey, Object> getPathsBetween(OutputFormat format, String[] source, 
			String[] target, Integer limit, GraphQueryLimit limitType);

	/**
	 * Runs a common upstream or downstream query.
	 *
	 * @param format output format
	 * @param source IDs of query seed
	 * @param limit search limit
	 * @param direction - can be {@link GraphQueryDirection#DOWNSTREAM} or {@link GraphQueryDirection#UPSTREAM}
	 * @return common stream
	 */
	Map<ResultMapKey, Object> getCommonStream(OutputFormat format, 
			String[] source, Integer limit, GraphQueryDirection direction);

	//---------------------------------------------------------------------------------------------|

	/**
	 * For the given biopax, converts to the desired output format.
     *
     * @param biopax
	 * @param format
	 * 
	 * @see ResultMapKey
	 * 
	 * @return
	 */
	Map<ResultMapKey, Object> convert(String biopax, OutputFormat format);
	
}
