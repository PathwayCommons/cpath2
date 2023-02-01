package cpath.service;


import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import cpath.service.metadata.Datasource;
import cpath.service.metadata.Metadata;
import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;

import cpath.service.metadata.Datasource.METADATA_TYPE;
import org.junit.jupiter.api.Test;

public class CPathUtilsTest {
	
	static Model model;
	static SimpleIOHandler exporter;

	static {
		exporter = new SimpleIOHandler(BioPAXLevel.L3);
		// extend Model for the converter calling 'merge' method to work
		model = BioPAXLevel.L3.getDefaultFactory().createModel();
		model.setXmlBase("test/");
	}

	@Test
	public void testCopyWithGzip() throws IOException {
		String outFilename = getClass().getClassLoader().getResource("").getPath() 
				+ File.separator + "testCopyWithGzip.gz";
		byte[] testData = "<rdf>          </rdf>".getBytes(); 
		ByteArrayInputStream is = new ByteArrayInputStream(testData);
		OutputStream gzip = new GZIPOutputStream(new FileOutputStream(outFilename));
		CPathUtils.copy(is, gzip);
		is.close();
		gzip.close();
		ByteArrayOutputStream os = new ByteArrayOutputStream();	
		CPathUtils.copy(new GZIPInputStream(new FileInputStream(outFilename)), os);		
        byte[] read = os.toByteArray();
        assertNotNull(read);
        assertTrue(Arrays.equals(testData, read)); 	
	}

	@Test
	public void testCopy() throws IOException {
		Path f = Paths.get(getClass().getClassLoader()
				.getResource("").getPath(),"testCopy.txt");
		byte[] testData = "<rdf>          </rdf>".getBytes(); 
		ByteArrayInputStream is = new ByteArrayInputStream(testData);
		CPathUtils.copy(is, Files.newOutputStream(f));
		is.close();		
		ByteArrayOutputStream os = new ByteArrayOutputStream();	
		CPathUtils.copy(Files.newInputStream(f), os);
        byte[] read = os.toByteArray();
        assertArrayEquals(testData, read);
	}

	@Test
	public void testReadWriteMetadata() {
		String url = "classpath:metadata.json";
		Metadata metadata = CPathUtils.readMetadata(url);
		List<Datasource> datasources = metadata.getDatasources();
		final Datasource datasource = datasources.stream()
				.filter(m -> m.getIdentifier().equals("TEST_UNIPROT"))
				.findFirst().orElse(null);
		assertAll(
				() -> assertEquals(3, datasources.size()),
				() -> assertNotNull(datasource),
				() -> assertEquals(METADATA_TYPE.WAREHOUSE, datasource.getType())
		);

		Datasource ds = metadata.getDatasources().get(0);
		ds.setNumPathways(-1);
		ds.setNumInteractions(-1);
		ds.setNumPhysicalEntities(-1);
		CPathUtils.saveMetadata(metadata, url); //will replace classpath: with file:/target/
		metadata = CPathUtils.readMetadata("file:target/metadata.json");
		assertEquals(-1, metadata.getDatasources().get(0).getNumPathways());
	}
	
	@Test
	public void testChebiOboXrefLinesPattern() {
		Pattern p = Pattern.compile(".+?:(.+?)\\s+\"(.+?)\"");
		Matcher m = p.matcher("NIST Chemistry WebBook:22325-47-9 \"CAS Registry Number\"");
		boolean matched = m.find();
		assertAll(
				() -> assertTrue(matched),
				() -> assertEquals("22325-47-9", m.group(1)), //ID
				() -> assertEquals("CAS Registry Number", m.group(2)) //DB
		);
	}

	@Test
	public void testFixSourceIdForMapping() {
		assertEquals("Q8TD86", CPathUtils.fixSourceIdForMapping("uniprot knowledgebase", "Q8TD86-1"));
		assertEquals("Q8TD86", CPathUtils.fixSourceIdForMapping("uniprot isoform", "Q8TD86-1"));
		assertEquals("NP_619650", CPathUtils.fixSourceIdForMapping("refseq", "NP_619650.1"));
	}

	@Test
	public void testGenerateFileNames() {
		assertAll(() -> assertEquals("foo.normalized.gz", CPathUtils.normalizedFile("foo.some.gz")),
		() -> assertEquals("/foo/bar.normalized.gz", CPathUtils.normalizedFile("/foo/bar.some.gz")),
		() -> assertEquals("./foo/bar.normalized.gz", CPathUtils.normalizedFile("./foo/bar.some.gz")),
		() -> assertEquals("foo.cleaned.gz", CPathUtils.cleanedFile("foo.some.gz")),
		() -> assertEquals("/foo/bar.cleaned.gz", CPathUtils.cleanedFile("/foo/bar.some.gz")),
		() -> assertEquals("./foo/bar.cleaned.gz", CPathUtils.cleanedFile("./foo/bar.some.gz")),
		() -> assertEquals("foo.converted.gz", CPathUtils.convertedFile("foo.some.gz")),
		() -> assertEquals("/foo/bar.converted.gz", CPathUtils.convertedFile("/foo/bar.some.gz")),
		() -> assertEquals("./foo/bar.converted.gz", CPathUtils.convertedFile("./foo/bar.some.gz")),
		() -> assertEquals("foo.issues.gz", CPathUtils.validationFile("foo.normalized.gz")),
		() -> assertEquals("/foo/bar.issues.gz", CPathUtils.validationFile("/foo/bar.normalized.gz")),
		() -> assertEquals("./foo/bar.issues.gz", CPathUtils.validationFile("./foo/bar.normalized.gz"))
		);
	}

}
