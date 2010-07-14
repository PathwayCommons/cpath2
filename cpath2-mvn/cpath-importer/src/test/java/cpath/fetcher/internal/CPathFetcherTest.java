package cpath.fetcher.internal;

import static org.junit.Assert.*;

import java.io.*;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cpath.dao.PaxtoolsDAO;
import cpath.dao.internal.DataServicesFactoryBean;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Metadata.TYPE;

public class CPathFetcherTest {
	private static Log log = LogFactory.getLog(CPathFetcherTest.class);
	
	static PaxtoolsDAO proteinsDAO;
	static PaxtoolsDAO moleculesDAO;
	static CPathFetcherImpl fetcher;
	
	static {
		// create test DBs and all the tables
		DataServicesFactoryBean.createSchema("cpath2_test");
		ApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] { "classpath:testContext-allDAO.xml" });
		proteinsDAO = (PaxtoolsDAO) context.getBean("proteinsDAO");
		moleculesDAO = (PaxtoolsDAO) context.getBean("moleculesDAO");
		fetcher = new CPathFetcherImpl();
	}
	
	
	@Test
	public void testGetProviderProteinData() throws IOException {
		// any resource location now works (not only http://...)!
		String url = "classpath:metadata.html";
		System.out.println("Loading metadata from " + url);
		Collection<Metadata> metadatas = fetcher.getProviderMetadata(url);
		assertEquals(2, metadatas.size());
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
		
		fetcher.storeWarehouseData(metadata, proteinsDAO);
		assertTrue(proteinsDAO.containsID("urn:miriam:uniprot:P62158"));
		
		// write the whole merged model (to target/test-classes dir)
		OutputStream out = new FileOutputStream(
			getClass().getClassLoader().getResource("").getPath() 
				+ File.separator + "DataServicesTest1.out.owl");
		proteinsDAO.exportModel(out);
	}
	
	@Test // TODO enable when ZIP is supported
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
		
		fetcher.storeWarehouseData(metadata, moleculesDAO);
		assertTrue(moleculesDAO.containsID("urn:miriam:uniprot:P62158"));
		
		// write the whole merged model (to target/test-classes dir)
		OutputStream out = new FileOutputStream(
			getClass().getClassLoader().getResource("").getPath() 
				+ File.separator + "DataServicesTest2.out.owl");
		moleculesDAO.exportModel(out);
	}
	
	@Test // TODO enable when ZIP is supported
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
		
		fetcher.storeWarehouseData(metadata, moleculesDAO);
		assertTrue(moleculesDAO.containsID("urn:miriam:uniprot:P62158"));
		
		// write the whole merged model (to target/test-classes dir)
		OutputStream out = new FileOutputStream(
			getClass().getClassLoader().getResource("").getPath() 
				+ File.separator + "DataServicesTest3.out.owl");
		moleculesDAO.exportModel(out);
	}
}
