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
import cpath.config.CPathSettings;
import cpath.config.CPathSettings.CPath2Property;
import cpath.dao.PaxtoolsDAO;
import cpath.dao.internal.DataServicesFactoryBean;
import cpath.importer.Cleaner;
import cpath.importer.Premerge;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.*;
import org.biopax.validator.api.Validator;
import org.biopax.validator.api.beans.*;
import org.biopax.validator.api.ValidatorUtils;
import org.biopax.validator.impl.IdentifierImpl;
import org.biopax.validator.utils.Normalizer;

import org.mskcc.psibiopax.converter.PSIMIBioPAXConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

import java.io.*;


/**
 * Class responsible for premerging pathway data.
 */
final class PremergeImpl implements Premerge {

    private static Log log = LogFactory.getLog(PremergeImpl.class);
    
	private final String xmlBase;

    private MetadataDAO metaDataDAO;
	private Validator validator;
	private Cleaner cleaner;
	private boolean createDb;
	private String identifier;
	private String version;

	/**
	 * Constructor.
	 *
	 * @param metaDataDAO
	 * @param validator Biopax Validator
	 */
	PremergeImpl(final MetadataDAO metaDataDAO, final Validator validator) 
	{
		this.metaDataDAO = metaDataDAO;
		this.validator = validator;
		this.createDb = true;
		this.xmlBase = CPathSettings.get(CPath2Property.XML_BASE);
	}

    /**
	 * (non-Javadoc)
	 * @see cpath.importer.Premerge#premerge
	 */
	@Override
    public void premerge() {

		// grab all metadata
		Collection<Metadata> metadataCollection = metaDataDAO.getAllMetadata();

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
			if (!metadata.getType().isWarehouseData()) {
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
			log.info("run(), getting pathway data for provider " 
					+ metadata);
		Collection<PathwayData> pathwayDataCollection =
			metaDataDAO.getPathwayDataByIdentifierAndVersion(metadata.getIdentifier(), metadata.getVersion());

		// Try to instantiate the Cleaner now, and exit if it fails!
		cleaner = null; //reset to null!
		final String cl = metadata.getCleanerClassname();
		if(cl != null && cl.length()>0) {
			cleaner = ImportFactory.newCleaner(cl);
			if (cleaner == null) {
				log.error("run(), failed to create the Cleaner: " + cl
					+ "; skipping for this data source...");
				return; // skip this data entirely due to the error
			} 			
		} else {
			log.info("run(), no Cleaner was specified; continue...");	
		}
		
		PaxtoolsDAO pemergeDAO = null;

		if (isCreateDb()) {
			// build the premerge DAO -
			// first, create a new database schema
			String premergeDbName = "cpath2_"
				+ metadata.getIdentifier() + "_" + metadata.getVersion();
			if(log.isInfoEnabled())
				log.info("Creating a new 'pre-merge' db and schema: " +
						premergeDbName);
			DataServicesFactoryBean.createSchema(premergeDbName);
			// next, get the PaxtoolsDAO instance
			pemergeDAO = ImportFactory.buildPaxtoolsHibernateDAO(premergeDbName);
		}
	
		// iterate over and process all pathway data
		if(log.isInfoEnabled())
			log.info("run(), interating over pathway data " 
				+ metadata.getIdentifier());
		for (PathwayData pathwayData : pathwayDataCollection) {
			try {
				pipeline(metadata, pathwayData, pemergeDAO);
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
		// here go data to process
		String data = new String(pathwayData.getPathwayData());
		String info = pathwayData.toString();
		
		/*
		 * First, get and clean (not modifying the original) pathway data,
		 * in other words, - apply data provider-specific "quick fixes"
		 * (a Cleaner class can be specified in the metadata.conf)
		 * to the original data (in PSI_MI, BioPAX L2, or L3 formats)
		 */
		if(cleaner != null) {
			log.info("pipeline(), cleaning data " + info +
				" with " + cleaner.getClass());
			data = cleaner.clean(data);
		}
		
		// Second, if psi-mi, convert to biopax L3
		if (metadata.getType() == Metadata.TYPE.PSI_MI) {
			if (log.isInfoEnabled())
				log.info("pipeline(), converting psi-mi data "
						+ info);
			try {
				data = convertPSIToBioPAX(data, metadata);
			} catch (RuntimeException e) {
				log.error("pipeline(), cannot convert PSI-MI data: "
						+ info + " to L3. - " + e);
				return;
			}
		} 
		
		log.info("pipeline(), validating pathway data "	+ info);
		
		
		/* Validate, auto-fix, and normalize (incl. convesion to L3): 
		 * e.g., synonyms in xref.db may be replaced 
		 * with the primary db name, as in Miriam, etc.
		 */
		Validation v = checkAndNormalize(pathwayData.toString(), data, metadata);
		if(v == null) {
			log.warn("pipeline(), skipping: " + info);
			return;
		}
			
		// get the updated BioPAX OWL and save it in the pathwayData bean
		pathwayData.setPremergeData(v.getModelData().getBytes());
		
		/* Now - add the serialized validation results 
		 * to the pathwayData entity bean, validationResults column.
		 * (using the last parameter, javax.xml.transform.Source
		 * of a XSLT stylesheet, the validation object can be 
		 * further transformed to a human-readable report.)
		 */
		
		/* but first, let's clear the (huge) 'serializedModel' field,
		 * because it's already saved in the 'premergeData' column 
		 */
		v.setModelData(null);
		
		StringWriter writer = new StringWriter();
		ValidatorUtils.write(v, writer, null);
		writer.flush();		
		final String validationResultStr = writer.toString();
		pathwayData.setValidationResults(validationResultStr.getBytes());
		
		// count critical not fixed error cases (ignore warnings and fixed ones)
		int noErrors = v.countErrors(null, null, null, null, true, true);
		log.info("pipeline(), summary for " + info
			+ ". Critical errors found:" + noErrors + ". " 
			+ v.getComment().toString() + "; " + v.toString());
		
		if(noErrors > 0) 
			pathwayData.setValid(false); 
		else 
			pathwayData.setValid(true);
		
		// update pathwayData (with premergeData and validationResults)
		metaDataDAO.importPathwayData(pathwayData);
		
		// persist
		if(isCreateDb()) {			
			// Get the normalized and validated model and persist it
			log.info("pipeline(), persisting pathway data "	+ info);
			
			Model pathwayDataModel = (Model) v.getModel();
			
			// persist all at once -
			premergeDAO.merge(pathwayDataModel);
		}
	}

	
	/**
	 * Converts psi-mi string to biopax 
	 * using a unique xml:base for URIS, which consists of 
	 * both our current xml:base and provider-specific part -
	 * to prevent URI clash from different PSI-MI data.
	 *
	 * @param psimiData String
	 * @param provider
	 */
	private String convertPSIToBioPAX(final String psimiData, Metadata provider) {

		String toReturn = "";
				
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			InputStream is = new ByteArrayInputStream(psimiData.getBytes("UTF-8"));
			PSIMIBioPAXConverter psimiConverter = 
				new PSIMIBioPAXConverter(BioPAXLevel.L3, provider.uri()+"_"); 
			psimiConverter.convert(is, os);
			toReturn = os.toString();
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}

		return toReturn;
	}

	
	/**
	 * Validates, fixes, and normalizes given pathway data.
	 *
	 * @param title short description
	 * @param data BioPAX OWL data
	 * @param metadata data provider's metadata
	 * @return
	 */
	private Validation checkAndNormalize(final String title, String data, Metadata metadata) 
	{	
		// create a new empty validation (use auto-fix, report all errors options) and associate with the model
		Validation validation = new Validation(new IdentifierImpl(), 
				title, true, Behavior.WARNING, 0, null); // sets the title
		
		// configure Normalizer
		Normalizer normalizer = new Normalizer();
		// set cpath2 xml:base for the normalizer to use instead of the model's one (important!)
		normalizer.setXmlBase(xmlBase);
		// to infer/auto-fix biopax properties
		normalizer.setFixDisplayName(true); // important
		normalizer.setInferPropertyDataSource(false); // not important since we started generating Provenance from Metadata
		normalizer.setInferPropertyOrganism(true); // important (for filtering by organism)
		
		// because errors are also reported during the import (e.g., syntax)
		try {
			validator.importModel(validation, new ByteArrayInputStream(data.getBytes("UTF-8")));			
			validator.validate(validation);
			// unregister the validation object 
			validator.getResults().remove(validation);
			
			// normalize
			Model model = (Model) validation.getModel();
			normalizer.normalize(model);
			
			// (in addition to normalizer's job) find existing or create new Provenance 
			// from the metadata to add it explicitly to all entities -
			metadata.setProvenanceFor(model);
			
			validation.setModelData(SimpleIOHandler.convertToOwl(model));
			
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

	
	boolean isCreateDb() {
		return createDb;
	}
	void setCreateDb(boolean createDb) {
		this.createDb = createDb;
	}

	String getIdentifier() {
		return identifier;
	}
	void setIdentifier(String identifier) {
		this.identifier = (identifier == null || identifier.isEmpty()) 
				? null : identifier;
	}

	String getVersion() {
		return version;
	}
	void setVersion(String version) {
		this.version = (version == null || version.isEmpty()) 
				? null : version;
	}
	
}
