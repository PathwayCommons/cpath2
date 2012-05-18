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
import java.net.URI;
import java.net.URLEncoder;
import java.util.Properties;

import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.impl.BioPAXElementImpl;
import org.biopax.paxtools.model.BioPAXElement;


/**
 * CPath2 constants and properties access.
 * 
 * @author rodche
 */
public final class CPathSettings {
	
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
	 * Name for a sub-directory (under cpath2 home dir.) to read/put data
	 */
	public static final String DATA_SUBDIR_NAME = "tmp";
	
	
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
		MOLECULES_DB("molecules.db"),
		PROTEINS_DB("proteins.db"),
		XML_BASE("xml.base"),
		MAX_SEARCH_HITS_PER_PAGE("maxSearchHitsPerPage"),
		EXPLAIN_ENABLED("explain.enabled"),
		DIGEST_URI_ENABLED("md5hex.uri.enabled")
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
		String file = getHomeDir() + File.separator + CPATH_PROPERTIES_FILE_NAME;		
		try {
			cPathProperties.load(new FileReader(file));
		} catch (IOException e) {
			throw new RuntimeException("Failed to load cPath2 properties " +
					"from " + file, e);
		}
	}

	
	/**
	 * This is the default name prefix for optional "pre-merge" DBs we 
	 * can create (or drop) to persist cleaned, normalized, validated 
	 * pathway data;
	 * 
	 * @deprecated we may remove this (pre-merge databases) feature in the future
	 */
	public static final String CPATH_DB_PREFIX = "cpath2_";

	/**
	 * Common prefix for cPath2 generated BioPAX comments
	 */
	public static final String CPATH2_GENERATED_COMMENT = "cPath2-generated";
		
	/**
	 * Gets current Home Directory (full path).
	 * 
	 * @return
	 */
	public static String getHomeDir() {
		return System.getProperty(HOME_VARIABLE_NAME);
	}
	
	/**
	 * Name for the system environment and/or JVM variable 
	 * cPath2 checks to enable extra/advanced debug output.
	 */
	public static final String JAVA_OPT_DEBUG = "cpath.debug";
	
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
	 * Makes the cpath2 instance specific URI for 
	 * an generated BioPAX object. This can be used for new BioPAX objects created: 
	 * - in data converting/importing (in "premerge") into the system;
	 * - in addition to original pathway data (in "merge");
	 * - by various internal data crawlers/fixers (in "post-merge", to be implemented...); 
	 * - if a cpath2 query generates and returns new (inferred) objects as its result; etc.
	 * 
	 * @param name
	 * @return
	 * 
	 * TODO future use...
	 */
	public static String generateInstanceSpecificURI(String localPart, Class<? extends BioPAXElement> type) {
		return URI.create(get(CPath2Property.XML_BASE) + type.getSimpleName()
			+ "/" + URLEncoder.encode(localPart.toUpperCase())).toString();
	}

	/**
	 * Makes an URI for an auto-generated BioPAX object
	 * using BioPAX URN prefix (Normalizer)
	 * (e.g., in "premerge" - Provenance).
	 * 
	 * @see 
	 * 
	 * @param name
	 * @return
	 */
	public static String generateBiopaxURI(String localPart, Class<? extends BioPAXElement> type) {
		return URI.create(ModelUtils.BIOPAX_URI_PREFIX + type.getSimpleName()
			+ ":" + URLEncoder.encode(localPart.toUpperCase())).toString();
	}
	
	
	/**
	 * Get a cpath2 instance property value.
	 * 
	 * @param xmlBase
	 * @return
	 */
	public static String get(CPath2Property prop) {
		String v = cPathProperties.getProperty(prop.toString());
		
		if (v == null) {
			// for some, set defaults
			switch (prop) {
			case XML_BASE:
				v = ModelUtils.BIOPAX_URI_PREFIX; 
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
			default:
				break;
			}
		}
		
		return v;
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
		
}
