package cpath.fetcher.internal;

// imports
import cpath.warehouse.beans.Metadata;
import cpath.converter.Converter;
import cpath.fetcher.WarehouseDataService;

import org.biopax.paxtools.model.Model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.*;

import java.util.zip.GZIPInputStream;

/**
 * Warehouse Data service.  Retrieves protein and small molecule data on behalf of warehouse.
 */
@Service
public class WarehouseDataServiceImpl implements WarehouseDataService {

	// logger
    private static Log log = LogFactory.getLog(WarehouseDataServiceImpl.class);
    
    @Autowired
    ResourceLoader applicationContext; // it is!
    
    /**
     * (non-Javadoc)
     * @see cpath.fetcher.WarehouseDataService#storeWarehouseData(cpath.warehouse.beans.Metadata, org.biopax.paxtools.model.Model)
     */
    @Override
    public void storeWarehouseData(final Metadata metadata, final Model model) throws IOException {

		String urlStr = metadata.getURLToPathwayData();
		Resource resource = applicationContext.getResource(urlStr);

		// protein data comes zipped
		if (log.isInfoEnabled()) {
			log.info("getWarehouseData(), data is zip/gz, unzipping.");
		}
		convert(metadata, resource.getInputStream(), model);
    }

    /**
     * Given an InputStream, unzip into individual files and create EntityReference objects from each
	 * and place in model.
	 *
	 * @param metadata Metadata
     * @param fetchedData InputStream
     * @param model Model
     */
    private void convert(final Metadata metadata, final InputStream fetchedData, final Model model) throws IOException {

        GZIPInputStream zis = null;

		// create converter
        if(log.isInfoEnabled())
        	log.info("getting a converter with name: " 
				+ metadata.getConverterClassname());
		Converter converter = getConverter(metadata.getConverterClassname());
		if (converter == null) {
			// TDB: report failure
			log.fatal("could not create converter class " 
					+ metadata.getConverterClassname());
			return;
		}

        try {
            // create a zip input stream
			zis = new GZIPInputStream(new BufferedInputStream(fetchedData));
			if (log.isInfoEnabled()) {
				log.info("created gzip input stream: " + zis);
			}

			// create entity reference objects
			if (log.isInfoEnabled()) {
				log.info("creating EntityReference objects, provider: " +
						 metadata.getIdentifier() + " version: " + metadata.getVersion());
			}

			// hook into biopax converter for given provider
			converter.convert(zis, model);
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
    private static void closeQuietly(final InputStream zis) {
        try {
            zis.close();
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
	private static Converter getConverter(final String converterClassName) {
		try {
			Class<?> converterClass = Class.forName(converterClassName);
			return (Converter)converterClass.newInstance();
		}
		catch (Exception e) {
			log.error("could not create converter class " 
					+ converterClassName, e);
		}
		return null;
	}
}
