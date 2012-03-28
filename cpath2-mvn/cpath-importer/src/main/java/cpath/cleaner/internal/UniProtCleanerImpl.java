package cpath.cleaner.internal;

// imports
import cpath.warehouse.beans.PathwayData;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of Cleaner interface for UniProt data.
 * Specifically, the class removes all shared accession numbers
 * between protein records.  The assumption is these shared accessions
 * are obsolete and have been left in the uniprot export for backward
 * compatability.  An example would be:
 *
 * ['A2A3T6', 'Q0VGC5', 'Q5VX65', 'Q5VX66', 'Q8IUU4']
 * between records: C1T9B_HUMAN, id: C1T9A_HUMAN
 *
 * This class works on UniProt exports of the form:
 * uniprot_sprot_xxxx.dat, where xxxx is species.
 *
 */
final class UniProtCleanerImpl extends BaseCleanerImpl {

    // delimiter between accessions
    private static final String AC_DELIMITER = "; ";

    // identifier for start of accessions line
    private static final String AC_PREFIX = "AC   ";
    
    // regex to capture protein identifier
    private static final String ID_REGEX = "^ID\\s*(\\w*).*$";
    private static Pattern pattern = Pattern.compile(ID_REGEX);

	// logger
    private static Log log = LogFactory.getLog(UniProtCleanerImpl.class);
    
	/**
	 * (non-Javadoc>
	 * @see cpath.importer.Cleaner#clean(PathwayData)
	 */
    @Override
	public String clean(final String uniProtData) {

        // string we will return
        String toReturn = uniProtData;
        
        if (log.isInfoEnabled()) {
        	log.info("clean(), starting...");
		}
        
        try {
            toReturn = cleanAccessions(uniProtData, populateAccessionsMap(uniProtData));
        }
        catch (IOException e) {
            throw new IllegalArgumentException("Error reading uniprot data");
        }

        if (log.isInfoEnabled()) {
        	log.info("clean(), done!");
		}
        
        // outta here
        return toReturn;
    }

    /**
     * For a given uniprot file, creates a map of accession (key) to list of protein id (values)
     * So, for each of these entries in the file:
     *
     * ID   BRCA1_HUMAN   Reviewed; 1863 AA.
     * AC   P38398; O15129; Q3LRJ0; Q7KYU9;
     *
     * A map would contain:
     *
     * P38398 -> BRCA1_HUMAN
     * O15129 -> BRCA1_HUMAN
     * Q3LRJ0 -> BRCA1_HUMAN
     * Q7KYU9 -> BRCA1_HUMAN
     *
     * @param uniprotData String
     * @return Map<String, List<String>>
     */
    private Map<String, List<String>> populateAccessionsMap(final String uniprotData) throws IOException {

        Map<String, List<String>> toReturn = new HashMap<String, List<String>>();

        // create a buffered reader
        BufferedReader bufferedReader = new BufferedReader(new StringReader(uniprotData));
        String line = bufferedReader.readLine();
        while (line != null) {
            // process each id
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                // grab the id
                String id = matcher.group(1);
                List<String> accessions = getAccessionsList(bufferedReader);
                // insert entry into map
                for (String accession : accessions) {
                    if (toReturn.containsKey(accession)) {
                        toReturn.get(accession).add(id);
                    }
                    else {
                        List<String> ids = new ArrayList<String>();
                        ids.add(id);
                        toReturn.put(accession, ids);
                    }
                }
            }
            // get ready for next loop
            line = bufferedReader.readLine();
        }

        // outta here
        return toReturn;
    }

    /**
     * For a given bufferedReader into a uniprot file, creates a list of accessions.  For example
     * if the buffered reader is pointing to the following:
     *
     * AC   P0CAP2; Q6EER8; Q6EES2; Q6EEV3; Q6EF00; Q6EF01; Q6EF02; Q6EF46;
     * AC   Q6EFN8; Q6EM48; Q6K046; Q6K050; 
     *
     * The following list is returned:
     *
     * [P0CAP2, Q6EER8, Q6EES2, Q6EEV3, Q6EF00, Q6EF01, Q6EF02, Q6EF46, Q6EFN8, Q6EM48, Q6K046, Q6K050]
     *
     * Its assumed that the bufferedReader argument is pointing to the line preceeding the accession list
     *
     * @param bufferedReader BufferedReader
     * @return List<String>
     */
    private List<String> getAccessionsList(BufferedReader bufferedReader) throws IOException {

        List<String> toReturn = new ArrayList<String>();

        String line = bufferedReader.readLine();
        while (line.startsWith(AC_PREFIX)) {
            String accessions = line.substring(AC_PREFIX.length());
            accessions = accessions.replaceAll(" ", "");
            toReturn.addAll(Arrays.asList(accessions.split(";")));
            // we may read a line after the last AC (usually DT)
            // in which case, we want to rollback the last readLine
            bufferedReader.mark(1024);
            line = bufferedReader.readLine();
        }
        // we've read past the last AC line, rollback
        bufferedReader.reset();

        // outta here
        return toReturn;
    }

    /**
     * Given a list of accessions, creates a string in the uniprot output format.
     * 
     * For example, if this is argument:
     *
     * [P0CAP2, Q6EER8, Q6EES2]
     *
     * The following is returned:
     *
     * AC   P0CAP2; Q6EER8; Q6EES2;
     *
     * @param accessionsList List<String>
     * @return String
     */
    private String getAccessionsListAsString(List<String> accessionsList) {
        
        StringBuffer toReturn = new StringBuffer(AC_PREFIX);

        for (String accession : accessionsList) {
            toReturn.append(accession + AC_DELIMITER);
        }

        // outta here
        return toReturn.toString().trim() + "\n";
    }

    /**
     * Given a string representing a uniprot export and a map containing accessions
     * to remove, method returns a uniprot export with desired accessions removed.
     *
     * As a side effect, protein records that have accessions spanning multiple lines
     * will have the accessions all concatenated on the same line.
     *
     * For example, the following input:
     *
     * AC   P0CAP2; Q6EER8; Q6EES2; Q6EEV3; Q6EF00;
     * AC   Q6EFN8; Q6EM48; Q6K046; Q6K050; 
     *
     * Will be converted to:
     *
     * AC   P0CAP2; Q6EER8; Q6EES2; Q6EEV3; Q6EF00; Q6EFN8; Q6EM48; Q6K046; Q6K050; 
     *
     * With any accessions removed determined by given map.
     *
     * @param uniprotData String
     * @param accessionsMap Map<String, List<String>>
     * @return String
     */
    private String cleanAccessions(final String uniprotData, Map<String, List<String>> accessionsMap) throws IOException {

        StringBuffer toReturn = new StringBuffer();

        // create a buffered reader
        BufferedReader bufferedReader = new BufferedReader(new StringReader(uniprotData));
        String line = bufferedReader.readLine();
        while (line != null) {
            toReturn.append(line + "\n");
            // process each id
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                String id = matcher.group(1);
                List<String> currentAccessions = getAccessionsList(bufferedReader);
                // check if any of these accessions should be removed
                List<String> accessionsToKeep = new ArrayList<String>(currentAccessions);
                for (String accession : currentAccessions) {
                    // get the list of protein id's associated w/this accession
                    // if accessions points to more than 1 protein record, clobber it
                    if (accessionsMap.get(accession).size() > 1) {
                        accessionsToKeep.remove(accession);
                    }
                }
                toReturn.append(getAccessionsListAsString(accessionsToKeep));
            }
            // get ready for next loop
            line = bufferedReader.readLine();
        }

        // outta here
        return toReturn.toString();
    }
}
