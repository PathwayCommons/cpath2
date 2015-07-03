/**
 * 
 */
package cpath.cleaner.internal;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.ControlledVocabulary;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.Xref;
import org.junit.Test;

import cpath.importer.Cleaner;

/**
 * @author rodche
 *
 */
public class InohCleanerImplTest {

	@Test
	public final void test() throws IOException {
		Cleaner cleaner = new InohCleanerImpl();	
		String f = getClass().getClassLoader().getResource("").getPath() + File.separator + "testINOH_cleaned.owl";
		cleaner.clean(new FileInputStream(getClass().getResource("/INOH_Glycolysis_Gluconeogenesis.owl").getFile()), new FileOutputStream(f));		
		Model m = new SimpleIOHandler().convertFromOWL(new FileInputStream(f));
		
		assertFalse(m.getObjects().isEmpty());
		
		
		Protein p = (Protein) m.getByID("http://www.inoh.org/biopax/id1278786247_PC");
		assertEquals(3, p.getXref().size());
		assertEquals(1, p.getEntityReference().getXref().size());
		assertTrue(p.getEntityReference().getXref().iterator().next() instanceof UnificationXref);
		
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
				assertFalse(x.getDb().equalsIgnoreCase("UniProt"));
			}
		}
		
		for(UnificationXref x : m.getObjects(UnificationXref.class)) {
			assertTrue(x.getXrefOf().size()==1 || x.getXrefOf().toString().contains("Vocabulary"));
		}
	}

}
