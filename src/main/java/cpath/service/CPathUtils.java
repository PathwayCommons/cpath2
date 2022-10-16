package cpath.service;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.*;

import cpath.service.api.Cleaner;
import cpath.service.api.Converter;
import cpath.service.api.RelTypeVocab;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.biopax.paxtools.controller.Fetcher;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.impl.BioPAXElementImpl;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.normalizer.Normalizer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

import cpath.service.jpa.Metadata;

public final class CPathUtils {
  private static Logger LOGGER = LoggerFactory.getLogger(CPathUtils.class);
  private static final String dataFileSuffixRegex = "[^.]+\\.gz$";

  // LOADER can handle file://, ftp://, http://  PROVIDER_URL resources
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

  /**
   * For the given url, returns a collection of Metadata Objects.
   *
   * @param url String
   * @return Collection<Metadata>
   */
  static Collection<Metadata> readMetadata(final String url) {
    // order of lines/records in the Metadata table does matter (since 2013/03);
    // so List is used here instead of HashSet
    List<Metadata> toReturn = new ArrayList<>();

    // check args
    if (url == null) {
      throw new IllegalArgumentException("url must not be null");
    }

    // get data from service
    try {
      JSONObject jo = (JSONObject) new JSONParser().parse(new InputStreamReader(LOADER.getResource(url).getInputStream()));
      for (JSONObject ds : (Iterable<JSONObject>) jo.get("datasources")) {
        Metadata.METADATA_TYPE type = Metadata.METADATA_TYPE.valueOf((String) ds.get("type"));
        List<String> names = (List<String>) ((JSONArray) ds.get("name")).stream().collect(Collectors.toList());
        Metadata metadata = new Metadata((String) ds.get("identifier"), names, (String) ds.get("description"),
          (String) ds.get("dataUrl"), (String) ds.get("homepageUrl"), (String) ds.get("iconUrl"),
          type, (String) ds.get("cleanerClass"), (String) ds.get("converterClass"),
          (String) ds.get("pubmedId"), (String) ds.get("availability"));
        LOGGER.info("readMetadata(): adding Metadata: " + metadata.getIdentifier());
        toReturn.add(metadata);
      }
    } catch (ParseException | IOException e) {
      throw new RuntimeException(e);
    }

    return toReturn;
  }

  /**
   * Replaces the URI of a BioPAX object
   * using java reflection. Normally, one should avoid this;
   * please use when absolutely necessary and with great care.
   * @param bpe biopax object from the model
   * @param model model
   * @param xmlBase xml:base for the new URI to be generated
   */
  static void changeUri(BioPAXElement bpe, Model model, String xmlBase) {
    replaceUri(model, bpe, Normalizer
      .uri(xmlBase, null, UUID.randomUUID() + bpe.getUri(), bpe.getModelInterface()));
  }

  /**
   * Replaces the URI of a BioPAX object
   * using java reflection. Normally, one should avoid this;
   * please use when absolutely necessary and with great care.
   *
   * @param model model
   * @param el biopax object from the model
   * @param newUri new URI
   */
  public static void replaceUri(Model model, BioPAXElement el, String newUri) {
    if (el.getUri().equals(newUri))
      return; // no action required
    model.remove(el);
    try {
      Method m = BioPAXElementImpl.class.getDeclaredMethod("setUri", String.class);
      m.setAccessible(true);
      m.invoke(el, newUri);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    model.add(el);
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
   * For a warehouse (normalized) EntityReference's or CV's URI
   * gets the corresponding identifier (e.g., UniProt or ChEBI primary ID).
   * (this depends on current biopax normalizer and cpath2 premerge
   * that make/consume 'http://identifiers.org/*' URIs for those utility class biopax objects.)
   *
   * @param uri URI
   * @return local part URI - ID
   */
  static String idFromNormalizedUri(String uri) {
    Assert.isTrue(uri.contains("http://identifiers.org/"),"Not a Identifiers.org URI");
    return uri.substring(uri.lastIndexOf('/') + 1);
  }

  /**
   * Auto-fix an ID of particular type before using it
   * for id-mapping. This helps mapping e.g., RefSeq versions ID and
   * UniProt isoforms to primary UniProt accessions despite our id-mapping db
   * does not have such records as e.g. "NP_12345.1 maps to P01234".
   *
   * @param fromDb type of the identifier (standard resource name, e.g., RefSeq)
   * @param fromId identifier
   * @return "fixed" ID
   */
  public static String fixSourceIdForMapping(String fromDb, String fromId) {
    Assert.hasText(fromId, "fromId is empty");
    Assert.hasText(fromDb, "fromDb is empty");

    String id = fromId;
    String db = fromDb.toUpperCase();

    if (db.startsWith("UNIPROT") || db.contains("SWISSPROT") || db.contains("TREMBL")) {
      //always use UniProt ID instead of the isoform ID for mapping
      if (id.contains("-"))
        id = id.replaceFirst("-\\d+$", "");
    } else if (db.equals("REFSEQ") && id.contains(".")) {
      //strip, e.g., refseq:NP_012345.2 to refseq:NP_012345
      id = id.replaceFirst("\\.\\d+$", "");
    } else if (db.startsWith("KEGG") && id.matches(":\\d+$")) {
      id = id.substring(id.lastIndexOf(':') + 1); //it's NCBI Gene ID;
    } else if (db.contains("PUBCHEM") && (db.contains("SUBSTANCE") || db.contains("SID"))) {
      id = id.toUpperCase(); //ok for a SID
      //add prefix if not present
      if (!id.startsWith("SID:") && id.matches("^\\d+$"))
        id = "SID:" + id;
    } else if (db.contains("PUBCHEM") && (db.contains("COMPOUND") || db.contains("CID"))) {
      id = id.toUpperCase(); //ok for a CID
      //add prefix if not present
      if (!id.startsWith("CID:") && id.matches("^\\d+$"))
        id = "CID:" + id;
    }

    return id;
  }

  /**
   * Whether a string starts with any of the prefixes (case insensitive).
   *
   * @param s a string
   * @param prefixes optional array of prefix terms to match
   * @return true/false
   */
  public static boolean startsWithAnyIgnoreCase(String s, String... prefixes) {
    for (String prefix : prefixes) {
      if (StringUtils.startsWithIgnoreCase(s, prefix)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Given relationship type CV 'term' and target biological 'db' and 'id',
   * finds or creates a new relationship xref (and its controlled vocabulary) in the model.
   * <p>
   * Note: the corresponding CV does not have a unification xref
   * (this method won't validate; so, non-standard CV terms can be used).
   *
   * @param vocab       relationship xref type
   * @param model       a biopax model where to find/add the xref
   * @param isPrimaryId whether it's a primary ID/AC (then adds a comment)
   */
  public static RelationshipXref findOrCreateRelationshipXref(
    RelTypeVocab vocab, String db, String id, Model model, boolean isPrimaryId) {
    Assert.notNull(vocab, "vocab is null");

    RelationshipXref toReturn;

    String uri = Normalizer.uri(model.getXmlBase(), db, id + "_" + vocab.toString(), RelationshipXref.class);
    if (model.containsID(uri)) {
      return (RelationshipXref) model.getByID(uri);
    }

    // create a new relationship xref
    toReturn = model.addNew(RelationshipXref.class, uri);
    toReturn.setDb(db.toLowerCase());
    toReturn.setId(id);
    if (isPrimaryId)
      toReturn.addComment("PRIMARY");

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

  static Set<String> getXrefIds(BioPAXElement bpe) {
    final Set<String> ids = new HashSet<>();

    //Can't use multiple threads (spring-data-jpa/hibernate errors occur in production, with filesystem H2 db...)
    //for Entity or ER, also collect IDs from child UX/RXs and map to other IDs (use idMapping)
    final Fetcher fetcher = new Fetcher(SimpleEditorMap.L3, Fetcher.nextStepFilter);
    fetcher.setSkipSubPathways(true);
    //fetch all children of (implicit) type XReferrable, which means - either
    //BioSource or ControlledVocabulary or Evidence or Provenance or Entity or EntityReference
    //(we actually want only the latter two types and their sub-types; will skip the rest later on):
    Set<XReferrable> children = fetcher.fetch(bpe, XReferrable.class);
    //include itself (- for fetcher only gets child elements)
    if (bpe instanceof XReferrable)
      children.add((XReferrable) bpe);

    for (XReferrable child : children) {
      //skip for unwanted utility class child elements, such as Evidence,CV,Provenance
      if (!(child instanceof Entity || child instanceof EntityReference))
        continue;
      // collect standard bio IDs (skip publications);
      // (we will use id-mapping later to associate more IDs)
      for (Xref x : child.getXref()) {
        if (!(x instanceof PublicationXref) && x.getId() != null && x.getDb() != null) {
          ids.add(x.getId());
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
   * Generate a URI (for a Provenance instance.)
   *
   * @return URI
   */
  static String getMetadataUri(Model model, Metadata metadata) {
    return model.getXmlBase() + metadata.getIdentifier();
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
   * this path will be stored in the corresponding Metadata.files collection and
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
