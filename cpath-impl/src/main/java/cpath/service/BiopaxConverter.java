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


package cpath.service;

import java.io.*;
import java.util.*;

import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.io.gsea.GSEAConverter;
import org.biopax.paxtools.io.sbgn.L3ToSBGNPDConverter;
import org.biopax.paxtools.io.sbgn.ListUbiqueDetector;
import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.Provenance;
import org.biopax.paxtools.pattern.miner.OldFormatWriter;
import org.biopax.paxtools.pattern.miner.SIFEnum;
import org.biopax.paxtools.pattern.miner.SIFInteraction;
import org.biopax.paxtools.pattern.miner.SIFSearcher;
import org.biopax.paxtools.pattern.util.Blacklist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.service.jaxb.DataResponse;
import cpath.service.jaxb.ServiceResponse;
import cpath.service.ErrorResponse;
import cpath.service.OutputFormat;
import static cpath.service.Status.*;

import javax.xml.bind.JAXBException;

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
	
	private boolean mergeEquivalentInteractions;
		
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
	 * For the given BioPAX data stream, converts it to the 
	 * desired output format.
	 *
	 * @param biopax
	 * @param format
	 * @return
	 */
    public ServiceResponse convert(InputStream biopax, OutputFormat format) {
        Model model = (new SimpleIOHandler()).convertFromOWL(biopax);
        if(model != null && !model.getObjects().isEmpty()) {
            return convert(model, format);
        } else {
        	return new ErrorResponse(NO_RESULTS_FOUND, "Empty BioPAX Model.");
        }
	}

    
	/**
     * Converts the BioPAX data into the other format.
     * 
     * @param m paxtools model
     * @param format output format
     * 
     * @return data response with the converted data or {@link ErrorResponse}.
     */
    public ServiceResponse convert(Model m, OutputFormat format, Object... args) 
    {
    	if(m == null || m.getObjects().isEmpty()) {
			return new ErrorResponse(NO_RESULTS_FOUND, "Empty BioPAX Model");
		}

		// otherwise, do convert (it's a DataResponse)
    	String data = null;
    	try {
			switch (format) {
			case BIOPAX: //to OWL (RDF/XML)
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				new SimpleIOHandler().convertToOWL(m, baos);
				data = baos.toString();
				break;
			case BINARY_SIF:
				data = convertToBinarySIF(m, false);
				break;
			case EXTENDED_BINARY_SIF:
				data = convertToBinarySIF(m, true);
				break;
			case GSEA:
				data = convertToGSEA(m, "uniprot");
				break;
            case SBGN:
				boolean doLayout = true;
				if (args != null && args.length > 0 && args[0] instanceof Boolean)
					doLayout = (Boolean) args[0];
				
                data = convertToSBGN(m, blacklist, doLayout);
                break;
			default: throw new UnsupportedOperationException(
					"convert, yet unsupported format: " + format);
			}
			
			DataResponse dataResponse = new DataResponse();
			dataResponse.setData(data);
			// extract and save data providers' names
			dataResponse.setProviders(providers(m));
			
			return dataResponse;
		}
        catch (Exception e) {
        	return new ErrorResponse(INTERNAL_ERROR, e);
		}
    }


    /**
     * Converts a BioPAX Model to SBGN format.
     *
     * @param m
     * @param blackList
     * @param doLayout
     * @return
     * @throws IOException
     */
    String convertToSBGN(Model m, Blacklist blackList, boolean doLayout)
		throws IOException, JAXBException
	{

		if(mergeEquivalentInteractions)
			ModelUtils.mergeEquivalentInteractions(m);
    	
    	L3ToSBGNPDConverter converter = new L3ToSBGNPDConverter(
			new ListUbiqueDetector(blackList.getListed()), null, doLayout);

		OutputStream stream = new ByteArrayOutputStream();
		converter.writeSBGN(m, stream);
        return stream.toString();
    }


    /**
	 * Converts service results that contain 
	 * a not empty BioPAX Model to GSEA format.
	 * 
     * @param m paxtools model
	 * @param outputIdType output identifiers type (db name)
	 * @return
	 * @throws IOException 
	 */
	String convertToGSEA(Model m, String outputIdType) throws IOException 
	{	
		// convert, replace DATA
		GSEAConverter gseaConverter = new GSEAConverter(outputIdType, true);
		OutputStream stream = new ByteArrayOutputStream();
		gseaConverter.writeToGSEA(m, stream);
		return stream.toString();
	}

	
	/**
	 * Converts a not empty BioPAX Model (contained in the service bean) 
	 * to the SIF or <strong>single-file</strong> extended SIF format.
	 * 
	 * This method is primarily designed for the web service.
	 * 
     * @param m biopax paxtools to convert
     * @param extended if true, calls SIFNX else - SIF
	 * @return
	 * @throws IOException 
	 */
	String convertToBinarySIF(Model m, boolean extended) throws IOException 
	{
		if(mergeEquivalentInteractions)
			ModelUtils.mergeEquivalentInteractions(m);

		SIFSearcher searcher = new SIFSearcher(SIFEnum.values());
		searcher.setBlacklist(blacklist);
		OutputStream out = new ByteArrayOutputStream();
				
		if (extended) {
			Set<SIFInteraction> binaryInts = searcher.searchSIF(m);
			OldFormatWriter.write(binaryInts, out);
		} else {
			searcher.searchSIF(m, out);
		}
		
		return out.toString();
	}

	
	/**
	 * Sets whether to run {@link ModelUtils#mergeEquivalentInteractions(Model)}
	 * before converting a biopax model to another format.
	 * Warn: use with care (or do not use) with the main (persistent) biopax model.
	 * 
	 * @param mergeEquivalentInteractions
	 */
	public void mergeEquivalentInteractions(
			boolean mergeEquivalentInteractions) {
		this.mergeEquivalentInteractions = mergeEquivalentInteractions;
	}

	
	/**
	 * The list of datasources (data providers)
	 * the BioPAX model contains.
	 * 
	 * @param m
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
//				log.info("DATASOURCE " + dsNames.toString());
			}
		}
		
		return (names != null && !names.isEmpty()) 
				? names : Collections.EMPTY_SET;
	}
}
