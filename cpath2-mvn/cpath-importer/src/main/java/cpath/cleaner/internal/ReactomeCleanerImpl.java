package cpath.cleaner.internal;

// imports
import cpath.cleaner.Cleaner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.IOException;
import java.io.StringReader;
import java.io.BufferedReader;

/**
 * Implementation of Cleaner interface for HPRD ppi data.
 */
public class ReactomeCleanerImpl extends BaseCleanerImpl implements Cleaner {

	// logger
    private static Log log = LogFactory.getLog(ReactomeCleanerImpl.class);

    private static final String RDF_ID_REPLACEMENT = "urn:miriam:reactome_";
    private static final Pattern START_OF_BPE_REGEX = Pattern.compile("(\\s*)<bp:(\\w+) rdf:ID=\"(\\w+)\">");
    private static final Pattern START_OF_REACTOME_XREF = Pattern.compile("(\\s*)<bp:xref rdf:resource=\"#(REACT_\\d+(\\.\\d+)?)\" />");

	/**
	 * (non-Javadoc>
	 * @see cpath.cleaner.Cleaner#clean(java.lang.String)
	 */
	public String clean(final String pathwayData) {

        // the string buffer to return
        StringBuffer toReturn = new StringBuffer();

        // create a buffered reader to process pathawy data
        BufferedReader bufferedReader =
            new BufferedReader(new StringReader(pathwayData));

        try {
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {

                // do we have the start of a new bpe?
                Matcher startBPEMatcher = START_OF_BPE_REGEX.matcher(line);
                if (startBPEMatcher.find()) {
                
                    // figure out string which designates end of bpe
                    String bpeEnd = (startBPEMatcher.group(1) + "</bp:" + startBPEMatcher.group(2) + ">");
                    
                    // read bpe into string buffer
                    StringBuffer bpeBuffer = new StringBuffer(line + "\n");
                    while ((line = bufferedReader.readLine()) != null) {
                        bpeBuffer.append(line + "\n");
                        if (line.equals(bpeEnd)) {
                            break;
                        }
                    }

                    // process bpe
                    toReturn.append(processBPE(startBPEMatcher, bpeBuffer));
                }
                else {
                    toReturn.append(line + "\n");
                }
            }
        }
        catch (IOException e) {
            if (log.isInfoEnabled()) {
            	log.info("clean(), Exception thrown while cleaning pathway data, returning dirty data...");
			}
            return pathwayData;
        }

        // outta here
        return toReturn.toString();
    }

    /**
     * Given a complete biopax element:
     *
     * <bp:Complex rdf:ID="xxxx">
     * ...
     * ...
     * <bp:xref rdf:resource="#REACT_6602.2" />
     * </bp:Complex>
     *
     * replaces rdf:ID="xxxx" with rdf:ID="#REACT_6602.2"
     * if xref to REACT exists.
     * 
     * @param startBPEMatcher Matcher - used to capture <bp:Complex ...>
     * @param bpeBuffer StringBuffer - buffer contains entire bpe 
     * @return StringBuffer bpeBuffer with or without rdf:ID replacement
     */
    private static StringBuffer processBPE(Matcher startBPEMatcher, StringBuffer bpeBuffer) throws IOException {
        
        StringBuffer toReturn = new StringBuffer();
		BufferedReader bpeReader = new BufferedReader (new StringReader(bpeBuffer.toString()));

        // determine if reactome xref exists
        String line = null;
        boolean processedRDFId = false;
        while ((line = bpeReader.readLine()) != null) {

            // special processing for start of bpe
            if (!processedRDFId) {
                String reactomeXref = findReactomeXref(bpeBuffer);
                // if we have an xref to reactome, replace rdf id of bpe with reactome xref
                if (reactomeXref != null) {
                    String newRDF = (startBPEMatcher.group(1) + "<bp:" +
                                     startBPEMatcher.group(2) + " rdf:ID=\"" +
                                     RDF_ID_REPLACEMENT + reactomeXref + "\">");
                    toReturn.append(newRDF + "\n");
                }
                else {
                    toReturn.append(line + "\n");
                }
                processedRDFId = true;
            }
            else {
                // all other lines in bpe just get appended to return buffer
                toReturn.append(line + "\n");
            }
        }
        
        // outta here
        return toReturn;
    }

    /**
     * Given a complete biopax element:
     *
     * <bp:Complex rdf:ID="xxxx">
     * ...
     * ...
     * </bp:Complex>
     *
     * looks for <bp:xref rdf:resource="#REACT_XXXX.X" />
     * and if it exists, returns REACT_XXXX.X.
     * 
     * @param bpeBuffer StringBuffer - buffer contains entire bpe 
     * @return String REACT id or null
     */
    private static String findReactomeXref(StringBuffer bpeBuffer) throws IOException {

        BufferedReader bpeReader = new BufferedReader (new StringReader(bpeBuffer.toString()));
        
        // determine if reactome xref exists
        String line = null;
        while ((line = bpeReader.readLine()) != null) {
            
            // do we have the start of a reactome xref?
            Matcher startReactomeXrefMatcher = START_OF_REACTOME_XREF.matcher(line);
            if (startReactomeXrefMatcher.find()) {
                return startReactomeXrefMatcher.group(2);
            }
        }

        // outta here
        return null;
    }
}
