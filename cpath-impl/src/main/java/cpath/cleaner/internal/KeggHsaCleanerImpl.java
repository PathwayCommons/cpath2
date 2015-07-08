package cpath.cleaner.internal;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Pathway;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.UtilityClass;
import org.biopax.paxtools.model.level3.XReferrable;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.paxtools.util.ClassFilterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.dao.CPathUtils;
import cpath.importer.Cleaner;


/**
 * Implementation of Cleaner interface 
 * for converted to BioPAX KEGG human pathway data
 * (provided by BioModels, Andreas Draeger).
 * 
 * Can normalize URIs for KEGG Pathways 
 * to http://identifiers.org/kegg.pathway/hsa* 
 * 
 * TODO add/remove features as needed...
 */
final class KeggHsaCleanerImpl implements Cleaner {

    private static Logger log = LoggerFactory.getLogger(KeggHsaCleanerImpl.class);

    public void clean(InputStream data, OutputStream cleanedData)
	{	
		// create bp model from dataFile
		SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
		Model model = simpleReader.convertFromOWL(data);
		log.info("Cleaning KEGG biopax data, please wait...");

		// Normalize Pathway URIs KEGG stable id, where possible
		Set<Pathway> entities = new HashSet<Pathway>(model.getObjects(Pathway.class));
		Map<String, Pathway> newUriToEntityMap = new HashMap<String, Pathway>();
		for(Pathway ent : entities) {
			Set<UnificationXref> uxrefs = new ClassFilterSet<Xref, UnificationXref>(
					new HashSet<Xref>(ent.getXref()), UnificationXref.class);
			//normally there is only one such xref
			if(!uxrefs.isEmpty()) {
				UnificationXref x = uxrefs.iterator().next();
				if(x.getId() != null && x.getId().startsWith("hsa")) {
					String uri = "http://identifiers.org/kegg.pathway/" + x.getId();
					
					if(!model.containsID(uri) && !newUriToEntityMap.containsKey(uri)) {
						newUriToEntityMap.put(uri, ent); //collect to replace URIs later (below)
					} else { //shared unification xref bug
						log.warn("Fixing: " + x.getId() + 
							" unification xref is shared by several entities: "
								+ x.getXrefOf());
						
						RelationshipXref rx = BaseCleaner.getOrCreateRx(x, model);						
						for(XReferrable owner : new HashSet<XReferrable>(x.getXrefOf())) {
							if(owner.equals(newUriToEntityMap.get(uri)))
								continue; //keep the entity to be updated unchanged
							owner.removeXref(x);
							owner.addXref(rx);
						}						
					}
				}
			}
			
			String standardName = ent.getStandardName();
			if(!(standardName == null || standardName.trim().isEmpty()) ) {
				//replace shortened/truncated pathway names
				ent.setDisplayName(standardName);
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
