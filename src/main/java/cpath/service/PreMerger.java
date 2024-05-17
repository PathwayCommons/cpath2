package cpath.service;


import cpath.service.api.Service;
import cpath.service.api.Cleaner;
import cpath.service.api.Converter;
import cpath.service.api.RelTypeVocab;
import cpath.service.metadata.Datasource;
import cpath.service.metadata.Mapping;
import cpath.service.metadata.Datasource.METADATA_TYPE;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.normalizer.Namespace;
import org.biopax.paxtools.normalizer.Normalizer;
import org.biopax.paxtools.normalizer.Resolver;
import org.biopax.validator.BiopaxIdentifier;
import org.biopax.validator.api.Validator;
import org.biopax.validator.api.beans.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import java.io.*;


/**
 * Class responsible for pre-merging pathway and warehouse data.
 */
final class PreMerger {

  private static Logger log = LoggerFactory.getLogger(PreMerger.class);

  private final String xmlBase;
  private final Validator validator;

  private Service service;

  /**
   * Constructor.
   * @param service   cpath2 service
   * @param validator BioPAX Validator
   */
  PreMerger(Service service, Validator validator) {
    this.service = service;
    this.validator = validator;
    this.xmlBase = service.settings().getXmlBase();
  }

  /**
   * Pre-process (import, clean, normalize) all data from all configured data sources.
   */
  void premerge() {
    // if this has been run before, there are some intermediate files left
    // in the corresponding output folder (can continue without processing the data from scratch)
    // (one can also manually clean up a particular /data subdirectory to start over)
    for (Datasource datasource : service.metadata().getDatasources())
    {
      final String mid = datasource.getIdentifier();

      if(!Files.isDirectory(Paths.get(service.intermediateDataDir(datasource)))) {
        service.clear(datasource); //actually - create, init...
      } else {
        datasource.getFiles().clear(); //just clear the list of input files
      }

      //read and analyze the input data archive
      log.info("premerge(), processing: " + mid);
      service.unzipData(datasource);
      log.debug("premerge(), " + mid + " contains " + datasource.getFiles().size() + " files");

      if (datasource.getType() == METADATA_TYPE.MAPPING) {
        log.info("premerge(), done for the mapping type data: " + mid);
        continue;
      }

      try {
        // Try to instantiate the Cleaner now, and exit if it fails!
        Cleaner cleaner = null;
        String cl = datasource.getCleanerClass();
        if (cl != null && cl.length() > 0) {
          cleaner = CPathUtils.newCleaner(cl);
          if (cleaner == null) {
            log.error("premerge(), failed to create the Cleaner: " + cl
              + "; skipping for this data source...");
            return; // skip this data entirely due to the error
          }
        } else {
          log.info("premerge(), Cleaner class is not defined for " + mid);
        }

        Converter converter = null;
        cl = datasource.getConverterClass();
        if (cl != null && cl.length() > 0) {
          converter = CPathUtils.newConverter(cl);
          if (converter == null) {
            log.error("premerge(), failed to create the Converter: " + cl
              + "; skipping for this data source...");
            return; // skip due to the error
          }
          converter.setXmlBase(datasource.getIdentifier()+":");
        } else {
          log.info("premerge(), Converter class is not defined for " + mid);
        }

        // Premerge for each pathway data: clean, convert, validate.
        for (String datafile : new HashSet<>(datasource.getFiles())) {
          pipeline(datasource, datafile, cleaner, converter);
        }
      } catch (Exception e) {
        log.error("premerge(), failed for datasource: " + mid, e);
      }
    }
  }

  /**
   * Builds a BioPAX Warehouse model using all available
   * WAREHOUSE type data sources, builds id-mapping tables from
   * MAPPING type data sources, generates extra xrefs, and saves the
   * result model.
   */
  void buildWarehouse() {
    Model warehouse = BioPAXLevel.L3.getDefaultFactory().createModel();
    warehouse.setXmlBase(xmlBase);

    // process "warehouse" type metadata
    for (Datasource datasource : service.metadata().getDatasources()) {
      //skip for not "warehouse" type data
      if (datasource.getType() != METADATA_TYPE.WAREHOUSE) {
        continue;
      }
      log.info("buildWarehouse(), adding data: " + datasource.getIdentifier());
      InputStream inputStream;
      for (String datafile : datasource.getFiles()) {
        try {
          inputStream = new GZIPInputStream(new FileInputStream(CPathUtils.normalizedFile(datafile)));
          Model m = new SimpleIOHandler(BioPAXLevel.L3).convertFromOWL(inputStream);
          m.setXmlBase(xmlBase);
          warehouse.merge(m);
        } catch (IOException e) {
          log.error("buildWarehouse(), skip: failed to load " + CPathUtils.normalizedFile(datafile), e);
        }
      }
    }
    log.info("buildWarehouse(), repairing the model...");
    warehouse.repair();

    //clear all id-mapping tables
    log.warn("buildWarehouse(), removing all previous id-mapping db entries...");
    service.initIndex(null, service.settings().indexDir(), false); //allow writing

    // Using the just built Warehouse BioPAX model, generate the id-mapping tables:
    buildIdMappingFromWarehouse(warehouse);

    // Process all external/custom MAPPING data (also save in the id-mapping repository/index)
    for (Datasource datasource : service.metadata().getDatasources()) {
      //skip not "mapping" data
      if (datasource.getType() != METADATA_TYPE.MAPPING) {
        continue;
      }
      log.info("buildWarehouse(), adding id-mapping: " + datasource.getIdentifier());
      for (String content : datasource.getFiles()) {
        Set<Mapping> mappings;
        try {
          mappings = loadSimpleMapping(content);
        } catch (Exception e) {
          log.error("buildWarehouse(), failed to get id-mapping from: " + content, e);
          continue;
        }
        if(mappings != null) {
          mappings.stream().forEach(m -> service.mapping().save(m));
          service.mapping().commit();
        }
      }
    }
    service.mapping().refresh();

    //remove dangling xrefs (PDB,RefSeq,..) - left after they've been used for creating id-mappings, then unlinked
    Set<BioPAXElement> removed = ModelUtils.removeObjectsIfDangling(warehouse, Xref.class);

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

  /*
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
   */
  private Set<Mapping> loadSimpleMapping(String mappingFile) throws IOException {
    log.info("loadSimpleMapping, loading: " + mappingFile);
    Set<Mapping> mappings = new HashSet<>();
    Scanner scanner = new Scanner(new GZIPInputStream(Files.newInputStream(Paths.get(mappingFile))),
      StandardCharsets.UTF_8.name());
    String line = scanner.nextLine(); //get the first, title line
    String[] head = line.split("\t");
    assert head.length == 2 : "bad header";
    String from = head[0].trim();
    String to = head[1].trim();

    //normalize from/to collection name as bioregistry.io prefix, e.g. 'uniprot', 'pubchem.compound'
    Namespace fns = Resolver.getNamespace(from, true);
    if(fns != null) {
      from = fns.getPrefix();
    }
    Namespace tns = Resolver.getNamespace(to, true);
    if(tns != null) {
      to = tns.getPrefix();
    }

    while (scanner.hasNextLine()) {
      line = scanner.nextLine();
      String[] pair = line.split("\t");

      //if possible, validate IDs and add banana+peel prefixes
      String src = pair[0].trim();
      src = bananaPeelId(fns, src); //null when invalid id
      String tgt = pair[1].trim();
      tgt = bananaPeelId(tns, tgt);

      if(src != null && tgt != null) {
        mappings.add(new Mapping(from, src, to, tgt));
      }
    }

    scanner.close();
    return mappings;
  }

  private String bananaPeelId(Namespace ns, String id) {
    if(ns == null) {
      return id;
    }

    if(!Resolver.checkRegExp(id, ns.getPrefix())) {
      return null;
    }

    String peel = ns.getBanana_peel(); //empty means no banana
    if(!StringUtils.isBlank(peel)) {
      return ns.getBanana() + peel + id;
    }

    return id;
  }

  /*
   * Extracts id-mapping information (name/id -> primary id)
   * from the Warehouse entity references' xrefs to the mapping tables.
   *
   * Currently, we use PR and SMR object types only.
   */
  private void buildIdMappingFromWarehouse(Model warehouse) throws AssertionError {
    log.info("buildIdMappingFromWarehouse(), updating id-mapping tables by analyzing the warehouse data...");

    //Generates Mapping tables:
    //a) ChEBI secondary IDs, PUBCHEM Compound, InChIKey, chem. name - to primary CHEBI AC;
    //b) UniProt secondary IDs, RefSeq, NCBI Gene (number), etc. - to primary UniProt AC.

    // for each ER, using its xrefs, map other IDs to the primary AC
    for(EntityReference er : warehouse.getObjects(EntityReference.class))
    {
      String destDb;
      if(er instanceof ProteinReference)
        destDb = "UNIPROT";
      else if(er instanceof SmallMoleculeReference)
        destDb = "CHEBI";
      else //there are only PR or SMR types of ER in the warehouse model
        throw new AssertionError("Unsupported warehouse ER type: " +
            er.getModelInterface().getSimpleName());

      //extract the primary id from the normalized URI (no db/banana/prefix)
      String ac = CPathUtils.idFromNormalizedUri(er.getUri());

      // There are lots of unification and different type relationship xrefs
      // generated by the uniprot and chebi Converters;
      // we use some of these (already normalized) xrefs to populate our id-mapping repository:
      for(Xref x : new HashSet<>(er.getXref())) {
        if(!(x instanceof PublicationXref)) {
          final String srcDb = x.getDb();
          if(x instanceof UnificationXref) {
            //map to itself; each warehouse ER has only one UX, the primary AC
            //new Mapping args (src and dest db and id) are
            service.mapping().save(new Mapping(srcDb, x.getId(), destDb, ac));
          }
          else if(x instanceof RelationshipXref) {
            // each warehouse RX has relationshipType property defined,
            // and the normalized CV's URI contains the term's ID
            RelationshipTypeVocabulary rtv = (((RelationshipXref) x).getRelationshipType());
            if(rtv.getUri().endsWith(RelTypeVocab.IDENTITY.id)
              || rtv.getUri().endsWith(RelTypeVocab.SECONDARY_ACCESSION_NUMBER.id)
              //other RX types ain't a good idea for id-mapping (has_part,has_role,is_conjugate_*)
            ) {
              service.mapping().save(new Mapping(srcDb, x.getId(), destDb, ac));
            }
            // remove the rel. xref unless secondary/parent ChEBI ID, HGNC Symbol, NCBI Gene ID
            // (id-mapping and search/graph queries do not need these xrefs anymore)
            if( !srcDb.equalsIgnoreCase("hgnc.symbol")
                && !StringUtils.equalsIgnoreCase(srcDb,"ncbigene")
                && !srcDb.equalsIgnoreCase("chebi")
            ) {
              er.removeXref(x);
            }
          }
        }
      }
      service.mapping().commit();
    }
    log.info("buildIdMappingFromWarehouse(), done.");
  }


  /*
   * Given Content undergoes clean/convert/validate/normalize data pipeline.
   *
   * @param datasource about the data provider
   * @param content provider's pathway data (file) to be processed and modified
   * @param cleaner data specific cleaner class (to apply before the validation/normalization)
   * @param converter data specific to BioPAX L3 converter class
   * @throws IOException
   */
  private void pipeline(final Datasource datasource, final String inputDataFile,
                        Cleaner cleaner, Converter converter) throws IOException
  {
    Path originalDataPath = Paths.get(inputDataFile);
    if (datasource.getType() == METADATA_TYPE.MAPPING) {
      log.info("pipeline(), skip for id-mapping data: " + originalDataPath);
      return;
    }

    log.info("pipeline(), process " + originalDataPath);
    File inputFile = originalDataPath.toFile(); // will be a different file at different steps
    final File cleaned = Paths.get(CPathUtils.cleanedFile(inputDataFile)).toFile();
    final File converted = Paths.get(CPathUtils.convertedFile(inputDataFile)).toFile();
    final File normalized = Paths.get(CPathUtils.normalizedFile(inputDataFile)).toFile();

    //an important shortcut
    if(normalized.exists()) {
      log.info("pipeline(), already normalized, done.");
      return;
    }

    // "clean" the data if needed
    if(cleaned.exists() || converted.exists()) {
      log.info("pipeline(), already cleaned");
      inputFile = cleaned;
    } else {
      //Clean the original data (apply data-specific "quick fixes" as needed)
      if (cleaner != null) {
        String cleanerClassName = cleaner.getClass().getSimpleName();
        try {
          InputStream is = new GZIPInputStream(new FileInputStream(inputFile));
          OutputStream os = new GZIPOutputStream(new FileOutputStream(cleaned));
          cleaner.clean(is, os);
          IOUtils.closeQuietly(is);
          IOUtils.closeQuietly(os);
        } catch (Exception e) {
          log.warn("pipeline(), failed to run " + cleanerClassName + "; " + e);
          return;
        }
        inputFile = cleaned;
      }
    }

    // convert to BioPAX Level3 if needed
    if(converted.exists()) {
      log.info("pipeline(), already converted");
      inputFile = converted;
    } else {
      //Convert data to BioPAX L3 if needed (generate the 'converted' output file in any case)
      if (converter != null) {
        String converterClassName = converter.getClass().getSimpleName();
        try {
          InputStream is = new GZIPInputStream(new FileInputStream(inputFile));
          OutputStream os = new GZIPOutputStream(new FileOutputStream(converted));
          converter.convert(is, os);
          IOUtils.closeQuietly(is);
          IOUtils.closeQuietly(os);
        } catch (Exception e) {
          log.warn("pipeline(), failed to run " + converterClassName + "; " + e);
          return;
        }
        inputFile = converted;
      }
    }

    // Validate & normalize the BioPAX model:
    // synonyms in xref.db property values may be replaced
    // with the primary db names (based on Miriam db); some URIs get normalized
    checkAndNormalize(datasource, inputFile);
  }


  /*
   * Validates, fixes, and normalizes given pathway data.
   *
   * @param datasource data provider's datasource
   * @param file one of data files from the provider
   */
  private void checkAndNormalize(Datasource datasource, File file) throws IOException
  {
    final String filename = file.getPath();
    InputStream biopaxStream = new GZIPInputStream(new FileInputStream(file));

    Model model;
    //validate or just normalize
    if(datasource.getType().isNotPathwayData()) { //when "warehouse" or "mapping" data type
      if(datasource.getType() == METADATA_TYPE.MAPPING) { //this should never happen, but let's handle and skip -
        log.info("checkAndNormalize, skipped MAPPING data " + filename);
        return; //skip as checkAndNormalize is not applicable to this datatype
      }
      //just load the model and skip validation
      log.info("checkAndNormalize, loading (no validation) {} {}", datasource.getType(), filename);
      model = new SimpleIOHandler(BioPAXLevel.L3).convertFromOWL(biopaxStream);
      IOUtils.closeQuietly(biopaxStream);
    } else { // validate and normalize the cleaned/converted BioPAX data
      try {
        log.info("checkAndNormalize, validating "	+ filename);
        // create a new empty validation (options: auto-fix=true, report all) and associate with the model
        Validation validation = new Validation(new BiopaxIdentifier(), filename, true, Behavior.WARNING,
          0, null);
        // errors are also reported during the data are being read (e.g., syntax errors)
        validator.importModel(validation, biopaxStream);
        IOUtils.closeQuietly(biopaxStream);

        validator.validate(validation); //check all semantic rules
        // unregister the validation object
        validator.getResults().remove(validation);

        // get the updated model
        model = (Model) validation.getModel();
        // update dataSource property (force new Provenance) for all entities
        datasource.setProvenanceFor(model, xmlBase);

        service.saveValidationReport(validation, CPathUtils.validationFile(filename));

        // count critical not fixed error cases (ignore warnings and fixed ones)
        int noErrors = validation.countErrors(null, null, null, null,
          true, true);
        log.info("checkAndNormalize, summary for {}; critical errors: {}; {}; {}", filename, noErrors,
            validation.getComment().toString(), validation);
      } catch (Exception e) {
        log.error("checkAndNormalize(), failed " + filename + "; " + e);
        return;
      }
    }

    //Normalize URIs, Xrefs, etc.
    log.info("checkAndNormalize, normalizing "	+ filename);
    // init Normalizer
    Normalizer normalizer = new Normalizer();
    //set xml:base to use instead of the original model's one
    //important; the idea is to re-use normalized CVs, xrefs later on instead of duplicating...
    normalizer.setXmlBase(xmlBase);
    normalizer.setFixDisplayName(true); // important
    normalizer.normalize(model); //using bioregistry.io prefix for xref.db values if possible

    // save
    try {
      OutputStream out = new GZIPOutputStream(new FileOutputStream(CPathUtils.normalizedFile(filename)));
      (new SimpleIOHandler(model.getLevel())).convertToOWL(model, out);
    } catch (Exception e) {
      throw new RuntimeException("checkAndNormalize(), failed " + filename, e);
    }
  }
}
