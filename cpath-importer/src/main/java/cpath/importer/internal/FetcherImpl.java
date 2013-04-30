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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import cpath.config.CPathSettings;
import cpath.importer.Fetcher;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;


/**
 * @author rodche, ben
 *
 */
public final class FetcherImpl implements Fetcher
{
	// logger
    private static Log log = LogFactory.getLog(FetcherImpl.class);
	
    // some bits for metadata reading
    private static final int METADATA_IDENTIFIER_INDEX = 0;
    private static final int METADATA_NAME_INDEX = 1;
    private static final int METADATA_DESCRIPTION_INDEX = 2;
    private static final int METADATA_DATA_URL_INDEX = 3;
    private static final int METADATA_HOMEPAGE_URL_INDEX = 4;
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
    
	// LOADER can handle file://, ftp://, http://  PROVIDER_URL resources
	private static final ResourceLoader LOADER = new DefaultResourceLoader();
	
	private boolean reUseFetchedDataFiles = true;
	
	
	/**
	 * Constructor.
	 * 
	 * @param reUseFetchedDataFiles whether to reuse previously fetched data files.
	 * 
	 */
	public FetcherImpl(boolean reUseFetchedDataFiles) {
		this.reUseFetchedDataFiles = reUseFetchedDataFiles;
	}

	
	@Override
    public Collection<Metadata> readMetadata(final String url) throws IOException 
    {
        // order of lines/records in the Metadata table does matter (since 2013/03);
		// so List is used here instead of HashSet
		List<Metadata> toReturn = new ArrayList<Metadata>();

        // check args
        if (url == null) {
            throw new IllegalArgumentException("url must not be null");
        }

        // get data from service
		readMetadata(LOADER.getResource(url).getInputStream(), toReturn);

        return toReturn;
    }

    /**
     * Populates the ordered list of metadata objects 
     * given the metadata file input stream
	 *
     * @param inputStream InputStream
	 * @param toReturn
	 * @param throws IOException
     */
    private void readMetadata(final InputStream inputStream, 
    	final List<Metadata> toReturn) throws IOException 
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
                	
                /* for now, assume line is delimited into 9 columns by '\t' (tab);
                 * empty strings in the middle (the result of using \t\t) and 
                 * trailing empty string after the last tabulation (i.e., Converter 
                 * class name, if any), will be added to the tokens array as well.
                 */
                String[] tokens = line.split("\t",-1);
                
				if (log.isDebugEnabled()) {
					log.debug("readMetadata(), token size: " + tokens.length);
					for (String token : tokens) {
						log.debug("readMetadata(), token: " + token);
					}
				}

                assert tokens.length == NUMBER_METADATA_ITEMS : "readMetadata(): " +
                		"wrong number of columns, " + tokens.length + " instead of "
                		+ NUMBER_METADATA_ITEMS + ", in the metadata record: " + line;


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
                		tokens[METADATA_DESCRIPTION_INDEX], 
                		tokens[METADATA_DATA_URL_INDEX],
                        tokens[METADATA_HOMEPAGE_URL_INDEX], 
                        iconData, 
                        metadataType,
						tokens[METADATA_CLEANER_CLASS_NAME_INDEX],
						tokens[METADATA_CONVERTER_CLASS_NAME_INDEX]);
           
                
				if (log.isInfoEnabled()) {
					log.info("readMetadata(): adding Metadata: "
					+ "identifier=" + metadata.getIdentifier() 
					+ "; name=" + metadata.getName()
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
    public void readPathwayData(final Metadata metadata) 
    	throws IOException 
    {
        Collection<PathwayData> pathwayDataCollection = new HashSet<PathwayData>();
		String url = "file://" + metadata.localDataFile();
		BufferedInputStream bis = new BufferedInputStream(LOADER.getResource(url).getInputStream());
		
		// pathway data is either owl, zip (multiple files allowed!) or gz (single data entry only)
		if(url.toLowerCase().endsWith(".gz")) {
			if(log.isInfoEnabled())
				log.info("getProviderPathwayData(): extracting data from gzip archive.");
			PathwayData pathwayData = readContent(metadata, new GZIPInputStream(bis));
			pathwayDataCollection.add(pathwayData);
		} 
		else if(url.toLowerCase().endsWith(".zip")) {
			if(log.isInfoEnabled())
				log.info("getProviderPathwayData(): extracting data from zip archive.");
			pathwayDataCollection = readZipContent(metadata, new ZipInputStream(bis));
		} else { // expecting BioPAX content (RDF+XML) 
			if(log.isInfoEnabled())
				log.info("getProviderPathwayData(): returning as is (supposed to be RDF+XML)");
			PathwayData pathwayData = readContent(metadata, bis);
			pathwayDataCollection.add(pathwayData);
		}
		
		metadata.getPathwayData().addAll(pathwayDataCollection);
    }

	
    /**
     * Reads the input stream content as PathwayData
     * 
     * @param inputStream plain text (uncompressed) data stream
     * @return
     * @throws IOException
     */
    private PathwayData readContent(Metadata metadata, final InputStream inputStream) 
    	throws IOException 
    {
        BufferedReader reader = null;
		StringBuilder sbuff = new StringBuilder();
		final String NEWLINE = System.getProperty ( "line.separator" );
        try {
            // we'd like to read lines at a time
            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            // are we ready to read?
            while (reader.ready()) {
            	// NEWLINE here is critical for the protein/molecule cleaner/converter
                sbuff.append(reader.readLine()).append(NEWLINE);
			}
        } finally { closeQuietly(reader); }

        String fetchedData = sbuff.toString();
		
		int idx = metadata.getUrlToData().lastIndexOf('/');
		String filename = metadata.getUrlToData().substring(idx+1); // not found (-1) gives entire string
		
		return new PathwayData(metadata, filename, fetchedData.getBytes());
	}

        
    /**
     * Given a zip stream, unzips it into individual 
     * files and creates PathwayData objects from each
     * 
     * @param metadata
     * @return
     * @throws IOException
     */
    private Collection<PathwayData> readZipContent(final Metadata metadata, final ZipInputStream zis) 
    		throws IOException 
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
				
				// create pathway data object
				if(log.isInfoEnabled())
					log.info("unzip(), creating pathway data object, zip entry: " 
						+ entryName + " provider: " + metadata.getIdentifier());
				PathwayData pathwayData = new PathwayData(metadata, entryName, content.getBytes());
				
				// add object to return collection
				toReturn.add(pathwayData);
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

    
   /**
    * Close the specified ZipInputStream quietly.
    * 
    * @param zis
    */
    private static void closeQuietly(final InputStream zis) {
        try {
            zis.close();
        }
        catch (Exception e) {
           log.warn("zis.close() failed." + e);
        }
    }

    
   /**
    * Close the specified reader quietly.
    * @param reader
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
	 * @return
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
	 * @return
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
					+ " - found in " + localFileName + ". Skip downloading.");
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
				size = 100 * 1024 * 1024 * 1024;
				
			ReadableByteChannel source = Channels.newChannel(resource.getInputStream());
			FileOutputStream dest = new FileOutputStream(localFileName);
			size = dest.getChannel().transferFrom(source, 0, size); // can throw runtime exceptions
			
			if(log.isInfoEnabled())
				log.info(size + " bytes downloaded from " + metadata.getUrlToData());
			dest.close();
		}

	}
		
}
