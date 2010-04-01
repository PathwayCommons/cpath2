package cpath.protein.internal;

// imports
import cpath.converter.Converter;
import cpath.warehouse.beans.Metadata;
import cpath.common.FetcherHTTPClient;
import cpath.protein.ProviderProteinDataService;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.level3.EntityReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedOutputStream;

import java.util.HashSet;
import java.util.Collection;
import java.util.zip.GZIPInputStream;
import java.net.URL;

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
    public Model getProviderProteinData(final Metadata metadata) throws IOException {

        Model toReturn = null;

		String urlStr = metadata.getURLToPathwayData();

		// protein data comes zipped
		log.info("getProviderPathwayData(), data is zip/gz, unzipping.");
		if (urlStr.startsWith("ftp://")) {
			URL url = new URL(urlStr);
			unzip(metadata, url.openConnection().getInputStream(), toReturn);
		}
		else {
			unzip(metadata, fetcherHTTPClient.getDataFromServiceAsStream(urlStr), toReturn);
			fetcherHTTPClient.releaseConnection();
		}

        // outta here
        return toReturn;
    }

    /**
     * Given an InputStream, unzip into individual files and create EntityReference objects from each
	 * and place in model.
	 *
	 * @param metadata Metadata
     * @param fetchedData InputStream
	 * @param model Model
     */
    private void unzip(final Metadata metadata, final InputStream fetchedData, Model model) throws IOException {

        GZIPInputStream zis = null;

		// create converter
		log.info("unzip(), getting a converter with name: " + metadata.getConverterClassname());
		Converter converter = getConverter(metadata.getConverterClassname());
		if (converter == null) {
			// TDB: report failure
			log.info("unzip(), could not create converter class " + metadata.getConverterClassname());
			return;
		}

        try {

            // create a zip input stream
			zis = new GZIPInputStream(new BufferedInputStream(fetchedData));
			log.info("unzip(), created gzip input stream: " + zis);

			// write file to buffered outputstream
			int count;
			byte data[] = new byte[BUFFER];
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			BufferedOutputStream dest = new BufferedOutputStream(bos, BUFFER);
			int total=0;
			while ((count = zis.read(data, 0, BUFFER)) != -1) {
				total+= count;
				log.info("unzip(), read " + total + " bytes so far.");
				dest.write(data, 0, count);
			}
			dest.flush();
			dest.close();

			// create entity reference objects
			log.info("unzip(), creating EntityReference objects, provider: " +
					 metadata.getIdentifier() + " version: " + metadata.getVersion());

			// hook into biopax converter for given provider
			converter.convert(new ByteArrayInputStream(bos.toByteArray()), BioPAXLevel.L3, model);
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
    private static void closeQuietly(final GZIPInputStream zis) {
    
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
	 * For the given converter class name,
	 * returns an instance of a class which
	 * implements the converter interface.
	 *
	 * @param converterClassName String
	 * @return Converter
	 */
	private Converter getConverter(final String converterClassName) {

		try {
			Class converterClass = getClass().forName(converterClassName);
			return (converterClass == null) ?
				null : (Converter)converterClass.newInstance();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
