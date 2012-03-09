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


/**
 * ServiceResponse Build Constants
 * 
 * @author rodche
 *
 */
public final class CPathSettings {
	private CPathSettings(){
		throw new AssertionError("Noninstantiable!");
	};
	
	public static final String NEWLINE = System.getProperty ( "line.separator" );
	
	/**
	 * Name for the system environment and/or JVM variable 
	 * cPath2 uses to know its "home" directory location.
	 */
	public static final String HOME_VARIABLE_NAME = "CPATH2_HOME";

	
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
	 * Tests whether this cPath2 is in the debug mode.
	 * 
	 * @see CPathSettings#JAVA_OPT_DEBUGA
	 * @return
	 */
	public static boolean isDebug() {
		return "true".equalsIgnoreCase(System.getProperty(CPathSettings.JAVA_OPT_DEBUG));
	}
}
