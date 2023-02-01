package cpath.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.GZIPInputStream;

import cpath.service.api.Service;
import cpath.service.metadata.*;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("admin")
public class ServiceIT {

  @Autowired
  private Service service;

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
  @DirtiesContext
  public void testIdMapping() {
    //capitalization is important in 99% of identifier types (we should not ignore it)
    // we should be able to save it and not get 'duplicate key' exception here
    final String indexDir = "target/work/idx";
    CPathUtils.cleanupDirectory(indexDir, false);
    service.initIndex(null, indexDir, false);
    //save(Mapping) in fact updates/overwrites, preventing duplicate id-mapping records in the index
    service.mapping().save(new Mapping("GeneCards", "ZHX1", "UNIPROT", "P12345"));
    service.mapping().save(new Mapping("GeneCards", "ZHX1-C8orf76", "UNIPROT", "Q12345"));
    service.mapping().save(new Mapping("GeneCards", "ZHX1-C8ORF76", "UNIPROT", "Q12345"));
    service.mapping().save(new Mapping("TEST", "FooBar", "CHEBI", "CHEBI:12345"));
    service.mapping().save(new Mapping("UNIPROT", "A2A2M3", "UNIPROT", "A2A2M3"));
    Mapping m = new Mapping("PubChem-substance", "14438", "CHEBI", "CHEBI:20");
    assertEquals("SID:14438", m.getSrcId()); //already auto-fixed src ID
    service.mapping().save(m);
    service.mapping().commit(); //all the above
    service.mapping().refresh(); //required to acquire an up-to-date index searcher used in service.map(..) etc.

    //check it's saved
    assertEquals(1, service.map("ZHX1-C8orf76", "UNIPROT").size());
    assertEquals(1, service.map("ZHX1-C8ORF76", "UNIPROT").size());
    // repeat (should successfully update)- add a Mapping
    assertTrue(service.map("FooBar", "UNIPROT").isEmpty());
    Set<String> mapsTo = service.map("FooBar", "CHEBI");
    assertEquals(1, mapsTo.size());
    assertEquals("CHEBI:12345", mapsTo.iterator().next());
    mapsTo = service.map("FooBar", "CHEBI");
    assertEquals(1, mapsTo.size());
    assertEquals("CHEBI:12345", mapsTo.iterator().next());
    //test that service.map(..) method can map isoform IDs despite they're not explicitly added to the mapping db
    Set<String> map = service.map("A2A2M3-1", "UNIPROT");
    assertEquals(1, map.size());
    assertEquals(1, service.map("A2A2M3", "UNIPROT").size());
    assertEquals(1, service.map("SID:14438", "CHEBI").size());
    //map from a list of IDs to target ID type (UNIPROT)
    List<String> srcIds = new ArrayList<>();
    //add IDs - both map to the same uniprot ID ()
    srcIds.add("ZHX1");
    srcIds.add("A2A2M3");
    //currently, mapping().find* methods cannot map uniprot isoform IDs (service.map(..) - can do by removing the suffix)
    List<Mapping> mappings = service.mapping().findBySrcIdInAndDstDbIgnoreCase(srcIds, "uNIPROT");
    assertEquals(2, mappings.size());
    //test new service.map(null, srcIds, "UNIPROT"), which must also support isoform IDs
    srcIds.remove("A2A2M3");
    srcIds.add("A2A2M3-1");
    assertEquals(2, service.map(srcIds, "UNIPROT").size());
  }

  @Test
  @DirtiesContext
  public void testReadContent() throws IOException {
    // in case there's no "datasource page" prepared -
    Datasource datasource = new Datasource("TEST",
      Arrays.asList("Test", "testReadContent"),
      "N/A",
      "classpath:test2.owl.zip",
      "",
      "",
      Datasource.METADATA_TYPE.BIOPAX,
      null, // no cleaner (same as using "")
      "", // no converter
      null,
      "",
      "free",
      0, 0, 0
    );

    CPathUtils.cleanupDirectory(service.intermediateDataDir(datasource), true);
    assertTrue(datasource.getFiles().isEmpty());

    service.unzipData(datasource);
    assertFalse(datasource.getFiles().isEmpty());

    String pd = datasource.getFiles().iterator().next();
    SimpleIOHandler reader = new SimpleIOHandler(BioPAXLevel.L3);
    reader.mergeDuplicates(true);
    InputStream is = new GZIPInputStream(new FileInputStream(pd));
    Model m = reader.convertFromOWL(is);
    assertFalse(m.getObjects().isEmpty());
  }
}
