package cpath.importer.internal;

import cpath.dao.PaxtoolsDAO;
import cpath.dao.internal.DataServicesFactoryBean;
import cpath.fetcher.internal.CPathFetcherImpl;
import cpath.warehouse.*;
import cpath.warehouse.beans.*;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Metadata.TYPE;
import cpath.warehouse.internal.OntologyManagerCvRepository;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.impl.ModelImpl;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.io.simpleIO.SimpleEditorMap;

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

	
	@Test
	public void testInMemoryModelMerge() {

		Model pcDAO = new ModelImpl(BioPAXLevel.L3.getDefaultFactory()) {
			@Override
			public void merge(Model source) {
				SimpleMerger simpleMerger = 
					new SimpleMerger(new SimpleEditorMap(BioPAXLevel.L3));
				simpleMerger.merge(this, source);
			}
		};

		MergerImpl merger = new MergerImpl(pcDAO, pathwayProviderModel, metadataDAO,
										   moleculesDAO, proteinsDAO, cvRepository);
		merger.merge();

		// TODO: add asserts
	}

	//@Test
	public void testHibernateMerge() {

		ApplicationContext context =
			new ClassPathXmlApplicationContext("classpath:testContext-cpathDAO.xml");
		PaxtoolsDAO paxtoolsDAO = (PaxtoolsDAO)context.getBean("paxtoolsDAO");

		MergerImpl merger = new MergerImpl((Model)paxtoolsDAO, pathwayProviderModel, metadataDAO,
										   moleculesDAO, proteinsDAO, cvRepository);
		merger.merge();

		// TODO: add asserts

	}
}