/**
 * 
 */
package cpath.admin;

import static cpath.config.CPathSettings.PROP_BLACKLIST_CONTROL_THRESHOLD;
import static cpath.config.CPathSettings.PROP_BLACKLIST_DEGREE_THRESHOLD;
import static cpath.config.CPathSettings.blacklistFile;
import static cpath.config.CPathSettings.property;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.ComplexAssembly;
import org.biopax.paxtools.model.level3.Control;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.Interaction;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;

import cpath.dao.Analysis;

/**
 * @author rodche
 *
 */
final class BlacklistingAnalysis implements Analysis {
	
	@Override
    public void execute(Model model) 
    {	
   	 	final int degreeThreshold = Integer.parseInt(property(PROP_BLACKLIST_DEGREE_THRESHOLD));
    	final int controlDegreeThreshold = Integer.parseInt(property(PROP_BLACKLIST_CONTROL_THRESHOLD));
        
    	// This is to keep track of ids and to prevent multiple addition of a single element
        Set<BioPAXElement> blacklistedBPEs = new HashSet<BioPAXElement>();

        // Get all small molecule references
        for (SmallMoleculeReference ref : model.getObjects(SmallMoleculeReference.class)) {
            Set<EntityReference> refs = new HashSet<EntityReference>();
            // Collect its member refs (and their members refs if present)
            getAllMemberRefs(ref, refs);

            Set<PhysicalEntity> entities = new HashSet<PhysicalEntity>();
            for (EntityReference entityReference : refs) {
                for (SimplePhysicalEntity entity : entityReference.getEntityReferenceOf()) {
                    //  Pool all entities and their member entities
                    getAllSPEs(entity, entities);
                }
            }

            // Count the degrees of the entities and sum them all
            int regDegree = 0;
            int allDegree = 0;
            for (PhysicalEntity entity : entities) {
                // These are the complexes
                allDegree += entity.getComponentOf().size();

                // These are the interactions
                for (Interaction interaction : entity.getParticipantOf()) {
                    if(!(interaction instanceof ComplexAssembly)) { // Since we already count the complexes
                        allDegree++;

                        // Also count the control iteractions
                        if(interaction instanceof Control) {
                            regDegree++;
                        }
                    }
                }
            } // End of iteration, degree calculation

            // See if it needs to be blacklisted
            if(regDegree < controlDegreeThreshold && allDegree > degreeThreshold) {
                // Adding to the blacklist 
                for (EntityReference entityReference : refs)
                    blacklistedBPEs.add(entityReference);

                for (PhysicalEntity entity : entities)
                    blacklistedBPEs.add(entity);
            }
        }

        // Write all the blacklisted ids to the output
		try {		
			PrintStream printStream = new PrintStream(
				new FileOutputStream(blacklistFile()));
        for (BioPAXElement bpe : blacklistedBPEs)
            printStream.println(bpe.getRDFId());
        	printStream.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Failed creating the file: " 
					+ blacklistFile(), e);
		} 

    }

    private void getAllSPEs(PhysicalEntity entity, Set<PhysicalEntity> entities) {
        entities.add(entity);

        for(PhysicalEntity memberEntity : entity.getMemberPhysicalEntity()) {
            if(!entities.contains(memberEntity)) {
                entities.add(memberEntity);
                getAllSPEs(memberEntity, entities);
            }
        }
    }

    private void getAllMemberRefs(EntityReference ref, Set<EntityReference> refs) {
        refs.add(ref);

        for (EntityReference entityReference : ref.getMemberEntityReference()) {
            if(!refs.contains(entityReference)) {
                refs.add(entityReference);
                getAllMemberRefs(entityReference, refs);
            }
        }
    }

}
