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
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.*;
import java.util.Collection;

public class CPathMergerTest {

	private static Log log = LogFactory.getLog(CPathMergerTest.class);

	private static MetadataDAO metadataDAO;
	private static WarehouseDAO proteinsDAO;
	private static WarehouseDAO moleculesDAO;
	private static WarehouseDAO cvRepository;
	private static Model pathwayProviderModel;
        
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

        // load test data
		CPathFetcherImpl fetcher = new CPathFetcherImpl();
		PathwayDataDAO pathwayDataDAO = (PathwayDataDAO) context.getBean("pathwayDataDAO");
		try {
			Collection<Metadata> metadata = fetcher.getProviderMetadata("classpath:metadata.html");
			for (Metadata mdata : metadata) {
				metadataDAO.importMetadata(mdata);
				if (mdata.getType() == TYPE.PROTEIN) {
					fetcher.storeWarehouseData(mdata, (Model)proteinsDAO);
				}
				else if (mdata.getType() == TYPE.SMALL_MOLECULE) {
					fetcher.storeWarehouseData(mdata, (Model)moleculesDAO);
				}
				else if ("TEST_BIOPAX".equals(mdata.getIdentifier())) {
					Collection<PathwayData> pathwayData = fetcher.getProviderPathwayData(mdata);
					assertEquals(1, pathwayData.size());
					for (PathwayData pwData : pathwayData) {
						pathwayDataDAO.importPathwayData(pwData);
						pathwayProviderModel =
							(new SimpleReader()).convertFromOWL(new ByteArrayInputStream(pwData.getPathwayData().getBytes()));
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	//@Ignore
	@Test
	public void testInMemoryModelMerge() throws IOException {

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
		merger.merge(pcDAO, pathwayProviderModel);

		assertMerge(pcDAO, true);
		
		// dump owl out to stdout for review
		System.out.println("Merged BioPAX (memory model): ");
		(new SimpleExporter(BioPAXLevel.L3)).convertToOWL(pcDAO, System.out);
	}

	//@Ignore
	@Test
	public void testHibernateMerge() throws IOException {

		ApplicationContext context =
			new ClassPathXmlApplicationContext("classpath:testContext-cpathDAO.xml");
		PaxtoolsDAO paxtoolsDAO = (PaxtoolsDAO)context.getBean("paxtoolsDAO");

		MergerImpl merger = new MergerImpl((Model)paxtoolsDAO, metadataDAO,
										   moleculesDAO, proteinsDAO, cvRepository);
		merger.merge((Model)paxtoolsDAO, pathwayProviderModel);

		assertMerge(paxtoolsDAO, false);
		
		// dump owl out to stdout for review
		//System.out.println("Merged BioPAX (memory model): ");
		//(new SimpleExporter(BioPAXLevel.L3)).convertToOWL(paxtoolsDAO, System.out);
	}
	
	/*
	 * Tests the merging routine.  We have to differenciate between
	 * testing with an in-memory paxtools model and using a paxtoolsDAO (hibernate)
	 * model.  This is true because during testing, warehouse data and in coming
	 * pathway data resides in the same database.  Thus, checks (and subsequent) merging
	 * of protein, CV, and small molecule data does not work as designed.
	 */
	private void assertMerge(Model mergedModel, boolean memoryModel) {
		
		// test proper merge of protein reference
		assertTrue(mergedModel.containsID("http://www.biopax.org/examples/myExample#Protein_54"));
		assertTrue(mergedModel.containsID("urn:miriam:uniprot:P27797"));
		assertTrue(mergedModel.containsID("urn:pathwaycommons:UnificationXref:uniprot_P27797"));
		// hibernate/mysql search is case insensitive by default, only check with memory model
		if (memoryModel) {
			assertTrue(!mergedModel.containsID("urn:pathwaycommons:UnificationXref:Uniprot_P27797"));
		}
		assertTrue(mergedModel.containsID("urn:miriam:taxonomy:9606"));
		
		ProteinReference pr = (ProteinReference)mergedModel.getByID("urn:miriam:uniprot:P27797");
		if (memoryModel) {
			assertEquals(8, pr.getName().size());
			assertEquals("CALR_HUMAN", pr.getDisplayName());
			assertEquals("Calreticulin", pr.getStandardName());
			assertEquals(6, pr.getXref().size());
			assertEquals("urn:miriam:taxonomy:9606", pr.getOrganism().getRDFId());
		}
		else {
			assertEquals(3, pr.getName().size());
			assertEquals("glucokinase", pr.getDisplayName());
			assertEquals("GLK", pr.getStandardName());
			assertEquals(1, pr.getXref().size());
			assertEquals("urn:miriam:taxonomy:562", pr.getOrganism().getRDFId());
		}
		
		// TODO: add asserts for CV
		
		// test proper merge of small molecule reference
		assertTrue(mergedModel.containsID("http://www.biopax.org/examples/myExample#beta-D-fructose_6-phosphate"));
		assertTrue(mergedModel.containsID("urn:pathwaycommons:CRPUJAZIXJMDBK-DTWKUNHWBS"));
		assertTrue(mergedModel.containsID("urn:pathwaycommons:ChemicalStructure:CRPUJAZIXJMDBK-DTWKUNHWBS"));
		assertTrue(mergedModel.containsID("urn:miriam:chebi:20"));
		assertTrue(mergedModel.containsID("urn:pathwaycommons:ChemicalStructure:chebi_20"));
		if (memoryModel) {
			assertTrue(!mergedModel.containsID("http://www.biopax.org/examples/myExample#ChemicalStructure_8"));
		}
		assertTrue(mergedModel.containsID("urn:miriam:pubchem.substance:14438"));
		assertTrue(mergedModel.containsID("urn:pathwaycommons:ChemicalStructure:pubchem.substance_14438"));
		if (memoryModel) {
			assertTrue(!mergedModel.containsID("http://www.biopax.org/examples/myExample#ChemicalStructure_6"));
		}
		
		SmallMolecule sm = (SmallMolecule)mergedModel.getByID("http://www.biopax.org/examples/myExample#beta-D-fructose_6-phosphate");
		SmallMoleculeReference smr = (SmallMoleculeReference)sm.getEntityReference();
		if (memoryModel) {
			assertEquals("urn:pathwaycommons:CRPUJAZIXJMDBK-DTWKUNHWBS", smr.getRDFId());

			smr = (SmallMoleculeReference)mergedModel.getByID("urn:miriam:chebi:20");
			assertEquals("(+)-camphene", smr.getStandardName());
			assertEquals(3, smr.getXref().size());

			smr = (SmallMoleculeReference)mergedModel.getByID("urn:miriam:pubchem.substance:14438");
			assertEquals("Geranyl formate", smr.getDisplayName());
			assertEquals(1, smr.getXref().size());
		}
		else {
			assertEquals("urn:miriam:chebi:20", smr.getRDFId());
			assertEquals("b-D-fru-6-p", smr.getStandardName());
			assertEquals(1, smr.getXref().size());

			smr = (SmallMoleculeReference)mergedModel.getByID("urn:miriam:pubchem.substance:14438");
			assertEquals("Adenosine 5'-diphosphate", smr.getDisplayName());
			assertEquals(1, smr.getXref().size());
		}
	}
}