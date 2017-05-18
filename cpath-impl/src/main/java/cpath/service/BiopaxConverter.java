package cpath.service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import cpath.config.CPathSettings;
import org.biopax.paxtools.io.gsea.GSEAConverter;
import org.biopax.paxtools.io.jsonld.JsonldBiopaxConverter;
import org.biopax.paxtools.io.jsonld.JsonldConverter;
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
import org.springframework.util.Assert;

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
     * @param m paxtools model (not null)
     * @param format output format
     * @param options format options
	 * @param os output stream
     */
    private void convert(Model m,
						 OutputFormat format,
						 Map<String, String> options,
						 OutputStream os) throws IOException
    {
		Assert.notNull(m,"Model is null");
    	switch (format) {
			case BIOPAX: //to OWL (RDF/XML)
				new SimpleIOHandler().convertToOWL(m, os);
				break;
			case BINARY_SIF:
			case SIF:
				convertToSIF(m, os, false, options);
				break;
			case EXTENDED_BINARY_SIF:
			case TXT:
				convertToSIF(m, os, true, options);
				break;
			case GSEA:
				convertToGSEA(m, os, options);
				break;
            case SBGN:
                convertToSBGN(m, os, blacklist, CPathSettings.getInstance().isSbgnLayoutEnabled());
                break;
			case JSONLD:
				convertToJsonLd(m, os);
				break;
			default: throw new UnsupportedOperationException(
					"convert, yet unsupported format: " + format);
			}
    }

	private void convertToJsonLd(Model m, OutputStream os) throws IOException {
		DataResponse dr = (DataResponse) convert(m, OutputFormat.BIOPAX, null);
		JsonldConverter converter = new JsonldBiopaxConverter();
		Path inp = (Path) dr.getData();
		converter.convertToJsonld(new FileInputStream(inp.toFile()), os);
		inp.toFile().delete();
	}


	/**
     * Converts not too large BioPAX model 
     * (e.g., a graph query result) to another format.
     * 
     * @param m a sub-model (not too large), e.g., a get/graph query result
     * @param format output format
     * @param options format options
     */
    public ServiceResponse convert(Model m,
								   OutputFormat format,
								   Map<String, String> options)
    {
    	if(m == null || m.getObjects().isEmpty()) {
    		//build an empty data response
			DataResponse r = new DataResponse();
			r.setFormat(format);
			return r;
		}
    	
		// otherwise, convert, return a new DataResponse
    	// (can contain up to ~ 1Gb unicode string data)
    	// a TMP File is used instead of a byte array; set the file path as dataResponse.data value
    	File tmpFile = null;
		try {
    		Path tmpFilePath = Files.createTempFile("cpath2", format.getExt());
    		tmpFile = tmpFilePath.toFile();
    		tmpFile.deleteOnExit();

    		convert(m, format, options, new FileOutputStream(tmpFile)); //FOS gets closed in there

			DataResponse dataResponse = new DataResponse();
			dataResponse.setFormat(format);
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
    private void convertToSBGN(Model m, OutputStream stream, Blacklist blackList, boolean doLayout)
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
	 * @param options format options
	 * @throws IOException when there is an output stream writing error
	 */
	private void convertToGSEA(Model m, OutputStream stream, Map<String,String> options)
			throws IOException
	{
		String idType;
		if((idType = options.get("db"))==null)
			idType = "uniprot"; //default; curr. one value is expected

		// It won't traverse into sub-pathways; will use only pre-defined organisms.
		// GSEAConverter's 'skipSubPathways' option is a different beast from the PC web api's 'subpw':
		// given sub-model (no matter how it was cut from the main model), there is still choice
		// to include gene IDs from sub-pathways (if there're any) into parent pathway's record or not.
		GSEAConverter gseaConverter = new GSEAConverter(idType, true, true);
		Set<String> allowedTaxIds = CPathSettings.getInstance().getOrganismTaxonomyIds();
		gseaConverter.setAllowedOrganisms(allowedTaxIds);
		gseaConverter.setSkipOutsidePathways(true);
		gseaConverter.writeToGSEA(m, stream);
	}

	
	/*
	 * Converts a not empty BioPAX Model (contained in the service bean) 
	 * to the SIF or <strong>single-file</strong> extended SIF format.
	 * This is mainly for calling internally through the web service api.
	 */
	private void convertToSIF(Model m, OutputStream out,
							  boolean extended, Map<String,String> options) throws IOException
	{
		String db;
		if ((db = options.get("db"))==null)
			db = "hgnc symbol"; //default

		ConfigurableIDFetcher idFetcher = new ConfigurableIDFetcher();
		idFetcher.chemDbStartsWithOrEquals("chebi");

		if(db == null || db.isEmpty() || db.toLowerCase().startsWith("hgnc")) {
			idFetcher.seqDbStartsWithOrEquals("hgnc");
		}
		else if(db.toLowerCase().startsWith("uniprot")) {
			idFetcher.seqDbStartsWithOrEquals("uniprot");
		} else {
			idFetcher.seqDbStartsWithOrEquals(db);
		}

		SIFType[] sifTypes;
		if(options.containsKey("pattern"))
		{
			String[] sifNames = options.get("pattern").split(",");
			sifTypes = new SIFType[sifNames.length];
			int i=0;
			for(String t : sifNames)
				sifTypes[i++] = SIFEnum.typeOf(t);
		}
		else
		{
			//default: apply all SIF rules but neighbor_of
			Collection<SIFType> c = new HashSet<SIFType>(Arrays.asList(SIFEnum.values()));
			c.remove(SIFEnum.NEIGHBOR_OF); //exclude NEIGHBOR_OF
			sifTypes = c.toArray(new SIFType[c.size()]);
		}

		SIFSearcher searcher = new SIFSearcher(idFetcher, sifTypes);
		searcher.setBlacklist(blacklist);

		if(extended) {
			Set<SIFInteraction> binaryInts = searcher.searchSIF(m);
			ExtendedSIFWriter.write(binaryInts, out);
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
