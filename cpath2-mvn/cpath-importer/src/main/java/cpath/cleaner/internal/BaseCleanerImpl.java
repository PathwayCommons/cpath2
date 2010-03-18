package cpath.cleaner.internal;

// imports
import cpath.cleaner.Cleaner;

/**
 * Implementation of Cleaner class for Reactome pathway data.
 */
public class BaseCleanerImpl implements Cleaner {

	/**
	 * (non-Javadoc>
	 * @see cpath.importer.cleaner.Cleaner#clean(java.lang.String)
	 */
	public String clean(final String pathwayData) {
		return pathwayData;
	}
}
