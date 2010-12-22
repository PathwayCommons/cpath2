package cpath.converter.internal;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cpath.dao.PaxtoolsDAO;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Base Implementation of Converter interface for SDF (ChEBI & PubChem) data.
 */
public abstract class BaseSDFConverterImpl extends BaseConverterImpl {
	
	private static final String PROCESS_SDF = "PROCESS_SDF";
	private static final String PROCESS_OBO = "PROCESS_OBO";

	private static final String SDF_ENTRY_START = "M  END";
	private static final String SDF_ENTRY_END = "$$$$";
	
	private static final String CHEBI_OBO_ENTRY_START = "[Term]";
	private static final String CHEBI_OBO_ENTRY_END = "";
	
	// some statics to identify names methods
	protected static final String DISPLAY_NAME = "DISPLAY_NAME";
	protected static final String STANDARD_NAME = "STANDARD_NAME";
	protected static final String ADDITIONAL_NAME = "ADDITIONAL_NAME";

	// some statics to identify secondary id delimiters
	protected static final String COLON_DELIMITER = ":";
	protected static final String EQUALS_DELIMITER = "=";
	
	 // loader can handle file://, ftp://, http://  URL resources
	private static final ResourceLoader LOADER = new DefaultResourceLoader();
	
	// url to chebi obo file - set by ChEBI subclass
	private String CHEBI_OBO_FILE_URL;
	
	// chebi obo file path/name
	private static final String CHEBI_OBO_FILE;
	static {
		CHEBI_OBO_FILE =
			System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + "chebi.obo";
	}
	
	// logger
    private static Log log = LogFactory.getLog(BaseSDFConverterImpl.class);
    
    // OBO converter
    private ChEBIOBOConverterImpl oboConverter;
    
	public BaseSDFConverterImpl() {
		this(null, null);
	}
	
	/**
	 * Constructor.
	 *
	 * @param model to merge converted data to
	 */
	public BaseSDFConverterImpl(Model model, String chebiOBOFileURL) {
		super(model);
		this.oboConverter = new ChEBIOBOConverterImpl(model, factory);
		this.CHEBI_OBO_FILE_URL = chebiOBOFileURL;
	}
	
	/**
	 * Creates a new small molecule 
	 * reference (and other related elements in it) 
	 * from the SDF entry.
	 * 
	 * @param entry
	 * @return TODO
	 * @throws IOException
	 */
	protected abstract SmallMoleculeReference
	buildSmallMoleculeReference(StringBuffer entry) throws IOException;
	
	@Override
	public void convert(final InputStream is) {
		
		// first convert given SDF input stream and store SM in warehouse
		convert(is, PROCESS_SDF, SDF_ENTRY_START, SDF_ENTRY_END);
		
		// Note - we are only converting ChEBI now, so assume OBO processing required
		InputStream oboIS = null;
		try {
			oboIS = getChEBIOBOInputStream();
			if (oboIS == null && log.isInfoEnabled()) {
				log.info("convert(): chebi-obo file is null, cannot create hierarchical relationships!");
			}
			else {
				if (oboConverter.getModel() == null) {
					oboConverter.setModel(model);
				}
				convert(oboIS, PROCESS_OBO, CHEBI_OBO_ENTRY_START, CHEBI_OBO_ENTRY_END);
			}
		}
		catch (IOException e) {
			log.error(e);
		}
		finally {
			if (oboIS != null) {
				try {
					oboIS.close();
				}
				catch (Exception e) {
					// ignore
				}
	           }
		}
		
		if (log.isInfoEnabled()) {
			log.info("convert(), exiting.");
		}
	}
	
	/**
	 * Utility function that helps convert both SDF and OBO files.
	 *  
	 * @param is InputStream
	 * @param whatEntryToProcess String
	 * @param entryStart String
	 * @param entryEnd String
	 */
	private void convert(final InputStream is, String whatEntryToProcess, String entryStart, String entryEnd) {

		// ref to reader here so, we can close in finally clause
        InputStreamReader reader = null;

        try {
			// setup the reader
            reader = new InputStreamReader(is, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(reader);
            if (log.isInfoEnabled()) {
            	log.info("convert(), starting to read data...");
			}
			// read the file
            String line = bufferedReader.readLine();
            while (line != null) {
				// start of entry
                if (line.startsWith(entryStart)) {
					StringBuffer entryBuffer = new StringBuffer(line + "\n");
					line = bufferedReader.readLine();
					while (line != null) {
						entryBuffer.append(line + "\n");
						// keep reading until we reach last modified
						if (whatEntryToProcess.equals(PROCESS_SDF) && line.startsWith(entryEnd)) {
							break;
						}
						else if (whatEntryToProcess.equals(PROCESS_OBO) && line.isEmpty()) {
							break;
						}
						line = bufferedReader.readLine();
					}
					if (whatEntryToProcess.equals(PROCESS_SDF)) {
						processSDFEntry(entryBuffer);
					}
					else if (whatEntryToProcess.equals(PROCESS_OBO)) {
						processOBOEntry(entryBuffer);
					}
                }
                line = bufferedReader.readLine();
            }
        }
		catch (IOException e) {
			log.error(e);
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
		
    }

	/**
	 * Given a string buffer for a single SDF entry,
	 * create a BioPAX Entity reference.
	 *
	 * @param entryBuffer StringBuffer
	 * @throws IOException
	 */
	private void processSDFEntry(StringBuffer entryBuffer) throws IOException { 

        if (log.isDebugEnabled()) {
        	log.debug("calling processSDFEntry()");
        }
        // build a new SMR with its dependent elements
		SmallMoleculeReference smr = buildSmallMoleculeReference(entryBuffer);
		if (smr != null) {
            if(model instanceof PaxtoolsDAO) {
            	((PaxtoolsDAO) model).merge(smr);
            }
            else {
            	// make a self-consistent sub-model from the smr
            	MERGER.merge(model, smr);
            }
		}
	}

	/**
	 * Given a string buffer for a single OBO entry, create proper relationships
	 * between SMR within the warehouse.
	 * 
	 * @param entryBuffer String Buffer
	 * @throws IOException
	 */
	private void processOBOEntry(StringBuffer entryBuffer) throws IOException {
		
		if (log.isDebugEnabled()) {
			log.debug("calling processOBOEntry()");
		}
		
		// create obo converter
		oboConverter.processOBOEntry(entryBuffer);
	}
	
	/**
	 * Method to download and return an InputStream to the OBO file.
	 * @return InputStream
	 * @throws IOException
	 */
	private InputStream getChEBIOBOInputStream() throws IOException {
		
		// sanity check
		if (CHEBI_OBO_FILE_URL == null) {
			return null;
		}
		
		Resource resource = LOADER.getResource(CHEBI_OBO_FILE_URL);
		long size = resource.contentLength();
		if(log.isInfoEnabled()) {
			log.info(CHEBI_OBO_FILE_URL + " content length= " + size);
		}
		ReadableByteChannel source = Channels.newChannel(resource.getInputStream());
		FileOutputStream dest = new FileOutputStream(CHEBI_OBO_FILE);
		size = dest.getChannel().transferFrom(source, 0, size); // can throw runtime exceptions
		if(log.isInfoEnabled()) {
			log.info(size + " bytes downloaded from " + CHEBI_OBO_FILE_URL);
		}

		// outta here
		return new FileInputStream(CHEBI_OBO_FILE);
	}
}
