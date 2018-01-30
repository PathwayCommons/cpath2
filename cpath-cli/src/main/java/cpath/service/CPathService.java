package cpath.service;


import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.biopax.paxtools.controller.PathAccessor;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.pattern.util.Blacklist;
import org.biopax.paxtools.query.algorithm.Direction;

import cpath.jpa.MappingsRepository;
import cpath.jpa.Metadata;
import cpath.jpa.MetadataRepository;
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
 */
public interface CPathService {
	
	Model getModel();
	
	void setModel(Model paxtoolsModel);
	
	Searcher getSearcher();
	
	void setSearcher(Searcher searcher);

	Blacklist getBlacklist();

	/**
	 * Retrieves the BioPAX element(s) by URI or identifier (e.g., gene symbol)
	 * - a complete BioPAX sub-model with all available child elements and 
	 * properties - and then converts it to the specified output format 
	 * (if applicable), such as BioPAX (RDF/XML), SIF, GSEA (.gmt).
	 * @param format output format type
	 * @param formatOptions format parameters
	 * @param subPathways optional, include/skip (default) sub-pathways
	 * @param uris the list of URIs to fetch  @return
	 */
	ServiceResponse fetch(OutputFormat format, Map<String, String> formatOptions,
						  boolean subPathways, String... uris);
	
	/**
	 * Full-text search for the BioPAX elements. 
	 * 
	 * @param queryStr
	 * @param page search results page no.
	 * @param biopaxClass
	 * @param dsources URIs of data sources
	 * @param organisms URIs of organisms
	 * @return search/error response
	 */
	ServiceResponse search(String queryStr,
			int page, Class<? extends BioPAXElement> biopaxClass, String[] dsources, String[] organisms);
	
	/**
	 * Runs a neighborhood query using the given parameters
	 * (returns a sub-model in the specified format, 
	 * wrapped as service object). 
	 *  @param format output format type
	 * @param formatOptions format options
	 * @param sources IDs of seed of neighborhood
	 * @param limit search limit (integer value)
	 * @param direction flag
	 * @param organisms optional filter
	 * @param datasources optional filter
	 * @param subPathways  optional, include/skip sub-pathways; it does not affect the graph search algorithm,
	 */
	ServiceResponse getNeighborhood(OutputFormat format,
									Map<String, String> formatOptions,
									String[] sources, Integer limit, Direction direction,
									String[] organisms, String[] datasources, boolean subPathways);

	/**
	 * Runs a paths-between query for the given sources	
	 * (returns a sub-model in the specified format, 
	 * wrapped as service object). 
	 *  @param format output format type
	 * @param formatOptions format options
	 * @param sources IDs of source molecules
	 * @param limit search limit (integer value)
	 * @param organisms optional filter
	 * @param datasources optional filter
	 * @param subPathways optional, include/skip sub-pathways; it does not affect the graph search algorithm,
	 */
	ServiceResponse getPathsBetween(OutputFormat format, Map<String, String> formatOptions,
									String[] sources, Integer limit, String[] organisms,
									String[] datasources, boolean subPathways);

	/**
	 * Runs a POI query from the given sources to the given targets
	 * (returns a sub-model in the specified format, wrapped as service object). 
	 *  @param format output format
	 * @param formatOptions format options
	 * @param sources IDs of source molecules
	 * @param targets IDs of target molecules
	 * @param limit search limit (integer value)
	 * @param organisms optional filter
	 * @param datasources optional filter
	 * @param subPathways optional, include/skip sub-pathways; it does not affect the graph search algorithm,
	 */
	ServiceResponse getPathsFromTo(OutputFormat format, Map<String, String> formatOptions,
								   String[] sources, String[] targets, Integer limit, String[] organisms,
								   String[] datasources, boolean subPathways);

	/**
	 * Runs a common upstream or downstream query
	 * (returns a sub-model in the specified format, 
	 * wrapped as service object). 
	 *  @param format output format
	 * @param formatOptions format options
	 * @param sources IDs of query seed
	 * @param limit search limit
	 * @param direction - can be {@link Direction#DOWNSTREAM} or {@link Direction#UPSTREAM}
	 * @param organisms optional filter
	 * @param datasources optional filter
	 * @param subPathways optional, include/skip sub-pathways; it does not affect the graph search algorithm,
	 */
	ServiceResponse getCommonStream(OutputFormat format, Map<String, String> formatOptions,
									String[] sources, Integer limit, Direction direction,
									String[] organisms, String[] datasources, boolean subPathways);

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
	 *
	 * @param q query string (keywords or Lucene syntax query string)
	 * @param organisms filter values (URIs, names, or taxonomy IDs)
	 * @param datasources filter values (URIs, names)
	 * @return
	 */
	ServiceResponse topPathways(String q, String[] organisms, String[] datasources);
	
	/**
	 * Maps an identifier to primary ID(s) of a given type.
	 * Auto-detects the source ID type or tries all types.
	 * The result set may contain more than one primary ID.
	 *
	 * @param fromId the source ID
	 * @param toDb standard (MIRIAM) preferred name of the target ID type (e.g., 'UniProt')
	 * @return a set of primary IDs of the type; normally one or none elements
	 */
	Set<String> map(String fromId, String toDb);

	/**
	 * Maps multiple identifiers to primary IDs of given type.
	 * Auto-detects the source ID type or tries all types.
	 * The result set may contain more than one primary ID.
	 *
	 * @param fromIds the source IDs
	 * @param toDb standard (MIRIAM) preferred name of the target ID type (e.g., 'UniProt')
	 * @return a set of primary IDs of the type; normally one or none elements
	 */
	Set<String> map(Collection<String> fromIds, String toDb);

	/**
	 * Record web service and data access events.
	 *  @param ip
     * @param category
     * @param label
     * @param action
     * @param client
     */
	void track(String ip, String category, String label, String action, String client);

	
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
	
	//spring-data-jpa repositories
	
	MappingsRepository mapping();
    
    MetadataRepository metadata();

    void init(); //only in production - to load the main biopax model from file

	/**
	 * Creates:
	 * <ul>
	 * <li>new BioPAX full-text index;</li>
	 * <li>the blacklist of ubiquitous small molecules;</li>
	 * <li>updates counts of different BioPAX entities per data source</li>
	 * </ul>
	 */
	void index() throws IOException;

	/**
	 * Clears the metadata object and the db record,
	 * and also drops/creates the data directory.
	 *
	 * @param metadata
	 * @return
     */
	Metadata clear(Metadata metadata);
}
