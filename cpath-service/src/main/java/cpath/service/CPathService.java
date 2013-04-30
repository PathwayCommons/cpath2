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


import org.biopax.paxtools.controller.PathAccessor;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.query.algorithm.Direction;

import cpath.dao.PaxtoolsDAO;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.ServiceResponse;


/**
 * CPath^2 Service is an adapter between DAO and web controllers. 
 * Can be used in a console application or integration tests 
 * (web container is not required.)
 * 
 * This interface defines several middle-tier data access and analysis methods 
 * that accept valid parameters, handle exceptions, and return results packed 
 * in a ServiceResponse bean. This creates an additional "elastic" layer between 
 * the public (web, console) api and persistence methods, allowing to modify 
 * either its implementation or the DAO implementation 
 * without breaking end user's services (backward compatibility).
 * 
 * 
 * @author rodche
 *
 */
public interface CPathService {

	//--- Graph queries ---------------------------------------------------------------------------|

	/**
	 * Gets the BioPAX element by id,
	 * converts to the required output format (if possible), 
	 * and returns as map.
	 * @param format
	 * @param uris the list of URIs to fetch
	 * 
	 * 
	 * 
	 * @return
	 */
	ServiceResponse fetch(OutputFormat format, String... uris);

	
	/**
	 * Full-text search for the BioPAX elements. 
	 * Returns the map result that contains the list of elements
	 * 
	 * 
	 * 
	 * @param queryStr
	 * @param page search results page no.
	 * @param biopaxClass
	 * @param dsources URIs of data sources
	 * @param organisms URIs of organisms
	 * @return
	 * 
	 * @see PaxtoolsDAO#search(String, Class[], String[], String[])
	 */
	ServiceResponse search(String queryStr, 
			int page, Class<? extends BioPAXElement> biopaxClass, String[] dsources, String[] organisms);

	
	/**
	 * Runs a neighborhood query using the given parameters.
	 *
	 * @param format output format
	 * @param sources IDs of seed of neighborhood
	 * @param limit search limit (integer value)
	 * @param direction flag 
	 * @param organisms optional filter
	 * @param datasources optional filter
	 * @return the neighborhood
	 */
	ServiceResponse getNeighborhood(OutputFormat format, 
		String[] sources, Integer limit, Direction direction,
			String[] organisms, String[] datasources);

	/**
	 * Runs a paths-between query for the given sources.
	 *
	 * @param format output format
	 * @param sources IDs of source molecules
	 * @param limit search limit (integer value)
	 * @param organisms optional filter
	 * @param datasources optional filter
	 * @return paths between
	 */
	ServiceResponse getPathsBetween(OutputFormat format, String[] sources, 
		Integer limit, String[] organisms, String[] datasources);

	/**
	 * Runs a POI query from the given sources to the given targets.
	 *
	 * @param format output format
	 * @param sources IDs of source molecules
	 * @param targets IDs of target molecules
	 * @param limit search limit (integer value)
	 * @param organisms optional filter
	 * @param datasources optional filter
	 * @return paths between
	 */
	ServiceResponse getPathsFromTo(OutputFormat format, String[] sources,
		String[] targets, Integer limit, String[] organisms, String[] datasources);
	
	
	/**
	 * Runs a common upstream or downstream query.
	 *
	 * @param format output format
	 * @param sources IDs of query seed
	 * @param limit search limit
	 * @param direction - can be {@link Direction#DOWNSTREAM} or {@link Direction#UPSTREAM}
	 * @param organisms optional filter
	 * @param datasources optional filter
	 * @return common stream
	 */
	ServiceResponse getCommonStream(OutputFormat format, 
		String[] sources, Integer limit, Direction direction, 
			String[] organisms, String[] datasources);

	//---------------------------------------------------------------------------------------------|

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
	ServiceResponse traverse(String propertyPath, String... sourceUris);
	
	
	/**
	 * Gets top (root) pathways (URIs, names) in the current BioPAX model.
	 * 
	 * @return
	 */
	SearchResponse topPathways();

	
	/** 
	 * Creates a stand-alone sub-model from the BioPAX elements (matched by URIs).
	 * 
	 * @param uris
	 * @return
	 */
	Model fetchBiopaxModel(String... uris);
	
}
