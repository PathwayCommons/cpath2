package cpath.merge;

// imports
import cpath.warehouse.beans.Metadata;

/**
 * Merger interface.  Class implementing this 
 * is responsible for merging all pathway databases
 * into main pc database.
 */
public interface Merger {

	/**
	 * Called to start the Merge process.
	 */
	void merge();
}
