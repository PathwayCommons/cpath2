package cpath.importer.internal;

import cpath.config.CPathSettings;
import cpath.dao.Analysis;
import cpath.dao.CPathUtils;
import cpath.dao.MetadataDAO;
import cpath.dao.PaxtoolsDAO;
import cpath.importer.Premerger;
import cpath.warehouse.beans.*;

import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.validator.utils.Normalizer;

import org.junit.*;

import static org.junit.Assert.*;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * @author rodche
 */
public class CPathMergerTest {

	final static ResourceLoader resourceLoader = new DefaultResourceLoader();	
	static final String XML_BASE = CPathSettings.xmlBase();
	

	@BeforeClass
	public static void init() {
//		System.out.println("Preparing...");
		//drop/make an empty DBs
		CPathUtils.createDatabase(CPathSettings.TEST_DB); 
		
		ClassPathXmlApplicationContext context = 
			new ClassPathXmlApplicationContext("classpath:testContext-dao.xml");
		final PaxtoolsDAO warehouseDAO = (PaxtoolsDAO) context.getBean("paxtoolsDAO");
		final MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");

        // load the test metadata and create warehouse
		Premerger premerger = new PremergeImpl(metadataDAO, (PaxtoolsDAO) warehouseDAO, null, null);
		metadataDAO.importMetadata("classpath:metadata.conf");			
		premerger.buildWarehouse();
		premerger.updateIdMapping(false);
		
		context.close();		
	}
	
		
	@Test
	public void testMerge() throws IOException {		
		final List<Model> pathwayModels = initPathwayModels();
			
		ClassPathXmlApplicationContext context = 
			new ClassPathXmlApplicationContext("classpath:testContext-dao.xml");
		final PaxtoolsDAO paxtoolsDAO = (PaxtoolsDAO) context.getBean("paxtoolsDAO");
		final MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");		

		assertNotNull(paxtoolsDAO);
		assertNotNull(metadataDAO);
		
		// note: in production we'd run it as ImportFactory.newMerger(paxtoolsDAO,...).merge();
		// cpath2 metadata contains the warehouse and id-mapping tables
		int i = 0;
		for(Model m : pathwayModels) {
			Analysis merger = new MergerAnalysis("model #" + i++, 
				m, metadataDAO, ((Model)paxtoolsDAO).getXmlBase());
			paxtoolsDAO.run(merger);
		}
		
		//check first whether it's ok after export/import as owl?	
		paxtoolsDAO.exportModel(new FileOutputStream("target/testMerge.out.owl"));					
		SimpleIOHandler reader = new SimpleIOHandler();
		reader.mergeDuplicates(true);
		Model m = reader.convertFromOWL(new FileInputStream("target/testMerge.out.owl"));			
		// run assertions for this in-memory model
		assertMerge(m, paxtoolsDAO);		
		
		// second, check the persistent model (what's actually going on within the main DB?)
		// this will be all run within a new DB transaction/session
		paxtoolsDAO.run(new Analysis() {
			@Override
			public void execute(Model model) {
				assertMerge(model, paxtoolsDAO);
			}
		});
		
		context.close();
	}
	
	
	// test everything here -
	private void assertMerge(Model mergedModel, PaxtoolsDAO paxtoolsDAO) {
		// test proper merge of protein reference
		assertTrue(mergedModel.containsID("http://www.biopax.org/examples/myExample#Protein_54"));
		assertTrue(mergedModel.containsID("http://identifiers.org/uniprot/P27797"));
		assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, "UNIPROT", "P27797", UnificationXref.class)));
		assertTrue(mergedModel.containsID("http://identifiers.org/taxonomy/9606"));
		assertTrue(mergedModel.containsID("http://identifiers.org/obo.go/GO:0005737"));
		
		ProteinReference pr = (ProteinReference)mergedModel.getByID("http://identifiers.org/uniprot/P27797");
		assertEquals(10, pr.getName().size()); //make sure this one is passed (important!)
		assertEquals("CALR_HUMAN", pr.getDisplayName());
		assertEquals("Calreticulin", pr.getStandardName());
		assertEquals(11, pr.getXref().size());
		assertEquals("http://identifiers.org/taxonomy/9606", pr.getOrganism().getRDFId());
		
		// test proper merge of small molecule reference
		assertTrue(mergedModel.containsID("http://www.biopax.org/examples/myExample#beta-D-fructose_6-phosphate"));
		assertTrue(mergedModel.containsID("http://identifiers.org/chebi/CHEBI:20"));
		assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, "CHEBI", "CHEBI:20", ChemicalStructure.class)));
		assertTrue(!mergedModel.containsID("http://www.biopax.org/examples/myExample#ChemicalStructure_8"));
		assertTrue(!mergedModel.containsID("http://identifiers.org/pubchem.substance/14438"));
		assertTrue(!mergedModel.containsID("http://identifiers.org/pubchem.substance/14439"));
				
		SmallMolecule sm = (SmallMolecule)mergedModel.getByID("http://pathwaycommons.org/test2#alpha-D-glucose_6-phosphate");
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
		
		// if the following fails, try to cleanup your java.io.tmpdir...
		assertTrue(((Model) paxtoolsDAO).containsID("http://identifiers.org/uniprot/P13631"));
		assertFalse(((Model) paxtoolsDAO).containsID("http://identifiers.org/uniprot/P22932"));		
		assertTrue(mergedModel.containsID("http://identifiers.org/uniprot/P13631"));
		assertFalse(mergedModel.containsID("http://identifiers.org/uniprot/P22932"));
		
		assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, "UNIPROT", "P01118", UnificationXref.class)));
		assertFalse(mergedModel.containsID("http://identifiers.org/uniprot/P01118"));
//		System.out.println("new xrefOf: " + newXref.getXrefOf().toString());
		assertTrue(mergedModel.containsID(Normalizer.uri(XML_BASE, "UNIPROT", "P01116", UnificationXref.class)));
		assertTrue(mergedModel.containsID("http://identifiers.org/uniprot/P01116"));
		
		sm = (SmallMolecule)mergedModel.getByID("http://www.biopax.org/examples/myExample#beta-D-fructose_6-phosphate");
		smr = (SmallMoleculeReference)sm.getEntityReference();

		smr = (SmallMoleculeReference) ((Model) paxtoolsDAO).getByID("http://identifiers.org/chebi/CHEBI:28");
		paxtoolsDAO.initialize(smr);
//		System.out.println("warehouse chebi:28 xrefs: " + smr.getXref().toString());
		assertEquals(7, smr.getXref().size());

		smr = (SmallMoleculeReference)mergedModel.getByID("http://identifiers.org/chebi/CHEBI:28");
//		System.out.println("merged chebi:28 xrefs: " + smr.getXref().toString());
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
		assertTrue(px.getXrefOf().contains(mergedModel.getByID("http://biocyc.org/biopax/biopax-level3Protein155359")));
		
		msmr = (SmallMoleculeReference)mergedModel
			.getByID("http://biocyc.org/biopax/biopax-level3SmallMoleculeReference171684");
		assertNotNull(msmr);
		assertNull(mergedModel.getByID("http://biocyc.org/biopax/biopax-level3SmallMoleculeReference165390"));
		smr = (SmallMoleculeReference)mergedModel.getByID("http://identifiers.org/chebi/CHEBI:28"); // was replaced from Warehouse
		sm = (SmallMolecule)mergedModel.getByID("http://biocyc.org/biopax/biopax-level3SmallMolecule173158");
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
		
	
	public List<Model> initPathwayModels() throws IOException {
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