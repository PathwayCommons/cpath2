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

package cpath.importer.internal;

import java.awt.image.BufferedImage;
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
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import cpath.config.CPathSettings;
import cpath.importer.Cleaner;
import cpath.importer.Converter;
import cpath.importer.Fetcher;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;


/**
 * @author rodche, ben
 *
 */
final class FetcherImpl implements Fetcher
{
	// logger
    private static Log log = LogFactory.getLog(FetcherImpl.class);
	
    // some bits for metadata reading
    private static final int METADATA_IDENTIFIER_INDEX = 0;
    private static final int METADATA_NAME_INDEX = 1;
    private static final int METADATA_VERSION_INDEX = 2;
    private static final int METADATA_RELEASE_DATE_INDEX = 3;
    private static final int METADATA_DATA_URL_INDEX = 4;
    private static final int METADATA_HOMEPAGE_URL_INDEX = 5;
    private static final int METADATA_ICON_URL_INDEX = 6;
    private static final int METADATA_TYPE_INDEX = 7;
	private static final int METADATA_CLEANER_CLASS_NAME_INDEX = 8;
	private static final int METADATA_CONVERTER_CLASS_NAME_INDEX = 9;
    private static final int NUMBER_METADATA_ITEMS = 10;
	

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
	
	private boolean reUseFetchedDataFiles = true;
	
	
	/**
	 * Protected Constructor.
	 */
	FetcherImpl() {
		reUseFetchedDataFiles = true;
	}
	
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
                if("".equals(line.trim()))
                	continue;
                else if(line.trim().startsWith("#")) {
                	if(log.isInfoEnabled())
                    	log.info("readMetadata(), line: " + line);
                	continue; //ignore/skip parsing
                }
                	
                /* for now, assume line is delimited into 9 columns by '<br>';
                 * empty strings in the middle (the result of using <br><br>) and 
                 * trailing empty string after the last '<br>' (i.e., Converter 
                 * class name, if any), will be added to the tokens array as well.
                 */
                // TODO: update when data moved to, e.g., a wiki page; by the way, <br> is wrong html tag...
                String[] tokens = line.split("<br>",-1);
                
				if (log.isDebugEnabled()) {
					log.debug("readMetadata(), token size: " + tokens.length);
					for (String token : tokens) {
						log.debug("readMetadata(), token: " + token);
					}
				}

                assert tokens.length == NUMBER_METADATA_ITEMS : "readMetadata(): " +
                		"wrong number of columns, " + tokens.length + " instead of "
                		+ NUMBER_METADATA_ITEMS + ", in the metadata record: " + line;

				// convert version string to float
				String version = null;
				try {
					version = tokens[METADATA_VERSION_INDEX];
				}
				catch (NumberFormatException e) {
					log.error("readMetadata(), number format exception caught for provider: "
							+ tokens[METADATA_IDENTIFIER_INDEX] + " skipping");
					continue;
				}

				// get metadata type
				Metadata.METADATA_TYPE metadataType = Metadata.METADATA_TYPE.valueOf(tokens[METADATA_TYPE_INDEX]);

				// get icon data from service
				if(log.isInfoEnabled())
					log.info("readMetadata(): fetching icon data from: " 
						+ tokens[METADATA_ICON_URL_INDEX]);
				byte[] iconData = null;
				try {
					InputStream stream = LOADER.getResource(tokens[METADATA_ICON_URL_INDEX]).getInputStream();
					BufferedImage image = ImageIO.read(stream);
					if(image != null) {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ImageIO.write(image, "gif", baos);
						baos.flush();
						iconData = baos.toByteArray();
					}
				} catch (IOException e) {
					log.error("readMetadata(): Cannot load image from " 
							+  tokens[METADATA_ICON_URL_INDEX] + ". Skipping. " + e);
				}
					
				if (iconData == null) { 
					if(log.isInfoEnabled())
						log.info("readMetadata(): missing or unaccessible " +
							"data (icon) to create Metadata bean: iconData.");
					iconData = new byte[]{};
				}
					
				if(log.isDebugEnabled())
					log.debug("readMetadata(): make a Metadata bean.");

                // create a metadata bean
                Metadata metadata = new Metadata(
                		tokens[METADATA_IDENTIFIER_INDEX], 
                		tokens[METADATA_NAME_INDEX],
                		version, 
                		tokens[METADATA_RELEASE_DATE_INDEX],
                        tokens[METADATA_DATA_URL_INDEX], 
                        tokens[METADATA_HOMEPAGE_URL_INDEX], 
                        iconData,
						metadataType,
						tokens[METADATA_CLEANER_CLASS_NAME_INDEX], tokens[METADATA_CONVERTER_CLASS_NAME_INDEX]);
                
                
                
                
				if (log.isInfoEnabled()) {
					log.info("readMetadata(): adding Metadata: "
					+ "identifier=" + metadata.getIdentifier() 
					+ "; name=" + metadata.getName()
					+ "; version=" + metadata.getVersion()
					+ "; date/comment=" + metadata.getDescription()
					+ "; location=" + metadata.getUrlToData()
					+ "; icon=" + tokens[METADATA_ICON_URL_INDEX]
					+ "; type=" + metadata.getType()
					+ "; cleaner=" + metadata.getCleanerClassname() 
					+ "; converter=" + metadata.getConverterClassname());
				}
					
				// add metadata object toc collection we return
				toReturn.add(metadata);
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
		String url = "file://" + metadata.localDataFile();
		BufferedInputStream bis = new BufferedInputStream(LOADER.getResource(url).getInputStream());
		
		// pathway data is either owl, zip (multiple file entries allowed) or gz (single data entry)
		if(url.toLowerCase().endsWith(".gz")) {
			if(log.isInfoEnabled())
				log.info("getProviderPathwayData(): extracting data from gzip archive.");
			PathwayData pathwayData = readContent(metadata, new GZIPInputStream(bis));
			toReturn.add(pathwayData);
		} 
		else if(url.toLowerCase().endsWith(".zip")) {
			if(log.isInfoEnabled())
				log.info("getProviderPathwayData(): extracting data from zip archive.");
			toReturn = readContent(metadata, new ZipInputStream(bis));
		} else { // expecting BioPAX content (RDF+XML) 
			if(log.isInfoEnabled())
				log.info("getProviderPathwayData(): returning as is (supposed to be RDF+XML)");
			PathwayData pathwayData = readContent(metadata, bis);
			toReturn.add(pathwayData);
		}

        return toReturn;
    }

	
    /*
     * Reads the input stream content as PathwayData
     * @param inputStream plain text (uncompressed) data stream
     */
    private PathwayData readContent(Metadata metadata, final InputStream inputStream) 
    	throws IOException 
    {
		String fetchedData = readContent(inputStream);
		int idx = metadata.getUrlToData().lastIndexOf('/');
		String filename = metadata.getUrlToData().substring(idx+1); // not found (-1) gives entire string
		String digest = getDigest(fetchedData.getBytes());
		
		return new PathwayData(metadata.getIdentifier(), metadata.getVersion(),
				filename, digest, fetchedData.getBytes());
	}

    
    private String readContent(final InputStream inputStream) throws IOException 
    {
            BufferedReader reader = null;
    		StringBuilder toReturn = new StringBuilder();
            try {
                // we'd like to read lines at a time
                reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

                // are we ready to read?
                while (reader.ready()) {
                	// NEWLINE here is critical for the protein/molecule cleaner/converter!
                    toReturn.append(reader.readLine()).append(CPathSettings.NEWLINE);
    			}
    		}
            catch (IOException e) {
                throw e;
            }
            finally {
                closeQuietly(reader);
            }

    		return toReturn.toString();
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
            	String entryName = entry.getName();
            	if(log.isInfoEnabled())
            		log.info("Processing zip entry: " + entryName);

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

				String content = bos.toString("UTF-8");
				// quick fix: skip undesired entries
				if(entry.isDirectory() 
						|| !( content.contains("RDF")
								&& content.contains("biopax.org/release/biopax")
							)
					) 
				{
					if(log.isInfoEnabled())
	            		log.info("Skipping not BioPAX (owl) zip entry: " 
	            			+ entryName);
					continue;
				}
				
				// create digest
				String digest = getDigest(bos.toByteArray());

				if (digest != null) {
					// create pathway data object
					if(log.isInfoEnabled())
						log.info("unzip(), creating pathway data object, zip entry: " 
							+ entryName +
							" provider: " + metadata.getIdentifier() +
							" version: " + metadata.getVersion() +
							" digest: " + digest);
					PathwayData pathwayData = new PathwayData(metadata.getIdentifier(), 
						metadata.getVersion(), entryName, digest, content.getBytes());
				
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
	/**
	 * {@inheritDoc}
	 * 
	 * Note: this method is now called from {@link PremergeImpl}.
	 */
	@Override
    public void storeWarehouseData(final Metadata metadata, final Model model) 
		throws IOException 
	{
		//shortcut for other/system warehouse data (not to be converted to BioPAX)
		if(metadata.getConverterClassname() == null 
				|| metadata.getConverterClassname().isEmpty()) 
		{
			log.info("storeWarehouseData(..), skip (no need to clean/convert) for: "
				+ metadata.getIdentifier() + " version: " + metadata.getVersion());
			return;
		}
		
		
		// use the local file (MUST have been previously fetched!)
		String urlStr = "file://" + metadata.localDataFile();
		InputStream is = new BufferedInputStream(LOADER.getResource(urlStr).getInputStream());
		log.info("storeWarehouseData(..): input stream is now open for provider: "
			+ metadata.getIdentifier() + " version: " + metadata.getVersion());
		
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
					log.info("storeWarehouseData(..): processing zip entry: " 
						+ entry.getName());
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
				log.info("storeWarehouseData(..): not using un(g)zip " +
					"(cannot guess from the extension) for " + urlStr);
			}

			log.info("storeWarehouseData(..): creating EntityReference objects, " +
				"provider: " + metadata.getIdentifier() + " version: "
					+ metadata.getVersion());

			// hook into a cleaner for given provider
			// Try to instantiate the Cleaner (if any) sooner, and exit if it fails!
			String cl = metadata.getCleanerClassname();
			Cleaner cleaner = null;
			if(cl != null && cl.length()>0) {
				cleaner = ImportFactory.newCleaner(cl);
				if (cleaner == null) {
					log.error("storeWarehouseData(..): " +
						"failed to create the specified Cleaner: " + cl);
					return; // skip for this data entry and return before reading anything
				}
			} else {
				log.info("storeWarehouseData(..): no Cleaner class was specified; " +
					"continue converting...");
			}
			
			// read the entire data from the input stream to a text string
			String data = readContent(is);
			
			// run the cleaner, if any -
			if(cleaner != null) {
				log.info("storeWarehouseData(..): running the Cleaner: " + cl);	
				data = cleaner.clean(data);
			}
			
			// re-open a new input stream for the cleaned data
			is = new BufferedInputStream(new ByteArrayInputStream(data.getBytes("UTF-8")));
			
			// hook into a converter for given provider
			cl = metadata.getConverterClassname();
			Converter converter = null;
			if(cl != null && cl.length()>0) {
				converter = ImportFactory.newConverter(cl);
				if(converter != null) {
					log.info("storeWarehouseData(..): running " +
							"the BioPAX Converter: " + cl);	
					// create a new empty in-memory model
					Model inMemModel = BioPAXLevel.L3.getDefaultFactory().createModel();
					inMemModel.setXmlBase(model.getXmlBase());
					// convert data into that
					converter.setModel(inMemModel);
					converter.convert(is);
					//repair
					log.info("storeWarehouseData(..): Preparing just created " +
						metadata.getIdentifier() + " BioPAX Model to merging...");
					inMemModel.repair();
					// merging may take quite a time...
					log.info("storeWarehouseData(..): Persisting " +
						metadata.getIdentifier());
					model.merge(inMemModel);
				}
				else 
					log.error(("storeWarehouseData(..): failed to create " +
						"the Converter class: " + cl
							+ "; so skipping for this warehouse data..."));
			} else {
				log.info("storeWarehouseData(..): No Converter class was specified; " +
					"so nothing else left to do");
			}

			log.info("storeWarehouseData(..): Exitting.");
			
		} finally {
			closeQuietly(is);
		}
	}

	
	@Override
	public void fetchData(Metadata metadata) throws IOException {
		
		File dir = new File(CPathSettings.localDataDir());
		if(!dir.exists()) {
			dir.mkdir();
		}
		
		String localFileName = metadata.localDataFile();
		File localFile = new File(localFileName);
		if(reUseFetchedDataFiles && localFile.exists() && localFile.isFile()) {
			if(log.isInfoEnabled())
				log.info(metadata.getType() + " data : " + metadata.getIdentifier() 
					+ "." + metadata.getVersion() + " - found in "
					+ localFileName + ". Skip downloading.");
		} else {
			if(log.isInfoEnabled())
				log.info("Downloading " + metadata.getType() + " from " +
					metadata.getUrlToData() + " to " + localFileName);
		
			Resource resource = LOADER.getResource(metadata.getUrlToData());
			
			long size = 0; 
			if(resource.isReadable()) {
				size = resource.contentLength();
				if(log.isInfoEnabled())
					log.info(metadata.getUrlToData() + " content length= " + size);
			}
			
			if(size < 0) 
				size = 100 * 1024 * 1024 * 1024; // TODO (may be make it a parameter) max bytes = 100Gb
				
			ReadableByteChannel source = Channels.newChannel(resource.getInputStream());
			FileOutputStream dest = new FileOutputStream(localFileName);
			size = dest.getChannel().transferFrom(source, 0, size); // can throw runtime exceptions
			
			if(log.isInfoEnabled())
				log.info(size + " bytes downloaded from " + metadata.getUrlToData());
		}

	}

	
	/**
	 * Flags whether to reuse previously fetched data files,
	 * if exist in the special data sub-directory under cpath2 home,
	 * or always download and replace (from specified
	 * in the metadata locations). Existing files are detected
	 * and related to the corresponding metadata by file name, 
	 * which is auto-generated from the metadata identifier, version,
	 * and file extention (if present).
	 * 
	 * @return
	 */
	final boolean isReUseFetchedDataFiles() {
		return reUseFetchedDataFiles;
	}

	final void setReUseFetchedDataFiles(boolean reUseFetchedDataFiles) {
		this.reUseFetchedDataFiles = reUseFetchedDataFiles;
	}
		
}
