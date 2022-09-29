/**
 * 
 */
package cpath.cleaner;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.ControlledVocabulary;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.SequenceModificationVocabulary;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.paxtools.util.ClassFilterSet;
import org.junit.jupiter.api.Test;

import cpath.service.api.Cleaner;

/**
 * @author rodche
 *
 */
public class NetPathCleanerTest {

	@Test
	public final void test() throws IOException {
		Cleaner cleaner = new NetPathCleaner();
		String f = getClass().getClassLoader().getResource("").getPath() + File.separator + "testNetPath_1_cleaned.owl";
		cleaner.clean(new FileInputStream(getClass().getResource("/NetPath_1.owl").getFile()), new FileOutputStream(f));		
		Model m = new SimpleIOHandler().convertFromOWL(new FileInputStream(f));
		
		assertFalse(m.getObjects().isEmpty());
		
		for(ControlledVocabulary cv : m.getObjects(ControlledVocabulary.class)) {
			Set<UnificationXref> urefs = new ClassFilterSet<Xref, UnificationXref>(
					new HashSet<>(cv.getXref()), UnificationXref.class);
			Set<RelationshipXref> rxrefs = new ClassFilterSet<Xref, RelationshipXref>(
					new HashSet<>(cv.getXref()), RelationshipXref.class);
			
			assertFalse(urefs.isEmpty() && !rxrefs.isEmpty()); //some CVs had no xrefs at all
			
			if(cv instanceof SequenceModificationVocabulary) {
				for(String t: cv.getTerm()) {
					if(t.contains("phospho-"))
						assertTrue(t.contains("phospho-L-"));
				}
			}
		}
		
		for(UnificationXref x : m.getObjects(UnificationXref.class)) {
			assertTrue(x.getXrefOf().size()==1 || x.getXrefOf().toString().contains("InteractionVocabulary"));
		}
	}

}
