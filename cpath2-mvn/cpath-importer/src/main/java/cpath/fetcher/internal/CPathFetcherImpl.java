/**
 ** Copyright (c) 2010 Memorial Sloan-Kettering Cancer Center (MSKCC)
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
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import cpath.converter.Converter;
import cpath.converter.internal.BaseConverterImpl;
import cpath.fetcher.CPathFetcher;
import cpath.fetcher.WarehouseDataService;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;


/**
 * @author rodche, ben
 *
 */
public class CPathFetcherImpl implements WarehouseDataService, CPathFetcher
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
    
	// LOADER can handle file://, ftp://, http://  URL resources
	private static final ResourceLoader LOADER = new DefaultResourceLoader();
	
	
	@Override
    public Collection<Metadata> getMetadata(final String url) throws IOException 
    {
        Collection<Metadata> toReturn = new HashSet<Metadata>();

        // check args
        if (url == null) {
            throw new IllegalArgumentException("url must not be null");
        }

        // get data from service
		readMetadata(LOADER.getResource(url).getInputStream(), toReturn);

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
    private void readMetadata(final InputStream inputStream, 
    	final Collection<Metadata> toReturn) throws IOException 
    {
        BufferedReader reader = null;
        try {
            // we'd like to read lines at a time
            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

            // are we ready to read?
            while (reader.ready()) 
            {
                // grab a line
                String line = reader.readLine();
                if(log.isDebugEnabled())
                	log.debug("readFromService(), line: " + line);

                // for now assume line is delimited by '<br>'
                // TODO: update when data moved to wiki page
                String[] tokens = line.split("<br>");
				if (log.isDebugEnabled()) {
					log.debug("readFromService(), token size: " + tokens.length);
					for (String token : tokens) {
						log.debug("readFromService(), token: " + token);
					}
				}

                if (tokens.length == NUMBER_METADATA_ITEMS) {

					// convert version string to float
					String version = null;
					try {
						version = tokens[METADATA_VERSION_INDEX];
					}
					catch (NumberFormatException e) {
						log.error("readFromService(), number format exception caught for provider: "
								+ tokens[METADATA_IDENTIFIER_INDEX] + " skipping");
						continue;
					}

					// get metadata type
					Metadata.TYPE metadataType = Metadata.TYPE.valueOf(tokens[METADATA_TYPE_INDEX]);

					// get icon data from service
					if(log.isInfoEnabled())
						log.info("fetching icon data from: " + tokens[METADATA_ICON_URL_INDEX]);
					byte[] iconData = null;
					try {
						InputStream stream = LOADER.getResource(tokens[METADATA_ICON_URL_INDEX]).getInputStream();
						// we could simply read to byte[] directly, but let's do more interesting things - 
						BufferedImage image = ImageIO.read(stream);
						// TODO conversion of the icon into another format could easily happen here
						if(image != null)
							iconData = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
					} catch (IOException e) {
						log.error("Cannot load image from " +  tokens[METADATA_ICON_URL_INDEX] 
						                                              + ". Skipping. " + e);
					}
					
					if (iconData == null) { 
						if(log.isInfoEnabled())
							log.info("readFromService(), missing or unaccessible " +
								"data (icon) to create Metadata bean: iconData.");
						iconData = new byte[]{};
					}
					
					if(log.isDebugEnabled())
						log.debug("readFromService(), make a Metadata bean.");

                        // create a metadata bean
                    Metadata metadata = new Metadata(tokens[METADATA_IDENTIFIER_INDEX], tokens[METADATA_NAME_INDEX],
                                                         version, tokens[METADATA_RELEASE_DATE_INDEX],
                                                         tokens[METADATA_DATA_URL_INDEX], iconData, metadataType,
														 tokens[METADATA_CLEANER_CLASS_NAME_INDEX],
														 tokens[METADATA_CONVERTER_CLASS_NAME_INDEX]);
					if (log.isInfoEnabled()) {
						log.info("Adding Metadata : "
							+ metadata.getIdentifier() 
							+ "; " + metadata.getName()
							+ "; " + metadata.getVersion()
							+ "; " + metadata.getReleaseDate()
							+ "; " + metadata.getURLToData()
							+ "; " + tokens[METADATA_ICON_URL_INDEX]
							+ "; " + metadata.getType());
					}
					if (metadata.getType() == Metadata.TYPE.PSI_MI
							|| metadata.getType() == Metadata.TYPE.BIOPAX) {
						if (log.isInfoEnabled())
							log.info(metadata.getCleanerClassname());
					} else if (metadata.getType() == Metadata.TYPE.PROTEIN) {
						if (log.isInfoEnabled())
							log.info(metadata.getConverterClassname());
					}

					// add metadata object toc collection we return
					toReturn.add(metadata);
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


	@Override
    public Collection<PathwayData> getProviderPathwayData(final Metadata metadata) 
    	throws IOException 
    {
        Collection<PathwayData> toReturn = new HashSet<PathwayData>();
		String url = "file://" + metadata.getLocalDataFile();
		BufferedInputStream bis = new BufferedInputStream(LOADER.getResource(url).getInputStream());
		
		// pathway data is either owl, zip (multiple file entries allowed) or gz (single data entry)
		if (url.toLowerCase().endsWith(".owl")) {
			if(log.isInfoEnabled())
				log.info("getProviderPathwayData(): data is owl (returning as is)");
			PathwayData pathwayData = readContent(metadata, bis);
			toReturn.add(pathwayData);
		} 
		else if(url.toLowerCase().endsWith(".gz")) {
			if(log.isInfoEnabled())
				log.info("getProviderPathwayData(): extracting data from gzip archive.");
			PathwayData pathwayData = readContent(metadata, new GZIPInputStream(bis));
			toReturn.add(pathwayData);
		} 
		else if(url.toLowerCase().endsWith(".zip")) {
			if(log.isInfoEnabled())
				log.info("getProviderPathwayData(): extracting data from zip archive.");
			toReturn = readContent(metadata, new ZipInputStream(bis));
		} else {
			if(log.isWarnEnabled())
				log.warn("getProviderPathwayData(): data format is not supported: " + url);
		}

        return toReturn;
    }

	
    /*
     * @param inputStream plain text (uncompressed) data stream
     */
    private PathwayData readContent(Metadata metadata, final InputStream inputStream) 
    	throws IOException 
    {
        BufferedReader reader = null;
		StringBuffer toReturn = new StringBuffer();
        try {
            // we'd like to read lines at a time
            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

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

		String fetchedData = toReturn.toString();
		int idx = metadata.getURLToData().lastIndexOf('/');
		String filename = metadata.getURLToData().substring(idx+1); // not found (-1) gives entire string
		String digest = getDigest(fetchedData.getBytes());
		
		return new PathwayData(metadata.getIdentifier(), metadata.getVersion(), filename, digest, fetchedData);
	}

    
    /*
     * Given a zip stream, unzips it into individual files and creates PathwayData objects from each
     */
    private Collection<PathwayData> readContent(final Metadata metadata, 
    	final ZipInputStream zis) throws IOException 
    {
    	Collection<PathwayData> toReturn = new HashSet<PathwayData>();
    	
        try {
			// interate over zip entries
			ZipEntry entry = null;
            while ((entry = zis.getNextEntry()) != null) 
            {
            	if(log.isInfoEnabled())
            		log.info("Processing zip entry: " + entry.getName());

				// write file to buffered outputstream
				int count;
				byte data[] = new byte[BUFFER];
				// use string builder to get over heap issue when
				// converting from byte[] to string in PathwayData constructor
				// we'll continue using bos to easily get digest 
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				BufferedOutputStream dest = new BufferedOutputStream(bos, BUFFER);
				while ((count = zis.read(data, 0, data.length)) != -1) {
					dest.write(data, 0, count);
				}
				dest.flush();
				dest.close();

				// create digest
				String digest = getDigest(bos.toByteArray());

				if (digest != null) {
					// create pathway data object
					if(log.isInfoEnabled())
						log.info("unzip(), creating pathway data object, zip entry: " 
							+ entry.getName() +
							" provider: " + metadata.getIdentifier() +
							" version: " + metadata.getVersion() +
							" digest: " + digest);
					PathwayData pathwayData = new PathwayData(metadata.getIdentifier(), 
						metadata.getVersion(), entry.getName(), digest, bos.toString());
				
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
        
        return toReturn;
    }

   /*
    * Close the specified ZipInputStream quietly.
    */
    private static void closeQuietly(final InputStream zis) {
        try {
            zis.close();
        }
        catch (Exception e) {
           log.warn("zis.close() failed." + e);
        }
    }

   /*
    * Close the specified reader quietly.
    */
    private static void closeQuietly(final Reader reader) {
        try {
            reader.close();
        }
        catch (Exception e) {
        	log.warn("reader.close() failed." + e);
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
		// use the local file (previously fetched from metadata.urlTodata)
		String urlStr = "file://" + metadata.getLocalDataFile();
		InputStream is = new BufferedInputStream(LOADER.getResource(urlStr)
				.getInputStream());
		if (log.isInfoEnabled())
			log.info("Input stream is now open for provider: "
					+ metadata.getIdentifier() + " version: "
					+ metadata.getVersion());
		
		try {
			// get an input stream from a resource file that is either .gz or
			// .zip
			if (urlStr.endsWith(".gz")) {
				is = new GZIPInputStream(is);
			} else if (urlStr.endsWith(".zip")) {
				ZipEntry entry = null;
				ZipInputStream zis = new ZipInputStream(is);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				while ((entry = zis.getNextEntry()) != null) {
					if (log.isInfoEnabled())
						log.info("Processing zip entry: " + entry.getName());
					// write file to buffered output stream
					int count;
					byte data[] = new byte[BUFFER];
					BufferedOutputStream dest = new BufferedOutputStream(baos,
							BUFFER);
					while ((count = zis.read(data, 0, BUFFER)) != -1) {
						dest.write(data, 0, count);
					}
					dest.flush();
					dest.close();
				}
				
				is = new ByteArrayInputStream(baos.toByteArray());
				
			} else {
				if (log.isInfoEnabled())
					log.info("Not using un(g)zip (cannot guess " +
						"from the extension) for " + urlStr);
			}

			if (log.isInfoEnabled()) {
				log.info("Creating EntityReference objects, provider: "
						+ metadata.getIdentifier() + " version: "
						+ metadata.getVersion());
			}

			// hook into biopax converter for given provider
			if (log.isInfoEnabled())
				log.info("getting a converter with name: "
						+ metadata.getConverterClassname());
			Converter converter = getConverter(metadata.getConverterClassname());
			if (converter == null) {
				// TDB: report failure
				log.fatal("could not create converter class "
						+ metadata.getConverterClassname());
				return;
			}
			((BaseConverterImpl) converter).setModel(model);

			converter.convert(is);
			
		} finally {
			closeQuietly(is);
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
			return (Converter) converterClass.newInstance();
		}
		catch (Exception e) {
			log.error("could not create converter class " 
					+ converterClassName, e);
		}
		return null;
	}

	
	@Override
	public void fetchData(Metadata metadata) throws IOException {
		
		File dir = new File(metadata.getDataLocalDir());
		if(!(dir.exists() && dir.isDirectory())) {
			dir.mkdir();
		}
		
		String localFileName = metadata.getLocalDataFile();
		File localFile = new File(localFileName);
		if(localFile.exists() && localFile.isFile()) {
			if(log.isInfoEnabled())
				log.info(metadata.getType() + " data : " + metadata.getIdentifier() 
					+ "." + metadata.getVersion() + " - found in "
					+ localFileName + ". Skip downloading.");
		} else {
			if(log.isInfoEnabled())
				log.info("Downloading " + metadata.getType() + " from " +
					metadata.getURLToData() + " to " + localFileName);
		
			Resource resource = LOADER.getResource(metadata.getURLToData());
			long size = resource.contentLength();
			if(log.isInfoEnabled())
				log.info(metadata.getURLToData() + " content length= " + size);
			ReadableByteChannel source = Channels.newChannel(resource.getInputStream());
			FileOutputStream dest = new FileOutputStream(localFileName);
			size = dest.getChannel().transferFrom(source, 0, size); // can throw runtime exceptions
			if(log.isInfoEnabled())
				log.info(size + " bytes downloaded from " + metadata.getURLToData());
		}

		if(metadata.getType() == Metadata.TYPE.MAPPING) {
			storeMappingData(metadata);
		}
		
	}
	
	/*
	 * Currently, does not support gzip archives;
	 * also, only one .bridge or .pgdb file per archive 
	 * is expected (it ignores the rest); 
	 * custom user mapping files should be fetched unpacked...
	 * 
	 */
	private void storeMappingData(final Metadata metadata) throws IOException {
		if (metadata.getType() != Metadata.TYPE.MAPPING) {
			log.error("Not a Mapping data: " + metadata);
			return;
	  	}
		
		// use the local file (previously fetched from metadata.urlTodata)
		String urlStr = "file://" + metadata.getLocalDataFile();
		if (log.isInfoEnabled()) {
			log.info("Processing mapping data: "
					+ metadata.getIdentifier() + " file: "
					+ metadata.getLocalDataFile());
		}
		InputStream is = new BufferedInputStream(
				LOADER.getResource(urlStr).getInputStream());
		try {
			if (urlStr.endsWith(".zip")) {
				ZipEntry entry = null;
				ZipInputStream zis = new ZipInputStream(is);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				while ((entry = zis.getNextEntry()) != null) {
					if(entry.isDirectory())
						continue;
					// check ext
					String tmp = entry.getName();
					int idx = tmp.lastIndexOf('.');
					tmp = tmp.substring(idx+1);
					if(!"bridge".equalsIgnoreCase(tmp) 
						&& !"pgdb".equalsIgnoreCase(tmp)) {
						log.error("There in " + metadata.getURLToData() + 
							", " + entry.getName() + " is not " +
							"a '.bridge' or '.pgdb' file! Skipped.");
						continue;
					}
					
					/* Add .bridge extension.
					 * Later, when it comes to instantiate a mapper, 
					 * we should be able to say - 
					 * 
					 * Class.forName("org.bridgedb.rdb.IDMapperRdb");
  					 * IDMapper mapper = BridgeDb.connect("idmapper-pgdb:"+ metadata.getDataLocalDir()+ File.separator);
  					 * 
  					 * (or at least - +metadata.getLocalDataFile() + ".bridge")
					 */
					String fname = metadata.getLocalDataFile() + ".bridge"; // .zip.bridge - ok
					if (log.isInfoEnabled())
						log.info("Processing zip entry: " + entry.getName()
							+ "; expanding to: " + fname);
					BufferedOutputStream dest = new BufferedOutputStream(
						new FileOutputStream(fname), BUFFER);
					int count;
					byte data[] = new byte[BUFFER];
					while ((count = zis.read(data, 0, BUFFER)) != -1) {
						dest.write(data, 0, count);
					}
					dest.flush();
					dest.close();
					break; // at most one file is expected!
				}
			}
		} finally {
			closeQuietly(is);
		}
	}
		
}
