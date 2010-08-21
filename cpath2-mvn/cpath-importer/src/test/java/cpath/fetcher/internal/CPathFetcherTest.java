package cpath.fetcher.internal;

import static org.junit.Assert.*;

import java.io.*;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.impl.ModelImpl;
import org.biopax.paxtools.io.simpleIO.SimpleEditorMap;
import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.junit.*;

import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Metadata.TYPE;

public class CPathFetcherTest {
	private static Log log = LogFactory.getLog(CPathFetcherTest.class);
	
	static CPathFetcherImpl fetcher;
	static Model model;
	static SimpleExporter exporter;
	static int count = 0;
	
	static {
		fetcher = new CPathFetcherImpl();
		exporter = new SimpleExporter(BioPAXLevel.L3);
		// extend Model for the converter calling 'merge' method to work
		model = new ModelImpl(BioPAXLevel.L3.getDefaultFactory()) {
			@Override
			public void merge(Model source) {
				SimpleMerger simpleMerger = 
					new SimpleMerger(new SimpleEditorMap(BioPAXLevel.L3));
				simpleMerger.merge(this, source);
			}
		}; // all tests outputs to the same model object
	}

	@Test
	public void testGetProviderProteinData() throws IOException {
		// any resource location now works (not only http://...)!
		String url = "classpath:metadata.html";
		System.out.println("Loading metadata from " + url);
		Collection<Metadata> metadatas = fetcher.getProviderMetadata(url);
		assertTrue(metadatas.size() > 2);
		Metadata metadata = null;
		for(Metadata mt : metadatas) {
			if(mt.getIdentifier().equalsIgnoreCase("TEST_UNIPROT")) {
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
		//location = "ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/taxonomic_divisions/uniprot_sprot_human.dat.gz"
		
		// in case there's no "metadata page" prepared -
		Metadata metadata = new Metadata(
				"PROT", "Proteins Test Data", 
				Float.valueOf("15.15"), "July 13, 2010",  
				location,
				new byte[]{}, 
				Metadata.TYPE.PROTEIN, 
				"cpath.cleaner.internal.BaseCleanerImpl", 
				"cpath.converter.internal.UniprotConverterImpl");
		
		fetcher.storeWarehouseData(metadata, model);
		assertTrue(model.containsID("urn:miriam:uniprot:P62158"));
	}
	
	@Test
	public void testImportChebiData() throws IOException {
		String location = "classpath:test_chebi_data.dat.zip";
		// in case there's no "metadata page" prepared -
		Metadata metadata = new Metadata(
				"CHEBI", "ChEBI Test Data", 
				Float.valueOf("15.15"), "July 13, 2010",  
				location,
				new byte[]{}, 
				Metadata.TYPE.SMALL_MOLECULE, 
				"cpath.cleaner.internal.BaseCleanerImpl", 
				"cpath.converter.internal.ChEBIConverterImpl");
		
		fetcher.storeWarehouseData(metadata, model);
		assertTrue(model.containsID("urn:miriam:chebi:20"));
		assertTrue(model.containsID("urn:pathwaycommons:ChemicalStructure:JVTAAEKCZFNVCJ-SNQCPAJUDF"));
	}
	
	@Test
	public void testImportPubchemData() throws IOException {
		String location = "classpath:test_pubchem_data.dat.zip";
		// in case there's no "metadata page" prepared -
		Metadata metadata = new Metadata(
				"PUBCHEM", "PubChem Test Data", 
				Float.valueOf("15.15"), "July 13, 2010",  
				location,
				new byte[]{}, 
				Metadata.TYPE.SMALL_MOLECULE, 
				"cpath.cleaner.internal.BaseCleanerImpl", 
				"cpath.converter.internal.PubChemConverterImpl");
		
		fetcher.storeWarehouseData(metadata, model);
		assertTrue(model.containsID("urn:miriam:pubchem.substance:14438"));
		assertTrue(model.containsID("urn:miriam:pubchem.substance:14439"));
		assertTrue(model.containsID("urn:pathwaycommons:CRPUJAZIXJMDBK-DTWKUNHWBS"));
		assertTrue(model.containsID("urn:pathwaycommons:ChemicalStructure:CRPUJAZIXJMDBK-DTWKUNHWBS"));
	}
	
	@Override
	/* 
	 * note: although called several times, 
	 * it finally exports the model
	 * that results from all the test methods.
	 */
	protected void finalize() throws Throwable {
		// write the whole merged model (to target/test-classes dir)
		OutputStream out = new FileOutputStream(
			getClass().getClassLoader().getResource("").getPath() 
				+ File.separator + "DataServicesTest.out.owl");
		exporter.convertToOWL(model, out);
		System.out.println("CPathFetcherTest finalize call# " + (++count));
		super.finalize();
	}
}
