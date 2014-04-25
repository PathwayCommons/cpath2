

import static org.junit.Assert.*;

import java.io.*;
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

import cpath.config.CPathSettings;
import cpath.dao.CPathUtils;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Content;
import cpath.warehouse.beans.Metadata.METADATA_TYPE;


public class CPathUtilsTest {
	
	static Model model;
	static SimpleIOHandler exporter;
	static int count = 0;
	static final CPathSettings cpath = CPathSettings.getInstance();
	
	static {
		exporter = new SimpleIOHandler(BioPAXLevel.L3);
		// extend Model for the converter calling 'merge' method to work
		model = BioPAXLevel.L3.getDefaultFactory().createModel();
		model.setXmlBase(cpath.getXmlBase());
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
		String outFilename = getClass().getClassLoader().getResource("").getPath() 
				+ File.separator + "testCopy.txt";
		byte[] testData = "<rdf>          </rdf>".getBytes(); 
		ByteArrayInputStream is = new ByteArrayInputStream(testData);
		CPathUtils.copy(is, new FileOutputStream(outFilename));	
		is.close();		
		ByteArrayOutputStream os = new ByteArrayOutputStream();	
		CPathUtils.copy(new FileInputStream(outFilename), os);		
        byte[] read = os.toByteArray();
        assertNotNull(read);
        assertTrue(Arrays.equals(testData, read)); 	
	}

	
	@Test
	public void testReadMetadata() throws IOException {
		String url = "classpath:metadata.conf";
		System.out.println("Loading metadata from " + url);
		Collection<Metadata> metadatas = CPathUtils.readMetadata(url);
		assertEquals(2, metadatas.size());
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
	public void testReadContent() throws IOException {
		String location = "classpath:test2.owl.zip";	
		// in case there's no "metadata page" prepared -
		Metadata metadata = new Metadata("TEST", 
				"Test;testReadContent", 
				"N/A", 
				location,  
				"",
				"", 
				Metadata.METADATA_TYPE.BIOPAX, 
				null, // no cleaner (same as using "")
				"", // no converter
				null,
				"free"
				);
		
		metadata.cleanupOutputDir();
		assertTrue(metadata.getContent().isEmpty());		
		CPathUtils.analyzeAndOrganizeContent(metadata);
		
		assertFalse(metadata.getContent().isEmpty());
		Content pd = metadata.getContent().iterator().next();
		SimpleIOHandler reader = new SimpleIOHandler(BioPAXLevel.L3);
		reader.mergeDuplicates(true);
		InputStream is = new GZIPInputStream(new FileInputStream(pd.originalFile()));
		Model m = reader.convertFromOWL(is);
		assertFalse(m.getObjects().isEmpty());
	}
	
	@Test
	public final void testReplaceID() {
		
		Model m = BioPAXLevel.L3.getDefaultFactory().createModel();
		UnificationXref xref = m.addNew(UnificationXref.class, "one");
		CPathUtils.replaceID(m, xref, "two");
		
		assertTrue(xref.getRDFId().equals("two"));
		assertTrue(m.containsID("two"));
	}
	
	
	@Test
	public void testPatternForChebiOboXrefLines() {
		Pattern p = Pattern.compile(".+?:(.+?)\\s+\"(.+?)\"");
		Matcher m = p.matcher("NIST Chemistry WebBook:22325-47-9 \"CAS Registry Number\"");
		assertTrue(m.find());		
		System.out.println("ID="+m.group(1)+"; DB="+m.group(2));		
	}
}
