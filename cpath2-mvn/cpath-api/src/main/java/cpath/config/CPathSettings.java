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


/**
 * CPath2 server-side instance-specific 
 * configuration constants and properties.
 * 
 * Some properties can be modified 
 * at runtime, i.e, via the admin tool.
 * 
 * But this is not for cpath2 clients to care.
 * 
 * @author rodche
 */
public final class CPathSettings {

	
	private static Properties settings;
	
	/**
	 * Name for the system environment and/or JVM variable 
	 * cPath2 uses to know its "home" directory location.
	 */
	public static final String HOME_VARIABLE_NAME = "CPATH2_HOME";
	
	/**
	 * Name for the cpath2 instance properties file (located in the cpath2 home directory)
	 */
	public static final String CPATH_PROPERTIES_FILE = "cpath.properties";	

	/**
	 * Name for a cpath2 data sub-directory (under cpath2 home dir.)
	 */
	public static final String DATA_SUBDIR = "data";
	
	/**
	 * A sub-directory (under cpath2 Home dir.) to organize various data available to download via the web app.
	 */
	public static final String DOWNLOADS_SUBDIR = "downloads";
	
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
	 * (loaded by Spring (property placeholder) from the cpath.properties,
	 * but can be also via java options too)
	 */
	public static final String PROP_DB_USER = "cpath2.db.user";
	public static final String PROP_DB_PASSW = "cpath2.db.password";
	public static final String PROP_DB_DRIVER = "cpath2.db.driver";
	public static final String PROP_DB_CONNECTION = "cpath2.db.connection";
	public static final String PROP_DB_DIALECT = "cpath2.db.dialect";
	public static final String PROP_ADMIN_USER = "cpath2.admin.user";
	public static final String PROP_ADMIN_PASSW = "cpath2.admin.password";
	public static final String PROP_MAIN_DB = "cpath2.main.db";
	public static final String PROP_METADATA_DB = "cpath2.metadata.db";
	public static final String PROP_WAREHOUSE_DB = "cpath2.warehouse.db";
	
	/* Unlike the above, following properties are not used by Spring/Hibernate right away;
	 * normally, cpath2 starts despite these are not provided and will try 
	 * to use reasonable defaults instead, which are defined here as well.
	 * 
	 */
	public static final String PROP_ADMIN_ENABLED = "cpath2.admin.enabled";
	public static final String PROP_XML_BASE="xml.base";	
	public static final String PROP_MAX_SEARCH_HITS_PER_PAGE = "maxSearchHitsPerPage";
	public static final int DEFAULT_MAX_SEARCH_HITS_PER_PAGE = 500;
	public static final String PROP_EXPLAIN_ENABLED = "explain.enabled";
	public static final String PROP_DIGEST_URI_ENABLED = "md5hex.uri.enabled";
	public static final String PROP_BLACKLIST_DEGREE_THRESHOLD = "blacklist.degree.threshold";
	public static final int DEFAULT_BLACKLIST_DEGREE_THRESHOLD = 100;    
	public static final String PROP_BLACKLIST_CONTROL_THRESHOLD = "blacklist.control.threshold";
	public static final int DEFAULT_BLACKLIST_CONTROL_THRESHOLD = 15;
	public static final String PROP_BLACKLIST_LOCATION = "blacklist.location";
	public static final String PROP_METADATA_LOCATION = "metadata.location";
	
	/*
	 * Following properties can be even updated at runtime (because cpath.properties resource is read by the webapp periodically)
	 */	
	public static final String PROVIDER_NAME = "provider.name";
	public static final String PROVIDER_DESCRIPTION = "provider.description";
    public static final String PROVIDER_VERSION = "provider.version";
    public static final String PROVIDER_URL = "provider.url";
	public static final String PROVIDER_LOGO_URL = "provider.logo.url";
	
	/*
	 * Define corresponding public constants to use with 
	 * java annotations (e.g. @Value($PROP_ADMIN_ENABLED) )
	 */
	public static final String $DB_USER = "${" + PROP_DB_USER + "}";
	public static final String $DB_DRIVER = "${" + PROP_DB_DRIVER + "}";
	public static final String $DB_DIALECT = "${" + PROP_DB_DIALECT + "}";
	public static final String $DB_PASSW = "${" + PROP_DB_PASSW + "}";
	public static final String $DB_CONNECTION = "${" + PROP_DB_CONNECTION + "}";
	public static final String $ADMIN_USER = "${" + PROP_ADMIN_USER + "}";
	public static final String $ADMIN_PASSW = "${" + PROP_ADMIN_PASSW + "}";	
	public static final String $MAIN_DB = "${" + PROP_MAIN_DB + "}";
	public static final String $METADATA_DB = "${" + PROP_METADATA_DB + "}";
	public static final String $WAREHOUSE_DB = "${" + PROP_WAREHOUSE_DB + "}";		
	
	
	/**
	 * Private Constructor
	 */
	private CPathSettings(){}

	
	/**
	 * Spath2 configuration properties
	 * static initializer.
	 */
	static {
		Properties defaults = new Properties();
		
		defaults.put(PROP_XML_BASE, "http://purl.org/pc2/");
		defaults.put(PROP_BLACKLIST_LOCATION, 
				homeDir() + File.separator + BLACKLIST_FILE);
		defaults.put(PROVIDER_NAME, "cPath2 Demo");
		defaults.put(PROVIDER_VERSION, "0");
		defaults.put(PROVIDER_DESCRIPTION, "cPath2 Demo");
		defaults.put(PROP_BLACKLIST_CONTROL_THRESHOLD, 15);
		defaults.put(PROP_BLACKLIST_DEGREE_THRESHOLD, 100);
		defaults.put(PROP_MAX_SEARCH_HITS_PER_PAGE, 500);
		defaults.put(PROP_METADATA_LOCATION, 
				homeDir() + File.separator + METADATA_FILE);
		
		settings = new Properties(defaults);		
		loadCPathProperties();
	}


	/**
	 * Gets current cpath2 property value
	 * or default value, if exists.
	 * 
	 * @param name cpath2 property name
	 * @return
	 */
	public static String property(String name) {		
		return settings.getProperty(name);
	}

	
	/**
	 * Reads cpath2 properties from the file.
	 */
	public static void loadCPathProperties() {
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
	 */
	public static void saveCPathProperties() {
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
	 * @return
	 */
	public static String homeDir() {
		return System.getProperty(HOME_VARIABLE_NAME);
	}
	
	
	/**
	 * Gets the full path to the local directory 
	 * where pathway and other data will be fetched and looked for.
	 * 
	 * @return
	 */
	public static String localDataDir() {
		return homeDir() + File.separator + DATA_SUBDIR;
	}
	
	
	/**
	 * Gets the full path to the cpath2 query/converter
	 * blacklist (whether it exists or yet to be generated) 
	 * 
	 * @return
	 */
	public static String blacklistFile() {
		return property(PROP_BLACKLIST_LOCATION);
	}	
	
	
	
	/**
	 * Answers whether cPath2 will also accept  
	 * digest IDs (i.e., 32-byte MD5 hex, primary key, string) 
	 * in place of actual URI/RDFId in queries.
	 * 
	 * @return
	 */
	public static boolean digestUriEnabled() {
		return "true".equalsIgnoreCase(property(PROP_DIGEST_URI_ENABLED));
	}
		
	
	/**
	 * Sets or updates a cpath2 instance property.
	 * 
	 * @param name
	 * @param value
	 */
	public static void setCPathProperty(String name, String value) {
		settings.setProperty(name, value);
		saveCPathProperties();
	}
	
	
	/**
	 * Flags if cPath2 explain full-text query 
	 * results mode is enabled. 
	 * 
	 * @return
	 */
	public static boolean explainEnabled() {
		return "true".equalsIgnoreCase(property(PROP_EXPLAIN_ENABLED));
	}
		

	/**
	 * Flags if cPath2 runs in the maintenance mode,
	 * (all services except for admin are disabled)
	 * 
	 * @return
	 */
	public static boolean isMaintenanceEnabled() {
		return "true".equalsIgnoreCase(property(PROP_ADMIN_ENABLED));
	}
	
	
	/**
	 * Gets the full path to the local directory 
	 * where cpath2 (batch data downloads) archives are stored.
	 * 
	 * @return
	 */
	public static String downloadsDir() {
		return CPathSettings.homeDir() 
	    	+ File.separator + DOWNLOADS_SUBDIR;
	}
	
	
	/**
	 * Gets the full path to current java TMP directory.
	 * 
	 * @return
	 */
	public static String tmpDir() {
		return System.getProperty("java.io.tmpdir");
	}
	
	
	/**
	 * Gets the full path to current java TMP directory.
	 * 
	 * @return
	 */
	public static String xmlBase() {
		return property(PROP_XML_BASE);
	}
}
