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
import org.biopax.paxtools.io.gsea.GSEAConverter;
import org.biopax.paxtools.io.sif.SimpleInteractionConverter;
import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.Named;
import org.biopax.validator.result.Validation;
import org.biopax.validator.result.ValidatorResponse;
import org.biopax.validator.utils.BiopaxValidatorUtils;
import org.springframework.stereotype.Service;

import cpath.dao.PaxtoolsDAO;
import cpath.service.CPathService;
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
 * TODO It's not finished at all; - add/implement methods, debug!
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
	
	private final SimpleReader reader;
	private final SimpleExporter exporter;
	private final SimpleMerger merger;
	
	private final JAXBContext jaxbContext;
	
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
		this.reader = new SimpleReader(BioPAXLevel.L3);
		reader.mergeDuplicates(true);
		this.exporter = new SimpleExporter(BioPAXLevel.L3);
		this.merger = new SimpleMerger(reader.getEditorMap());
		
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
	
	@Override
	public Map<ResultMapKey, Object> find(String queryStr, 
			Class<? extends BioPAXElement> biopaxClass, boolean countOnly,
			Integer[] taxids, String... dsources) 
	{
		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();

		if(biopaxClass == null) 
			biopaxClass = BioPAXElement.class;
		if(taxids == null)
			taxids = new Integer[]{};
		
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

	
	String exportToOWL(Model model) {
		if(model == null) return null;
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			exporter.convertToOWL(model, out);
		} catch (IOException e) {
			log.error(e);
		}
		return out.toString();
	}

	
	@Override
	public Map<ResultMapKey, Object> fetch(OutputFormat format, String... uris) {
		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();
		try {
			switch (format) {
			case BINARY_SIF:
				map = fetchAsBinarySIF(uris);
				break;
			case GSEA:
				map = fetchAsGSEA("uniprot", uris); // default
				break;
			case PC_GENE_SET: // TODO
				
				break;
			case TSV: // TODO
				
				break;
			case ID_LIST: // TODO
				
				break;
			case IMAGE_MAP: // TODO ?
				
				break;
			case XML: 
				// map to the legacy cpath xml format
				map = fetchAsXmlSearchResponse(uris);
				break;
			case BIOPAX: // is default
			default:
				map = fetchAsBiopax(uris);
			}
		} catch (Exception e) {
			map.put(ERROR, e.toString());
		}
		return map;
	}	

	
	/**
	 * @param uris
	 * @param string
	 * @return
	 */
	public Map<ResultMapKey, Object> fetchAsGSEA(String idType, String... uris) {
		// get as BioPAX first
		Map<ResultMapKey, Object> map = fetchAsBiopax(uris);
		
		// check for internal errors
		if(map.containsKey(ERROR)) {
			return map; // return as is (with error)
		}
		
		// convert, replace DATA
		Model m = (Model) map.get(MODEL);
		GSEAConverter gseaConverter = new GSEAConverter(idType, false);
		OutputStream stream = new ByteArrayOutputStream();
		try {
	        gseaConverter.writeToGSEA(m, stream);
	        map.put(DATA, stream.toString());
		} catch (Exception e) {
			map.put(ERROR, e.toString());
		}
		
		return map;
	}


	/*
	 * (non-Javadoc)
	 * @see cpath.service.CPathService#fetchAsBinarySIF(java.lang.String[], java.lang.String[])
	 * 
	 * TODO 'rules' parameter is currently ignored (requires conversion 
	 * from strings to the rules, e.g., using enum. BinaryInteractionRule from cpath-web-service)
	 */
	public Map<ResultMapKey, Object> fetchAsBinarySIF(String[] uris, String... rules) {
		// get as BioPAX first
		Map<ResultMapKey, Object> map = fetchAsBiopax(uris);
		
		// check for internal errors
		if(map.containsKey(ERROR)) {
			return map; // return as is (with error)
		}
		
		// convert, replace DATA value in the map to return
		// TODO match 'rules' parameter to rule types (currently, it uses all)
		Model m = (Model) map.get(MODEL);
		SimpleInteractionConverter sic = new SimpleInteractionConverter(
				new org.biopax.paxtools.io.sif.level3.ComponentRule(),
				new org.biopax.paxtools.io.sif.level3.ConsecutiveCatalysisRule(),
				new org.biopax.paxtools.io.sif.level3.ControlRule(),
				new org.biopax.paxtools.io.sif.level3.ControlsTogetherRule(),
				new org.biopax.paxtools.io.sif.level3.ParticipatesRule());
		try {
			OutputStream edgeStream = new ByteArrayOutputStream();
			//sic.writeInteractionsInSIF(m, edgeStream); // default SIF output uses IDs
			// lets use extended format and write other attributes
			OutputStream nodeStream = new ByteArrayOutputStream();
	        sic.writeInteractionsInSIFNX(m, edgeStream, nodeStream, 
	        		false, reader.getEditorMap(), "name", "xref");
	        map.put(DATA, edgeStream.toString() + "\n\n" + nodeStream.toString());
		} catch (Exception e) {
			map.put(ERROR, e.toString());
		}

		return map;
	}
	

	public Map<ResultMapKey, Object> fetchAsBiopax(String... uris) {
		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();
		
		if(uris.length >= 1) {	
			// extract a sub-model
			Model m = mainDAO.getValidSubModel(Arrays.asList(uris));
			map.put(MODEL, m);
			map.put(DATA, exportToOWL(m));
		} 
		
		if(uris.length == 1) {
			// also put the object (element) in the map
			BioPAXElement element = mainDAO.getByID(uris[0]);
			if (element != null) {
				mainDAO.initialize(element);
				map.put(ELEMENT, element);
			}
		}
		
		return map;
	}
	
	
	// TODO finish...
	public Map<ResultMapKey, Object> fetchAsXmlSearchResponse(String... uris) {
		Map<ResultMapKey, Object> toReturn = new HashMap<ResultMapKey, Object>();
		
		// extract a Biopax sub-model
		Model m = mainDAO.getValidSubModel(Arrays.asList(uris));
		
		// build a SearchResponseType, fill in, marshal
		SearchResponseType searchResponse = new SearchResponseType();
		List<ExtendedRecordType> hits = searchResponse.getSearchHit();
		for (String id : uris) {
			BioPAXElement element = m.getByID(id); //= mainDAO.getByID(id);
			if (element != null) {
				//mainDAO.initialize(element); // if above: mainDAO.getByID(id)
				ExtendedRecordType rec = new ExtendedRecordType();
				rec.setPrimaryId(id);
				if(element instanceof Named)
					rec.setName(((Named)element).getName().toString());
				rec.setEntityType(element.getModelInterface().getSimpleName());
				hits.add(rec);
				// TODO set all fields... (requires using extra, pre-calculated parent-child, data!)
			} else {
				// ignore not found...
			}
		}
		
		if(hits.size() != 0) {
			searchResponse.setTotalNumHits(Long.valueOf(hits.size()));
			toReturn.put(DATA, marshalSearchResponse(searchResponse));
		} // otherwise, returns empty map
		
		return toReturn;
	}


	// TODO finish later...
	Map<ResultMapKey, Object> fetchAsXmlSummaryResponse(String... uris) {
		return new HashMap<ResultMapKey, Object>();
	}
	
	
	String marshalSearchResponse(SearchResponseType obj) {
		StringWriter writer = new StringWriter();
		try {
			Marshaller ma = jaxbContext.createMarshaller();
			ma.setProperty("jaxb.formatted.output", true);
			ma.marshal(
			new JAXBElement<SearchResponseType>(new QName("","search_response"), 
					SearchResponseType.class, obj), writer);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		return writer.toString();
	}

	
	String marshalSummaryResponce(SummaryResponseType obj) {
		StringWriter writer = new StringWriter();
		try {
			Marshaller ma = jaxbContext.createMarshaller();
			ma.setProperty("jaxb.formatted.output", true);
			ma.marshal(
			new JAXBElement<SummaryResponseType>(new QName("","summary_response"), 
					SummaryResponseType.class, obj), writer);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		return writer.toString();
	}


	/* (non-Javadoc)
	 * @see cpath.service.CPathService#getValidationReport(java.lang.String)
	 */
	@Override
	public Map<ResultMapKey, Object> getValidationReport(
			String metadataIdentifier) 
	{
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
						validation = (Validation) BiopaxValidatorUtils.getUnmarshaller()
							.unmarshal(new StreamSource(new StringReader(xmlResult)));
					} catch (Exception e) {
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
			
			//if(log.isDebugEnabled()) log.debug(writer.toString());
		}
		
		return toReturn;
	}
}
