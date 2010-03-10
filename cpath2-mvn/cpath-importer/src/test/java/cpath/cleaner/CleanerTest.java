package cpath.cleaner;

// imports
import cpath.importer.cleaner.Cleaner;
import cpath.importer.cleaner.CleanerLoader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedOutputStream;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:cpath/cleaner/cleaner-test-context.xml")
public class CleanerTest {

	private static final int BUFFER = 2048;
    private static Log log = LogFactory.getLog(CleanerTest.class);

    @Autowired
    private CleanerLoader cleanerLoader;

	@Before
	public void setUp() throws Exception {
    }

	@Test
	public void testRun() throws Exception {

		cleanerLoaderTest();
    }

	private void cleanerLoaderTest() throws Exception {

		String byteCodeFile = "/cpath/cleaner/CleanerImpl.class";

        // grab the data
		byte[] cleanerData = loadCleanerData(byteCodeFile);
		assertTrue(cleanerData != null);

		// get instance of Cleaner
		Cleaner cleaner = cleanerLoader.getCleaner(cleanerData);
		assertTrue(cleaner != null);

		// quick test
		String testString = "TestString";
		String cleanedTestString = cleaner.clean(testString);
		assertTrue(cleanedTestString.equals(testString));
	}

	private byte[] loadCleanerData(String byteCodeFile) {

		byte[] toReturn = null;

		try {
			InputStream inputStream = this.getClass().getResourceAsStream(byteCodeFile);

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			BufferedOutputStream dest = new BufferedOutputStream(bos, BUFFER);

			int count;
			byte data[] = new byte[BUFFER];

			while ((count = inputStream.read(data, 0, BUFFER)) != -1) {
				dest.write(data, 0, count);
			}
			dest.flush();
			dest.close();

			toReturn = bos.toByteArray();
		}
		catch (Exception e) {
			return null;
		}

		return toReturn;
	}
}
