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
 * CPathSquared Build Constants
 * 
 * @author rodche
 *
 */
public final class CPathSettings {
	protected CPathSettings(){};
	
	public static final String NEWLINE = System.getProperty ( "line.separator" );
	
	public static final String HOME_VARIABLE_NAME = "CPATH2_HOME";
	
	/*
	 * These define only the "KEYS" for the DataServicesFactoryBean's 
	 * data sources map. Values (actual db names) are defined
	 * in cpath.properties file (in the CPATH2_HOME directory).
	 * So, these keys are used in the corresponding Spring 
	 * context configuration files (where SessionFactory is defined)
	 * to define the dataSource beans, e.g.:
	 * <bean id="cpath2_meta" class="cpath.dao.internal.DataServicesFactoryBean"/>
	 * ('cpath2_meta' is a key to get the data source from the factory)
	 */
	public static final String MAIN_DB_KEY = "cpath2_main";
	public static final String METADATA_DB_KEY = "cpath2_meta";
	public static final String MOLECULES_DB_KEY = "cpath2_molecules";
	public static final String PROTEINS_DB_KEY = "cpath2_proteins";
	public static final String PREMERGE_DB_KEY = "premergeDataSource";
	public static final String CREATE_DB_KEY = "createSchema";
	
	/*
	 * URI prefix for auto-generated/converted during the data import 
	 * and normalization utility class objects 
	 * (i.e., for xrefs, ChemicalStructure, etc.)
	 */
	public static final String CPATH_URI_PREFIX = "urn:pathwaycommons:"; //=Normalizer.BIOPAX_URI_PREFIX;
	
	/*
	 * Use this default prefix for DB names that we create (drop),
	 * e.g., for pre-merge and unit test databases.
	 * (this does not affect db names specified in cpath.properties)
	 */
	public static final String CPATH_DB_PREFIX = "cpath2_";

	
	/**
	 * Gets current Home Directory (full path).
	 * 
	 * @return
	 */
	public static String getHomeDir() {
		return System.getenv(HOME_VARIABLE_NAME);
	}
	
}
