package cpath.importer.internal;

import cpath.dao.PaxtoolsDAO;
import cpath.dao.internal.DataServicesFactoryBean;
import cpath.dao.internal.PaxtoolsModelDAO;
import cpath.warehouse.*;
import cpath.warehouse.beans.*;
import cpath.warehouse.beans.Metadata.TYPE;

import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.io.SimpleIOHandler;

import org.junit.*;
import static org.junit.Assert.*;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * 
 * @author rodche
 */
public class CPathMergerTest {
	private static final MetadataDAO metadataDAO;
	private static final WarehouseDAO proteinsDAO;
	private static final WarehouseDAO moleculesDAO;
	private static final WarehouseDAO cvRepository;
	
	private Set<Model> pathwayModels; // pathways to merge
	
	/**
	 * Mocks an empty CV repository
	 * 
	 * @author rodche
	 */
	public static class MockCvWarehouse implements WarehouseDAO {
		@Override
		public Set<String> getByXref(Set<? extends Xref> arg0,
				Class<? extends XReferrable> arg1)  {
			return Collections.EMPTY_SET;
		}
		@Override
		public <T extends BioPAXElement> T getObject(String arg0, Class<T> arg1) { 
			return null;
		}
	}
	
	
	static {
		// init the test database
		DataServicesFactoryBean.createSchema("cpath2_test");

		// load beans
		ApplicationContext context = new ClassPathXmlApplicationContext(
			new String[] {
				"classpath:testContext-whDAO.xml", 
//				"classpath:applicationContext-cvRepository.xml"
				});
		proteinsDAO = (WarehouseDAO) context.getBean("proteinsDAO");
		moleculesDAO = (WarehouseDAO) context.getBean("moleculesDAO");
//		cvRepository = (WarehouseDAO) context.getBean("cvFetcher");
		cvRepository = new MockCvWarehouse();
		metadataDAO = (MetadataDAO) context.getBean("metadataDAO");

        /* load the test metadata and ONLY (!) 
		 * test proteins and molecules data into the warehouse
		 */
		FetcherImpl fetcher = new FetcherImpl();
		try {
			Collection<Metadata> metadata = fetcher.getMetadata("classpath:metadata.html");
			for (Metadata mdata : metadata) {
				// store metadata in the warehouse
				metadataDAO.importMetadata(mdata);
				fetcher.fetchData(mdata);
				if (mdata.getType() == TYPE.PROTEIN) {
					// store PRs in the warehouse
					fetcher.storeWarehouseData(mdata, (Model)proteinsDAO);
				}
				else if (mdata.getType() == TYPE.SMALL_MOLECULE) {
					// store SMRs in the warehouse
					fetcher.storeWarehouseData(mdata, (Model)moleculesDAO);
				} 
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	
	@Before
	/*
	 * this is required before each (merge) test,
	 * because the source models become incomplete/useless
	 * during the merge!
	 */
	public void initPathwayModels() throws IOException {
		final ResourceLoader resourceLoader = new DefaultResourceLoader();
		pathwayModels = new HashSet<Model>();
		SimpleIOHandler reader = new SimpleIOHandler();
		reader.mergeDuplicates(true);
		Model model;
		model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:test-normalized.owl").getInputStream());
		if(model == null)
			fail("Failed to import test data from classpath:test-normalized.owl");
		pathwayModels.add(model);
		
		model = null;
		model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:test-normalized-2.owl").getInputStream());
		pathwayModels.add(model);
		model = null;
		model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:pid_60446.owl").getInputStream());
		pathwayModels.add(model); //PR P22932 caused the trouble
		model = null;
		model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:pid_6349.owl").getInputStream());
		pathwayModels.add(model); //Xref for P01118 caused the trouble
	}
	
	
	@Test
	public void testInMemoryModelMerge() throws IOException 
	{
		PaxtoolsDAO memoPcModel = new PaxtoolsModelDAO(BioPAXLevel.L3);
		MergerImpl merger = new MergerImpl(memoPcModel, metadataDAO,
				moleculesDAO, proteinsDAO, cvRepository);
		
		for(Model model : pathwayModels) {
			merger.merge(model);
		}
		
		// dump owl out for review
		OutputStream out = new FileOutputStream(
			getClass().getClassLoader().getResource("").getPath() 
				+ File.separator + "InMemoryMergerTest.out.owl");
		(new SimpleIOHandler(BioPAXLevel.L3)).convertToOWL((Model) memoPcModel, out);
		
		assertMerge((Model) memoPcModel);
	}
	
	private void assertMerge(Model mergedModel) {
		// test proper merge of protein reference
		assertTrue(mergedModel.containsID("http://www.biopax.org/examples/myExample#Protein_54"));
		assertTrue(mergedModel.containsID("urn:miriam:uniprot:P27797"));
		assertTrue(mergedModel.containsID("urn:biopax:UnificationXref:UNIPROT_P27797"));
		assertTrue(!mergedModel.containsID("urn:biopax:UnificationXref:Uniprot_P27797"));
		assertTrue(mergedModel.containsID("urn:miriam:taxonomy:9606"));
		
		ProteinReference pr = (ProteinReference)mergedModel.getByID("urn:miriam:uniprot:P27797");
		assertEquals(9, pr.getName().size());
		assertEquals("CALR_HUMAN", pr.getDisplayName());
		assertEquals("Calreticulin", pr.getStandardName());
		assertEquals(6, pr.getXref().size());
		assertEquals("urn:miriam:taxonomy:9606", pr.getOrganism().getRDFId());
		
		assertTrue(mergedModel.containsID("urn:miriam:obo.go:GO%3A0005737"));
		
		// test proper merge of small molecule reference
		assertTrue(mergedModel.containsID("http://www.biopax.org/examples/myExample#beta-D-fructose_6-phosphate"));
		assertTrue(mergedModel.containsID("urn:miriam:chebi:20"));
		assertTrue(mergedModel.containsID("urn:biopax:ChemicalStructure:chebi_20"));
		assertTrue(!mergedModel.containsID("http://www.biopax.org/examples/myExample#ChemicalStructure_8"));
		assertTrue(!mergedModel.containsID("urn:miriam:pubchem.substance:14438"));
		assertTrue(!mergedModel.containsID("urn:miriam:pubchem.substance:14439"));
				
		SmallMolecule sm = (SmallMolecule)mergedModel.getByID("http://pathwaycommons.org/test2#alpha-D-glucose_6-phosphate");
		SmallMoleculeReference smr = (SmallMoleculeReference)sm.getEntityReference();
		assertEquals("urn:miriam:chebi:422", smr.getRDFId());
		// smr must contain one member SMR
		if(mergedModel instanceof PaxtoolsDAO) {
			((PaxtoolsDAO) mergedModel).initialize(smr);
		}
		assertEquals(1, smr.getMemberEntityReference().size());
//		System.out.println("merged chebi:422 xrefs: " + smr.getXref().toString());
		assertEquals(3, smr.getXref().size());		
		
		SmallMoleculeReference msmr = (SmallMoleculeReference)mergedModel.getByID("urn:miriam:chebi:20");
		assertEquals("(+)-camphene", msmr.getStandardName());
//		System.out.println("merged chebi:20 xrefs: " + msmr.getXref().toString());
		assertEquals(4, msmr.getXref().size());
		if(mergedModel instanceof PaxtoolsDAO) {
			((PaxtoolsDAO) mergedModel).initialize(msmr);
		}
		assertTrue(msmr.getMemberEntityReferenceOf().contains(smr));
		
		// if the following fails, try to cleanup your java.io.tmpdir...
		assertTrue(((Model) proteinsDAO).containsID("urn:miriam:uniprot:P13631"));
		assertFalse(((Model) proteinsDAO).containsID("urn:miriam:uniprot:P22932"));
		
		assertTrue(mergedModel.containsID("urn:miriam:uniprot:P13631"));
		assertFalse(mergedModel.containsID("urn:miriam:uniprot:P22932"));
		
		assertTrue(mergedModel.containsID("urn:biopax:UnificationXref:UNIPROT_P01118"));
		assertFalse(mergedModel.containsID("urn:miriam:uniprot:P01118"));
//		System.out.println("new xrefOf: " + newXref.getXrefOf().toString());
		assertTrue(mergedModel.containsID("urn:biopax:UnificationXref:UNIPROT_P01116"));
		assertTrue(mergedModel.containsID("urn:miriam:uniprot:P01116"));
				
		pr = (ProteinReference)mergedModel.getByID("urn:miriam:uniprot:P27797");
		if(mergedModel instanceof PaxtoolsDAO)
			((PaxtoolsDAO) mergedModel).initialize(pr);
		assertEquals(9, pr.getName().size());
		assertEquals("CALR_HUMAN", pr.getDisplayName());
		assertEquals("Calreticulin", pr.getStandardName());
		assertEquals(6, pr.getXref().size());
		assertEquals("urn:miriam:taxonomy:9606", pr.getOrganism().getRDFId());
		
		// TODO: add asserts for CV
		assertTrue(mergedModel.containsID("urn:miriam:obo.go:GO%3A0005737"));

		sm = (SmallMolecule)mergedModel.getByID("http://www.biopax.org/examples/myExample#beta-D-fructose_6-phosphate");
		if(mergedModel instanceof PaxtoolsDAO)
			((PaxtoolsDAO) mergedModel).initialize(sm);
		smr = (SmallMoleculeReference)sm.getEntityReference();

		smr = moleculesDAO.getObject("urn:miriam:chebi:28", SmallMoleculeReference.class);
//		System.out.println("warehouse chebi:28 xrefs: " + smr.getXref().toString());
		assertEquals(14, smr.getXref().size());

		smr = (SmallMoleculeReference)mergedModel.getByID("urn:miriam:chebi:28");
		if(mergedModel instanceof PaxtoolsDAO)
			((PaxtoolsDAO) mergedModel).initialize(smr);
//		System.out.println("merged chebi:28 xrefs: " + smr.getXref().toString());
		assertEquals(6, smr.getXref().size()); // relationship xrefs were removed before merging
		assertEquals("(R)-linalool", smr.getDisplayName());
		assertEquals(4, smr.getEntityReferenceOf().size());
		
		
		UnificationXref x = (UnificationXref) mergedModel.getByID("urn:biopax:UnificationXref:TAXONOMY_9606");
		assertEquals(1, x.getXrefOf().size());
	}
	
	
	@Test
	public void testMergeIntoDAO() throws IOException {
		// init the target test db
		DataServicesFactoryBean.createSchema("cpath2_testpc"); // target db, for pcDAO
		final PaxtoolsDAO pcDAO = (PaxtoolsDAO) (
				new ClassPathXmlApplicationContext("classpath:testContext-pcDAO.xml"))
				.getBean("pcDAO");
		assertNotNull(pcDAO);
		assertTrue(((Model)pcDAO).getObjects().isEmpty());
		
		MergerImpl merger = new MergerImpl(pcDAO, metadataDAO, moleculesDAO, proteinsDAO, cvRepository);
		for(Model model : pathwayModels) {
			merger.merge(model);
		}

		String outFilename = getClass().getClassLoader().getResource("").getPath() 
			+ File.separator + "pcDaoMergerTest.out.owl";
		//check first whether it becomes ok after export/import as owl?
		
		pcDAO.exportModel(new FileOutputStream(outFilename));
		SimpleIOHandler reader = new SimpleIOHandler();
		reader.mergeDuplicates(true);
		Model m = reader.convertFromOWL(new FileInputStream(outFilename));
		
		assertMerge(m);
	}
}