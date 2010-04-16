package cpath.cleaner.internal;

// imports
import cpath.cleaner.Cleaner;

/**
 * Implementation of Cleaner interface for HPRD ppi data.
 */
public final class HPRDCleanerImpl extends BaseCleanerImpl implements Cleaner {

	/**
	 * (non-Javadoc>
	 * @see cpath.cleaner.Cleaner#clean(java.lang.String)
	 */
	public String clean(final String pathwayData) {

		// we want to add refType=identity to uniprot secondaryRef
		String toReturn = pathwayData.replaceAll("^(\\s*)<secondaryRef db=\"uniprot\" dbAc=\"(.*)\" id=\"(.*)\"\\/>\\s*$",
												 "$1<secondaryRef db=\"uniprot\" dbAc=\"$2\" id=\"$3\" refType=\"identity\"/>");

		// outta here
		return toReturn;
	}

}
