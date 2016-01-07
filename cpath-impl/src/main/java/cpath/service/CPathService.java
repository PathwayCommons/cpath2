package cpath.service;


import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import cpath.config.CPathSettings;
import org.biopax.paxtools.controller.PathAccessor;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.query.algorithm.Direction;
import org.biopax.validator.api.beans.ValidatorResponse;

import cpath.jpa.Content;
import cpath.jpa.LogEntitiesRepository;
import cpath.jpa.LogEntity;
import cpath.jpa.LogEvent;
import cpath.jpa.Mapping;
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
	 * @return search/error response
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
	ServiceResponse getCommonStream(OutputFormat format, String[] sources, Integer limit, Direction direction,
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
	ServiceResponse topPathways(String[] organisms, String[] datasources);

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
     * Test if this or equivalent record exists
     * in the id-mapping db and if not, saves the new one.
     * 
     * @param mapping
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
	 * Creates or updates log db entries for the data file
	 * downloaded by a user.
	 * 
	 * @param fileName
	 * @param ipAddr
	 */
	void log(String fileName, String ipAddr);
	
	/**
	 * Increases the number (counter) 
	 * of user's requests of some 
	 * sort and location for given date.
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
	 * Updates (overwrites) or saves the number 
	 * of user's requests of some 
	 * sort and location for given date.
	 * 
	 * @param date
	 * @param event
	 * @param ipAddr
	 * @return
	 */
	LogEntity update(String date, LogEvent event, String ipAddr, Long newCount);

	/**
	 * Creates a list of new log events to update counts for -
	 * name, format, provider - from the data archive/file name
	 * (in the batch downloads or another directory),
	 * but does not save these events in the log database yet.
	 *
	 * @param filename see {@link CPathSettings#biopaxExportFileName(String)} for how it's created.
	 * @return
	 */
	Set<LogEvent> logEventsFromFilename(String filename);
	
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
    
    LogEntitiesRepository log();

    /**
     * Loads or re-loads the main BioPAX Model 
     * and blacklist from archive.
     */
	void init();
	
	/**
	 * Checks whether the service object is fully initialized, i.e,
	 * if all data repositories, in-memory BioPAX model and search
	 * engine are ready to go.
	 * 
	 * @return true if it's fully initialized
	 */
	boolean ready();


	/**
	 * Creates:
	 * <ul>
	 * <li>new BioPAX full-text index;</li>
	 * <li>the blacklist of ubiquitous small molecules;</li>
	 * <li>updates counts of different BioPAX entities per data source</li>
	 * </ul>
	 */
	void index() throws IOException;
}
