package cpath.cleaner.internal;

// imports
import cpath.cleaner.Cleaner;

import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.io.simpleIO.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Set;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Implementation of Cleaner interface for Reactome data. 
 *
 * For ALL biopax element, such as:
 *
 * <bp:Complex rdf:ID="xxxx">
 * ...
 * ...
 * <bp:xref rdf:resource="#REACT_6602.2" />
 * </bp:Complex>
 *
 * replaces rdf:ID="xxxx" with rdf:ID="#REACT_6602.2" -
 * if xref to REACT exists.  All references to element are
 * properly updated with the help of paxtools.
 *
 * This was motivated by wordy rdf id's used by reactome elements which 
 * may only differ by the case of a single character:
 *
 * http://www.reactome.org/biopax#HIV_1_aborted_elongation_complex_after_arrest__nucleoplasm_
 * http://www.reactome.org/biopax#HIV_1_Aborted_elongation_complex_after_arrest__nucleoplasm_
 *
 * then subsequently caused primary key conflicts since (by default) mysql does not
 * differenciate between upper and lower case on primary keys.
 */
public class ReactomeCleanerImpl extends BaseCleanerImpl implements Cleaner {
	
	// logger
    private static Log log = LogFactory.getLog(ReactomeCleanerImpl.class);
    
    private static final String RDF_ID_REPLACEMENT = "urn:miriam:reactome_";
    private static final Pattern REACTOME_XREF_REGEX = Pattern.compile("REACT_\\d+(\\.\\d+)?");

	/**
	 * (non-Javadoc>
	 * @see cpath.cleaner.Cleaner#clean(java.lang.String)
	 */
	public String clean(final String pathwayData) {
		
		// create bp model from pathwayData
		InputStream inputStream =
			new BufferedInputStream(new ByteArrayInputStream(pathwayData.getBytes()));
		SimpleReader simpleReader = new SimpleReader(BioPAXLevel.L3);
		Model bpModel = simpleReader.convertFromOWL(inputStream);
		
		Set<BioPAXElement> sourceElements = bpModel.getObjects();
		for (BioPAXElement bpe : sourceElements) {
			if (bpe instanceof XReferrable) {
				Set<Xref> xrefs = ((XReferrable)bpe).getXref();
				// look for REACT xref
				for (Xref xref : xrefs) {
					if (xref.getId() == null) {
						if (log.isDebugEnabled()) {
							log.debug("clean(), Encountered xref without any id!, bpe: " +
                                      bpe.getRDFId() + " skipping rdf id replacement...");
						}
						continue;
					}
					Matcher reactXref = REACTOME_XREF_REGEX.matcher(xref.getId());
	                if (reactXref.find()) {
	                	String newRDF = RDF_ID_REPLACEMENT + xref.getId();
	                	String idVersion = xref.getIdVersion();
	                	if (idVersion != null & idVersion.length() > 0) {
	                		newRDF += '.' + idVersion;
	                	}
	                	// replace rdf of bpe with rdf:ID of xref (should be of the form REACT_XXXX)
	                	bpe.setRDFId(newRDF);
	                }
				}
			}
		}
		
		// convert model back to outputstream for return
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			SimpleExporter simpleExporter = new SimpleExporter(BioPAXLevel.L3);
			simpleExporter.convertToOWL(bpModel, outputStream);
		}
		catch (IOException e) {
			if (log.isInfoEnabled()) {
				log.info("clean(), Exception thrown while cleaning pathway data, returning dirty data...");
			}
			return pathwayData;
		}
		
		// outta here
		return outputStream.toString();
	}
}
