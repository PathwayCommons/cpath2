package cpath.dao;

import static org.junit.Assert.*;

import java.io.*;
import java.util.Collection;

import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.junit.*;

import cpath.config.CPathSettings;
import cpath.dao.CPathUtils;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;
import cpath.warehouse.beans.Metadata.METADATA_TYPE;


public class CPathUtilsTest {
	
	static Model model;
	static SimpleIOHandler exporter;
	static int count = 0;
	
	static {
		exporter = new SimpleIOHandler(BioPAXLevel.L3);
		// extend Model for the converter calling 'merge' method to work
		model = BioPAXLevel.L3.getDefaultFactory().createModel();
		model.setXmlBase(CPathSettings.xmlBase());
	}

	
	@Test
	public void testReadMetadata() throws IOException {
		String url = "classpath:meta.conf";
		System.out.println("Loading metadata from " + url);
		Collection<Metadata> metadatas = CPathUtils.readMetadata(url);
		assertEquals(8, metadatas.size());
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
	public void testReadPathwayData() throws IOException {
		String location = "classpath:test2.owl";
		// in case there's no "metadata page" prepared -
		Metadata metadata = new Metadata(
				"TEST", "Test", 
				"N/A", location,  
				"",
				new byte[]{}, 
				Metadata.METADATA_TYPE.BIOPAX, 
				null, // no cleaner (same as using "")
				"" // no converter
				);
		
		metadata.cleanupOutputDir();
		
		assertTrue(metadata.getPathwayData().isEmpty());
		CPathUtils.readPathwayData(metadata);
		assertFalse(metadata.getPathwayData().isEmpty());
		PathwayData pd = metadata.getPathwayData().iterator().next();
		String owl = new String(pd.getData());
		assertTrue(owl != null && owl.length() > 0);
		assertTrue(owl.contains("<bp:Protein"));
		SimpleIOHandler reader = new SimpleIOHandler(BioPAXLevel.L3);
		reader.mergeDuplicates(true);
		Model m = reader.convertFromOWL(new ByteArrayInputStream(owl.getBytes("UTF-8")));
		assertFalse(m.getObjects().isEmpty());
	}
	
}
