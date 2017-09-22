package cpath.config;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.service.Scope;


/**
 * CPath2 server-side instance-specific 
 * configuration constants and properties.
 * Singleton.
 *
 * But this is not for cpath2 clients to care.
 * 
 * @author rodche
 */
public final class CPathSettings
{
	private static final Logger LOG = LoggerFactory.getLogger(CPathSettings.class);
    private static CPathSettings instance;
	private Properties settings;
	
	/**
	 * Name for the system environment and/or JVM variable 
	 * cPath2 uses to know its "home" directory location.
	 */
	public static final String HOME_DIR = "CPATH2_HOME";
	
	/**
	 * Name for the cpath2 instance properties file (located in the cpath2 home directory)
	 */
	public static final String CPATH_PROPERTIES_FILE = "cpath2.properties";	

	/**
	 * Name for a cpath2 data sub-directory (under cpath2 home dir.)
	 */
	public static final String DATA_SUBDIR = "data";
	
	/**
	 * The sub-directory (under cpath2 Home dir.) to organize user downloadable data.
	 */
	public static final String DOWNLOADS_SUBDIR = "downloads";
	
	/**
	 * The cache sub-directory (under cpath2 home dir.)
	 */
	public static final String CACHE_SUBDIR = "cache";
	
	/**
	 * The index sub-directory (under cpath2 home dir.)
	 */
	public static final String INDEX_SUBDIR = "index";	
	
	/**
	 * cpath2 internal "blacklist" (of the ubiquitous molecules
	 * to be excluded by graph queries and data converters/exporters).
	 */
	public static final String BLACKLIST_FILE = "blacklist.txt";

	public static final String EXPORT_SCRIPT_FILE ="export.sh";

	/**
	 * cpath2 Metadata configuration default file name.
	 */
	public static final String METADATA_FILE = "metadata.conf";
	
	/**
	 * Common prefix for cPath2 generated BioPAX comments
	 */
	public static final String CPATH2_GENERATED_COMMENT = "cPath2-generated";

	
	/* System / Environment property names used by cPath2
	 * (loaded by Spring property placeholder from the cpath2.properties,
	 * but can be also via java -D options too)
	 */
	
	/* Unlike the above, following properties are not used by Spring/Hibernate right away;
	 * normally, cpath2 starts even though these might not be set and will  
	 * use reasonable defaults, which are defined below.
	 */
	public static final String PROP_ADMIN_ENABLED = "cpath2.admin.enabled";
	public static final String PROP_XML_BASE="cpath2.xml.base";	
	public static final String PROP_MAX_SEARCH_HITS_PER_PAGE = "cpath2.maxSearchHitsPerPage";
	public static final String PROP_DEBUG_ENABLED = "cpath2.debug.enabled";
	public static final String PROP_METADATA_LOCATION = "cpath2.metadata.location";
	public static final String PROP_SBGN_LAYOUT_ENABLED = "cpath2.sbgn.layout.enabled";
	
	public static final String PROVIDER_NAME = "cpath2.provider.name";
	public static final String PROVIDER_DESCRIPTION = "cpath2.provider.description";
    public static final String PROVIDER_VERSION = "cpath2.provider.version";
    public static final String PROVIDER_URL = "cpath2.provider.url";
	public static final String PROVIDER_LOGO_URL = "cpath2.provider.logo.url";
	public static final String PROVIDER_ORGANISMS = "cpath2.provider.organisms";
	public static final String PROVIDER_DOWNLOADS_URL = "cpath2.provider.downloads.url";
	public static final String PROVIDER_GA = "cpath2.provider.ga"; //Google Analytics code

	/**
	 * Private Constructor
	 */
	private CPathSettings() {
		// put default values
		Properties defaults = new Properties();
		defaults.put(PROP_XML_BASE, "http://pathwaycommons.org/test/");
		defaults.put(PROVIDER_NAME, "Pathway Commons demo");
		defaults.put(PROVIDER_VERSION, "0");
		defaults.put(PROVIDER_DESCRIPTION, "Pathway Commons Team");
		defaults.put(PROVIDER_ORGANISMS, "Homo sapiens (9606)");
		defaults.put(PROP_MAX_SEARCH_HITS_PER_PAGE, "500");
		defaults.put(PROP_METADATA_LOCATION, homeDir() + FileSystems.getDefault().getSeparator() + METADATA_FILE);
		defaults.put(PROP_DEBUG_ENABLED, "false");
		defaults.put(PROP_ADMIN_ENABLED, "false");
		defaults.putIfAbsent(PROP_SBGN_LAYOUT_ENABLED,"false");
		//default settings
		settings = new Properties(defaults);
	}

	
	/**
	 * Gets the cPath2 settings singleton.
	 * A change (calling a setter method) will have global effect.
	 * 
	 * @return
	 */
	public static synchronized CPathSettings getInstance() {
		if(instance == null) {
			instance = new CPathSettings();
			instance.subDir(""); //creates the home dir if it did not exist
			instance.subDir(DATA_SUBDIR); //creates the data dir
			//load properties from file; overrides some of the defaults
			instance.loadCPathProperties();
		}		
		return instance;
	}


	/**
	 * The service provider name (cPath2 instance owner)
	 * @return
	 */
	public String getName() {
		return property(PROVIDER_NAME);
	}

	public void setName(String name) {
		setCPathProperty(PROVIDER_NAME, name);
	}


	/**
	 * The service provider description (cPath2 instance owner)
	 * @return
	 */
	public String getDescription() {
		return property(PROVIDER_DESCRIPTION);
	}

	public void setDescription(String description) {
		setCPathProperty(PROVIDER_DESCRIPTION, description);
	}


	public String getLogoUrl() {
		return property(PROVIDER_LOGO_URL);
	}

	public void setLogoUrl(String logoUrl) {
		setCPathProperty(PROVIDER_LOGO_URL, logoUrl);
	}


	public String getUrl() {
		return property(PROVIDER_URL);
	}

	public void setUrl(String url) {
		setCPathProperty(PROVIDER_URL, url);
	}


	public String getDownloadsUrl() {
		return property(PROVIDER_DOWNLOADS_URL);
	}

	public void setDownloadsUrl(String url) {
		setCPathProperty(PROVIDER_DOWNLOADS_URL, url);
	}


	public String getGa() {
		return property(PROVIDER_GA);
	}

	public void setGa(String ga) {
		setCPathProperty(PROVIDER_GA, ga);
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
	 * but only these organisms, specified in the cpath2.properties file, are
	 * considered to generate export data archives and to be shown on web pages.
	 * 
	 * Default is {"Homo sapiens (9606)"} (when the property is not set)
	 *  
	 * @return the list of supported organisms (defined as 'name (taxID)')
	 */
	public String[] getOrganisms() {
		String orgs = property(PROVIDER_ORGANISMS);
		return orgs.split("\\s*,\\s*");
	}

	public void setOrganisms(String[] organisms) {
		setCPathProperty(PROVIDER_ORGANISMS, StringUtils.join(organisms, ','));
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
		Set<String> taxids = new HashSet<String>();
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
		Map<String,String> m = new HashMap<String,String>();
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
	 * This cPath2 instance version
	 * (not cpath2 software's but the resource's)
	 * 
	 * @return cPath2 db/instance version
	 */
	public String getVersion() {
		return property(PROVIDER_VERSION);
	}

	public void setVersion(String version) {
		setCPathProperty(PROVIDER_VERSION, version);
	}


	public boolean isAdminEnabled() {
		return "true".equalsIgnoreCase(property(PROP_ADMIN_ENABLED));
	}
	
	public void setAdminEnabled(boolean enabled) {
		setCPathProperty(PROP_ADMIN_ENABLED, Boolean.toString(enabled));
	}


	public String getMaxHitsPerPage() {
		return property(PROP_MAX_SEARCH_HITS_PER_PAGE);
	}

	public void setMaxHitsPerPage(String maxHitsPerPage) {
		setCPathProperty(PROP_MAX_SEARCH_HITS_PER_PAGE, maxHitsPerPage);
	}


	/**
	 * This cPath2 instance's xml:base 
	 * (cpath2 service should use a cpath2 db
	 * build using the same xml:base as the instance's)
	 * 
	 * @return
	 */
	public String getXmlBase() {
		return property(PROP_XML_BASE);
	}

	public void setXmlBase(String xmlBase) {
		setCPathProperty(PROP_XML_BASE, xmlBase);
	}
	
		
	/**
	 * Gets a cpath2 property value
	 * by name from:
	 * - System (JVM), then (if not set) -
	 * - from the cpath2.properties file 
	 * (lastly, the default value is used if the property 
	 * is not set in the file)
	 * 
	 * @param name cpath2 property name
	 * @return
	 */
	public synchronized String property(String name) {	
		String val = System.getProperty(name);
		
		if(val == null || val.isEmpty())
			val =  settings.getProperty(name);		
		
		return val;
	}

	
	/**
	 * Reads cpath2 properties from the file.
	 */
	public void loadCPathProperties() {
		Path file = Paths.get(homeDir(), CPATH_PROPERTIES_FILE);
		try {
			settings.load(Files.newBufferedReader(file));
		} catch (IOException e) {
			LOG.warn("Couldn't update cPath2 properties " +
				"from " + file + "; will use defaults. " 
					+ e.toString());
		}
	}

	/**
	 * Gets current Home Directory (full path; must exist).
	 * 
	 * The cpath2 home system environment variable 
	 * (which must be set) overrides the java option, if any.
	 * 
	 * @return
	 */
	public String homeDir() {
		String homedir = System.getProperty(HOME_DIR);

		if(homedir == null || homedir.isEmpty()) {
			homedir = System.getenv(HOME_DIR);
			if (homedir == null || homedir.isEmpty()) {
				homedir = Paths.get(System.getProperty("java.io.tmpdir"), "cpath2_home").toString();
				LOG.warn("'" + HOME_DIR + "' is UNDEFINED (using: '" + homedir + "')");
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
	 * Sets or updates a cpath2 instance property
	 * but only if Admin mode is enabled; Admin mode
	 * though can be altered always.
	 * 
	 * @param name
	 * @param value
	 */
	public synchronized void setCPathProperty(String name, String value) {
		if(PROP_ADMIN_ENABLED.equals(name) || isAdminEnabled())
		{
			setProp(name,value);
		} 
		else 
		{	//ok to alter some props in the 'normal' state too
			if(PROP_DEBUG_ENABLED.equals(name)
					|| PROP_MAX_SEARCH_HITS_PER_PAGE.equals(name) //always allow
			) {
				setProp(name, value);
			} else {
				// not allowed in this mode (not maintenance)
				throw new IllegalStateException("Attempt to set property "
					+ name + " when " + PROP_ADMIN_ENABLED 
						+ " = false");
			}
		}
	}
	
	
	private void setProp(String name, String value) {
		if(value==null) {
			settings.remove(name);
			System.getProperties().remove(name);
		} else {
			System.setProperty(name, value);
			settings.setProperty(name, value);
		}
	}


	/**
	 * Flags if cPath2 debug mode is enabled.
	 * This triggers the inclusion of score and explanation
	 * into search results and use of md5 sum digest (primary key)
	 * in place of URI in some queries.
	 * 
	 * @return
	 */
	public boolean isDebugEnabled() {
		return "true".equalsIgnoreCase(property(PROP_DEBUG_ENABLED));
	}
		
	public void setDebugEnabled(boolean enabled) {
		setCPathProperty(PROP_DEBUG_ENABLED, Boolean.toString(enabled));
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
	 * Gets the full path to query cache directory.
	 * @deprecated
	 * @return
	 */
	public String cacheDir() {
		return subDir(CACHE_SUBDIR);
	}
	
	
	/**
	 * Full path to the archive file where a BioPAX sub-model is exported.
	 * 
	 * @param name - a Metadata's identifier, organism name, or a special name, such as "All", "Warehouse", "Detailed".
	 * @return
	 * @see #downloadsDir()
	 */
	public String biopaxFileNameFull(String name) {
		return downloadsDir() + FileSystems.getDefault().getSeparator() + biopaxFileName(name);
	}

	/**
	 * Local name of the BioPAX sub-model file (in the batch downloads directory).
	 *
	 * @param name - a Metadata's identifier, organism name, or a special name, such as "All", "Warehouse", "Detailed".
	 * @return
	 * @see #downloadsDir()
	 */
	public String biopaxFileName(String name) {
		return exportArchivePrefix() + name + ".BIOPAX.owl.gz";
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
		return WordUtils.capitalize(property(PROVIDER_NAME) + property(PROVIDER_VERSION))
				.replaceAll("\\W+","") + ".";
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


	public boolean isSbgnLayoutEnabled() {
		return "true".equalsIgnoreCase(property(PROP_SBGN_LAYOUT_ENABLED));
	}

	public void setSbgnLayoutEnabled(boolean enabled) {
		setCPathProperty(PROP_SBGN_LAYOUT_ENABLED, Boolean.toString(enabled));
	}

}
