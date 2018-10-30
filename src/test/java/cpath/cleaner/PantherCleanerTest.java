package cpath.cleaner;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.BioSource;
import org.junit.Test;

public class PantherCleanerTest {

	@Test
	public final void testClean() throws IOException {
		Path f = Paths.get(getClass().getClassLoader().getResource("").getPath(),"testCleanPanther.owl");
		ZipFile zf = new ZipFile(getClass().getResource("/panther.zip").getFile());
		assertTrue(zf.entries().hasMoreElements());
		ZipEntry ze = zf.entries().nextElement();
		(new PantherCleaner()).clean(zf.getInputStream(ze), Files.newOutputStream(f));
		Model m = new SimpleIOHandler().convertFromOWL(Files.newInputStream(f));
		Set<BioSource> bioSources = m.getObjects(BioSource.class);
		assertEquals(1, bioSources.size());
		assertTrue(bioSources.iterator().next().getUri().endsWith("9606"));
	}

}
