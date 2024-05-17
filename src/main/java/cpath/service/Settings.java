package cpath.service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cpath2")
public class Settings
{
  private static final Logger LOG = LoggerFactory.getLogger(Settings.class);

  /**
   * System environment/JVM variable name to define the work directory
   * (for properties, data, log).
   */
  public static final String HOME_DIR = "CPATH2_HOME";

  /**
   * Name for a cpath2 data subdirectory (under cpath2 home dir.)
   */
  public static final String DATA_SUBDIR = "data";

  /**
   * The subdirectory (under cpath2 Home dir.) to organize user downloadable data.
   */
  public static final String DOWNLOADS_SUBDIR = "downloads";

  /**
   * The index subdirectory (under cpath2 home dir.)
   */
  public static final String INDEX_SUBDIR = "index";

  /**
   * The "blacklist" of the ubiquitous molecules
   * to be excluded by graph queries and data converters.
   */
  public static final String BLACKLIST_FILE = "blacklist.txt";

  public static final String EXPORT_SCRIPT_FILE ="export.sh";

  /**
   * Datasource configuration default file name.
   */
  public static final String METADATA_FILE = "metadata.json";

  private Boolean sbgnLayoutEnabled;
  private String xmlBase;
  private Integer maxHitsPerPage;
  private String metadataLocation = "file:" + Paths.get(homeDir(), METADATA_FILE);
  private String name;
  private String description;
  private String version;
  private String url;
  private String logo;
  private String species;
  private String downloads;
  private String email;
  private String organization;

  public Settings() {
    LOG.info("Working ('home') directory: " + homeDir());
    subDir(""); //creates if not exists
    subDir(DATA_SUBDIR);
    sbgnLayoutEnabled = Boolean.FALSE;
  }

  public String getXmlBase() {
    return xmlBase;
  }

  public void setXmlBase(String xmlBase) {
    this.xmlBase = xmlBase;
  }

  public Integer getMaxHitsPerPage() {
    return maxHitsPerPage;
  }

  public void setMaxHitsPerPage(Integer maxHitsPerPage) {
    this.maxHitsPerPage = maxHitsPerPage;
  }

  public String getMetadataLocation() { //uri string
    return metadataLocation;
  }

  public void setMetadataLocation(String metadataLocation) {
    this.metadataLocation = metadataLocation;
  }

  public Boolean getSbgnLayoutEnabled() {
    return sbgnLayoutEnabled;
  }

  public void setSbgnLayoutEnabled(Boolean sbgnLayoutEnabled) {
    this.sbgnLayoutEnabled = sbgnLayoutEnabled;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getLogo() {
    return logo;
  }

  public void setLogo(String logo) {
    this.logo = logo;
  }

  public String getSpecies() {
    return species;
  }

  public void setSpecies(String species) {
    this.species = species;
  }

  public String getDownloads() {
    return downloads;
  }

  public void setDownloads(String downloads) {
    this.downloads = downloads;
  }

  public boolean isSbgnLayoutEnabled() {
    return (getSbgnLayoutEnabled()==null)? false : getSbgnLayoutEnabled().booleanValue();
  }

  /**
   * Species supported by this cPath2 instance
   * (i.e., the organisms of which data were prepared and
   * intentionally imported in to the system, can be filtered by
   * in the web queries, and corresponding data archives
   * were made available to download by users).
   *
   * Imported pathway data may also have other BioSource objects,
   * and those can be used in a search query as the filter by organism value as well;
   * but only these organisms, specified in the properties file, are
   * considered to generate export data archives and to be shown on web pages.
   *
   * Default is {"Homo sapiens (9606)"} (when the property is not set)
   *
   * @return the list of supported organisms (defined as 'name (taxID)')
   */
  public String[] getOrganisms() {
    String orgs = getSpecies();
    return orgs.split("\\s*,\\s*");
  }

  public void setOrganisms(String[] organisms) {
    setSpecies(String.join(",", organisms));
  }

  /**
   * Taxonomy IDs supported by this cPath2 instance,
   * which are extracted from the list returned by #getOrganisms
   * method.
   *
   * @return the list of taxonomy IDs of the organisms
   * @throws AssertionError when taxonomy ID cannot be recognised or not found there.
   */
  public Set<String> getOrganismTaxonomyIds() {
    Set<String> taxids = new HashSet<>();
    final Pattern taxIdPattern = Pattern.compile("\\(\\s*(\\d+)\\s*\\)");
    for(String org : getOrganisms()) {
      //extract and collect taxIDs
      Matcher matcher = taxIdPattern.matcher(org);
      if(matcher.find()) {
        taxids.add(matcher.group(1));
      } else
        throw new AssertionError("getOrganismTaxonomyIds, taxonomy ID not found in: " + org);
    }
    return taxids;
  }

  /**
   * A map of supported taxonomy id, name,
   * @return the map of taxId,name that this service supports
   * @throws AssertionError when taxonomy ID cannot be recognised or not found there.
   */
  public Map<String,String> getOrganismsAsTaxonomyToNameMap() {
    Map<String,String> m = new HashMap<>();
    final Pattern taxIdPattern = Pattern.compile("([a-zA-Z0-9\\. ]+)\\s*\\(\\s*(\\d+)\\s*\\)");
    for(String org : getOrganisms()) {
      Matcher matcher = taxIdPattern.matcher(org);
      if(matcher.find()) {
        m.put(matcher.group(2), matcher.group(1).trim());
      } else
        throw new AssertionError("getOrganismTaxonomyIds, taxonomy ID not found in: " + org);
    }
    return m;
  }

  // Internal/utility methods that rely on the app/env properties

  /*
   * The working directory path (must exist).
   * CPATH2_HOME environment variable overrides the JVM option if defined.
   * The default is 'target/work' (for unit tests.)
   */
  String homeDir() {
    String homedir = System.getProperty(HOME_DIR);

    if(homedir == null || homedir.isEmpty()) {
      homedir = System.getenv(HOME_DIR);
      if (homedir == null || homedir.isEmpty()) {
        homedir = Paths.get("target","work").toString();
      }
      System.setProperty(HOME_DIR, homedir);
    }

    return homedir;
  }

  private String subDir(String subDirectoryName) {
    Path path = Paths.get(homeDir(), subDirectoryName);

    if(!Files.exists(path))
      try {
        Files.createDirectory(path);
      } catch (IOException e) {
        throw new RuntimeException("Cannot create directory: " + path, e);
      }

    return path.toString();
  }

  /*
   * Path to the local directory
   * where pathway and other data will be fetched and looked for.
   */
  String dataDir() {
    return subDir(DATA_SUBDIR);
  }

  /*
   * Path to the default Lucene index.
   */
  String indexDir() {
    return subDir(INDEX_SUBDIR);
  }

  /*
   * Path to the query/converter's blacklist (whether it exists or yet to be generated)
   */
  String blacklistFile() {
    return downloadsDir() + FileSystems.getDefault().getSeparator() + BLACKLIST_FILE;
  }

  /*
   * Path to the local directory where output data archives are stored.
   */
  String downloadsDir() {
    return subDir(DOWNLOADS_SUBDIR);
  }

  /*
   * Path to a compressed BioPAX RDFXML file (e.g. intermediate/normalized source models or warehouse).
   * @param name - a Datasource's identifier, organism name, or a special name, such as "Warehouse".
   */
  String biopaxFileName(String name) {
    return downloadsDir() + FileSystems.getDefault().getSeparator() + StringUtils.lowerCase(name) + ".owl.gz";
  }

  String mainModelFile() {
    return biopaxFileName("pc-biopax");
  }

  String warehouseModelFile() {
    return biopaxFileName("utility");
  }

}
