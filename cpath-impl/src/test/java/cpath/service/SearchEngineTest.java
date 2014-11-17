package cpath.service;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Interaction;
import org.biopax.paxtools.model.level3.Pathway;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.Provenance;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;
import org.junit.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import cpath.config.CPathSettings;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;

public class SearchEngineTest {
	
	static final ResourceLoader resourceLoader = new DefaultResourceLoader();
	
	final String indexLocation = CPathSettings.getInstance().tmpDir() 
			+ File.separator + "SearchEngineTest_index";

	@Test
	public final void testSearch() throws IOException {
		SimpleIOHandler reader = new SimpleIOHandler();
		Model model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:merge/pathwaydata1.owl").getInputStream());
		SearchEngine searchEngine = new SearchEngine(model, indexLocation);
		searchEngine.index();
		assertTrue(new File(indexLocation).exists());
		
		SearchResponse response = searchEngine.search("ATP", 0, null, null, null);
		assertNotNull(response);
		assertFalse(response.isEmpty());
		assertEquals(7, response.getSearchHit().size());
		assertEquals(7, response.getNumHits().intValue());
		
		response = searchEngine.search("ATP", 0, Interaction.class, null, null);
		assertNotNull(response);
		assertFalse(response.isEmpty());
		assertEquals(2, response.getSearchHit().size());
		//if cPath2 debugging is disabled, - no score/explain in the excerpt
		CPathSettings.getInstance().setDebugEnabled(false);
		assertFalse(response.getSearchHit().get(0).getExcerpt().contains("-SCORE-"));
		
		//enable cPath2 debugging...
		CPathSettings.getInstance().setDebugEnabled(true);
		
		response = searchEngine.search("ATP", 0, Pathway.class, null, null);
		assertNotNull(response);
		assertFalse(response.isEmpty());
		assertEquals(1, response.getSearchHit().size());
		
		SearchHit hit = response.getSearchHit().get(0);
		assertEquals(5, hit.getSize().intValue());
		assertTrue(hit.getExcerpt().contains("-SCORE-"));
		
		//test a special implementation for wildcard queries
		response = searchEngine.search("*", 0, Pathway.class, null, null);
		assertNotNull(response);
		assertFalse(response.isEmpty());
		assertEquals(1, response.getSearchHit().size());
		
		//find all objects (this here works with page=0 as long as the 
		//total no. objects in the test model < max hits per page)
		response = searchEngine.search("*", 0, null, null, null);
		assertEquals(50, response.getSearchHit().size());
			
		response = searchEngine.search("*", 0, PhysicalEntity.class, null, null);
		assertEquals(8, response.getSearchHit().size());
		
		response = searchEngine.search("*", 0, PhysicalEntity.class, null, new String[] {"562"});
		assertEquals(2, response.getSearchHit().size());
		
		response = searchEngine.search("*", 0, PhysicalEntity.class, null, new String[] {"Escherichia"});
		assertFalse(response.isEmpty());
		assertEquals(2, response.getSearchHit().size());
		
		response = searchEngine.search("*", 0, PhysicalEntity.class, null, new String[] {"Escherichia coliü"});
		assertFalse(response.isEmpty());
		assertEquals(2, response.getSearchHit().size());
		
		response = searchEngine.search("*", 0, Provenance.class, null, null);
		assertEquals(2, response.getSearchHit().size());
		
		response = searchEngine.search("*", 0, Provenance.class, new String[] {"kegg"}, null);
		assertEquals(1, response.getSearchHit().size());
		
		//datasource filter using a URI (required for -update-counts console command and datasources.html page to work)
		response = searchEngine.search("*", 0, Pathway.class, new String[] {"http://identifiers.org/kegg.pathway/"}, null);
		assertFalse(response.isEmpty());
		assertEquals(1, response.getSearchHit().size());
		
		response = searchEngine.search("pathway:glycolysis", 0, SmallMoleculeReference.class, null, null);
		assertEquals(5, response.getSearchHit().size());
		
		//test search with pagination
		searchEngine.setMaxHitsPerPage(10);
		response = searchEngine.search("*", 0, null, null, null);
		assertEquals(50, response.getNumHits().intValue());
		assertEquals(10, response.getSearchHit().size());
		response = searchEngine.search("*", 1, null, null, null);
		assertEquals(10, response.getSearchHit().size());
		
	}

}
