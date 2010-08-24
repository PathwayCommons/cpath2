package cpath.importer.internal;

import cpath.dao.PaxtoolsDAO;
import cpath.dao.internal.DataServicesFactoryBean;
import cpath.fetcher.internal.CPathFetcherImpl;
import cpath.warehouse.*;
import cpath.warehouse.beans.*;
import cpath.warehouse.beans.Metadata.TYPE;
import cpath.warehouse.internal.OntologyManagerCvRepository;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.impl.ModelImpl;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.io.simpleIO.*;

import org.junit.*;
import static org.junit.Assert.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * @author rodche
 *
 */
public class CPathMergerTest {

	private static Log log = LogFactory.getLog(CPathMergerTest.class);
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
			new String[]{"classpath:testContext-allDAO.xml"});
		proteinsDAO = (WarehouseDAO) context.getBean("proteinsDAO");
		moleculesDAO = (WarehouseDAO) context.getBean("moleculesDAO");
		cvRepository = new OntologyManagerCvRepository(new ClassPathResource("ontologies.xml"), null);
		metadataDAO = (MetadataDAO) context.getBean("metadataDAO");

        // load the test metadata and data into the warehouse
		CPathFetcherImpl fetcher = new CPathFetcherImpl();
		try {
			Collection<Metadata> metadata = fetcher.getProviderMetadata("classpath:metadata.html");
			for (Metadata mdata : metadata) {
				// store metadata in the warehouse
				metadataDAO.importMetadata(mdata);
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
		for (Metadata mdata : metadataDAO.getAll()) {
			if (mdata.getType() == TYPE.BIOPAX) {
				//if(!mdata.getIdentifier().equals("TEST_BIOPAX"))
					//continue; // TODO remove this break to test several PWs merge (but testMergeIntoDAO fails then, although similar idea test in cpath-dao works)
				
				// do NOT create pathway data db (for the tests)! 
				// instead, build models right away (must be normalized/cleaned data!):
				String url = mdata.getURLToPathwayData();
				Model model = (new SimpleReader()).convertFromOWL(
						resourceLoader.getResource(url).getInputStream());
				if(model != null)
					pathwayModels.add(model);
				else 
					fail("Failed to import Model from:" + url);
			}
		}
	}
	
	
	@Test
	public void testInMemoryModelMerge() throws IOException 
	{
		// extend Model for the 'merge' to work
		Model pcDAO = new ModelImpl(BioPAXLevel.L3.getDefaultFactory()) {
			@Override
			public void merge(Model source) {
				SimpleMerger simpleMerger = 
					new SimpleMerger(new SimpleEditorMap(BioPAXLevel.L3));
				simpleMerger.merge(this, source);
			}
		};
		
		MergerImpl merger = new MergerImpl(pcDAO, metadataDAO,
										   moleculesDAO, proteinsDAO, cvRepository);
		
		for(Model model : pathwayModels) {
			merger.merge(model);
		}
		
		// dump owl out for review
		OutputStream out = new FileOutputStream(
			getClass().getClassLoader().getResource("").getPath() 
				+ File.separator + "InMemoryMergerTest.out.owl");
		(new SimpleExporter(BioPAXLevel.L3)).convertToOWL(pcDAO, out);
		
		assertMerge(pcDAO);
	}
	
	private void assertMerge(Model mergedModel) {
		
		// test proper merge of protein reference
		assertTrue(mergedModel.containsID("http://www.biopax.org/examples/myExample#Protein_54"));
		assertTrue(mergedModel.containsID("urn:miriam:uniprot:P27797"));
		assertTrue(mergedModel.containsID("urn:pathwaycommons:UnificationXref:uniprot_P27797"));
		assertTrue(!mergedModel.containsID("urn:pathwaycommons:UnificationXref:Uniprot_P27797"));
		assertTrue(mergedModel.containsID("urn:miriam:taxonomy:9606"));
		
		ProteinReference pr = (ProteinReference)mergedModel.getByID("urn:miriam:uniprot:P27797");
		assertEquals(8, pr.getName().size());
		assertEquals("CALR_HUMAN", pr.getDisplayName());
		assertEquals("Calreticulin", pr.getStandardName());
		assertEquals(6, pr.getXref().size());
		assertEquals("urn:miriam:taxonomy:9606", pr.getOrganism().getRDFId());
		
		// TODO: add asserts for CV
		assertTrue(mergedModel.containsID("urn:miriam:obo.go:GO%3A0005737"));
		
		// test proper merge of small molecule reference
		assertTrue(mergedModel.containsID("http://www.biopax.org/examples/myExample#beta-D-fructose_6-phosphate"));
		assertTrue(mergedModel.containsID("urn:pathwaycommons:CRPUJAZIXJMDBK-DTWKUNHWBS"));
		assertTrue(mergedModel.containsID("urn:pathwaycommons:ChemicalStructure:CRPUJAZIXJMDBK-DTWKUNHWBS"));
		assertTrue(mergedModel.containsID("urn:miriam:chebi:20"));
		assertTrue(mergedModel.containsID("urn:pathwaycommons:ChemicalStructure:chebi_20"));
		assertTrue(!mergedModel.containsID("http://www.biopax.org/examples/myExample#ChemicalStructure_8"));
		assertTrue(mergedModel.containsID("urn:miriam:pubchem.substance:14438"));
		assertTrue(mergedModel.containsID("urn:pathwaycommons:ChemicalStructure:pubchem.substance_14438"));
		assertTrue(!mergedModel.containsID("http://www.biopax.org/examples/myExample#ChemicalStructure_6"));
		
		SmallMolecule sm = (SmallMolecule)mergedModel.getByID("http://www.biopax.org/examples/myExample#beta-D-fructose_6-phosphate");
		SmallMoleculeReference smr = (SmallMoleculeReference)sm.getEntityReference();
		assertEquals("urn:pathwaycommons:CRPUJAZIXJMDBK-DTWKUNHWBS", smr.getRDFId());
		smr = (SmallMoleculeReference)mergedModel.getByID("urn:miriam:chebi:20");
		assertEquals("(+)-camphene", smr.getStandardName());
		assertEquals(3, smr.getXref().size());
		smr = (SmallMoleculeReference)mergedModel.getByID("urn:miriam:pubchem.substance:14438");
		assertEquals("Geranyl formate", smr.getDisplayName());
		assertEquals(1, smr.getXref().size());
		
		//TODO test entityReferenceOf (of PEs from different pathways), xrefOf, etc.
	}
	
	
	@Test
	public void testMergeIntoDAO() throws IOException {
		// init the target test db
		DataServicesFactoryBean.createSchema("cpath2_testpc"); // target db, for pcDAO
		final PaxtoolsDAO pcDAO = (PaxtoolsDAO)(new ClassPathXmlApplicationContext("classpath:testContext-pcDAO.xml"))
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
		Model m = (new SimpleReader()).convertFromOWL(new FileInputStream(outFilename));
		assertMerge(m);
		
		
		// now - test pcDAO model directly
		//		
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
		assertEquals("urn:pathwaycommons:CRPUJAZIXJMDBK-DTWKUNHWBS", smr.getRDFId());
		
		smr = (SmallMoleculeReference)pcDAO.getByID("urn:miriam:chebi:20");
		pcDAO.initialize(smr);
		assertEquals("(+)-camphene", smr.getStandardName());
		assertEquals(3, smr.getXref().size());
		
		smr = (SmallMoleculeReference)pcDAO.getByID("urn:miriam:pubchem.substance:14438");
		pcDAO.initialize(smr);
		assertEquals("Geranyl formate", smr.getDisplayName());
		assertEquals(1, smr.getXref().size());
		assertTrue(smr.getEntityReferenceOf().isEmpty()); // because 'inchi' one is the one that is referenced
	}
}