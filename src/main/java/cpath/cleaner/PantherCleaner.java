package cpath.cleaner;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import cpath.service.CPathUtils;
import org.biopax.paxtools.controller.Cloner;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.level3.Process;
import org.biopax.paxtools.util.ClassFilterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.service.api.Cleaner;

/**
 * Cleaner for the PANTHER Pathway 3.4.1 BioPAX model
 * 
 * Remove non-human entity references. Add "dangling" Controls to Pathways.
 */
final class PantherCleaner implements Cleaner {
	
  private static Logger log = LoggerFactory.getLogger(PantherCleaner.class);

    
  public void clean(InputStream data, OutputStream cleanedData)
	{	
		SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
		Model originalModel = simpleReader.convertFromOWL(data);
		
		//find "human" (BioSource); it must be already there "normalized" - has absolute URI="9606" (rdf:about="9606")
		final BioSource human = (BioSource) originalModel.getByID("9606");
		// - new version PANTHER pathway data contain URIS and xrefs like that, unfortunately...
		if(human == null) //fail shortly (importer must skip this dataFile)
			throw new RuntimeException("Human data (e.g. BioSource) not found.");

		// fix the preferred standard name (bioregistry.io prefix) for Taxonomy xref.db
		human.getXref().iterator().next().setDb("ncbitaxon");
		
		//Remove/replace non-human BioSources, SequenceEntityReferences;
		//Pathways all have organism=null; let's set 'human' for all
		ExecutorService exec = Executors.newFixedThreadPool(3);
		//make a copy of all model's objects set to avoid concurrent modification exceptions.
		final Set<BioPAXElement> objects = new HashSet<>(originalModel.getObjects());
		final Model model = originalModel; //has to be final to use inside execute(), lambdas...
		exec.execute(() -> {
			for(BioPAXElement o : objects) {
				if((o instanceof BioSource) && !human.equals(o)) {
					model.remove(o);
				}
			}
		});
		exec.execute(() -> {
			for(SequenceEntityReference er : new ClassFilterSet<>(objects, SequenceEntityReference.class)) {
				if(er.getOrganism() != null && !er.getOrganism().equals(human)) {
					model.remove(er);
					for(SimplePhysicalEntity spe : new HashSet<>(er.getEntityReferenceOf()))
						spe.setEntityReference(null);
					for(EntityReference generic : new HashSet<>(er.getMemberEntityReferenceOf()))
						generic.removeMemberEntityReference(er);
				}
			}
		});
		exec.execute(() -> {
			for(Pathway p : new ClassFilterSet<>(objects, Pathway.class)) {
				if(p.getUri().contains("identifiers.org/panther.pathway/")
						|| p.getUri().contains("bioregistry.io/panther.pathway:")
						|| !p.getPathwayComponent().isEmpty()
						|| !p.getPathwayOrder().isEmpty()) { //seems they don't use pathwayOrder property, anyway
					p.setOrganism(human);
				} else //black box and no-components pathways
					p.setOrganism(null); //clear in case it's set to other org.
			}
		});
		exec.execute(() -> {
			// remove ALL "PANTHER Pathway" and "PANTHER Pathway Component" xrefs
			// (they collide with UniProt IDs like P01234 and confuse id-mapping, full-text search)
			for(BioPAXElement o : objects) {
				if((o instanceof Xref) && CPathUtils
						.startsWithAnyIgnoreCase(String.valueOf(((Xref)o).getDb()),"panther pathway"))
				{
					model.remove(o);
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
		final Model cleanModel = (new Cloner(SimpleEditorMap.L3, BioPAXLevel.L3.getDefaultFactory())).clone(originalModel.getObjects());
		cleanModel.setXmlBase(originalModel.getXmlBase());
		log.info((objects.size()-cleanModel.getObjects().size())
				+ " non-human objects (and all corresponding properties) were cleared.");
		originalModel = null; // free some memory, perhaps...
		
		if(cleanModel.getObjects(BioSource.class).size() != 1)
			throw new IllegalStateException("There are still several BioSource objects");
		
		//replace generic ERs that have only one non-generic memberER with that member
		//(should not be nested generics; if so, won't replace those)
		for(SequenceEntityReference generic : new HashSet<>(
				cleanModel.getObjects(SequenceEntityReference.class)))
		{
			if(generic.getMemberEntityReference().size() != 1)
				continue; //non-generic or non-trivial
			
			// check for nested generic ERs
			if(!generic.getMemberEntityReferenceOf().isEmpty()) {
				log.warn("Found nested generic " + generic + "; skip it.");
				continue;
			}
			
			// Use that single member ER instead of this generic ER;
			// copy names and xrefs
			EntityReference member = generic.getMemberEntityReference().iterator().next();
			
			//PANTHER contains DnaRegionReferences having ProteinRefrence members (wrong BioPAX model)
			if(generic.getModelInterface().isInstance(member)) {
				member.getXref().addAll(generic.getXref());
				member.getName().addAll(generic.getName());
				if(member.getDisplayName() == null)
					member.setDisplayName(generic.getDisplayName());
				if(member.getStandardName() == null)
					member.setStandardName(generic.getStandardName());
				// replace
				for(SimplePhysicalEntity spe : new HashSet<>(generic.getEntityReferenceOf()))
					spe.setEntityReference(member);
				
				generic.removeMemberEntityReference(member);				
				cleanModel.remove(generic);
			} else if(generic instanceof DnaReference || generic instanceof RnaReference
					|| generic instanceof SmallMoleculeReference || generic instanceof ProteinReference)
			{// it's in fact allowed, except for these constraints:
			// R:RnaReference=RnaReference R:SmallMoleculeReference=SmallMoleculeReference
			// R:DnaReference=DnaReference R:ProteinReference=ProteinReference
				log.error(generic.getModelInterface().getSimpleName() + ", uri:"
					+ generic.getUri() + ", has a member ER of incompatible type: "
					+ member.getModelInterface().getSimpleName());
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
		try {
			simpleReader.convertToOWL(cleanModel, cleanedData);
		} catch (Exception e) {
			throw new RuntimeException("Failed writing cleaned PANTHER BioPAX model to RDF/XML", e);
		}
	}

}
