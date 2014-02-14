package cpath.cleaner.internal;

import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.GZIPInputStream;

import org.junit.Test;

import cpath.dao.CPathUtils;
import cpath.importer.Cleaner;

public class PantherCleanerImplTest {

	@Test
	public final void testClean() throws IOException {
		Cleaner cleaner = new PantherCleanerImpl();
		byte[] bytes = CPathUtils.readContent(new GZIPInputStream(new BufferedInputStream(
				getClass().getResourceAsStream("/panther.owl.gz"))), true);
		assertTrue(bytes.length>0);
		
		String cleanedPantherData = cleaner.clean(new String(bytes, "UTF-8"));
		assertNotNull(cleanedPantherData);
		
		// dump owl for review
		String outFilename = getClass().getClassLoader().getResource("").getPath() 
			+ File.separator + "testCleanPanther.owl";
        Writer out = new OutputStreamWriter(new FileOutputStream(outFilename));
        out.write(cleanedPantherData);
        out.flush();
        out.close();
	}

}
