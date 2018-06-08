package cpath.service;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.junit.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import cpath.config.CPathSettings;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;

public class SearchEngineTest {
	
	static final ResourceLoader resourceLoader = new DefaultResourceLoader();
	
	final String indexLocation = 
		CPathSettings.getInstance().indexDir() + "_se";

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
		assertEquals(5, response.getSearchHit().size()); //- only Entity and ER types are indexed
		assertEquals(5, response.getNumHits().intValue());
		
		CPathSettings.getInstance().setDebugEnabled(false);
		response = searchEngine.search("ATP", 0, Interaction.class, null, null);
		assertNotNull(response);
		assertFalse(response.isEmpty());
		assertEquals(2, response.getSearchHit().size());
		//if cPath2 debugging is disabled, - no excerpt field in hits
		assertNull(response.getSearchHit().get(0).getExcerpt());
		//enable cPath2 debugging...
		CPathSettings.getInstance().setDebugEnabled(true);
		response = searchEngine.search("ATP", 0, Pathway.class, null, null);
		assertNotNull(response);
		assertFalse(response.isEmpty());
		assertEquals(1, response.getSearchHit().size());
		SearchHit hit = response.getSearchHit().get(0);
		assertEquals(11, hit.getSize().intValue()); //member processes and participants, not counting the hit itself
		assertEquals(4, hit.getNumProcesses().intValue());
		assertEquals(7, hit.getNumParticipants().intValue());
		assertNotNull(response.getSearchHit().get(0).getExcerpt());
		assertTrue(hit.getExcerpt().contains("-SCORE-"));
		CPathSettings.getInstance().setDebugEnabled(false);
		
		//test a special implementation for wildcard queries
		response = searchEngine.search("*", 0, Pathway.class, null, null);
		assertNotNull(response);
		assertFalse(response.isEmpty());
		assertEquals(1, response.getSearchHit().size());
		
		//find all objects (this here works with page=0 as long as the 
		//total no. objects in the test model < max hits per page)
		response = searchEngine.search("*", 0, null, null, null);
		assertEquals(23, response.getSearchHit().size()); //only Entity and ER types (since 23/12/2015)
			
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

		// only Entity, ER, Provenance, BioSource types are indexed (since 06/01/2016)
		response = searchEngine.search("*", 0, Provenance.class, null, null);
		assertFalse(response.getSearchHit().isEmpty());
		response = searchEngine.search("*", 0, Provenance.class, null, null);
		assertEquals(2, response.getSearchHit().size());
		response = searchEngine.search("*", 0, Provenance.class, new String[] {"kegg"}, null);
		assertEquals(1, response.getSearchHit().size());
		
		//datasource filter using a URI (required for -update-counts console command and datasources.html page to work)
		response = searchEngine.search("*", 0, Pathway.class, new String[] {"http://identifiers.org/kegg.pathway/"}, null);
		assertFalse(response.isEmpty());
		assertEquals(1, response.getSearchHit().size());
		//using metadata identifier
		response = searchEngine.search("*", 0, Pathway.class, new String[] {"kegg.pathway"}, null);
		assertFalse(response.isEmpty());
		assertEquals(1, response.getSearchHit().size());
		
		response = searchEngine.search("pathway:glycolysis", 0, SmallMoleculeReference.class, null, null);
		assertEquals(5, response.getSearchHit().size());
		response = searchEngine.search("pathway:GlycoLysis", 0, SmallMoleculeReference.class, null, null);
		assertTrue(response.isEmpty()); //case-sensitive
		response = searchEngine.search("pathway:pathway50", 0, SmallMoleculeReference.class, null, null);
		assertTrue(response.getSearchHit().isEmpty()); //ending part of URI - case-sensitive
		response = searchEngine.search("pathway:Pathway50", 0, SmallMoleculeReference.class, null, null);
		assertEquals(5, response.getSearchHit().size()); //ok
		response = searchEngine.search("uri:pathway50", 0, null, null, null);
		assertTrue(response.isEmpty()); //part of URI - case-sensitive
		response = searchEngine.search("uri:Pathway50", 0, null, null, null);
		assertFalse(response.isEmpty());//1
		//find by absolute URI (quoted)
		response = searchEngine.search("uri:\""+model.getXmlBase()+"Pathway50\"", 0, null, null, null);
		assertEquals(1, response.getSearchHit().size());
		response = searchEngine.search("pathway:\""+model.getXmlBase()+"Pathway50\"", 0, Pathway.class, null, null);
		assertEquals(1, response.getSearchHit().size());

		//test search with pagination
		searchEngine.setMaxHitsPerPage(10);
		response = searchEngine.search("*", 0, null, null, null);
		assertEquals(0, response.getPageNo().intValue());

		// only Entity, ER, and Provenance types are indexed (since 06/01/2016)
		assertEquals(23, response.getNumHits().intValue());
		assertEquals(10, response.getSearchHit().size());
		response = searchEngine.search("*", 1, null, null, null);
		assertEquals(10, response.getSearchHit().size());
		assertEquals(1, response.getPageNo().intValue());


		//test that service.search works (as expected) for IDs that contain ':', such as ChEBI IDs
		response =  searchEngine.search("CHEBI?20", 0, SmallMoleculeReference.class, null, null);
		assertFalse(response.getSearchHit().isEmpty());
		response =  searchEngine.search("xrefid:CHEBI?20", 0, SmallMolecule.class, null, null);
		assertFalse(response.getSearchHit().isEmpty());

		//NO result as the MultiFieldQueryParser there ignores (or splits by, analyzes...) colons, etc.
		response =  searchEngine.search("CHEBI:20", 0, SmallMoleculeReference.class, null, null);
		assertTrue(response.getSearchHit().isEmpty());

		//if excaped - '\:' - then it works (now, after recent changes in the indexer)
		response =  searchEngine.search("CHEBI\\:20", 0, SmallMoleculeReference.class, null, null);
		assertFalse(response.getSearchHit().isEmpty());
		response =  searchEngine.search("xrefid:CHEBI\\:20", 0, SmallMoleculeReference.class, null, null);
		assertFalse(response.getSearchHit().isEmpty());
		response =  searchEngine.search("xrefid:\"CHEBI\\:20\"", 0, SmallMoleculeReference.class, null, null);
		assertFalse(response.getSearchHit().isEmpty());
		response =  searchEngine.search("xrefid:chebi\\:20", 0, SmallMoleculeReference.class, null, null);
		assertFalse(response.getSearchHit().isEmpty());
		response =  searchEngine.search("xrefid:\"chebi\\:20\"", 0, SmallMoleculeReference.class, null, null);
		assertFalse(response.getSearchHit().isEmpty());

		//find by name: beta-D-fructose-6-phosphate
		response =  searchEngine.search("beta-d-fructose-6-phosphate", 0, SmallMoleculeReference.class, null, null);
		assertFalse(response.getSearchHit().isEmpty());
		assertEquals(3, response.getSearchHit().size());
		assertEquals("b-D-fru-6-p", response.getSearchHit().iterator().next().getName()); //gets top hit's standardName
		//- because dashes work like spaces here (StandardAnalyzer, field: keyword); and 'phosphate' matches in 3 times there...

		response =  searchEngine.search("\"beta-D-fructose-6-phosphate\"", 0, SmallMoleculeReference.class, null, null);
		assertFalse(response.getSearchHit().isEmpty());
		assertEquals(1, response.getSearchHit().size());

		response =  searchEngine.search("name:\"b-D-fru-6-p\"", 0, SmallMolecule.class, null, null);
		assertFalse(response.getSearchHit().isEmpty());
		assertEquals(1, response.getSearchHit().size());

		response =  searchEngine.search("name:b?D?fru?6?p", 0, SmallMolecule.class, null, null);
		assertFalse(response.getSearchHit().isEmpty());
		assertEquals(1, response.getSearchHit().size());

		// (hardly useful in practice) wildcards inside a quoted phrase - does not match -
		response =  searchEngine.search("name:\"b?D?fru?6?p\"", 0, SmallMolecule.class, null, null);
		assertTrue(response.getSearchHit().isEmpty());

		response =  searchEngine.search("name:b\\-D\\-fru\\-6\\-p", 0, SmallMolecule.class, null, null);
		assertFalse(response.getSearchHit().isEmpty());
		assertEquals(1, response.getSearchHit().size());

		response =  searchEngine.search("name:b-D-fru-6-p", 0, SmallMolecule.class, null, null);
		assertFalse(response.getSearchHit().isEmpty());
		assertEquals(1, response.getSearchHit().size());

		response =  searchEngine.search("fructose", 0, SmallMolecule.class, null, null);
		assertFalse(response.getSearchHit().isEmpty());
		assertEquals(1, response.getSearchHit().size());

		//"name:*fructose*" matches "beta-D-fructose-6-phosphate" in the name field (StringField, using KeywordAnalyzer)
		response =  searchEngine.search("name:*fructose*", 0, SmallMolecule.class, null, null);
		assertFalse(response.getSearchHit().isEmpty());
		assertEquals(1, response.getSearchHit().size());

		//TODO: "name:fructose" does not match "beta-D-fructose-6-phosphate" name (and there is no "fructose" name exactly)
		response =  searchEngine.search("name:fructose", 0, SmallMolecule.class, null, null);
		assertTrue(response.getSearchHit().isEmpty());

		//matches because there is a name="fructose" exactly!
		response =  searchEngine.search("name:fructose", 0, SmallMoleculeReference.class, null, null);
		assertFalse(response.getSearchHit().isEmpty());
		assertEquals(1, response.getSearchHit().size());
	}

}
