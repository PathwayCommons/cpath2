package cpath.fetcher.internal;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cpath.dao.PaxtoolsDAO;
import cpath.dao.internal.DataServicesFactoryBean;
import cpath.fetcher.ProviderMetadataService;
import cpath.fetcher.WarehouseDataService;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Metadata.TYPE;

public class DataServicesTest {
	private static Log log = LogFactory.getLog(DataServicesTest.class);
	
	PaxtoolsDAO proteinsDAO;
	WarehouseDataService warehouseDataService;
	ProviderMetadataService metadataService;
	
	@Before
	public void setUp() throws Exception {
			// create test DBs and all the tables 
			DataServicesFactoryBean.createSchema("cpath2_test");
			ApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] {"classpath:testContext-dao.xml",
				"classpath:applicationContext-cpathFetcher.xml"});
			proteinsDAO = (PaxtoolsDAO) context.getBean("proteinsDAO");
			warehouseDataService = (WarehouseDataService) context.getBean("warehouseDataService");
			metadataService = (ProviderMetadataService) context.getBean("providerMetadataService");
	}
	
	@Test
	public void testGetProviderProteinData() throws IOException {
		// any resource location now works (not only http://...)!
		String url = "classpath:metadata.html";
		System.out.println("Loading metadata from " + url);
		Collection<Metadata> metadatas = metadataService.getProviderMetadata(url);
		assertEquals(2, metadatas.size());
		boolean found = false;
		Metadata metadata = null;
		for(Metadata mt : metadatas) {
			if(mt.getIdentifier().equalsIgnoreCase("TEST_UNIPROT")) {
				metadata = mt;
				break;
			}
		}
		assertNotNull(metadata);
		assertEquals(TYPE.PROTEIN, metadata.getType());

		// any resource location inside the metadata page works!
		//String location = "file://" + getClass().getResource("/test_uniprot_data.dat.gz").getPath();
		//location = "classpath:test_uniprot_data.dat.gz";
		//location = "ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/taxonomic_divisions/uniprot_sprot_human.dat.gz"
		/*
		metadata = new Metadata(
				"UNIPROT-TEST", 
				"Uniprot TEST", 
				Float.valueOf("15.15"), 
				"Mar 2, 2010",  
				location,
				"".getBytes(), 
				Metadata.TYPE.PROTEIN, 
				"cpath.cleaner.internal.BaseCleanerImpl", 
				"cpath.converter.internal.UniprotConverterImpl");
		*/
		
		warehouseDataService.storeWarehouseData(metadata, proteinsDAO);
		assertFalse(proteinsDAO.getObjects().isEmpty());
		//assertTrue(m.containsID("http://uniprot.org#NGNC_TOP1MT"));

		assertTrue(proteinsDAO.containsID("urn:miriam:uniprot:P62158"));
	}
}
