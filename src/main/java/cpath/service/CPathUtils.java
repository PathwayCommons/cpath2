package cpath.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import cpath.service.api.Cleaner;
import cpath.service.api.Converter;
import cpath.service.api.RelTypeVocab;
import cpath.service.metadata.Metadata;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.biopax.paxtools.controller.Fetcher;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.normalizer.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

public final class CPathUtils {
  private static Logger LOGGER = LoggerFactory.getLogger(CPathUtils.class);
  private static final String dataFileSuffixRegex = "[^.]+\\.gz$";

  // LOADER can handle file://, ftp://, http://  resources
  public static final ResourceLoader LOADER = new DefaultResourceLoader();

  private CPathUtils() {
    throw new AssertionError("Not instantiable");
  }

  /**
   * Empties the directory or creates a new one.
   *
   * @param path      path to the directory
   * @param createNew true/false
   */
  static void cleanupDirectory(String path, boolean createNew) {
    Path dir = Paths.get(path);
    try {
      if (Files.exists(dir) && Files.isDirectory(dir)) {
        FileUtils.deleteQuietly(dir.toFile());
      }
      if (createNew)
        Files.createDirectory(dir);
    } catch (IOException e) {
      throw new RuntimeException("Failed to cleanup or create directory: " + path, e);
    }
  }

  static Metadata readMetadata(String url) {
    try {
      return new ObjectMapper().readValue(LOADER.getResource(url).getInputStream(), Metadata.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  static void saveMetadata(Metadata metadata, String path) {
    //path fix-up for metadata location property, e.g., file:metadata.json, classpath:metadata.json (test/demo)
    if(StringUtils.startsWithIgnoreCase(path, "classpath:")) {
      path = StringUtils.replaceIgnoreCase(path, "classpath:", "target/"); //for test/demo
    } else if(StringUtils.startsWithIgnoreCase(path, "file:")) {
      path = StringUtils.replaceIgnoreCase(path, "file:", "");
    } else if (StringUtils.containsIgnoreCase(path, ":")) { //duh... to be safe
      path = StringUtils.substringAfter(path, ":");
    }
    try {
      new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(Files.newOutputStream(Paths.get(path)), metadata);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Replaces the URI of a BioPAX object using java reflection.
   * Please use when absolutely necessary and with great care.
   *
   * @param model model
   * @param el biopax object from the model
   * @param newUri new URI
   */
  public static void replaceUri(Model model, BioPAXElement el, String newUri) {
    if (!el.getUri().equals(newUri)) {
      ModelUtils.updateUri(model, el, newUri);
    }
  }

  /**
   * Smart replace the xml:base in the URI.
   * Mind empty or same xmlBase, standard, or CURIEs like 'uniprot:P12345'.
   * Also, xml:base can be like "http://smpdb.ca/pathways/#" or "https://pantherdb.org/pathways/biopax/P04373#"
   * and absolute URIs like "http://smpdb.ca/pathways/#DNA/1_Mitochondrial_Matrix/Stoichiometry/1.0"
   * (i.e. have xml:base ending with '#' plus '/' after that....)
   *
   * @param absoluteUri - URI of a biopax element
   * @param fromBase    - the URI prefix to replace (can occur in this or other objects); can be null, currentBase or any;
   * @param toBase      - the new xml:base, to use as new prefix where makes sense
   * @return same or updated URI (never null) using the toBase prefix
   */
  static String rebaseUri(String absoluteUri, String fromBase, String toBase) {
    Assert.hasText(absoluteUri, "URI cannot be blank/null");
    toBase = (StringUtils.isBlank(toBase)) ? "" : toBase;
    String uri = absoluteUri;

    if(StringUtils.isBlank(fromBase)) {
      //try to auto-detect and replace a prefix in the URI but skip for standard, normalized, already using toBase ones.
      if(StringUtils.containsAny(absoluteUri, "identifiers.org/", "bioregistry.io/")) {
        //nothing to do here
      } else if (StringUtils.containsNone(absoluteUri, ':', '/', '#')) {
        uri = toBase + absoluteUri;
      } else if (StringUtils.contains(absoluteUri, '#')){
        uri = toBase + StringUtils.substringAfterLast(absoluteUri, "#");
      } else if (StringUtils.contains(absoluteUri, '/')){
        uri = toBase + StringUtils.substringAfterLast(absoluteUri, "/");
      }
    } else {
      // just replace the fromBase prefix, if present, in the URI or return the URI unchanged
      if(StringUtils.startsWith(absoluteUri, fromBase)) {
        uri = StringUtils.replace(absoluteUri, fromBase, toBase);
      }
    }

    return uri; //not null
  }

  /*
   * Replaces xml:base for the normalized model and updates the URis of all non-normalized objects
   * (mostly Entity, Evidence, etc.)
   * The model is already normalized, which means the URIs of many xrefs, CVs, entity reference start with
   * http://bioregistry.io/ or are CURIEs like e.g. chebi:1234, pubmed:1234556.
   */
  public static void rebaseUris(Model model, String fromBase, String toBase) {
    Assert.hasText(toBase, "Blank/null value is not allowed for xmlBase");
    for(BioPAXElement bpe : new HashSet<>(model.getObjects())) {//copy the collection due to CPathUtils.replaceUri modifies the model map
      String currUri = bpe.getUri();
      String uri = CPathUtils.rebaseUri(currUri, fromBase, toBase); //null - prevents replacing for already normalized objects
      //if uri was updated but another object uses the new uri, add the hash to the end
      if(!StringUtils.equals(currUri, uri) && model.getByID(uri) != null) {
        uri = String.format("%s_%s", uri, ModelUtils.md5hex(currUri));
      }
      CPathUtils.replaceUri(model, bpe, uri);
    }
    model.setXmlBase(toBase);
  }

  /**
   * Loads the BioPAX model from a Gzip archive
   * previously created by the same cpath2 instance.
   *
   * @param archive file path
   * @return big BioPAX model
   */
  static Model importFromTheArchive(String archive) {
    Model model = null;

    try {
      LOGGER.info("Loading the BioPAX Model from " + archive);
      model = (new SimpleIOHandler(BioPAXLevel.L3))
          .convertFromOWL(new GZIPInputStream(Files.newInputStream(Paths.get(archive))));
    } catch (IOException e) {
      LOGGER.error("Failed to import model from '" + archive + "' - " + e);
    }

    return model;
  }

  /**
   * Reads from the input and writes to the output stream
   *
   * @param is input
   * @param os output
   * @throws IOException when i/o error
   */
  public static void copy(InputStream is, OutputStream os) throws IOException {
    IOUtils.copy(is, os);
    is.close();
    os.close();
  }

  /**
   * From a normalized ER/CV URI, extract the id (uniprot, chebi,..)
   *
   * @param uri some (preferably normalized) ER or CV URI
   * @return identifier; null when the URI is nothing like *identifiers.org/* or *bioregistry.io/*
   */
  static String idFromNormalizedUri(String uri) {
    if(Stream.of("identifiers.org/", "bioregistry.io/")
        .anyMatch(s -> StringUtils.containsIgnoreCase(uri, s))) {

      String id = uri.substring(uri.lastIndexOf('/') + 1);
      //remove prefix/banana for now
      if(StringUtils.contains(id,":")) {
        id = StringUtils.substringAfter(id, ":");
      }
      //add CID:/SID:/CHEBI: prefix to the id before id-mapping due to our id-mapping/index implementation
      if(StringUtils.containsIgnoreCase(uri,"substance")) //contains 'substance' or 'pubchem...substance'...
        id = "SID:" + id;
      else if(StringUtils.containsIgnoreCase(uri,"compound") || StringUtils.containsIgnoreCase(uri,"pubchem"))
        id = "CID:" + id;
      else if(StringUtils.containsIgnoreCase(uri,"chebi"))
        id = "CHEBI:" + id;

      return id;
    }

    return null;
  }

  /**
   * Auto-fix some ID types before searching or saving in the id-mapping index.
   * This helps to map e.g. a RefSeq version or UniProt isoform ID to the primary UniProt AC,
   * despite our id-mapping index/table does not have records like "NP_12345.1 maps to P01234".
   *
   * @param db type of the identifier (standard resource name, e.g., RefSeq)
   * @param id identifier
   * @return "fixed" ID
   */
  public static String fixIdForMapping(String db, String id) {
    Assert.hasText(id, "fromId is empty");
    Assert.hasText(db, "fromDb is empty");

    db = db.toUpperCase();

    if (db.startsWith("UNIPROT")) {
      //always use UniProt ID instead of the isoform ID for mapping
      if (id.contains("-")) {
        id = id.replaceFirst("-\\d+$", "");
      }
    }
    else if (db.equals("CHEBI")) {
      //by design of this app, chebi id must always have 'CHEBI:' (banana+peel) prefix for id-mapping/indexing/searching
      id = id.toUpperCase(); //converts ChEBI:*, chebi:*, etc. => CHEBI:*
      if (!StringUtils.startsWith(id, "CHEBI:")) {
        id = "CHEBI:" + id;
      }
    }
    else if (db.equals("REFSEQ") && id.contains(".")) {
      //strip, e.g., refseq:NP_012345.2 to refseq:NP_012345
      id = id.replaceFirst("\\.\\d+$", "");
    }
    else if (db.startsWith("KEGG") && id.matches(":\\d+$")) {
      id = id.substring(id.lastIndexOf(':') + 1); //it's a NCBI Gene ID!
    }
    else if (db.contains("PUBCHEM") && (db.contains("SUBSTANCE") || db.contains("SID"))) {
      id = id.toUpperCase(); //ok for a SID
      //add prefix if not present
      if (!id.startsWith("SID:") && id.matches("^\\d+$"))
        id = "SID:" + id;
    }
    else if (db.contains("PUBCHEM") && (db.contains("COMPOUND") || db.contains("CID"))) {
      id = id.toUpperCase(); //ok for a CID
      //add prefix if not present
      if (!id.startsWith("CID:") && id.matches("^\\d+$")) {
        id = "CID:" + id;
      }
    }

    return id;
  }

  /**
   * Whether a string starts with any of the prefixes (case-insensitive).
   *
   * @param str a string
   * @param prefixes optional array of prefix terms to match
   * @return true/false
   */
  public static boolean startsWithAnyIgnoreCase(String str, String... prefixes) {
    return Arrays.stream(prefixes).anyMatch(p -> StringUtils.startsWithIgnoreCase(str, p));
  }

  /**
   * Given relationship type CV 'term' and target biological 'db' and 'id',
   * finds or creates a new relationship xref (and its controlled vocabulary) in the model.
   * <p>
   * Note: the corresponding CV does not have a unification xref
   * (this method won't validate; so, non-standard CV terms can be used).
   *
   * @param vocab relationship xref type
   * @param model a biopax model where to find/add the xref
   */
  public static RelationshipXref findOrCreateRelationshipXref(
    RelTypeVocab vocab, String db, String id, Model model) {
    Assert.notNull(vocab, "vocab is null");

    RelationshipXref toReturn;

    //if chebi, make sure 'CHEBI:' is present
    if(StringUtils.equalsIgnoreCase(db, "chebi") && !StringUtils.startsWithIgnoreCase(id, "chebi:")) {
      id = "CHEBI:" + id;
    }

    String uri = Normalizer.uri(model.getXmlBase(), db, id + "_" + vocab, RelationshipXref.class);
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

  /**
   * Recursively extracts all unification and relationship xrefs from the BioPAX object and its children.
   * @param bpe
   * @return
   */
  static Set<String> getXrefIds(BioPAXElement bpe) {
    final Set<String> ids = new HashSet<>();

    //Can't use multiple threads (spring-data-jpa/hibernate errors occur in production, with filesystem H2 db...)
    //for Entity or ER, also collect IDs from child UX/RXs and map to other IDs (use idMapping)
    final Fetcher fetcher = new Fetcher(SimpleEditorMap.L3, Fetcher.nextStepFilter);
    fetcher.setSkipSubPathways(true);
    //fetch all children of (implicit) type XReferrable, which means - either
    //BioSource or ControlledVocabulary or Evidence or Provenance or Entity or EntityReference
    //(we actually want only the latter two types and their subtypes; will skip the rest later on):
    Set<XReferrable> children = fetcher.fetch(bpe, XReferrable.class);
    //include itself (- for fetcher only gets child elements)
    if (bpe instanceof XReferrable)
      children.add((XReferrable) bpe);

    for (XReferrable child : children) {
      //skip unwanted utility class elements, such as Evidence, CV, Provenance
      if (!(child instanceof Entity || child instanceof EntityReference)) {
        continue;
      }
      // collect standard bio IDs (skip publications);
      // (we will use id-mapping later to associate more IDs)
      for (Xref x : child.getXref()) {
        if (!(x instanceof PublicationXref) && x.getId() != null && x.getDb() != null) {
          String id = x.getId();
          //add 'CHEBI:' ("banana and peel" prefix) if it's missing
          if(StringUtils.equalsIgnoreCase("chebi", x.getDb()) &&
              !StringUtils.startsWithIgnoreCase(id, "chebi:")) {
            id = "CHEBI:" + id;
          }
          ids.add(id);
        }
      }
    }

    return ids;
  }

  static InputStream gzipInputStream(String gzPath) {
    Path path = Paths.get(gzPath);
    try {
      return new GZIPInputStream(Files.newInputStream(path));
    } catch (IOException e) {
      LOGGER.error("Cannot read gzip: " + gzPath, e);
    }
    return null;
  }


  /**
   * For the given converter class name,
   * returns an instance of a class which
   * implements the converter interface.
   *
   * @param converterClassName String
   * @return Converter
   */
  public static Converter newConverter(String converterClassName) {
    return (Converter) newInstance(converterClassName);
  }

  /**
   * For the given cleaner class name,
   * returns an instance of a class which
   * implements the cleaner interface.
   *
   * @param cleanerClassName canonical java class name for the Cleaner implementation
   * @return instance of the class
   */
  static Cleaner newCleaner(String cleanerClassName) {
    return (Cleaner) newInstance(cleanerClassName);
  }

  /*
   * Reflectively creates a new instance of a cpath2
   * supplementary/plugin class (e.g., Cleaner or Converter).
   * This method can create non-public classes as well.
   */
  private static Object newInstance(final String className) {
    try {
      Class<?> clazz = Class.forName(className);
      Constructor<?> c = clazz.getDeclaredConstructor();
      c.setAccessible(true);
      return c.newInstance();
    }
    catch (Exception e) {
      LOGGER.error(("Failed to instantiate " + className), e) ;
    }

    return null;
  }

  /*
   * Generate a sanitized file name for an original source zip entry;
   * this path will be stored in the corresponding Datasource.files collection
   * and then processed during premerge (clean, convert, normalize) and merge steps (ETL).
   */
  static String originalFile(String dataSubDir, String zipEntryName) {
    return Paths.get(dataSubDir,zipEntryName.replaceAll("[^a-zA-Z0-9.-]", "_")
      + ".orig.gz").toString(); //important
  }

  static String normalizedFile(String inputFile) {
    return inputFile.replaceFirst(dataFileSuffixRegex,"normalized.gz");
  }

  static String validationFile(String inputFile) {
    return inputFile.replaceFirst(dataFileSuffixRegex,"issues.gz");
  }

  static String convertedFile(String inputFile) {
    return inputFile.replaceFirst(dataFileSuffixRegex,"converted.gz");
  }

  static String cleanedFile(String inputFile) {
    return inputFile.replaceFirst(dataFileSuffixRegex,"cleaned.gz");
  }

}
