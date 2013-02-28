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
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.impl.BioPAXElementImpl;


/**
 * CPath2 constants and properties access.
 * 
 * @author rodche
 */
public final class CPathSettings {
	private static final Log log = LogFactory.getLog(CPathSettings.class);
	
	
	private static final Properties cPathProperties = new Properties();

	/**
	 * Current Newline symbol ('line.separator' system property value)
	 */
	public static final String NEWLINE = System.getProperty ( "line.separator" );
	
	/**
	 * Name for the system environment and/or JVM variable 
	 * cPath2 uses to know its "home" directory location.
	 */
	public static final String HOME_VARIABLE_NAME = "CPATH2_HOME";

	/**
	 * Name for the cpath2 instance properties file (located in the cpath2 home directory)
	 */
	public static final String CPATH_PROPERTIES_FILE_NAME = "cpath.properties";
	
	/**
	 * Name for a cpath2 data sub-directory (under cpath2 home dir.)
	 */
	public static final String DATA_SUBDIR = "data";
	
	/**
	 * A sub-directory (under cpath2 Home dir.) to organize various data available to download via the web app.
	 */
	public static final String DOWNLOADS_SUBDIR = "downloads";
	
	
	private CPathSettings(){
		throw new AssertionError("Noninstantiable!");
	};
	
	
	/**
	 * Property names used in the cpath2 properties file,
	 * except for some properties, such as db connection, 
	 * are not listed (to prevent access via the public api, 
	 * for security reasons).
	 * 
	 * @author rodche
	 */
	public static enum CPath2Property {
		MAIN_DB("main.db"),
		METADATA_DB("metadata.db"),
		WAREHOUSE_DB("warehouse.db"),
		XML_BASE("xml.base"),
		MAX_SEARCH_HITS_PER_PAGE("maxSearchHitsPerPage"),
		EXPLAIN_ENABLED("explain.enabled"), //can be also used for debug/verbose output
		DIGEST_URI_ENABLED("md5hex.uri.enabled"),
        BLACKLIST_DEGREE_THRESHOLD("blacklist.degree.threshold"),
        BLACKLIST_CONTROL_THRESHOLD("blacklist.control.threshold"),
        PROVIDER("provider.name"),
        DESCRIPTION("provider.description"),
        VERSION("provider.version"),
        URL("provider.url"),
        LOGO_URL("provider.logo.url"),
        MAINTENANCE_MODE_ENABLED("maintenance-mode.enabled")
		;
		
		private final String name;
		
		private CPath2Property(String name) {
			this.name = name;
		}
		
		@Override
		public String toString() {
			return name;
		}
	}
	
	
	static {
		String file = homeDir() + File.separator + CPATH_PROPERTIES_FILE_NAME;		
		try {
			cPathProperties.load(new FileReader(file));
		} catch (IOException e) {
			throw new RuntimeException("Failed to load cPath2 properties " +
					"from " + file, e);
		}
	}


	/**
	 * Common prefix for cPath2 generated BioPAX comments
	 */
	public static final String CPATH2_GENERATED_COMMENT = "cPath2-generated";
		
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
		return homeDir() 
			+ File.separator + CPathSettings.DATA_SUBDIR;
	}
	
	
	/**
	 * Gets the full path to the cpath2 query/converter
	 * blacklist (whether it exists or yet to be generated) 
	 * 
	 * @return
	 */
	public static String blacklistFile() {
		return homeDir() + File.separator + "blacklist.txt";
	}	
	
	
	
	/**
	 * Answers whether cPath2 will also accept  
	 * digest IDs (i.e., 32-byte MD5 hex, primary key, string) 
	 * in place of actual URI/RDFId in queries.
	 * 
	 * @see BioPAXElementImpl
	 * 
	 * @return
	 */
	public static boolean digestUriEnabled() {
		return "true".equalsIgnoreCase(get(CPath2Property.DIGEST_URI_ENABLED));
	}
		
	
	/**
	 * Get a cpath2 instance property value.
	 * 
	 * @param xmlBase
	 * @return
	 */
	public static String get(CPath2Property prop) {
		String v = cPathProperties.getProperty(prop.toString());
		
		if (v == null || v.isEmpty()) {
			// for some, set defaults
			switch (prop) {
			case XML_BASE:
				v = ""; 
				log.warn(prop + " is not defined in the cpath.properties files. " +
					"This is not critical for the cpath2 web server application. " +
					"However, one must perform data import using the same " +
					"xml:base in all its stages (fetch, premerge, and merge).");
				break;
			case DIGEST_URI_ENABLED:
				v = "false"; 
				break;
			case EXPLAIN_ENABLED:
				v = "false"; 
				break;
			case MAX_SEARCH_HITS_PER_PAGE:
				v = String.valueOf(Integer.MAX_VALUE); 
				break;
			case MAINTENANCE_MODE_ENABLED:
				v = "false"; 
				break;
			case BLACKLIST_CONTROL_THRESHOLD:
				v = "15";
				break;
			case BLACKLIST_DEGREE_THRESHOLD:
				v = "100";
				break;
			default:
				break;
			}
		}
		
		return v;
	}

	
	/**
	 * Sets a cpath2 instance property.
	 * 
	 * @param prop
	 * @param value
	 */
	public static void set(CPath2Property prop, String value) {
		cPathProperties.setProperty(prop.toString(), value);
	}
	
	
	/**
	 * Flags if cPath2 explain full-text query 
	 * results mode is enabled. 
	 * 
	 * @return
	 */
	public static boolean explainEnabled() {
		return "true".equalsIgnoreCase(get(CPath2Property.EXPLAIN_ENABLED));
	}
		
		
	/**
	 * Flags if cPath2 runs in the maintenance mode,
	 * (all services except for admin are disabled)
	 * 
	 * @return
	 */
	public static boolean isMaintenanceModeEnabled() {
		return "true".equalsIgnoreCase(get(CPath2Property.MAINTENANCE_MODE_ENABLED));
	}

	
	/**
	 * Enable/disable cPath2 maintenance mode.
	 * 
	 * @param value
	 */
	public static void setMaintenanceModeEnabled(boolean value) {
		set(CPath2Property.MAINTENANCE_MODE_ENABLED, Boolean.toString(value));
	}
	
	
	/**
	 * Gets the full path to the local directory 
	 * where cpath2 (batch data downloads) archives are stored.
	 * 
	 * @return
	 */
	public static String downloadsDir() {
		return CPathSettings.homeDir() 
	    	+ File.separator + CPathSettings.DOWNLOADS_SUBDIR;
	}
	
	
	/**
	 * Gets the current TMP directory (full path).
	 * 
	 * @return
	 */
	public static String tmpDir() {
		return System.getProperty("java.io.tmpdir");
	}
}
