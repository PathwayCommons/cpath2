package cpath.cleaner;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.util.ClassFilterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.service.CPathUtils;
import cpath.service.api.Cleaner;

import org.biopax.paxtools.model.level3.Process;

/**
 * Implementation of Cleaner interface for Reactome data. 
 * 
 * Can normalize URIs for some Reactome Entity class objects (pathways, interaction)
 * to bioregistry.io/reactome:R-* form if a unification xref with the stable Reactome ID is found.
 * Removes "unstable" Reactome ID xref from objects where a stable ID is present.
 */
final class ReactomeCleaner implements Cleaner {
  private static Logger log = LoggerFactory.getLogger(ReactomeCleaner.class);

  public void clean(InputStream data, OutputStream cleanedData)
	{	
		// import the original Reactome BioPAX model from file
		log.info("Cleaning Reactome data...");
		SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
		Model model = simpleReader.convertFromOWL(data);
		// Normalize pathway URIs, where possible, using Reactome stable IDs
		// Since v54, Reactome stable ID format has been changed to like: "R-HSA-123456"
		final Map<String, Entity> newUriToEntityMap = new HashMap<>();
		final Set<Process> processes = new HashSet<>(model.getObjects(Process.class));

		for(Process proc : processes)
		{
			if (StringUtils.containsAny(proc.getUri(),
					"identifiers.org/reactome", "bioregistry.io/reactome")) {
				continue; //skip for already normalized pathway or interaction
			}
			final Set<UnificationXref> uxrefs = new ClassFilterSet<>(new HashSet<>(proc.getXref()), UnificationXref.class);
			for (UnificationXref x : uxrefs) {
				if (StringUtils.equalsIgnoreCase(x.getDb(),"reactome")) {
					String stableId = x.getId();
					//remove 'REACTOME:' (length=9) prefix if present (it's optional - according to MIRIAM/Bioregistry)
					if (stableId.startsWith("REACTOME:")) {
						stableId = stableId.substring(9);
						// stableID is like 'R-HSA-123456'
					}
					final String uri = "bioregistry.io/reactome:" + stableId;
					if (!model.containsID(uri) && !newUriToEntityMap.containsKey(uri)) {
						//save it in the map to replace the URI later (see below)
						newUriToEntityMap.put(uri, proc);
					} else { //fix the 'shared unification xref' problem right away
						log.warn("Fixing " + x.getId() + " UX that's shared by several objects: " + x.getXrefOf());
						RelationshipXref rx = BaseCleaner.getOrCreateRx(x, model);
						for (XReferrable owner : new HashSet<>(x.getXrefOf())) {
							if (owner.equals(newUriToEntityMap.get(uri)))
								continue; //keep the entity to be updated unchanged
							owner.removeXref(x);
							owner.addXref(rx);
						}
					}
					break; //skip the rest of xrefs (mustn't have multiple 'Reactome' UXs on the same entity)
				}
			}
		}

		// set standard URIs for selected entities (processes);
		for(String uri : newUriToEntityMap.keySet()) {
			CPathUtils.replaceUri(model, newUriToEntityMap.get(uri), uri);
		}
		
		// All Conversions in Reactome are LEFT-TO-RIGH, 
		// unless otherwise was specified (confirmed with Guanming Wu, 2013/12)
		final Set<Conversion> conversions = new HashSet<>(model.getObjects(Conversion.class));
		for(Conversion ent : conversions) {
			if(ent.getConversionDirection() == null)
				ent.setConversionDirection(ConversionDirectionType.LEFT_TO_RIGHT);
		}
		
		// Remove unstable UnificationXrefs like "Reactome Database ID Release 65"
		// if there is a stable xref in the same object
		// Since Reactome v54, stable ID format is different (not like REACT_12345...)
		final Set<Xref> xrefsToRemove = new HashSet<>();
		for(Xref xref: new HashSet<>(model.getObjects(Xref.class)))
		{
			if(StringUtils.startsWithIgnoreCase(xref.getDb(),"reactome database"))
			{
				//remove the long comment (save some RAM)
				if(!(xref instanceof PublicationXref)) {
					xref.getComment().clear();
				}
				for(XReferrable owner :  new HashSet<>(xref.getXrefOf())) {
					for(Xref x : new HashSet<>(owner.getXref())) {
						if(StringUtils.equalsIgnoreCase(x.getDb(), "reactome")) {
							//if a standard "reactome" xref is also present in the same owner object,
							//then remove the unstable ID xref from that object
							owner.removeXref(xref);
							xrefsToRemove.add(xref);
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
			throw new RuntimeException("clean(), failed saving the cleaned Reactome model", e);
		}
	}
}
