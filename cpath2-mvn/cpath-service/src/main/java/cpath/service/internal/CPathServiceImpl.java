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
import cpath.service.jaxb.SearchResponseType;
import cpath.service.CPathService;
import cpath.service.OutputFormat;

//import cpath.warehouse.CvRepository;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.PathwayData;

import static cpath.service.CPathService.ResultMapKey.*;

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

	private static SearchResponseType topPathways;
	
	// this is probably required for the echcache to work
	public CPathServiceImpl() {
	}
	
    /**
     * Constructor.
     */
	public CPathServiceImpl(
			PaxtoolsDAO mainDAO, 
			MetadataDAO metadataDAO) 
	{
		this.mainDAO = mainDAO;
		//this.proteinsDAO = proteinsDAO;
		//this.moleculesDAO = moleculesDAO;
		//this.cvFetcher = cvFetcher;
		this.metadataDAO = metadataDAO;
		this.simpleIO = new SimpleIOHandler(BioPAXLevel.L3);
		simpleIO.mergeDuplicates(true);
	}

	
	@PostConstruct
	public void init() 
	{
		topPathways = mainDAO.getTopPathways();
	}
	
	
	/*
	 * Interface methods
	 */	
	@Cacheable(cacheName = "findElementsCache")
	@Override
	public Map<ResultMapKey, Object> findElements(String queryStr, 
			int page, Class<? extends BioPAXElement> biopaxClass, SearchFilter... searchFilters) 
	{
		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();
		try {
			// do search
			SearchResponseType hits = mainDAO.findElements(queryStr, page, biopaxClass, searchFilters); 
			map.put(DATA, hits);
			map.put(COUNT, hits.getNumHitsBeforeRefined()); // becomes Integer
		} catch (Exception e) {
			map.put(ERROR, e);
		}
		
		return map;
	}
	

	@Cacheable(cacheName = "findEntitiesCache")
	@Override
	public Map<ResultMapKey, Object> findEntities(String queryStr, 
			int page, Class<? extends BioPAXElement> biopaxClass, SearchFilter... searchFilters) 
	{
		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();
		try {
			// do search
			SearchResponseType hits = mainDAO.findEntities(queryStr, page, biopaxClass, searchFilters); 
			map.put(DATA, hits);
			map.put(COUNT, hits.getNumHitsBeforeRefined()); // becomes Integer
		} catch (Exception e) {
			map.put(ERROR, e);
		}
		
		return map;
	}
	

	/*
     * (non-Javadoc)
	 * @see cpath.service.CPathService#fetch(..)
	 */
	@Cacheable(cacheName = "elementByIdCache")
	@Override
	public Map<ResultMapKey, Object> fetch(OutputFormat format, String... uris) {

        // before anything, get the biopax
        Map<ResultMapKey, Object> map = fetchAsBiopax(uris);

        return (map.containsKey(ERROR) || format == OutputFormat.BIOPAX) 
        	? map : convert(map, format);
    }

	/*
     * (non-Javadoc)
	 * @see cpath.service.CPathService#convert(..)
	 */
    @Override
    public Map<ResultMapKey, Object> convert(String biopax, OutputFormat format) {

        // put incoming biopax into map
		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();
        Model model = simpleIO.convertFromOWL(new ByteArrayInputStream(biopax.getBytes()));
        map.put(MODEL, model);
        map.put(DATA, biopax);

        return convert(map, format);
	}

    
    /*
     * Function used by both convert(String, OutputFormat)
     * and fetch(OutputFormat, String... uris).
     */
    Map<ResultMapKey, Object> convert(Map<ResultMapKey, 
    		Object> map, OutputFormat format) 
    {
		try {
			switch (format) {
			case BINARY_SIF:
				map = fetchAsBinarySIF(map, false);
				break;
			case EXTENDED_BINARY_SIF:
				map = fetchAsBinarySIF(map, true);
				break;
			case GSEA:
				map = fetchAsGSEA(map, "uniprot");
				break;
            case BIOPAX: // default handler
			default:
                // do nothing
			}
		}
        catch (Exception e) {
			map.put(ERROR, e);
		}

		return map;
    }
	
	/** 
	 * Gets BioPAX elements by id, 
	 * creates a sub-model, and returns everything as map.
	 * 
	 * @see ResultMapKey
	 * 
	 * @param uris
	 * @return
	 */
	Map<ResultMapKey, Object> fetchAsBiopax(String... uris) {

		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();
		
		if (uris.length >= 1) {	
			// extract a sub-model
			Model m = mainDAO.getValidSubModel(Arrays.asList(uris));
			map.put(MODEL, m);
			map.put(DATA, exportToOWL(m));
		} 
		
		return map;
	}	
	
	/**
	 * Gets BioPAX elements by id (URIs), 
	 * extracts a sub-model, converts to GSEA format, 
	 * and returns everything as map values.
	 * 
     * @param map Map<ResultMapKey, Object>
	 * @param outputIdType output identifiers type (db name)
	 * @return
	 * @throws IOException 
	 */
	Map<ResultMapKey, Object> fetchAsGSEA(Map<ResultMapKey, Object> map, 
		String outputIdType) throws IOException 
	{	
		// convert, replace DATA
		Model m = (Model) map.get(MODEL);
		if (m != null && m.getObjects().size()>0) {
			GSEAConverter gseaConverter = new GSEAConverter(outputIdType, true);
			OutputStream stream = new ByteArrayOutputStream();
			gseaConverter.writeToGSEA(m, stream);
			map.put(DATA, stream.toString());
		} else {
			log.info("Won't convert to GSEA: empty Model!");
		}
		
		return map;
	}

	/**
	 * Gets BioPAX elements by id, 
	 * creates a sub-model, converts to SIF format, 
	 * and returns everything as map values.
	 * 
	 * TODO 'rules' parameter is currently ignored (requires conversion 
	 * from strings to the rules, e.g., using enum. BinaryInteractionRule from cpath-web-service)
	 * 
     * @param map Map<ResultMapKey, Object>
     * @param extended if true, call SIFNX else SIF
	 * @param rules (optional) the names of SIF rules to apply
	 * @return
	 * @throws IOException 
	 */
	Map<ResultMapKey, Object> fetchAsBinarySIF(Map<ResultMapKey, Object> map, 
		boolean extended, String... rules) throws IOException 
	{
		// convert, replace DATA value in the map to return
		// TODO match 'rules' parameter to rule types (currently, it uses all)
		Model m = (Model) map.get(MODEL);
		
		if (m == null || m.getObjects().size() == 0) {
			log.info("Won't convert to SIF: empty Model!");
			return map; //unchanged
		}
		
		SimpleInteractionConverter sic = getSimpleInteractionConverter(m);

		OutputStream edgeStream = new ByteArrayOutputStream();
		if (extended) {
			OutputStream nodeStream = new ByteArrayOutputStream();
			sic.writeInteractionsInSIFNX(m, edgeStream, nodeStream,
										 Arrays.asList("Entity/name", "Entity/xref:UnificationXref", "Entity/xref:RelationshipXref"),
										 Arrays.asList("Interaction/dataSource/name", "Interaction/xref:PublicationXref"), true);
			String edgeColumns = "PARTICIPANT_A\tINTERACTION_TYPE\tPARTICIPANT_B\tINTERACTION_DATA_SOURCE\tINTERACTION_PUBMED_ID\n";
			String nodeColumns = "PARTICIPANT\tPARTICIPANT_TYPE\tPARTICIPANT_NAME\tUNIFICATION_XREF\tRELATIONSHIP_XREF\n";
			map.put(DATA, edgeColumns + removeDuplicateBinaryInteractions(edgeStream) + "\n" + nodeColumns + nodeStream.toString());
		} else {
			sic.writeInteractionsInSIF(m, edgeStream);
			map.put(DATA, removeDuplicateBinaryInteractions(edgeStream));
		}

		return map;
	}

	/*
     * (non-Javadoc)
	 * @see cpath.service.CPathService#getValidationReport(...)
	 */
	@Override
	public Map<ResultMapKey, Object> getValidationReport(String metadataIdentifier) {

		Map<ResultMapKey, Object> toReturn = new HashMap<ResultMapKey, Object>();
		
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
						toReturn.put(ERROR, e);
						log.error(e);
					}
					if(validation != null)
						response.getValidationResult().add(validation);
				}
			}

			toReturn.put(ResultMapKey.ELEMENT, response);
			// write report and add it to the map(DATA)
			StringWriter writer = new StringWriter();
			// (the last parameter below can be a xsltSource)
			BiopaxValidatorUtils.write(response, writer, null); 
			toReturn.put(DATA, writer.toString());
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
	Map<ResultMapKey, Object> runAnalysis(Analysis analysis, OutputFormat format, Object... params)
	{
		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();
		try
		{
			Model m = mainDAO.runAnalysis(analysis, params);
			map.put(MODEL, m);
			map.put(DATA, exportToOWL(m));
			if(format != null && format != OutputFormat.BIOPAX) {
				map = convert(map, format);
			}
		}
		catch (Exception e)
		{
			map.put(ERROR, e);
			log.error("getNeighborhood failed. ", e);
		}

		return map;
	}


	@Cacheable(cacheName = "getNeighborhoodCache")
	@Override
	public Map<ResultMapKey, Object> getNeighborhood(OutputFormat format, String[] sources,
		Integer limit, Direction direction)
	{
		Analysis analysis = new NeighborhoodAnalysis();
		return runAnalysis(analysis, format, sources, limit, direction);
	}

	@Cacheable(cacheName = "getPathsBetweenCache")
	@Override
	public Map<ResultMapKey, Object> getPathsBetween(OutputFormat format, String[] sources,
		String[] targets, Integer limit)
	{
		
		Analysis analysis = new PathsBetweenAnalysis();
		return runAnalysis(analysis, format, sources, targets, limit);
	}

	@Cacheable(cacheName = "getCommonStreamCache")
	@Override
	public Map<ResultMapKey, Object> getCommonStream(OutputFormat format, String[] sources,
		Integer limit, Direction direction)
	{
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

	
	/**
	 * Returns a results map using following pre-defined keys:
	 * - key: {@link ResultMapKey#DATA}, value: Map (property value, source URI)
	 * (for object properties, URIs are used instead of object value)
	 * - key: {@link ResultMapKey#ERROR}, value: error message
	 * 
	 */
	@Override
	public Map<ResultMapKey, Object> traverse(String propertyPath, String... sourceUris) {
		Map<ResultMapKey, Object> toReturn = new HashMap<ResultMapKey, Object>();
		// get results from the DAO
		Map<Object, String> values = mainDAO.traverse(propertyPath, sourceUris);
		// TODO convert to Map<String, String>
		Map<String, String> stringValues = new HashMap<String, String>();
		for(Object o: values.keySet()) {
			if(o instanceof BioPAXElement) {
				stringValues.put(((BioPAXElement) o).getRDFId(), values.get(o));
			} else if(o instanceof String) {
				stringValues.put(((String) o), values.get(o));
			} else {
				stringValues.put(String.valueOf(o), values.get(o));
			}
		}
		toReturn.put(DATA, stringValues);
		return toReturn;
	}

	
	/**
	 * Returns the set of top pathway URIs
	 * 
	 */
	@Override
	public SearchResponseType getTopPathways() {
		return topPathways;
	}
}
