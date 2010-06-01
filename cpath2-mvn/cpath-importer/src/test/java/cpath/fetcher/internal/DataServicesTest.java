package cpath.fetcher.internal;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collection;

import org.biopax.paxtools.model.Model;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import cpath.dao.PaxtoolsDAO;
import cpath.fetcher.ProviderMetadataService;
import cpath.fetcher.WarehouseDataService;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Metadata.TYPE;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:applicationContext-whouseProteins.xml",
		"classpath:applicationContext-cpathFetcher.xml"})
@TransactionConfiguration(transactionManager="proteinsTransactionManager")
public class DataServicesTest {

	@Autowired
	PaxtoolsDAO proteinsDAO;
	
	@Autowired
	WarehouseDataService warehouseDataService;
	
	@Autowired
	ProviderMetadataService metadataService;
	
	@Test
	@Transactional
	public void testGetProviderProteinData() throws IOException {
		// any resource location is now welcomed -
		String location = "file://" + getClass().getResource("/test_uniprot_data.dat.gz").getPath();
		assertTrue(location != null);
		System.out.println("Test loading from PROTEIN data: " 
				+ location);
		Metadata metadata = new Metadata(
				"UNIPROT-TEST", 
				"Uniprot TEST", 
				Float.valueOf("15.15"), 
				"Mar 2, 2010", 
				//"ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/taxonomic_divisions/uniprot_sprot_human.dat.gz", 
				location,
				"".getBytes(), 
				Metadata.TYPE.PROTEIN, 
				"cpath.cleaner.internal.BaseCleanerImpl", 
				"cpath.converter.internal.UniprotConverterImpl");
		
		Model m = warehouseDataService.getWarehouseData(metadata);
		assertFalse(m.getObjects().isEmpty());
		//assertTrue(m.containsID("http://uniprot.org#NGNC_TOP1MT"));
		
		//proteinsDAO.importModel(m);
		//List<RelationshipXref> returnClasses = proteinsDAO.search("ngnc", RelationshipXref.class);
	}
	
	
	@Test
	public void testFetchMetadata() throws IOException {
		// any, not only URL, resource location is now welcomed -
		String url = "file://" + getClass().getResource("/TestDataProviderPage.html").getPath();
		System.out.println("Test loading metadata from " + url);
		Collection<Metadata> metadatas = metadataService.getProviderMetadata(url);
		assertEquals(5, metadatas.size());
		boolean found = false;
		for(Metadata mt : metadatas) {
			if(mt.getIdentifier().equalsIgnoreCase("CELLMAP")) {
				assertEquals(TYPE.BIOPAX, mt.getType());
				found = true;
				break;
			}
		}
		assertTrue(found);
	}
	
}
