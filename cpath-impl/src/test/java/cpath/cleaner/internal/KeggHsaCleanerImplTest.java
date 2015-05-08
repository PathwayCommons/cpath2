package cpath.cleaner.internal;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.BioSource;
import org.junit.Test;

import cpath.importer.Cleaner;

public class KeggHsaCleanerImplTest {

	@Test
	public final void testClean() throws IOException {	
		Cleaner cleaner = new KeggHsaCleanerImpl();	

		String f10 = getClass().getClassLoader().getResource("").getPath() 
				+ File.separator + "testCleanKegghsa00010.owl";
		cleaner.clean(new FileInputStream(getClass().getResource("/hsa00010.owl").getFile()), 
				new FileOutputStream(f10));		
		Model m10 = new SimpleIOHandler().convertFromOWL(new FileInputStream(f10));
		Set<BioSource> bioSources = m10.getObjects(BioSource.class);
		assertEquals(1, bioSources.size());
//		assertEquals("Homo sapiens (human)", bioSources.iterator().next().getDisplayName());		
		//check pathway URIs are http://identifiers.org/kegg.pathway/hsa*
		assertTrue(m10.containsID("http://identifiers.org/kegg.pathway/hsa00010"));

		
		String f562 = getClass().getClassLoader().getResource("").getPath() 
				+ File.separator + "testCleanKegghsa00562.owl";		
		cleaner.clean(new FileInputStream(getClass().getResource("/hsa00562.owl").getFile()), 
				new FileOutputStream(f562));		
		Model m562 = new SimpleIOHandler().convertFromOWL(new FileInputStream(f562));		
		assertTrue(m562.containsID("http://identifiers.org/kegg.pathway/hsa00562"));	
		assertTrue(m562.containsID("http://identifiers.org/kegg.pathway/hsa00010"));
		
		
		//TODO test how simple merging of these two files works...
		
	}

}
