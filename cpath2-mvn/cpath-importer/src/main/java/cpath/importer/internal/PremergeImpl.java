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
import cpath.config.CPathSettings;
import cpath.dao.DataServices;
import cpath.dao.PaxtoolsDAO;
import cpath.dao.internal.DataServicesFactoryBean;
import cpath.importer.Premerge;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;
import cpath.warehouse.beans.Metadata.TYPE;

import org.biopax.paxtools.controller.AbstractTraverser;
import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.controller.Fetcher;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.controller.PropertyFilter;
import org.biopax.paxtools.io.simpleIO.SimpleEditorMap;
import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Pathway;
import org.biopax.validator.Behavior;
import org.biopax.validator.Validator;
import org.biopax.validator.result.Validation;
import org.biopax.validator.utils.BiopaxValidatorUtils;

import org.mskcc.psibiopax.converter.PSIMIBioPAXConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import java.io.*;

import javax.sql.DataSource;

/**
 * Class responsible for premerging pathway data.
 */
public class PremergeImpl extends Thread implements Premerge {

    private static Log log = LogFactory.getLog(PremergeImpl.class);

    private MetadataDAO metaDataDAO;
	private Validator validator;
	private PremergeDispatcher premergeDispatcher;
	private Metadata metadata;
	private Cleaner cleaner;

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
		assert(metadata.getType() == TYPE.BIOPAX
				|| metadata.getType() == TYPE.PSI_MI);
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
	  try {
		
		if(log.isInfoEnabled())
			log.info("run(), starting...");

		// sanity check
		if (metadata == null) {
			log.info("run(), metadata object is null.");
			return;
		}

		// get pathway data
		if(log.isInfoEnabled())
			log.info("run(), getting pathway data for provider.");
		Collection<PathwayData> pathwayDataCollection =
			metaDataDAO.getByIdentifierAndVersion(metadata.getIdentifier(), metadata.getVersion());

		// create cleaner
		if(log.isInfoEnabled())
			log.info("run(), getting a cleaner with name: " + metadata.getCleanerClassname());
		cleaner = getCleaner(metadata.getCleanerClassname());
		if (cleaner == null) {
			// TDB: report failure
			if(log.isInfoEnabled())
				log.info("run(), could not create cleaner class " 
					+ metadata.getCleanerClassname());
			return;
		}
		
		// build the premerge DAO - 
		// first, create a new database schema
		String premergeDbName = CPathSettings.CPATH_DB_PREFIX + metadata.getIdentifier();
		DataServicesFactoryBean.createSchema(premergeDbName);
		// next, get the PaxtoolsDAO instance
		PaxtoolsDAO pemergeDAO = buildPremergeDAO(premergeDbName);
		
		// iterate over and process all pathway data
		if(log.isInfoEnabled())
			log.info("run(), interating over pathway data " 
				+ metadata.getIdentifier());
		for (PathwayData pathwayData : pathwayDataCollection) {
			try {
				pipeline(pathwayData, pemergeDAO);
			} catch(Exception e) {
				log.error("pipeline(), failed for " + pathwayData, e);
			}
		}

	  } finally {
		premergeDispatcher.premergeComplete(metadata);
		if(log.isInfoEnabled())
			log.info("run(), exitting ("
				+ metadata.getIdentifier() 
				+ ") ...");
	  }
	}

	/**
	 * Pushes given PathwayData through pipeline.
	 *
	 * @param pathwayData provider's pathway data (usually from a single data file)
	 * @param premergeDAO persistent BioPAX model for the data provider
	 */
	private void pipeline(PathwayData pathwayData, PaxtoolsDAO premergeDAO) {
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
		
		// get the updated BioPAX OWL and save it in the pathwayData bean
		v.updateModelSerialized();
		pathwayData.setPremergeData(v.getModelSerialized());
		
		/* Now - add the serialized validation results 
		 * to the pathwayData entity bean, validationResults column.
		 * 
		 * (TODO using the last parameter, javax.xml.transform.Source
		 * of a XSLT stylesheet, the validation object can be 
		 * transformed to a human-readable report.)
		 */
		/* First, let's clear the (huge) 'serializedModel' field,
		 * because it's already saved in the 'premergeData' column 
		 */
		v.setModelSerialized(null);
		
		StringWriter writer = new StringWriter();
		BiopaxValidatorUtils.write(v, writer, null);
		writer.flush();		
		pathwayData.setValidationResults(writer.toString());
		
		// count error cases (ignoring warnings)
		int noErrors = v.countErrors(null, null, null, true);
		if(log.isInfoEnabled()) {
			log.info("pipeline(), summary for " + info
				+ ". Critical errors found:" + noErrors + ". " 
				+ v.getComment().toString() + "; " 
				+ v.toString());
		}
		
		
		// update pathwayData (with premergeData and validationResults)
		metaDataDAO.importPathwayData(pathwayData);
		
		// shall we continue with saving or reject this pathway data?
		if(noErrors <= 0 ) {			
			// Get the normalized and validated model and persist it
			if (log.isInfoEnabled())
				log.info("pipeline(), persisting pathway data "
					+ info);
			
			Model pathwayDataModel = v.getModel();
			
			// persist
			premergeDAO.merge(pathwayDataModel);
			// optimization: split into pathways and merge
			//splitAndMerge(pathwayDataModel, premergeDAO);
		}
		
	}

	
	/**
	 * Splits the "big" model into several 
	 * smaller self-consistent pathways.
	 * 
	 * @param bigModel
	 * @return
	 */
	private void splitAndMerge(Model bigModel, PaxtoolsDAO premergeDAO) 
	{
		PropertyFilter filter = new PropertyFilter() {
			@Override
			public boolean filter(PropertyEditor editor) {
				return !"nextStep".equalsIgnoreCase(editor.getProperty());
			}
		};
		EditorMap editorMap = new SimpleEditorMap(bigModel.getLevel());
		Fetcher fetcher = new Fetcher(editorMap, filter);
		SimpleReader reader = new SimpleReader(bigModel.getLevel());
		SimpleExporter exporter = new SimpleExporter(bigModel.getLevel());
		
		// collect "top" pathways only
		// copy set is required (otherwise remove() method is not supported!)
		final Set<Pathway> objects = new HashSet<Pathway>();
		objects.addAll(bigModel.getObjects(Pathway.class));
		AbstractTraverser checker = new AbstractTraverser(editorMap) {
			@Override
			protected void visit(Object value, BioPAXElement parent, Model model,
					PropertyEditor editor) {
				if(value instanceof Pathway && objects.contains(value)) 
					objects.remove(value); 
			}
		};	
		// this removes sub-pathways
		for(BioPAXElement e : bigModel.getObjects()) {
			checker.traverse(e, null);
		}
		
		// for each "top" element
		for(Pathway instance : objects) {
			if(log.isInfoEnabled())
				log.info("pipeline(), now fetching Pathway: "
					+ instance.getRDFId() + " into a separate model...");
			Model m = bigModel.getLevel().getDefaultFactory().createModel();
			fetcher.fetch(instance, m);
			m.getNameSpacePrefixMap().put("", bigModel.getNameSpacePrefixMap().get(""));
			// now, completely detach from the original model (via xml)
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try {
				// write
				exporter.convertToOWL(m, bytes);
				// read back
				InputStream is = new ByteArrayInputStream(bytes.toByteArray());
				m = reader.convertFromOWL(is);
			} catch (IOException e) {
				log.error("pipeline(), failed extracting a sub-model: ", e);
			}
			
			if(m != null || !m.getObjects().isEmpty())
				// persist
				premergeDAO.merge(m);
			else 
				log.error("pipeline(), Empty model resulted from " 
					+ instance.getRDFId());
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
			log.fatal(e);
			return null;
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
			log.error("pipeline(), Failed to process " + title + " - " + e);
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
}
