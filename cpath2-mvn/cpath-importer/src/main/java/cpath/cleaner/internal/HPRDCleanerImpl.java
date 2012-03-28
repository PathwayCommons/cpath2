package cpath.cleaner.internal;

// imports
import cpath.warehouse.beans.PathwayData;

/**
 * Implementation of Cleaner interface for HPRD ppi data.
 */
final class HPRDCleanerImpl extends BaseCleanerImpl {

	/**
	 * (non-Javadoc>
	 * @see cpath.importer.Cleaner#clean(PathwayData)
	 */
	@Override
	public String clean(final String pathwayData) {

		// we want to add refType=identity to uniprot secondaryRef
		String toReturn = pathwayData.replaceAll("^(\\s*)<secondaryRef db=\"uniprot\" dbAc=\"(.*)\" id=\"(.*)\"\\/>\\s*$",
						 "$1<secondaryRef db=\"uniprot\" dbAc=\"$2\" id=\"$3\" refType=\"identity\"/>");

		// outta here
		return toReturn;
	}

}
