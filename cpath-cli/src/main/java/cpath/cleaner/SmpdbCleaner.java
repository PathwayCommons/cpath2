package cpath.cleaner;

//import cpath.service.CPathUtils;
import cpath.service.Cleaner;
//import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
//import org.biopax.paxtools.util.ClassFilterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;


/**
 * Implementation of Cleaner interface for the SMPDB BioPAX L3 pathway data.
 * 
 * Can normalize pathway URIs to, e.g., http://identifiers.org/smpdb/SMP00016
 * (this allows to then merge same pathways from different input files)
 */
final class SmpdbCleaner implements Cleaner {

    private static Logger log = LoggerFactory.getLogger(SmpdbCleaner.class);

    public void clean(InputStream data, OutputStream cleanedData)
	{	
		// create bp model from dataFile
		SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
		Model model = simpleReader.convertFromOWL(data);
		log.info("Cleaning SMPDB biopax file...");

// As we managed to get only human data archive from SMPDB there is no need for filtering by organism anymore -
//		/*
//		  Fail shortly if there is no TAXONOMY:9606 unif. xref,
//		  but there are other (non-human) BioSource objects
// 		  (some human data files out there have no human BioSource defined)
//		*/
//		if(!model.containsID(model.getXmlBase() + "Reference/TAXONOMY_9606")
//				&& !model.getObjects(BioSource.class).isEmpty())
//			throw new RuntimeException("Highly likely non-human datafile (skip).");

		// Normalize Pathway URIs KEGG stable id, where possible
		Set<Pathway> pathways = new HashSet<>(model.getObjects(Pathway.class));
//		final Map<Pathway, Pathway> replacements = new HashMap<>();
		for(Pathway pw : pathways) {
			//since 1-Apr-2018 - skip normalized pathways
			if(!pw.getUri().startsWith("http://identifiers.org/smpdb/"))
			{
				throw new RuntimeException("Unexpected (malformed) SMPDB pathway URI: " + pw.getUri());
//				Set<UnificationXref> uxrefs = new ClassFilterSet<>(new HashSet<>(pw.getXref()), UnificationXref.class);
//				//normally there are two unif. xrefs, e.g., SMP00016 and PW000149, per pathway
//				for (UnificationXref x : uxrefs) {
//					if (x.getId() == null)
//						continue;
//					;
//					if (x.getId().startsWith("SMP")) { // SMPDB 07-Jul-2015
//						String uri = "http://identifiers.org/smpdb/" + x.getId();
//						if (!model.containsID(uri)) {
//							CPathUtils.replaceID(model, pw, uri);
//						} else {
//							//collect to replace the duplicate with equivalent, normalized URI pathway
//							replacements.put(pw, (Pathway) model.getByID(uri));
//							model.remove(pw);
//						}
//						break;
//					} else if (x.getId().startsWith("http://identifiers.org/smpdb/")) { //SMPDB 05-Jun-2016
//						String uri = x.getId();
//						if (!model.containsID(uri)) {
//							CPathUtils.replaceID(model, pw, uri);
//						} else {
//							//collect to replace the duplicate with equivalent, normalized URI pathway
//							replacements.put(pw, (Pathway) model.getByID(uri));
//							model.remove(pw);
//						}
//						String id = uri.replaceFirst("http://identifiers.org/smpdb/", "");
//						x.setId(id);
//						break; //there must be only one such xref
//					}
//				}
			}
			//replace shortened ugly displayName with standardName
			pw.removeName("SubPathway");
			pw.removeName("SubPathwayOutput");
			pw.removeName("SubPathwayInput");
		}

//		ModelUtils.replace(model, replacements);
//		ModelUtils.removeObjectsIfDangling(model, UtilityClass.class);
		
		// convert model back to OutputStream for return
		try {
			simpleReader.convertToOWL(model, cleanedData);
		} catch (Exception e) {
			throw new RuntimeException("clean(), Exception thrown while saving cleaned data", e);
		}		
	}
}
