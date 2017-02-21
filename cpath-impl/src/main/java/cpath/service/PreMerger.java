package cpath.service;


import cpath.config.CPathSettings;
import cpath.jpa.Content;
import cpath.jpa.Mapping;
import cpath.jpa.Metadata;
import cpath.jpa.Metadata.METADATA_TYPE;

import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.normalizer.Normalizer;
import org.biopax.validator.api.Validator;
import org.biopax.validator.api.beans.*;
import org.biopax.validator.impl.IdentifierImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import java.io.*;


/**
 * Class responsible for premerging pathway and warehouse data.
 */
public final class PreMerger {

	private static Logger log = LoggerFactory.getLogger(PreMerger.class);

	private final String xmlBase;
	private final Validator validator;

	private CPathService service;

	/**
	 * Constructor.
	 *
	 * @param service   cpath2 service (provides data query methods)
	 * @param validator Biopax Validator
	 */
	public PreMerger(CPathService service, Validator validator) {
		this.service = service;
		this.validator = validator;
		this.xmlBase = CPathSettings.getInstance().getXmlBase();
	}

	/**
	 * Pre-process (import, clean, normalize) all data from all configured data sources.
	 */
	public void premerge() {
		// If premerge was previously run, there're some output files
		// in the corresponding data sub-folder, which will stay untouched ( if there was a problem with a data source,
		// we'd like to re-run to continue premerging instead of doing all over and over again for all data;
		// you can cleanup a sub-directory under /data manually if re-doing is required due to previous errors/failure).

		// Iterate over all metadata:
		for (Metadata metadata : service.metadata().findAll())
		{
			final File dir = new File(metadata.outputDir());
			if(dir.isDirectory() && dir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.getName().endsWith("original");
				}
			}).length>0) {
				log.warn("premerge(), found " + metadata.outputDir() + " folder; skip previously premerged "
						+ metadata.getIdentifier());
				continue; //skip
			}

			try {
				log.info("premerge(), processing " + metadata.getIdentifier());
				// Try to instantiate the Cleaner now, and exit if it fails!
				Cleaner cleaner = null; //reset to null!
				String cl = metadata.getCleanerClassname();
				if (cl != null && cl.length() > 0) {
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
				if (cl != null && cl.length() > 0) {
					converter = ImportFactory.newConverter(cl);
					if (converter == null) {
						log.error("premerge(), failed to create the Converter: " + cl
								+ "; skipping for this data source...");
						return; // skip due to the error
					}
					converter.setXmlBase(xmlBase);
				} else {
					log.info("premerge(), no Converter class was specified; continue...");
				}

				// clear
				metadata = service.clear(metadata);

				//expand/re-pack/save or overwrite the original data files and create/recover Content db table rows
				log.info("premerge(), " + metadata.getIdentifier() + ", expanding data files to " + metadata.outputDir());
				CPathUtils.analyzeAndOrganizeContent(metadata);

				// Premerge for each pathway data: clean, convert, validate,
				// and then update premergeData, validationResults db fields.
				for (Content content : new HashSet<Content>(metadata.getContent())) {
					try {
						pipeline(metadata, content, cleaner, converter);
					} catch (Exception e) {
						metadata.getContent().remove(content);
						log.warn("premerge(), removed " + content + " due to error", e);
					}
				}

				// save/update validation status
				metadata = service.save(metadata);
				log.debug("premerge(), " + metadata.getIdentifier() + ": saved "
						+ metadata.getContent().size() + " files");

			} catch (Exception e) {
				log.error("premerge(), failed to do " + metadata.getIdentifier(), e);
			}
		}
	}

	/**
	 * Builds a BioPAX Warehouse model using all available
	 * WAREHOUSE type data sources, builds id-mapping tables from
	 * MAPPING type data sources, generates extra xrefs, and saves the
	 * result model.
	 */
	public void buildWarehouse() {

		Model warehouse = BioPAXLevel.L3.getDefaultFactory().createModel();
		warehouse.setXmlBase(xmlBase);

		// iterate over all metadata
		for (Metadata metadata : service.metadata().findAll()) {
			//skip for not warehouse data
			if (metadata.getType() != METADATA_TYPE.WAREHOUSE)
				continue;

			log.info("buildWarehouse(), adding data: " + metadata.getUri());
			InputStream inputStream;
			for (Content content : metadata.getContent()) {
				try {
					inputStream = new GZIPInputStream(new FileInputStream(content.normalizedFile()));
					Model m = new SimpleIOHandler(BioPAXLevel.L3).convertFromOWL(inputStream);
					m.setXmlBase(xmlBase);
					warehouse.merge(m);
				} catch (IOException e) {
					log.error("buildWarehouse(), skip for " + content.toString() +
							"; failed to read/merge from " + content.convertedFile(), e);
					continue;
				}
			}
		}
		log.info("buildWarehouse(), repairing the model...");
		warehouse.repair();

		//clear all id-mapping tables
		log.warn("buildWarehouse(), removing all previous id-mapping db entries...");
		service.mapping().deleteAll();

		// Using the just built Warehouse BioPAX model, generate the id-mapping tables:
		buildIdMappingFromWarehouse(warehouse);

		// Next, process all extra MAPPING data files, build, save in the id-mapping db repository.
		for (Metadata metadata : service.metadata().findAll()) {
			//skip not id-mapping data
			if (metadata.getType() != METADATA_TYPE.MAPPING)
				continue;

			log.info("buildWarehouse(), adding id-mapping: " + metadata.getUri());
			for (Content content : metadata.getContent()) {
				Set<Mapping> mappings = null;
				try {
					mappings = loadSimpleMapping(content);
				} catch (Exception e) {
					log.error("buildWarehouse(), failed to get id-mapping, " +
							"using: " + content.toString(), e);
					continue;
				}
				if(mappings != null) //i.e., when no exception was thrown above
					service.mapping().save(mappings);
			}
		}

		//remove dangling xrefs (PDB,RefSeq,..) - left after they've been used for creating id-mappings, then unlinked
		ModelUtils.removeObjectsIfDangling(warehouse, Xref.class);

		// save to compressed file
		String whFile = CPathSettings.getInstance().warehouseModelFile();
		log.info("buildWarehouse(), creating Warehouse BioPAX archive: " + whFile);
		try {
			new SimpleIOHandler(BioPAXLevel.L3).convertToOWL(warehouse,
					new GZIPOutputStream(new FileOutputStream(whFile)));
		} catch (IOException e) {
			log.error("buildWarehouse(), failed", e);
		}

		//Don't persist (do later after Merger)
		log.info("buildWarehouse(), done.");
	}


	/**
	 * Creates mapping objects
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
	 * @return
	 * @throws IOException 
	 */
	Set<Mapping> loadSimpleMapping(Content content) throws IOException {

		Set<Mapping> mappings = new HashSet<Mapping>();
		
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(new GZIPInputStream(new FileInputStream(content.originalFile()))));

		String line = reader.readLine(); //get the first, title line
		String head[] = line.split("\t");
		assert head.length == 2 : "bad header";
		String from = head[0].trim();
		String to = head[1].trim();
		while ((line = reader.readLine()) != null) {
			String pair[] = line.split("\t");
			String srcId = pair[0].trim();
			String tgtId = pair[1].trim();
			mappings.add(new Mapping(from, srcId, to, tgtId));
		}
		reader.close();
		
		return mappings;
	}


	/*
	 * Extracts id-mapping information (name/id -> primary id) 
	 * from the Warehouse entity references's xrefs to the mapping tables.
	 */
	private void buildIdMappingFromWarehouse(Model warehouse) {		
		log.info("buildIdMappingFromWarehouse(), updating id-mapping " +
				"tables by analyzing the warehouse data...");
				
		//Generates Mapping tables (objects) using ERs:
		//a. ChEBI secondary IDs, PUBCHEM Compound, InChIKey, chem. name - to primary CHEBI AC;
		//b. UniProt secondary IDs, RefSeq, NCBI Gene, etc. - to primary UniProt AC.
		final Set<Mapping> mappings = new HashSet<Mapping>();
		
		// for each ER, using its xrefs, map other identifiers to the primary accession
		for(EntityReference er : warehouse.getObjects(EntityReference.class)) 
		{	
			String destDb = null;
			if(er instanceof ProteinReference)
				destDb = "UNIPROT";
			else if(er instanceof SmallMoleculeReference)
				destDb = "CHEBI";
			else //there're only PR or SMR types of ER in the warehouse model
				throw new AssertionError("Unsupported warehouse ER type: " + er.getModelInterface().getSimpleName());
			
			//extract the primary id from the standard (identifiers.org) URI
			final String ac = CPathUtils.idfromNormalizedUri(er.getUri());

			// There are lots of unification and different type relationship xrefs
			// generated by the the uniprot and  chebi Converters;
			// we use some of these xrefs to populate our id-mapping repository:
			for(Xref x : new HashSet<Xref>(er.getXref())) {
				if(!(x instanceof PublicationXref)) {
					final String src = x.getDb().toUpperCase();
					if(x instanceof UnificationXref) {
						//map to itself; each warehouse ER has only one UX, the primary AC
						mappings.add(new Mapping(src, x.getId(), destDb, ac));
					}
					else if(x instanceof RelationshipXref) {
						// each warehouse RX has relationshipType property defined,
						// and the normalized CV's URI contains the term's ID
						RelationshipTypeVocabulary rtv = (((RelationshipXref) x).getRelationshipType());
						if(rtv.getUri().endsWith(RelTypeVocab.IDENTITY.id)
						  	|| rtv.getUri().endsWith(RelTypeVocab.SECONDARY_ACCESSION_NUMBER.id)
						//other RX types ain't a good idea for id-mapping (has_part,has_role,is_conjugate_*)
						) {
							mappings.add(new Mapping(src, x.getId(), destDb, ac));
						}
						// remove the rel. xref unless it's the secondary/parent ChEBI ID, 'HGNC Symbol'
						// (id-mapping and search/graph queries do not need these xrefs anymore)
						if(!src.equalsIgnoreCase("HGNC Symbol") && !src.startsWith("NCBI Gene")
								&& !src.equalsIgnoreCase("CHEBI")) {
							er.removeXref(x);
						}
					}
				}
			}
		}

		//save/update to the id-mapping database
		log.info("buildIdMappingFromWarehouse(), saving all...");
		service.mapping().save(mappings);

		log.info("buildIdMappingFromWarehouse(), done.");
	}
	

	/*
	 * Given Content undergoes clean/convert/validate/normalize data pipeline.
	 * 
	 * @param metadata about the data provider
	 * @param content provider's pathway data (file) to be processed and modified
	 * @param cleaner data specific cleaner class (to apply before the validation/normalization)
	 * @param converter data specific to BioPAX L3 converter class
	 * @throws IOException 
	 */
	private void pipeline(final Metadata metadata, final Content content, 
			Cleaner cleaner, Converter converter) throws IOException 
	{	
		final String info = content.toString();
		
		InputStream dataStream = new GZIPInputStream(new FileInputStream(content.originalFile()));
		
		//Clean the data, i.e., apply data-specific "quick fixes".
		if(cleaner != null) {
			if((new File(content.cleanedFile())).exists())
				log.info("pipeline(), skip existing " + content.cleanedFile());
			else {
				log.info("pipeline(), cleaning " + info + " with " + cleaner.getClass());
				OutputStream os = new GZIPOutputStream(new FileOutputStream(content.cleanedFile()));
				cleaner.clean(dataStream, os); //os must be closed inside
			}
				//re-assign the input data stream
				dataStream = new GZIPInputStream(new FileInputStream(content.cleanedFile()));
		}
		
		if(metadata.getType() == METADATA_TYPE.MAPPING) {
			dataStream.close();
			return; //for id-mapping data - no need to convert, normalize
		}
		
		//Convert data to BioPAX L3 if needed (generate the 'converted' output file in any case)
		if (converter != null) {
			if((new File(content.convertedFile())).exists())
				log.info("pipeline(), skip existing " + content.convertedFile());
			else {
				log.info("pipeline(), converting " + info + " with " + converter.getClass());
				OutputStream os = new GZIPOutputStream(new FileOutputStream(content.convertedFile()));
				converter.convert(dataStream, os);//os must be closed inside
			}
			dataStream = new GZIPInputStream(new FileInputStream(content.convertedFile())); 		
		}
		// here 'dataStream' is either orig., cleaned, or converted data file
		// (depending on cleaner/converter availability above).

		// Validate & auto-fix and normalize: e.g., synonyms in xref.db may be replaced
		// with the primary db name, as in Miriam, some URIs get normalized, etc.
		if((new File(content.normalizedFile())).exists())
			log.info("pipeline(), skip existing " + content.normalizedFile());
		else
			checkAndNormalize(info, dataStream, metadata, content);
	}

	
	/*
	 * Validates, fixes, and normalizes given pathway data.
	 *
	 * @param title short description
	 * @param biopaxStream BioPAX OWL stream
	 * @param metadata data provider's metadata
	 * @param content current chunk of data from the data source
	 */
	private void checkAndNormalize(String title, InputStream biopaxStream, Metadata metadata, Content content)
	{
		// init Normalizer
		Normalizer normalizer = new Normalizer();
		//set xml:base to use instead of the original model's one (important!)
		normalizer.setXmlBase(xmlBase);
		normalizer.setFixDisplayName(true); // important
		normalizer.setDescription(title);

		Model model = null;
		//validate or just normalize
		if(metadata.getType() == METADATA_TYPE.MAPPING) {
			throw new IllegalArgumentException("checkAndNormalize, unsupported Metadata type (MAPPING)");
		} else if(metadata.isNotPathwayData()) { //that's Warehouse data
			//get the cleaned/converted model; skip validation
			model = new SimpleIOHandler(BioPAXLevel.L3).convertFromOWL(biopaxStream);
//			content.setValid(true);
		} else { //validate/normalize pathway data (cleaned, converted biopax data)
			try {
				log.info("checkAndNormalize, validating "	+ title);
				// create a new empty validation (options: auto-fix=true, report all) and associate with the model
				Validation validation = new Validation(new IdentifierImpl(), title, true, Behavior.WARNING, 0, null);
				// errors are also reported during the data are being read (e.g., syntax errors)
				validator.importModel(validation, biopaxStream);
				validator.validate(validation); //check all semantic rules
				// unregister the validation object
				validator.getResults().remove(validation);

				// get the updated model
				model = (Model) validation.getModel();
				// update dataSource property (force new Provenance) for all entities
				metadata.setProvenanceFor(model);

				content.saveValidationReport(validation);

				// count critical not fixed error cases (ignore warnings and fixed ones)
				int noErrors = validation.countErrors(null, null, null, null, true, true);
				log.info("pipeline(), summary for " + title + ". Critical errors found:" + noErrors + ". "
					+ validation.getComment().toString() + "; " + validation.toString());

			} catch (Exception e) {
				throw new RuntimeException("checkAndNormalize(), failed " + title, e);
			}
		}

		//Normalize URIs, etc.
		log.info("checkAndNormalize, normalizing "	+ title);
		normalizer.normalize(model);

		// save
		try {
			OutputStream out = new GZIPOutputStream(new FileOutputStream(content.normalizedFile()));
			(new SimpleIOHandler(model.getLevel())).convertToOWL(model, out);
		} catch (Exception e) {
			throw new RuntimeException("checkAndNormalize(), failed " + title, e);
		}
	}
}
