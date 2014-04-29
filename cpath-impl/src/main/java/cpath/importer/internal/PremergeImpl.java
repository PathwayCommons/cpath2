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
import cpath.dao.CPathUtils;
import cpath.dao.MetadataDAO;
import cpath.dao.PaxtoolsDAO;
import cpath.importer.Cleaner;
import cpath.importer.Converter;
import cpath.importer.Premerger;
import cpath.warehouse.beans.Content;
import cpath.warehouse.beans.Mapping;
import cpath.warehouse.beans.Mapping.Type;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Metadata.METADATA_TYPE;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.*;
import org.biopax.validator.api.Validator;
import org.biopax.validator.api.beans.*;
import org.biopax.validator.impl.IdentifierImpl;
import org.biopax.validator.utils.Normalizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import java.io.*;


/**
 * Class responsible for premerging pathway and warehouse data.
 */
public final class PremergeImpl implements Premerger {

    private static Logger log = LoggerFactory.getLogger(PremergeImpl.class);
    
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
		this.xmlBase = CPathSettings.getInstance().getXmlBase();
		this.identifier = (provider == null || provider.isEmpty()) 
				? null : provider;
	}

	
    /**
	 * (non-Javadoc)
	 * @see cpath.importer.Premerger#premerge
	 */
    public void premerge() {

		// grab all metadata (initially, there are no pathway data files yet;
		// but if premerge was already called, there are can be not empty dataFile 
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
					log.info("premerge(), now processing " + metadata.getIdentifier() );
					
					// Try to instantiate the Cleaner now, and exit if it fails!
					Cleaner cleaner = null; //reset to null!
					String cl = metadata.getCleanerClassname();
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
					
					Converter converter = null;
					cl = metadata.getConverterClassname();
					if(cl != null && cl.length()>0) {
						converter = ImportFactory.newConverter(cl);
						if (converter == null) {
							log.error("premerge(), failed to create the Converter: " + cl
								+ "; skipping for this data source...");
							return; // skip due to the error
						} 
						
						// initialize
						converter.setXmlBase(xmlBase);
						
					} else {
						
						log.info("premerge(), no Converter class was specified; continue...");	
						
						
					}
										
					// clear all existing output files, parse input files, reset counters, save.
					log.debug("no. pd before init, " + metadata.getIdentifier() + ": " + metadata.getContent().size());
					metadata = metaDataDAO.init(metadata);
					
					//load/re-pack/save orig. data
					CPathUtils.analyzeAndOrganizeContent(metadata);
					
					// Premerge for each pathway data: clean, convert, validate, 
					// and then update premergeData, validationResults db fields.
					for (Content content : new HashSet<Content>(metadata.getContent())) {
						try {					
							pipeline(metadata, content, cleaner, converter);
						} catch (Exception e) {
							metadata.getContent().remove(content);
							log.warn("premerge(), removed " + content 
									+ " due to error", e);
						}		
					}
					
					// save/update validation status
					metadata = metaDataDAO.saveMetadata(metadata);
					log.debug("premerge(), for " + metadata.getIdentifier() + 
						", saved " + metadata.getContent().size() + " files");
				
			} catch (Exception e) {
				log.error("premerge(): failed", e);
				e.printStackTrace();
			}
		}
	}

	
	public void buildWarehouse() {		
		// grab all metadata
		Collection<Metadata> metadataCollection = metaDataDAO.getAllMetadata();
		// iterate over all metadata
		for (Metadata metadata : metadataCollection) 
		{
			//skip not warehouse data
			if (metadata.getType() != METADATA_TYPE.WAREHOUSE)
				continue; 
			
			log.info("buildWarehouse(), adding data: " + metadata.getUri());			
			InputStream inputStream;
			for(Content content : metadata.getContent()) {
				try {
					inputStream = new GZIPInputStream(new FileInputStream(content.convertedFile()));
					Model m = new SimpleIOHandler(BioPAXLevel.L3).convertFromOWL(inputStream);				
					paxtoolsDAO.merge(m);
				} catch (IOException e) {
					log.error("buildWarehouse, skip for " + content.toString() + 
						"; failed to read/merge from " + content.convertedFile(), e);
					continue;
				}
			}
		}
		
		// Using the Warehouse, generate the id-mapping tables
		// from that BioPAX model:
		buildIdMappingFromWarehouse(true);
		
		// Next, process all MAPPING data files
		for (Metadata metadata : metadataCollection) 
		{
			//skip not id-mapping data
			if (metadata.getType() != METADATA_TYPE.MAPPING)
				continue; 
			
			log.info("buildWarehouse(), adding id-mapping: " + metadata.getUri());			
			for(Content content : metadata.getContent()) {
				try {
					Mapping mapping = simpleMapping(content, new GZIPInputStream(
							new FileInputStream(content.originalFile())));
					metaDataDAO.saveMapping(mapping);
				} catch (Exception e) {
					log.error("buildWarehouse(), failed to get id-mapping, " +
							"using: " + content.toString(), e);
					continue;
				}
			}
		}				
	}


	/**
	 * Creates a mapping object 
	 * from a simple two-column (tab-separated) text file, 
	 * where the first line contains standard names of 
	 * the source and target ID types, and on each next line -
	 * source and target IDs, respectively.
	 * Currently, only ChEBI and UniProt are supported
	 * (valid) as the target ID type.
	 * 
	 * This is a package-private method, mainly for jUnit testing
	 * (not API).
	 * 
	 * @param content
	 * @param inputStream
	 * @return
	 * @throws IOException 
	 */
	Mapping simpleMapping(Content content, InputStream inputStream) 
			throws IOException 
	{
		Map<String, String> map = new HashMap<String, String>();
		Set<String> added = new HashSet<String>();		
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));				
		String line = reader.readLine(); //get the first, title line
		String head[] = line.split("\t");
		assert head.length == 2 : "bad header";
		Type type = Type.valueOf(head[1].toUpperCase().trim());
		while ((line = reader.readLine()) != null) {
			String pair[] = line.split("\t");
			String src = pair[0].trim();
			String tgt = pair[1].trim();
			
			if(added.contains(tgt))
				log.warn("skip " + src + "; another id was already mapped to " + tgt);
			
			if(map.containsKey(src) && !map.get(src).equals(tgt)) 
				log.warn("skip " + src + "; already mapped to " + map.get(src));

			map.put(src, tgt);
			added.add(tgt);
		}
		reader.close();
		
		return new Mapping(type, content.toString(), map);
	}


	/**
	 * Extracts id-mapping information (name/id -> primary id) 
	 * from the Warehouse entity references's xrefs to the mapping tables.
	 * 
	 * @param exportAmbiguousMappings whether to file errors or not (ambiguous id mappings)
	 */
	private void buildIdMappingFromWarehouse(boolean exportAmbiguousMappings) {		
		log.info("updateIdMapping(), updating id-mapping " +
				"tables by analyzing the warehouse data...");
		
		// create and execute a new Analysis that populates the id maps within 
		// a new DB transaction
		GeneIdMappingAnalysis geneIdMappingAnalysis = new GeneIdMappingAnalysis();
		paxtoolsDAO.run(geneIdMappingAnalysis);
		//save new Mapping entity with type UNIPROT
		Mapping uniprotMapping = new Mapping(Mapping.Type.UNIPROT, 
			"From Gene symbols, secondary IDs, RefSeq, NCBI Gene, etc. to primary UniProt ID." +
			"(generated from the cPath2 UniProt warehouse (xrefs)", geneIdMappingAnalysis.getIdMap());
		metaDataDAO.saveMapping(uniprotMapping);
		
		//save ambiguous mappings to a special error file.
		if(exportAmbiguousMappings)
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
		if(exportAmbiguousMappings)
			exportAmbiguousMappings(Mapping.Type.CHEBI, 
					chemIdMappingAnalysis.getAmbiguousIdMap());
		
		log.info("updateIdMapping(), exitting...");
	}
	
	
	private void exportAmbiguousMappings(Mapping.Type type,
			Map<String, Set<String>> ambiguousIdMap) {
		try {
			PrintWriter writer = new PrintWriter(CPathSettings.getInstance().downloadsDir() 
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
	 * Pushes given Content through pipeline:
	 * Updates the Content object adding the 
	 * normalized data (BioPAX L3) and the validation results 
	 * (XML report and status)
	 * 
	 * @param metadata
	 * @param dataFile provider's pathway data (usually from a single data file) to be processed and modified
	 * @param cleaner data specific cleaner class (to apply before the validation/normalization)
	 * @throws IOException 
	 */
	private void pipeline(final Metadata metadata, final Content content, 
			Cleaner cleaner, Converter converter) throws IOException 
	{	
		final String info = content.toString();
		
		InputStream dataStream = new GZIPInputStream(new FileInputStream(content.originalFile()));
		
		//Clean the data, i.e., apply data-specific "quick fixes".
		if(cleaner != null) {
			log.info("pipeline(), cleaning " + info + " with " + cleaner.getClass());
			OutputStream os = new GZIPOutputStream(new FileOutputStream(content.cleanedFile()));
			cleaner.clean(dataStream, os); //os must be closed inside
			//re-assign the input data stream
			dataStream = new GZIPInputStream(new FileInputStream(content.cleanedFile())); 
		}
		
		//Convert data to BioPAX L3 if needed
		if (converter != null) {
			log.info("pipeline(), converting " + info + " with " + converter.getClass());					
			OutputStream os = new GZIPOutputStream(new FileOutputStream(content.convertedFile()));
			converter.convert(dataStream, os);//os must be closed inside
			if(metadata.isNotPathwayData())	{
				return; //all done!
			} 
			
			dataStream = new GZIPInputStream(new FileInputStream(content.convertedFile())); 
			
		} else if(metadata.getType() == METADATA_TYPE.WAREHOUSE) {
			log.warn("pipeline(), no BioPAX converter set for warehouse data: " 
					+ info + "; suppose it's BioPAX");				
			File src = new File(
				(cleaner != null) ? content.cleanedFile() : content.originalFile()
			);			
			FileUtils.moveFile(src, new File(content.convertedFile()));
			return; //all done!
		}
		
		//Validate and normalize cleaned and converted pathway data
		if(!metadata.isNotPathwayData()) {
			// process a pathway/interaction data file
			log.info("pipeline(), validating pathway data "	+ info);		

			/* Validate, auto-fix, and normalize (incl. convesion to L3): 
			 * e.g., synonyms in xref.db may be replaced 
			 * with the primary db name, as in Miriam, etc.
			 */
			//use a file instead of String for the RDF/XML data (which can be >2Gb and fail!)
			Validation v = checkAndNormalize(info, dataStream, metadata, content.normalizedFile());

			//save report data
			content.saveValidationReport(v);

			// count critical not fixed error cases (ignore warnings and fixed ones)
			int noErrors = v.countErrors(null, null, null, null, true, true);
			log.info("pipeline(), summary for " + info
					+ ". Critical errors found:" + noErrors + ". " 
					+ v.getComment().toString() + "; " + v.toString());

			if(noErrors > 0) 
				content.setValid(false); 
			else 
				content.setValid(true);
		}
	}

	
	/**
	 * Validates, fixes, and normalizes given pathway data.
	 *
	 * @param title short description
	 * @param biopaxStream BioPAX OWL
	 * @param metadata data provider's metadata
	 * @param
	 * @return the object explaining the validation/normalization results
	 */
	private Validation checkAndNormalize(String title, InputStream biopaxStream, Metadata metadata, String outFileName) 
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
		normalizer.setDescription(title);
		
		// because errors are also reported during the import (e.g., syntax)
		try {
			validator.importModel(validation, biopaxStream);			
			validator.validate(validation);
			// unregister the validation object 
			validator.getResults().remove(validation);
			
			// normalize
			Model model = (Model) validation.getModel();
			normalizer.normalize(model);
			
			// (in addition to normalizer's job) find existing or create new Provenance 
			// from the metadata to add it explicitly to all entities -
			metadata.setProvenanceFor(model);
			
			OutputStream out = new GZIPOutputStream(new FileOutputStream(outFileName));
			(new SimpleIOHandler(model.getLevel())).convertToOWL(model, out);
			
		} catch (Exception e) {
			throw new RuntimeException("checkAndNormalize(), " +
				"Failed " + title, e);
		}
		
		return validation;
	}

}
