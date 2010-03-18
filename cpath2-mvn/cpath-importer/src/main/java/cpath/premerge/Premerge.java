package cpath.premerge;

// imports
import cpath.cleaner.Cleaner;
import cpath.dao.PaxtoolsDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;
import cpath.warehouse.metadata.MetadataDAO;
import cpath.warehouse.pathway.PathwayDataDAO;
import cpath.warehouse.pathway.internal.PathwayDataJDBCServices;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.biopax.paxtools.proxy.level3.BioPAXFactoryForPersistence;

import org.mskcc.psibiopax.converter.PSIMIBioPAXConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Implements Premerger interface.
 */
public final class Premerge extends Thread {

	// log
    private static Log log = LogFactory.getLog(Premerge.class);

	// ref to MetadataDAO
	private MetadataDAO metadataDAO;

	// ref to PathwayDataDAO
    private PathwayDataDAO pathwayDataDAO;
	
	// ref to PathwayDataJDBCServices
	private PathwayDataJDBCServices pathwayDataJDBCServices;

	// ref to paxtools dao
    private PaxtoolsDAO paxtoolsDAO;

	// ref to metadata
	private Metadata metadata;

	/**
	 *
	 * Constructor.
	 *
	 * @param metadata Metadata
	 */
	public Premerge(final MetadataDAO metadataDAO,
					final PathwayDataDAO pathwayDataDAO,
					final PaxtoolsDAO paxtoolsDAO,
					final PathwayDataJDBCServices pathwayDataJDBCServices) {

		// init members
		this.metadataDAO = metadataDAO;
		this.pathwayDataDAO = pathwayDataDAO;
		this.paxtoolsDAO = paxtoolsDAO;
		this.pathwayDataJDBCServices = pathwayDataJDBCServices;
	}

	/**
	 * Used by PremergeDispatcher to set metadata object to process.
	 *
	 * @param metadata Metadata
	 */
	public void setMetadata(final Metadata metadata) {
		this.metadata = metadata;
	}

	/**
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() {

		log.info("run(), starting...");

		// sanity check
		if (metadata == null) {
			log.info("run(), metadata object is null.");
			return;
		}

		// drop/create database for this provider
		log.info("run(), creating provider data base.");
		if (!pathwayDataJDBCServices.createProviderDatabase(metadata, true)) {
			// TBD: report failure
			log.info("run(), could not create provider database for persistence.");
			return;
		}

		// get pathway data
		log.info("run(), getting pathway data for provider.");
		Collection<PathwayData> pathwayDataCollection =
			pathwayDataDAO.getByIdentifier(metadata.getIdentifier());

		// create cleaner
		log.info("run(), getting a cleaner.");
		Cleaner cleaner = getCleaner(metadata.getCleanerClassname());
		if (cleaner == null) {
			// TDB: report failure
			log.info("run(), could not create cleaner class " + metadata.getCleanerClassname());
			return;
		}
		
		// iterate over all pathway data
		log.info("run(), interating over pathway data.");
		for (PathwayData pData : pathwayDataCollection) {

			// clean
			log.info("run(), cleaning pathway data.");
			String pathwayData = cleaner.clean(pData.getPathwayData());

			// if psi-mi, convert to biopax
			if (metadata.isPSI()) {
				log.info("run(), converting psi-mi data.");
				try {
					ByteArrayOutputStream os = new ByteArrayOutputStream();
					InputStream is = new ByteArrayInputStream(pathwayData.getBytes());
					PSIMIBioPAXConverter psimiConverter = new PSIMIBioPAXConverter(BioPAXLevel.L3);
					psimiConverter.convert(is, os);
					// wait for conversion to finish
					while(true) {
						sleep(100);
						if (psimiConverter.conversionIsComplete()) {
							break;
						}
					}
					// made it here, conversion is complete
					pathwayData = os.toString();
				}
				catch(Exception e) {
					// TBD: report failure, return
				}
			}

			// create paxtools model from pathway data
			log.info("run(), creating paxtools model from pathway data.");
			SimpleReader simple = new SimpleReader(new BioPAXFactoryForPersistence(), BioPAXLevel.L3);
			Model model = simple.convertFromOWL(new ByteArrayInputStream(pathwayData.getBytes()));

			// normalize
			log.info("run(), normalizing pathway data.");

			// validate
			log.info("run(), validating pathway data.");

			// persist paxtools model
			log.info("run(), persisting pathway data.");
			//persistModel(model);
		}
	}

	/**
	 * Persists the given paxtools model to an individual database.
	 *
	 * @param model Model
	 */
	private void persistModel(Model model) {

		java.util.Properties properties = new java.util.Properties();
		properties.setProperty("hibernate.hbm2ddl.auto", "create");
		properties.setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.jdbc2.optional.MysqlDataSource");		
		properties.setProperty("hibernate.connection.url", "jdbc:mysql://localhost/" + metadata.getIdentifier());
		properties.setProperty("hibernate.connection.username", "cbio");
		properties.setProperty("hibernate.connection.password", "cbio");
		properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL5Dialect");

		log.info("persistModel(), creating configuration...");
		org.hibernate.cfg.Configuration configuration = 
			new org.hibernate.cfg.Configuration();
		configuration = configuration.addProperties(properties);

		org.hibernate.cfg.Settings settings = configuration.buildSettings();

		log.info("persistModel(), creating schema export...");
		org.hibernate.tool.hbm2ddl.SchemaExport se =
			new org.hibernate.tool.hbm2ddl.SchemaExport(configuration, settings);

		log.info("persistModel(), creating db...");
		se.create(false, true);

		// persist the db
		paxtoolsDAO.importModel(model, false);
	}

	/**
	 * For the given cleaner class name,
	 * returns an instance of a class which
	 * implements the cleaner interface.
	 *
	 * @param cleanerClassName String
	 * @return Cleaner
	 */
	Cleaner getCleaner(final String cleanerClassName) {

		try {
			Class cleanerClass = getClass().forName(cleanerClassName);
			return (cleanerClass == null) ?
				null : (Cleaner)cleanerClass.newInstance();
		}
		catch (Exception e) {
			return null;
		}
	}
}
