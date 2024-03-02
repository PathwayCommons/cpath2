package cpath.cleaner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

import cpath.service.api.Cleaner;


/**
 * Implementation of Cleaner interface for HPRD ppi data.
 */
final class HPRDCleaner implements Cleaner {

	public void clean(InputStream data, OutputStream cleanedData) {
		//HPRD data is less than 2Gb (max String length)
		Scanner sc = new Scanner(data);
		StringBuilder sb = new StringBuilder();
		while (sc.hasNextLine())
			sb.append(sc.nextLine());
		sc.close(); sc = null;

		String pathwayDataString = sb.toString();
		// we want to add refType=identity to uniprot secondaryRef
		pathwayDataString = pathwayDataString.replaceAll(
				"^(\\s*)<secondaryRef db=\"uniprot\" dbAc=\"(.*)\" id=\"(.*)\"\\/>\\s*$",
				"$1<secondaryRef db=\"uniprot\" dbAc=\"$2\" id=\"$3\" refType=\"identity\"/>");

		// A quick and dirty fix for the latest export: HPRD_SINGLE_PSIMI_041210.xml
		// Duplicate id error due to a trailing space error
		pathwayDataString = pathwayDataString.replaceAll("\"07467 \"", "\"074670\"");

		try {
			cleanedData.write(pathwayDataString.getBytes());
			cleanedData.close();
		} catch (IOException e) {
			throw new RuntimeException("Failed writing to output stream", e);
		}
	}

}
