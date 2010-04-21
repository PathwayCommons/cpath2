package cpath.importer.internal;

// imports
import cpath.importer.Merger;
import cpath.dao.PaxtoolsDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.CPathWarehouse;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.PathwayDataJDBCServices;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.ControlledVocabulary;
import org.biopax.paxtools.model.level3.UtilityClass;
import org.biopax.paxtools.proxy.level3.BioPAXFactoryForPersistence;
import org.biopax.paxtools.controller.SimpleMerger;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Set;


/**
 * Class responsible for Merging pathway data.
 */
public final class MergerImpl implements Merger {

	// log
    private static Log log = LogFactory.getLog(MergerImpl.class);

	// ref to MetadataDAO
	private MetadataDAO metadataDAO;

	// ref to pc repository
	private PaxtoolsDAO pcDAO;

	// ref to the warehouse
	private CPathWarehouse cpathWarehouse;
	
	// ref to jdbc services
	private PathwayDataJDBCServices pathwayDataJDBCServices;

	// ref to simple merger
	private SimpleMerger simpleMerger;

	/**
	 *
	 * Constructor.
	 *
	 * @param pcDAO PaxtoolsDAO;
     * @param metadataDAO MetadataDAO
	 * @param cPathWarehouse CPathWarehouse
	 * @param pathwaydDataJDBCServices PathwayDataJDBCServices
	 * @param simpleMerger SimpleMerger
	 */
	public MergerImpl(final PaxtoolsDAO pcDAO,
					  final MetadataDAO metadataDAO,
					  final CPathWarehouse cpathWarehouse,
					  final PathwayDataJDBCServices pathwayDataJDBCServices,
					  final SimpleMerger simpleMerger) {

		// init members
		this.pcDAO = pcDAO;
		this.metadataDAO = metadataDAO;
		this.cpathWarehouse = cpathWarehouse;
		this.pathwayDataJDBCServices = pathwayDataJDBCServices;
		this.simpleMerger = simpleMerger;
	}

    /**
	 * (non-Javadoc)
	 * @see cpath.importer.Merger#merge
	 */
	@Override
	public void merge() {

		// create pc model
		Model pcModel= (new BioPAXFactoryForPersistence()).createModel();

		// iterate over all providers
		for (Metadata metadata : metadataDAO.getAll()) {

			// get the persisted model for this provider
			Model pathwayModel = getModel(metadata);

			// iterate over all elements in pathway model
			for (BioPAXElement bpe : pathwayModel.getObjects()) {

				// check if element already exists in pc
				boolean elementExistsInPC = (pcModel.getByID(bpe.getRDFId()) != null);

				// merge all protein/SM references & controlled vocabularies
				if ((bpe instanceof ProteinReference ||
					 bpe instanceof SmallMoleculeReference ||
					 bpe instanceof ControlledVocabulary)
					&& !elementExistsInPC) {
					pcModel.add(getUtilityClass(bpe));
				}
			}

			// merge
			simpleMerger.merge(pcModel, pathwayModel);
		}

		pcDAO.importModel(pcModel, true);
	}

	/**
	 * For the given provider, gets the persisted model.
	 *
	 * @param metadata Metadata
	 * @return Model
	 */
	private Model getModel(final Metadata metadata) {

		// create data source
		MysqlDataSource mysqlDataSource = new MysqlDataSource();
		mysqlDataSource.setURL(pathwayDataJDBCServices.getDbConnection() + metadata.getIdentifier());
		mysqlDataSource.setUser(pathwayDataJDBCServices.getDbUser());
		mysqlDataSource.setPassword(pathwayDataJDBCServices.getDbPassword());

		// get application context after setting custom datasource
		DataSource.beansByName.put("mergeDataSource", mysqlDataSource);
		ApplicationContext context = 
			new ClassPathXmlApplicationContext("classpath:applicationContext-cpathMerge.xml");

		// get a ref to PaxtoolsDAO
		PaxtoolsDAO mergePaxtoolsDAO = (PaxtoolsDAO)context.getBean("mergePaxtoolsDAO");

		// create a model, grab all elements from db and add to model
		Model toReturn = BioPAXLevel.L3.getDefaultFactory().createModel();
		Set<BioPAXElement> allElements = mergePaxtoolsDAO.getObjects(BioPAXElement.class, true);
		for (BioPAXElement bpe : allElements) {
			toReturn.add(bpe);
		}

		// outta here
		return toReturn;
	}


	/**
	 * Given a BioPAXElement, returns the proper utility class
	 * from the warehouse.
	 *
	 * @param bpe BioPAXElement
	 * @return <T extends UtilityClass>
	 */
	private <T extends UtilityClass> T getUtilityClass(BioPAXElement bpe) {

		if (bpe instanceof ProteinReference) {
			return (T)cpathWarehouse.createUtilityClass(bpe.getRDFId(), ProteinReference.class);
		}
		else if (bpe instanceof SmallMoleculeReference) {
			return (T)cpathWarehouse.createUtilityClass(bpe.getRDFId(), SmallMoleculeReference.class);
		}
		else if (bpe instanceof ControlledVocabulary) {
			return (T)cpathWarehouse.createUtilityClass(bpe.getRDFId(), ControlledVocabulary.class);
		}

		// should not get here
		return null;
	}
}