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
	 * Merges a new single (provider's pathway) model
	 * into the existing target model.
	 * 
	 * @param pathwayModel - one-pathway or several interactions, model
	 */
	public void merge(Model pathwayModel);
}
