package cpath.cleaner;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Control;
import org.biopax.paxtools.model.level3.ControlledVocabulary;
import org.biopax.paxtools.model.level3.PathwayStep;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.PublicationXref;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;
import org.biopax.paxtools.model.level3.TemplateReaction;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.UtilityClass;
import org.biopax.paxtools.model.level3.XReferrable;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.paxtools.util.ClassFilterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.service.Cleaner;

/**
 * Implementation of Cleaner interface for INOH data. 
 */
final class InohCleaner implements Cleaner {
	// logger
    private static Logger log = LoggerFactory.getLogger(InohCleaner.class);

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
		
		//convert "UniProt" PublichationXrefs (usually owned by some Evidence) to Rel.Xrefs.
		for(PublicationXref x : new HashSet<PublicationXref>(model.getObjects(PublicationXref.class))) {
			if("UniProt".equalsIgnoreCase(x.getDb())) {
				RelationshipXref rx = BaseCleaner.getOrCreateRx(x, model);
				for(XReferrable owner : new HashSet<XReferrable>(x.getXrefOf())) {
					owner.removeXref(x);
					owner.addXref(rx);
					log.debug("replaced PX " + x + " with RX " + rx);
				}
			}
		}

		// using PhysicalEntity instead SimplePhysicalEntity below also fixes for Complexes' xrefs;
		// move some of unification xrefs from physical entity to entity reference
		for(PhysicalEntity spe : new HashSet<PhysicalEntity>(model.getObjects(PhysicalEntity.class))) {			
			Set<UnificationXref> xrefs = new ClassFilterSet<Xref,UnificationXref>(new HashSet<Xref>(spe.getXref()), UnificationXref.class);
			Set<UnificationXref> proteinUniprotUnifXrefs = new HashSet<UnificationXref>();
			//first pass (do not move/convert proteins' uniprot unif. xrefs yet)
			for(UnificationXref x : xrefs) {
				if("INOH".equalsIgnoreCase(x.getDb())) 
					continue; //leave as/where it is
				
				if(x.getDb()==null || x.getId()==null) { //just in case there are some...
					spe.removeXref(x);
					log.debug("removed bad xref: " + x + " from " + spe.getUri());
					continue;
				}
				
				spe.removeXref(x);
				
				if("UniProt".equalsIgnoreCase(x.getDb()) && spe instanceof Protein) {
					Protein p = (Protein) spe;
					ProteinReference pr = (ProteinReference) p.getEntityReference();
					if(pr == null) {
						pr = model.addNew(ProteinReference.class, p.getUri()+"_ref");
						p.setEntityReference(pr);
						pr.getName().addAll(spe.getName());
					}
					proteinUniprotUnifXrefs.add(x);
					continue;
				}

				RelationshipXref rx = BaseCleaner.getOrCreateRx(x, model);
				spe.addXref(rx);
				if(spe instanceof SimplePhysicalEntity && ((SimplePhysicalEntity)spe).getEntityReference() != null) {
					((SimplePhysicalEntity)spe).getEntityReference().addXref(rx);
				} 
			}
			
			//second pass - process protein's uniprot unif. xrefs
			//(if proteinUniprotUnifXrefs is not empty, then spe is a Protein that has not null PR;
			// also, x was removed from the spe; see above)
			for(UnificationXref x : proteinUniprotUnifXrefs) {
				Protein p = (Protein) spe;
				ProteinReference pr = (ProteinReference) p.getEntityReference();
				if(proteinUniprotUnifXrefs.size() > 1) {
					addMember(pr, x, model);
				} else {
					pr.addXref(x); //it's the only xref out there
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
						log.debug("replaced term '"+term+"' with '"+newTerm+"' in " + cv);
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
				RelationshipXref rx = BaseCleaner.getOrCreateRx(x, model);
				for(XReferrable owner : new HashSet<XReferrable>(x.getXrefOf())) {
					if(owner instanceof ControlledVocabulary)
						continue; //CVs can use same UX, but that means they are to merge...
					owner.removeXref(x);
					owner.addXref(rx);
				}			
				log.debug("replaced UnificationXref " + x + " with RX " + rx);
			}
		}
		
		//remove all TRs where template is null (due to illegal property range, Complex values were ignored by the reader)
		for(TemplateReaction tr : new HashSet<TemplateReaction>(model.getObjects(TemplateReaction.class))) {
			if(tr.getTemplate() == null) {
				model.remove(tr);
				for(PathwayStep ps : new HashSet<PathwayStep>(tr.getStepProcessOf())) {
					ps.removeStepProcess(tr);
				}		
				for(Control co : new HashSet<Control>(tr.getControlledOf())) {
					co.removeControlled(tr);
				}
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

	/*
	 * Creates or gets a member ProteinReference from the (UniProt) 
	 * UnificationXref and adds it to the existing ProteinReference.
	 */
    private void addMember(ProteinReference pr, UnificationXref x, Model model) {
    	final String xmlbase = (model.getXmlBase() != null) ? model.getXmlBase() : "";
		String id = x.getId();
		if(x.getIdVersion() != null) id += "." + x.getIdVersion();
		String db = x.getDb();
		if(x.getDbVersion() != null) db += "." + x.getDbVersion();
		String uri = xmlbase + "memberPR_" + BaseCleaner.encode(db + "_"+ id);		
		ProteinReference mpr = (ProteinReference) model.getByID(uri);
		if(mpr == null) { //make a new one
			mpr = model.addNew(ProteinReference.class, uri);
			mpr.addXref(x);
		}	
		pr.addMemberEntityReference(mpr);
	}
}
