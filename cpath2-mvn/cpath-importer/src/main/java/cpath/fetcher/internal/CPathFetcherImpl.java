/**
 ** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** both UofT and MSKCC have no obligations to provide maintenance, 
 ** support, updates, enhancements or modifications.  In no event shall
 ** UofT or MSKCC be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** UofT or MSKCC have been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this software; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA;
 ** or find it at http://www.fsf.org/ or http://www.gnu.org.
 **/

package cpath.fetcher.internal;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.Model;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import cpath.converter.Converter;
import cpath.fetcher.ProviderMetadataService;
import cpath.fetcher.ProviderPathwayDataService;
import cpath.fetcher.WarehouseDataService;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;

/**
 * @author rodche
 *
 */
public class CPathFetcherImpl implements ProviderMetadataService,
		ProviderPathwayDataService, WarehouseDataService 
{
	// logger
    private static Log log = LogFactory.getLog(CPathFetcherImpl.class);
	
    // some bits for metadata reading
    private static final int METADATA_IDENTIFIER_INDEX = 0;
    private static final int METADATA_NAME_INDEX = 1;
    private static final int METADATA_VERSION_INDEX = 2;
    private static final int METADATA_RELEASE_DATE_INDEX = 3;
    private static final int METADATA_DATA_URL_INDEX = 4;
    private static final int METADATA_ICON_URL_INDEX = 5;
    private static final int METADATA_TYPE_INDEX = 6;
	private static final int METADATA_CLEANER_CLASS_NAME_INDEX = 7;
	private static final int METADATA_CONVERTER_CLASS_NAME_INDEX = 8;
    private static final int NUMBER_METADATA_ITEMS = 9;
	

	// used in unzip method
	private static final int BUFFER = 2048;

	// used for md5sum display
	static final byte[] HEX_CHAR_TABLE = {
		(byte)'0', (byte)'1', (byte)'2', (byte)'3',
		(byte)'4', (byte)'5', (byte)'6', (byte)'7',
		(byte)'8', (byte)'9', (byte)'a', (byte)'b',
		(byte)'c', (byte)'d', (byte)'e', (byte)'f'
    }; 
    
	
	private static final ResourceLoader LOADER = new DefaultResourceLoader();
	
	
	/* (non-Javadoc)
	 * @see cpath.fetcher.ProviderMetadataService#getProviderMetadata(java.lang.String)
	 */
	@Override
    public Collection<Metadata> getProviderMetadata(final String url) throws IOException {

        Collection<Metadata> toReturn = new HashSet<Metadata>();

        // check args
        if (url == null) {
            throw new IllegalArgumentException("url must not be null");
        }

        // get data from service
		readFromService(LOADER.getResource(url).getInputStream(), toReturn);

        // outta here
        return toReturn;
    }

    /**
     * Populates a collection of metadata objects given an input stream
	 *
     * @param inputStream InputStream
	 * @param toReturn Collection<Metadata>
	 * @param throws IOException
     */
    private void readFromService(final InputStream inputStream, final Collection<Metadata> toReturn) throws IOException {

        BufferedReader reader = null;
        try {

            // we'd like to read lines at a time
            reader = new BufferedReader(new InputStreamReader(inputStream));

            // are we ready to read?
            while (reader.ready()) 
            {
                // grab a line
                String line = reader.readLine();
                log.info("readFromService(), line: " + line);

                // for now assume line is delimited by '<br>'
                // TODO: update when data moved to wiki page
                String[] tokens = line.split("<br>");
				log.info("readFromService(), token size: " + tokens.length);
				for (String token : tokens) {
					log.info("readFromService(), token: " + token);
				}

                if (tokens.length == NUMBER_METADATA_ITEMS) {

					// convert version string to float
					Float version = null;
					try {
						version = new Float(tokens[METADATA_VERSION_INDEX]);
					}
					catch (NumberFormatException e) {
						log.info("readFromService(), number format exception caught for provider: " + tokens[METADATA_IDENTIFIER_INDEX] + " skipping");
						continue;
					}

					// get metadata type
					Metadata.TYPE metadataType = Metadata.TYPE.valueOf(tokens[METADATA_TYPE_INDEX]);

					// get icon data from service
					log.info("fetching icon data from: " + tokens[METADATA_ICON_URL_INDEX]);
					InputStream stream = LOADER.getResource(tokens[METADATA_ICON_URL_INDEX]).getInputStream();
					// we could simply read to byte[] directly, but let's do more interesting things - 
					BufferedImage image = ImageIO.read(stream);
					// TODO conversion of the icon into another format could easily happen here
					byte[] iconData = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();

                    if (iconData != null) {

						log.info("readFromService(), we have enough data to make a Metadata bean.");

                        // create a metadata bean
                        Metadata metadata = new Metadata(tokens[METADATA_IDENTIFIER_INDEX], tokens[METADATA_NAME_INDEX],
                                                         version, tokens[METADATA_RELEASE_DATE_INDEX],
                                                         tokens[METADATA_DATA_URL_INDEX], iconData, metadataType,
														 tokens[METADATA_CLEANER_CLASS_NAME_INDEX],
														 tokens[METADATA_CONVERTER_CLASS_NAME_INDEX]);
                        log.info(metadata.getIdentifier());
                        log.info(metadata.getName());
                        log.info(metadata.getVersion());
                        log.info(metadata.getReleaseDate());
                        log.info(metadata.getURLToPathwayData());
                        log.info(tokens[METADATA_ICON_URL_INDEX]);
                        log.info(metadata.getType());
						if (metadata.getType() == Metadata.TYPE.PSI_MI ||
							metadata.getType() == Metadata.TYPE.BIOPAX) {
							log.info(metadata.getCleanerClassname());
						}
						else if (metadata.getType() == Metadata.TYPE.PROTEIN) {
							log.info(metadata.getConverterClassname());
						}

                        // add metadata object toc collection we return
                        toReturn.add(metadata);
                    }
					else {
						log.info("readFromService(), missing data (icon) to create Metadata bean: iconData.");
					}
                }
            }
        }
        catch (java.io.UnsupportedEncodingException e) {
        }
        catch (IOException e) {
            throw e;
        }
        finally {
            closeQuietly(reader);
        }
    }


	/* (non-Javadoc)
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
			String fetchedData = readFromService(LOADER.getResource(url).getInputStream());
			int idx = url.lastIndexOf("/");
			if(idx < 0) idx = 0; // '/' not found, - use entire string
			String filename = url.substring(idx);
			String digest = getDigest(fetchedData.getBytes());
			PathwayData pathwayData = new PathwayData(metadata.getIdentifier(), 
					metadata.getVersion(), filename, digest, fetchedData);
			toReturn.add(pathwayData);
		}
		else {
			log.info("getProviderPathwayData(), data is zip/gz, unzipping.");
			unzip(metadata, LOADER.getResource(url).getInputStream(), toReturn);
		}

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
            while ((entry = zis.getNextEntry()) != null) 
            {
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
					PathwayData pathwayData = new PathwayData(metadata.getIdentifier(), 
						metadata.getVersion(), entry.getName(), digest, stringBuilder.toString());
				
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
 
    
	/* (non-Javadoc)
	 * @see cpath.fetcher.WarehouseDataService#storeWarehouseData(cpath.warehouse.beans.Metadata, org.biopax.paxtools.model.Model)
	 */
	@Override
    public void storeWarehouseData(final Metadata metadata, final Model model) 
		throws IOException 
	{
		String urlStr = metadata.getURLToPathwayData();

		// protein data comes zipped
		if (log.isInfoEnabled()) {
			log.info("getWarehouseData(), data is zip/gz, unzipping.");
		}
		convert(metadata, LOADER.getResource(urlStr).getInputStream(), model);
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