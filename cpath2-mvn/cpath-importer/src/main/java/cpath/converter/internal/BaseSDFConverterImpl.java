package cpath.converter.internal;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.*;

/**
 * Base Implementation of Converter interface for SDF (ChEBI & PubChem) data.
 */
public abstract class BaseSDFConverterImpl extends BaseConverterImpl {

	// logger
    private static Log log = LogFactory.getLog(BaseSDFConverterImpl.class);

	// the following are set by children
	private String ENTRY_START;
	private String ENTRY_END;
	private SDFUtil.SOURCE source;


	// testing code (inchi key is key, space delimited string of chebi ids)
	public Map<String, String> INCHI_KEY_MAP = new HashMap<String, String>();

	/**
	 * Constructor.
	 *
	 * @param source STFUtil.Source
	 * @param entryStart String
	 * @param entryEnd String
	 */
	public BaseSDFConverterImpl(SDFUtil.SOURCE source, String entryStart, String entryEnd) {

		// init members
		this.source = source;
		this.ENTRY_START = entryStart;
		this.ENTRY_END = entryEnd;
	}
	
	/**
	 * (non-Javadoc)
	 * @see cpath.converter.Converter#convert(java.io.InputStream, org.biopax.paxtools.model.Model)
	 */
	@Override
	public void convert(final InputStream is, final Model model) {

		// ref to reader here so, we can close in finally clause
        InputStreamReader reader = null;

        try {
			// setup the reader
            reader = new InputStreamReader(is);
            BufferedReader bufferedReader = new BufferedReader(reader);
            if (log.isInfoEnabled()) {
            	log.info("convert(), starting to read data...");
			}
			// read the file
            String line = bufferedReader.readLine();
            while (line != null) {
				// start of entry
                if (line.startsWith(ENTRY_START)) {
					StringBuffer entryBuffer = new StringBuffer(line + "\n");
					line = bufferedReader.readLine();
					while (line != null) {
						entryBuffer.append(line + "\n");
						// keep reading until we reach last modified
						if (line.startsWith(ENTRY_END)) {
							break;
						}
						line = bufferedReader.readLine();
					}
					// process entry
					processEntry(entryBuffer, model);
                }
                line = bufferedReader.readLine();
            }
        }
		catch (IOException e) {
			log.error("Failed", e);
		}
		finally {
			if (log.isInfoEnabled()) {
				log.info("convert(), closing reader.");
			}
            if (reader != null) {
				try {
					reader.close();
				}
				catch (Exception e) {
					// ignore
				}
            }
        }
		if (log.isInfoEnabled()) {
			log.info("convert(), exiting.");
		}

		/*
		// testing code - dump the chebi map 
		log.info("dumping inchi keys (values is rdf id that references inchi key)...");
		for (String key : INCHI_KEY_MAP.keySet()) {
			log.info(key + ": " + INCHI_KEY_MAP.get(key));
		}
		*/
    }

	/**
	 * Given a string buffer for a single ChEBI entry,
	 * create a BioPAX Entity reference.
	 *
	 * @param entryBuffer StringBuffer
	 * @throwns IOException
	 */
	private void processEntry(StringBuffer entryBuffer, Model model) throws IOException {

        if (log.isInfoEnabled()) {
        	log.info("processEntry(), calling sdfUtil.setSmallMoleculeReference.");
		}

		// create local model to add ER
		Model smallMoleculeReferenceModel = BioPAXLevel.L3.getDefaultFactory().createModel();

		// create STDUtil to do dirty work
		SDFUtil sdfUtil = new SDFUtil(source, smallMoleculeReferenceModel);

		// set the small molecule reference - gets added to model by sdfUtil
		sdfUtil.setSmallMoleculeReference(entryBuffer);

		// testing code: update testing map
		//updateTestingMap(smallMoleculeReferenceModel);

		// merge
		model.merge(smallMoleculeReferenceModel);
	}

	// testing code
	private void updateTestingMap(Model smallMoleculeReferenceModel) {

		// used below
		String[] inchiParts = null;
		String[] chebiOrPubchemParts = null;

		// get smallmolecules from model
		Set<SmallMoleculeReference> smrs =
			smallMoleculeReferenceModel.getObjects(SmallMoleculeReference.class);

		for (SmallMoleculeReference smr : smrs) {
			String rdf = smr.getRDFId();
			if (rdf.contains("urn:inchi")) {
				inchiParts = smr.getRDFId().split(":");
			}
			else {
				chebiOrPubchemParts = smr.getRDFId().split(":");
			}
		}

		if (inchiParts == null || chebiOrPubchemParts == null) {
			return;
		}

		if (INCHI_KEY_MAP.containsKey(inchiParts[2])) {
			String existingIds = INCHI_KEY_MAP.get(inchiParts[2]);
			INCHI_KEY_MAP.put(inchiParts[2], existingIds + chebiOrPubchemParts[3] + " ");
		}
		else {
			INCHI_KEY_MAP.put(inchiParts[2], chebiOrPubchemParts[3] + " ");
		}
	}
}
