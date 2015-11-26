package cpath.service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import cpath.config.CPathSettings;
import org.biopax.paxtools.io.gsea.GSEAConverter;
import org.biopax.paxtools.io.sbgn.L3ToSBGNPDConverter;
import org.biopax.paxtools.io.sbgn.ListUbiqueDetector;
import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.Provenance;
import org.biopax.paxtools.pattern.miner.*;
import org.biopax.paxtools.pattern.util.Blacklist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.service.jaxb.DataResponse;
import cpath.service.jaxb.ServiceResponse;
import static cpath.service.Status.*;

/**
 * A utility class to convert a BioPAX 
 * L3 RDF/XML data stream or {@link Model} 
 * to one of {@link OutputFormat}s  
 * (including - to BioPAX L3 RDF/XML)
 * 
 * @author rodche
 */
public class BiopaxConverter {
	private static final Logger log = LoggerFactory.getLogger(BiopaxConverter.class);
	
	private final Blacklist blacklist;
		
	/**
	 * Constructor.
	 * 
	 * @param blacklist of ubiquitous molecules to exclude (in some algorithms)
	 */
	public BiopaxConverter(Blacklist blacklist)
	{
		this.blacklist = blacklist;
	}

 
	/**
     * Converts the BioPAX data into the other format.
     * 
     * @param m paxtools model
     * @param format output format
     * @param os output stream
     * @param args optional format-specific parameters
	 * @throws IOException when an error occurs while writing to the output stream
     */
    public void convert(Model m, OutputFormat format, OutputStream os, Object... args) 
    		throws IOException 
    {
			switch (format) {
			case BIOPAX: //to OWL (RDF/XML)
				new SimpleIOHandler().convertToOWL(m, os);
				break;
			case BINARY_SIF:
				String db = null; //default: will use HGNC Symbol
				if (args.length > 0 && args[0] instanceof String)
					db = (String) args[0];
				convertToBinarySIF(m, os, false, db);
				break;
			case EXTENDED_BINARY_SIF:
				db = null; //default: will use HGNC Symbol
				if (args.length > 0 && args[0] instanceof String)
					db = (String) args[0];
				convertToBinarySIF(m, os, true, db);
				break;
			case GSEA:
				db = "uniprot"; //default
				if (args.length > 0 && args[0] instanceof String)
					db = (String) args[0];
				convertToGSEA(m, os, db);
				break;
            case SBGN:
				boolean doLayout = true;
				if (args.length > 0) {
					doLayout = (args[0] instanceof Boolean)
							? ((Boolean)args[0]).booleanValue() 
								: Boolean.parseBoolean(String.valueOf(args[0]));
				}
                convertToSBGN(m, os, blacklist, doLayout);
                break;
			default: throw new UnsupportedOperationException(
					"convert, yet unsupported format: " + format);
			}
    }    
    
    
	/**
     * Converts not too large BioPAX model 
     * (e.g., a graph query result) to another format.
     * 
     * @param m a sub-model (not too large), e.g., a get/graph query result
     * @param format output format
     * @param args optional format-specific parameters
     * 
     * @return data response with the converted data (up to 1Gb utf-8 string) 
     * 			or {@link ErrorResponse}.
     */
    public ServiceResponse convert(Model m, OutputFormat format, Object... args) 
    {
    	if(m == null || m.getObjects().isEmpty()) {
			return new ErrorResponse(NO_RESULTS_FOUND, "Empty BioPAX Model");
		}
    	
		// otherwise, convert, return a new DataResponse
    	// (can contain up to ~ 1Gb unicode string data)
    	// a TMP File is used instead of a byte array; set the file path as dataResponse.data value
    	File tmpFile = null;
    	try {
    		Path tmpFilePath = Files.createTempFile("cpath2", "");
    		tmpFile = tmpFilePath.toFile();
    		tmpFile.deleteOnExit();
        	OutputStream os = new FileOutputStream(tmpFile);
    		convert(m, format, os, args); //os gets auto-closed there		
    		DataResponse dataResponse = new DataResponse();
			dataResponse.setData(tmpFilePath);
			// extract and save data provider names
			dataResponse.setProviders(providers(m));			
			return dataResponse;
		}
        catch (Exception e) {
        	if(tmpFile != null)
        		tmpFile.delete();
        	return new ErrorResponse(INTERNAL_ERROR, e);
		}
    }


    /**
     * Converts a BioPAX Model to SBGN format.
     *
     * @param m BioPAX object model to convert
     * @param stream output stream for the SBGN-ML result
     * @param blackList skip-list of ubiquitous small molecules
     * @param doLayout whether to apply the default layout or not
     * 
     * @throws IOException when there is an output stream writing error
     */
    public void convertToSBGN(Model m, OutputStream stream, Blacklist blackList, boolean doLayout)
		throws IOException
	{
    	
    	L3ToSBGNPDConverter converter = new L3ToSBGNPDConverter(
			new ListUbiqueDetector(blackList.getListed()), null, doLayout);

		converter.writeSBGN(m, stream);
    }


    /**
	 * Converts service results that contain 
	 * a not empty BioPAX Model to GSEA format.
	 * 
     * @param m paxtools model
     * @param stream output stream
	 * @param outputIdType output identifiers type (db name, is data-specific, the default is UniProt)
	 * 
	 * @throws IOException when there is an output stream writing error
	 */
	public void convertToGSEA(Model m, OutputStream stream, String outputIdType) 
			throws IOException 
	{	
		if(outputIdType==null || outputIdType.isEmpty())
			outputIdType = "uniprot";

		//Using the model and cPath2 instance properties,
		//prepare a list of Provenance to skip traversing into sub-pathways of:
		Set<Provenance> skipSubPathwaysOf = new HashSet();
		for(String mid : CPathSettings.getInstance().getMetadataIdsForGseaSkipSubPathways()) {
			Provenance pro = (Provenance) m.getByID(CPathSettings.getInstance().getXmlBase()+mid);
			if(pro != null) skipSubPathwaysOf.add(pro);
		}

		if(log.isDebugEnabled() && !skipSubPathwaysOf.isEmpty()) {
			log.debug("GSEAConverter, using skipSubPathwaysOf argument, value: " + skipSubPathwaysOf.toString());
		}

		// convert, replace DATA
		GSEAConverter gseaConverter = new GSEAConverter(outputIdType, true, skipSubPathwaysOf);
		gseaConverter.writeToGSEA(m, stream);
	}

	
	/**
	 * Converts a not empty BioPAX Model (contained in the service bean) 
	 * to the SIF or <strong>single-file</strong> extended SIF format.
	 * 
	 * This method is primarily designed for the web service.
	 * 
     * @param m biopax paxtools to convert
     * @param out stream
     * @param extended if true, calls SIFNX else - SIF
	 * @param db - either 'uniprot' or null (then HGNC symbols will be used by default)
	 * 
	 * @throws IOException when there is an output stream writing error
	 */
	public void convertToBinarySIF(Model m, OutputStream out, boolean extended, String db) 
			throws IOException 
	{
		ConfigurableIDFetcher idFetcher = new ConfigurableIDFetcher();
		idFetcher.chemDbStartsWithOrEquals("chebi");
		if("uniprot".equalsIgnoreCase(db)) {
			idFetcher.seqDbStartsWithOrEquals("uniprot");
		} else {
			idFetcher.seqDbStartsWithOrEquals("hgnc");
		}

		SIFSearcher searcher = new SIFSearcher(idFetcher, SIFEnum.values());
		searcher.setBlacklist(blacklist);

		if (extended) {
			Set<SIFInteraction> binaryInts = searcher.searchSIF(m);
			OldFormatWriter.write(binaryInts, out);
		} else {
			searcher.searchSIF(m, out);
		}
	}
	
	/**
	 * The list of datasources (data providers)
	 * the BioPAX model contains.
	 * 
	 * @param m BioPAX object model
	 */
	@SuppressWarnings("unchecked")
	private Set<String> providers(Model m) {
		Set<String> names = null;
		
		if(m != null) {
			Set<Provenance> provs = m.getObjects(Provenance.class);		
			if(provs!= null && !provs.isEmpty()) {
				names = new TreeSet<String>();
				for(Provenance prov : provs) {
					String name = prov.getStandardName();
					if(name != null)
						names.add(name);
					else {
						name = prov.getDisplayName();
						if(name != null)
							names.add(name);
						else 
							log.warn("No standard|display name found for " + prov);
					}
				}
			}
		}
		
		return (names != null && !names.isEmpty()) 
				? names : Collections.EMPTY_SET;
	}
}
