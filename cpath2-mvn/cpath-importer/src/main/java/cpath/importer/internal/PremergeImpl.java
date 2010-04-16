package cpath.importer.internal;

// imports
import cpath.cleaner.Cleaner;
import cpath.dao.PaxtoolsDAO;
import cpath.importer.Normalizer;
import cpath.importer.Premerge;
import cpath.warehouse.PathwayDataDAO;
import cpath.warehouse.PathwayDataJDBCServices;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.io.BioPAXIOHandler;

import org.biopax.validator.Validator;
import org.biopax.validator.result.Validation;
import org.biopax.validator.utils.BiopaxValidatorUtils;

import org.mskcc.psibiopax.converter.PSIMIBioPAXConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import java.util.Collection;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Class responsible for premerging pathway data.
 */
public final class PremergeImpl extends Thread implements Premerge {

	// log
    private static Log log = LogFactory.getLog(PremergeImpl.class);

	// ref to PathwayDataDAO
    private PathwayDataDAO pathwayDataDAO;

	// ref to jdbc services
	private PathwayDataJDBCServices pathwayDataJDBCServices;

	// ref to normalizer
	private Normalizer idNormalizer;

	// ref to validator
	private Validator validator;

	// ref to validator utils
	private BiopaxValidatorUtils validatorUtils;

	// ref to paxtools reader
	private BioPAXIOHandler simpleReader;
	
	// ref to dispatcher
	private PremergeDispatcher premergeDispatcher;

	// ref to metadata
	private Metadata metadata;
	
	// ref to cleaner
	private Cleaner cleaner;

	/**
	 *
	 * Constructor.
	 *
	 * @param metadata Metadata
	 * @param pathwayDataDAO PathwayDataDAO
	 * @param pathwaydDataJDBCServices
	 * @param idNormalizer Normalizer
	 * @param validator Biopax Validator
	 * @param simpleReader BioPAXIOHandler
	 * @param validatorUtils BiopaxValidatorUtils
	 */
	public PremergeImpl(final PathwayDataDAO pathwayDataDAO,
						final PathwayDataJDBCServices pathwayDataJDBCServices,
						final Normalizer idNormalizer,
						final Validator validator,
						final BioPAXIOHandler simpleReader,
						final BiopaxValidatorUtils validatorUtils) {
		// init members
		this.pathwayDataDAO = pathwayDataDAO;
		this.pathwayDataJDBCServices = pathwayDataJDBCServices;
		this.idNormalizer = idNormalizer;
		this.validator = validator;
		this.simpleReader = simpleReader;
		this.validatorUtils = validatorUtils;
	}

    /**
	 * (non-Javadoc)
	 * @see cpath.importer.Premerge#setDispatcher(cpath.importer.internal.PremergeDispatcher)
	 */
	@Override
	public void setDispatcher(final PremergeDispatcher premergeDispatcher) {
		this.premergeDispatcher = premergeDispatcher;
	}

	/**
	 * (non-Javadoc)
	 * @see cpath.importer.Premerge#setMetadata(cpath.warehouse.beans.Metadata)
	 */
	@Override
	public void setMetadata(final Metadata metadata) {
		this.metadata = metadata;
	}

    /**
	 * (non-Javadoc)
	 * @see cpath.importer.Premerge#premerge
	 */
	@Override
	public void premerge() {
		this.start();
	}

	/**
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {

		log.info("run(), starting...");

		// sanity check
		if (metadata == null) {
			log.info("run(), metadata object is null.");
			return;
		}

		// create db
		//pathwayDataJDBCServices.createProviderDatabase(metadata, true);

		// get pathway data
		log.info("run(), getting pathway data for provider.");
		Collection<PathwayData> pathwayDataCollection =
			pathwayDataDAO.getByIdentifier(metadata.getIdentifier());

		// create cleaner
		log.info("run(), getting a cleaner with name: " + metadata.getCleanerClassname());
		cleaner = getCleaner(metadata.getCleanerClassname());
		if (cleaner == null) {
			// TDB: report failure
			log.info("run(), could not create cleaner class " + metadata.getCleanerClassname());
			return;
		}
		
		// iterate over all pathway data
		log.info("run(), interating over pathway data.");
		for (PathwayData pathwayData : pathwayDataCollection) {
			pipeline(pathwayData);
		}

		// outta here
		premergeDispatcher.premergeComplete(metadata);
		log.info("run(), exiting...");
	}

	/**
	 * Pushes given PathwayData through pipeline.
	 *
	 * @param pathwayData PathwayData
	 */
	private void pipeline(final PathwayData pathwayData) {

		String pathwayDataStr = "";
		String pathwayDataDescription = (pathwayData.getIdentifier() + ", " +
										 pathwayData.getVersion() + ", " +
										 pathwayData.getFilename() + ".");

		// clean
		log.info("pipeline(), cleaning pathway data.");
		pathwayDataStr = cleaner.clean(pathwayData.getPathwayData());

		// if psi-mi, convert to biopax
		if (metadata.getType() == Metadata.TYPE.PSI_MI) {
			log.info("pipeline(), converting psi-mi data.");
			pathwayDataStr = convertToBioPAX(pathwayDataStr);
		}

		// error during conversion
		if (pathwayDataStr.length() == 0) {
			// TBD: report failure
			log.info("pipeline(), error converting psi-mi data to biopax: " + pathwayDataDescription);
			return;
		}

		// normalize
		log.info("pipeline(), normalizing pathway data.");
		try {
			pathwayDataStr = idNormalizer.normalize(pathwayDataStr);
		} catch (RuntimeException e) {
			// TBD: report failure
			log.info("pipeline(), error normalizing pathway data: " + pathwayDataDescription, e);
			return;
		}

		// validate
		// TODO due to possible syntax errors, it may worth validating both before and after the normalization...
		log.info("pipeline(), validating pathway data.");
		if(!validatePathway(pathwayData, pathwayDataStr)) {
			// TBD: report failure
			log.info("pipeline(), error validating pathway data: " + pathwayDataDescription);
			return;
		}

		// create paxtools model from pathway data (owl)
		log.info("run(), creating paxtools model from pathway data.");
		Model model = simpleReader.convertFromOWL(new ByteArrayInputStream(pathwayDataStr.getBytes()));
		// persist paxtools model
		log.info("pipeline(), persisting pathway data.");
		if (!persistPathway(pathwayData, model)) {
			// TBD: report failure
			log.info("pipeline(), error persisting pathway data: " + pathwayDataDescription);
			return;
		}
	}

	/**
	 * For the given cleaner class name,
	 * returns an instance of a class which
	 * implements the cleaner interface.
	 *
	 * @param cleanerClassName String
	 * @return Cleaner
	 */
	private Cleaner getCleaner(final String cleanerClassName) {

		try {
			Class cleanerClass = getClass().forName(cleanerClassName);
			return (cleanerClass == null) ?
				null : (Cleaner)cleanerClass.newInstance();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Converts psi-mi string to biopax
	 *
	 * @param psimiData String
	 */
	private String convertToBioPAX(final String psimiData) {

		String toReturn = "";
				
		try {

			ByteArrayOutputStream os = new ByteArrayOutputStream();
			InputStream is = new ByteArrayInputStream(psimiData.getBytes());
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
			toReturn = os.toString();
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		// outta here
		return toReturn;
	}

	/**
	 * Validates the given pathway data.
	 *
	 * @param pathwayData
	 * @param pathwayDataStr OWL content; may be different from original one (e.g., normalized)
	 * @return boolean
	 */
	private boolean validatePathway(final PathwayData pathwayData, final String pathwayDataStr) {

		boolean toReturn = true;
		
		// get result and marshall to xml string to store
		StringWriter writer = new StringWriter();
		// the following is 
		// create a new empty validation and associate with the model data
		Validation validation = new Validation(pathwayData.getIdentifier());
		// because errors are reported during the import (e.g., syntax)
		validator.importModel(validation, new ByteArrayInputStream(pathwayDataStr.getBytes()));
		// now post-validate
		validator.validate(validation);
		// serialize
		validatorUtils.write(validation, writer);
		pathwayData.setValidationResults(writer.toString());

		// outta here 
		return toReturn;
	}

	/**
	 * Persists the given PathwayData object with clean data
	 *
	 * @param pathwayData PathwayData
	 * @param model Model
	 * @return boolean
	 */
	@Transactional
	private boolean persistPathway(final PathwayData pathwayData, final Model model) {

		// 8create db
		if (!pathwayDataJDBCServices.createProviderDatabase(metadata, true)) {
			return false;
		}

		// create data source
		MysqlDataSource mysqlDataSource = new MysqlDataSource();
		mysqlDataSource.setURL(pathwayDataJDBCServices.getDbConnection() + metadata.getIdentifier());
		mysqlDataSource.setUser(pathwayDataJDBCServices.getDbUser());
		mysqlDataSource.setPassword(pathwayDataJDBCServices.getDbPassword());

		// get application context after setting custom datasource
		DataSource.beansByName.put("premergeDataSource", mysqlDataSource);
		ApplicationContext context = 
			new ClassPathXmlApplicationContext("classpath:applicationContext-cpathPremerge.xml");

		// get a ref to PaxtoolsDAO
		PaxtoolsDAO paxtoolsDAO = (PaxtoolsDAO)context.getBean("premergePaxtoolsDAO");
		paxtoolsDAO.importModel(model, false);

		// outta here
		return true;
	}
}
