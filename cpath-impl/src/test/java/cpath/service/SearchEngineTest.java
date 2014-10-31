package cpath.service;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Interaction;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import cpath.config.CPathSettings;
import cpath.service.jaxb.SearchResponse;

@Ignore //TODO fix and test
public class SearchEngineTest {
	
	static final ResourceLoader resourceLoader = new DefaultResourceLoader();
	
	final String indexLocation = CPathSettings.getInstance()
			.tmpDir() + File.separator + "index";
	final String notIndexLocation = CPathSettings.getInstance()
			.tmpDir() + File.separator + "notindex";

	@Test
	public final void testSearch() throws IOException {
		SimpleIOHandler reader = new SimpleIOHandler();
		Model model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:merge/pathwaydata1.owl").getInputStream());
		SearchEngine searchEngine = new SearchEngine(model, indexLocation);
		searchEngine.index();
		assertTrue(searchEngine.indexExists(new File(indexLocation)));
		
		SearchResponse response = searchEngine.search("ATP", 0, null, null, null);
		assertFalse(response.isEmpty());
		
		//TODO add more assertions (find a ER by ID, by keyword, find All, etc...)
	}


	@Test
	public final void testIndexExists() {
		SearchEngine indexer = new SearchEngine(null, "");
		assertFalse(indexer.indexExists(new File(notIndexLocation)));
	}

}
