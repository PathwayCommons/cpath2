package cpath.fetcher.internal;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import cpath.dao.PaxtoolsDAO;
import cpath.warehouse.beans.Metadata;

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:applicationContext-whouseProteins.xml"})
@TransactionConfiguration(transactionManager="proteinsTransactionManager")
public class ProviderProteinDataServiceImplTest {

	@Autowired
	PaxtoolsDAO proteinsDAO;
	
	@Test
	@Transactional
	public void testGetProviderProteinData() throws IOException {
		Metadata metadata = new Metadata(
				"UNIPROT-HUMAN", 
				"Uniprot Human", 
				Float.valueOf("15.15"), 
				"Mar 2, 2010", 
				"ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/taxonomic_divisions/uniprot_sprot_human.dat.gz", 
				"".getBytes(), 
				Metadata.TYPE.PROTEIN, 
				"cpath.cleaner.internal.BaseCleanerImpl", 
				"cpath.converter.internal.UniprotConverterImpl");
		
		Model m = (new ProviderProteinDataServiceImpl(new FetcherHTTPClientImpl()))
			.getProviderProteinData(metadata);
		
		assertTrue(m.containsID("http://uniprot.org#NGNC_TOP1MT"));
		
		proteinsDAO.importModel(m);
		
		List<RelationshipXref> returnClasses = proteinsDAO
			.search("ngnc", RelationshipXref.class, false);
	}

}
