package cpath.service;


import cpath.service.api.CPathService;
import cpath.service.api.Cleaner;
import cpath.service.api.Converter;
import cpath.service.api.RelTypeVocab;
import cpath.service.jpa.Content;
import cpath.service.jpa.Mapping;
import cpath.service.jpa.Metadata;
import cpath.service.jpa.Metadata.METADATA_TYPE;

import org.apache.commons.io.IOUtils;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.normalizer.Normalizer;
import org.biopax.validator.BiopaxIdentifier;
import org.biopax.validator.api.Validator;
import org.biopax.validator.api.beans.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
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
  private final boolean overwrite;

  private CPathService service;

  /**
   * Constructor.
   * @param service   cpath2 service (provides data query methods)
   * @param validator Biopax Validator
   * @param overwrite whether to re-input content files or continue (default: false).
   */
  public PreMerger(CPathService service, Validator validator, boolean overwrite) {
    this.service = service;
    this.validator = validator;
    this.xmlBase = service.settings().getXmlBase();
    this.overwrite = overwrite;
  }

  /**
   * Pre-process (import, clean, normalize) all data from all configured data sources.
   */
  public void premerge() {
    // if this has been run before, there're some output files left
    // in the corresponding output folder, which will stay unless overwrite==true
    // (we'd continue instead of re-doing all input data; can also cleanup a sub-directory under /data manually).

    // Iterate over all metadata
    for (Metadata metadata : service.metadata().findAll())
    {
      if(overwrite || !Files.isDirectory(Paths.get(service.outputDir(metadata)))) {
        service.clear(metadata); //empties the corresponding directory and db entries
      } else {
        //just clear the list of input files (content of the archive)
        metadata.getContent().clear();
      }

      //read and analyze the input data archive
      log.info("premerge(), " + metadata.getIdentifier());
      service.buildContent(metadata);
      metadata = service.metadata().save(metadata); //inserts content file names into the db table
      log.debug("premerge(), " + metadata.getIdentifier() + " contains "
        + metadata.getContent().size() + " files");

      try {
        log.info("premerge(), processing " + metadata.getIdentifier());
        // Try to instantiate the Cleaner now, and exit if it fails!
        Cleaner cleaner = null; //reset to null!
        String cl = metadata.getCleanerClassname();
        if (cl != null && cl.length() > 0) {
          cleaner = CPathUtils.newCleaner(cl);
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
          converter = CPathUtils.newConverter(cl);
          if (converter == null) {
            log.error("premerge(), failed to create the Converter: " + cl
              + "; skipping for this data source...");
            return; // skip due to the error
          }
          converter.setXmlBase(xmlBase);
        } else {
          log.info("premerge(), no Converter class was specified; continue...");
        }

        // Premerge for each pathway data: clean, convert, validate.
        for (Content content : new HashSet<>(metadata.getContent())) {
          pipeline(metadata, content, cleaner, converter);
        }

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

      log.info("buildWarehouse(), adding data: " + metadata.getIdentifier());
      InputStream inputStream;
      for (Content content : metadata.getContent()) {
        try {
          inputStream = new GZIPInputStream(new FileInputStream(service.normalizedFile(content)));
          Model m = new SimpleIOHandler(BioPAXLevel.L3).convertFromOWL(inputStream);
          m.setXmlBase(xmlBase);
          warehouse.merge(m);
        } catch (IOException e) {
          log.error("buildWarehouse(), skip for " + content.toString() +
            "; failed to read/merge from " + service.convertedFile(content), e);
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

      log.info("buildWarehouse(), adding id-mapping: " + metadata.getIdentifier());
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
          service.mapping().saveAll(mappings);
      }
    }

    //remove dangling xrefs (PDB,RefSeq,..) - left after they've been used for creating id-mappings, then unlinked
    ModelUtils.removeObjectsIfDangling(warehouse, Xref.class);

    // save to compressed file
    String whFile = service.settings().warehouseModelFile();
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
  Set<Mapping> loadSimpleMapping(Content content) throws IOException
  {
    Set<Mapping> mappings = new HashSet<>();

    Scanner scaner = new Scanner(new GZIPInputStream(new FileInputStream(service.originalFile(content))));

    String line = scaner.nextLine(); //get the first, title line
    String head[] = line.split("\t");
    assert head.length == 2 : "bad header";
    String from = head[0].trim();
    String to = head[1].trim();
    while (scaner.hasNextLine()) {
      line = scaner.nextLine();
      String pair[] = line.split("\t");
      String srcId = pair[0].trim();
      String tgtId = pair[1].trim();
      mappings.add(new Mapping(from, srcId, to, tgtId));
    }

    scaner.close();

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
    final Set<Mapping> mappings = new HashSet<>();

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
      for(Xref x : new HashSet<>(er.getXref())) {
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
    service.mapping().saveAll(mappings);

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
    File inputFile = new File(service.originalFile(content));
    log.info("pipeline(), do " + inputFile.getPath());

    //Clean the data, i.e., apply data-specific "quick fixes".
    if(cleaner != null) {
      String cleanerClassName = cleaner.getClass().getSimpleName();
      File outputFile = new File(service.cleanedFile(content));
      if(outputFile.exists()) {
        log.info("pipeline(), re-use " + outputFile.getName());
      } else {
        try {
          InputStream is = new GZIPInputStream(new FileInputStream(inputFile));
          OutputStream os = new GZIPOutputStream(new FileOutputStream(outputFile));
          cleaner.clean(is, os);
          IOUtils.closeQuietly(is);
          IOUtils.closeQuietly(os);
        } catch (Exception e) {
          log.warn("pipeline(), fail " + info + " due to " + cleanerClassName + " failed: " + e);
          return;
        }
        log.info("pipeline(), " + cleanerClassName + " produced " + outputFile.getName());
      }
      inputFile = outputFile;
    }

    if(metadata.getType() == METADATA_TYPE.MAPPING) {
      return; //for id-mapping data - no need to convert, normalize
    }

    //Convert data to BioPAX L3 if needed (generate the 'converted' output file in any case)
    if (converter != null) {
      String converterClassName = converter.getClass().getSimpleName();
      File outputFile = new File(service.convertedFile(content));
      if(outputFile.exists()) {
        log.info("pipeline(), re-use " + outputFile.getName());
      } else {
        try {
          InputStream is = new GZIPInputStream(new FileInputStream(inputFile));
          OutputStream os = new GZIPOutputStream(new FileOutputStream(outputFile));
          converter.convert(is, os);
          IOUtils.closeQuietly(is);
          IOUtils.closeQuietly(os);
        } catch (Exception e) {
          log.warn("pipeline(), fail " + info + " due to " + converterClassName + " failed: " + e);
          return;
        }
        log.info("pipeline(), " + converterClassName + " produced " + outputFile.getName());
      }
      inputFile = outputFile;
    }

    // Validate & auto-fix and normalize: e.g., synonyms in xref.db may be replaced
    // with the primary db name, as in Miriam, some URIs get normalized, etc.
    if(Files.exists(Paths.get(service.normalizedFile(content)))) {
      log.warn("checkAndNormalize, skip validation/normalization - use existing data files.");
    } else {
      InputStream is = new GZIPInputStream(new FileInputStream(inputFile));
      checkAndNormalize(info, is, metadata, content);
      IOUtils.closeQuietly(is);
    }
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
    } else { //validate/normalize cleaned, converted biopax data
      try {
        log.info("checkAndNormalize, validating "	+ title);
        // create a new empty validation (options: auto-fix=true, report all) and associate with the model
        Validation validation = new Validation(new BiopaxIdentifier(), title, true, Behavior.WARNING, 0, null);
        // errors are also reported during the data are being read (e.g., syntax errors)
        validator.importModel(validation, biopaxStream);
        validator.validate(validation); //check all semantic rules
        // unregister the validation object
        validator.getResults().remove(validation);

        // get the updated model
        model = (Model) validation.getModel();
        // update dataSource property (force new Provenance) for all entities
        metadata.setProvenanceFor(model);

        service.saveValidationReport(content, validation);

        // count critical not fixed error cases (ignore warnings and fixed ones)
        int noErrors = validation.countErrors(null, null, null, null, true, true);
        log.info("pipeline(), summary for " + title + ". Critical errors found:" + noErrors + ". "
          + validation.getComment().toString() + "; " + validation.toString());

      } catch (Exception e) {
        log.error("checkAndNormalize(), failed " + title + "; " + e);
        return;
      }
    }

    //Normalize URIs, etc.
    log.info("checkAndNormalize, normalizing "	+ title);
    normalizer.normalize(model);

    // save
    try {
      OutputStream out = new GZIPOutputStream(new FileOutputStream(service.normalizedFile(content)));
      (new SimpleIOHandler(model.getLevel())).convertToOWL(model, out);
    } catch (Exception e) {
      throw new RuntimeException("checkAndNormalize(), failed " + title, e);
    }
  }
}
