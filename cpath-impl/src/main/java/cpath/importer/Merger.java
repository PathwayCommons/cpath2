package cpath.importer;


/**
 * Merger interface.  Class implementing this 
 * is responsible for merging pathway data
 * into the main cpath2 database.
 */
public interface Merger {	
	
	/**
	 * Starts the standard cpath2 merge process
	 * (merge all the data configured and pre-merged)
	 * 
	 */
	void merge();
	
}
