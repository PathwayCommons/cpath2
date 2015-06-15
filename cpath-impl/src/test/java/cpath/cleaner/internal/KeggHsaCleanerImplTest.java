package cpath.cleaner.internal;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.Pathway;
import org.junit.Test;

import cpath.importer.Cleaner;

public class KeggHsaCleanerImplTest {

	@Test
	public final void testClean() throws IOException {	
		Cleaner cleaner = new KeggHsaCleanerImpl();	
		final String testPathwayUri = "http://identifiers.org/kegg.pathway/hsa00010";

		String f10 = getClass().getClassLoader().getResource("").getPath() 
				+ File.separator + "testCleanKegghsa00010.owl";
		cleaner.clean(new FileInputStream(getClass().getResource("/hsa00010.owl").getFile()), 
				new FileOutputStream(f10));		
		Model m10 = new SimpleIOHandler().convertFromOWL(new FileInputStream(f10));
		Set<BioSource> bioSources = m10.getObjects(BioSource.class);
		assertEquals(1, bioSources.size());
//		assertEquals("Homo sapiens (human)", bioSources.iterator().next().getDisplayName());		
		//check pathway URIs are http://identifiers.org/kegg.pathway/hsa*
		assertTrue(m10.containsID(testPathwayUri));

		
		String f562 = getClass().getClassLoader().getResource("").getPath() 
				+ File.separator + "testCleanKegghsa00562.owl";		
		cleaner.clean(new FileInputStream(getClass().getResource("/hsa00562.owl").getFile()), 
				new FileOutputStream(f562));		
		Model m562 = new SimpleIOHandler().convertFromOWL(new FileInputStream(f562));		
		assertTrue(m562.containsID("http://identifiers.org/kegg.pathway/hsa00562"));	
		assertTrue(m562.containsID(testPathwayUri));
		
		
		//Test whether the simple merging of these two files just works
		// and does not depend on the order of the sub-models to merge
		Model model = BioPAXLevel.L3.getDefaultFactory().createModel();
		model.merge(m10);
		model.merge(m562);		
		assertTrue(model.containsID(testPathwayUri));
		assertTrue(model.containsID("http://identifiers.org/kegg.pathway/hsa00562"));
		
		//save result 1
		new SimpleIOHandler().convertToOWL(model, new FileOutputStream(
			getClass().getClassLoader().getResource("").getPath() 
				+ File.separator + "testCleanedKeggSimpleMerge10562.owl"));
		
		Pathway pw = (Pathway) model.getByID(testPathwayUri);
		assertFalse(pw.getPathwayComponent().isEmpty());
		
		//Now merge again in the other order
		model = BioPAXLevel.L3.getDefaultFactory().createModel();
		model.merge(m562);	
		model.merge(m10);
		assertTrue(model.containsID(testPathwayUri));
		assertTrue(model.containsID("http://identifiers.org/kegg.pathway/hsa00562"));
		
		//save test result 2
		new SimpleIOHandler().convertToOWL(model, new FileOutputStream(
				getClass().getClassLoader().getResource("").getPath() 
					+ File.separator + "testCleanedKeggSimpleMerge56210.owl"));
		
		pw = (Pathway) model.getByID(testPathwayUri);
		//with SimpleMerger only, pathways with the same URI do not merge properly...
		assertTrue(pw.getPathwayComponent().isEmpty());
	}

}
