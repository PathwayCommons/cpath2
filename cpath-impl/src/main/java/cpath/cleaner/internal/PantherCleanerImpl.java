package cpath.cleaner.internal;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.biopax.paxtools.controller.Cloner;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.Control;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.Pathway;
import org.biopax.paxtools.model.level3.Process;
import org.biopax.paxtools.model.level3.SequenceEntityReference;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;
import org.biopax.paxtools.model.level3.UtilityClass;
import org.biopax.paxtools.util.ClassFilterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import cpath.dao.CPathUtils;
import cpath.importer.Cleaner;

/**
 * Cleaner for PANTHER Pathway 3.3. 
 * 
 * Remove non-human entity references. Add "dangling" Controls to Pathways.
 */
final class PantherCleanerImpl implements Cleaner {
	
    private static Logger log = LoggerFactory.getLogger(PantherCleanerImpl.class);

    @Override
	public String clean(final String pathwayData) 
	{	
		InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(pathwayData.getBytes()));
		SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
		Model originalModel = simpleReader.convertFromOWL(inputStream);
		
		//find "human" (BioSource); it's already there normalized - uses Identifiers.org URI
		final BioSource human = (BioSource) originalModel.getByID("http://identifiers.org/taxonomy/9606");
		if(human == null) //fail shortly (importer must skip this pathwayData)
			throw new RuntimeException("http://identifiers.org/taxonomy/9606 (human) BioSource not found");
		
		//Remove/replace non-human BioSources, SequenceEntityReferences 
		//Pathways all have organism=null; let's set 'human' for all
		//Use parallel execution
		ExecutorService exec = Executors.newFixedThreadPool(3);
		//make a copy of all model's objects set to avoid concurrent modification exceptions.
		final Set<BioPAXElement> objects = new HashSet<BioPAXElement>(originalModel.getObjects());
		final Model model = originalModel;
		exec.execute(new Runnable() {
			@Override
			public void run() {
				for(BioPAXElement o : objects) {
					if((o instanceof BioSource) && !human.equals(o)) {
						model.remove(o);
					}
				}
			}
		});
		exec.execute(new Runnable() {
			@Override
			public void run() {
				for(SequenceEntityReference er : new ClassFilterSet<BioPAXElement, SequenceEntityReference>(objects, SequenceEntityReference.class)) {
					if(er.getOrganism() != null && !er.getOrganism().equals(human)) {
						model.remove(er);
						for(SimplePhysicalEntity spe : new HashSet<SimplePhysicalEntity>(er.getEntityReferenceOf()))
							spe.setEntityReference(null);
						for(EntityReference generic : new HashSet<EntityReference>(er.getMemberEntityReferenceOf()))
							generic.removeMemberEntityReference(er);						
					}
				}
			}
		});
		exec.execute(new Runnable() {
			@Override
			public void run() {
				for(Pathway p : new ClassFilterSet<BioPAXElement, Pathway>(objects, Pathway.class)) {
					if(p.getRDFId().startsWith("http://identifiers.org/panther.pathway/")
							|| !p.getPathwayComponent().isEmpty()
							|| !p.getPathwayOrder().isEmpty()) { //- seems they don't use pathwayOrder property, anyway
						p.setOrganism(human);
					} else //black box and no-components pathways
						p.setOrganism(null); //clear in case it's set to other org.
				}
			}
		});
		exec.shutdown(); //accept no more tasks
		try {//wait
			exec.awaitTermination(120, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted!", e);
		}
		
		//clone the model (to actually get rid of removed objects in all object properties)
		final Model cleanModel = (new Cloner(SimpleEditorMap.L3, BioPAXLevel.L3.getDefaultFactory()))
				.clone(originalModel, originalModel.getObjects());		
		log.info((objects.size()-cleanModel.getObjects().size())
				+ " non-human objects (and all corresponding properties) were cleared.");
		originalModel = null; // free some memory	
		
		if(cleanModel.getObjects(BioSource.class).size() != 1)
			throw new IllegalStateException("There are still several BioSource objects");
		
		//replace generic ERs that have only one non-generic memberER with that member
		//(should not be nested generics; if so, won't replace those)
		for(SequenceEntityReference generic : new HashSet<SequenceEntityReference>(cleanModel.getObjects(SequenceEntityReference.class))) {
			if(generic.getMemberEntityReference().size() != 1)
				continue; //non-generic or non-trivial
			
			// check for nested generic ERs
			if(!generic.getMemberEntityReferenceOf().isEmpty()) {
				log.warn("Found nested generic " + generic 
					+ "; skip (TODO: modify cleaner?)");
				continue;
			}
			
			// will use that single member instead of the generic;
			// copy names and xrefs
			EntityReference member = generic.getMemberEntityReference().iterator().next();
			
			//PANTHER contains DnaRegionReferences having ProteinRefrence members; This is wrong BioPAX model (not allowed)
			// skip such beasts (TODO or treat somehow?):
			if(generic.getModelInterface().isInstance(member)) {
//				member.getComment().addAll(generic.getComment()); //skip misleading/old comments
				member.getXref().addAll(generic.getXref());
				member.getName().addAll(generic.getName());
				if(member.getDisplayName() == null)
					member.setDisplayName(generic.getDisplayName());
				if(member.getStandardName() == null)
					member.setStandardName(generic.getStandardName());
				// replace
				for(SimplePhysicalEntity spe : new HashSet<SimplePhysicalEntity>(generic.getEntityReferenceOf()))
					spe.setEntityReference(member);
				
				generic.removeMemberEntityReference(member);				
				cleanModel.remove(generic);
			} else {
				//TODO e.g., copy names, xrefs, organism from member to the generic and delete the member (generic becomes non-generic)?
			}
		}
				
		//Add "dangling" Catalyses/Controls to related Pathways (to pathwayComponent) where controlled reaction belong
		for(Control c : cleanModel.getObjects(Control.class)) {
			for(Process process : c.getControlled()) {
				for(Pathway pathway : process.getPathwayComponentOf()) {
					pathway.addPathwayComponent(c);
				}
			}
		}
		
		//cleanup (remove above replaced generics, etc.)
		ModelUtils.removeObjectsIfDangling(cleanModel, UtilityClass.class);
		
		// convert model back to OutputStream for return
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			simpleReader.convertToOWL(cleanModel, outputStream);
		} catch (Exception e) {
			throw new RuntimeException("Failed writing BioPAX model to RDF/XML", e);
		}

		return outputStream.toString();		
	}

}
