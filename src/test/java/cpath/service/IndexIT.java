package cpath.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;

import cpath.service.metadata.Mapping;
import cpath.service.metadata.Mappings;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import org.springframework.test.annotation.DirtiesContext;

public class IndexIT {
  static final ResourceLoader resourceLoader = new DefaultResourceLoader();
  static final Logger logger = LoggerFactory.getLogger(IndexIT.class);

  @Test
  public final void search() throws IOException {
    SimpleIOHandler reader = new SimpleIOHandler();
    Model model = reader.convertFromOWL(resourceLoader
      .getResource("classpath:merge/pathwaydata1.owl").getInputStream());
    IndexImpl index = new IndexImpl(model, "target/test-idx", false);
    index.save(model);

    //close index writer and re-open the index searcher in the read-only mode
    //(optional; tests should pass regardless; if you remove the following two lines, keep index.close() at the end)
    index.close();
    index = new IndexImpl(model, "target/test-idx", true);

    SearchResponse response = index.search("ATP", 0, null, null, null);
    assertNotNull(response);
    assertFalse(response.isEmpty());
    assertEquals(5, response.getSearchHit().size()); //- only Entity and ER types are indexed
    assertEquals(5, response.getNumHits().intValue());

    response = index.search("ATP", 0, Interaction.class, null, null);
    assertNotNull(response);
    assertFalse(response.isEmpty());
    assertEquals(2, response.getSearchHit().size());

    //if debug logging is enabled, there will be 'excerpt' field
    if(logger.isDebugEnabled())
      assertNotNull(response.getSearchHit().get(0).getExcerpt());
    else
      assertNull(response.getSearchHit().get(0).getExcerpt());

    response = index.search("ATP", 0, Pathway.class, null, null);
    assertNotNull(response);
    assertFalse(response.isEmpty());
    assertEquals(1, response.getSearchHit().size());
    SearchHit hit = response.getSearchHit().get(0);
    assertEquals(4, hit.getNumProcesses().intValue());
    assertEquals(7, hit.getNumParticipants().intValue());

    //test a special implementation for wildcard queries
    response = index.search("*", 0, Pathway.class, null, null);
    assertNotNull(response);
    assertFalse(response.isEmpty());
    assertEquals(1, response.getSearchHit().size());

    //find all objects (this here works with page=0 as long as the
    //total no. objects in the test model < max hits per page)
    response = index.search("*", 0, null, null, null);
    assertEquals(23, response.getSearchHit().size()); //only Entity and ER types (since 23/12/2015)

    response = index.search("*", 0, PhysicalEntity.class, null, null);
    assertEquals(8, response.getSearchHit().size());

    response = index.search("*", 0, PhysicalEntity.class, null, new String[] {"562"});
    assertEquals(2, response.getSearchHit().size());

    response = index.search("*", 0, PhysicalEntity.class, null, new String[] {"Escherichia"});
    assertFalse(response.isEmpty());
    assertEquals(2, response.getSearchHit().size());

    response = index.search("*", 0, PhysicalEntity.class, null, new String[] {"Escherichia coliü"});
    assertFalse(response.isEmpty());
    assertEquals(2, response.getSearchHit().size());

    // only Entity, ER, Provenance, BioSource types are indexed (since 06/01/2016)
    response = index.search("*", 0, Provenance.class, null, null);
    assertFalse(response.getSearchHit().isEmpty());
    response = index.search("*", 0, Provenance.class, null, null);
    assertEquals(2, response.getSearchHit().size());
    response = index.search("*", 0, Provenance.class, new String[] {"kegg"}, null);
    assertEquals(1, response.getSearchHit().size());

    //find by partial name of a datasource - "pathway" of "KEGG Pathway"...
    response = index.search("*", 0, Pathway.class, new String[] {"pathway"}, null);
    assertFalse(response.isEmpty());
    assertEquals(1, response.getSearchHit().size());
    assertTrue(response.getSearchHit().stream().anyMatch(h -> h.getDataSource().contains("test:kegg_test")));

    response = index.search("pathway:glycolysis", 0, SmallMoleculeReference.class, null, null);
    assertEquals(5, response.getSearchHit().size());
    response = index.search("pathway:GlycoLysis", 0, SmallMoleculeReference.class, null, null);
    assertTrue(response.isEmpty()); //case-sensitive
    response = index.search("pathway:pathway50", 0, SmallMoleculeReference.class, null, null);
    assertTrue(response.getSearchHit().isEmpty()); //ending part of URI - case-sensitive
    response = index.search("pathway:Pathway50", 0, SmallMoleculeReference.class, null, null);
    assertEquals(5, response.getSearchHit().size()); //ok
    response = index.search("uri:pathway50", 0, null, null, null);
    assertTrue(response.isEmpty()); //part of URI - case-sensitive
    response = index.search("uri:Pathway50", 0, null, null, null);
    assertFalse(response.isEmpty());//1
    //find by absolute URI (quoted)
    response = index.search("uri:\""+model.getXmlBase()+"Pathway50\"", 0, null, null, null);
    assertEquals(1, response.getSearchHit().size());
    response = index.search("pathway:\""+model.getXmlBase()+"Pathway50\"", 0, Pathway.class, null, null);
    assertEquals(1, response.getSearchHit().size());

    //test search with pagination
    index.setMaxHitsPerPage(10);
    response = index.search("*", 0, null, null, null);
    assertEquals(0, response.getPageNo().intValue());

    // only Entity, ER, and Provenance types are indexed (since 06/01/2016)
    assertEquals(23, response.getNumHits().intValue());
    assertEquals(10, response.getSearchHit().size());
    response = index.search("*", 1, null, null, null);
    assertEquals(10, response.getSearchHit().size());
    assertEquals(1, response.getPageNo().intValue());

    //test that service.search works (as expected) for IDs that contain ':', such as ChEBI IDs with banana ('CHEBI:')
    response =  index.search("CHEBI?20", 0, SmallMoleculeReference.class, null, null);
    assertFalse(response.getSearchHit().isEmpty());
    response =  index.search("xrefid:CHEBI?20", 0, SmallMolecule.class, null, null);
    assertFalse(response.getSearchHit().isEmpty());

    //NO result as the MultiFieldQueryParser there ignores (or splits by, analyzes...) colons, etc.
    response =  index.search("CHEBI:20", 0, SmallMoleculeReference.class, null, null);
    assertTrue(response.getSearchHit().isEmpty());
    //if escaped - '\:' - then it works (now, after recent changes in the indexer)
    response =  index.search("20", 0, SmallMoleculeReference.class, null, null);
    assertFalse(response.getSearchHit().isEmpty());
    response =  index.search("CHEBI\\:20", 0, SmallMoleculeReference.class, null, null);
    assertFalse(response.getSearchHit().isEmpty());
    response =  index.search("xrefid:CHEBI\\:20", 0, SmallMoleculeReference.class, null, null);
    assertFalse(response.getSearchHit().isEmpty());
    response =  index.search("xrefid:\"chebi\\:20\"", 0, SmallMoleculeReference.class, null, null);
    assertFalse(response.getSearchHit().isEmpty()); //we allow case-insensitive id prefix search e.g. chebi:* and CHEBI:*
    response =  index.search("xrefid:chebi\\:20", 0, SmallMoleculeReference.class, null, null);
    assertFalse(response.getSearchHit().isEmpty()); //ID search is case-sensitive but the prefix (if any, e.g. 'chebi:') is not
    response =  index.search("xrefid:\"chebi\\:20\"", 0, SmallMoleculeReference.class, null, null);
    assertFalse(response.getSearchHit().isEmpty()); //xrefid field is case-sensitive but the prefix (if any, e.g. 'chebi:') is not

    response =  index.search("xrefid:P16104", 0, Protein.class, null, null);
    assertFalse(response.getSearchHit().isEmpty()); //unprefixed (no "banana") id search is case-sensitive
    response =  index.search("xrefid:p16104", 0, Protein.class, null, null);
    assertTrue(response.getSearchHit().isEmpty()); //unprefixed (no "banana") id search is case-sensitive - so no results here

    //find by name: beta-D-fructose-6-phosphate
    response =  index.search("beta-d-fructose-6-phosphate", 0, SmallMoleculeReference.class, null, null);
    assertFalse(response.getSearchHit().isEmpty());
    assertEquals(3, response.getSearchHit().size());
    assertEquals("b-D-fru-6-p", response.getSearchHit().iterator().next().getName()); //gets top hit's standardName
    //- because dashes work like spaces here (StandardAnalyzer, field: keyword); and 'phosphate' matches in 3 times there...

    response =  index.search("\"beta-D-fructose-6-phosphate\"", 0, SmallMoleculeReference.class, null, null);
    assertFalse(response.getSearchHit().isEmpty());
    assertEquals(1, response.getSearchHit().size());

    response =  index.search("name:\"b-D-fru-6-p\"", 0, SmallMolecule.class, null, null);
    assertFalse(response.getSearchHit().isEmpty());
    assertEquals(1, response.getSearchHit().size());

    response =  index.search("name:b?D?fru?6?p", 0, SmallMolecule.class, null, null);
    assertFalse(response.getSearchHit().isEmpty());
    assertEquals(1, response.getSearchHit().size());

    // (hardly useful in practice) wildcards inside a quoted phrase - does not match -
    response =  index.search("name:\"b?D?fru?6?p\"", 0, SmallMolecule.class, null, null);
    assertTrue(response.getSearchHit().isEmpty());

    response =  index.search("name:b\\-D\\-fru\\-6\\-p", 0, SmallMolecule.class, null, null);
    assertFalse(response.getSearchHit().isEmpty());
    assertEquals(1, response.getSearchHit().size());

    response =  index.search("name:b-D-fru-6-p", 0, SmallMolecule.class, null, null);
    assertFalse(response.getSearchHit().isEmpty());
    assertEquals(1, response.getSearchHit().size());

    response =  index.search("fructose", 0, SmallMolecule.class, null, null);
    assertFalse(response.getSearchHit().isEmpty());
    assertEquals(1, response.getSearchHit().size());

    //"name:*fructose*" matches "beta-D-fructose-6-phosphate" in the name field (StringField, using KeywordAnalyzer)
    response =  index.search("name:*fructose*", 0, SmallMolecule.class, null, null);
    assertFalse(response.getSearchHit().isEmpty());
    assertEquals(1, response.getSearchHit().size());

    //"name:fructose" does not match "beta-D-fructose-6-phosphate" name (and there is no "fructose" name exactly in this class)
    response =  index.search("name:fructose", 0, SmallMolecule.class, null, null);
    assertTrue(response.getSearchHit().isEmpty());

    //matches because there is a name="fructose" exactly!
    response =  index.search("name:fructose", 0, SmallMoleculeReference.class, null, null);
    assertFalse(response.getSearchHit().isEmpty());
    assertEquals(1, response.getSearchHit().size());

    //re-open to write
    index.close();
    index = new IndexImpl(model, "target/test-idx", false);
    index.drop();
    response = index.search("*", 1, null, null, null);
    assertTrue(response.getSearchHit().isEmpty());

    index.close();
  }

  @Test
  @DirtiesContext
  public void idMapping() {
    Mappings mappings = new IndexImpl(null, "target/test-idx", false);
    //capitalization is important in 99% of identifier types (we should not ignore it)
    //but, let's allow both chebi:1234 and CHEBI:1234 and 1234 for ChEBI IDs when searching...

    //save some id mappings
    mappings.save(new Mapping("GeneCards", "ZHX1-C8orf76", "UNIPROT", "Q12345"));
    mappings.save(new Mapping("GeneCards", "ZHX1-C8ORF76", "UNIPROT", "Q12345"));
    mappings.save(new Mapping("TEST", "FooBar", "CHEBI", "12345"));
    Mapping m = new Mapping("PubChem-substance", "14438", "CHEBI", "20");
    assertEquals("SID:14438", m.getSrcId()); //already auto-fixed src ID
    mappings.save(m);
    mappings.commit(); //all the above
    mappings.refresh(); //required to acquire an up-to-date index searcher used in service.map(..) etc.

    //check that both of those two similar IDs were saved
    assertEquals(1, mappings.findBySrcIdInAndDstDbIgnoreCase(List.of("ZHX1-C8orf76"), "UNIPROT").size());
    assertEquals(1, mappings.findBySrcIdInAndDstDbIgnoreCase(List.of("ZHX1-C8ORF76"), "UNIPROT").size());
    // repeat (should successfully update)- add a Mapping
    assertTrue(mappings.findBySrcIdInAndDstDbIgnoreCase(List.of("FooBar"), "UNIPROT").isEmpty());
    List<Mapping> mapsTo = mappings.findBySrcIdInAndDstDbIgnoreCase(List.of("FooBar"), "CHEBI");
    assertEquals(1, mapsTo.size());
    assertEquals("CHEBI:12345", mapsTo.iterator().next().getDstId());
    mapsTo = mappings.findBySrcIdInAndDstDbIgnoreCase(List.of("FooBar"), "CHEBI");
    assertEquals(1, mapsTo.size());
    assertEquals("CHEBI:12345", mapsTo.iterator().next().getDstId());
    assertEquals(1, mappings.findBySrcIdInAndDstDbIgnoreCase(List.of("SID:14438"), "CHEBI").size());

    mappings.close();
  }

}
