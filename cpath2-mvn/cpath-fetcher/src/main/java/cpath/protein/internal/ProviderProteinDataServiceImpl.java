package cpath.protein.internal;

// imports
import cpath.warehouse.beans.Metadata;
import cpath.common.FetcherHTTPClient;
import cpath.protein.ProviderProteinDataService;

import org.biopax.paxtools.model.level3.EntityReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedOutputStream;

import java.util.HashSet;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Provider Protein Data service.  Retrieves provider protein data.
 */
@Service
public final class ProviderProteinDataServiceImpl implements ProviderProteinDataService {

	// used in unzip method
	private static final int BUFFER = 2048;

	// logger
    private static Log log = LogFactory.getLog(ProviderProteinDataServiceImpl.class);

	// ref to FetcherHTTPClient
    private FetcherHTTPClient fetcherHTTPClient;

    /**
	 * Default Constructor.
	 */
	public ProviderProteinDataServiceImpl() {}

	/**
     * Constructor.
     * 
     * @param fetcherHTTPClient FetcherHTTPClient
     */
	public ProviderProteinDataServiceImpl(FetcherHTTPClient fetcherHTTPClient) {
		this.fetcherHTTPClient = fetcherHTTPClient;
	}

    /**
     * (non-Javadoc)
     * @see cpath.protein.ProviderProteinDataService#getProviderProteinData(cpath.warehouse.beans.Metadata)
     */
    @Override
    public Collection<EntityReference> getProviderProteinData(final Metadata metadata) throws IOException {

        Collection<EntityReference> toReturn = new HashSet<EntityReference>();

		String url = metadata.getURLToPathwayData();

		// protein data comes zipped
		log.info("getProviderPathwayData(), data is zip/gz, unzipping.");
		unzip(metadata, fetcherHTTPClient.getDataFromServiceAsStream(url), toReturn);
		fetcherHTTPClient.releaseConnection();

        // outta here
        return toReturn;
    }

    /**
     * Given an InputStream, unzip into individual files and create EntityReference objects from each
	 *
	 * @param metadata Metadata
     * @param fetchedData InputStream
	 * @param toReturn Collection<PathwayData> 
     */
    private void unzip(final Metadata metadata, final InputStream fetchedData, final Collection<EntityReference> toReturn) throws IOException {

        ZipInputStream zis = null;

        try {

            // create a zip intput stream
			zis = new ZipInputStream(new BufferedInputStream(fetchedData));

			// interate over zip entries
			ZipEntry entry = null;
            while ((entry = zis.getNextEntry()) != null) {

				log.info("Processing zip entry: " + entry.getName());

				// write file to buffered outputstream
				int count;
				byte data[] = new byte[BUFFER];
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				BufferedOutputStream dest = new BufferedOutputStream(bos, BUFFER);
				while ((count = zis.read(data, 0, BUFFER)) != -1) {
					dest.write(data, 0, count);
				}
				dest.flush();
				dest.close();

				// create entity reference objects
				log.info("unzip(), creating EntityReference objects, zip entry: " + entry.getName() +
						 " provider: " + metadata.getIdentifier() + " version: " + metadata.getVersion());

				// hook into biopax converter for given provider
				
				// add object to return collection
				//toReturn.add();
			}
        }
        catch (IOException e) {
            throw e;
        }
        finally {
            closeQuietly(zis);
        }
    }

   /**
    * Close the specified reader quietly.
    *
    * @param zis ZipInputStream
    */
    private static void closeQuietly(final ZipInputStream zis) {
    
        try {
            zis.close();
        }
        catch (Exception e) {
            // ignore
        }
    }

   /**
    * Close the specified reader quietly.
    *
    * @param zis ZipInputStream
    */
    private static void closeQuietly(final BufferedReader reader) {
    
        try {
            reader.close();
        }
        catch (Exception e) {
            // ignore
        }
    }
}
