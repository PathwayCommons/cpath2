package cpath.importer;

import cpath.config.CPathSettings;
import cpath.dao.Analysis;
import cpath.dao.CPathUtils;
import cpath.jpa.Mapping;
import cpath.jpa.Metadata;
import cpath.jpa.Metadata.METADATA_TYPE;
import cpath.service.CPathService;
import cpath.service.ErrorResponse;
import cpath.service.OutputFormat;
import cpath.service.jaxb.DataResponse;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.ServiceResponse;

import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.validator.api.Validator;
import org.biopax.validator.utils.Normalizer;

import org.junit.*;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * @author rodche
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={
		"classpath:META-INF/spring/applicationContext-dao.xml",
		"classpath:META-INF/spring/applicationContext-jpa.xml",
		"classpath:META-INF/spring/appContext-validator.xml"})
@ActiveProfiles("dev")
public class DataImportTest {
	static final Logger log = LoggerFactory.getLogger(DataImportTest.class);
	static final ResourceLoader resourceLoader = new DefaultResourceLoader();	
	static final String XML_BASE = CPathSettings.getInstance().getXmlBase();
	
	static {
		//this triggers intermediate and result files' location
		// (will be under tmp instead of CPATH2_HOME...)
		CPathSettings.setDevelop(true);
	}
		
	@Autowired
	CPathService service;
	
	@Autowired
	Validator validator;
	

	
	@Test
	@DirtiesContext
	public void testPremergeAndMerge() throws IOException {		
		//prepare the metadata
        // load the test metadata and create warehouse
		service.addOrUpdateMetadata("classpath:metadata.conf");	
		Metadata ds = service.metadata().findByIdentifier("TEST_UNIPROT");
		assertNotNull(ds);
		ds = service.metadata().findByIdentifier("TEST_CHEBI");
		assertNotNull(ds);		
		ds = service.metadata().findByIdentifier("TEST_MAPPING");
		assertNotNull(ds);
		
		PreMerger premerger = new PreMerger(service, validator, null);
		premerger.premerge();		
		premerger.buildWarehouse(); //- also writes Warehouse archive
		
		//Some assertions about the initial biopax warehouse model (before the merger is run)	
		Model warehouse = CPathUtils.loadWarehouseBiopaxModel();		
		assertFalse(warehouse.getObjects(ProteinReference.class).isEmpty());
		assertTrue(warehouse.containsID("http://identifiers.org/uniprot/P62158"));
		assertFalse(warehouse.getObjects(SmallMoleculeReference.class).isEmpty());
		assertTrue(warehouse.containsID("http://identifiers.org/chebi/CHEBI:20"));											
		ProteinReference pr = (ProteinReference) warehouse.getByID("http://identifiers.org/uniprot/P62158");
		assertNotNull(pr);
		assertNotNull(pr.getName());
		assertFalse(pr.getName().isEmpty());
		assertNotNull(pr.getOrganism());
		assertEquals("Homo sapiens", pr.getOrganism().getStandardName());
		assertFalse(pr.getXref().isEmpty());
		
		// test some id-mapping using different srcDb names (UniProt synonyms...)
		String ac = service.map(null, "A2A2M3", "UNIPROT").iterator().next(); 
		assertEquals("Q8TD86", ac);
		ac = service.map("uniprot knowledgebase", "A2A2M3", "UNIPROT").iterator().next(); 
		assertEquals("Q8TD86", ac);
		ac = service.map("uniprot", "A2A2M3", "UNIPROT").iterator().next(); 
		assertEquals("Q8TD86", ac);
		
		assertTrue(warehouse.containsID("http://identifiers.org/uniprot/" + ac));	
		assertTrue(service.map(null, "Q8TD86-1", "UNIPROT").isEmpty());
		assertTrue(service.map("uniprot", "Q8TD86-1", "UNIPROT").isEmpty());
		assertTrue(service.map("uniprot knowledgebase", "Q8TD86-1", "UNIPROT").isEmpty());
		//infers Q8TD86
		assertFalse(service.map("uniprot isoform", "Q8TD86-1", "UNIPROT").isEmpty());
		assertEquals("Q8TD86", service.map("uniprot isoform", "Q8TD86-1", "UNIPROT").iterator().next());					
		// also -
		assertTrue(service.map(null, "NP_619650.1", "UNIPROT").isEmpty());		
		// when the last arg. is not null, map(..) calls 'suggest' method to replace NP_619650.1 with NP_619650
		// (the id-mapping table only has canonical uniprot IDs, no isoform IDs)
		assertFalse(service.map("refseq", "NP_619650.1", "UNIPROT").isEmpty());
		ac = service.map("refseq", "NP_619650", "UNIPROT").iterator().next(); 
		assertEquals("Q8TD86", ac);
		assertTrue(warehouse.containsID("http://identifiers.org/uniprot/" + ac));
		
		Set<String> ids = service.map("P01118");
		assertTrue(ids.size()==1);
		assertTrue(ids.contains("P01116"));
		ids = service.map("uniprot", "P01118", "uniprot");
		assertTrue(ids.size()==1);
		assertTrue(ids.contains("P01116"));
		List<Mapping> mps = service.mapping().findByDestIgnoreCaseAndDestId("UNIPROT", "P01116");
		assertTrue(mps.size()>2);
//		System.out.println(mps);
		mps = service.mapping().findBySrcIdAndDestIgnoreCase("P01118", "UniProt");
		assertTrue(mps.size()==1);
		assertTrue("P01116".equals(mps.iterator().next().getDestId()));
		mps = service.mapping().findBySrcIgnoreCaseAndSrcIdAndDestIgnoreCase("UNIPROT", "P01118", "UNIPROT");
		assertTrue(mps.size()==1);
		assertTrue("P01116".equals(mps.iterator().next().getDestId()));
		
		
		// **** MERGE ***
		
		//Load test models from files
		final List<Model> pathwayModels = initPathwayModels();
		
		// note: in production we'd run it as ImportFactory.newMerger(paxtoolsDAO,...).merge();
		// cpath2 metadata contains the warehouse and id-mapping tables
		Merger merger = new Merger(service, true);
		int i = 0;
		for(Model m : pathwayModels) {
			merger.merge("model #" + i++, m);
		}
		merger.save(); //to the file
		
		//get the model from the archive and test it
		Model m = CPathUtils.loadMainBiopaxModel();	
		//check the model
		assertMerge(m);	

		//pid, reactome,humancyc,.. were there in the test models
		assertEquals(4, m.getObjects(Provenance.class).size());
		
		//additional metadata entry
		Metadata md = new Metadata("test", "Reactome", "Foo", "", "", 
				"", METADATA_TYPE.BIOPAX, "", "", null, "free");		
		service.save(md);	
		// normally, setProvenanceFor gets called during Premerge stage
		md.setProvenanceFor(m); 
		// which EXPLICITELY REMOVEs all other datasources from object properties;
		// former Provenances normally become DANGLING...
		// but does not remove dangling objects from the persistent model
		assertEquals(5, m.getObjects(Provenance.class).size()); 
		
		
		// persist
		service.biopax().removeAll();
		service.biopax().merge(m);		
		assertEquals(5, ((Model)service.biopax()).getObjects(Provenance.class).size()); 
		//still five (should not matter for queries/analyses)
		// index
		service.biopax().index();
		
		// Test the persistent model (runs within a transaction)
		service.biopax().run(new Analysis() {
			@Override
			public void execute(Model model) {
				assertMerge(model);
			}
		});		
			
		
		/*
		 * SERVICE-TIER features tests
		 */
		
		// Test full-text search	
		// search with a secondary (RefSeq) accession number
		//NP_619650 occurs in the warehouse only, not in the merged model
		SearchResponse resp =  service.biopax().search("NP_619650", 0, RelationshipXref.class, null, null);
		assertTrue(resp.getSearchHit().isEmpty());
		//now find another one in the main model
		resp =  service.biopax().search("NP_005099", 0, RelationshipXref.class, null, null);
		Collection<SearchHit> prs = resp.getSearchHit();
		assertFalse(prs.isEmpty());
		Collection<String> prIds = new HashSet<String>();
		for(SearchHit e : prs)
			prIds.add(e.getUri());		
		String uri = Normalizer.uri(XML_BASE, "REFSEQ", "NP_005099_identity", RelationshipXref.class);				
		assertTrue(prIds.contains(uri));
		Xref x = (RelationshipXref) ((Model)service.biopax()).getByID(uri);
		assertNotNull(x);
		service.biopax().initialize(x);
		service.biopax().initialize(x.getXrefOf());
		assertFalse(x.getXrefOf().isEmpty());
		pr = (ProteinReference) ((Model)service.biopax()).getByID("http://identifiers.org/uniprot/O75191");
		assertTrue(x.getXrefOf().contains(pr));
		
		// fetch as BIOPAX
		ServiceResponse res = service.fetch(OutputFormat.BIOPAX, "http://identifiers.org/uniprot/P27797");
		assertNotNull(res);
		assertFalse(res instanceof ErrorResponse);
		assertTrue(res instanceof DataResponse);
		assertFalse(res.isEmpty());
		assertTrue(((DataResponse)res).getData().toString().length()>0);		
		
		// fetch as SIF
		res = service.fetch(OutputFormat.BINARY_SIF, 
			Normalizer.uri(XML_BASE, null, 
				"http://pathwaycommons.org/test2#glucokinase_converts_alpha-D-glu_to_alpha-D-glu-6-p", 
					Catalysis.class));
		assertNotNull(res);
		assertTrue(res instanceof DataResponse);
		assertFalse(res.isEmpty());
		String data = (String) ((DataResponse)res).getData();		
		assertNotNull(data);
		assertNotNull(((DataResponse)res).getProviders());
		assertFalse(((DataResponse)res).getProviders().isEmpty());

		log.info(data);
		assertTrue(data.contains("reacts-with"));
		assertTrue(data.contains("used-to-produce"));
		assertTrue(data.contains("CALR"));
		
		// test search res. contains the list of data providers (standard names)
		res = service.search("*", 0, PhysicalEntity.class, null, null);
		assertNotNull(res);
		assertTrue(res instanceof SearchResponse);
		assertNotNull(((SearchResponse)res).getProviders());
		assertFalse(((SearchResponse)res).getProviders().isEmpty());
		log.info("Providers found by second search: " + ((SearchResponse)res).getProviders().toString());
	}
	
	
	// test everything
	// WARN: CHEBI ID, names, relationships here might be FAKE ones - just for these tests!
	private void assertMerge(Model mergedModel) {
		// test proper merge of protein reference
		assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, null, "http://www.biopax.org/examples/myExample#Protein_54", Protein.class)));
		assertTrue(mergedModel.containsID("http://identifiers.org/uniprot/P27797"));
		assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, "UNIPROT", "P27797", UnificationXref.class)));
		assertTrue(mergedModel.containsID("http://identifiers.org/taxonomy/9606"));
		assertTrue(mergedModel.containsID("http://identifiers.org/go/GO:0005737"));
		
		assertTrue(mergedModel.containsID("http://identifiers.org/uniprot/P13631"));
		assertFalse(mergedModel.containsID("http://identifiers.org/uniprot/P22932"));		
		assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, "UNIPROT", "P01118", UnificationXref.class)));
		assertFalse(mergedModel.containsID("http://identifiers.org/uniprot/P01118")); //must be replaced with P01116 and gone
		assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, "UNIPROT", "P01116", UnificationXref.class)));
		assertTrue(mergedModel.containsID("http://identifiers.org/uniprot/P01116"));
		
		ProteinReference pr = (ProteinReference)mergedModel.getByID("http://identifiers.org/uniprot/P27797");
		assertEquals(10, pr.getName().size()); //make sure this one is passed (important!)
		assertEquals("CALR_HUMAN", pr.getDisplayName());
		assertEquals("Calreticulin", pr.getStandardName());
		System.out.println("CALR_HUMAN xrefs: " + pr.getXref().toString());
		assertEquals(11, pr.getXref().size()); //11, no duplicate xrefs (10 of the warehouse PR, +1 - comes from...)!
		assertEquals("http://identifiers.org/taxonomy/9606", pr.getOrganism().getRDFId());
		
		// test proper merge of small molecule reference
		assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, null, "http://www.biopax.org/examples/myExample#beta-D-fructose_6-phosphate",SmallMolecule.class)));
		assertTrue(mergedModel.containsID("http://identifiers.org/chebi/CHEBI:20"));
//		assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, "CHEBI", "CHEBI:20", ChemicalStructure.class))); //OLD SDF converter used such URI
		SmallMoleculeReference smr = (SmallMoleculeReference) mergedModel.getByID("http://identifiers.org/chebi/CHEBI:20");
		assertNotNull(smr.getStructure());
		assertTrue(StructureFormatType.InChI == smr.getStructure().getStructureFormat());
		assertNotNull(smr.getStructure().getStructureData());
		
		assertTrue(!mergedModel.containsID(Normalizer.uri(XML_BASE, null, "http://www.biopax.org/examples/myExample#ChemicalStructure_8",ChemicalStructure.class)));

		//a special test id-mapping file (SID, CID to ChEBI) is present.
		// The 14438 SMR won't be replaced/merged, because its original xref has 'PubChem' as db name (AMBIGUOUS)
		assertTrue(mergedModel.containsID("http://identifiers.org/pubchem.substance/14438")); //if it were 'PubChem-substance' it would map to CHEBI:20
		// But 14439 is replaced/merged
		assertFalse(mergedModel.containsID("http://identifiers.org/pubchem.substance/14439")); //maps to CHEBI:28
				
		SmallMolecule sm = (SmallMolecule)mergedModel.getByID(Normalizer.uri(XML_BASE, null, 
				"http://pathwaycommons.org/test2#alpha-D-glucose_6-phosphate",SmallMolecule.class));
		smr = (SmallMoleculeReference)sm.getEntityReference();
		assertNotNull(smr);
		assertEquals("http://identifiers.org/chebi/CHEBI:422", smr.getRDFId());
		// smr must contain one member SMR
		// but only if ChEBI OBO was also imported (ChebiOntologyAnalysis run)!
		assertEquals(1, smr.getMemberEntityReference().size());
//		System.out.println("merged chebi:422 xrefs: " + smr.getXref().toString());
		assertEquals(19, smr.getXref().size());//1 unif. + 9 rel.  + 9 PubMed xrefs are there!
		
		SmallMoleculeReference msmr = (SmallMoleculeReference)mergedModel.getByID("http://identifiers.org/chebi/CHEBI:20");
		assertEquals("(+)-camphene", msmr.getDisplayName());
		assertEquals("(1R,4S)-2,2-dimethyl-3-methylidenebicyclo[2.2.1]heptane", msmr.getStandardName());
//		System.out.println("merged chebi:20 xrefs: " + msmr.getXref().toString());
		assertEquals(9, msmr.getXref().size());
		assertTrue(msmr.getMemberEntityReferenceOf().contains(smr));
		
		sm = (SmallMolecule)mergedModel.getByID(Normalizer.uri(XML_BASE, null, 
				"http://www.biopax.org/examples/myExample#beta-D-fructose_6-phosphate",SmallMolecule.class));
		smr = (SmallMoleculeReference)sm.getEntityReference();
		assertNotNull(smr);
		assertEquals(smr, msmr);//CHEBI:20

		smr = (SmallMoleculeReference) mergedModel.getByID("http://identifiers.org/chebi/CHEBI:28");
//		System.out.println("merged chebi:28 xrefs: " + smr.getXref().toString());
		assertEquals(12, smr.getXref().size()); // relationship xrefs were removed before merging
		assertEquals("(R)-linalool", smr.getDisplayName());
		assertEquals(5, smr.getEntityReferenceOf().size());
		
		BioSource bs = (BioSource) mergedModel.getByID("http://identifiers.org/taxonomy/9606");
		assertNotNull(bs);
		assertEquals(1, bs.getXref().size());
		UnificationXref x = (UnificationXref) bs.getXref().iterator().next();
		assertEquals(1, x.getXrefOf().size());
		assertEquals("http://identifiers.org/taxonomy/9606", x.getXrefOf().iterator().next().getRDFId());
//		System.out.println(x.getRDFId() + " is " + x);
		UnificationXref ux = (UnificationXref) mergedModel.getByID(Normalizer.uri(XML_BASE, "TAXONOMY", "9606", UnificationXref.class));
//		System.out.println(ux.getRDFId() + " - " + ux);
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
		assertEquals(2, px.getXrefOf().size()); 
		//these are not the two original ProteinReference (got replaced/removed)
		//the first owner of the px (PR)
		assertTrue(px.getXrefOf().contains(mergedModel.getByID("http://identifiers.org/uniprot/O75191")));
		//the second owner of the px (Protein)
		String pUri = Normalizer.uri(XML_BASE, null, "http://biocyc.org/biopax/biopax-level3Protein155359", Protein.class);
		System.out.println("pUri=" + pUri);
		Protein p = (Protein) mergedModel.getByID(pUri);
		assertNotNull(p);
		System.out.println(px + ", xrefOf=" + px.getXrefOf());
//		assertTrue(px.getXrefOf().contains(p));
		for(XReferrable r : px.getXrefOf()) {
			if(r.getRDFId().equals(pUri))
				assertEquals(p, r);
		}
		
		// check a SMR and member SMR
		msmr = (SmallMoleculeReference) mergedModel
			.getByID(Normalizer.uri(XML_BASE, null, "http://biocyc.org/biopax/biopax-level3SmallMoleculeReference171684",SmallMoleculeReference.class));
		assertNotNull(msmr);
		assertNull(mergedModel.getByID(Normalizer.uri(XML_BASE, null, "http://biocyc.org/biopax/biopax-level3SmallMoleculeReference165390",SmallMoleculeReference.class)));
		smr = (SmallMoleculeReference)mergedModel.getByID("http://identifiers.org/chebi/CHEBI:28"); // was matched/replaced the same URI Warehouse SMR
		sm = (SmallMolecule)mergedModel.getByID(Normalizer.uri(XML_BASE, null, "http://biocyc.org/biopax/biopax-level3SmallMolecule173158",SmallMolecule.class));
		assertFalse(smr.getXref().isEmpty());
		assertFalse(smr.getMemberEntityReference().isEmpty());	
		assertFalse(smr.getEntityReferenceOf().isEmpty());
		assertTrue(smr.getEntityReferenceOf().contains(sm));
		smr = (SmallMoleculeReference)mergedModel.getByID("http://identifiers.org/chebi/CHEBI:36141"); //wasn't replaced (not found in Warehouse!)		
		
		// there were 3 member ERs in the orig. file, but, 
		// e.g., SmallMoleculeReference165390 was removed (became dangling after the replacement of CHEBI:28)
		assertEquals(1, msmr.getMemberEntityReferenceOf().size()); 
		assertTrue(msmr.getMemberEntityReferenceOf().contains(smr));
		
		// the following would be also true if we'd keep old property/inverse prop. relationships, but we do not
//		assertEquals(2, msmr.getMemberEntityReferenceOf().size());
//		assertTrue(msmr.getMemberEntityReferenceOf().contains(mergedModel.getByID("http://identifiers.org/chebi/CHEBI:28")));	
	}
		
	
	private static List<Model> initPathwayModels() throws IOException {
		final List<Model> pathwayModels = new ArrayList<Model>();
		
		SimpleIOHandler reader = new SimpleIOHandler();
		Normalizer normalizer = new Normalizer();
		normalizer.setXmlBase(XML_BASE);
		reader.mergeDuplicates(true);
		Model model;

		model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:merge/pathwaydata1.owl").getInputStream());
		normalizer.normalize(model);
		pathwayModels.add(model);
		
		model = null;
		model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:merge/pathwaydata2.owl").getInputStream());
		normalizer.normalize(model);
		pathwayModels.add(model);
		model = null;
		model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:merge/pid_60446.owl").getInputStream());
		normalizer.normalize(model);
		pathwayModels.add(model); //PR P22932 caused the trouble
		model = null;
		model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:merge/pid_6349.owl").getInputStream());
		normalizer.normalize(model);
		pathwayModels.add(model); //Xref for P01118 caused the trouble
		model = null;
		model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:merge/hcyc.owl").getInputStream());
		normalizer.normalize(model);
		pathwayModels.add(model);
		model = null;
		model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:merge/hcyc2.owl").getInputStream());
//		normalizer.normalize(model);
		pathwayModels.add(model);	
		
		return pathwayModels;
	}
}