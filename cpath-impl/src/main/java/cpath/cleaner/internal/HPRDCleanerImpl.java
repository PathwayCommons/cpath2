package cpath.cleaner.internal;


/**
 * Implementation of Cleaner interface for HPRD ppi data.
 */
final class HPRDCleanerImpl extends BaseCleanerImpl {

	@Override
	public String clean(final String pathwayData) {

		// we want to add refType=identity to uniprot secondaryRef
		String toReturn = pathwayData.replaceAll("^(\\s*)<secondaryRef db=\"uniprot\" dbAc=\"(.*)\" id=\"(.*)\"\\/>\\s*$",
						 "$1<secondaryRef db=\"uniprot\" dbAc=\"$2\" id=\"$3\" refType=\"identity\"/>");

        // A quick and dirty fix for the latest export: HPRD_SINGLE_PSIMI_041210.xml
        // Duplicate id error due to a trailing space error
        toReturn = toReturn.replaceAll("\"07467 \"", "\"074670\"");

		return toReturn;
	}

}
