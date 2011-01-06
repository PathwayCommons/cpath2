package cpath.importer;

public interface Premerge {

	/**
	 * Called to start the premerge process.
	 */
	void premerge();
	
	/**
	 * Sets whether premerge database should be also created.
	 * 
	 * @param createDb
	 */
	void setCreateDb(boolean createDb);
	
	/**
	 * Sets a metadata identifier filter
	 * 
	 * @param identifier
	 */
	void setIdentifier(String identifier);
	
	/**
	 * Sets a metadata version filter
	 * 
	 * @param version
	 */
	void setVersion(String version);
}
