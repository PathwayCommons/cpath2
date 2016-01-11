package cpath.importer;


import cpath.config.CPathSettings;
import cpath.dao.CPathUtils;
import cpath.jpa.Content;
import cpath.jpa.Mapping;
import cpath.jpa.Metadata;
import cpath.jpa.Metadata.METADATA_TYPE;
import cpath.service.CPathService;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.normalizer.Normalizer;
import org.biopax.validator.api.Validator;
import org.biopax.validator.api.beans.*;
import org.biopax.validator.impl.IdentifierImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.util.Assert;

import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import java.io.*;


/**
 * Class responsible for premerging pathway and warehouse data.
 */
public final class PreMerger {

	private static Logger log = LoggerFactory.getLogger(PreMerger.class);

	/**
	 * Values to generate standard BioPAX RelationshipTypeVocabulary objects.
	 */
	public static enum RelTypeVocab {
		IDENTITY("identity", "http://identifiers.org/psimi/MI:0356", "MI", "MI:0356"),
		SECONDARY_ACCESSION_NUMBER("secondary-ac", "http://identifiers.org/psimi/MI:0360", "MI", "MI:0360"),
		ADDITIONAL_INFORMATION("see-also", "http://identifiers.org/psimi/MI:0361", "MI", "MI:0361"),
		//next should work for rel. xrefs pointing to a protein but attached to a Gene, Dna*, Rna* biopax objects
		GENE_PRODUCT("gene product", "http://identifiers.org/psimi/MI:0251", "MI", "MI:0251"),
		SET_MEMBER("set member", "http://identifiers.org/psimi/MI:1341", "MI", "MI:1341"),
		//next one is probably for chebi "is_a" relationships (when parent is a chemical class/concept rather than compound)
		MULTIPLE_PARENT_REFERENCE("multiple parent reference", "http://identifiers.org/psimi/MI:0829", "MI", "MI:0829"),
		ISOFORM_PARENT("isoform-parent", "http://identifiers.org/psimi/MI:0243", "MI", "MI:0243"),;

		public final String term;
		public final String uri;
		public final String db;
		public final String id;

		private RelTypeVocab(String term, String uri, String db, String id) {
			this.term = term;
			this.uri = uri;
			this.db = db;
			this.id = id;
		}

		@Override
		public String toString() {
			return term;
		}
	}


	private CPathService service;

	private final String xmlBase;
	private final Validator validator;

	private String identifier;

	/**
	 * Constructor.
	 *
	 * @param service   cpath2 service (provides data query methods)
	 * @param validator Biopax Validator
	 * @param provider  pathway data provider's identifier
	 */
	public PreMerger(CPathService service, Validator validator, String provider) {
		this.service = service;
		this.validator = validator;
		this.xmlBase = CPathSettings.getInstance().getXmlBase();
		this.identifier = (provider == null || provider.isEmpty())
				? null : provider;
	}

	/**
	 * Pre-process (import, clean, normalize) all data from all configured data sources.
	 */
	public void premerge() {
		// Initially, there are no pathway data files yet,
		// but if premerge was already called, then there're not empty dataFile
		// and result files for the corresp. metadata objects, which will be cleared.
		// Iterate over all metadata:
		for (Metadata metadata : service.metadata().findAll()) {
			// use filter if set (identifier and version)
			if (identifier != null) {
				if (!metadata.getIdentifier().equals(identifier))
					continue;
			}

			try {
				log.info("premerge(), now processing " + metadata.getIdentifier());
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
					log.debug("num. of data files before init, " + metadata.getIdentifier() + ": " + metadata.getContent().size());
					metadata = service.init(metadata);
					metadata.setPremerged(null);
					
					//load/re-pack/save orig. data
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
					log.debug("premerge(), for " + metadata.getIdentifier() + 
						", saved " + metadata.getContent().size() + " files");
				
			} catch (Exception e) {
				log.error("premerge(): failed", e);
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
			for(Content content : metadata.getContent()) {
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
			for(Content content : metadata.getContent()) {
				try {
					Set<Mapping> mappings = loadSimpleMapping(content);
					saveIgnoringDuplicates(mappings);
				} catch (Exception e) {
					log.error("buildWarehouse(), failed to get id-mapping, " +
							"using: " + content.toString(), e);
					continue;
				}
			}
		}
		
		// add more relationship xrefs to the warehouse ERs; 
		// this makes full-text search for ER by some secondary IDs possible
		log.info("buildWarehouse(), adding more Xrefs to ERs using id-mapping...");
		for(EntityReference er : new HashSet<EntityReference>(warehouse.getObjects(EntityReference.class))) 
		{
			Assert.isTrue(er.getUri().contains("/chebi/") || er.getUri().contains("/uniprot/"),
					er + " - warehouse ER is neither PR nor SMR (bug)!");
			addRelXrefsToWarehouseEntityRef(warehouse, er);
		}
		
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


	private void saveIgnoringDuplicates(Set<Mapping> mappings) {
		for(Mapping mapping : mappings) {
			try {
				service.saveIfUnique(mapping);
			} catch (DataIntegrityViolationException e) {} //ignore same entries
		}
	}


	private void addRelXrefsToWarehouseEntityRef(Model warehouse, EntityReference er) 
	{
		final String primaryDb = (er instanceof ProteinReference) ? "UNIPROT" : "CHEBI"; 
		final String primaryId = CPathUtils.idfromNormalizedUri(er.getUri());
		
		//reverse id-mapping (from the primary db/id to all db/id entries that map to the primary pair)
		List<Mapping> map = service.mapping().findByDestIgnoreCaseAndDestId(primaryDb, primaryId);		
		for(Mapping m : map) 
		{
			Assert.isTrue(m.getDest().equals(primaryDb) && m.getDestId().equals(primaryId), 
				"findByDestIgnoreCaseAndDestId result contains mappings with different primary db/id (bug!)");
					
			//find the unif.xref by the normalized URI, if exists
			final String uxUri = Normalizer.uri(xmlBase, m.getSrc(), m.getSrcId(), UnificationXref.class);
			UnificationXref ux = (UnificationXref) warehouse.getByID(uxUri);			
			if(ux != null && er.getXref().contains(ux)) 
				continue; //skip existing equivalet unif. xref

			//otherwise - find/make special rel.xref
			RelationshipXref rx = findOrCreateRelationshipXref(
					RelTypeVocab.IDENTITY, m.getSrc(), m.getSrcId(), warehouse);			
			er.addXref(rx); 
		}
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
		log.info("buildIdMappingFromWarehouse(), updating id-mapping tables by analyzing the warehouse data...");
				
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

			for(Xref x : er.getXref()) {
				// By design (warehouse), there are unif. and rel. xrefs added
				// by the Uniprot Converter, and we will uses those,
				// skipping publication and illegal xrefs:
				if(x.getDb() != null && x.getId() != null
					&&
					(// any unification xref
						(x instanceof UnificationXref)
						|| // or for any typed relationship xref -
						( (x instanceof RelationshipXref)
							  && ((RelationshipXref)x).getRelationshipType()!=null
							  &&
							  (	//use any identity type relationship xref
								((RelationshipXref)x).getRelationshipType().getUri().endsWith(RelTypeVocab.IDENTITY.id)
								|| // or any secondary accession type relationship xref
								((RelationshipXref)x).getRelationshipType().getUri().endsWith(RelTypeVocab.SECONDARY_ACCESSION_NUMBER.id)
							  )
						)
					)
				) {
					mappings.add(new Mapping(x.getDb(), x.getId(), destDb, ac));
				}
			}

		}

		saveIgnoringDuplicates(mappings);
		log.info("buildIdMappingFromWarehouse(), done.");
	}
	

	/**
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
			log.info("pipeline(), cleaning " + info + " with " + cleaner.getClass());
			OutputStream os = new GZIPOutputStream(new FileOutputStream(content.cleanedFile()));
			cleaner.clean(dataStream, os); //os must be closed inside
			//re-assign the input data stream
			dataStream = new GZIPInputStream(new FileInputStream(content.cleanedFile())); 
		}
		
		if(metadata.getType() == METADATA_TYPE.MAPPING) {
			dataStream.close();
			return; //all done about the id-mappingdata (no need to convert/normalize)
		}
		
		//Convert data to BioPAX L3 if needed (generate the 'converted' output file in any case)
		if (converter != null) {
			log.info("pipeline(), converting " + info + " with " + converter.getClass());					
			OutputStream os = new GZIPOutputStream(new FileOutputStream(content.convertedFile()));
			converter.convert(dataStream, os);//os must be closed inside
			dataStream = new GZIPInputStream(new FileInputStream(content.convertedFile())); 		
		}
		
		//here, the 'dataStream' will read either from the orig., cleaned, or converted file,
		//(depending on cleaner/converter availability above)
		
		
		//Go validate and normalize the pathway data
		log.info("pipeline(), validating pathway data "	+ info);

		/* Validate, auto-fix, and normalize (incl. convesion to L3): 
		 * e.g., synonyms in xref.db may be replaced 
		 * with the primary db name, as in Miriam, etc.
		 */
		//use a file instead of String for the RDF/XML data (which can be >2Gb and fail!)
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
		// clearContent Normalizer
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
				if(noErrors > 0)
					content.setValid(false);
				else
					content.setValid(true);
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

	
	/**
	 * Given relationship type CV 'term' and target biological 'db' and 'id', 
	 * finds or creates a new relationship xref (and its controlled vocabulary) in the model.
	 * 
	 * Note: the corresponding CV does not have a unification xref
	 * (this method won't validate; so, non-standard CV terms can be used).
	 * 
	 * @param vocab relationship xref type
	 * @param model a biopax model where to find/add the xref
	 */
	public static RelationshipXref findOrCreateRelationshipXref(
			RelTypeVocab vocab, String db, String id, Model model) 
	{
		Assert.notNull(vocab);
		
		RelationshipXref toReturn = null;

		String uri = Normalizer.uri(model.getXmlBase(), db, id + "_" + vocab.toString(), RelationshipXref.class);
		if (model.containsID(uri)) {
			return (RelationshipXref) model.getByID(uri);
		}

		// create a new relationship xref
		toReturn = model.addNew(RelationshipXref.class, uri);
		toReturn.setDb(db.toLowerCase());
		toReturn.setId(id);

		// create/add the relationship type vocabulary
		String relTypeCvUri = vocab.uri; //identifiers.org standard URI
		RelationshipTypeVocabulary rtv = (RelationshipTypeVocabulary) model.getByID(relTypeCvUri);		
		if (rtv == null) {
			rtv = model.addNew(RelationshipTypeVocabulary.class, relTypeCvUri);
			rtv.addTerm(vocab.term);
			//add the unif.xref
			uri = Normalizer.uri(model.getXmlBase(), vocab.db, vocab.id, UnificationXref.class);
			UnificationXref rtvux = (UnificationXref) model.getByID(uri);
			if (rtvux == null) {
				rtvux = model.addNew(UnificationXref.class, uri);
				rtvux.setDb(vocab.db.toLowerCase());
				rtvux.setId(vocab.id);
			}	
			rtv.addXref(rtvux);
		}
		toReturn.setRelationshipType(rtv);

		return toReturn;
	}
}
