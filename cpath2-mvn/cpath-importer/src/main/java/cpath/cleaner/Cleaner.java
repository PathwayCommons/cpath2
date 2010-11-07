package cpath.cleaner;

// imports

/**
 * Cleaners clean original biopax or psimi:
 * - remove or replace short labels
 * - update refTypeValues
 */
public interface Cleaner {

	/**
	 * Cleans the given pathway data.
	 *
	 * @param pathwayData String
	 * @return String
	 */
	String clean(final String pathwayData);
}
