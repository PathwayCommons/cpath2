package cpath.importer.internal;

import static org.junit.Assert.*;

import java.io.*;
import java.util.Collection;

import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;
import org.junit.*;

import cpath.importer.internal.FetcherImpl;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;
import cpath.warehouse.beans.Metadata.TYPE;

//@Ignore
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
	}

	
	@Test
	public void testGetMetadata() throws IOException {
		// any resource location now works (not only http://...)!
		String url = "classpath:metadata.conf";
		System.out.println("Loading metadata from " + url);
		Collection<Metadata> metadatas = fetcher.getMetadata(url);
		assertEquals(7, metadatas.size());
		Metadata metadata = null;
		for(Metadata mt : metadatas) {
			if(mt.getIdentifier().equals("TEST_UNIPROT")) {
				metadata = mt;
				break;
			}
		}
		assertNotNull(metadata);
		assertEquals(TYPE.PROTEIN, metadata.getType());
	}
	
	@Test
	public void testImportProteinData() throws IOException {
		// any resource location inside the metadata page works now!
		//String location = "file://" + getClass().getResource("/test_uniprot_data.dat.gz").getPath();
		String location = "classpath:test_uniprot_data.dat.gz";
		// in case there's no "metadata page" prepared -
		Metadata metadata = new Metadata(
				"TEST_UNIPROT", "Proteins Test Data", 
				"2010.10", "October 03, 2010",  
				location,
				new byte[]{}, 
				Metadata.TYPE.PROTEIN, 
				null,
				"cpath.converter.internal.UniprotConverterImpl");
		fetcher.fetchData(metadata);
		fetcher.storeWarehouseData(metadata, model);
		assertFalse(((Model)model).getObjects(ProteinReference.class).isEmpty());
		assertTrue(((Model)model).containsID("http://identifiers.org/uniprot/P62158"));
	}
	
	@Test
	public void testImportChebiData() throws IOException {
		String location = "classpath:test_chebi_data.dat.zip";
		// in case there's no "metadata page" prepared -
		Metadata metadata = new Metadata(
				"TEST_CHEBI", "ChEBI Test Data", 
				"2010.10", "October 03, 2010",  
				location,
				new byte[]{}, 
				Metadata.TYPE.SMALL_MOLECULE, 
				null, 
				"cpath.converter.internal.ChEBITestConverterImpl");
		fetcher.fetchData(metadata);
		fetcher.storeWarehouseData(metadata, model);
		assertFalse(((Model)model).getObjects(SmallMoleculeReference.class).isEmpty());
		assertTrue(((Model)model).containsID("http://identifiers.org/obo.chebi/CHEBI:20"));
	}
	
	
	@Test
	public void testFetchMappingData() throws IOException {
		Metadata metadata = new Metadata(
				"TEST_MAPPING_TXT", "Test Id Mapping Data", 
				"2010.10", "October 03, 2010",  
				"classpath:yeast_id_mapping.txt",
				new byte[]{}, 
				Metadata.TYPE.MAPPING, 
				"",
				"");
		
		File f = new File(metadata.localDataFile());
		if(f.exists()) {
			f.delete();
		}
		fetcher.fetchData(metadata);
		assertTrue(f.exists() && f.isFile());
	}


	@Test
	public void testGetProviderPathwayData() throws IOException {
		String location = "classpath:test-normalized-2.zip";
		// in case there's no "metadata page" prepared -
		Metadata metadata = new Metadata(
				"TEST_BIOPAX2", "Test Pathway Data 2", 
				"1", "N/A",  
				location,
				new byte[]{}, 
				Metadata.TYPE.BIOPAX, 
				null, // no cleaner (same as using "")
				"" // no converter
				);
		
		fetcher.fetchData(metadata);
		Collection<PathwayData> pathwayData =
			fetcher.getProviderPathwayData(metadata);
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
