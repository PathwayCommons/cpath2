package cpath.importer;

public interface Premerger {

	/**
	 * Called to start the premerge process.
	 */
	void premerge();
	
	
	/**
	 * Creates the BioPAX entity references warehouse.
	 */
	void buildWarehouse();
	
	
	/**
	 * Extracts id-mapping information (name/id -> primary id) 
	 * from the Warehouse entity references's xrefs to the mapping tables.
	 * 
	 * @param writeExcludedIdsToFile
	 */
	void updateIdMapping(boolean writeExcludedIdsToFile);
}
