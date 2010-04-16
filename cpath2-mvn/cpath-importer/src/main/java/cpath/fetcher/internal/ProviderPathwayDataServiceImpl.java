package cpath.fetcher.internal;

// imports
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;
import cpath.fetcher.FetcherHTTPClient;
import cpath.fetcher.ProviderPathwayDataService;

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
 * Provider PathwayData service.  Retrieves provider pathway data.
 */
@Service
public final class ProviderPathwayDataServiceImpl implements ProviderPathwayDataService {

	// used in unzip method
	private static final int BUFFER = 2048;

	// used for md5sum display
	static final byte[] HEX_CHAR_TABLE = {
		(byte)'0', (byte)'1', (byte)'2', (byte)'3',
		(byte)'4', (byte)'5', (byte)'6', (byte)'7',
		(byte)'8', (byte)'9', (byte)'a', (byte)'b',
		(byte)'c', (byte)'d', (byte)'e', (byte)'f'
    }; 

	// logger
    private static Log log = LogFactory.getLog(ProviderPathwayDataServiceImpl.class);

	// ref to FetcherHTTPClient
    private FetcherHTTPClient fetcherHTTPClient;

    /**
	 * Default Constructor.
	 */
	public ProviderPathwayDataServiceImpl() {}

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
     * @see cpath.fetcher.ProviderPathwayDataService#getProviderPathwayData(cpath.warehouse.beans.Metadata)
     */
    @Override
    public Collection<PathwayData> getProviderPathwayData(final Metadata metadata) throws IOException {

        Collection<PathwayData> toReturn = new HashSet<PathwayData>();

		String url = metadata.getURLToPathwayData();

		// set isOWL
		boolean isOWL = (url.endsWith(".owl") || url.endsWith(".OWL"));

		// pathway data is either owl or zip/gz
		if (isOWL) {
			log.info("getProviderPathwayData(), data is owl, directly returning.");
			String fetchedData = readFromService(fetcherHTTPClient.getDataFromServiceAsStream(url));
			String filename = url.substring(url.lastIndexOf("/"));
			String digest = getDigest(fetchedData.getBytes());
			PathwayData pathwayData = new PathwayData(metadata.getIdentifier(), metadata.getVersion(), filename, digest, fetchedData);
			toReturn.add(pathwayData);
		}
		else {
			log.info("getProviderPathwayData(), data is zip/gz, unzipping.");
			unzip(metadata, fetcherHTTPClient.getDataFromServiceAsStream(url), toReturn);
		}
		fetcherHTTPClient.releaseConnection();

        // outta here
        return toReturn;
    }

    /**
     * Given an input stream, returns a string.
	 *
     * @param inputStream InputStream
	 * @return String
	 * @throws IOException
     */
    private String readFromService(final InputStream inputStream) throws IOException {

        BufferedReader reader = null;
		StringBuffer toReturn = new StringBuffer();

        try {

            // we'd like to read lines at a time
            reader = new BufferedReader(new InputStreamReader(inputStream));

            // are we ready to read?
            while (reader.ready()) {
                toReturn.append(reader.readLine());
			}
		}
        catch (IOException e) {
            throw e;
        }
        finally {
            closeQuietly(reader);
        }

		// outta here
		return toReturn.toString();
	}

    /**
     * Given an InputStream, unzip into individual files and creates PathwayData objects from each
	 *
	 * @param metadata Metadata
     * @param fetchedData InputStream
	 * @param toReturn Collection<PathwayData> 
     */
    private void unzip(final Metadata metadata, final InputStream fetchedData, final Collection<PathwayData> toReturn) throws IOException {

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
				// use string builder to get over heap issue when
				// converting from byte[] to string in PathwayData constructor
				// we'll continue using bos to easily get digest
				StringBuilder stringBuilder = new StringBuilder(); 
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				BufferedOutputStream dest = new BufferedOutputStream(bos, BUFFER);
				while ((count = zis.read(data, 0, BUFFER)) != -1) {
					dest.write(data, 0, count);
					// we assume encoding is UTF8, which is why we can append byte[], not char[]
					stringBuilder.append(data);
				}
				dest.flush();
				dest.close();

				// create digest
				String digest = getDigest(bos.toByteArray());

				if (digest != null) {

					// create pathway data object
					log.info("unzip(), creating pathway data object, zip entry: " + entry.getName() +
							 " provider: " + metadata.getIdentifier() +
							 " version: " + metadata.getVersion() +
							 " digest: " + digest);
					PathwayData pathwayData = new PathwayData(metadata.getIdentifier(), metadata.getVersion(), entry.getName(), digest, stringBuilder.toString());
				
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
			digest.reset();
			return getHexString(digest.digest(data));
		}
		catch (java.security.NoSuchAlgorithmException e) {
			return null;
		}
		catch (java.io.UnsupportedEncodingException e) {
			return null;
		}
	}

	/**
	 * Converts byte[] to displayable string.
	 *
	 * @param raw byte[]
	 * @return String
	 * @throws java.io.UnsupportedEncodingException
	 */
	public static String getHexString(byte[] raw) throws java.io.UnsupportedEncodingException {

        byte[] hex = new byte[2 * raw.length];
        int index = 0;
        for (byte b : raw) {
            int v = b & 0xFF;
            hex[index++] = HEX_CHAR_TABLE[v >>> 4];
            hex[index++] = HEX_CHAR_TABLE[v & 0xF];
        }
        return new String(hex, "ASCII").toUpperCase();
	}
}
