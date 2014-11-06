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


import java.util.Collection;
import java.util.Set;

import org.biopax.paxtools.controller.PathAccessor;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.query.algorithm.Direction;
import org.biopax.validator.api.beans.ValidatorResponse;

import cpath.dao.PaxtoolsDAO;
import cpath.jpa.Content;
import cpath.jpa.LogEntitiesRepository;
import cpath.jpa.LogEntity;
import cpath.jpa.LogEvent;
import cpath.jpa.Mapping;
import cpath.jpa.MappingsRepository;
import cpath.jpa.Metadata;
import cpath.jpa.MetadataRepository;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.ServiceResponse;


/**
 * CPath^2 Service is an adapter between DAO and web controllers. 
 * Can be used in a console application or integration tests 
 * (web container is not required.)
 * 
 * This interface defines several middle-tier data access and analysis methods 
 * that accept valid parameters, handle exceptions, and return results packed 
 * in a ServiceResponse bean.
 * 
 * @author rodche
 *
 */
public interface CPathService {

	/**
	 * Retrieves the BioPAX element(s) by URI or identifier (e.g., gene symbol)
	 * - a complete BioPAX sub-model with all available child elements and 
	 * properties - and then converts it to the specified output format 
	 * (if applicable), such as BioPAX (RDF/XML), SIF, GSEA (.gmt).
	 * 
	 * @param format
	 * @param uris the list of URIs to fetch
	 * @return
	 */
	ServiceResponse fetch(OutputFormat format, String... uris);

	
	/**
	 * Full-text search for the BioPAX elements. 
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
	 * Runs a neighborhood query using the given parameters
	 * (returns a sub-model in the specified format, 
	 * wrapped as service object). 
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
	 * Runs a paths-between query for the given sources	
	 * (returns a sub-model in the specified format, 
	 * wrapped as service object). 
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
	 * Runs a POI query from the given sources to the given targets
	 * (returns a sub-model in the specified format, wrapped as service object). 
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
	 * Runs a common upstream or downstream query
	 * (returns a sub-model in the specified format, 
	 * wrapped as service object). 
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
	 * @param organisms filter values (URIs, names, or taxonomy IDs)
	 * @param datasources filter values (URIs, names)
	 * @return
	 */
	SearchResponse topPathways(String[] organisms, String[] datasources);
	
	
	/**
	 * Guess the identifier type (chemical vs gene/protein) 
	 * and returns other Is it maps to. This method can be wrong
	 * about mapping, it's weak, designed to use primarily in
	 * BioPAX graph queries and not for data integration/merging.
	 * 
	 * @param identifier
	 * @return
	 */
	Set<String> map(String identifier);
	
	
	/**
     * Maps an identifier to primary ID(s) of a given type.
     * 
     * Normally, the result set contains only one ID.
     * If the result contains more than one value, which does not
     * necessarily an error, then it's up to other methods to decide 
     * how to proceed; e.g., one should not probably merge different 
     * data objects if the mapping is known to be umbiguous,
     * but it's usually ok to generate relationship xrefs 
     * or use the resulting IDs in a BioPAX graph query.
     * 
     * @param fromDb data collection name or null (to use all source ID types)
     * @param fromId the source ID
     * @param toDb standard (MIRIAM) preferred name of the target ID type (e.g., 'UniProt')
     * 
     * @return a set of primary IDs of the type; normally one or none elements
     */
    Set<String> map(String fromDb, String fromId, String toDb);
        
    /**
     * Test if this or equivalent record exists
     * in the id-mapping db and if not, saves the new one.
     * 
     * @param mapping
     * @return
     */
    void saveIfUnique(Mapping mapping);

	/**
	 * Saves and counts a series of data access events 
	 * (usually associated with the same web request) 
	 * to the log db.
	 * 
	 * @param events
	 * @param ipAddr
	 */
	void log(Collection<LogEvent> events, String ipAddr);
    
	/**
	 * Increases the number (counter) 
	 * of today user's requests of some 
	 * sort and location.
	 * 
	 * Right now, only country code there
	 * matters for location matching 
	 * (city and region are ignored).
	 * 
	 * @param date
	 * @param event
	 * @param ipAddr
	 * @return
	 */
	LogEntity count(String date, LogEvent event, String ipAddr);	
	
    /**
     * Persists or updates the given metadata object.
     *
     * @param metadata Metadata
     * @return
     */
	Metadata save(Metadata metadata);
	
	/**
	 * Deletes the provider Metadata and data sub-directory.
	 * 
	 * @param metadata
	 */
	void delete(Metadata metadata);
	
	/**
	 * Imports Metadata from a config file.
	 * 
	 * @param location
	 */
	void addOrUpdateMetadata(String location);
	
    /**
	 * Removes from the system all entries and 
	 * previously converted / premerged / validated 
	 * files accociated with this data provider.
	 * 
	 * @param metadata
	 * @return updated/saved object
	 */
	Metadata init(Metadata metadata);		
    
	/**
	 * Generates the BioPAX validation report for a pathway data file.
	 * 
	 * @param provider data source (Metadata) identifier, not null
	 * @param file - base filename as in {@link Content}, or null (for all files)
	 * @return
	 */
	ValidatorResponse validationReport(String provider, String file);
	
	
	//spring-data-jpa repositories
	
	MappingsRepository mapping();
    
    MetadataRepository metadata();
    
    PaxtoolsDAO biopax();
    
    LogEntitiesRepository log();

    /**
     * Loads or re-loads the main BioPAX Model 
     * and blacklist from archive.
     */
	void init();
}
