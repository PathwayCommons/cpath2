package cpath.cleaner.internal;

import cpath.warehouse.beans.PathwayData;

import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.io.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Set;
import java.util.HashSet;

import java.io.*;

/**
 * Implementation of Cleaner interface for Reactome data. 
 *
 * @deprecated Reactome (2011/09) now uses different xml:base suffix for different species and consistent IDs!
// * The following is not a problem with Reactome anymore! 
// * For all Entity elements, replace RDF id with id that will
// * not clash with other id's in model or other id's across
// * species files belonging to the same data provider.
//
// * This was motivated by wordy RDF id's used by Reactome elements which 
// * may only differ by the case of a single character:
// *
// * HIV_1_aborted_elongation_complex_after_arrest__nucleoplasm_
// * HIV_1_Aborted_elongation_complex_after_arrest__nucleoplasm_
// *
// * or
// *
// * Active_Calmodulin__name_copied_from_entity_in_Homo_sapiens___nucleoplasm_
// * active_Calmodulin__name_copied_from_entity_in_Homo_sapiens___nucleoplasm_
// *
// * then subsequently caused primary key conflicts since (by default) mysql does not
// * differentiate between upper and lower case on primary keys.
 */
@Deprecated
final class ReactomeCleanerImpl extends BaseCleanerImpl {
	
	// logger
    private static Log log = LogFactory.getLog(ReactomeCleanerImpl.class);

	/**
	 * (non-Javadoc>
	 * @see cpath.importer.Cleaner#clean(PathwayData)
	 */
    @Override
	public String clean(final String pathwayData) 
	{	
		// create bp model from pathwayData
		InputStream inputStream =
			new BufferedInputStream(new ByteArrayInputStream(pathwayData.getBytes()));
		SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
		Model model = simpleReader.convertFromOWL(inputStream);
			
		if (log.isInfoEnabled()) {
			log.info("Cleaning Reactome data, this may take some time, please be patient...");
		}
		
// since we're using new md5 primary key, no need to replace/shorten URIs!		
// code was removed here...
		
		
		// convert model back to OutputStream for return
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			simpleReader.convertToOWL(model, outputStream);
		}
		catch (Exception e) {
			throw new RuntimeException("clean(), Exception thrown while cleaning pathway data!", e);
		}

		return outputStream.toString();
	}

}
