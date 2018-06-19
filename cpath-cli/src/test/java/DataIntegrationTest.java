import cpath.console.BiopaxValidatorConfig;
import cpath.console.CPathApplicationConfig;
import cpath.config.CPathSettings;
import cpath.jpa.Mapping;
import cpath.jpa.Metadata;
import cpath.jpa.Metadata.METADATA_TYPE;
import cpath.service.*;
import cpath.service.jaxb.DataResponse;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.ServiceResponse;

import cpath.service.jaxb.TraverseResponse;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.validator.api.Validator;
import org.biopax.paxtools.normalizer.Normalizer;
import org.junit.*;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;


/**
 * CPath2 Integration Tests (using test metadata, data, index).
 *
 * @author rodche
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {CPathApplicationConfig.class, BiopaxValidatorConfig.class})
//@ActiveProfiles("default") //is our test/demo profile - is the default anyway
public class DataIntegrationTest {
	static final Logger log = LoggerFactory.getLogger(DataIntegrationTest.class);
	static final ResourceLoader resourceLoader = new DefaultResourceLoader();	

	@Autowired
	CPathService service;
	
	@Autowired
	Validator validator;

	@Autowired
    CPathSettings cpath;

	@Test
	@DirtiesContext
	public void testPremergeAndMerge() throws IOException {
		//test env. sanity quick-test
		assertEquals("PathwayCommonsDemo0", cpath.exportArchivePrefix());

		//should not fail:
        cpath.getOrganismTaxonomyIds();

		assertTrue(cpath.getOrganismsAsTaxonomyToNameMap().containsKey("9606"));
		assertEquals("Homo sapiens", cpath.getOrganismsAsTaxonomyToNameMap().get("9606"));

		// prepare the metadata (always cleanup the data output directories FOR TESTS, because of recent updates in PreMerger!)
		// load the test metadata and create warehouse
		service.addOrUpdateMetadata("classpath:metadata.conf");	
		Metadata ds = service.metadata().findByIdentifier("TEST_UNIPROT");
		assertNotNull(ds);
		service.clear(ds);
		ds = service.metadata().findByIdentifier("TEST_CHEBI");
		assertNotNull(ds);
		service.clear(ds);
		ds = service.metadata().findByIdentifier("TEST_MAPPING");
		assertNotNull(ds);
		service.clear(ds);

		PreMerger premerger = new PreMerger(service, validator, true);
		premerger.premerge();		
		premerger.buildWarehouse(); //- also writes Warehouse archive
		
		//Some assertions about the initial biopax warehouse model (before the merger is run)	
		Model warehouse = CPathUtils.importFromTheArchive(cpath.warehouseModelFile());
		assertNotNull(warehouse);
		assertFalse(warehouse.getObjects(ProteinReference.class).isEmpty());
		assertTrue(warehouse.containsID("http://identifiers.org/uniprot/P62158"));
		assertFalse(warehouse.getObjects(SmallMoleculeReference.class).isEmpty());
		assertTrue(warehouse.containsID("http://identifiers.org/chebi/CHEBI:20"));											
		ProteinReference pr = (ProteinReference) warehouse.getByID("http://identifiers.org/uniprot/P62158");
		assertNotNull(pr);
		assertNotNull(pr.getName());
		assertFalse(pr.getName().isEmpty());
		assertEquals("CALM_HUMAN", pr.getDisplayName());
		assertNotNull(pr.getOrganism());
		assertEquals("Homo sapiens", pr.getOrganism().getStandardName());
		assertFalse(pr.getXref().isEmpty());
		
		// test some id-mapping using different srcDb names (UniProt synonyms...)
		assertFalse(service.map("A2A2M3", "UNIPROT").isEmpty());
		String ac = service.map("A2A2M3", "UNIPROT").iterator().next();
		assertEquals("Q8TD86", ac);
		
		assertTrue(warehouse.containsID("http://identifiers.org/uniprot/" + ac));
		//can map an isoform id to primary AC with or without specifying the source db name (uniprot)
		Collection<String> ids = service.map("Q8TD86-1", "UNIPROT");
		assertFalse(ids.isEmpty());

		//infers Q8TD86
		assertEquals("Q8TD86", ids.iterator().next());

		//can auto-remove RefSeq version numbers even when the type (refseq) of the ID is not provided!
		assertFalse(service.map("NP_619650.1", "UNIPROT").isEmpty());
		assertFalse(service.map("NP_004334", "UNIPROT").isEmpty());
		// also, with the first arg. is not null, map(..)
		// calls 'suggest' method to replace NP_619650.1 with NP_619650
		// (the id-mapping table only has canonical uniprot IDs, no isoform IDs)
		ac = service.map("NP_619650", "UNIPROT").iterator().next();
		assertEquals("Q8TD86", ac);
		assertTrue(warehouse.containsID("http://identifiers.org/uniprot/" + ac));

		ids = service.map("P01118","UNIPROT");
		assertEquals(1, ids.size());
		assertTrue(ids.contains("P01116"));
		ids = service.map("P01118-2","UNIPROT");//also works when any isoform id is used
		assertEquals(1, ids.size());
		assertTrue(ids.contains("P01116"));
		List<Mapping> mps = service.mapping().findByDestIgnoreCaseAndDestId("UNIPROT", "P01116");
		assertTrue(mps.size()>2);
		mps = service.mapping().findBySrcIdAndDestIgnoreCase("P01118", "UniProt");
		assertEquals(1, mps.size());
		assertTrue("P01116".equals(mps.iterator().next().getDestId()));
		mps = service.mapping().findBySrcIgnoreCaseAndSrcIdAndDestIgnoreCase("UNIPROT", "P01118", "UNIPROT");
		assertEquals(1, mps.size());
		assertTrue("P01116".equals(mps.iterator().next().getDestId()));
		mps = service.mapping().findBySrcIdAndDestIgnoreCase("1J7P", "UNIPROT");//PDB to UniProt
		assertFalse(mps.isEmpty());
		assertEquals(1, mps.size());
		assertTrue("P62158".equals(mps.iterator().next().getDestId()));

		// **** MERGE ***
		Merger merger = new Merger(service);
		
		/* In this test, for simplicity, we don't use Metadata 
		 * and thus bypass some of Merger methods 
		 * (in production, we'd simply run as merger.merge())
		*/
		//Load test models from files
		final List<Model> pathwayModels = initPathwayModels();	
		int i = 0;
		Model target = BioPAXLevel.L3.getDefaultFactory().createModel();
		for(Model m : pathwayModels) {
			merger.merge("", m, target); //use empty "" description
		}	
		merger.getMainModel().merge(target);
		
		//export the main model (for manual check up)
		//it's vital to save to and then read the model from file,
		//because doing so repairs inverse properties (e.g. entityReferenceOf)!
		merger.save(); 
		//load back the model from archive
		Model m = CPathUtils.importFromTheArchive(cpath.mainModelFile());
		
		//Check the all-data integrated model
		assertMerge(m);

		//pid, reactome,humancyc,.. were there in the test models
		assertEquals(4, m.getObjects(Provenance.class).size());
		
		//additional 'test' metadata entry
		Metadata md = new Metadata("test", "Reactome", "Foo", "", "",
				"", METADATA_TYPE.BIOPAX, "", "", null, "free");
		service.save(md);	
		// normally, setProvenanceFor gets called during Premerge stage
		md.setProvenanceFor(m); 
		// which EXPLICITELY REMOVEs all other Provenance values from dataSource properties;
		assertEquals(1, m.getObjects(Provenance.class).size()); 		


		// SERVICE-TIER features tests

		// Before next tests - update the main file due to changes to dataSource prop. above
		// (persistent and in-memory models must be the same as the indexer/searcher reads the model from file)
		new SimpleIOHandler(BioPAXLevel.L3).convertToOWL(m, 
			new GZIPOutputStream(new FileOutputStream(cpath.mainModelFile())));

		//index (it uses additional id-mapping service internally)
        cpath.setAdminEnabled(true);
		service.index();
        cpath.setAdminEnabled(false);

		// Test FULL-TEXT SEARCH
		SearchResponse resp;

		// search with a secondary (RefSeq) accession number -
		// NP_619650 (primary AC = Q8TD86) occurs in the test UniProt data only, not in the model
//Xrefs are not indexed anymore (only Entity and EntityReference type get indexed now)
//		resp =  (SearchResponse) service.search("NP_619650", 0, RelationshipXref.class, null, null);
//		assertTrue(resp.getSearchHit().isEmpty()); //no hits - ok (such xrefs were removed from both warehouse and model)
		resp =  (SearchResponse) service.search("NP_619650", 0, ProteinReference.class, null, null);
		assertTrue(resp.getSearchHit().isEmpty());

		//P27797 should be both in the warehouse and merged models (other IDs: NP_004334, 2CLR,..)
		resp =  (SearchResponse) service.search("NP_004334", 0, RelationshipXref.class, null, null);
		assertTrue(resp.getSearchHit().isEmpty()); //no hits; the ID was used for mapping, indexing, and then xref deleted
		//it should definitely find the PR or its primary UX by using its primary AC
		resp =  (SearchResponse) service.search("P27797", 0, UnificationXref.class, null, null);
		assertTrue(resp.getSearchHit().isEmpty()); //Xrefs are not indexed anymore
		resp =  (SearchResponse) service.search("P27797", 0, ProteinReference.class, null, null);
		assertFalse(resp.getSearchHit().isEmpty());
		//also, it should find the PR by using its secondary ID (though, there's no such xref physically present)
		resp =  (SearchResponse) service.search("NP_004334", 0, ProteinReference.class, null, null);
		assertFalse(resp.getSearchHit().isEmpty());

		//also, it could previously find an Xref by some other ID that maps to its 'id' value; since 12/2015, Xrefs are not indexed anymore
		resp =  (SearchResponse) service.search("NP_004334", 0, UnificationXref.class, null, null);
		assertTrue(resp.getSearchHit().isEmpty()); //so - no result is normal here
//		assertTrue(resp.getSearchHit().iterator().next().getUri().endsWith("P27797")); //was valid assertion until 12/2015

		// test search res. contains the list of data providers (standard names)
		ServiceResponse res = service.search("*", 0, PhysicalEntity.class, null, null);
		assertNotNull(res);
		assertTrue(res instanceof SearchResponse);
		assertFalse(res.isEmpty());
		assertFalse(((SearchResponse)res).getProviders().isEmpty());
		log.info("Providers found by second search: " + ((SearchResponse)res).getProviders().toString());


		// Test FETCH (get an object or subnetwork by URI or ID service)

		// fetch as BIOPAX
		res = service.fetch(OutputFormat.BIOPAX, null, false, "http://identifiers.org/uniprot/P27797");
		assertNotNull(res);
		assertTrue(res instanceof DataResponse);
		assertFalse(res.isEmpty());
		assertTrue(((DataResponse)res).getData().toString().length()>0);		
		
		// fetch as SIF; apply only one SIF rule
		res = service.fetch(OutputFormat.SIF, Collections.singletonMap("pattern","controls-production-of"),
		false, "http://pathwaycommons.org/test2#glucokinase_converts_alpha-D-glu_to_alpha-D-glu-6-p");
		assertTrue(res instanceof DataResponse);
		assertFalse(res.isEmpty());
		Object respData = ((DataResponse)res).getData();
		assertNotNull(respData);
		assertTrue(respData instanceof Path);
		assertNotNull(((DataResponse)res).getProviders());
		assertFalse(((DataResponse)res).getProviders().isEmpty());

		// fetch a small molecule by URI
		res = (DataResponse) service.fetch(OutputFormat.BIOPAX, null, false,
			"http://identifiers.org/chebi/CHEBI:20");
		assertNotNull(res);
		assertFalse(res.isEmpty());
		// fetch the same small molecule by ID (ChEBI, contains ":" in it...)
		res = service.fetch(OutputFormat.BIOPAX, null, false, "CHEBI:20");
		assertTrue(res instanceof DataResponse);
		assertFalse(res.isEmpty());

		//test traverse using path and URI
		res = service.traverse("ProteinReference/displayName",
				"http://identifiers.org/uniprot/P27797");
		assertTrue(res instanceof TraverseResponse);
		assertFalse(res.isEmpty());
		List<String> vals = ((TraverseResponse)res).getTraverseEntry().get(0).getValue();
		assertFalse(vals.isEmpty());
		assertEquals("CALR_HUMAN",vals.get(0));
		// test - using ID instead of URI
		res = service.traverse("EntityReference/comment", "P27797");
		assertTrue(res instanceof TraverseResponse);
		assertFalse(res.isEmpty());
		vals = ((TraverseResponse)res).getTraverseEntry().get(0).getValue();
		assertEquals(2, vals.size());
		// can now e.g. find pathways by participant IDs and list pathway names using one query
		res = service.traverse("Pathway/name", "P27797");
		assertTrue(res instanceof TraverseResponse);
		assertFalse(res.isEmpty());
		assertEquals(1, ((TraverseResponse) res).getTraverseEntry().size());
		vals = ((TraverseResponse)res).getTraverseEntry().get(0).getValue();
		assertEquals(4, vals.size());
	}
	
	
	// test everything
	// WARN: CHEBI ID, names, relationships here might be FAKE ones - just for these tests!
	private void assertMerge(Model mergedModel) {
	    final String XML_BASE = cpath.getXmlBase();
		// test proper merge of protein reference
		assertTrue(mergedModel.containsID("http://www.biopax.org/examples/myExample#Protein_54"));
		assertTrue(mergedModel.containsID("http://identifiers.org/uniprot/P27797"));
		assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, "UNIPROT", "P27797", UnificationXref.class)));
		final String HsUri = Normalizer.uri(XML_BASE, "TAXONOMY", "9606", BioSource.class);
		assertTrue(mergedModel.containsID(HsUri));
		assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, "GO", "GO:0005737", CellularLocationVocabulary.class)));
		
		assertTrue(mergedModel.containsID("http://identifiers.org/uniprot/P13631"));
		assertFalse(mergedModel.containsID("http://identifiers.org/uniprot/P22932"));
		//sec. ACs are not kept anymore (they're used in creating id-mapping and index, and then removed)
		assertFalse(mergedModel.containsID(Normalizer.uri(XML_BASE, "UNIPROT", "P01118_secondary-ac", RelationshipXref.class)));
		assertFalse(mergedModel.containsID("http://identifiers.org/uniprot/P01118")); //must be replaced with P01116 and gone
		assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, "UNIPROT", "P01116", UnificationXref.class)));
		assertTrue(mergedModel.containsID("http://identifiers.org/uniprot/P01116"));
		
		ProteinReference pr = (ProteinReference)mergedModel.getByID("http://identifiers.org/uniprot/P27797");
		assertEquals(10, pr.getName().size()); //make sure this one is passed (important!)
		assertEquals("CALR_HUMAN", pr.getDisplayName());
		assertEquals("Calreticulin", pr.getStandardName());
//		System.out.println("CALR_HUMAN xrefs: " + pr.getXref().toString());
		assertEquals(2, pr.getXref().size()); // 1 - primary uniprot (sec.ACs were removed); 1 - 'hgnc symbol' (no 'hgnc' id)
		assertEquals("9606", pr.getOrganism().getXref().iterator().next().getId());
		
		// test proper merge of small molecule reference
		assertTrue(mergedModel.containsID("http://www.biopax.org/examples/myExample#beta-D-fructose_6-phosphate"));
		assertTrue(mergedModel.containsID("http://identifiers.org/chebi/CHEBI:20"));
//		assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, "CHEBI", "CHEBI:20", ChemicalStructure.class))); //OLD SDF converter used such URI
		SmallMoleculeReference smr = (SmallMoleculeReference) mergedModel.getByID("http://identifiers.org/chebi/CHEBI:20");
		assertNotNull(smr.getStructure());
		assertTrue(StructureFormatType.InChI == smr.getStructure().getStructureFormat());
		assertNotNull(smr.getStructure().getStructureData());

		assertTrue(!mergedModel.containsID("http://www.biopax.org/examples/myExample#ChemicalStructure_8"));

		// A special test id-mapping file (some PubChem SIDs and CIDs to ChEBI) is there present.
		// The PubChem:14438 SMR would not be replaced by CHEBI:20 if it were not having standard URI
		// (because the original xref has ambiguous db='PubChem' it wouldn't map to CHEBI:20);
		assertFalse(mergedModel.containsID("http://identifiers.org/pubchem.substance/14438"));

		// but 14439 gets successfully replaced/merged
		assertFalse(mergedModel.containsID("http://identifiers.org/pubchem.substance/14439")); //maps to CHEBI:28 by xrefs
				
		SmallMolecule sm = (SmallMolecule)mergedModel.getByID("http://pathwaycommons.org/test2#alpha-D-glucose_6-phosphate");
		smr = (SmallMoleculeReference)sm.getEntityReference();
		assertNotNull(smr);
		assertEquals("http://identifiers.org/chebi/CHEBI:422", smr.getUri());
		// smr must not contain any member SMR anymore (changeed on 2015/11/26)
		// (if ChEBI OBO was previously converted by ChebiOntologyAnalysis)
		assertEquals(0, smr.getMemberEntityReference().size());
		System.out.println("merged chebi:422 xrefs: " + smr.getXref().toString());
		assertEquals(4, smr.getXref().size());//0 PX, 1 UX and 3 RX (ChEBI) are there!
		SmallMoleculeReference msmr = (SmallMoleculeReference)mergedModel.getByID("http://identifiers.org/chebi/CHEBI:20");
		assertEquals("(+)-camphene", msmr.getDisplayName());
		assertEquals("(1R,4S)-2,2-dimethyl-3-methylidenebicyclo[2.2.1]heptane", msmr.getStandardName());
		assertEquals(3, msmr.getXref().size());
		assertTrue(msmr.getMemberEntityReferenceOf().isEmpty());
		
		sm = (SmallMolecule)mergedModel.getByID("http://www.biopax.org/examples/myExample#beta-D-fructose_6-phosphate");
		smr = (SmallMoleculeReference)sm.getEntityReference();
		assertNotNull(smr);
		assertEquals(smr, msmr);//CHEBI:20

		smr = (SmallMoleculeReference) mergedModel.getByID("http://identifiers.org/chebi/CHEBI:28");
//		System.out.println("merged chebi:28 xrefs: " + smr.getXref().toString());
		assertEquals(5, smr.getXref().size()); // relationship xrefs were removed before merging
		assertEquals("(R)-linalool", smr.getDisplayName());
		assertEquals(5, smr.getEntityReferenceOf().size());
		
		BioSource bs = (BioSource) mergedModel.getByID(HsUri);
		assertNotNull(bs);
		assertTrue(bs.getUri().endsWith("9606"));
		assertEquals(1, bs.getXref().size());
//		System.out.println("Organism: " + bs.getUri() + "; xrefs: " + bs.getXref());
		UnificationXref x = (UnificationXref) bs.getXref().iterator().next();
		System.out.println("Organism: " + bs.getUri() + "; its xrefOf: " + x.getXrefOf());
		assertEquals(1, x.getXrefOf().size());
		assertEquals(HsUri, x.getXrefOf().iterator().next().getUri());
		assertEquals(bs, x.getXrefOf().iterator().next());
//		System.out.println(x.getUri() + " is " + x);
		UnificationXref ux = (UnificationXref) mergedModel.getByID(Normalizer.uri(
				XML_BASE, "TAXONOMY", "9606", UnificationXref.class));
//		System.out.println(ux.getUri() + " - " + ux);
		assertEquals(1, ux.getXrefOf().size());
		
		// check features from the warehouse and pathway data were merged properly
		pr = (ProteinReference)mergedModel.getByID("http://identifiers.org/uniprot/P01116");
		assertEquals(5, pr.getEntityFeature().size()); // 3 from test uniprot + 2 from test data files
		for(EntityFeature ef : pr.getEntityFeature()) {
			assertTrue(pr == ef.getEntityFeatureOf());
		}
		
		// inspired by humancyc case ;)
		assertTrue(mergedModel.containsID("http://identifiers.org/pubmed/9763671"));
		PublicationXref px = (PublicationXref) mergedModel.getByID("http://identifiers.org/pubmed/9763671");
		assertEquals(1, px.getXrefOf().size());
		//these are not the two original ProteinReference (those got replaced/removed)
		//the xref is not copied from the original PR to the merged (canonical) one anymore -
		assertFalse(px.getXrefOf().contains(mergedModel.getByID("http://identifiers.org/uniprot/O75191")));
		//the owner of the px is the Protein
		String pUri = "http://biocyc.org/biopax/biopax-level3Protein155359";
//		System.out.println("pUri=" + pUri);
		Protein p = (Protein) mergedModel.getByID(pUri);
		assertNotNull(p);
//		System.out.println(px + ", xrefOf=" + px.getXrefOf());
		for(XReferrable r : px.getXrefOf()) {
			if(r.getUri().equals(pUri))
				assertEquals(p, r);
		}
		
		//SmallMoleculeReference165390 SMR should have been replaced with one from the warehouse (ChEBI) or removed
		assertNull(mergedModel.getByID("http://biocyc.org/biopax/biopax-level3SmallMoleculeReference165390"));
		// check the canonical SMR has proper member/memberOf
		smr = (SmallMoleculeReference)mergedModel.getByID("http://identifiers.org/chebi/CHEBI:28");
		// - was matched/replaced by the same URI Warehouse SMR
		sm = (SmallMolecule)mergedModel.getByID("http://biocyc.org/biopax/biopax-level3SmallMolecule173158");
		assertFalse(smr.getXref().isEmpty());
		assertTrue(smr.getMemberEntityReference().isEmpty()); //no memberERs after 2015/11/26 change in the converter
		assertFalse(smr.getEntityReferenceOf().isEmpty());
		assertTrue(smr.getEntityReferenceOf().contains(sm));

		//now, this SMR is in the warehouse despite having no InChIKey
		smr = (SmallMoleculeReference)mergedModel.getByID("http://identifiers.org/chebi/CHEBI:36141");
		assertNotNull(smr);

		msmr = (SmallMoleculeReference) mergedModel.getByID(
				"http://biocyc.org/biopax/biopax-level3SmallMoleculeReference171684");
		assertNotNull(msmr);

//		// there were 3 member ERs in the orig. file, but,
//		// e.g., SmallMoleculeReference165390 was removed (became dangling after the replacement of CHEBI:28)
		assertEquals(1, msmr.getMemberEntityReferenceOf().size());
		assertTrue(msmr.getMemberEntityReferenceOf().contains(smr));

		// the following would be also true if we'd keep old property/inverse prop. relationships, but we do not
//		assertEquals(2, msmr.getMemberEntityReferenceOf().size());
//		assertTrue(msmr.getMemberEntityReferenceOf().contains(mergedModel.getByID("http://identifiers.org/chebi/CHEBI:28")));	
	}
		
	
	private List<Model> initPathwayModels() throws IOException {
		final List<Model> pathwayModels = new ArrayList<>();
		
		SimpleIOHandler reader = new SimpleIOHandler();
		Normalizer normalizer = new Normalizer();
		normalizer.setXmlBase(cpath.getXmlBase());
		reader.mergeDuplicates(true);

		Model model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:merge/pathwaydata1.owl").getInputStream());
		normalizer.normalize(model);
		pathwayModels.add(model);
		model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:merge/pathwaydata2.owl").getInputStream());
		normalizer.normalize(model);
		pathwayModels.add(model);
		model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:merge/pid_60446.owl").getInputStream());
		normalizer.normalize(model);
		pathwayModels.add(model); //PR P22932 caused the trouble
		model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:merge/pid_6349.owl").getInputStream());
		normalizer.normalize(model);
		pathwayModels.add(model); //Xref for P01118 caused the trouble
		model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:merge/hcyc.owl").getInputStream());
		normalizer.normalize(model);
		pathwayModels.add(model);
		model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:merge/hcyc2.owl").getInputStream());
//		normalizer.normalize(model);
		pathwayModels.add(model);	
		
		return pathwayModels;
	}
}