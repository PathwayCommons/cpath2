package cpath.cleaner;

import cpath.service.Cleaner;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Pathway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;

/**
 * Implementation of Cleaner interface for the CTD BioPAX L3 model,
 * which is generated with pathwaycommons:ctd-to-biopax converter tool
 * (java console app, run separately).
 * 
 * This is mainly to remove "pathways", such as '#taxon_pathway_9606',
 * which are nor really useful and even bad for, e.g., SIF and GSEA converters.
 */
final class CtdCleaner implements Cleaner {

    private static Logger log = LoggerFactory.getLogger(CtdCleaner.class);

    public void clean(InputStream data, OutputStream cleanedData)
	{	
		SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
		Model model = simpleReader.convertFromOWL(data);
		log.info("Cleaning CTD biopax...");

		for(Pathway pw : new HashSet<Pathway>(model.getObjects(Pathway.class))) {
			if(pw.getUri().contains("taxon_pathway_")) {
				model.remove(pw);
			}
		}

		try {
			simpleReader.convertToOWL(model, cleanedData);
		} catch (Exception e) {
			throw new RuntimeException("clean(), Exception thrown while saving cleaned data", e);
		}		
	}
}
