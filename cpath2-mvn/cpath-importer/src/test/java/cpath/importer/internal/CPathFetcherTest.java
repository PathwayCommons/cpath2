package cpath.importer.internal;

import static org.junit.Assert.*;

import java.io.*;
import java.util.Collection;

import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.junit.*;

import cpath.config.CPathSettings;
import cpath.importer.internal.FetcherImpl;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;
import cpath.warehouse.beans.Metadata.METADATA_TYPE;


public class CPathFetcherTest {
	
	static FetcherImpl fetcher;
	static Model model;
	static SimpleIOHandler exporter;
	static int count = 0;
	
	static {
		fetcher = new FetcherImpl();
		fetcher.setReUseFetchedDataFiles(false); // replace old test data files
		exporter = new SimpleIOHandler(BioPAXLevel.L3);
		// extend Model for the converter calling 'merge' method to work
		model = BioPAXLevel.L3.getDefaultFactory().createModel();
		model.setXmlBase(CPathSettings.xmlBase());
	}

	
	@Test
	public void testReadMetadata() throws IOException {
		String url = "classpath:metadata.conf";
		System.out.println("Loading metadata from " + url);
		Collection<Metadata> metadatas = fetcher.readMetadata(url);
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
		String location = "classpath:pathwaydata2.owl";
		// in case there's no "metadata page" prepared -
		Metadata metadata = new Metadata(
				"TEST_BIOPAX2", "Test Pathway Data 2", 
				"N/A", location,  
				"",
				new byte[]{}, 
				Metadata.METADATA_TYPE.BIOPAX, 
				null, // no cleaner (same as using "")
				"" // no converter
				);
		
		fetcher.fetchData(metadata);
		Collection<PathwayData> pathwayData = fetcher.readPathwayData(metadata);
		PathwayData pd = pathwayData.iterator().next();
		String owl = new String(pd.getPathwayData());
		assertTrue(owl != null && owl.length() > 0);
		assertTrue(owl.contains("<bp:Protein"));
		SimpleIOHandler reader = new SimpleIOHandler(BioPAXLevel.L3);
		reader.mergeDuplicates(true);
		Model m = reader.convertFromOWL(new ByteArrayInputStream(owl.getBytes("UTF-8")));
		assertFalse(m.getObjects().isEmpty());
	}
	
}
