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
import cpath.dao.Analysis;
import cpath.dao.PaxtoolsDAO;
import cpath.importer.Cleaner;
import cpath.importer.Converter;
import cpath.importer.Fetcher;
import cpath.importer.Premerge;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Metadata.METADATA_TYPE;
import cpath.warehouse.beans.PathwayData;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.PublicationXref;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.validator.api.Validator;
import org.biopax.validator.api.beans.*;
import org.biopax.validator.api.ValidatorUtils;
import org.biopax.validator.impl.IdentifierImpl;
import org.biopax.validator.utils.Normalizer;

import org.mskcc.psibiopax.converter.PSIMIBioPAXConverter;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import java.io.*;


/**
 * Class responsible for premerging pathway data.
 */
final class PremergeImpl implements Premerge {

    private static Log log = LogFactory.getLog(PremergeImpl.class);
    private static final int BUFFER = 2048;
    private static final ResourceLoader LOADER = new DefaultResourceLoader();
    
	private final String xmlBase;

    private MetadataDAO metaDataDAO;
    private PaxtoolsDAO warehouseDAO;
	private Validator validator;
	private Cleaner cleaner;
	private String identifier;

	/**
	 * Constructor.
	 *
	 * @param metaDataDAO
	 * @param validator Biopax Validator
	 */
	PremergeImpl(final MetadataDAO metaDataDAO, final PaxtoolsDAO moleculesDAO, final Validator validator) 
	{
		this.metaDataDAO = metaDataDAO;
		this.warehouseDAO = moleculesDAO;
		this.validator = validator;
		this.xmlBase = CPathSettings.get(CPath2Property.XML_BASE);
	}

	
    /**
	 * (non-Javadoc)
	 * @see cpath.importer.Premerge#premerge
	 */
	@Override
    public void premerge() {

		Fetcher fetcher = ImportFactory.newFetcher(true);
		
		// grab all metadata
		Collection<Metadata> metadataCollection = metaDataDAO.getAllMetadata();

		// iterate over all metadata
		for (Metadata metadata : metadataCollection) {
			// use filter if set (identifier and version)
			if(identifier != null) {
				if(!metadata.getIdentifier().equals(identifier))
					continue;
			}
				
			try {
			
				if (metadata.getType().isNotPathwayData()) {
					log.info("premerge(), converting and saving Warehouse data: " 
						+ metadata.getUri());
					if(metadata.getType() == METADATA_TYPE.MAPPING)
						// read and import the mapping table to metaDataDAO
						storeMappingData(metadata);
					else // it's WAREHOUSE data; - convert and persist to the BioPAX Warehouse
						storeWarehouseData(metadata, (Model) warehouseDAO);
				} 
				else {
					log.info("premerge(), reading original pathway data: " +
						metadata.getIdentifier() + ", ver. " + metadata.getVersion());
					
					// Try to instantiate the Cleaner now, and exit if it fails!
					cleaner = null; //reset to null!
					final String cl = metadata.getCleanerClassname();
					if(cl != null && cl.length()>0) {
						cleaner = ImportFactory.newCleaner(cl);
						if (cleaner == null) {
							log.error("premerge(), failed to create the Cleaner: " + cl
								+ "; skipping for this data source...");
							return; // skip this data entirely due to the error
						} 			
					} else {
						log.info("premerge(), no Cleaner was specified; continue...");	
					}
										
					// read all files
					Collection<PathwayData> pathwayDataCollection 
						= fetcher.readPathwayData(metadata);
					
					// process pathway data
					for (PathwayData pathwayData : pathwayDataCollection) {
						pipeline(metadata, pathwayData);		
					}								
				} 				
				
			} catch (Exception e) {
				log.error("premerge(), error: ", e);
				e.printStackTrace();
			}
		}
		// at this point, warehouse and pathway data were converted, validated and saved.
		updateMappingData();
	}

	
	/**
	 * {@inheritDoc}
	 * 
	 * Extracts id-mapping information (name/id -> primary id) 
	 * from the Warehouse entity references's xrefs and
	 * puts first into a hash map (provided via the optional argument 
	 * in the Analysis.execute method) and then to the idmapping table.
	 *
	 */
	@Override
	public void updateMappingData() {
		log.info("premerge(), generating id-mapping table from the warehouse data...");
		//fill the id-mapping table from Warehouse EntityReference xrefs
		final Map<String,String> idMap = new HashMap<String, String>();
		
		// create a new Analysis object to populate the idMap within a DB transaction
		Analysis createIdMap = new Analysis() {
			
			@Override
			public Set<BioPAXElement> execute(Model model, Object... args) {
				// the first argument must be the Map
				final Map<String,String> idMap = (Map<String, String>) args[0];
				final Set<String> exclude = new HashSet<String>();
				
				// for each (UniProt) ProteinReference in the Warehouse,
				for(ProteinReference pr : model.getObjects(ProteinReference.class)) {
					//extract the primary id from the standard (identifiers.org) URI
					final String ac = pr.getRDFId().substring(pr.getRDFId().lastIndexOf('/')+1);
					for(Xref x : pr.getXref()) {
						//by (warehouse) design, there are various unif. and rel. xrefs added by the data converter
						if(!(x instanceof PublicationXref) && x.getDb() != null) {
							String id = x.getId();
							//ban an identifier associated with several different proteins
							if(exclude.contains(id)) {
								log.warn("premerge(), already excluded: " + id);
							} else if(idMap.containsKey(id) && !idMap.get(id).equals(ac)) {
								log.warn("premerge(), excluding " + id + 
									" from idMap because it maps to: " + ac + 
									" and " + idMap.get(id) + ", at least");
								idMap.remove(id);
								exclude.add(id);
							} else {
								idMap.put(id, ac);
							}
						}
					}
				}
				
				return null; //no return value required
			}			
		};
		
		// execute it
		warehouseDAO.runAnalysis(createIdMap, idMap);
		
		// persist mapping map
		metaDataDAO.importIdMapping(idMap);
	}
		
	
	/**
	 * Reads gene id-mapping records from a two-column text file
	 * and inserts into the db table (using MetadataDAO repository).
	 * 
	 * @param metadata
	 * @throws IOException 
	 */
	private void storeMappingData(Metadata metadata) throws IOException {
		Map<String,String> idMap = new HashMap<String, String>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(
			LOADER.getResource("file://" + metadata.localDataFile()).getInputStream()));

		while (reader.ready()) {
			String[] cols = reader.readLine().trim().split("\\s+");
			idMap.put(cols[0], cols[1]);
        }
		
		metaDataDAO.importIdMapping(idMap);
	}


	/**
	 * Pushes given PathwayData through pipeline.
	 *
	 * @param pathwayData provider's pathway data (usually from a single data file)
	 */
	private void pipeline(Metadata metadata, PathwayData pathwayData) {
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
		if (metadata.getType() == Metadata.METADATA_TYPE.PSI_MI) {
			log.info("pipeline(), converting psi-mi data " + info);
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
				new PSIMIBioPAXConverter(BioPAXLevel.L3, provider.getUri()+"_"); 
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
		// create a new empty validation (options: auto-fix=true, report all) and associate with the model
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

	String getIdentifier() {
		return identifier;
	}
	
	void setIdentifier(String identifier) {
		this.identifier = (identifier == null || identifier.isEmpty()) 
				? null : identifier;
	}
	
	
    /**
     * For the given Metadata, converts target data 
     * to EntityReference objects and adds to given model.
     *
	 * @param metadata
     * @param model target model
     * @throws IOException if an IO error occurs
     */
    static void storeWarehouseData(final Metadata metadata, final Model model) 
		throws IOException 
	{
		//shortcut for other/system warehouse data (not to be converted to BioPAX)
		if(metadata.getConverterClassname() == null 
				|| metadata.getConverterClassname().isEmpty()) 
		{
			log.info("storeWarehouseData(..), skip (no need to clean/convert) for: "
				+ metadata.getIdentifier() + " version: " + metadata.getVersion());
			return;
		}
				
		// use the local file (MUST have been previously fetched by Fetcher!)
		String urlStr = "file://" + metadata.localDataFile();
		InputStream is = new BufferedInputStream(LOADER.getResource(urlStr).getInputStream());
		log.info("storeWarehouseData(..): input stream is now open for provider: "
			+ metadata.getIdentifier() + " version: " + metadata.getVersion());
		
		try {
			// get an input stream from a resource file that is either .gz or
			// .zip
			if (urlStr.endsWith(".gz")) {
				is = new GZIPInputStream(is);
			} else if (urlStr.endsWith(".zip")) {
				ZipEntry entry = null;
				ZipInputStream zis = new ZipInputStream(is);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				while ((entry = zis.getNextEntry()) != null) {
					log.info("storeWarehouseData(..): processing zip entry: " 
						+ entry.getName());
					// write file to buffered output stream
					int count;
					byte data[] = new byte[BUFFER];
					BufferedOutputStream dest = new BufferedOutputStream(baos,
							BUFFER);
					while ((count = zis.read(data, 0, BUFFER)) != -1) {
						dest.write(data, 0, count);
					}
					dest.flush();
					dest.close();
				}
				
				is = new ByteArrayInputStream(baos.toByteArray());
				
			} else {
				log.info("storeWarehouseData(..): not using un(g)zip " +
					"(cannot guess from the extension) for " + urlStr);
			}

			log.info("storeWarehouseData(..): creating EntityReference objects, " +
				"provider: " + metadata.getIdentifier() + " version: "
					+ metadata.getVersion());

			// hook into a cleaner for given provider
			// Try to instantiate the Cleaner (if any) sooner, and exit if it fails!
			String cl = metadata.getCleanerClassname();
			Cleaner cleaner = null;
			if(cl != null && cl.length()>0) {
				cleaner = ImportFactory.newCleaner(cl);
				if (cleaner == null) {
					log.error("storeWarehouseData(..): " +
						"failed to create the specified Cleaner: " + cl);
					return; // skip for this data entry and return before reading anything
				}
			} else {
				log.info("storeWarehouseData(..): no Cleaner class was specified; " +
					"continue converting...");
			}
			
			// read the entire data from the input stream to a text string
			String data = readContent(is);
			
			// run the cleaner, if any -
			if(cleaner != null) {
				log.info("storeWarehouseData(..): running the Cleaner: " + cl);	
				data = cleaner.clean(data);
			}
			
			// re-open a new input stream for the cleaned data
			is = new BufferedInputStream(new ByteArrayInputStream(data.getBytes("UTF-8")));
			
			// hook into a converter for given provider
			cl = metadata.getConverterClassname();
			Converter converter = null;
			if(cl != null && cl.length()>0) {
				converter = ImportFactory.newConverter(cl);
				if(converter != null) {
					log.info("storeWarehouseData(..): running " +
							"the BioPAX Converter: " + cl);	
					// create a new empty in-memory model
					Model inMemModel = BioPAXLevel.L3.getDefaultFactory().createModel();
					inMemModel.setXmlBase(model.getXmlBase());
					// convert data into that
					converter.setModel(inMemModel);
					converter.convert(is);
					//repair
					log.info("storeWarehouseData(..): Preparing just created " +
						metadata.getIdentifier() + " BioPAX Model to merging...");
					inMemModel.repair();
					// merging may take quite a time...
					log.info("storeWarehouseData(..): Persisting " +
						metadata.getIdentifier());
					model.merge(inMemModel);
				}
				else 
					log.error(("storeWarehouseData(..): failed to create " +
						"the Converter class: " + cl
							+ "; so skipping for this warehouse data..."));
			} else {
				log.info("storeWarehouseData(..): No Converter class was specified; " +
					"so nothing else left to do");
			}

			log.info("storeWarehouseData(..): Exitting.");
			
		} finally {
	        try {
	            is.close();
	        } catch (Exception e) {
	           log.warn("is.close() failed." + e);
	        }
		}
	}
    
    
    /**
     * Reads from the stream to a string (UTF-8).
     * 
     * @param inputStream
     * @return
     * @throws IOException
     */
    private static String readContent(final InputStream inputStream) throws IOException 
    {
            BufferedReader reader = null;
    		StringBuilder toReturn = new StringBuilder();
            try {
                // we'd like to read lines at a time
                reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                // are we ready to read?
                while (reader.ready()) {
                	// NEWLINE here is critical for the protein/molecule cleaner/converter!
                    toReturn.append(reader.readLine()).append(CPathSettings.NEWLINE);
    			}
    		}
            catch (IOException e) {
                throw e;
            }
            finally {
    	        try {
    	            reader.close();
    	        } catch (Exception e) {
    	           log.warn("reader.close() failed." + e);
    	        }
            }

    		return toReturn.toString();
    }

}
