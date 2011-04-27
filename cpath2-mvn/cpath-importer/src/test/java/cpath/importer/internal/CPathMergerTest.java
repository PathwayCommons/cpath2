package cpath.importer.internal;

import cpath.config.CPathSettings;
import cpath.dao.PaxtoolsDAO;
import cpath.dao.internal.DataServicesFactoryBean;
import cpath.fetcher.internal.CPathFetcherImpl;
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
        
	static {
		// init the test database
		DataServicesFactoryBean.createSchema("cpath2_test");

		// load beans
		ApplicationContext context = new ClassPathXmlApplicationContext(
			new String[] {
				"classpath:testContext-whDAO.xml", 
				"classpath:applicationContext-cvRepository.xml"
				});
		proteinsDAO = (WarehouseDAO) context.getBean("proteinsDAO");
		moleculesDAO = (WarehouseDAO) context.getBean("moleculesDAO");
		cvRepository = (WarehouseDAO) context.getBean("cvFetcher");
		metadataDAO = (MetadataDAO) context.getBean("metadataDAO");

        /* load the test metadata and ONLY (!) 
		 * test proteins and molecules data into the warehouse
		 */
		CPathFetcherImpl fetcher = new CPathFetcherImpl();
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
		Model model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:test-normalized.owl").getInputStream());
		pathwayModels.add(model);
		
		model = null;
		model = reader.convertFromOWL(resourceLoader
			.getResource("classpath:test-normalized-2.owl").getInputStream());
		pathwayModels.add(model);
	}
	
	
	@Test
	public void testInMemoryModelMerge() throws IOException 
	{
		Model memoPcModel = BioPAXLevel.L3.getDefaultFactory().createModel();
		MergerImpl merger = new MergerImpl(memoPcModel, metadataDAO,
				moleculesDAO, proteinsDAO, cvRepository);
		
		for(Model model : pathwayModels) {
			merger.merge(model);
		}
		
		// dump owl out for review
		OutputStream out = new FileOutputStream(
			getClass().getClassLoader().getResource("").getPath() 
				+ File.separator + "InMemoryMergerTest.out.owl");
		(new SimpleIOHandler(BioPAXLevel.L3)).convertToOWL(memoPcModel, out);
		
		assertMerge(memoPcModel);
	}
	
	private void assertMerge(Model mergedModel) {
		// test proper merge of protein reference
		assertTrue(mergedModel.containsID("http://www.biopax.org/examples/myExample#Protein_54"));
		assertTrue(mergedModel.containsID("urn:miriam:uniprot:P27797"));
		assertTrue(mergedModel.containsID(CPathSettings.CPATH_URI_PREFIX+"UnificationXref:UNIPROT_P27797"));
		assertTrue(!mergedModel.containsID(CPathSettings.CPATH_URI_PREFIX+"UnificationXref:Uniprot_P27797"));
		assertTrue(mergedModel.containsID("urn:miriam:taxonomy:9606"));
		
		ProteinReference pr = (ProteinReference)mergedModel.getByID("urn:miriam:uniprot:P27797");
		assertEquals(8, pr.getName().size());
		assertEquals("CALR_HUMAN", pr.getDisplayName());
		assertEquals("Calreticulin", pr.getStandardName());
		assertEquals(6, pr.getXref().size());
		assertEquals("urn:miriam:taxonomy:9606", pr.getOrganism().getRDFId());
		
		assertTrue(mergedModel.containsID("urn:miriam:obo.go:GO%3A0005737"));
		
		// test proper merge of small molecule reference
		assertTrue(mergedModel.containsID("http://www.biopax.org/examples/myExample#beta-D-fructose_6-phosphate"));
		assertTrue(mergedModel.containsID("urn:miriam:chebi:20"));
		assertTrue(mergedModel.containsID(CPathSettings.CPATH_URI_PREFIX+"ChemicalStructure:chebi_20"));
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
		
		SmallMoleculeReference msmr = (SmallMoleculeReference)mergedModel.getByID("urn:miriam:chebi:20");
		assertEquals("(+)-camphene", msmr.getStandardName());
		assertEquals(5, msmr.getXref().size());
		if(mergedModel instanceof PaxtoolsDAO) {
			((PaxtoolsDAO) mergedModel).initialize(msmr);
		}
		assertTrue(msmr.getMemberEntityReferenceOf().contains(smr));
	}
	
	
	@Test
	public void testMergeIntoDAO() throws IOException {
		// init the target test db
		DataServicesFactoryBean.createSchema("cpath2_testpc"); // target db, for pcDAO
		final PaxtoolsDAO pcDAO = (PaxtoolsDAO) (
				new ClassPathXmlApplicationContext("classpath:testContext-pcDAO.xml"))
				.getBean("pcDAO");
		assertNotNull(pcDAO);
		assertTrue(pcDAO.getObjects().isEmpty());
		
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
		
		
		// now - test pcDAO model directly	
		ProteinReference pr = (ProteinReference)pcDAO.getByIdInitialized("urn:miriam:uniprot:P27797");
		assertEquals(8, pr.getName().size());
		assertEquals("CALR_HUMAN", pr.getDisplayName());
		assertEquals("Calreticulin", pr.getStandardName());
		assertEquals(6, pr.getXref().size());
		assertEquals("urn:miriam:taxonomy:9606", pr.getOrganism().getRDFId());
		
		// TODO: add asserts for CV
		assertTrue(pcDAO.containsID("urn:miriam:obo.go:GO%3A0005737"));
		
		SmallMolecule sm = (SmallMolecule)pcDAO.getByIdInitialized("http://www.biopax.org/examples/myExample#beta-D-fructose_6-phosphate");
		SmallMoleculeReference smr = (SmallMoleculeReference)sm.getEntityReference();
		assertEquals("urn:miriam:chebi:20", smr.getRDFId());
		
		smr = (SmallMoleculeReference)pcDAO.getByID("urn:miriam:chebi:20");
		pcDAO.initialize(smr);
		assertEquals("(+)-camphene", smr.getStandardName());
		assertEquals(5, smr.getXref().size());
		
		smr = (SmallMoleculeReference)pcDAO.getByID("urn:miriam:chebi:28");
		pcDAO.initialize(smr);
		assertEquals("(R)-linalool", smr.getDisplayName());
		assertEquals(14, smr.getXref().size());
		assertEquals(4, smr.getEntityReferenceOf().size());
	}
}