package cpath.cleaner.internal;

// imports
import cpath.cleaner.Cleaner;

import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.io.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Set;
import java.util.HashSet;

import java.io.*;

/**
 * Implementation of Cleaner interface for Reactome data. 
 *
 * For all Entity elements, replace RDF id with id that will
 * not clash with other id's in model or other id's across
 * species files belonging to the same data provider.
 * See getRDFIdReplacement in the base cleaner class.

 * This was motivated by wordy RDF id's used by Reactome elements which 
 * may only differ by the case of a single character:
 *
 * HIV_1_aborted_elongation_complex_after_arrest__nucleoplasm_
 * HIV_1_Aborted_elongation_complex_after_arrest__nucleoplasm_
 *
 * or
 *
 * Active_Calmodulin__name_copied_from_entity_in_Homo_sapiens___nucleoplasm_
 * active_Calmodulin__name_copied_from_entity_in_Homo_sapiens___nucleoplasm_
 *
 * then subsequently caused primary key conflicts since (by default) mysql does not
 * differentiate between upper and lower case on primary keys.
 */
public class ReactomeCleanerImpl extends BaseCleanerImpl implements Cleaner {
	
	// logger
    private static Log log = LogFactory.getLog(ReactomeCleanerImpl.class);

	/**
	 * (non-Javadoc>
	 * @see cpath.cleaner.Cleaner#clean(java.lang.String)
	 */
	public String clean(final String pathwayData) 
	{	
		// create bp model from pathwayData
		InputStream inputStream =
			new BufferedInputStream(new ByteArrayInputStream(pathwayData.getBytes()));
		SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
		Model model = simpleReader.convertFromOWL(inputStream);
		
		// get the tax id for the model, we will append this to id (prevent clashes across species)
		String taxID = super.getTaxID(model);
		if (taxID == null) {
			if (log.isInfoEnabled()) {
				log.info("clean(), a taxonomy ID cannot be found while cleaning pathway data, returning dirty data...");
			}
			return pathwayData;
		}
		
		if (log.isInfoEnabled()) {
			log.info("Cleaning Reactome data, this may take some time, please be patient...");
		}
		ModelUtils modelUtils = new ModelUtils(model);
		Set<Level3Element> sourceElements = new HashSet<Level3Element>(model.getObjects(Level3Element.class));
		for (Level3Element l3e : sourceElements) {
			// we only modify entity and pathway step rdfIds
			if (l3e instanceof Entity || l3e instanceof PathwayStep) {
				String newRDFId = super.getRDFIdReplacement(model, l3e, taxID);
				// before update, store original in a comment
				l3e.addComment("Original RDFId (before applying ReactomeCleaner): " + l3e.getRDFId());
				//quickly replace ID
				modelUtils.replaceID(l3e.getRDFId(), newRDFId);
			}
		}
		//repair model (add lost children, fix properties and idMap...)
		model.repair();
		
		// update ns prefix (to upper case) as well -
		String xmlns = model.getNameSpacePrefixMap().remove("");
		model.getNameSpacePrefixMap().put("", xmlns.toUpperCase());
		
		// convert model back to OutputStream for return
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			simpleReader.convertToOWL(model, outputStream);
		}
		catch (Exception e) {
			if (log.isInfoEnabled()) {
				log.info("clean(), Exception thrown while cleaning pathway data, returning dirty data...");
			}
			//TODO why to return dirty data? Why not letting the (runtime) exception propagate...
			return pathwayData;
		}

		// outta here
		return outputStream.toString();
	}
}
