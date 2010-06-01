package cpath.fetcher.internal;

// imports
import cpath.warehouse.beans.Metadata;
import cpath.converter.Converter;
import cpath.fetcher.WarehouseDataService;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.*;

import java.util.zip.GZIPInputStream;

/**
 * Warehouse Data service.  Retrieves protein and small molecule data on behalf of warehouse.
 */
@Service
public final class WarehouseDataServiceImpl implements WarehouseDataService {

	// used in unzip method
	private static final int BUFFER = 2048;

	// logger
    private static Log log = LogFactory.getLog(WarehouseDataServiceImpl.class);

    @Autowired
    ApplicationContext applicationContext;
    
    /**
     * (non-Javadoc)
     * @see cpath.fetcher.WarehouseDataService#getWarehouseData(cpath.warehouse.beans.Metadata)
     */
    @Override
    public Model getWarehouseData(final Metadata metadata) throws IOException {

        Model toReturn = null;

		String urlStr = metadata.getURLToPathwayData();

		Resource resource = applicationContext.getResource(urlStr);
		// protein data comes zipped
		if(log.isInfoEnabled())
			log.info("getWarehouseData(), data is zip/gz, unzipping.");
		toReturn = convert(metadata, resource.getInputStream());
		if(log.isInfoEnabled())
			log.info("getWarehouseData(), return model: " + toReturn);

        // outta here
        return toReturn;
    }

    /**
     * Given an InputStream, unzip into individual files and create EntityReference objects from each
	 * and place in model.
	 *
	 * @param metadata Metadata
     * @param fetchedData InputStream
	 * @return model Model
     */
    private Model convert(final Metadata metadata, final InputStream fetchedData) throws IOException {

        Model toReturn = null;
        GZIPInputStream zis = null;

		// create converter
        if(log.isInfoEnabled())
        	log.info("unzip(), getting a converter with name: " 
				+ metadata.getConverterClassname());
        
		Converter converter = getConverter(metadata.getConverterClassname());
		if (converter == null) {
			// TDB: report failure
			log.fatal("unzip(), could not create converter class " 
					+ metadata.getConverterClassname());
			return null;
		}

        try {

            // create a zip input stream
			zis = new GZIPInputStream(new BufferedInputStream(fetchedData));
			if(log.isInfoEnabled()	)
				log.info("unzip(), created gzip input stream: " + zis);

			// write file to buffered output stream
			int count;
			byte data[] = new byte[BUFFER];
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			BufferedOutputStream dest = new BufferedOutputStream(bos, BUFFER);
			int total=0;
			while ((count = zis.read(data, 0, BUFFER)) != -1) {
				total+= count;
				if(log.isInfoEnabled()	)
					log.info("unzip(), read " + total + " bytes so far.");
				dest.write(data, 0, count);
			}
			dest.flush();
			dest.close();

			// create entity reference objects
			if(log.isInfoEnabled()	)
				log.info("unzip(), creating EntityReference objects, provider: " +
					 metadata.getIdentifier() + " version: " + metadata.getVersion());

			// hook into biopax converter for given provider
			toReturn = converter.convert(new ByteArrayInputStream(bos.toByteArray()), BioPAXLevel.L3);
        }
        catch (IOException e) {
            throw e;
        }
        finally {
            closeQuietly(zis);
        }
		// outta here
		return toReturn;
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
			Class<?> converterClass = Class.forName(converterClassName);
			return (Converter)converterClass.newInstance();
		}
		catch (Exception e) {
			log.error("unzip(), could not create converter class " 
					+ converterClassName, e);
		}
		return null;
	}
}
