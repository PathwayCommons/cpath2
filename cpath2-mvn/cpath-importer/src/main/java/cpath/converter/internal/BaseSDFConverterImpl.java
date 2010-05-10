package cpath.converter.internal;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level2.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.BioPAXElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.*;
import java.net.URLEncoder;

/**
 * Base Implementation of Converter interface for SDF (ChEBI & PubChem) data.
 */
public abstract class BaseSDFConverterImpl extends BaseConverterImpl {

	// logger
    private static Log log = LogFactory.getLog(BaseSDFConverterImpl.class);

	// the following are set by children
	private String NAMESPACE_PREFIX;
	private String ENTRY_START;
	private String ENTRY_END;
	private SDFUtil.SOURCE source;

	// ref to bp level
	private BioPAXLevel bpLevel;

	// ref to bp model
	private Model bpModel;

	// ref to stdf util
	private SDFUtil sdfUtil;

	/**
	 * Constructor.
	 *
	 * @param source STFUtil.Source
	 * @param namespacePrefix String
	 * @param entryStart String
	 * @param entryEnd String
	 */
	public BaseSDFConverterImpl(SDFUtil.SOURCE source, String namespacePrefix, String entryStart, String entryEnd) {

		// init members
		this.source = source;
		this.NAMESPACE_PREFIX = namespacePrefix;
		this.ENTRY_START = entryStart;
		this.ENTRY_END = entryEnd;
	}
	
	/**
	 * (non-Javadoc)
	 * @see cpath.converter.Converter#convert(java.io.InputStream, org.biopax.paxtools.model.BioPXLevel)
	 */
	@Override
	public Model convert(final InputStream is, BioPAXLevel level) {

		// sanity check
		if (level != BioPAXLevel.L3) {
            throw new IllegalArgumentException("BioPAX level: " + level + " is not supported.");
		}

		// init members
		this.bpLevel = level;

		// create the model
		createBPModel();

		// create an sdfUtil class
		sdfUtil = new SDFUtil(source, bpModel);

		// ref to reader here so, we can close in finally clause
        InputStreamReader reader = null;

		// create a model
        if (log.isInfoEnabled()) {
        	log.info("convert(), creating Biopax Model.");
		}
	

        try {
			// setup the reader
            reader = new InputStreamReader(is);
            BufferedReader bufferedReader = new BufferedReader(reader);
            if(log.isInfoEnabled()) {
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
					processEntry(entryBuffer);
                }
                line = bufferedReader.readLine();
            }
            if (log.isInfoEnabled()) {
            	log.info("convert(), no. of elements created: " + bpModel.getObjects().size());
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

		// outta here
		return bpModel;
    }

	/**
	 * Given a string buffer for a single ChEBI entry,
	 * create a BioPAX Entity reference.
	 *
	 * @param entryBuffer StringBuffer
	 * @throwns IOException
	 */
	private void processEntry(StringBuffer entryBuffer) throws IOException {

        if (log.isInfoEnabled()) {
        	log.info("processEntry(), calling sdfUtil.setSmallMoleculeReference.");
		}

		// set the small molecule reference and add to model
		sdfUtil.setSmallMoleculeReference(entryBuffer);
	}

	/**
	 * Creates a paxtools model based on level passed to convert.
	 */
	private void createBPModel() {

		// create the model
		if (bpLevel == BioPAXLevel.L3) {
			bpModel = BioPAXLevel.L3.getDefaultFactory().createModel();
		}

		// setup base
		Map<String, String> nsMap = bpModel.getNameSpacePrefixMap();
		nsMap.put("", NAMESPACE_PREFIX);
	}
}
