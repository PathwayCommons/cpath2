package cpath.premerge.internal;

// imports
import cpath.cleaner.Cleaner;
import cpath.dao.PaxtoolsDAO;
import cpath.premerge.Premerge;
import cpath.premerge.PremergeDispatcher;
import cpath.normalizer.IdNormalizer;
import cpath.validator.BiopaxValidator;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;
import cpath.warehouse.metadata.MetadataDAO;
import cpath.warehouse.pathway.PathwayDataDAO;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.io.BioPAXIOHandler;
//import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.biopax.paxtools.io.simpleIO.SimpleExporter;
//import org.biopax.paxtools.proxy.level3.BioPAXFactoryForPersistence;

import org.biopax.validator.result.Validation;
import org.biopax.validator.utils.BiopaxValidatorUtils;

import org.mskcc.psibiopax.converter.PSIMIBioPAXConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Class responsible for premerging pathway data.
 */
@Repository
public final class PremergeImpl extends Thread implements Premerge {

	// log
    private static Log log = LogFactory.getLog(PremergeImpl.class);

	// ref to MetadataDAO
	private MetadataDAO metadataDAO;

	// ref to PathwayDataDAO
    private PathwayDataDAO pathwayDataDAO;

	// ref to normalizer
	private IdNormalizer idNormalizer;

	// ref to validator
	private BiopaxValidator validator;

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
	 * @param idNormalizer IdNormalizer
	 * @param validator BiopaxValidator
	 * @param simpleReader BioPAXIOHandler
	 * @param validatorUtils BiopaxValidatorUtils
	 */
	public PremergeImpl(final MetadataDAO metadataDAO,
						final PathwayDataDAO pathwayDataDAO,
						final IdNormalizer idNormalizer,
						final BiopaxValidator validator,
						final BioPAXIOHandler simpleReader,
						final BiopaxValidatorUtils validatorUtils) {

		// init members
		this.metadataDAO = metadataDAO;
		this.pathwayDataDAO = pathwayDataDAO;
		this.idNormalizer = idNormalizer;
		this.validator = validator;
		this.simpleReader = simpleReader;
		this.validatorUtils = validatorUtils;
	}

    /**
	 * (non-Javadoc)
	 * @see cpath.premerge.Premerge#setDispatcher(cpath.premerge.PremergeDispatcher)
	 */
	@Override
	public void setDispatcher(final PremergeDispatcher premergeDispatcher) {
		this.premergeDispatcher = premergeDispatcher;
	}

	/**
	 * (non-Javadoc)
	 * @see cpath.premerge.Premerge#setMetadata(cpath.warehouse.beans.Metadata)
	 */
	@Override
	public void setMetadata(final Metadata metadata) {
		this.metadata = metadata;
	}

    /**
	 * (non-Javadoc)
	 * @see cpath.premerge.Premerge#premerge
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
	}

	/**
	 * Pushes given PathwayData through pipeline.
	 *
	 * @param pathwayData PathwayData
	 */
	private void pipeline(PathwayData pathwayData) {

		String pathwayDataStr = "";
		String pathwayDataDescription = (pathwayData.getIdentifier() + ", " +
										 pathwayData.getVersion() + ", " +
										 pathwayData.getFilename() + ".");

		// clean
		log.info("pipeline(), cleaning pathway data.");
		pathwayDataStr = cleaner.clean(pathwayData.getPathwayData());

		// if psi-mi, convert to biopax
		if (metadata.isPSI()) {
			log.info("pipeline(), converting psi-mi data.");
			pathwayDataStr = convertToBioPAX(pathwayDataStr);
		}

		// error during conversion
		if (pathwayDataStr.length() == 0) {
			// TBD: report failure
			log.info("pipeline(), error converting psi-mi data to biopax: " + pathwayDataDescription);
			return;
		}

		// create paxtools model from pathway data (owl)
		log.info("run(), creating paxtools model from pathway data.");
		//SimpleReader simple = new SimpleReader(new BioPAXFactoryForPersistence(), BioPAXLevel.L3);
		Model model = simpleReader.convertFromOWL(new ByteArrayInputStream(pathwayDataStr.getBytes()));
		
		// normalize
		log.info("pipeline(), normalizing pathway data.");
		if(!normalizePathway(model)) {
			// TBD: report failure
			log.info("pipeline(), error normalizing pathway data: " + pathwayDataDescription);
			return;
		}

		// validate
		log.info("pipeline(), validating pathway data.");
		if(!validatePathway(pathwayData, model)) {
			// TBD: report failure
			log.info("pipeline(), error validating pathway data: " + pathwayDataDescription);
			return;
		}

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
	private String convertToBioPAX(String psimiData) {

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
	 * Normalizes the given pathway data.
	 *
	 * @param model Model
	 * @return boolean
	 */
	private boolean normalizePathway(Model model) {

		boolean toReturn = true;

		idNormalizer.filter(model);

		// outta here 
		return toReturn;
	}

	/**
	 * Validates the given pathway data.
	 *
	 * @param model Model
	 * @param validation Validation
	 * @return boolean
	 */
	private boolean validatePathway(PathwayData pathwayData, Model model) {

		boolean toReturn = true;

		// get result and marshall to xml string to store
		StringWriter writer = new StringWriter();
		validatorUtils.write(validator.validate(model), writer);
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
	private boolean persistPathway(PathwayData pathwayData, Model model) {

		boolean toReturn = true;

		// write out the file
		try {
			// use simple exporter to create string from paxtools model
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			SimpleExporter simpleExporter = new SimpleExporter(BioPAXLevel.L3);
			simpleExporter.convertToOWL(model, bos);
			pathwayData.setPremergedPathwayData(bos.toString());
			pathwayDataDAO.importPathwayData(pathwayData);
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}

		// outta here
		return toReturn;
	}
}
