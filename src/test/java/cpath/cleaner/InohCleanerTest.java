/**
 * 
 */
package cpath.cleaner;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.ControlledVocabulary;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.PublicationXref;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.Xref;
import org.junit.Test;

import cpath.service.api.Cleaner;

/**
 * @author rodche
 *
 */
public class InohCleanerTest {

	@Test
	public final void test() throws IOException {
		Cleaner cleaner = new InohCleaner();
		String f = getClass().getClassLoader().getResource("").getPath() + File.separator + "testINOH_cleaned.owl";
		cleaner.clean(new FileInputStream(getClass().getResource("/INOH_Glycolysis_Gluconeogenesis.owl").getFile()), new FileOutputStream(f));		
		Model m = new SimpleIOHandler().convertFromOWL(new FileInputStream(f));
		
		assertFalse(m.getObjects().isEmpty());
		
		Protein p = (Protein) m.getByID("http://www.inoh.org/biopax/id1278786247_PC");
		assertEquals(3, p.getXref().size());
		assertEquals(2, p.getEntityReference().getXref().size());
		assertEquals(0, p.getEntityReference().getMemberEntityReference().size()); //because there's only one xref
		
		ControlledVocabulary cv = (ControlledVocabulary) m.getByID("http://www.inoh.org/biopax/cell");
		String term = cv.getTerm().iterator().next();
		assertEquals("cell", term);		
		for(ControlledVocabulary v : m.getObjects(ControlledVocabulary.class)) {		
			for(String t : cv.getTerm()) {
				assertFalse(t.contains(":"));
			}
		}
		
		for(SimplePhysicalEntity spe : m.getObjects(SimplePhysicalEntity.class)) {		
			for(Xref x : spe.getXref()) {
				assertFalse("UniProt".equalsIgnoreCase(x.getDb()));
			}
		}
		
		for(UnificationXref x : m.getObjects(UnificationXref.class)) {
			assertTrue(x.getXrefOf().size()==1 || x.getXrefOf().toString().contains("Vocabulary"));
		}
		
		//make sure all "UniProt" PXs were gone (converted to RXs)
		for(PublicationXref x : m.getObjects(PublicationXref.class)) {
			assertFalse("UniProt".equalsIgnoreCase(x.getDb()));
		}
		
		for(Protein protein : m.getObjects(Protein.class)) {
			assertNotNull(protein.getEntityReference());
		}
	}
	
	// tests that member PRs gets greated where a protein has got multiple uniprot unification xrefs
	@Test
	public final void testForMemberPRs() throws IOException {
		Cleaner cleaner = new InohCleaner();
		String f = getClass().getClassLoader().getResource("").getPath() + File.separator + "testINOH_cleaned2.owl";
		cleaner.clean(new FileInputStream(getClass().getResource("/INOH_GPCR_signaling-pertussis_toxin-.owl").getFile()), new FileOutputStream(f));		
		Model m = new SimpleIOHandler().convertFromOWL(new FileInputStream(f));
		
		assertFalse(m.getObjects().isEmpty());
			
		Protein p = (Protein) m.getByID("http://www.inoh.org/biopax/id438071725_G_gamma");
		assertEquals(5, p.getXref().size());
		assertEquals(1, p.getEntityReference().getXref().size());
		assertEquals(3, p.getEntityReference().getMemberEntityReference().size());
		EntityReference mpr = p.getEntityReference().getMemberEntityReference().iterator().next();
		assertTrue(mpr instanceof ProteinReference);//not null
		assertTrue(mpr.getUri().contains("UniProt"));
		assertTrue(mpr.getXref().iterator().next() instanceof UnificationXref);
		
		
		for(Protein protein : m.getObjects(Protein.class)) {
			assertNotNull(protein.getEntityReference());
		}
	}

}
