package cpath.cleaner.internal;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.ControlledVocabulary;
import org.biopax.paxtools.model.level3.PublicationXref;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.UtilityClass;
import org.biopax.paxtools.model.level3.XReferrable;
import org.biopax.paxtools.model.level3.Xref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.importer.Cleaner;

/**
 * Implementation of Cleaner interface for INOH data. 
 */
final class InohCleanerImpl implements Cleaner {	
	// logger
    private static Logger log = LoggerFactory.getLogger(InohCleanerImpl.class);

    public void clean(InputStream data, OutputStream cleanedData)
	{	
		// create bp model from dataFile
		SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
		simpleReader.mergeDuplicates(true); //there are objects with same URI in one file 
		//(e.g., EventRelation_IER_0000004_id1634539774 in signaling_Growth_hormone_signaling.owl; similar issues in -  
		//signaling_JAK_STAT_pathway_and_regulation.owl, /signaling_CD4_T_cell_receptor_signaling.owl, 
		//signaling_CD4_T_cell_receptor_signaling-JNK_cascade-.owl, and signaling_CD4_T_cell_receptor_signaling-NFkB_cascade-.owl) 
		
		Model model = simpleReader.convertFromOWL(data);
		log.info("Cleaning INOH data, please be patient...");

		// move some of unification xrefs from physical entity to entity reference
		for(SimplePhysicalEntity spe : new HashSet<SimplePhysicalEntity>(model.getObjects(SimplePhysicalEntity.class))) {			
			Set<Xref> xrefs = new HashSet<Xref>(spe.getXref());
			for(Xref x : xrefs) {
				if((x instanceof PublicationXref) || "INOH".equalsIgnoreCase(x.getDb()))
						continue; //leave as/where it is
				
				if(x.getDb()==null || x.getId()==null) { //just in case there are some...
					spe.removeXref(x);
					log.info("removed bad xref: " + x + " from " + spe.getRDFId());
					continue;
				}
				
				if("UniProt".equalsIgnoreCase(x.getDb()) && spe.getEntityReference() != null) {
					spe.removeXref(x);
					spe.getEntityReference().addXref(x);
					continue;
				}
				
				if(x instanceof UnificationXref) {
					RelationshipXref rXref = BaseCleaner.getOrCreateRxByUx((UnificationXref)x, model);
					spe.removeXref(x);
					spe.addXref(rXref);
				}
			}
		}
		
		//fix CV (InteractionVocabulary) terms that contain one of xref.id in them (e.g., "IEV_0000183:Transcription")
		for(ControlledVocabulary cv : model.getObjects(ControlledVocabulary.class)) {		
	terms:	for(String term : new HashSet<String>(cv.getTerm())) {
				for(Xref xref : cv.getXref()) {
					if(term.startsWith(xref.getId() + ":")) {
						cv.removeTerm(term);
						String newTerm = term.substring(xref.getId().length()+1);
						cv.addTerm(newTerm);
						log.info("replaced term '"+term+"' with '"+newTerm+"' in " + cv);
						continue terms;
					}
				}
			}
		}
		
		
		//fix shared UnificationXrefs
		Set<UnificationXref> uxrefs =  new HashSet<UnificationXref>(model.getObjects(UnificationXref.class));
		for(UnificationXref x : uxrefs) {
			if(x.getXrefOf().size() > 1) {
				//convert to RX, re-associate
				RelationshipXref rx = BaseCleaner.getOrCreateRxByUx(x, model);
				for(XReferrable owner : new HashSet<XReferrable>(x.getXrefOf())) {
					if(owner instanceof ControlledVocabulary)
						continue; //CVs can use same UX, but that means they are to merge...
					owner.removeXref(x);
					owner.addXref(rx);
				}			
				log.info("replaced UnificationXref " + x + " with RX " + rx);
			}
		}
		
		ModelUtils.removeObjectsIfDangling(model, UtilityClass.class);
		
		// convert model back to OutputStream for return
		try {
			simpleReader.convertToOWL(model, cleanedData);
		} catch (Exception e) {
			throw new RuntimeException("clean(), Exception thrown while saving cleaned NetPath data", e);
		}		
	}
}
