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

import javax.validation.constraints.NotNull;
import javax.xml.bind.*;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.controller.ModelUtils.RelationshipType;
import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.io.gsea.GSEAConverter;
import org.biopax.paxtools.io.sif.SimpleInteractionConverter;
import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.Named;
import org.biopax.paxtools.model.level3.Provenance;
import org.biopax.paxtools.model.level3.RelationshipTypeVocabulary;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.SequenceEntityReference;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.paxtools.query.algorithm.Direction;
import org.biopax.paxtools.query.algorithm.LimitType;
import org.biopax.validator.result.Validation;
import org.biopax.validator.result.ValidatorResponse;
import org.biopax.validator.utils.BiopaxValidatorUtils;
import org.springframework.stereotype.Service;

import com.googlecode.ehcache.annotations.Cacheable;

import cpath.dao.Analysis;
import cpath.dao.PaxtoolsDAO;
import cpath.dao.filters.SearchFilter;
import cpath.dao.internal.DataServicesFactoryBean;
import cpath.service.analyses.CommonStreamAnalysis;
import cpath.service.analyses.NeighborhoodAnalysis;
import cpath.service.analyses.PathsBetweenAnalysis;
import cpath.service.jaxb.ErrorType;
import cpath.service.jaxb.SearchHitType;
import cpath.service.jaxb.SearchResponseType;
import cpath.service.CPathService;

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

	/*[rodche] 
	 * Looks, one DAO per service instance is enough; 
	 * the "second query" use case can be covered by 
	 * the second (third, etc..) CPathService instance!
	@NotNull
	private PaxtoolsDAO proteinsDAO;
	@NotNull
	private PaxtoolsDAO moleculesDAO;
	@NotNull
	private CvRepository cvFetcher;
	*/
	
	@NotNull
	private MetadataDAO metadataDAO;

	private SimpleMerger merger;	
	private JAXBContext jaxbContext;
	private SimpleIOHandler simpleIO;

	// this is probably required for the echcache to work
	public CPathServiceImpl() {
	}
	
    /**
     * Constructor.
     */
	public CPathServiceImpl(
			PaxtoolsDAO mainDAO, 
			//PaxtoolsDAO proteinsDAO,
			//PaxtoolsDAO moleculesDAO,
			//CvRepository cvFetcher,
			MetadataDAO metadataDAO) 
	{
		
		this.mainDAO = mainDAO;
		//this.proteinsDAO = proteinsDAO;
		//this.moleculesDAO = moleculesDAO;
		//this.cvFetcher = cvFetcher;
		this.metadataDAO = metadataDAO;
		this.simpleIO = new SimpleIOHandler(BioPAXLevel.L3);
		simpleIO.mergeDuplicates(true);
		this.merger = new SimpleMerger(simpleIO.getEditorMap());
		
		// init cpath legacy xml schema jaxb context
		try {
			jaxbContext = JAXBContext.newInstance(ErrorType.class, SearchResponseType.class, SearchHitType.class);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}
	
	/*
	 * Interface methods
	 */	
	@Cacheable(cacheName = "findElementsCache")
	@Override
	public Map<ResultMapKey, Object> findElements(String queryStr, 
			Class<? extends BioPAXElement> biopaxClass, SearchFilter... searchFilters) 
	{
		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();
		try {
			// do search
			List<BioPAXElement> data = mainDAO.findElements(queryStr, biopaxClass, searchFilters); 
			// build (xml/json) serializable search hit types
			List<SearchHitType> hits = toSearchHits(data);
			map.put(DATA, hits);
			map.put(COUNT, hits.size()); // becomes Integer
		} catch (Exception e) {
			map.put(ERROR, e.toString());
		}
		
		return map;
	}
	
	
	/**
	 * Converts the returned by a query BioPAX elements to 
	 * simpler "hit" java beans (serializable to XML, etc..) 
	 * 
	 * @param data
	 * @return
	 */
	private List<SearchHitType> toSearchHits(List<? extends BioPAXElement> data) {
		List<SearchHitType> hits = new ArrayList<SearchHitType>(data.size());
		
		for(BioPAXElement bpe : data) {
			mainDAO.initialize(bpe);
			SearchHitType hit = new SearchHitType();
			hit.setUri(bpe.getRDFId());
			hit.setBiopaxClass(bpe.getModelInterface().getSimpleName());
			// add standard and display names if any -
			if(bpe instanceof Named) {
				Named named = (Named)bpe;
				String std = named.getStandardName();
				if( std != null)
					hit.getName().add(std);
				String dsp = named.getDisplayName();
				if(dsp != null && !dsp.equalsIgnoreCase(std))
					hit.getName().add(dsp);
			}
			// add organisms and data sources
			if(bpe instanceof Entity) {
				// add data sources (URIs)
				for(Provenance pro : ((Entity)bpe).getDataSource()) {
					hit.getDataSource().add(pro.getRDFId());
				}
				
				// add organisms and pathways (URIs);
				// at the moment, this apply to Entities only -
				for(Xref x : ((Entity)bpe).getXref()) 
				{
					mainDAO.initialize(x);
					if((x instanceof RelationshipXref) && ((RelationshipXref) x).getRelationshipType() != null) 
					{
						RelationshipXref rx = (RelationshipXref) x;
						RelationshipTypeVocabulary cv = rx.getRelationshipType();
						mainDAO.initialize(cv);
						if(cv.getRDFId().equalsIgnoreCase(ModelUtils
							.relationshipTypeVocabularyUri(RelationshipType.ORGANISM.name()))) 
						{
							hit.getOrganism().add(rx.getId());
						} 
						else if(cv.getRDFId().equalsIgnoreCase(ModelUtils
							.relationshipTypeVocabularyUri(RelationshipType.PROCESS.name()))) 
						{
							hit.getPathway().add(rx.getId());
						}	
					}
				}
			}
			
			// set organism for some of EntityReference
			if(bpe instanceof SequenceEntityReference) {
				BioSource bs = ((SequenceEntityReference)bpe).getOrganism(); 
				if(bs != null)
					hit.getOrganism().add(bs.getRDFId());
			}
			
			
			hits.add(hit);
		}
		
		return hits;
	}

	@Cacheable(cacheName = "findEntitiesCache")
	@Override
	public Map<ResultMapKey, Object> findEntities(String queryStr, 
			Class<? extends BioPAXElement> biopaxClass, SearchFilter... searchFilters) 
	{
		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();
		try {
			// do search
			List<Entity> data = mainDAO.findEntities(queryStr, biopaxClass, searchFilters); 
			// build (xml/json) serializable search hit types
			List<SearchHitType> hits = toSearchHits(data);
			map.put(DATA, hits);
			map.put(COUNT, hits.size()); // becomes Integer
		} catch (Exception e) {
			map.put(ERROR, e.toString());
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

        // outta here
        return (map.containsKey(ERROR)) ? map : convert(map, format);
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

        // outta here
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
			map.put(ERROR, e.toString());
		}

        // outta here
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
		
		if (uris.length == 1) {
			// also put the object (element) in the map
			BioPAXElement element = mainDAO.getByID(uris[0]);
			if (element != null) {
				mainDAO.initialize(element);
				map.put(ELEMENT, element);
			}
		}
		
        // outta here
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
	 */
	Map<ResultMapKey, Object> fetchAsGSEA(Map<ResultMapKey, Object> map, String outputIdType) {
		
		// convert, replace DATA
		Model m = (Model) map.get(MODEL);
		GSEAConverter gseaConverter = new GSEAConverter(outputIdType, false);
		OutputStream stream = new ByteArrayOutputStream();
		try {
	        gseaConverter.writeToGSEA(m, stream);
	        map.put(DATA, stream.toString());
		} catch (Exception e) {
			map.put(ERROR, e.toString());
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
	 */
	Map<ResultMapKey, Object> fetchAsBinarySIF(Map<ResultMapKey, Object> map, boolean extended, String... rules) {

		// convert, replace DATA value in the map to return
		// TODO match 'rules' parameter to rule types (currently, it uses all)
		Model m = (Model) map.get(MODEL);
		SimpleInteractionConverter sic = getSimpleInteractionConverter(m);

		try {
			OutputStream edgeStream = new ByteArrayOutputStream();
            if (extended) {
                OutputStream nodeStream = new ByteArrayOutputStream();
                sic.writeInteractionsInSIFNX(m, edgeStream, nodeStream,
                	Arrays.asList("Entity/name","Entity/xref","Entity/organism"), 
                	Arrays.asList("Interaction/xref:PublicationXref"));
                map.put(DATA, edgeStream.toString() + "\n\n" + nodeStream.toString());
            }
            else {
                sic.writeInteractionsInSIF(m, edgeStream);
                map.put(DATA, edgeStream.toString());
            }
		}
        catch (Exception e) {
			map.put(ERROR, e.toString());
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
			putRequiredOutput(m, format, map);
		}
		catch (Exception e)
		{
			map.put(ERROR, e);
			log.error("getNeighborhood failed. ", e);
		}

		return map;
	}

	/**
	 * Converts the given biopax model into the requested format and puts into the result map.
	 * TODO: Implement this conversion.
	 *
	 * @param model result model
	 * @param format requested format
	 * @param map result map
	 */
	protected void putRequiredOutput(Model model, OutputFormat format,
		Map<ResultMapKey, Object> map)
	{
		// Currently only biopax, sif, gsea output is supported
		map.put(MODEL, model);
		
		if(format == null || format == OutputFormat.BIOPAX) {
			map.put(DATA, exportToOWL(model));
		} 
		else if(format == OutputFormat.BINARY_SIF) {
			map.put(DATA, fetchAsBinarySIF(map, false));
		} 
		else if(format == OutputFormat.EXTENDED_BINARY_SIF) {
			map.put(DATA, fetchAsBinarySIF(map, true));
		}
		else if(format == OutputFormat.GSEA) {
			map.put(DATA, fetchAsGSEA(map, "UniProt"));
		}
	}

	@Cacheable(cacheName = "getNeighborhoodCache")
	@Override
	public Map<ResultMapKey, Object> getNeighborhood(OutputFormat format, String[] source,
		Integer limit, Direction direction)
	{
		Analysis analysis = new NeighborhoodAnalysis();
		return runAnalysis(analysis, format, source, limit, direction);
	}

	@Cacheable(cacheName = "getPathsBetweenCache")
	@Override
	public Map<ResultMapKey, Object> getPathsBetween(OutputFormat format, String[] source,
		String[] target, Integer limit, LimitType limitType)
	{
		
		Analysis analysis = new PathsBetweenAnalysis();
		return runAnalysis(analysis, format, source, target, limit, limitType);
	}

	@Cacheable(cacheName = "getCommonStreamCache")
	@Override
	public Map<ResultMapKey, Object> getCommonStream(OutputFormat format, String[] source,
		Integer limit, Direction direction)
	{
		Analysis analysis = new CommonStreamAnalysis();
		return runAnalysis(analysis, format, source, limit, direction);
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
				new org.biopax.paxtools.io.sif.level3.ParticipatesRule());
        }

        // should not make it here
        return null;
    }
    
    @Override
    protected void finalize() throws Throwable {
    	try {
    		DataServicesFactoryBean.clearAllDatasources();
    	} finally {
    		super.finalize();
    	}
    }
}
