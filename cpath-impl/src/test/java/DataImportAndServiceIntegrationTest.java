

import cpath.config.CPathSettings;
import cpath.dao.Analysis;
import cpath.dao.MetadataDAO;
import cpath.dao.PaxtoolsDAO;
import cpath.importer.Premerger;
import cpath.importer.internal.Merge;
import cpath.importer.internal.MergerImpl;
import cpath.importer.internal.PremergeImpl;
import cpath.service.CPathService;
import cpath.service.ErrorResponse;
import cpath.service.OutputFormat;
import cpath.service.jaxb.DataResponse;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.ServiceResponse;
import cpath.warehouse.beans.Mapping;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Metadata.METADATA_TYPE;

import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.io.SimpleIOHandler;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.imageio.ImageIO;


/**
 * @author rodche
 */
//@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:testContext-2.xml"})
public class DataImportAndServiceIntegrationTest {
	static Logger log = LoggerFactory.getLogger(DataImportAndServiceIntegrationTest.class);
	
	static final ResourceLoader resourceLoader = new DefaultResourceLoader();	
	static final String XML_BASE = CPathSettings.xmlBase();
	
	@Autowired
	CPathService service;
	
	@Autowired
	PaxtoolsDAO paxtoolsDAO;
	
	@Autowired
	MetadataDAO metadataDAO;

	
	@Test
	@DirtiesContext
	public void testPremergeAndMerge() throws IOException {
		
		//prepare the metadata
        // load the test metadata and create warehouse
		Premerger premerger = new PremergeImpl(metadataDAO, paxtoolsDAO, null, null);
		metadataDAO.addOrUpdateMetadata("classpath:metadata.conf");			
		premerger.buildWarehouse();
		premerger.updateIdMapping(false);
		// now metadata contains id-mapping tables, and paxtoolsDAO - warehouse data
		paxtoolsDAO.index();
				
		assertFalse(((Model)paxtoolsDAO).getObjects(ProteinReference.class).isEmpty());
		assertTrue(((Model)paxtoolsDAO).containsID("http://identifiers.org/uniprot/P62158"));
		assertFalse(((Model)paxtoolsDAO).getObjects(SmallMoleculeReference.class).isEmpty());
		assertTrue(((Model)paxtoolsDAO).containsID("http://identifiers.org/chebi/CHEBI:20"));				
							
		ProteinReference pr = (ProteinReference) ((Model)paxtoolsDAO).getByID("http://identifiers.org/uniprot/P62158");
		paxtoolsDAO.initialize(pr);
		paxtoolsDAO.initialize(pr.getName());
		paxtoolsDAO.initialize(pr.getXref());
		assertNotNull(pr);
		assertNotNull(pr.getName());
		assertFalse(pr.getName().isEmpty());
		assertNotNull(pr.getOrganism());
		assertEquals("Homo sapiens", pr.getOrganism().getStandardName());
		assertFalse(pr.getXref().isEmpty());
		
		// test id-mapping
		String ac = metadataDAO.mapIdentifier("A2A2M3", Mapping.Type.UNIPROT, "uniprot").iterator().next(); 
		assertEquals("Q8TD86", ac);
		assertTrue(((Model)paxtoolsDAO).containsID("http://identifiers.org/uniprot/" + ac));	
		assertTrue(metadataDAO.mapIdentifier("Q8TD86-1", Mapping.Type.UNIPROT, null).isEmpty());
		assertTrue(metadataDAO.mapIdentifier("Q8TD86-1", Mapping.Type.UNIPROT, "uniprot").isEmpty());
		//infers Q8TD86
		assertFalse(metadataDAO.mapIdentifier("Q8TD86-1", Mapping.Type.UNIPROT, "uniprot isoform").isEmpty());
		assertEquals("Q8TD86", metadataDAO.mapIdentifier("Q8TD86-1", Mapping.Type.UNIPROT, "uniprot isoform").iterator().next());			
				
		// Test full-text search		
		// search with a secondary (RefSeq) accession number
		SearchResponse resp =  paxtoolsDAO.search("NP_619650", 0, RelationshipXref.class, null, null);
		Collection<SearchHit> prs = resp.getSearchHit();
		assertFalse(prs.isEmpty());
		Collection<String> prIds = new HashSet<String>();
		for(SearchHit e : prs)
			prIds.add(e.getUri());
		
		String uri = Normalizer.uri(XML_BASE, "REFSEQ", "NP_619650", RelationshipXref.class);				
		assertTrue(prIds.contains(uri));
		
		// get that xref
		Xref x = (RelationshipXref) ((Model)paxtoolsDAO).getByID(uri);
		assertNotNull(x);
		paxtoolsDAO.initialize(x);
		paxtoolsDAO.initialize(x.getXrefOf());
		assertFalse(x.getXrefOf().isEmpty()); 
		
		// alternatively -
		ac = metadataDAO.mapIdentifier("NP_619650", Mapping.Type.UNIPROT, "refseq").iterator().next(); 
		assertTrue(metadataDAO.mapIdentifier("NP_619650.1", Mapping.Type.UNIPROT, null).isEmpty());
		
		//mapIdentifier uses 'suggest' method internally to infer NP_619650 from NP_619650.1 
		//(the id-mapping table only has canonical uniprot IDs)
		assertFalse(metadataDAO.mapIdentifier("NP_619650.1", Mapping.Type.UNIPROT, "refseq").isEmpty());
		assertEquals("Q8TD86", ac);
		assertTrue(((Model)paxtoolsDAO).containsID("http://identifiers.org/uniprot/" + ac));
			
		Metadata ds = metadataDAO.getMetadataByIdentifier("TEST_BIOPAX");
		assertNotNull(ds);
				
		// test icon/image I/O
		byte[] icon = ds.getIcon();
		assertNotNull(icon);
		assertTrue(icon.length >0);				
		BufferedImage bImageFromConvert = ImageIO.read(new ByteArrayInputStream(icon));
		ImageIO.write(bImageFromConvert, "gif", 
			new File(getClass().getClassLoader().getResource("").getPath() 
				+ File.separator + "out.gif"));		
		
		
		// MERGE
		
		//Load test models from files
		final List<Model> pathwayModels = initPathwayModels();	
		// note: in production, we'd simply run it as ImportFactory.newMerger(paxtoolsDAO,...).merge();
		// but here, due to a different way of how we initialized input models, we have to bypass that single call
		// and replace it with the following:
		//#BEGIN merge all models amd persist
		Model model = MergerImpl.load(paxtoolsDAO); //load warehouse (initial) model
		model.setXmlBase(XML_BASE);
		int i = 0;
		for(Model m : pathwayModels) {
			Merge merger = new Merge("model #" + i++, 
				m, metadataDAO, XML_BASE);
			merger.execute(model); //merge into the 'model'
		}
		paxtoolsDAO.merge(model); //persist
		//#END
		
		//check first whether it's ok after export/import as owl?
		final String outf = getClass().getClassLoader().getResource("").getPath() 
				+ File.separator + "testMerge.out.owl";
		FileOutputStream fos = new FileOutputStream(outf);
		paxtoolsDAO.exportModel(fos);
		fos.close();
		SimpleIOHandler reader = new SimpleIOHandler();
		reader.mergeDuplicates(true);
		Model m = reader.convertFromOWL(new FileInputStream(outf));			
		// run assertions for this in-memory model
		assertMerge(m);		
		
		// second, check the persistent model (what's actually going on within the main DB?)
		// this will be all run within a new DB transaction/session
		paxtoolsDAO.run(new Analysis() {
			@Override
			public void execute(Model model) {
				assertMerge(model);
			}
		});
		
		
		/*
		 * SERVICE-TIER features tests
		 */
		//pid, reactome,humancyc,.. were there in the test models
		assertEquals(4, m.getObjects(Provenance.class).size());
		//additional metadata entry
		Metadata md = new Metadata("test", "Reactome", "Foo", "", "", new byte[]{}, METADATA_TYPE.BIOPAX, "", "");		
		metadataDAO.saveMetadata(md);	
		// normally, setProvenanceFor gets called during Premerge stage
		md.setProvenanceFor(m); 
		// which EXPLICITELY REMOVEs all other datasources from object properties;
		// former Provenances normally become DANGLING...
		// next, merge there updates all biopax object relationships and data properties
		paxtoolsDAO.merge(m);
		// but does not remove dangling objects from the persistent model
		assertEquals(5, m.getObjects(Provenance.class).size()); 
		//still five (should not matter for queries/analyses)
		// reindex all
//		CPathUtils.cleanupDirectory(new File(CPathSettings.tmpDir() 
//				+ File.separator + "tests" + File.separator + "test2.idx"));
		paxtoolsDAO.index();
		
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
		assertTrue(data.contains("REACTS_WITH"));
		assertTrue(data.contains("GENERIC_OF"));
		assertTrue(data.contains("http://identifiers.org/uniprot/P27797"));
		
		// test search res. contains the list of data providers (standard names)
		res = service.search("*", 0, PhysicalEntity.class, null, null);
		assertNotNull(res);
		assertTrue(res instanceof SearchResponse);
		assertNotNull(((SearchResponse)res).getProviders());
		assertFalse(((SearchResponse)res).getProviders().isEmpty());
		log.info("Providers found by second search: " + ((SearchResponse)res).getProviders().toString());
	}
	
	
	// test everything here -
	private void assertMerge(Model mergedModel) {
		// test proper merge of protein reference
		assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, null, "http://www.biopax.org/examples/myExample#Protein_54", Protein.class)));
		assertTrue(mergedModel.containsID("http://identifiers.org/uniprot/P27797"));
		assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, "UNIPROT", "P27797", UnificationXref.class)));
		assertTrue(mergedModel.containsID("http://identifiers.org/taxonomy/9606"));
		assertTrue(mergedModel.containsID("http://identifiers.org/go/GO:0005737"));
		
		ProteinReference pr = (ProteinReference)mergedModel.getByID("http://identifiers.org/uniprot/P27797");
		assertEquals(10, pr.getName().size()); //make sure this one is passed (important!)
		assertEquals("CALR_HUMAN", pr.getDisplayName());
		assertEquals("Calreticulin", pr.getStandardName());
		assertEquals(11, pr.getXref().size());
		assertEquals("http://identifiers.org/taxonomy/9606", pr.getOrganism().getRDFId());
		
		// test proper merge of small molecule reference
		assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, null, "http://www.biopax.org/examples/myExample#beta-D-fructose_6-phosphate",SmallMolecule.class)));
		assertTrue(mergedModel.containsID("http://identifiers.org/chebi/CHEBI:20"));
		assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, "CHEBI", "CHEBI:20", ChemicalStructure.class)));
		assertTrue(!mergedModel.containsID(Normalizer.uri(XML_BASE, null, "http://www.biopax.org/examples/myExample#ChemicalStructure_8",ChemicalStructure.class)));
		assertTrue(!mergedModel.containsID("http://identifiers.org/pubchem.substance/14438"));
		assertTrue(!mergedModel.containsID("http://identifiers.org/pubchem.substance/14439"));
				
		SmallMolecule sm = (SmallMolecule)mergedModel.getByID(Normalizer.uri(XML_BASE, null, "http://pathwaycommons.org/test2#alpha-D-glucose_6-phosphate",SmallMolecule.class));
		SmallMoleculeReference smr = (SmallMoleculeReference)sm.getEntityReference();
		assertNotNull(smr);
		assertEquals("http://identifiers.org/chebi/CHEBI:422", smr.getRDFId());
		// smr must contain one member SMR
		assertEquals(1, smr.getMemberEntityReference().size());
//		System.out.println("merged chebi:422 xrefs: " + smr.getXref().toString());
		assertEquals(3, smr.getXref().size());		
		
		SmallMoleculeReference msmr = (SmallMoleculeReference)mergedModel.getByID("http://identifiers.org/chebi/CHEBI:20");
		assertEquals("(+)-camphene", msmr.getStandardName());
//		System.out.println("merged chebi:20 xrefs: " + msmr.getXref().toString());
		assertEquals(5, msmr.getXref().size());
		assertTrue(msmr.getMemberEntityReferenceOf().contains(smr));
		
		assertTrue(mergedModel.containsID("http://identifiers.org/uniprot/P13631"));
		assertFalse(mergedModel.containsID("http://identifiers.org/uniprot/P22932"));
		
		assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, "UNIPROT", "P01118", UnificationXref.class)));
		assertFalse(mergedModel.containsID("http://identifiers.org/uniprot/P01118"));
//		System.out.println("new xrefOf: " + newXref.getXrefOf().toString());
		assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, "UNIPROT", "P01116", UnificationXref.class)));
		assertTrue(mergedModel.containsID("http://identifiers.org/uniprot/P01116"));
		
		sm = (SmallMolecule)mergedModel.getByID(Normalizer.uri(XML_BASE, null, "http://www.biopax.org/examples/myExample#beta-D-fructose_6-phosphate",SmallMolecule.class));
		smr = (SmallMoleculeReference)sm.getEntityReference();

		smr = (SmallMoleculeReference) mergedModel.getByID("http://identifiers.org/chebi/CHEBI:28");
		assertEquals(7, smr.getXref().size());

		smr = (SmallMoleculeReference)mergedModel.getByID("http://identifiers.org/chebi/CHEBI:28");
		assertEquals(7, smr.getXref().size()); // relationship xrefs were removed before merging
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
//		System.out.println(px.getXrefOf());
		assertTrue(px.getXrefOf().contains(mergedModel.getByID("http://identifiers.org/uniprot/O75191")));
		assertTrue(px.getXrefOf().contains(mergedModel.getByID(Normalizer.uri(XML_BASE, null, "http://biocyc.org/biopax/biopax-level3Protein155359",Protein.class))));
		
		msmr = (SmallMoleculeReference)mergedModel
			.getByID(Normalizer.uri(XML_BASE, null, "http://biocyc.org/biopax/biopax-level3SmallMoleculeReference171684",SmallMoleculeReference.class));
		assertNotNull(msmr);
		assertNull(mergedModel.getByID(Normalizer.uri(XML_BASE, null, "http://biocyc.org/biopax/biopax-level3SmallMoleculeReference165390",SmallMoleculeReference.class)));
		smr = (SmallMoleculeReference)mergedModel.getByID("http://identifiers.org/chebi/CHEBI:28"); // was replaced from Warehouse
		sm = (SmallMolecule)mergedModel.getByID(Normalizer.uri(XML_BASE, null, "http://biocyc.org/biopax/biopax-level3SmallMolecule173158",SmallMolecule.class));
		assertFalse(smr.getXref().isEmpty());
		assertFalse(smr.getMemberEntityReference().isEmpty());	
		assertFalse(smr.getEntityReferenceOf().isEmpty());
		assertTrue(smr.getEntityReferenceOf().contains(sm));
		smr = (SmallMoleculeReference)mergedModel.getByID("http://identifiers.org/chebi/CHEBI:36141"); //wasn't replaced (not found in Warehouse!)
		
		// was 3 (in the file); but SmallMoleculeReference165390 was removed (became dangling after the replacement of chebi:28)
		assertEquals(1, msmr.getMemberEntityReferenceOf().size()); 
		assertTrue(msmr.getMemberEntityReferenceOf().contains(smr));
		
		// the following would be also true if we'd copy old prop./inv.prop relationships, but we do not
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
			.getResource("classpath:pathwaydata1.owl").getInputStream());
		normalizer.normalize(model);
		pathwayModels.add(model);
		
		model = null;
		model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:pathwaydata2.owl").getInputStream());
		normalizer.normalize(model);
		pathwayModels.add(model);
		model = null;
		model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:pid_60446.owl").getInputStream());
		normalizer.normalize(model);
		pathwayModels.add(model); //PR P22932 caused the trouble
		model = null;
		model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:pid_6349.owl").getInputStream());
		normalizer.normalize(model);
		pathwayModels.add(model); //Xref for P01118 caused the trouble
		model = null;
		model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:hcyc.owl").getInputStream());
		normalizer.normalize(model);
		pathwayModels.add(model);
		model = null;
		model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:hcyc2.owl").getInputStream());
//		normalizer.normalize(model);
		pathwayModels.add(model);	
		
		return pathwayModels;
	}
}