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
import org.biopax.paxtools.model.level3.Conversion;
import org.biopax.paxtools.model.level3.ConversionDirectionType;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.PublicationXref;
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
 * Implementation of Cleaner interface for Reactome data. 
 * 
 * Can normalize URIs for Reactome Entity class objects 
 * to http://identifiers.org/reactome/REACT_* form
 * if the unification xref with the stable Reactome ID is attached. 
 */
final class ReactomeCleanerImpl implements Cleaner {
	
	// logger
    private static Logger log = LoggerFactory.getLogger(ReactomeCleanerImpl.class);

    public void clean(InputStream data, OutputStream cleanedData)
	{	
		// create bp model from dataFile
		SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
		Model model = simpleReader.convertFromOWL(data);
		log.info("Cleaning Reactome data, this may take some time, please be patient...");

		// (why the data provider could not always do this?)	
		// Normalize Entity URIs using Reactome stable id, where possible
		// (not required for utility class objects)
		Set<Entity> entities = new HashSet<Entity>(model.getObjects(Entity.class));
		Map<String, Entity> newUriToEntityMap = new HashMap<String, Entity>();
		for(Entity ent : entities) {
			Set<UnificationXref> uxrefs = new ClassFilterSet<Xref, UnificationXref>(
					new HashSet<Xref>(ent.getXref()), UnificationXref.class);			
			for(UnificationXref x : uxrefs) {
				if(x.getId() != null && x.getId().startsWith("REACT_")) {
					String id = x.getId();
					if(x.getIdVersion() != null && !x.getId().contains(".")) 
						id += "." + x.getIdVersion();
					
					String uri = "http://identifiers.org/reactome/" + id;
					
					if(!model.containsID(uri) && !newUriToEntityMap.containsKey(uri)) {
						newUriToEntityMap.put(uri, ent); //collect to replace URIs later (below)
					} else { //shared unification xref bug
						log.warn("Fixing Reactome: " + x.getId() + 
							" unification xref is shared by several entities: "
								+ x.getXrefOf());
							
						RelationshipXref rx = BaseCleaner.getOrCreateRxByUx(x, model);
						for(XReferrable owner : new HashSet<XReferrable>(x.getXrefOf())) {
							if(owner.equals(newUriToEntityMap.get(uri)))
								continue; //keep the entity to be updated unchanged
							owner.removeXref(x);
							owner.addXref(rx);
						}						
					}
					break; //skip the rest of xrefs, if any (must not have >1 REACT_* unif. xref for the same entity)
				}
			}
		}
		
		// set standard URIs for selected entities
		for(String uri : newUriToEntityMap.keySet())
			CPathUtils.replaceID(model, newUriToEntityMap.get(uri), uri);
		
		// All Conversions in Reactome are LEFT-TO-RIGH, 
		// unless otherwise was specified (confirmed with Guanming Wu, 2013/12)
		Set<Conversion> conversions = new HashSet<Conversion>(model.getObjects(Conversion.class));
		for(Conversion ent : conversions) {
			if(ent.getConversionDirection() == null)
				ent.setConversionDirection(ConversionDirectionType.LEFT_TO_RIGHT);
		}
		
		// Remove unstable UnificationXrefs like "Reactome Database ID Release XX"
		// if there is a stable one in the same object
		Set<Xref> xrefsToRemove = new HashSet<Xref>();
		for(Xref xref: new HashSet<Xref>(model.getObjects(Xref.class))) {
			if(xref.getDb() != null && xref.getDb()
				.toLowerCase().startsWith("reactome database id")) 
			{
				//remove the long comment (save some RAM)
				if(!(xref instanceof PublicationXref))
					xref.getComment().clear();
				
				//proceed with a unification xref only...
				if(xref instanceof UnificationXref) {
					for(XReferrable owner :  new HashSet<XReferrable>(xref.getXrefOf())) {
						for(Xref x : new HashSet<Xref>(owner.getXref())) {
							if(!(x instanceof UnificationXref) || x.equals(xref)) 
								continue;
							//another unif. xref present in the same owner object
							if(x.getDb() != null && x.getDb().toLowerCase().startsWith("reactome")
									&& x.getId()!= null && x.getId().startsWith("REACT_")) {
								//remove the unstable ID ref from the object
								owner.removeXref(xref); 
								xrefsToRemove.add(xref);
								//(it's ok to keep only this stable x)
							}
						}
					}
				}
			}
		}
		log.info(xrefsToRemove.size() + " unstable unif. xrefs, where a stable one also exists, " +
			"were removed from the corresponding xref properties.");
		
		ModelUtils.removeObjectsIfDangling(model, UtilityClass.class);
		
		// convert model back to OutputStream for return
		try {
			simpleReader.convertToOWL(model, cleanedData);
		} catch (Exception e) {
			throw new RuntimeException("clean(), Exception thrown while saving cleaned Reactome data", e);
		}
		
	}

}
