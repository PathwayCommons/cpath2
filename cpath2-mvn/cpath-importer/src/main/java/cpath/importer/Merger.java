package cpath.importer;

import org.biopax.paxtools.model.Model;


/**
 * Merger interface.  Class implementing this 
 * is responsible for merging all pathway databases
 * into main pc database.
 */
public interface Merger {

	/**
	 * Called to start the standard Merge process
	 * (merge all the data as configured)
	 */
	void merge();
	
	/**
	 * Merges a new (provider's pathway) model
	 * into the current target model.
	 * @param pathwayModel - one-pathway or several interactions, model
	 */
	public void merge(Model pathwayModel);
	
	/**
	 * Sets whether provider specific premerge databases 
	 * should be used (otherwise, pathwayData.premergeData is used)
	 * 
	 * @param createDb
	 */
	void setUseDb(boolean createDb);
	
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
	
	
	/**
	 * Sets whether to force merge data that 
	 * failed to pass the validator (invalid)
	 * 
	 * @param forceInvalid
	 */
	public void setForce(boolean forceInvalid);
}
