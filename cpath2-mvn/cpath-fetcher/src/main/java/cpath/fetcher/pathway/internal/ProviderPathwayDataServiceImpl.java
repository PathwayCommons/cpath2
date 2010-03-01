package cpath.fetcher.pathway.internal;

// imports
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;
import cpath.fetcher.common.ServiceReader;
import cpath.fetcher.common.FetcherHTTPClient;
import cpath.fetcher.pathway.ProviderPathwayDataService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedOutputStream;

import java.util.HashSet;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Provider Metadata service.  Retrieves provider metadata.
 */
@Service
public final class ProviderPathwayDataServiceImpl implements ProviderPathwayDataService {

	// used in unzip method
	private static final int BUFFER = 2048;

	// logger
    private static Log log = LogFactory.getLog(ProviderPathwayDataServiceImpl.class);

	// ref to FetcherHTTPClient
    private FetcherHTTPClient fetcherHTTPClient;

	// are we fetching an owl file (as opposed to zip/gz)
	private boolean isOWL;

	/**
     * Constructor.
     * 
     * @param fetcherHTTPClient FetcherHTTPClient
     */
	public ProviderPathwayDataServiceImpl(FetcherHTTPClient fetcherHTTPClient) {
		this.fetcherHTTPClient = fetcherHTTPClient;
	}

    /**
     * (non-Javadoc)
     * @see cpath.fetcher.pathway.ProviderPathwayDataService#getProviderPathwayData(cpath.warehouse.beans.Metadata)
     */
    @Override
    public Collection<PathwayData> getProviderPathwayData(final Metadata metadata) throws IOException {

        Collection<PathwayData> toReturn = new HashSet<PathwayData>();

		String url = metadata.getURLToPathwayData();

		// set isOWL
		isOWL = (url.endsWith(".owl") || url.endsWith(".OWL"));

        // get data from service
		byte[] fetchedData = fetcherHTTPClient.getDataFromService(url);

		// pathway data is either owl or zip/gz
		if (isOWL && fetchedData != null) {
			log.info("getProviderPathwayData(), data is owl, directly returning.");
			String digest = getDigest(fetchedData);
			PathwayData pathwayData = new PathwayData(metadata.getIdentifier(), metadata.getVersion(), digest, new String(fetchedData));
			toReturn.add(pathwayData);
		}
		else {
			log.info("getProviderPathwayData(), data is zip/gz, unzipping.");
			unzip(metadata, fetchedData, toReturn);
		}

        // outta here
        return toReturn;
    }

    /**
     * Given a byte[], unzip into individual files and creates PathwayData objects from each
	 *
	 * @param metadata Metadata
     * @param fetchedData byte[]
	 * @param toReturn Collection<PathwayData> 
     */
    private void unzip(final Metadata metadata, final byte[] fetchedData, final Collection<PathwayData> toReturn) throws IOException {

        ZipInputStream zis = null;


        try {

            // create a zip intput stream
			zis = new ZipInputStream(new ByteArrayInputStream(fetchedData));

			// interate over zip entries
			ZipEntry entry = null;
            while ((entry = zis.getNextEntry()) != null) {

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

				// create digest
				String digest = getDigest(bos.toByteArray());

				if (digest != null) {

					// create pathway data object
					log.info("unzip(), creating pathway data object, provider: " + metadata.getIdentifier() +
							 " version: " + metadata.getVersion() +
							 " digest: " + digest);
					PathwayData pathwayData = new PathwayData(metadata.getIdentifier(), metadata.getVersion(), digest, bos.toString());
				
					// add object to return collection
					toReturn.add(pathwayData);
				}
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
	 * Given the following string, computes an MD5 digest.
	 *
	 * @param data byte[]
	 * @return String
	 */
	private String getDigest(byte[] data) {

		java.security.MessageDigest digest = null;
		try {
			digest = java.security.MessageDigest.getInstance("MD5");
			return new String(digest.digest());
		}
		catch (java.security.NoSuchAlgorithmException e) {
			return null;
		}
	}
}
