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


import cpath.config.CPathSettings;
import cpath.dao.Analysis;
import cpath.dao.CPathUtils;
import cpath.dao.MetadataDAO;
import cpath.dao.PaxtoolsDAO;
import cpath.importer.Cleaner;
import cpath.importer.Converter;
import cpath.importer.Premerger;
import cpath.warehouse.beans.Mapping;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Metadata.METADATA_TYPE;
import cpath.warehouse.beans.PathwayData;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.*;
import org.biopax.validator.api.Validator;
import org.biopax.validator.api.beans.*;
import org.biopax.validator.impl.IdentifierImpl;
import org.biopax.validator.utils.Normalizer;

import org.mskcc.psibiopax.converter.PSIMIBioPAXConverter;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import java.io.*;


/**
 * Class responsible for premerging pathway data.
 */
public final class PremergeImpl implements Premerger {

    private static Log log = LogFactory.getLog(PremergeImpl.class);
    private static final int BUFFER = 2048;
    private static final ResourceLoader LOADER = new DefaultResourceLoader();
    
	private final String xmlBase;

    private MetadataDAO metaDataDAO;
    private PaxtoolsDAO paxtoolsDAO;
	private Validator validator;
	private String identifier;

	/**
	 * Constructor.
	 *
	 * @param metaDataDAO
	 * @param validator Biopax Validator
	 * @param provider pathway data provider's identifier
	 */
	public PremergeImpl(final MetadataDAO metaDataDAO, final PaxtoolsDAO paxtoolsDAO, 
		final Validator validator, String provider) 
	{
		this.metaDataDAO = metaDataDAO;
		this.paxtoolsDAO = paxtoolsDAO;
		this.validator = validator;
		this.xmlBase = CPathSettings.xmlBase();
		this.identifier = (provider == null || provider.isEmpty()) 
				? null : provider;
	}

	
    /**
	 * (non-Javadoc)
	 * @see cpath.importer.Premerger#premerge
	 */
	@Override
    public void premerge() {

		// grab all metadata (initially, there are no pathway data files yet;
		// but if premerge was already called, there are can be not empty pathwayData 
		// and result files for the corresp. metadata objects, which will be cleared anyway.)
		List<Metadata> metadataCollection = metaDataDAO.getAllMetadata();
		// iterate over all metadata
		for (Metadata metadata : metadataCollection) {
			// use filter if set (identifier and version)
			if(identifier != null) {
				if(!metadata.getIdentifier().equals(identifier))
					continue;
			}
			
			try {	
				//if it is pathway data (not 'mapping' or 'warehouse' data)
				if (!metadata.getType().isNotPathwayData()) {
					log.info("premerge(), now processing " +
						metadata.getIdentifier() );
					
					// Try to instantiate the Cleaner now, and exit if it fails!
					Cleaner cleaner = null; //reset to null!
					final String cl = metadata.getCleanerClassname();
					if(cl != null && cl.length()>0) {
						cleaner = ImportFactory.newCleaner(cl);
						if (cleaner == null) {
							log.error("premerge(), failed to create the Cleaner: " + cl
								+ "; skipping for this data source...");
							return; // skip this data entirely due to the error
						} 			
					} else {
						log.info("premerge(), no Cleaner class was specified; continue...");	
					}
										
					// clear all existing output files, parse input files, reset counters, save.
					log.debug("no. pd before init, " + metadata.getIdentifier() + ": " + metadata.getPathwayData().size());
					metadata = metaDataDAO.init(metadata);
					//load orig. pathway data
					CPathUtils.readPathwayData(metadata);
					// Premerge for each pathway data: clean, convert, validate, 
					// and then update premergeData, validationResults db fields.
					for (PathwayData pathwayData : metadata.getPathwayData()) {
						pipeline(metadata, pathwayData, cleaner);
					}
					// save/update validation status
					metaDataDAO.saveMetadata(metadata);
					log.debug("no. pd after saved, " + metadata.getIdentifier() + ": " + metadata.getPathwayData().size());
				} 				
				
			} catch (Exception e) {
				log.error("premerge(): failed", e);
				e.printStackTrace();
			}
		}
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void buildWarehouse() {		
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
						updateIdMapping(metadata, metaDataDAO); //TODO implement this optional method (it is empty)
					else // it's WAREHOUSE data; - convert and persist to the BioPAX Warehouse
						updateBiopaxWarehouse(metadata, (Model) paxtoolsDAO);
				} 			
				
			} catch (Exception e) {
				log.error("premerge(), error: ", e);
				e.printStackTrace();
			}
		}
	}
		

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateIdMapping(boolean writeExcludedIdsToFile) {		
		log.info("updateIdMapping(), updating id-mapping tables by analyzing the warehouse data...");
		
		// create and execute a new Analysis that populates the id maps within 
		// a new DB transaction
		GeneIdMappingAnalysis geneIdMappingAnalysis = new GeneIdMappingAnalysis();
		paxtoolsDAO.run(geneIdMappingAnalysis);
		//save new Mapping entity with type UNIPROT
		Mapping uniprotMapping = new Mapping(Mapping.Type.UNIPROT, 
			"From Gene symbols, secondary IDs, RefSeq, NCBI Gene, etc. to primary UniProt ID." +
			"(generated from the cPath2 UniProt warehouse (xrefs)", geneIdMappingAnalysis.getIdMap());
		metaDataDAO.saveMapping(uniprotMapping);
		
		//save ambiguous mappings to a file in the cpath2 home dir.
		if(writeExcludedIdsToFile)
			exportAmbiguousMappings(Mapping.Type.UNIPROT, 
					geneIdMappingAnalysis.getAmbiguousIdMap());
		
		ChemIdMappingAnalysis chemIdMappingAnalysis = new ChemIdMappingAnalysis();
		paxtoolsDAO.run(chemIdMappingAnalysis);
		//save new Mapping entity with type CHEBI
		Mapping chebiMapping = new Mapping(Mapping.Type.CHEBI, 
			"From secondary IDs, PUBCHEM, InChIKey to primary CHEBI ID." +
			"(generated from the cPath2 UniProt warehouse (xrefs)", chemIdMappingAnalysis.getIdMap());
		metaDataDAO.saveMapping(chebiMapping);
		
		//save ambiguous mappings to a file in the cpath2 home dir.
		if(writeExcludedIdsToFile)
			exportAmbiguousMappings(Mapping.Type.CHEBI, 
					chemIdMappingAnalysis.getAmbiguousIdMap());
		
		log.info("updateIdMapping(), exitting...");
	}
	
	
	private void exportAmbiguousMappings(Mapping.Type type,
			Map<String, Set<String>> ambiguousIdMap) {
		try {
			PrintWriter writer = new PrintWriter(CPathSettings.downloadsDir() 
				+ File.separator + "ambiguous_id_mapping."
				+ type + ".txt");
			writer.println("#Identifier\tPrimary accessions it maps to (separated by tab)");
			for(String id : ambiguousIdMap.keySet()) {
				writer.println(id + "\t" + StringUtils.join(ambiguousIdMap.get(id), '\t'));
			}
			writer.close();
		} catch (IOException e) {
			log.error("Failed to create output file for ambiguous id mappings.", e);
		}	
	}


	/**
	 * Reads gene id-mapping records from a two-column text file
	 * and inserts into the table (using MetadataDAO repository).
	 * 
	 * @param metadata
	 * @param metaDataDAO
	 * @throws IOException 
	 */
	private void updateIdMapping(Metadata metadata, MetadataDAO metaDataDAO) throws IOException {
		//TODO implement: decide file format; consider mapping for genes/proteins and for chemicals separately.
	}


	/**
	 * Pushes given PathwayData through pipeline:
	 * Updates the PathwayData object adding the 
	 * normalized data (BioPAX L3) and the validation results 
	 * (XML report and status)
	 * 
	 * @param metadata
	 * @param pathwayData provider's pathway data (usually from a single data file) to be processed and modified
	 * @param cleaner data specific cleaner class (to apply before the validation/normalization)
	 */
	private void pipeline(final Metadata metadata, final PathwayData pathwayData, Cleaner cleaner)
	{
		
		// here go data to process
		String data = new String(pathwayData.getData());
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
		
		pathwayData.setData(data.getBytes()); //writes data file
		
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
		
		pathwayData.setData(data.getBytes()); //writes data file
		
		log.info("pipeline(), validating pathway data "	+ info);		
		
		/* Validate, auto-fix, and normalize (incl. convesion to L3): 
		 * e.g., synonyms in xref.db may be replaced 
		 * with the primary db name, as in Miriam, etc.
		 */
		Validation v = checkAndNormalize(pathwayData, metadata);
		if(v == null) {
			log.warn("pipeline(), skipping: " + info);
			return;
		}
			
		// save the normalized BioPAX
		pathwayData.setNormalizedData(v.getModelData().getBytes());
		
		/* clear the huge 'serializedModel',
		 * because it's already saved 
		 */
		v.setModelData(null);
		
		//save report data
		pathwayData.setValidationReport(v);
		
		// count critical not fixed error cases (ignore warnings and fixed ones)
		int noErrors = v.countErrors(null, null, null, null, true, true);
		log.info("pipeline(), summary for " + info
			+ ". Critical errors found:" + noErrors + ". " 
			+ v.getComment().toString() + "; " + v.toString());
		
		if(noErrors > 0) 
			pathwayData.setValid(false); 
		else 
			pathwayData.setValid(true);
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
	private Validation checkAndNormalize(PathwayData pathwayData, Metadata metadata) 
	{	
		final String title = pathwayData.toString();
		final byte[] data = pathwayData.getData();
		
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
		normalizer.setDescription(title);
		
		// because errors are also reported during the import (e.g., syntax)
		try {
			validator.importModel(validation, new ByteArrayInputStream(data));			
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

	
    /**
     * For the given Metadata, converts target data 
     * to EntityReference objects and adds to given 
     * (BioPAX data Warehouse) model.
     *
	 * @param metadata
     * @param warehouseModel target model
     * @throws IOException if an IO error occurs
     */
    private void updateBiopaxWarehouse(final Metadata metadata, final Model warehouseModel) 
		throws IOException 
	{
		//shortcut for other/system warehouse data (not to be converted to BioPAX)
    	//TODO e.g., simply merge if already BioPAX data (currently, we do not expect this)
		if(metadata.getConverterClassname() == null 
				|| metadata.getConverterClassname().isEmpty()) 
		{
			log.info("updateBiopaxWarehouse: skipping (no Converter): "
				+ metadata.getIdentifier() );
			return;
		}
				
		// use the local file
		String urlStr = metadata.origDataLocation();
		InputStream is = new BufferedInputStream(LOADER.getResource(urlStr).getInputStream());
		log.info("updateBiopaxWarehouse: input stream is now open for provider: "
			+ metadata.getIdentifier());
		
		try {
			// get an input stream from a resource file that is either .gz or .zip
			if (urlStr.endsWith(".gz")) {
				is = new GZIPInputStream(is);
			} else if (urlStr.endsWith(".zip")) {
				ZipEntry entry = null;
				ZipInputStream zis = new ZipInputStream(is);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				while ((entry = zis.getNextEntry()) != null) {
					log.info("updateBiopaxWarehouse: processing zip entry: " 
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
				zis.close();
				
				is = new ByteArrayInputStream(baos.toByteArray());
				
			} else {
				log.info("updateBiopaxWarehouse: not using un(g)zip " +
					"(cannot guess from the extension) for " + urlStr);
			}

			// hook into a cleaner for given provider
			// Try to instantiate the Cleaner (if any) sooner, and exit if it fails
			String cl = metadata.getCleanerClassname();
			Cleaner cleaner = null;
			if(cl != null && cl.length()>0) {
				cleaner = ImportFactory.newCleaner(cl);
				if (cleaner == null) {
					log.error("updateBiopaxWarehouse: " +
						"failed to create the specified Cleaner: " + cl);
					return; // skip for this data entry and return before reading anything
				}
			} else {
				log.info("updateBiopaxWarehouse: no Cleaner class was specified; " +
					"continue converting...");
			}
			
			// read the entire data from the input stream to a text string
			String data = readContent(is);			
			// run the cleaner, if any -
			if(cleaner != null) {
				log.info("updateBiopaxWarehouse: running the Cleaner: " + cl);	
				data = cleaner.clean(data);
			}
					
			// hook into a converter for given provider
			cl = metadata.getConverterClassname();
			if(cl != null && cl.length() > 0) {
				final Converter converter = ImportFactory.newConverter(cl);
				if(converter != null) {
					log.info("updateBiopaxWarehouse: running " + "Converter: " + cl);	
					
					// open a new input stream for the cleaned data
					// and initialize the Converter object
					final InputStream dataStream = new BufferedInputStream(
							new ByteArrayInputStream(data.getBytes("UTF-8")));	
					converter.setInputStream(dataStream);
					converter.setXmlBase(warehouseModel.getXmlBase());
					
					if(converter instanceof Analysis) { //e.g. ChEBI OBO conv.					
						if(warehouseModel instanceof PaxtoolsDAO) {
							log.info("updateBiopaxWarehouse: converting data " 
								+ "within a Hibernate transaction...");	
							((PaxtoolsDAO) warehouseModel).run((Analysis) converter);
						} 
						else { //for junit tests (in-memory Warehouse model)
							log.info("updateBiopaxWarehouse: converting... ");
							((Analysis) converter).execute(warehouseModel);
						}
					}
					else {
						// the converter does not modify existing data;
						// returns a new in-memory biopax model
						log.info("updateBiopaxWarehouse: in-memory converting... ");
						Model generatedModel = converter.convert();
						log.info("updateBiopaxWarehouse: Merging the warehouse model " +
								"converted from: " + metadata.getIdentifier());
						warehouseModel.merge(generatedModel);
					}
					
				}
				else {
					log.error(("updateBiopaxWarehouse: failed to create " +
						"the Converter class: " + cl
						+ "; so skipping for this warehouse data..."));
				}
			} 
			else {
				log.info("updateBiopaxWarehouse: No Converter class was specified; " +
					"so nothing else left to do");
			}

			log.info("updateBiopaxWarehouse: Exitting.");
			
		} catch(Exception e) {
			log.error(e);
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
    		final String NEWLINE = System.getProperty ( "line.separator" );
            try {
                // we'd like to read lines at a time
                reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                // are we ready to read?
                while (reader.ready()) {
                	// NEWLINE here is critical for the protein/molecule cleaner/converter
                    toReturn.append(reader.readLine()).append(NEWLINE);
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
