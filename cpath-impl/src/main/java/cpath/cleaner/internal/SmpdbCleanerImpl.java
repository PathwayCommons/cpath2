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
		log.info("Cleaning SMPDB biopax data, please wait...");

		// Normalize Pathway URIs KEGG stable id, where possible
		Set<Pathway> pathways = new HashSet<Pathway>(model.getObjects(Pathway.class));
		Map<String, Pathway> newUriToEntityMap = new HashMap<String, Pathway>();
		for(Pathway pw : pathways) {
			Set<UnificationXref> uxrefs = new ClassFilterSet<Xref, UnificationXref>(
					new HashSet<Xref>(pw.getXref()), UnificationXref.class);
			//normally there are two unif. xrefs, e.g., SMP00016 and PW000149
			for(UnificationXref x : uxrefs) {
				if (x.getId() != null && x.getId().startsWith("SMP")) {
					String uri = "http://identifiers.org/smpdb/" + x.getId();
					if (!model.containsID(uri) && !newUriToEntityMap.containsKey(uri)) {
						newUriToEntityMap.put(uri, pw); //collect to replace URIs later (below)
					} else { //shared unification xref bug
						log.warn("Fixing: " + x.getId() +
								" unification xref is shared by several entities: " + x.getXrefOf());
						RelationshipXref rx = BaseCleaner.getOrCreateRx(x, model);
						for (XReferrable owner : new HashSet<XReferrable>(x.getXrefOf())) {
							if (owner.equals(newUriToEntityMap.get(uri)))
								continue; //keep the entity to be updated unchanged
							owner.removeXref(x);
							owner.addXref(rx);
						}
					}
					break;
				}
			}
			//replace shortened ugly displayName with standardName
			pw.removeName("SubPathway");
		}

		//unlink from a SimplePhysicalEntity Xrefs that also belong to the entity reference (if not null)
		for(SimplePhysicalEntity spe : model.getObjects(SimplePhysicalEntity.class)) {
			EntityReference er = spe.getEntityReference();
			if(er != null) {
				for(Xref x : new HashSet<Xref>(spe.getXref())) {
					if(er.getXref().contains(x)) {
						spe.removeXref(x);
					}
				}
			}
		}

		// set standard URIs for selected entities
		for(String uri : newUriToEntityMap.keySet())
			CPathUtils.replaceID(model, newUriToEntityMap.get(uri), uri);
			
		ModelUtils.removeObjectsIfDangling(model, UtilityClass.class);
		
		// convert model back to OutputStream for return
		try {
			simpleReader.convertToOWL(model, cleanedData);
		} catch (Exception e) {
			throw new RuntimeException("clean(), Exception thrown while saving cleaned data", e);
		}		
	}
}
