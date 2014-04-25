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
	
}
