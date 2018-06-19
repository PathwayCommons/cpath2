package cpath.service;


import static org.junit.Assert.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.junit.*;

import cpath.jpa.Metadata;
import cpath.jpa.Metadata.METADATA_TYPE;

public class CPathUtilsTest {
	
	static Model model;
	static SimpleIOHandler exporter;
	static int count = 0;

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
        assertNotNull(read);
        assertTrue(Arrays.equals(testData, read)); 	
	}

	
	@Test
	public void testReadMetadata() throws IOException {
		String url = "classpath:metadata.conf";
		System.out.println("Loading metadata from " + url);
		Collection<Metadata> metadatas = CPathUtils.readMetadata(url);
		assertEquals(3, metadatas.size());
		Metadata metadata = null;
		for(Metadata mt : metadatas) {
			if(mt.getIdentifier().equals("TEST_UNIPROT")) {
				metadata = mt;
				break;
			}
		}
		assertNotNull(metadata);
		assertEquals(METADATA_TYPE.WAREHOUSE, metadata.getType());
	}

	@Test
	public final void testReplaceID() {
		
		Model m = BioPAXLevel.L3.getDefaultFactory().createModel();
		UnificationXref xref = m.addNew(UnificationXref.class, "one");
		CPathUtils.replaceID(m, xref, "two");
		
		assertTrue(xref.getUri().equals("two"));
		assertTrue(m.containsID("two"));
	}
	
	
	@Test
	public void testChebiOboXrefLinesPattern() {
		Pattern p = Pattern.compile(".+?:(.+?)\\s+\"(.+?)\"");
		Matcher m = p.matcher("NIST Chemistry WebBook:22325-47-9 \"CAS Registry Number\"");
		assertTrue(m.find());		
		System.out.println("ID="+m.group(1)+"; DB="+m.group(2));		
	}


	@Test
	public void testFixSourceIdForMapping() {
		assertEquals("Q8TD86", CPathUtils.fixSourceIdForMapping("uniprot knowledgebase", "Q8TD86-1"));
		assertEquals("Q8TD86", CPathUtils.fixSourceIdForMapping("uniprot isoform", "Q8TD86-1"));
		assertEquals("NP_619650", CPathUtils.fixSourceIdForMapping("refseq", "NP_619650.1"));
	}
}
