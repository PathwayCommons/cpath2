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
package cpath.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;


/**
 * CPath2 server-side instance-specific 
 * configuration constants and properties.
 * Singleton.
 * 
 * Some properties can be modified 
 * at runtime, i.e, via the admin tool.
 * 
 * But this is not for cpath2 clients to care.
 * 
 * @author rodche
 */
public final class CPathSettings {
	
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
	 * The cache sub-directory (under cpath2 Home dir.)
	 */
	public static final String CACHE_SUBDIR = "cache";
	
	/**
	 * cpath2 internal "blacklist" (of the ubiquitous molecules
	 * to be excluded by some queries and format converters).
	 */
	public static final String BLACKLIST_FILE = "blacklist.txt";
	
	/**
	 * cpath2 Metadata configuration default file name.
	 */
	public static final String METADATA_FILE = "metadata.conf";
	
	/**
	 * Common prefix for cPath2 generated BioPAX comments
	 */
	public static final String CPATH2_GENERATED_COMMENT = "cPath2-generated";

	
	/* System / Environment property names used by cPath2
	 * (loaded by Spring (property placeholder) from the cpath2.properties,
	 * but can be also via java -D options too)
	 */
	public static final String PROP_DB_USER = "cpath2.db.user";
	public static final String PROP_DB_PASSW = "cpath2.db.password";
	public static final String PROP_DB_DRIVER = "cpath2.db.driver";
	public static final String PROP_DB_CONNECTION = "cpath2.db.connection";
	public static final String PROP_DB_DIALECT = "cpath2.db.dialect";
	public static final String PROP_ADMIN_USER = "cpath2.admin.user";
	public static final String PROP_ADMIN_PASSW = "cpath2.admin.password";
	public static final String PROP_MAIN_DB = "cpath2.db";
	
	/* Unlike the above, following properties are not used by Spring/Hibernate right away;
	 * normally, cpath2 starts despite these are not provided and will try 
	 * to use reasonable defaults instead, which are defined here as well.
	 */
	public static final String PROP_ADMIN_ENABLED = "cpath2.admin.enabled";
	public static final String PROP_XML_BASE="cpath2.xml.base";	
	public static final String PROP_MAX_SEARCH_HITS_PER_PAGE = "cpath2.maxSearchHitsPerPage";
	public static final String PROP_DEBUG_ENABLED = "cpath2.debug.enabled";
	public static final String PROP_BLACKLIST_DEGREE_THRESHOLD = "cpath2.blacklist.degree.threshold";   
	public static final String PROP_BLACKLIST_CONTROL_THRESHOLD = "cpath2.blacklist.control.threshold";
	public static final String PROP_METADATA_LOCATION = "cpath2.metadata.location";
	public static final String PROP_ABSOLUTE_URI_ENABLED="cpath2.absolute.uri.enabled"; //when generating biopx archives in "downloads"
	public static final String PROP_PROXY_MODEL_ENABLED="cpath2.proxy.model.enabled";
	
	/*
	 * Following properties can be even updated at runtime (because cpath2.properties resource is read by the webapp periodically)
	 */	
	public static final String PROVIDER_NAME = "cpath2.provider.name";
	public static final String PROVIDER_DESCRIPTION = "cpath2.provider.description";
    public static final String PROVIDER_VERSION = "cpath2.provider.version";
    public static final String PROVIDER_URL = "cpath2.provider.url";
	public static final String PROVIDER_LOGO_URL = "cpath2.provider.logo.url";
	public static final String PROVIDER_ORGANISMS = "cpath2.provider.organisms";
	
	
	/**
	 * Private Constructor
	 */
	private CPathSettings() {
		// check/init cpath2 home dir JVM property
		String home = System.getenv(HOME_DIR);		
		if(home == null || home.isEmpty()) {
			throw new IllegalStateException(HOME_DIR + " environment variable is undefined!");
		} else {
			//replace/override the JVM option
			synchronized (home) {
				System.setProperty(HOME_DIR, home);
			}	
		}
		
		// put default values
		Properties defaults = new Properties();
		defaults.put(PROP_XML_BASE, "http://purl.org/pc2/test/");
		defaults.put(PROVIDER_NAME, "cPath2 Demo");
		defaults.put(PROVIDER_VERSION, "");
		defaults.put(PROVIDER_DESCRIPTION, "cPath2 Demo");
		defaults.put(PROVIDER_ORGANISMS, "homo sapiens");
		defaults.put(PROP_BLACKLIST_CONTROL_THRESHOLD, "15");
		defaults.put(PROP_BLACKLIST_DEGREE_THRESHOLD, "100");
		defaults.put(PROP_MAX_SEARCH_HITS_PER_PAGE, "500");
		defaults.put(PROP_METADATA_LOCATION, homeDir() + File.separator + METADATA_FILE);
		defaults.put(PROP_DEBUG_ENABLED, "false");
		defaults.put(PROP_ADMIN_ENABLED, "false");	
		defaults.put(PROP_ABSOLUTE_URI_ENABLED, "true");
		defaults.put(PROP_PROXY_MODEL_ENABLED, "false");
		
		settings = new Properties(defaults);
		
		loadCPathProperties();
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
	 * Default is {"Homo sapiens"} (when the property is not set)
	 *  
	 * @return
	 */
	public String[] getOrganisms() {
		String orgs = property(PROVIDER_ORGANISMS);
		return orgs.split("\\s*,\\s*");
	}

	public void setOrganisms(String[] organisms) {
		setCPathProperty(PROVIDER_ORGANISMS, StringUtils.join(organisms, ','));
	}

	
	/**
	 * This cPath2 instance version
	 * (not cpath2 software's but the resource's)
	 * 
	 * @return
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


	/**
	 * This cPath2 instance's database name.
	 * 
	 * @return
	 */
	public String getMainDb() {
		return property(PROP_MAIN_DB);
	}

	public void setMainDb(String mainDb) {
		setCPathProperty(PROP_MAIN_DB, mainDb);
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
	 * Flags if cPath2 services are to use the in-memory
	 * (proxy) Model in front of the persistent biopax model.
	 * 
	 * @return
	 */
	public boolean isProxyModelEnabled() {
		return "true".equalsIgnoreCase(property(PROP_PROXY_MODEL_ENABLED));
	}	

	
	public void setProxyModelEnabled(boolean enabled) {
		setCPathProperty(PROP_PROXY_MODEL_ENABLED, Boolean.toString(enabled));
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
		String file = homeDir() + File.separator + CPATH_PROPERTIES_FILE;		
		try {
			settings.load(new FileReader(file));
		} catch (IOException e) {
			throw new RuntimeException("Failed to load cPath2 properties " +
					"from " + file, e);
		}
	}
	

	/**
	 * Stores cpath2 properties back to the file 
	 * (overwrites).
	 * 
	 * @throws IllegalStateException when maintenance mode is disabled
	 */
	public void saveCPathProperties() {
		
		if(!isAdminEnabled())
			throw new IllegalStateException("Not in Maintenance mode.");
		
		String file = homeDir() + File.separator + CPATH_PROPERTIES_FILE;		
		try {
			settings.store(new FileOutputStream(file), 
					"cPath2 server configuration properties");
		} catch (IOException e) {
			throw new RuntimeException("Failed to write cPath2 properties " +
					"to " + file, e);
		}		
	}

		
	/**
	 * Gets current Home Directory (full path).
	 * 
	 * The cpath2 home system environment variable 
	 * (which must be set) overrides the java option, if any.
	 * 
	 * @return
	 */
	public String homeDir() {		
		return property(HOME_DIR);
	}
	
	
	/**
	 * Gets the full path to the local directory 
	 * where pathway and other data will be fetched and looked for.
	 * 
	 * @return
	 */
	public String dataDir() {
		return homeDir() + File.separator + DATA_SUBDIR;
	}
	
	
	/**
	 * Gets the full path to the cpath2 query/converter
	 * blacklist (whether it exists or yet to be generated) 
	 * 
	 * @return
	 */
	public String blacklistFile() {
		return homeDir() + File.separator + BLACKLIST_FILE;
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
			System.setProperty(name, value);
			settings.setProperty(name, value);
		} 
		else 
		{	//ok to alter some props in the 'normal' state too
			if(PROP_PROXY_MODEL_ENABLED.equals(name)
					|| PROP_DEBUG_ENABLED.equals(name)
					|| PROP_MAX_SEARCH_HITS_PER_PAGE.equals(name)
			) {
				System.setProperty(name, value);
				settings.setProperty(name, value);
			} else {
				// not allowed in this mode (not maintenance)
				throw new IllegalStateException("Attempt to set property "
					+ name + " when " + PROP_ADMIN_ENABLED 
						+ " = false");
			}
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
		return homeDir() + File.separator + DOWNLOADS_SUBDIR;
	}
	
	
	/**
	 * Gets the full path to current java TMP directory.
	 * 
	 * @return
	 */
	public String tmpDir() {
		return System.getProperty("java.io.tmpdir");
	}
	
	
	/**
	 * Gets the full path to query cache directory.
	 * 
	 * @return
	 */
	public String cacheDir() {
		return homeDir() + File.separator + CACHE_SUBDIR;
	}
	
	
	/**
	 * Full path to the archive file where BioPAX db/model is exported. 
	 * 
	 * @param name - a Metadata's Standard Name, or "All", "Warehouse".
	 * @return
	 */
	public String biopaxExportFileName(String name) {
		return downloadsDir() + File.separator + 
				exportArchivePrefix() + name 
					+ ".BIOPAX.owl.gz";
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
		return property(PROVIDER_NAME) + 
				"." + property(PROVIDER_VERSION) + 
				".";
	}
}
