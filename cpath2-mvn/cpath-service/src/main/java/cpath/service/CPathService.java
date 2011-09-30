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

import org.biopax.paxtools.controller.PathAccessor;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.query.algorithm.Direction;
import org.biopax.validator.result.ValidatorResponse;

import cpath.dao.PaxtoolsDAO;
import cpath.dao.filters.SearchFilter;
import cpath.service.jaxb.SearchResponseType;


/**
 * CPath^2 Service is an adapter between DAO and web controllers. 
 * Can be used in a console application or integration tests 
 * (web container is not required!)
 * 
 * This interface defines several middle-tier data access and analysis methods 
 * that accept valid parameters, handle exceptions, and return results packed 
 * in a universal HashMap (using predefined keys). This class therefore creates an 
 * additional "elastic" layer between the public (web, console) api and persistence 
 * methods, allowing to modify either its implementation or the DAO implementation 
 * without breaking end user's services (backward compatibility).
 * 
 * 
 * @author rodche
 *
 */
public interface CPathService {

	/**
	 * Enumeration: map keys for the cPath^2 service tier. 
	 * Methods have to return a Map using key strings from this enumeration.
	 * 
	 */
	public static enum ResultMapKey {
		/**
		 * key to a BioPAX (PaxTools) Model, if any is returned, or any other "model" object (up to the implementation)
		 */
		MODEL, 
		/**
		 *  key to a BioPAXElement (detached from DAO) or other object (e.g., ValidationResponse)
		 */
		ELEMENT, 
		/**
		 *  key to query results (to be treated by the caller), e.g., id-list, image, etc.
		 */
		DATA, 
		/**
		 *  key to an error string or Exception object (e.g., toString() will be used to get message)
		 */
		ERROR,
		/**
		 *  key to "records" count, e.g. items in the ID-list or no. of BioPAX elements
		 */
		COUNT, 
		/**
		 *  key to, e.g., lucene search statistics, etc.
		 */
		MISC; 
	}

	//--- Graph queries ---------------------------------------------------------------------------|

	/**
	 * Gets the BioPAX element by id,
	 * converts to the required output format (if possible), 
	 * and returns as map.
	 * @param format
	 * @param uris the list of URIs to fetch
	 * 
	 * @see ResultMapKey
	 * 
	 * @return
	 */
	Map<ResultMapKey, Object> fetch(OutputFormat format, String... uris);

	
	/**
	 * Full-text search for the BioPAX elements. 
	 * Returns the map result that contains the list of elements
	 * 
	 * @see ResultMapKey
	 * 
	 * @param queryStr
	 * @param page TODO
	 * @param biopaxClass
	 * @param filterValues can be arrays of URIs of organisms, data sources, etc.
	 * @return
	 * 
	 * @see PaxtoolsDAO#find(String, Class[], SearchFilter[]...)
	 */
	Map<ResultMapKey, Object> findElements(String queryStr, 
			int page, Class<? extends BioPAXElement> biopaxClass, SearchFilter... searchFilters);

	
	/**
	 * Full-text search for the BioPAX Entity class elements. 
	 * Returns the map result that contains the list of elements
	 * 
	 * @see PaxtoolsDAO#findEntities(String, int, Class, SearchFilter...)
	 * @see ResultMapKey
	 * 
	 * @param queryStr
	 * @param page TODO
	 * @param biopaxClass
	 * @param filterValues can be arrays of URIs of organisms, data sources, etc.
	 * @return
	 * 
	 * @see PaxtoolsDAO#find(String, Class[], SearchFilter[]...)
	 */
	Map<ResultMapKey, Object> findEntities(String queryStr, 
			int page, Class<? extends BioPAXElement> biopaxClass, SearchFilter... searchFilters);
	
	
	/**
	 * Generates the BioPAX validation report for the pathway data provider:
	 * - XML report will be associated with {@link ResultMapKey#DATA} key in the returned map;
	 * - {@link ValidatorResponse} bean will be associated with {@link ResultMapKey#ELEMENT} key.
	 * 
	 * @param metadataIdentifier
	 * @return
	 */
	Map<ResultMapKey, Object> getValidationReport(String metadataIdentifier);
	

	/**
	 * Runs a neighborhood query using the given parameters.
	 *
	 * @param format output format
	 * @param sources IDs of seed of neighborhood
	 * @param limit search limit (integer value)
	 * @param direction flag 
	 * @return the neighborhood
	 */
	Map<ResultMapKey, Object> getNeighborhood(OutputFormat format, 
			String[] sources, Integer limit, Direction direction);

	/**
	 * Runs a paths-between query from the given sources to the given targets.
	 *
	 * @param format output format
	 * @param sources IDs of source molecules
	 * @param targets IDs of target molecules
	 * @param limit search limit (integer value)
	 * @return paths between
	 */
	Map<ResultMapKey, Object> getPathsBetween(OutputFormat format, String[] sources, 
			String[] targets, Integer limit);

	/**
	 * Runs a common upstream or downstream query.
	 *
	 * @param format output format
	 * @param sources IDs of query seed
	 * @param limit search limit
	 * @param direction - can be {@link Direction#DOWNSTREAM} or {@link Direction#UPSTREAM}
	 * @return common stream
	 */
	Map<ResultMapKey, Object> getCommonStream(OutputFormat format, 
			String[] sources, Integer limit, Direction direction);

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
	
	
	/**
	 * Collects BioPAX property values at the end of the property path
	 * applied to each BioPAX object in the list (defined by URIs), 
	 * where applicable.
	 *  
	 * @see PathAccessor
	 * 
	 * @param propertyPath
	 * @param sourceUris
	 * @return
	 */
	Map<ResultMapKey, Object> traverse(String propertyPath, String... sourceUris);
	
	
	/**
	 * Gets top (root) pathways (URIs, names) in the current BioPAX model.
	 * 
	 * @return
	 */
	SearchResponseType getTopPathways();
}
