package cpath.cleaner.internal;

import cpath.dao.CPathUtils;
import cpath.importer.Cleaner;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.util.ClassFilterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;


/**
 * Implementation of Cleaner interface for the SMPDB BioPAX L3 pathway data
 * 
 * Can normalize pathway URIs to, e.g., http://identifiers.org/smpdb/SMP00016
 * (this allows to then merge same pathways from different input files)
 */
final class SmpdbCleanerImpl implements Cleaner {

    private static Logger log = LoggerFactory.getLogger(SmpdbCleanerImpl.class);

    public void clean(InputStream data, OutputStream cleanedData)
	{	
		// create bp model from dataFile
		SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
		Model model = simpleReader.convertFromOWL(data);
		log.info("Cleaning SMPDB biopax file...");

		// Normalize Pathway URIs KEGG stable id, where possible
		Set<Pathway> pathways = new HashSet<Pathway>(model.getObjects(Pathway.class));
		final Map<Pathway, Pathway> replacements = new HashMap<Pathway, Pathway>();
		for(Pathway pw : pathways) {
			Set<UnificationXref> uxrefs = new ClassFilterSet<Xref, UnificationXref>(
					new HashSet<Xref>(pw.getXref()), UnificationXref.class);
			//normally there are two unif. xrefs, e.g., SMP00016 and PW000149
			for(UnificationXref x : uxrefs) {
				if (x.getId() != null && x.getId().startsWith("SMP")) {
					String uri = "http://identifiers.org/smpdb/" + x.getId();
					if (!model.containsID(uri)) {
						CPathUtils.replaceID(model, pw, uri);
					} else {
						//collect to replace the duplicate with equivalent, normalized URI pathway
						replacements.put(pw, (Pathway) model.getByID(uri));
					}
					break;
				}
			}
			//replace shortened ugly displayName with standardName
			pw.removeName("SubPathway");
		}

		ModelUtils.replace(model, replacements);
		ModelUtils.removeObjectsIfDangling(model, Pathway.class);
		ModelUtils.removeObjectsIfDangling(model, UtilityClass.class);
		
		// convert model back to OutputStream for return
		try {
			simpleReader.convertToOWL(model, cleanedData);
		} catch (Exception e) {
			throw new RuntimeException("clean(), Exception thrown while saving cleaned data", e);
		}		
	}
}
