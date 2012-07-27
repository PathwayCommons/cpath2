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
import org.biopax.paxtools.io.gsea.GSEAConverter;
import org.biopax.paxtools.io.sif.SimpleInteractionConverter;
import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.Pathway;
import org.biopax.paxtools.model.level3.Provenance;
import org.biopax.paxtools.query.algorithm.Direction;
import org.biopax.validator.result.Validation;
import org.biopax.validator.result.ValidatorResponse;
import org.biopax.validator.utils.BiopaxValidatorUtils;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.googlecode.ehcache.annotations.Cacheable;

import cpath.config.CPathSettings;
import cpath.dao.Analysis;
import cpath.dao.PaxtoolsDAO;
import cpath.service.analyses.CommonStreamAnalysis;
import cpath.service.analyses.NeighborhoodAnalysis;
import cpath.service.analyses.PathsBetweenAnalysis;
import cpath.service.analyses.PathsFromToAnalysis;
import cpath.service.jaxb.DataResponse;
import cpath.service.jaxb.ErrorResponse;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.ServiceResponse;
import cpath.service.jaxb.TraverseResponse;
import cpath.service.CPathService;
import cpath.service.OutputFormat;
import static cpath.service.Status.*;

import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;

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

    // this is probably required for the ehcache to work
	public CPathServiceImpl() {
	}
	
    /**
     * Constructor.
     */
	CPathServiceImpl(PaxtoolsDAO mainDAO, MetadataDAO metadataDAO) 
	{
		this.mainDAO = mainDAO;
		this.metadataDAO = metadataDAO;
		this.simpleIO = new SimpleIOHandler(BioPAXLevel.L3);
		simpleIO.mergeDuplicates(true);
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
        ServiceResponse res = fetchBiopaxModel(uris);
        if(!(res instanceof ErrorResponse)) 
        	convert(res, format);
        return res;
    }

	/*
     * (non-Javadoc)
	 * @see cpath.service.CPathService#convert(..)
	 */
    @Override
    public ServiceResponse convert(String biopax, OutputFormat format) {
        // put incoming biopax into map
    	DataResponse dataResponse = new DataResponse();
        Model model = simpleIO.convertFromOWL(new ByteArrayInputStream(biopax.getBytes()));
        if(!model.getObjects().isEmpty()) {
            dataResponse.setData(model);
            convert(dataResponse, format);
            return dataResponse;
        } else {
        	return NO_RESULTS_FOUND.errorResponse("Empty BioPAX Model!");
        }
	}

    
    /*
     * Function used by both convert(String, OutputFormat)
     * and fetch(OutputFormat, String... uris).
     */
    ServiceResponse convert(ServiceResponse serviceResponse, OutputFormat format) 
    {
		if(serviceResponse instanceof ErrorResponse) {
			return serviceResponse; //unchanged
		} else if(serviceResponse.isEmpty()) {
			return NO_RESULTS_FOUND.errorResponse("Empty BioPAX Model returned!");
		}

		// otherwise, do convert (it's a DataResponse)
		DataResponse dataResponse = (DataResponse) serviceResponse;
    	try {
			switch (format) {
			case BINARY_SIF:
				convertToBinarySIF(dataResponse, false);
				break;
			case EXTENDED_BINARY_SIF:
				convertToBinarySIF(dataResponse, true);
				break;
			case GSEA:
				convertToGSEA(dataResponse, "uniprot");
				break;
            case BIOPAX: // default handler
            	convertToBiopaxOWL(dataResponse);
			default:
                // do nothing
			}
			
			return serviceResponse;
		}
        catch (Exception e) {
        	return INTERNAL_ERROR.errorResponse(e);
		}
    }
	
    /**
     * Replace the Model object contained in the service bean 
     * with its OWL serialization (text data)
     * 
     * @param serviceResponse
     */
	void convertToBiopaxOWL(DataResponse serviceResponse) {
		Model m = (Model) serviceResponse.getData();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		simpleIO.convertToOWL(m, baos);
		serviceResponse.setData(baos.toString());
	}


	/*
	 * (non-Javadoc)
	 * @see cpath.service.CPathService#fetchBiopaxModel(java.lang.String[])
	 */
	@Override
	public ServiceResponse fetchBiopaxModel(String... uris) {
		if (uris.length > 0) {	
			// extract a sub-model
			Model m = mainDAO.getValidSubModel(Arrays.asList(uris));
			if(m != null && !m.getObjects().isEmpty()) {
				DataResponse dataResponse = new DataResponse();
				dataResponse.setData(m);
				return dataResponse;
			} else {
				return NO_RESULTS_FOUND.errorResponse(
					"No results for: " + Arrays.toString(uris));
			}
		} else {
			return NO_RESULTS_FOUND.errorResponse(
				"No URIs were specified for the query!");
		}
	}	

	
	/**
	 * Converts service results that contain 
	 * a not empty BioPAX Model to GSEA format.
	 * 
     * @param resp ServiceResponse
	 * @param outputIdType output identifiers type (db name)
	 * @return
	 * @throws IOException 
	 */
	void convertToGSEA(DataResponse resp, 
		String outputIdType) throws IOException 
	{	
		// convert, replace DATA
		Model m = (Model) resp.getData();
		GSEAConverter gseaConverter = new GSEAConverter(outputIdType, true);
		OutputStream stream = new ByteArrayOutputStream();
		gseaConverter.writeToGSEA(m, stream);
		resp.setData(stream.toString());
	}

	/**
	 * Converts a not empty BioPAX Model (contained in the service bean) 
	 * to SIF data format.
	 * 
	 * TODO 'rules' parameter is currently ignored (requires conversion 
	 * from strings to the rules, e.g., using enum. BinaryInteractionRule from cpath-web-service)
	 * 
     * @param resp ServiceResponse
     * @param extended if true, call SIFNX else SIF
	 * @param rules (optional) the names of SIF rules to apply
	 * @return
	 * @throws IOException 
	 */
	void convertToBinarySIF(DataResponse resp, 
		boolean extended, String... rules) throws IOException 
	{
		// convert, replace DATA value in the map to return
		// TODO match 'rules' parameter to rule types (currently, it uses all)
		Model m = (Model) resp.getData();
		SimpleInteractionConverter sic = getSimpleInteractionConverter(m);
		OutputStream edgeStream = new ByteArrayOutputStream();
		if (extended) {
			OutputStream nodeStream = new ByteArrayOutputStream();
			sic.writeInteractionsInSIFNX(
					m, edgeStream, nodeStream,
					Arrays.asList("EntityReference/displayName", "EntityReference/xref:UnificationXref", "EntityReference/xref:RelationshipXref"),
					Arrays.asList("Interaction/dataSource/displayName", "Interaction/xref:PublicationXref"), 
					true);
			String edgeColumns = "PARTICIPANT_A\tINTERACTION_TYPE\tPARTICIPANT_B\tINTERACTION_DATA_SOURCE\tINTERACTION_PUBMED_ID\n";
			String nodeColumns = "PARTICIPANT\tPARTICIPANT_TYPE\tPARTICIPANT_NAME\tUNIFICATION_XREF\tRELATIONSHIP_XREF\n";
			resp.setData(edgeColumns + removeDuplicateBinaryInteractions(edgeStream) + "\n" + nodeColumns + nodeStream.toString());
		} else {
			sic.writeInteractionsInSIF(m, edgeStream);
			resp.setData(removeDuplicateBinaryInteractions(edgeStream));
		}

	}

	/*
     * (non-Javadoc)
	 * @see cpath.service.CPathService#getValidationReport(...)
	 */
	@Override
	public ServiceResponse getValidationReport(String metadataIdentifier) {
		// get validationResults from PathwayData beans
		Collection<PathwayData> pathwayDataCollection = metadataDAO
				.getPathwayDataByIdentifier(metadataIdentifier);
		if (!pathwayDataCollection.isEmpty()) {
			// a new container to collect separately stored file validations
			ValidatorResponse response = new ValidatorResponse();
			for (PathwayData pathwayData : pathwayDataCollection) {				
				try {
					byte[] xmlResult = pathwayData.getValidationResults();
					// unmarshal and add
					ValidatorResponse resp = (ValidatorResponse) BiopaxValidatorUtils.getUnmarshaller()
						.unmarshal(new BufferedInputStream(new ByteArrayInputStream(xmlResult)));
					assert resp.getValidationResult().size() == 1; // current design (of the premerge pipeline)
					Validation validation = resp.getValidationResult().get(0); 
					if(validation != null)
						response.getValidationResult().add(validation);
				} catch (Exception e) {
                    log.error(e);
					return INTERNAL_ERROR.errorResponse(e);
				}
			}
			
			DataResponse toReturn = new DataResponse();
			toReturn.setData(response);
			return toReturn;
		} else {
			return NO_RESULTS_FOUND.errorResponse(
				"No pathway data found by " + metadataIdentifier);
		}
	}

	
	@Override
	public ServiceResponse getValidationReport(Integer pathwayDataPk) {
		PathwayData pathwayData = metadataDAO.getPathwayData(pathwayDataPk);
		if(pathwayData != null) {
			try {
				byte[] xmlResult = pathwayData.getValidationResults();	
				ValidatorResponse resp = (ValidatorResponse) BiopaxValidatorUtils
					.getUnmarshaller().unmarshal(new BufferedInputStream(new ByteArrayInputStream(xmlResult)));
				DataResponse ret = new DataResponse();
				ret.setData(resp);
				return ret;
			} catch (Throwable e) {
                log.error(e);
				return INTERNAL_ERROR.errorResponse(e);
			}
		} else {
			return NO_RESULTS_FOUND.errorResponse(
				"No pathway data exists with pathway_id:" + pathwayDataPk);
		}
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
			
			DataResponse resp = new DataResponse();
			resp.setData(m);
			convert(resp, format);
			
			return resp;
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
		return runAnalysis(analysis, format, sources, targets, limit);
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

    /**
     * Given paxtools model, returns as string
     *
     * @param model Model
     * @return String
     */
	String exportToOWL(Model model) {

		if (model == null) {
			return null;
        }
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		simpleIO.convertToOWL(model, out);

		return out.toString();
	}

    /**
     * Returns proper simple interaction converter based on level
     * of model. We don't have to be l2 compatible, but it doesn't hurt.
     *
     * @param model Model
     * @return SimpleInteractionConverter
     */
    SimpleInteractionConverter getSimpleInteractionConverter(Model model) {
        // Currently we don't have any options
        // But I will just leave it here for the sake
        // of API interface and future use
        Map options = new HashMap();

        if (model.getLevel() == BioPAXLevel.L2) {

            return new SimpleInteractionConverter(
                options,
                blacklist,
				new org.biopax.paxtools.io.sif.level2.ComponentRule(),
				new org.biopax.paxtools.io.sif.level2.ConsecutiveCatalysisRule(),
				new org.biopax.paxtools.io.sif.level2.ControlRule(),
				new org.biopax.paxtools.io.sif.level2.ControlsTogetherRule(),
				new org.biopax.paxtools.io.sif.level2.ParticipatesRule());
        }
        else if (model.getLevel() == BioPAXLevel.L3) {
            return new SimpleInteractionConverter(
                options,
                blacklist,
				new org.biopax.paxtools.io.sif.level3.ComponentRule(),
				new org.biopax.paxtools.io.sif.level3.ConsecutiveCatalysisRule(),
				new org.biopax.paxtools.io.sif.level3.ControlRule(),
				new org.biopax.paxtools.io.sif.level3.ControlsTogetherRule(),
				new org.biopax.paxtools.io.sif.level3.ParticipatesRule()
				);
        }

        // should not make it here
        return null;
    }
    
	/**
	 * Remove duplicate binary interactions from SIF/SIFNX converter output
	 *
	 * @param edgeStream OutputStream from converter
	 * @return String
	 */
	private String removeDuplicateBinaryInteractions(OutputStream edgeStream) {

		StringBuffer toReturn = new StringBuffer();
		HashSet<String> interactions = new HashSet<String>();
		for (String interaction : edgeStream.toString().split("\n")) {
			interactions.add(interaction);
		}
		Iterator iterator = interactions.iterator();
		while (iterator.hasNext()) {
			toReturn.append(iterator.next() + "\n");
		}
		
		return toReturn.toString();
	}

	
	@Cacheable(cacheName = "traverseCache")
	@Override
	public ServiceResponse traverse(String propertyPath, String... sourceUris) {
		try {
			// get results from the DAO
			TraverseResponse results = mainDAO.traverse(propertyPath, sourceUris);
			return results;
		} catch (Exception e) {
			log.error("Failed. ", e);
			return INTERNAL_ERROR.errorResponse(e);
		}
	}

	
	/**
	 * "Top pathway" can mean different things...
	 * 
	 * 1) One may want simply collect pathways which are not 
	 * values of any BioPAX property (i.e., a "graph-theoretic" approach, 
	 * used by {@link ModelUtils#getRootElements(Class)} method) and by 
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
						//i.e., keep only pathways where 'pathway' index field is empty (no controlledOf and pathwayComponentOf values)
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

	/**
	 * A special setter for the blacklist property
	 * (to be used by Spring)
	 * 
	 * @param blacklistResource
	 * @throws IOException
	 */
    public void setBlacklistLocation(Resource blacklistResource) throws IOException {
		if(blacklistResource.exists()) {
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
			log.warn(blacklistResource.getDescription() + 
				" does not exists (a 'blacklist' file)!");
		}
    }

    // blacklist property standard getter/setter pair
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
	 * (it uses lazy initialization, "double-check idiom", 
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
					final Set<String> metadataIds = new HashSet<String>();
					for(Metadata met : metadataDAO.getAllMetadata())
						if(!met.getType().isWarehouseData())
							metadataIds.add(CPathSettings.generateBiopaxURI(met.getName(), Provenance.class));
					
					// find all Provenance instances and filter them out
					final List<SearchHit> hits = result.getSearchHit(); //empty
					int page = 0; // will use search pagination
					SearchResponse searchResponse;
					while(!(searchResponse = 
						mainDAO.search("*", page++, Provenance.class, null, null))
							.isEmpty())
					{
						//remove pathways having 'pathway' index field not empty, 
						//i.e., keep only pathways where 'pathway' index field is empty (no controlledOf and pathwayComponentOf values)
						for(SearchHit h : searchResponse.getSearchHit()) {
							if(metadataIds.contains(h.getUri()))
								hits.add(h);
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

	
	@Override
	public ServiceResponse getPathwayDataInfo(String metadataIdentifier) {
		Collection<PathwayData> pathwayData = metadataDAO.getPathwayDataByIdentifier(metadataIdentifier);
		Map<Integer, String> map = new TreeMap<Integer, String>();
		for(PathwayData pd : pathwayData)
			map.put(pd.getId(), pd.getFilename() 
				+ " (" + pd.getIdentifier() 
				+ "; version: " + pd.getVersion() 
				+ "; passed: " + pd.getValid() + ")");
		
		DataResponse ret = new DataResponse();
		ret.setData(map);
		return ret;
	}
		
}
