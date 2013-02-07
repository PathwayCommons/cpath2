package cpath.importer;

public interface Premerge {

	/**
	 * Called to start the premerge process.
	 */
	void premerge();
	
	/**
	 * Creates or updates the id-mapping table(s).
	 */
	void updateMappingData();
}
