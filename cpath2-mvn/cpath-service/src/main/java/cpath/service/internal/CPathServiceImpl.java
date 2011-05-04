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
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.io.gsea.GSEAConverter;
import org.biopax.paxtools.io.sif.SimpleInteractionConverter;
import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.Named;
import org.biopax.validator.result.Validation;
import org.biopax.validator.result.ValidatorResponse;
import org.biopax.validator.utils.BiopaxValidatorUtils;
import org.springframework.stereotype.Service;

import cpath.dao.Analysis;
import cpath.dao.PaxtoolsDAO;
import cpath.service.CPathService;
import cpath.service.CPathService.ResultMapKey;
import cpath.service.analyses.NearestNeighborhoodQueryAnalysis;
import cpath.service.jaxb.*;
import cpath.warehouse.CvRepository;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.PathwayData;

import static cpath.service.CPathService.ResultMapKey.*;

/**
 * Service tier class - to uniformly access 
 * persisted BioPAX model (DAO) from console and webservice 
 * 
 * @author rodche
 *
 * TODO add/implement methods, debug!
 */
@Service
public class CPathServiceImpl implements CPathService {
	private static final Log log = LogFactory.getLog(CPathServiceImpl.class);
	
	@NotNull
	private PaxtoolsDAO mainDAO;

	@NotNull
	private PaxtoolsDAO proteinsDAO;
	
	@NotNull
	private PaxtoolsDAO moleculesDAO;
	
	@NotNull
	private MetadataDAO metadataDAO;
	
	@NotNull
	private CvRepository cvFetcher;

	private final SimpleMerger merger;	
	private final JAXBContext jaxbContext;
	private final SimpleIOHandler simpleIO;

    /**
     * Constructor.
     */
	public CPathServiceImpl(
			PaxtoolsDAO mainDAO, 
			PaxtoolsDAO proteinsDAO,
			PaxtoolsDAO moleculesDAO,
			MetadataDAO metadataDAO,
			CvRepository cvFetcher) {
		
		this.mainDAO = mainDAO;
		this.proteinsDAO = proteinsDAO;
		this.moleculesDAO = moleculesDAO;
		this.metadataDAO = metadataDAO;
		this.cvFetcher = cvFetcher;
		this.simpleIO = new SimpleIOHandler(BioPAXLevel.L3);
		simpleIO.mergeDuplicates(true);
		this.merger = new SimpleMerger(simpleIO.getEditorMap());
		
		// init cpath legacy xml schema jaxb context
		try {
			jaxbContext = JAXBContext.newInstance("cpath.service.jaxb");
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}
	
	/*
	 * Interface methods
	 */	
	
	/*
     * (non-Javadoc)
	 * @see cpath.service.CPathService#find(..)
	 */
	@Override
	public Map<ResultMapKey, Object> find(String queryStr, 
			Class<? extends BioPAXElement> biopaxClass, boolean countOnly,
			String[] taxids, String... dsources) 
	{
		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();

		if(biopaxClass == null) 
			biopaxClass = BioPAXElement.class;
		if(taxids == null)
			taxids = new String[]{};
		
		try {
			if (countOnly) {
				Integer count =  mainDAO.count(null, biopaxClass);
				map.put(COUNT, count);
			} else {
				Collection<String> data = mainDAO.find(queryStr, biopaxClass);
				map.put(DATA, data);
				map.put(COUNT, data.size()); // becomes Integer
			}
		} catch (Exception e) {
			map.put(ERROR, e.toString());
		}
		
		return map;
	}

	/*
     * (non-Javadoc)
	 * @see cpath.service.CPathService#fetch(..)
	 */
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
    Map<ResultMapKey, Object> convert(Map<ResultMapKey, Object> map, OutputFormat format) {

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
                                             true, new SimpleEditorMap(m.getLevel()), "NAME", "XREF", "ORGANISM");
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

        // outta here
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

	/**
     * (non-Javadoc)
	 * @see cpath.service.CPathService#getNeighborhood(...)
     */
    @Override
	public Map<ResultMapKey, Object> getNeighborhood(OutputFormat format, String... uris) {

		// for now, only return biopax
		return getNeighborhood(uris);
	}
	
	/**
	 * Executes a nearest neighborhood query on the global persistent BioPAX model (main).
	 * 
	 * @param uris
	 * @return
	 */
	Map<ResultMapKey, Object> getNeighborhood(String... uris) {
		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();
		
		if (uris.length >= 1) {	
			try {
				Analysis analysis = new NearestNeighborhoodQueryAnalysis();
				Model m = mainDAO.runAnalysis(analysis, uris);
				map.put(MODEL, m);
				map.put(DATA, exportToOWL(m));
			}
            catch (Exception e) {
				map.put(ERROR, e);
				log.error("getNeighborhood failed. ", e);
			}
		} 
		
		return map;
	}

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
}
