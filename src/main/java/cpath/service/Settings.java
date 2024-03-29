package cpath.service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.WordUtils;
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

  /**
   * Gets the work directory (must exist).
   * CPATH2_HOME system environment variable value overrides the JVM option if it's defined.
   * @return CPATH2_HOME value (default 'target/work' is for unit tests)
   */
  public String homeDir() {
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

  /**
   * Gets the full path to the local directory
   * where pathway and other data will be fetched and looked for.
   *
   * @return the data directory path
   */
  public String dataDir() {
    return subDir(DATA_SUBDIR);
  }

  /**
   * A full path to the default Lucene index.
   *
   * @return
   */
  public String indexDir() {
    return subDir(INDEX_SUBDIR);
  }

  /**
   * Gets the full path to the cpath2 query/converter
   * blacklist (whether it exists or yet to be generated)
   *
   * @return
   */
  public String blacklistFile() {
    return downloadsDir() + FileSystems.getDefault().getSeparator() + BLACKLIST_FILE;
  }


  /**
   * Gets the full path to the to-be-generated script.
   *
   * @return
   */
  public String exportScriptFile() {
    return downloadsDir() + FileSystems.getDefault().getSeparator() + EXPORT_SCRIPT_FILE;
  }


  /**
   * Gets the full path to the local directory
   * where cpath2 (batch data downloads) archives are stored.
   *
   * @return
   */
  public String downloadsDir() {
    return subDir(DOWNLOADS_SUBDIR);
  }


  /**
   * Full path to the archive file where a BioPAX sub-model is exported.
   *
   * @param name - a Datasource's identifier, organism name, or a special name, such as "All", "Warehouse", "Detailed".
   * @return
   * @see #downloadsDir()
   */
  public String biopaxFileNameFull(String name) {
    return downloadsDir() + FileSystems.getDefault().getSeparator() + biopaxFileName(name);
  }

  /**
   * Local name of the BioPAX sub-model file (in the batch downloads directory).
   *
   * @param name - a Datasource's identifier, organism name, or a special name, such as "All", "Warehouse", "Detailed".
   * @return
   * @see #downloadsDir()
   */
  public String biopaxFileName(String name) {
    return exportArchivePrefix() + "." + name + ".BIOPAX.owl.gz";
  }

  /**
   * Gets the common file name prefix (includes instance's provider
   * name and version, i.e., that comes after the directory path
   * and file separator but before source name and format)
   * for all cpath2-generated export data archives.
   *
   * @return
   */
  public String exportArchivePrefix() {
    return WordUtils.capitalize(getName() + getVersion())
      .replaceAll("\\W+","");
  }

  /**
   * Full path to the large archive where
   * the complete merged BioPAX model is stored.
   *
   * @return
   */
  public String mainModelFile() {
    return biopaxFileNameFull(Scope.ALL.toString());
  }

  /**
   * Full path to the archive where
   * the Warehouse BioPAX model is stored.
   *
   * @return
   */
  public String warehouseModelFile() {
    return biopaxFileNameFull(Scope.WAREHOUSE.toString());
  }

  /**
   * Predefined large pathway data submodels
   * that are generated and used by the application.
   * (toString method here is to get the part of the
   * corresponding sub-model filename, such as 'All' in '*.All.*.gz').
   *
   * In addition, by-organism and by-source archives
   * are also created in the batch downloads directory,
   * but those filenames do not require this enum.
   *
   * @author rodche
   */
  enum Scope {
      ALL,
      DETAILED,
      WAREHOUSE;

      @Override
      public String toString() { //e.g. "All"
          return name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
      }
  }
}
