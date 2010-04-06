package cpath.cleaner.internal;

// imports
import cpath.cleaner.Cleaner;

/**
 * Implementation of Cleaner interface for use when data
 * does not need to be cleaned.
 */
public class BaseCleanerImpl implements Cleaner {

	/**
	 * (non-Javadoc>
	 * @see cpath.cleaner.Cleaner#clean(java.lang.String)
	 */
	public String clean(final String pathwayData) {
		return pathwayData;
	}
}
