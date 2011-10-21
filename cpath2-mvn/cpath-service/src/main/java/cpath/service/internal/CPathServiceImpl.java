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

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.io.gsea.GSEAConverter;
import org.biopax.paxtools.io.sif.SimpleInteractionConverter;
import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.query.algorithm.Direction;
import org.biopax.validator.result.Validation;
import org.biopax.validator.result.ValidatorResponse;
import org.biopax.validator.utils.BiopaxValidatorUtils;
import org.springframework.stereotype.Service;

import com.googlecode.ehcache.annotations.Cacheable;

import cpath.dao.Analysis;
import cpath.dao.PaxtoolsDAO;
import cpath.dao.filters.SearchFilter;
import cpath.service.analyses.CommonStreamAnalysis;
import cpath.service.analyses.NeighborhoodAnalysis;
import cpath.service.analyses.PathsBetweenAnalysis;
import cpath.service.analyses.PathsOfInterestAnalysis;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.ServiceResponse;
import cpath.service.jaxb.TraverseResponse;
import cpath.service.CPathService;
import cpath.service.OutputFormat;
import cpath.service.Status;

import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.PathwayData;

/**
 * Service tier class - to uniformly access 
 * persisted BioPAX model (DAO) from console and webservice 
 * 
 * @author rodche
 */
@Service
public class CPathServiceImpl implements CPathService {
	private static final Log log = LogFactory.getLog(CPathServiceImpl.class);
	
	@NotNull
	private PaxtoolsDAO mainDAO;
	
	@NotNull
	private MetadataDAO metadataDAO;

	private SimpleIOHandler simpleIO;

	private static SearchResponse topPathways;
	
	// this is probably required for the echcache to work
	public CPathServiceImpl() {
	}
	
    /**
     * Constructor.
     */
	public CPathServiceImpl(PaxtoolsDAO mainDAO, MetadataDAO metadataDAO) 
	{
		this.mainDAO = mainDAO;
		this.metadataDAO = metadataDAO;
		this.simpleIO = new SimpleIOHandler(BioPAXLevel.L3);
		simpleIO.mergeDuplicates(true);
	}

	
	@PostConstruct
	public void init() 
	{
		// call this only once
		topPathways = mainDAO.getTopPathways();
		topPathways.setMaxHits(topPathways.getSearchHit().size());
	}
	
	
	/*
	 * Interface methods
	 */	
	@Cacheable(cacheName = "findElementsCache")
	@Override
	public ServiceResponse findElements(String queryStr, 
			int page, Class<? extends BioPAXElement> biopaxClass, SearchFilter... searchFilters) 
	{
		ServiceResponse serviceResponse = new ServiceResponse();
		
		try {
			// do search
			SearchResponse hits = mainDAO.findElements(queryStr, page, biopaxClass, searchFilters);
			serviceResponse.setResponse(hits);
			serviceResponse.setQuery("Find '" + queryStr  + 
					"' in " + 
					((biopaxClass == null) ? "all types" : biopaxClass.getSimpleName()) 
					+ "; filters: " + Arrays.toString(searchFilters));
			if(serviceResponse.isEmpty())
				serviceResponse.setNoResultsFoundResponse("No hits");
		} catch (Exception e) {
			serviceResponse.setExceptionResponse(e);
		}
		
		return serviceResponse;
	}
	

	@Cacheable(cacheName = "findEntitiesCache")
	@Override
	public ServiceResponse findEntities(String queryStr, 
			int page, Class<? extends BioPAXElement> biopaxClass, SearchFilter... searchFilters) 
	{
		ServiceResponse serviceResponse = new ServiceResponse();
		try {
			// do search
			SearchResponse hits = mainDAO.findEntities(queryStr, page, biopaxClass, searchFilters); 
			serviceResponse.setResponse(hits);
			serviceResponse.setQuery("Find (Entity) '" + queryStr  + 
				"' in " + 
				((biopaxClass == null) ? "all types" : biopaxClass.getSimpleName()) 
				+ "; filters: " + Arrays.toString(searchFilters));
			if(serviceResponse.isEmpty())
				serviceResponse.setNoResultsFoundResponse("No (Entity) hits");
		} catch (Exception e) {
			serviceResponse.setExceptionResponse(e);
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
        ServiceResponse res = fetchAsBiopax(uris);
        return (res.isError())? res : convert(res, format);
    }

	/*
     * (non-Javadoc)
	 * @see cpath.service.CPathService#convert(..)
	 */
    @Override
    public ServiceResponse convert(String biopax, OutputFormat format) {
        // put incoming biopax into map
		ServiceResponse serviceResponse = new ServiceResponse();
        Model model = simpleIO.convertFromOWL(new ByteArrayInputStream(biopax.getBytes()));
        if(!model.getObjects().isEmpty()) {
            serviceResponse.setData(model);
            return convert(serviceResponse, format);
        } else {
        	serviceResponse.setNoResultsFoundResponse("Empty BioPAX Model!");
        	return serviceResponse;
        }
	}

    
    /*
     * Function used by both convert(String, OutputFormat)
     * and fetch(OutputFormat, String... uris).
     */
    ServiceResponse convert(ServiceResponse serviceResponse, OutputFormat format) 
    {
		try {
			switch (format) {
			case BINARY_SIF:
				serviceResponse = fetchAsBinarySIF(serviceResponse, false);
				break;
			case EXTENDED_BINARY_SIF:
				serviceResponse = fetchAsBinarySIF(serviceResponse, true);
				break;
			case GSEA:
				serviceResponse = fetchAsGSEA(serviceResponse, "uniprot");
				break;
            case BIOPAX: // default handler
			default:
                // do nothing
			}
		}
        catch (Exception e) {
        	serviceResponse.setExceptionResponse(e);
		}

		return serviceResponse;
    }
	
	/** 
	 * Gets BioPAX elements by id, 
	 * creates a sub-model, and returns everything as map.
	 * 
	 * @param uris
	 * @return
	 */
	ServiceResponse fetchAsBiopax(String... uris) {

		ServiceResponse serviceResponse = new ServiceResponse();
		
		if (uris.length > 0) {	
			// extract a sub-model
			Model m = mainDAO.getValidSubModel(Arrays.asList(uris));
			if(!m.getObjects().isEmpty()) {
				serviceResponse.setData(m);
			} else {
				serviceResponse.setNoResultsFoundResponse(
					"No results for: " + Arrays.toString(uris));
			}
		} else {
			serviceResponse.setNoResultsFoundResponse("No URIs were specified for the query!");
		}
		
		return serviceResponse;
	}	
	
	/**
	 * Converts service results that contain a BioPAXmodel to GSEA format.
	 * 
     * @param resp ServiceResponse
	 * @param outputIdType output identifiers type (db name)
	 * @return
	 * @throws IOException 
	 */
	ServiceResponse fetchAsGSEA(ServiceResponse resp, 
		String outputIdType) throws IOException 
	{	
		// convert, replace DATA
		Model m = (Model) resp.getData();
		if (m != null && m.getObjects().size()>0) {
			GSEAConverter gseaConverter = new GSEAConverter(outputIdType, true);
			OutputStream stream = new ByteArrayOutputStream();
			gseaConverter.writeToGSEA(m, stream);
			resp.setData(stream.toString());
		} else {
			log.info("Won't convert to GSEA: empty Model!");
		}
		
		return resp;
	}

	/**
	 * Gets BioPAX elements by id, 
	 * creates a sub-model, converts to SIF format.
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
	ServiceResponse fetchAsBinarySIF(ServiceResponse resp, 
		boolean extended, String... rules) throws IOException 
	{
		// convert, replace DATA value in the map to return
		// TODO match 'rules' parameter to rule types (currently, it uses all)
		Model m = (Model) resp.getData();
		
		if (m == null || m.getObjects().size() == 0) {
			log.info("Won't convert to SIF: empty Model!");
			return resp; //unchanged
		}
		
		SimpleInteractionConverter sic = getSimpleInteractionConverter(m);

		OutputStream edgeStream = new ByteArrayOutputStream();
		if (extended) {
			OutputStream nodeStream = new ByteArrayOutputStream();
			sic.writeInteractionsInSIFNX(
					m, edgeStream, nodeStream,
					Arrays.asList("Entity/displayName", "Entity/xref:UnificationXref", "Entity/xref:RelationshipXref"),
					Arrays.asList("Interaction/dataSource/displayName", "Interaction/xref:PublicationXref"), 
					true);
			String edgeColumns = "PARTICIPANT_A\tINTERACTION_TYPE\tPARTICIPANT_B\tINTERACTION_DATA_SOURCE\tINTERACTION_PUBMED_ID\n";
			String nodeColumns = "PARTICIPANT\tPARTICIPANT_TYPE\tPARTICIPANT_NAME\tUNIFICATION_XREF\tRELATIONSHIP_XREF\n";
			resp.setData(edgeColumns + removeDuplicateBinaryInteractions(edgeStream) + "\n" + nodeColumns + nodeStream.toString());
		} else {
			sic.writeInteractionsInSIF(m, edgeStream);
			resp.setData(removeDuplicateBinaryInteractions(edgeStream));
		}

		return resp;
	}

	/*
     * (non-Javadoc)
	 * @see cpath.service.CPathService#getValidationReport(...)
	 */
	@Override
	public ServiceResponse getValidationReport(String metadataIdentifier) {

		ServiceResponse toReturn = new ServiceResponse();
		
		// get validationResults from PathwayData beans
		Collection<PathwayData> pathwayDataCollection = metadataDAO.getPathwayDataByIdentifier(metadataIdentifier);
		if (!pathwayDataCollection.isEmpty()) {
			// new container to collect different files validation results
			ValidatorResponse response = new ValidatorResponse();

			for (PathwayData pathwayData : pathwayDataCollection) {
				String xmlResult = pathwayData.getValidationResults().trim();
				if (xmlResult != null && xmlResult.length() > 0) {
					// unmarshal and add to the 'results' list
					Validation validation = null;
					try {
						ValidatorResponse resp = (ValidatorResponse) BiopaxValidatorUtils.getUnmarshaller()
							.unmarshal(new StreamSource(new StringReader(xmlResult)));
						validation = resp.getValidationResult().get(0);
					}
                    catch (Exception e) {
						toReturn.setExceptionResponse(e);
						log.error(e);
					}
					if(validation != null)
						response.getValidationResult().add(validation);
				}
			}

			toReturn.setData(response);
		} else {
			toReturn.setNoResultsFoundResponse("No validation data found by " + metadataIdentifier);
		}
		
		return toReturn;
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
		ServiceResponse resp = new ServiceResponse();
		
		try {
			Model m = mainDAO.runAnalysis(analysis, params);
			resp.setData(m);
			resp = convert(resp, format);
		} catch (Exception e) {
			resp.setExceptionResponse(e);
			log.error("runAnalysis failed. ", e);
		}

		return resp;
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
		return runAnalysis(analysis, format, sources, limit, direction);
	}

	@Cacheable(cacheName = "getPathsBetweenCache")
	@Override
	public ServiceResponse getPathsBetween(OutputFormat format, String[] sources, Integer limit)
	{
		
		Analysis analysis = new PathsBetweenAnalysis();
		return runAnalysis(analysis, format, sources, limit);
	}
	
	@Cacheable(cacheName = "getPathsOfInterestCache")
	@Override
	public ServiceResponse getPathsOfInterest(OutputFormat format, String[] sources,
		String[] targets, Integer limit)
	{
		
		Analysis analysis = new PathsOfInterestAnalysis();
		return runAnalysis(analysis, format, sources, targets, limit);
	}
	

	@Cacheable(cacheName = "getCommonStreamCache")
	@Override
	public ServiceResponse getCommonStream(OutputFormat format, String[] sources,
		Integer limit, Direction direction)
	{
		if (direction == Direction.BOTHSTREAM) {
			ServiceResponse resp = new ServiceResponse();
			resp.setResponse(Status.BAD_REQUEST
				.errorResponse("Direction cannot be BOTHSTREAM for the COMMONSTREAM query!"));
			return resp;
		} else if(direction == null) {
			direction = Direction.DOWNSTREAM;	
		}
			
		Analysis analysis = new CommonStreamAnalysis();
		return runAnalysis(analysis, format, sources, limit, direction);
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

        if (model.getLevel() == BioPAXLevel.L2) {
            return new SimpleInteractionConverter(
				new org.biopax.paxtools.io.sif.level2.ComponentRule(),
				new org.biopax.paxtools.io.sif.level2.ConsecutiveCatalysisRule(),
				new org.biopax.paxtools.io.sif.level2.ControlRule(),
				new org.biopax.paxtools.io.sif.level2.ControlsTogetherRule(),
				new org.biopax.paxtools.io.sif.level2.ParticipatesRule());
        }
        else if (model.getLevel() == BioPAXLevel.L3) {
            return new SimpleInteractionConverter(
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
		ServiceResponse toReturn = new ServiceResponse();
		
		// get results from the DAO
		TraverseResponse results = mainDAO.traverse(propertyPath, sourceUris);
		toReturn.setResponse(results);
		
		return toReturn;
	}

	
	/**
	 * Gets top pathways.
	 * 
	 */
	@Override
	public SearchResponse getTopPathways() {
		return topPathways;
	}
}
