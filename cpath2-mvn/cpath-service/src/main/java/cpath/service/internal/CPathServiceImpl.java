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


package cpath.service.internal;

import java.io.*;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.Pathway;
import org.biopax.paxtools.model.level3.Provenance;
import org.biopax.paxtools.query.algorithm.Direction;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.googlecode.ehcache.annotations.Cacheable;

import cpath.dao.Analysis;
import cpath.dao.PaxtoolsDAO;
import cpath.service.analyses.CommonStreamAnalysis;
import cpath.service.analyses.NeighborhoodAnalysis;
import cpath.service.analyses.PathsBetweenAnalysis;
import cpath.service.analyses.PathsFromToAnalysis;
import cpath.service.jaxb.ErrorResponse;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.ServiceResponse;
import cpath.service.jaxb.TraverseResponse;
import cpath.service.CPathService;
import cpath.service.OutputFormat;
import cpath.service.OutputFormatConverter;
import static cpath.service.Status.*;

import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.Metadata;

/**
 * Service tier class - to uniformly access 
 * persisted BioPAX model (DAO) from console 
 * and web service controllers. This can be 
 * configured (instantiated) to access any cpath2
 * persistent BioPAX data storage (PaxtoolsDAO), 
 * i.e., - either the "main" cpath2 db or a Warehouse one (proteins or molecules)
 * 
 * @author rodche
 */
@Service
class CPathServiceImpl implements CPathService {
	private static final Log log = LogFactory.getLog(CPathServiceImpl.class);
	
	private PaxtoolsDAO mainDAO;
	
	private MetadataDAO metadataDAO;
	
	private SimpleIOHandler simpleIO;
	
	private Set<String> blacklist;
    
	//lazy initialize the following three fields on first request (prevents web server startup timeout)
  	//only includes pathway data providers configured/created from the cpath2 metadata configuration (not all provenances...)
  	private volatile SearchResponse dataSources; 
  	private volatile SearchResponse bioSources;
  	private volatile SearchResponse topPathways;
  	
  	private OutputFormatConverter formatConverter;

    // this is probably required for the ehcache to work
	public CPathServiceImpl() {
	}
	
    /**
     * Constructor.
     * @throws IOException 
     */
	public CPathServiceImpl(PaxtoolsDAO mainDAO, MetadataDAO metadataDAO, 
		OutputFormatConverter formatConverter, Resource blacklistResource) throws IOException 
	{
		this.mainDAO = mainDAO;
		this.metadataDAO = metadataDAO;
		this.simpleIO = new SimpleIOHandler(BioPAXLevel.L3);
		simpleIO.mergeDuplicates(true);
		this.formatConverter = formatConverter;
		
		if(blacklistResource != null && blacklistResource.exists()) {
			Scanner scanner = new Scanner(blacklistResource.getFile());
			blacklist = new HashSet<String>();
			while(scanner.hasNextLine())
				blacklist.add(scanner.nextLine().trim());
			scanner.close();
			if(log.isInfoEnabled())
				log.info("Successfully loaded " + blacklist.size()
				+ " URIs for a (graph queries) 'blacklist' resource: " 
				+ blacklistResource.getDescription());
		} else {
			log.warn("'blacklist' file is not used (" + 
				((blacklistResource == null) ? "not provided" 
					: blacklistResource.getDescription()) + ")");
		}
	}
	
	
	/*
	 * Interface methods
	 */	
	@Cacheable(cacheName = "findElementsCache")
	@Override
	public ServiceResponse search(String queryStr, 
			int page, Class<? extends BioPAXElement> biopaxClass, String[] dsources, String[] organisms) 
	{
		ServiceResponse serviceResponse;
		
		try {
			// do search
			SearchResponse hits = mainDAO.search(queryStr, page, biopaxClass, dsources, organisms);
			if(hits.isEmpty())
				serviceResponse = NO_RESULTS_FOUND.errorResponse("No hits");
			else {
				hits.setComment("Search '" + queryStr  + "' in " + 
					((biopaxClass == null) ? "all types" : biopaxClass.getSimpleName()) 
					+ "; ds: " + Arrays.toString(dsources)+ "; org.: " + Arrays.toString(organisms));
				serviceResponse = hits;
			}
			
		} catch (Exception e) {
			serviceResponse = INTERNAL_ERROR.errorResponse(e);
		}
		
		return serviceResponse;
	}
		

	/*
     * (non-Javadoc)
	 * @see cpath.service.CPathService#fetch(..)
	 */
	@Cacheable(cacheName = "elementByIdCache")
	@Override
	public ServiceResponse fetch(OutputFormat format, String... uris) {
		if (uris.length == 0)
			return NO_RESULTS_FOUND.errorResponse(
					"No URIs were specified for the query!");
		
		Model m = fetchBiopaxModel(uris);
		
		if(m != null && !m.getObjects().isEmpty())
			return formatConverter.convert(m, format, true);
		else
			return NO_RESULTS_FOUND.errorResponse(
				"No results for: " + Arrays.toString(uris));
    }


	/*
	 * (non-Javadoc)
	 * @see cpath.service.CPathService#fetchBiopaxModel(java.lang.String[])
	 */
	@Override
	public Model fetchBiopaxModel(String... uris) {
		// extract a sub-model
		return mainDAO.getValidSubModel(Arrays.asList(uris));
	}

	
	//--- Graph queries ---------------------------------------------------------------------------|

	/**
	 * Runs any analysis with the provided parameters.
	 *
	 * @param analysis the required analysis
	 * @param format the required output format
	 * @param params parameters for the analysis
	 * @return analysis result
	 */
	ServiceResponse runAnalysis(Analysis analysis, OutputFormat format, Object... params)
	{
		try {
			Model m = mainDAO.runAnalysis(analysis, params);
			if(m == null || m.getObjects().isEmpty())
				return NO_RESULTS_FOUND.errorResponse("Graph query " +
					"returned empty BioPAX model (" 
						+ analysis.getClass().getSimpleName() + ")");	
			else
				return formatConverter.convert(m, format, true);
		} catch (Exception e) {
			log.error("runAnalysis failed. ", e);
			return INTERNAL_ERROR.errorResponse(e);
		}
	}


	@Cacheable(cacheName = "getNeighborhoodCache")
	@Override
	public ServiceResponse getNeighborhood(OutputFormat format, String[] sources,
		Integer limit, Direction direction)
	{
		if(direction == null) {
			direction = Direction.BOTHSTREAM;	
		}
		Analysis analysis = new NeighborhoodAnalysis();
		return runAnalysis(analysis, format, sources, limit, direction, blacklist);
	}

	@Cacheable(cacheName = "getPathsBetweenCache")
	@Override
	public ServiceResponse getPathsBetween(OutputFormat format, String[] sources, Integer limit)
	{
		
		Analysis analysis = new PathsBetweenAnalysis();
		return runAnalysis(analysis, format, sources, limit, blacklist);
	}
	
	@Cacheable(cacheName = "getPathsFromToCache")
	@Override
	public ServiceResponse getPathsFromTo(OutputFormat format, String[] sources,
										  String[] targets, Integer limit)
	{
		Analysis analysis = new PathsFromToAnalysis();
		return runAnalysis(analysis, format, sources, targets, limit, blacklist);
	}
	

	@Cacheable(cacheName = "getCommonStreamCache")
	@Override
	public ServiceResponse getCommonStream(OutputFormat format, String[] sources,
		Integer limit, Direction direction)
	{
		if (direction == Direction.BOTHSTREAM) {
			return BAD_REQUEST.errorResponse(
				"Direction cannot be BOTHSTREAM for the COMMONSTREAM query!");
		} else if(direction == null) {
			direction = Direction.DOWNSTREAM;	
		}
			
		Analysis analysis = new CommonStreamAnalysis();
		return runAnalysis(analysis, format, sources, limit, direction, blacklist);
	}

	//---------------------------------------------------------------------------------------------|
	
	@Cacheable(cacheName = "traverseCache")
	@Override
	public ServiceResponse traverse(String propertyPath, String... sourceUris) {
		try {
			// get results from the DAO
			TraverseResponse results = mainDAO.traverse(propertyPath, sourceUris);
			return results;
		} catch (IllegalArgumentException e) {
			log.error("Failed to init path accessor: ", e);
			ErrorResponse err = BAD_REQUEST.errorResponse(e);
			err.setErrorDetails(e.getMessage()); //easy message (removes stack trace)
			return err;
		} catch (Exception e) {
			log.fatal("Failed. ", e);
			return INTERNAL_ERROR.errorResponse(e);
		}
	}

	
	/**
	 * "Top pathway" can mean different things...
	 * 
	 * 1) One may want simply collect pathways which are not 
	 * values of any BioPAX property (i.e., a "graph-theoretic" approach, 
	 * used by {@link ModelUtils#getRootElements(org.biopax.paxtools.model.Model, Class)} method) and by
	 * BioPAX normalizer, which (in the cPath2 "premerge" stage),
	 * for all Entities, generates relationship xrefs to their "parent" pathways.
	 * 
	 * 2) Another approach would be to check whether specific (inverse) 
	 * properties, such as controlledOf, pathwayComponentOf and stepProcessOf, are empty.
	 * 
	 * Here we follow the second method!
	 * 
	 * Note:
	 * This method generates the list of top pathways on the first call, 
	 * and then all next calls return the same list (much faster).
	 * (it uses lazy initialization, "double-check idiom", 
	 * to prevent possible server startup timeout)
	 * 
	 */
	@Override
	public SearchResponse topPathways() {
		SearchResponse result = topPathways;
		if(result == null) { //first check (no locking)
			synchronized (this) {
				result = topPathways; 
				if(result == null) { //second check (with locking)
					result = new SearchResponse();
					final List<SearchHit> hits = result.getSearchHit(); //empty
					int page = 0; // will use search pagination
					SearchResponse searchResponse = mainDAO.search("*", page, Pathway.class, null, null);
					while(!searchResponse.isEmpty()) {
						log.info("Retrieving top pathways search results, page #" + page);
						//remove pathways having 'pathway' index field not empty, 
						//i.e., keep only pathways where 'pathway' index field
						// is empty (no controlledOf and pathwayComponentOf values)
						for(SearchHit h : searchResponse.getSearchHit()) {
							if(h.getPathway().isEmpty())
								hits.add(h); //add to topPathways list
						}
						// go next page
						searchResponse = mainDAO.search("*", ++page, Pathway.class, null, null);
					}
					// final touches...
					result.setNumHits(hits.size());
					//TODO update the following comment if implementation has changed!
					result.setComment("Top Pathways (technically, each has empty index " +
							"field 'pathway'; that also means, they are neither components of " +
							"other pathways nor controlled of any process)");
					result.setMaxHitsPerPage(hits.size());
					result.setPageNo(0);
					
					topPathways = result;
				}
			}
		} 
		
		return result;
	}


	public Set<String> getBlacklist() {
		return blacklist;
	}
	public void setBlacklist(Set<String> blacklist) {
		this.blacklist = blacklist;
	}

	/**
	 * Note:
	 * This method generates the list of datasources on the first call, 
	 * and then all next calls return the same list (much faster).
	 * (it uses thread-safe lazy initialization, "double-check idiom", 
	 * to prevent possible server startup timeout)
	 */
	@Override
	public SearchResponse dataSources() {
		SearchResponse result = dataSources;
		if(result == null) {
			synchronized (this) {
				result = dataSources; //second check (with locking)
				if(result == null) {
					result = new SearchResponse();				
					// collect metadata identifiers of pathway data sources
					final Map<String, Metadata> metadataMap = new HashMap<String, Metadata>();
					for(Metadata met : metadataDAO.getAllMetadata())
						if(!met.getType().isWarehouseData())
							metadataMap.put(met.uri(), met);
					
					// find all Provenance instances and filter them out
					final List<SearchHit> hits = result.getSearchHit(); //empty
					int page = 0; // will use search pagination
					SearchResponse searchResponse;
					while(!(searchResponse = 
						mainDAO.search("*", page++, Provenance.class, null, null))
							.isEmpty())
					{
						for(SearchHit h : searchResponse.getSearchHit()) {
							if(metadataMap.containsKey(h.getUri())) {
								h.setExcerpt(metadataMap.get(h.getUri()).getDescription()); //a hack (to save comments)
								hits.add(h);
							}
						}
					
						if(searchResponse.getSearchHit().size() == searchResponse.getNumHits()) 
							break;
					}
					// final touches...
					result.setNumHits(hits.size());
					result.setMaxHitsPerPage(hits.size());
					result.setPageNo(0);
					
					dataSources = result;
				}
			}
		}
		
		return result;
	}

	
	/**
	 * Note:
	 * This method generates the list of organisms on the first call, 
	 * and then all next calls return the same list (much faster).
	 * (it uses lazy initialization, "double-check idiom", 
	 * to prevent possible server startup timeout)
	 */
	@Override
	public SearchResponse bioSources() {
		SearchResponse result = bioSources;
		if(result == null) {
			synchronized (this) {
				result = bioSources;
				if(result == null) { //second check (with locking)
					result = new SearchResponse();
					
					// find all Provenance instances and filter them out
					final List<SearchHit> hits = result.getSearchHit(); //empty
					int page = 0; // will use search pagination
					SearchResponse searchResponse;
					while(!(searchResponse = 
						mainDAO.search("*", page++, BioSource.class, null, null))
							.isEmpty()) 
					{
						hits.addAll(searchResponse.getSearchHit());
						// go next page
						if(searchResponse.getSearchHit().size() == searchResponse.getNumHits())
							break;
					}
					// final touches...
					result.setNumHits(hits.size());
					result.setMaxHitsPerPage(hits.size());
					result.setPageNo(0);
					
					bioSources = result;
				}
			}
		}
			
		return result;
	}

}
