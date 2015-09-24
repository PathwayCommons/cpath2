package cpath.cleaner.internal;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipInputStream;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.BioSource;
import org.junit.Test;

import cpath.dao.CPathUtils;
import cpath.importer.Cleaner;

public class PantherCleanerImplTest {

	@Test
	public final void testClean() throws IOException {
		// dump owl for review
		String f = getClass().getClassLoader().getResource("").getPath() 
			+ File.separator + "testCleanPanther.owl";
		
		Cleaner cleaner = new PantherCleanerImpl();
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		CPathUtils.unzip(new ZipInputStream(new FileInputStream(
				getClass().getResource("/panther.owl.zip").getFile())), bos);
		
		cleaner.clean(new ByteArrayInputStream(bos.toByteArray()), new FileOutputStream(f));
		
		Model m = new SimpleIOHandler().convertFromOWL(new FileInputStream(f));
		Set<BioSource> bioSources = m.getObjects(BioSource.class);
		assertEquals(1, bioSources.size());
		assertTrue(bioSources.iterator().next().getUri().endsWith("/9606"));
	}

}
