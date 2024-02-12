package cpath.service;

import cpath.service.api.Service;
import cpath.service.api.OutputFormat;
import cpath.service.metadata.Mapping;
import cpath.service.metadata.Datasource;
import cpath.service.metadata.Datasource.METADATA_TYPE;
import cpath.service.jaxb.*;

import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.normalizer.Normalizer;

import org.biopax.validator.BiopaxIdentifier;
import org.biopax.validator.api.Validator;
import org.biopax.validator.api.ValidatorUtils;
import org.biopax.validator.api.beans.Behavior;
import org.biopax.validator.api.beans.Validation;
import org.biopax.validator.rules.ProteinModificationFeatureCvRule;
import org.biopax.validator.rules.XrefRule;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Console app - data integration tests (using test metadata, data, index).
 */
@SpringBootTest
@ActiveProfiles({"admin", "premerge"})
public class ConsoleApplicationIT
{
  private static final Logger log = LoggerFactory.getLogger(ConsoleApplicationIT.class);
  private static final ResourceLoader resourceLoader = new DefaultResourceLoader();

  @Autowired
  Service service;

  @Autowired(required = false)
  Validator validator;

  @Autowired
  XrefRule xrefRule;

  @Autowired
  ProteinModificationFeatureCvRule proteinModificationFeatureCvRule;

  @Autowired
  ValidatorUtils utils;

  private final BioPAXFactory level3 = BioPAXLevel.L3.getDefaultFactory();

  /*
   * This tests that the BioPAX Validator framework
   * is properly configured and usable in the current context.
   */
  @Test
  public void testValidateModel() {
    Catalysis ca = level3.create(Catalysis.class, "catalysis1");
    ca.setControlType(ControlType.INHIBITION);
    ca.addComment("error: illegal controlType");
    TemplateReactionRegulation tr = level3.create(TemplateReactionRegulation.class, "regulation1");
    tr.setControlType(ControlType.ACTIVATION_ALLOSTERIC);
    tr.addComment("error: illegal controlType");
    Model m = level3.createModel();
    m.add(ca);
    m.add(tr);
    Validation v = new Validation(new BiopaxIdentifier());//, "", true, null, 0, null);// do auto-fix
    v.setModel(m);
    validator.validate(v);
    validator.getResults().remove(v);
    assertEquals(2, v.countErrors(null, null, "range.violated", null, false, false));
  }

  /*
   * Checks DB names and synonyms were loaded there -
   */
  @Test
  public void xrefRuleEntezGene() {
    UnificationXref x = level3.create(UnificationXref.class, "1");
    x.setDb("EntrezGene"); //but official preferred name is: "NCBI Gene"
    x.setId("0000000");
    Validation v = new Validation(new BiopaxIdentifier());
    xrefRule.check(v, x);
    assertTrue(v.getError().isEmpty()); //no error
  }

  @Test
  public void proteinModificationFeatureCvRule() {
    //System.out.print("proteinModificationFeatureCvRule valid terms are: " + rule.getValidTerms().toString());
    assertTrue(proteinModificationFeatureCvRule.getValidTerms().contains("(2S,3R)-3-hydroxyaspartic acid".toLowerCase()));
    SequenceModificationVocabulary cv = level3.create(SequenceModificationVocabulary.class, "MOD_00036");
    cv.addTerm("(2S,3R)-3-hydroxyaspartic acid");
    ModificationFeature mf = level3.create(ModificationFeature.class, "MF_MOD_00036");
    mf.setModificationType(cv);
    Validation v = new Validation(new BiopaxIdentifier(), "", true, null, 0, null); // auto-fix=true - fixex "no xref" error
    proteinModificationFeatureCvRule.check(v, mf);
    assertEquals(0, v.countErrors(mf.getUri(), null, "illegal.cv.term", null, false, false));
    assertEquals(1, v.countErrors(mf.getUri(), null, "no.xref.cv.terms", null, false, false)); //- one but fixed though -
    assertEquals(0, v.countErrors(null, null, null, null, false, true)); //- no unfixed errors
  }


  @Test
  public void normalizeTestFile() {
    SimpleIOHandler simpleReader = new SimpleIOHandler();
    simpleReader.mergeDuplicates(true);

    Normalizer normalizer = new Normalizer();
    String base = "http://test/";
    normalizer.setXmlBase(base);

    Model m = simpleReader.convertFromOWL(getClass().getResourceAsStream("/biopax-level3-test.owl"));
    normalizer.normalize(m, true);

    /*
     * Normalizer, if used alone (without Validator), does not turn DB or ID values to upper case
     * when generating a new xref URI anymore... (that was actually a bad idea);
     * "c00022", by the way, is illegal identifier (- C00022 is a valid KEGG id),
     * which wouldn't pass the Premerger (import pipeline) stage without critical errors...
     * BioPAX Normalizer alone cannot fix such IDs, because there are non-trivial cases,
     * where we cannot simply convert the first symbol to upper case...;
     * More importantly, bio identifiers are normally case-sensitive.
     */
    assertTrue(m.containsID(Normalizer.uri(base, "kegg compound", "c00002", UnificationXref.class)));
    assertTrue(m.containsID(Normalizer.uri(base, "kegg compound", "C00002", UnificationXref.class)));

    // However, using the validator (with autofix=true) and then - normalizer (as it's done in Premerger) together
    // will, in fact, fix and merge these two xrefs
    m = simpleReader.convertFromOWL(getClass().getResourceAsStream("/biopax-level3-test.owl"));
    Validation v = new Validation(new BiopaxIdentifier(), null, true, null, 0, null);
    v.setModel(m);
    m.setXmlBase(base);
    validator.validate(v);
    validator.getResults().remove(v);
    m = (Model) v.getModel();
    normalizer.normalize(m, true);

    assertFalse(m.containsID(Normalizer.uri(base, "KEGG COMPOUND", "c00002", UnificationXref.class)));
    assertTrue(m.containsID(Normalizer.uri(base, "KEGG COMPOUND", "C00002", UnificationXref.class)));
  }

  /*
   * Checks that correct classpath:profiles.properties (less strict profile)
   * is loaded (not that from the biopax-validator jar)
   */
  @Test
  public void rulesProfile() {
    assertEquals(Behavior.IGNORE,
      utils.getRuleBehavior("org.biopax.validator.rules.AcyclicPathwayRule", null));
  }


  @Test
  @DirtiesContext
  public void premergeAndMerge() throws IOException {
    //test env. sanity quick-test
    assertEquals("Pc0", service.settings().exportArchivePrefix());

    //should not fail:
    assertDoesNotThrow(
        () -> service.settings().getOrganismTaxonomyIds()
    );

    assertTrue(service.settings().getOrganismsAsTaxonomyToNameMap().containsKey("9606"));
    assertEquals("Homo sapiens", service.settings().getOrganismsAsTaxonomyToNameMap().get("9606"));

    Datasource ds = service.metadata().findByIdentifier("TEST_UNIPROT");
    assertNotNull(ds);
    ds = service.metadata().findByIdentifier("TEST_CHEBI");
    assertNotNull(ds);
    ds = service.metadata().findByIdentifier("TEST_MAPPING");
    assertNotNull(ds);

    PreMerger premerger = new PreMerger(service, validator);
    premerger.premerge();
    premerger.buildWarehouse(); //also writes Warehouse archive

    //Some assertions about the initial biopax warehouse model (before the merger is run)
    Model warehouse = CPathUtils.importFromTheArchive(service.settings().warehouseModelFile());
    assertNotNull(warehouse);
    assertFalse(warehouse.getObjects(ProteinReference.class).isEmpty());
    assertTrue(warehouse.containsID("bioregistry.io/uniprot:P0DP23"));
    assertFalse(warehouse.getObjects(SmallMoleculeReference.class).isEmpty());
    assertTrue(warehouse.containsID("bioregistry.io/chebi:20"));
    ProteinReference pr = (ProteinReference) warehouse.getByID("bioregistry.io/uniprot:P0DP23");
    assertNotNull(pr);
    assertNotNull(pr.getName());
    assertFalse(pr.getName().isEmpty());
    assertEquals("CALM1_HUMAN", pr.getDisplayName());
    assertNotNull(pr.getOrganism());
    assertEquals("Homo sapiens", pr.getOrganism().getStandardName());
    assertFalse(pr.getXref().isEmpty());

    service.index().refresh();
    assertTrue(warehouse.containsID("bioregistry.io/uniprot:Q8TD86"));
    // test some id-mapping using different srcDb names (UniProt synonyms...)
    Set<String> acs = service.map(List.of("A2A2M3"), "UNIPROT");
    assertFalse(acs.isEmpty());
    assertTrue(acs.contains("Q8TD86"));
    //can map an isoform id to primary AC with or without specifying the source db name (uniprot)
    Collection<String> ids = service.map(List.of("Q8TD86-1"), "UNIPROT");
    assertFalse(ids.isEmpty());

    //infers Q8TD86
    assertEquals("Q8TD86", ids.iterator().next());

    //can auto-remove RefSeq version numbers even when the type (refseq) of the ID is not provided!
    assertFalse(service.map(List.of("NP_619650.1"), "UNIPROT").isEmpty());
    assertFalse(service.map(List.of("NP_004334"), "UNIPROT").isEmpty());
    // also, with the first arg. is not null, map(..) will replace NP_619650.1 with NP_619650 before searching
    // (the id-mapping table only has canonical uniprot AC/IDs, not isoform IDs)
    acs = service.map(List.of("NP_619650"), "UNIPROT");
    assertTrue(acs.contains("Q8TD86"));
    assertTrue(warehouse.containsID("bioregistry.io/uniprot:Q8TD86"));

    ids = service.map(List.of("P01118"), "UNIPROT");
    assertEquals(1, ids.size());
    assertTrue(ids.contains("P01116"));
    ids = service.map(List.of("P01118-2"), "UNIPROT");//also works when any isoform id is used
    assertEquals(1, ids.size());
    assertTrue(ids.contains("P01116"));
    List<Mapping> mps = service.mapping().findByDstDbIgnoreCaseAndDstId("UNIPROT", "P01116");
    assertTrue(mps.size() > 2);

    List<String> srcids = Arrays.asList("P01118","1J7P"); //UniProt, PDB
    mps = service.mapping().findBySrcIdInAndDstDbIgnoreCase(srcids, "UniProt"); //to UniProt
    assertEquals(2, mps.size());
    assertArrayEquals(new String[]{"P01116","P0DP23"}, mps.stream().map(Mapping::getDstId).sorted().toArray());

    // **** MERGE ***
    Merger merger = new Merger(service);
    /* For simplicity, we don't use Datasource and thus bypass some Merger methods
     * (in production, we'd simply run as merger.merge())
     */
    //Load the test models from classpath:resources/merge folder
    final List<Model> pathwayModels = initPathwayModels();
    Model target = BioPAXLevel.L3.getDefaultFactory().createModel();
    for (Model m : pathwayModels) {
      ModelUtils.removeObjectsIfDangling(m, UtilityClass.class);
      ModelUtils.normalizeGenerics(m);
      merger.merge(m.getName(), m, target);
    }
    Model mainModel = merger.getMainModel();
    mainModel.merge(target);
    ModelUtils.removeObjectsIfDangling(mainModel, UtilityClass.class);
    //export the main model (for manual check up)
    //it's vital to save to and then read the model from file,
    //because doing so repairs inverse properties (e.g. entityReferenceOf)!
    merger.save();

    //load back the integrated test data model from the archive and validate it...
    Model m = CPathUtils.importFromTheArchive(service.settings().mainModelFile());
    assertMerge(m);

    //pid, reactome,humancyc,.. were there in the test
    assertEquals(4, m.getObjects(Provenance.class).size());

    //additional 'test' metadata entry
    Datasource md = new Datasource("test", Collections.singletonList("Reactome"),
            "Foo", "", "", "",
            METADATA_TYPE.BIOPAX, "", "", null,
            "", "free",
            0, 0, 0);

    // normally, setProvenanceFor gets called during Premerge stage
    md.setProvenanceFor(m, service.settings().getXmlBase());
    // which EXPLICITLY removes all other Provenance values from dataSource properties;
    assertEquals(1, m.getObjects(Provenance.class).size());

    // SERVICE-TIER features tests

    // Before next tests - update the main file due to changes to dataSource prop. above
    // (persistent and in-memory models must be the same as the indexer/searcher reads the model from file)
    new SimpleIOHandler(BioPAXLevel.L3).convertToOWL(m,
      new GZIPOutputStream(new FileOutputStream(service.settings().mainModelFile())));

    //index (it uses additional id-mapping service internally)
    service.setModel(m);
    service.index().save(m);

    // Test FULL-TEXT SEARCH
    SearchResponse resp;
    // search with a secondary (RefSeq) accession number -
    // NP_619650 (primary AC = Q8TD86) occurs in the test UniProt data only, not in the model
    resp = (SearchResponse) service.search("NP_619650", 0, ProteinReference.class, null, null);
    assertTrue(resp.getSearchHit().isEmpty());
    //P27797 should be both in the warehouse and merged models (other IDs: NP_004334, 2CLR,..)
    resp = (SearchResponse) service.search("NP_004334", 0, RelationshipXref.class, null, null);
    assertTrue(resp.getSearchHit().isEmpty()); //no hits; the ID was used for mapping, indexing, and then xref deleted
    //it should definitely find the PR or its primary UX by using its primary AC
    resp = (SearchResponse) service.search("P27797", 0, UnificationXref.class, null, null);
    assertTrue(resp.getSearchHit().isEmpty()); //Xrefs are not indexed anymore
    resp = (SearchResponse) service.search("P27797", 0, ProteinReference.class, null, null);
    assertFalse(resp.getSearchHit().isEmpty());
    //also, it should find the PR by using its secondary ID (though, there's no such xref physically present)
    resp = (SearchResponse) service.search("NP_004334", 0, ProteinReference.class, null, null);
    assertFalse(resp.getSearchHit().isEmpty());
    //also, it used to find Xref by any ID that maps to its 'id' value, but not after 12/2015 (Xrefs are not indexed anymore!)
    resp = (SearchResponse) service.search("NP_004334", 0, UnificationXref.class, null, null);
    assertTrue(resp.getSearchHit().isEmpty()); //so - no result is normal here
    // test search res. contains the list of data providers (standard names)
    ServiceResponse res = service.search("*", 0, PhysicalEntity.class, null, null);
    assertNotNull(res);
    assertTrue(res instanceof SearchResponse);
    assertFalse(res.isEmpty());
    assertFalse(((SearchResponse) res).getProviders().isEmpty());
    log.info("Providers found by second search: " + ((SearchResponse) res).getProviders().toString());

    // Test FETCH (get an object or subnetwork by URI or ID service; uses the full-text id-mapping index too)

    // fetch as BIOPAX
    res = service.fetch(OutputFormat.BIOPAX, null, false, "bioregistry.io/uniprot:P27797");
    assertNotNull(res);
    assertTrue(res instanceof DataResponse);
    assertFalse(res.isEmpty());
    assertTrue(((DataResponse) res).getData().toString().length() > 0);

    // fetch as SIF; apply only one SIF rule
    res = service.fetch(OutputFormat.SIF, Collections.singletonMap("pattern", "controls-production-of"),
      false, "http://pathwaycommons.org/test2#glucokinase_converts_alpha-D-glu_to_alpha-D-glu-6-p");
    assertTrue(res instanceof DataResponse);
    assertFalse(res.isEmpty());
    Object respData = ((DataResponse) res).getData();
    assertNotNull(respData);
    assertTrue(respData instanceof Path);
    assertNotNull(((DataResponse) res).getProviders());
    assertFalse(((DataResponse) res).getProviders().isEmpty());

    // fetch a small molecule by URI
    res = service.fetch(OutputFormat.BIOPAX, null, false,
      "bioregistry.io/chebi:20");
    assertNotNull(res);
    assertFalse(res.isEmpty());
    // fetch the same small molecule by ID (ChEBI, contains ":" in it...)
    res = service.fetch(OutputFormat.BIOPAX, null, false, "CHEBI:20");
    assertTrue(res instanceof DataResponse);
    assertFalse(res.isEmpty());

    //test traverse using path and URI
    res = service.traverse("ProteinReference/displayName",
      "bioregistry.io/uniprot:P27797");
    assertTrue(res instanceof TraverseResponse);
    assertFalse(res.isEmpty());
    List<String> vals = ((TraverseResponse) res).getTraverseEntry().get(0).getValue();
    assertFalse(vals.isEmpty());
    assertEquals("CALR_HUMAN", vals.get(0));
    // test - using ID instead of URI
    res = service.traverse("EntityReference/comment", "P27797");
    assertTrue(res instanceof TraverseResponse);
    assertFalse(res.isEmpty());
    vals = ((TraverseResponse) res).getTraverseEntry().get(0).getValue();
    assertEquals(1, vals.size()); //(just one - as we modified the uniprot converter - no "FUNCTION" comments anymore)
    // can now e.g. find pathways by participant IDs and list pathway names using one query
    res = service.traverse("Pathway/name", "P27797");
    assertTrue(res instanceof TraverseResponse);
    assertFalse(res.isEmpty());
    assertEquals(1, ((TraverseResponse) res).getTraverseEntry().size());
    vals = ((TraverseResponse) res).getTraverseEntry().get(0).getValue();
    assertEquals(4, vals.size());

    service.index().close();
  }


  // test everything
  // WARN: CHEBI ID, names, relationships here might be FAKE ones - just for these tests!
  private void assertMerge(Model mergedModel) {
    final String XML_BASE = service.settings().getXmlBase();
    // test proper merge of protein reference
    assertTrue(mergedModel.containsID("http://www.biopax.org/examples/myExample#Protein_54"));
    assertTrue(mergedModel.containsID("bioregistry.io/uniprot:P27797")); //CALR_HUMAN
    assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, "UNIPROT", "P27797", UnificationXref.class)));
    final String humanUri = Normalizer.uri(XML_BASE, "taxonomy", "9606", BioSource.class);
    assertTrue(mergedModel.containsID(humanUri));
    String clUri = Normalizer.uri(XML_BASE, "GO", "GO:0005737", CellularLocationVocabulary.class);
    assertTrue(mergedModel.containsID(clUri));

    assertTrue(mergedModel.containsID("bioregistry.io/uniprot:P13631"));
    assertFalse(mergedModel.containsID("bioregistry.io/uniprot:P22932"));
    //sec. ACs are not kept anymore (they're used in creating id-mapping and index, and then removed)
    assertFalse(mergedModel.containsID(Normalizer.uri(XML_BASE, "UNIPROT", "P01118_secondary-ac", RelationshipXref.class)));
    assertFalse(mergedModel.containsID("bioregistry.io/uniprot:P01118")); //must be replaced with P01116 and gone
    assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, "UNIPROT", "P01116", UnificationXref.class)));
    assertTrue(mergedModel.containsID("bioregistry.io/uniprot:P01116"));

    ProteinReference pr = (ProteinReference) mergedModel.getByID("bioregistry.io/uniprot:P27797");
    assertEquals(10, pr.getName().size()); //make sure this one is passed (important!)
    assertEquals("CALR_HUMAN", pr.getDisplayName());
    assertEquals("Calreticulin", pr.getStandardName());
    assertEquals(3, pr.getXref().size()); // 1 primary uniprot (secondary were removed); 1 'hgnc symbol' (no 'hgnc' id); 1 ncbigene id
    assertEquals("9606", pr.getOrganism().getXref().iterator().next().getId());

    // test proper merge of small molecule reference
    assertTrue(mergedModel.containsID("http://www.biopax.org/examples/myExample#beta-D-fructose_6-phosphate"));
    assertTrue(mergedModel.containsID("bioregistry.io/chebi:20"));
    SmallMoleculeReference smr = (SmallMoleculeReference) mergedModel.getByID("bioregistry.io/chebi:20");
    assertNotNull(smr.getStructure());
    assertSame(StructureFormatType.InChI, smr.getStructure().getStructureFormat());
    assertNotNull(smr.getStructure().getStructureData());

    assertTrue(!mergedModel.containsID("http://www.biopax.org/examples/myExample#ChemicalStructure_8"));

    // A special test id-mapping file (some PubChem SIDs and CIDs to ChEBI) is there present.
    // The PubChem:14438 SMR would not be replaced by CHEBI:20 if it were not having standard URI
    // (because the original xref has ambiguous db='PubChem' it wouldn't map to CHEBI:20);
    assertFalse(mergedModel.containsID("bioregistry.io/pubchem.substance:14438"));

    // but 14439 gets successfully replaced/merged
    assertFalse(mergedModel.containsID("bioregistry.io/pubchem.substance:14439")); //maps to CHEBI:28 by xrefs

    SmallMolecule sm = (SmallMolecule) mergedModel.getByID("http://pathwaycommons.org/test2#alpha-D-glucose_6-phosphate");
    smr = (SmallMoleculeReference) sm.getEntityReference();
    assertNotNull(smr);
    assertEquals("bioregistry.io/chebi:422", smr.getUri());
    // smr must not contain any member SMR anymore (changeed on 2015/11/26)
    // (if ChEBI OBO was previously converted by ChebiOntologyAnalysis)
    assertEquals(0, smr.getMemberEntityReference().size());
    assertEquals(4, smr.getXref().size());//0 PX, 1 UX and 3 RX (ChEBI) are there!
    SmallMoleculeReference msmr = (SmallMoleculeReference) mergedModel.getByID("bioregistry.io/chebi:20");
    assertEquals("(+)-camphene", msmr.getDisplayName());
    assertEquals("(1R,4S)-2,2-dimethyl-3-methylidenebicyclo[2.2.1]heptane", msmr.getStandardName());
    assertEquals(3, msmr.getXref().size());
    assertTrue(msmr.getMemberEntityReferenceOf().isEmpty());

    sm = (SmallMolecule) mergedModel.getByID("http://www.biopax.org/examples/myExample#beta-D-fructose_6-phosphate");
    smr = (SmallMoleculeReference) sm.getEntityReference();
    assertNotNull(smr);
    assertEquals(smr, msmr);//CHEBI:20

    smr = (SmallMoleculeReference) mergedModel.getByID("bioregistry.io/chebi:28");
    assertEquals(5, smr.getXref().size(), "chebi:28 smr.xref.size!=5"); // relationship xrefs were removed before merging
    assertEquals("(R)-linalool", smr.getDisplayName());
    assertEquals(5, smr.getEntityReferenceOf().size(), "chebi:28 entityReferenceOf.size!=5");

    BioSource bs = (BioSource) mergedModel.getByID(humanUri);
    assertNotNull(bs);
    assertTrue(bs.getUri().endsWith("9606"));
    assertEquals(1, bs.getXref().size());
    UnificationXref x = (UnificationXref) bs.getXref().iterator().next();
    assertEquals(1, x.getXrefOf().size());
    assertEquals(humanUri, x.getXrefOf().iterator().next().getUri());
    assertEquals(bs, x.getXrefOf().iterator().next());
    UnificationXref ux = (UnificationXref) mergedModel.getByID(Normalizer.uri(
      XML_BASE, "TAXONOMY", "9606", UnificationXref.class));
    assertEquals(1, ux.getXrefOf().size());

    // check features from the warehouse and pathway data were merged properly
    pr = (ProteinReference) mergedModel.getByID("bioregistry.io/uniprot:P01116");
    assertEquals(2, pr.getEntityFeature().size()); //from test models (no mod_res features from uniprot anymore)
    for (EntityFeature ef : pr.getEntityFeature()) {
      assertSame(pr, ef.getEntityFeatureOf());
    }

    //SmallMoleculeReference165390 SMR should have been replaced with one from the warehouse (ChEBI) or removed
    assertNull(mergedModel.getByID("http://identifiers.org/chebi/CHEBI:28")); //shoulda match by ID and become bioregistry.io/chebi:28!
    assertNull(mergedModel.getByID("http://biocyc.org/biopax/biopax-level3SmallMoleculeReference165390"));
    // check the canonical SMR has proper member/memberOf
    smr = (SmallMoleculeReference) mergedModel.getByID("bioregistry.io/chebi:28");
    // - was matched/replaced by the same URI Warehouse SMR
    sm = (SmallMolecule) mergedModel.getByID("http://biocyc.org/biopax/biopax-level3SmallMolecule173158");
    assertFalse(smr.getXref().isEmpty());
    assertTrue(smr.getMemberEntityReference().isEmpty()); //no memberERs after 2015/11/26 change in the converter
    assertFalse(smr.getEntityReferenceOf().isEmpty());
    assertTrue(smr.getEntityReferenceOf().contains(sm));

    //now, this SMR is in the warehouse despite having no InChIKey
    smr = (SmallMoleculeReference) mergedModel.getByID("http://identifiers.org/chebi/CHEBI:36141"); //the orig.; wasn't matched/replaced
    assertNotNull(smr);

    msmr = (SmallMoleculeReference) mergedModel.getByID(
      "http://biocyc.org/biopax/biopax-level3SmallMoleculeReference171684");
    assertNotNull(msmr);

  	//there were 3 member ERs in orig. file, but e.g. SmallMoleculeReference165390 was removed (dangling after replacing CHEBI:28)
    assertEquals(1, msmr.getMemberEntityReferenceOf().size());
    assertTrue(msmr.getMemberEntityReferenceOf().contains(smr));
  }


  private List<Model> initPathwayModels() throws IOException {
    final List<Model> pathwayModels = new ArrayList<>();

    SimpleIOHandler reader = new SimpleIOHandler();
    Normalizer normalizer = new Normalizer();
    normalizer.setXmlBase(service.settings().getXmlBase());
    reader.mergeDuplicates(true);

    Model model = reader.convertFromOWL(resourceLoader
      .getResource("classpath:merge/pathwaydata1.owl").getInputStream());
    model.setName("pathwaydata1");
    normalizer.normalize(model, true);
    pathwayModels.add(model);
    model = reader.convertFromOWL(resourceLoader
      .getResource("classpath:merge/pathwaydata2.owl").getInputStream());
    normalizer.normalize(model, true);
    model.setName("pathwaydata2");
    pathwayModels.add(model);
    model = reader.convertFromOWL(resourceLoader
      .getResource("classpath:merge/pid_60446.owl").getInputStream());
    normalizer.normalize(model, true);
    model.setName("pid_60446");
    pathwayModels.add(model); //PR P22932 caused the trouble
    model = reader.convertFromOWL(resourceLoader
      .getResource("classpath:merge/pid_6349.owl").getInputStream());
    normalizer.normalize(model, true);
    model.setName("pid_6349");
    pathwayModels.add(model); //Xref for P01118 caused the trouble
    model = reader.convertFromOWL(resourceLoader
      .getResource("classpath:merge/hcyc.owl").getInputStream());
    normalizer.normalize(model, true);
    model.setName("hcyc");
    pathwayModels.add(model);
    model = reader.convertFromOWL(resourceLoader
      .getResource("classpath:merge/hcyc2.owl").getInputStream());
		normalizer.normalize(model, true);
    model.setName("hcyc2");
    pathwayModels.add(model);

    return pathwayModels;
  }
}