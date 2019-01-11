package cpath.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.GZIPInputStream;

import cpath.service.api.CPathService;
import cpath.service.jpa.*;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.validator.api.beans.Behavior;
import org.biopax.validator.api.beans.Validation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;

import cpath.service.jpa.Metadata.METADATA_TYPE;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ActiveProfiles("admin")
@DataJpaTest(includeFilters = @ComponentScan.Filter(Component.class))
public class ServiceAndJpaTest {

  @Autowired
  private CPathService service;

  @Test
  public final void testSettings() {
    Settings s = service.settings();
    assertNotNull(s);
    assertEquals("test/", s.getXmlBase());
    assertEquals("Pathway Commons Demo",s.getName());
    assertEquals(1, s.getOrganismTaxonomyIds().size());
    assertEquals(Integer.valueOf(100),s.getMaxHitsPerPage());
  }

  @Test
  public final void testLogGa() {
    service.track("172.20.10.3", "command", "search");
    service.track("172.20.10.3", "provider", "Reactome");
    service.track("172.20.10.3", "error", "bad request");
    service.track("172.20.10.3", "client", null);
  }

  @Test
  @DirtiesContext
  public void testIdMapping() {
    //capitalization is important in 99% of identifier types (we should not ignore it)
    // we should be able to save it and not get 'duplicate key' exception here
    service.mapping().save(new Mapping("GeneCards", "ZHX1", "UNIPROT", "P12345"));
    service.mapping().save(new Mapping("GeneCards", "ZHX1-C8orf76", "UNIPROT", "Q12345"));
    service.mapping().save(new Mapping("GeneCards", "ZHX1-C8ORF76", "UNIPROT", "Q12345"));
    assertEquals(1, service.mapping()
        .findBySrcIgnoreCaseAndSrcIdAndDestIgnoreCase("GeneCards", "ZHX1-C8ORF76", "UNIPROT").size());

    //check it's saved
    assertEquals(1, service.map("ZHX1-C8orf76", "UNIPROT").size());
    assertEquals(1, service.map("ZHX1-C8ORF76", "UNIPROT").size());

    // repeat (should successfully update)- add a Mapping
    service.mapping().save(new Mapping("TEST", "FooBar", "CHEBI", "CHEBI:12345"));
    assertTrue(service.map("FooBar", "UNIPROT").isEmpty());

    Set<String> mapsTo = service.map("FooBar", "CHEBI");
    assertEquals(1, mapsTo.size());
    assertEquals("CHEBI:12345", mapsTo.iterator().next());
    mapsTo = service.map("FooBar", "CHEBI");
    assertEquals(1, mapsTo.size());
    assertEquals("CHEBI:12345", mapsTo.iterator().next());

    //test that service.map(..) method can map isoform IDs despite they're not explicitly added to the mapping db
    service.mapping().save(new Mapping("UNIPROT", "A2A2M3", "UNIPROT", "A2A2M3"));
    assertEquals(1, service.map("A2A2M3-1", "UNIPROT").size());
    assertEquals(1, service.map("A2A2M3", "UNIPROT").size());

    Mapping m = new Mapping("PubChem-substance", "14438", "CHEBI", "CHEBI:20");
    service.mapping().save(m);
    assertEquals("SID:14438", m.getSrcId());
    assertEquals(1, service.map("SID:14438", "CHEBI").size());

    //map from a list of IDs to target ID type (UNIPROT)
    List<String> srcIds = new ArrayList<>();
    //add IDs - both map to the same uniprot ID ()
    srcIds.add("ZHX1");
    srcIds.add("A2A2M3");
    //currently, mapping().find* methods cannot map uniprot isoform IDs (service.map(..) - can do by removing the suffix)
    List<Mapping> mappings = service.mapping().findBySrcIdInAndDestIgnoreCase(srcIds, "UNIPROT");
    assertEquals(2, mappings.size());
    //test new service.map(null, srcIds, "UNIPROT"), which must also support isoform IDs
    srcIds.remove("A2A2M3");
    srcIds.add("A2A2M3-1");
    assertEquals(2, service.map(srcIds, "UNIPROT").size());
  }

  @Test
  @DirtiesContext
  public void testImportContent() {
    // mock metadata and pathway data
    Metadata md = new Metadata("TEST", "test", "test", "", "",
        "", METADATA_TYPE.BIOPAX, null, null, null, "free");

    //cleanup previous tests data if any
    service.clear(md);
    String content = "test 0"; //space will be replaced
    md.getFiles().add(content);
    //add the second pd (for the tests at the end of this method)
    md.getFiles().add("test1");
    // persist
    service.metadata().save(md);

    // test pathwaydata content is not accidentally erased
    Iterator<String> it = md.getFiles().iterator();
    content = it.next();
    //we want test0 for following assertions
    if ("test1".equals(content))
      content = it.next();
    assertEquals("test_0", content);

    //even if we update from the db, data must not be empty
    md = service.metadata().findByIdentifier(md.getIdentifier());
    assertNotNull(md);
    assertEquals("TEST", md.getIdentifier());
    assertEquals(2, md.getFiles().size());
    it = md.getFiles().iterator();
    content = it.next();
    //we want test0 for following assertions
    if ("test1".equals(content))
      content = it.next();
    assertEquals("test_0", content);

    // write validation result to files
    for (String f : md.getFiles()) {
      String out = service.validationFile(f);
      service.saveValidationReport(new Validation(null, f, false, Behavior.WARNING, 0, null), out);
      assertTrue(Files.exists(Paths.get(out)));
    }

    //cleanup
    service.clear(md);
    assertTrue(md.getFiles().isEmpty());
    md = service.metadata().findByIdentifier("TEST");
    assertTrue(md.getFiles().isEmpty());
  }

  @Test
  @DirtiesContext
  public void testReadContent() throws IOException {
    // in case there's no "metadata page" prepared -
    Metadata metadata = new Metadata("TEST",
        "Test;testReadContent",
        "N/A",
        "classpath:test2.owl.zip",
        "",
        "",
        Metadata.METADATA_TYPE.BIOPAX,
        null, // no cleaner (same as using "")
        "", // no converter
        null,
        "free"
    );

    CPathUtils.cleanupDirectory(service.outputDir(metadata), true);
    assertTrue(metadata.getFiles().isEmpty());

    service.unzipData(metadata);
    assertFalse(metadata.getFiles().isEmpty());

    String pd = metadata.getFiles().iterator().next();
    SimpleIOHandler reader = new SimpleIOHandler(BioPAXLevel.L3);
    reader.mergeDuplicates(true);
    InputStream is = new GZIPInputStream(new FileInputStream(pd));
    Model m = reader.convertFromOWL(is);
    assertFalse(m.getObjects().isEmpty());
  }
}
