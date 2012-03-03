/**
 ** Copyright (c) 2010 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** both UofT and MSKCC have no obligations to provide maintenance, 
 ** support, updates, enhancements or modifications.  In no event shall
 ** UofT or MSKCC be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** UofT or MSKCC have been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this software; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA;
 ** or find it at http://www.fsf.org/ or http://www.gnu.org.
 **/
package cpath.importer.internal;

// imports
import cpath.cleaner.Cleaner;
import cpath.cleaner.internal.BaseCleanerImpl;
import cpath.config.CPathSettings;
import cpath.dao.DataServices;
import cpath.dao.PaxtoolsDAO;
import cpath.dao.internal.DataServicesFactoryBean;
import cpath.importer.Premerge;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;

import org.biopax.miriam.MiriamLink;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.Provenance;
import org.biopax.validator.result.*;
import org.biopax.validator.Validator;
import org.biopax.validator.utils.BiopaxValidatorUtils;
import org.biopax.validator.utils.Normalizer;
import org.biopax.validator.utils.Normalizer.NormalizerOptions;

import org.mskcc.psibiopax.converter.PSIMIBioPAXConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.*;

import java.io.*;

import javax.sql.DataSource;

/**
 * Class responsible for premerging pathway data.
 */
public class PremergeImpl implements Premerge {

    private static Log log = LogFactory.getLog(PremergeImpl.class);

    private MetadataDAO metaDataDAO;
	private Validator validator;
	private Cleaner cleaner;
	private boolean createDb;
	private String identifier;
	private String version;

	/**
	 *
	 * Constructor.
	 *
	 * @param metadata
	 * @param metaDataDAO
	 * @param validator Biopax Validator
	 */
	public PremergeImpl(final MetadataDAO metaDataDAO,
						final Validator validator) 
	{
		this.metaDataDAO = metaDataDAO;
		this.validator = validator;
		this.createDb = true;
	}

    /**
	 * (non-Javadoc)
	 * @see cpath.importer.Premerge#premerge
	 */
	@Override
    public void premerge() {

		// grab all metadata
		Collection<Metadata> metadataCollection = metaDataDAO.getAll();

		// iterate over all metadata
		for (Metadata metadata : metadataCollection) {
			// use filter if set (identifier and version)
			if(identifier != null) {
				if(!metadata.getIdentifier().equals(identifier))
					continue;
				if(version != null)
					if(!metadata.getVersion().equals(version))
						continue;
			}
			
			// only process interaction or pathway data
			if (metadata.getType() == Metadata.TYPE.PSI_MI ||
				metadata.getType() == Metadata.TYPE.BIOPAX) {
				process(metadata);
			}
		}
	}

	/**
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	private void process(Metadata metadata) {
	  try {
		// get pathway data
		if(log.isInfoEnabled())
			log.info("run(), getting pathway data for provider.");
		Collection<PathwayData> pathwayDataCollection =
			metaDataDAO.getPathwayDataByIdentifierAndVersion(metadata.getIdentifier(), metadata.getVersion());

		// create cleaner
		if(log.isInfoEnabled())
			log.info("run(), getting a cleaner with name: " + metadata.getCleanerClassname());
		cleaner = BaseCleanerImpl.getCleaner(metadata.getCleanerClassname());
		if (cleaner == null) {
			// TDB: report failure
			if(log.isInfoEnabled())
				log.info("run(), could not create cleaner class " 
					+ metadata.getCleanerClassname());
			return;
		}
		
		PaxtoolsDAO pemergeDAO = null;

		if (isCreateDb()) {
			// build the premerge DAO -
			// first, create a new database schema
			String premergeDbName = CPathSettings.CPATH_DB_PREFIX
				+ metadata.getIdentifier() + "_" + metadata.getVersion();
			if(log.isInfoEnabled())
				log.info("Creating a new 'pre-merge' db and schema: " +
						premergeDbName);
			DataServicesFactoryBean.createSchema(premergeDbName);
			// next, get the PaxtoolsDAO instance
			pemergeDAO = buildPremergeDAO(premergeDbName);
		}
	
		// iterate over and process all pathway data
		if(log.isInfoEnabled())
			log.info("run(), interating over pathway data " 
				+ metadata.getIdentifier());
		for (PathwayData pathwayData : pathwayDataCollection) {
			try {
				pipeline(metadata, pathwayData, pemergeDAO); //pemergeDAO);
			}
			catch(Exception e) {
				log.error("pipeline(), failed for " + pathwayData, e);
				e.printStackTrace();
			}
		}

	  }
	  finally {
		if(log.isInfoEnabled()) {
			log.info("run(), exitting (" + metadata.getIdentifier() + ") ...");
		}
	  }
	}

	/**
	 * Pushes given PathwayData through pipeline.
	 *
	 * @param pathwayData provider's pathway data (usually from a single data file)
	 * @param premergeDAO persistent BioPAX model for the data provider
	 */
	private void pipeline(Metadata metadata, PathwayData pathwayData, PaxtoolsDAO premergeDAO) {
		String data = null;
		String info = pathwayData.toString();

		// get the BioPAX OWL from the pathwayData bean
		data = pathwayData.getPathwayData();
		
		/*
		 * First, clean - 
		 * in other words - apply data provider-specific "quick fixes"
		 * (the Cleaner class is specified at the Metadata conf. level)
		 * to the original data (in PSI_MI, BioPAX L2, or L3 formats)
		 */
		if(log.isInfoEnabled())
			log.info("pipeline(), cleaning data " + info);
		
		data = cleaner.clean(data);
		
		// Second, if psi-mi, convert to biopax L3
		if (metadata.getType() == Metadata.TYPE.PSI_MI) {
			if (log.isInfoEnabled())
				log.info("pipeline(), converting psi-mi data "
						+ info);
			try {
				data = convertPSIToBioPAX(data);
			} catch (RuntimeException e) {
				log.error("pipeline(), cannot convert PSI-MI data: "
						+ info + " to L3. - " + e);
				return;
			}
		} 
		
		if(log.isInfoEnabled())
			log.info("pipeline(), validating pathway data "
				+ info);
		
		
		/* Validate, auto-fix, and normalize (incl. convesion to L3): 
		 * e.g., synonyms in xref.db may be replaced 
		 * with the primary db name, as in Miriam, etc.
		 */
		Validation v = checkAndNormalize(pathwayData.toString(), data);
		if(v == null) {
			if(log.isInfoEnabled())
				log.info("pipeline(), skipping: " + info);
			return;
		}
		
		// (in addition to normalizer's job) find existing or create new Provenance 
		// from the metadata.name to add it explicitly to all entities now!
		fixDataSource(v.getModel(), metadata);	
		
		// TODO calculate pathway membership (stored in the bpe.annotation)
//		(new ModelUtils(v.getModel())).calculatePathwayMembership(Entity.class, false, true);
		
		// get the updated BioPAX OWL and save it in the pathwayData bean
		v.updateModelSerialized();
		pathwayData.setPremergeData(v.getModelSerialized());
		
		/* Now - add the serialized validation results 
		 * to the pathwayData entity bean, validationResults column.
		 * (using the last parameter, javax.xml.transform.Source
		 * of a XSLT stylesheet, the validation object can be 
		 * further transformed to a human-readable report.)
		 */
		
		/* First, let's clear the (huge) 'serializedModel' field,
		 * because it's already saved in the 'premergeData' column 
		 */
		v.setModelSerialized(null);
		
		StringWriter writer = new StringWriter();
		BiopaxValidatorUtils.write(v, writer, null);
		writer.flush();		
		pathwayData.setValidationResults(writer.toString());
		
		// count critical not fixed error cases (ignore warnings and fixed ones)
		int noErrors = v.countErrors(null, null, null, null, true, true);
		if(log.isInfoEnabled()) {
			log.info("pipeline(), summary for " + info
				+ ". Critical errors found:" + noErrors + ". " 
				+ v.getComment().toString() + "; " 
				+ v.toString());
		}
		
		if(noErrors > 0) 
			pathwayData.setValid(false); 
		else 
			pathwayData.setValid(true);
		
		// update pathwayData (with premergeData and validationResults)
		metaDataDAO.importPathwayData(pathwayData);
		
		// persist
		if(noErrors <= 0 && isCreateDb()) {			
			// Get the normalized and validated model and persist it
			if (log.isInfoEnabled())
				log.info("pipeline(), persisting pathway data "
					+ info);
			
			Model pathwayDataModel = v.getModel();
			
			// persist all at once -
			premergeDAO.merge(pathwayDataModel);
		}
	}

	
	/**
	 * Converts psi-mi string to biopax
	 *
	 * @param psimiData String
	 */
	private String convertPSIToBioPAX(final String psimiData) {

		String toReturn = "";
				
		try {

			ByteArrayOutputStream os = new ByteArrayOutputStream();
			InputStream is = new ByteArrayInputStream(psimiData.getBytes("UTF-8"));
			PSIMIBioPAXConverter psimiConverter = new PSIMIBioPAXConverter(BioPAXLevel.L3);
			psimiConverter.convert(is, os);
			toReturn = os.toString();
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}

		// outta here
		return toReturn;
	}

	
	/**
	 * Validates the given pathway data.
	 *
	 * @param title short description
	 * @param data BioPAX OWL data
	 * @return
	 */
	private Validation checkAndNormalize(final String title, String data) 
	{	
		// create a new empty validation and associate with the model data
		Validation validation = new Validation(title); // sets the title
		// set auto-fix and normalize modes
		validation.setFix(true);
		validation.setNormalize(true);
		// use special normalizer options
		NormalizerOptions normalizerOptions = new NormalizerOptions();
		// to infer/autofix biopax properties
		normalizerOptions.setFixDisplayName(true); // important
		normalizerOptions.setInferPropertyDataSource(true); // important
		normalizerOptions.setInferPropertyOrganism(true); // important
		// disable auto-generated xrefs (not required by cpath2, since 2012/01)
		normalizerOptions.setGenerateRelatioshipToOrganismXrefs(false); //was to filter search results...
		normalizerOptions.setGenerateRelatioshipToPathwayXrefs(false);
		normalizerOptions.setGenerateRelatioshipToInteractionXrefs(false);
		
		validation.setNormalizerOptions(normalizerOptions);
		// collect both errors and warnings
		validation.setThreshold(Behavior.WARNING); // means - all err./warn.
		
		// because errors are also reported during the import (e.g., syntax)
		try {
			validator.importModel(validation, new ByteArrayInputStream(data.getBytes("UTF-8")));
			// now post-validate
			if(validation.getModel() != null) {
				validator.validate(validation);
			}
			/* unregister the validation object 
			 * (rules are still active (AOP), but we just don't want any messages)
			 */
			validator.getResults().remove(validation);
		} catch (Exception e) {
			/*
			  throw new RuntimeException("pipeline(), " +
				"Failed to check/normalize " + title, e);
			*/ 
			//e.printStackTrace();
			log.error("pipeline(), Failed to process " + title, e);
			e.printStackTrace();
			return null;
		}
		
		return validation;
	}
	
	/**
	 * Creates new PaxtoolsDAO instance to work with existing "premerge"
	 * database. This is used both during the "pre-merge" (here) and "merge".
	 */
	public static PaxtoolsDAO buildPremergeDAO(String premergeDbName) {
		/* 
		 * set system properties and data source 
		 * (replaces existing one in the same thread),
		 * load another specific application context
		 */
		String home = CPathSettings.getHomeDir();
		if (home==null) {
			throw new RuntimeException(
				"Please set " + CPathSettings.HOME_VARIABLE_NAME + " environment variable " +
            	" (point to a directory where cpath.properties, etc. files are placed)");
		}
		
		// get the data source factory bean (aware of the driver, user, and password)
		ApplicationContext context = 
			new ClassPathXmlApplicationContext("classpath:internalContext-dsFactory.xml");
		DataServices dataServices = (DataServices) context.getBean("&dsBean");
		DataSource premergeDataSource = dataServices.getDataSource(premergeDbName);
		DataServicesFactoryBean.getDataSourceMap().put(CPathSettings.PREMERGE_DB_KEY, premergeDataSource);
		// get the premerge DAO
		context = new ClassPathXmlApplicationContext("classpath:internalContext-premerge.xml");	
		return (PaxtoolsDAO)context.getBean("premergePaxtoolsDAO");
	}

	public boolean isCreateDb() {
		return createDb;
	}
	public void setCreateDb(boolean createDb) {
		this.createDb = createDb;
	}

	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	
	
	private void fixDataSource(Model model, Metadata metadata) {
		Provenance pro = null;
		
		String urn; 
		try {
		 urn = MiriamLink.getDataTypeURI(metadata.getName());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(
				"Metadata 'name' must be a valid Miriam standard name " +
				"or synonym for the data source!", e);
		}
		
		if(model.containsID(urn))
			pro = (Provenance) model.getByID(urn);
		else {
			pro = model.addNew(Provenance.class, urn);
			Normalizer.autoName(pro); // + standard name and synonyms
		}

		// add additional info about the current version, source, identifier, etc...
		String keyComment = CPathSettings.CPATH2_GENERATED_COMMENT + ". ";
		if(!pro.getComment().contains(keyComment)) {
			pro.addComment(keyComment);
			pro.addComment(CPathSettings.CPATH2_GENERATED_COMMENT + 
				". Data loaded from: " + metadata.getURLToData());
			pro.addComment(CPathSettings.CPATH2_GENERATED_COMMENT + 
				". Data type: " + metadata.getType());
			pro.addComment(CPathSettings.CPATH2_GENERATED_COMMENT + 
					". Data version: " + metadata.getVersion() + 
					", " + metadata.getReleaseDate());
		}
		
		// add new value to each entity (but not to the model yet - it's
		// simpleMerger's job)
		for (Entity ent : model.getObjects(Entity.class)) {
			ent.addDataSource(pro);
		}
	}
}
