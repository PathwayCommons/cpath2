package cpath.config;

import java.io.File;

public final class CPathSettings {
	protected CPathSettings(){};
	
	public static final String HOME_VARIABLE_NAME = "CPATH2_HOME";
	
	/*
	 * These define "keys" of the DataServicesFactoryBean's 
	 * data sources map. Values (actual db names) are defined
	 * in cpath.properties file (in the CPATH2_HOME directory).
	 * So, these keys are used in the corresponding Spring 
	 * context configuration files (where SessionFactory is defined)
	 * to define the dataSource beans, e.g.:
	 * <bean id="cpath2_meta" class="cpath.dao.internal.DataServicesFactoryBean"/>
	 * ('cpath2_meta' is a key to get the data source from the factory)
	 */
	public static final String MAIN_DB = "cpath2_main";
	public static final String METADATA_DB = "cpath2_meta";
	public static final String MOLECULES_DB = "cpath2_molecules";
	public static final String PROTEINS_DB = "cpath2_proteins";
	public static final String PREMERGE_DB = "premergeDataSource";
	
	// PREMERGE_INDEX_DIR_VARIABLE value must match the one used in the 
	// cpath-importer, internalContext-premerge.xml!
	public static final String PREMERGE_INDEX_DIR_VARIABLE = "premerge.index.dir";
	
	public static final String MAPPING_DATA_DIR = "idmapping";
	
	public static final String WHOUSE_SEARCH_INDEX = "cpathwhouse";
	
	/*
	 * URI prefix for auto-generated/converted during the data import 
	 * and normalization utility class objects 
	 * (i.e., for xrefs, ChemicalStructure, etc.)
	 */
	public static final String CPATH_URI_PREFIX = "urn:pathwaycommons:";
	
	
	/**
	 * Gets the path to the directory with BridgeDb id-mapping files.
	 * 
	 * @return
	 */
	public static String getMappingDir() {
		return System.getenv(HOME_VARIABLE_NAME) 
			+ File.separator + MAPPING_DATA_DIR;
	}
}
