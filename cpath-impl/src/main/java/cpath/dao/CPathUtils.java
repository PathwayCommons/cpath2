/**
 * 
 */
package cpath.dao;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.biopax.paxtools.impl.BioPAXElementImpl;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import cpath.config.CPathSettings;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Content;

/**
 * @author rodche
 *
 */
public final class CPathUtils {
	// logger
    private static Logger LOGGER = LoggerFactory.getLogger(CPathUtils.class);
	
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
	private static final int METADATA_PUBMEDID_INDEX = 9;
	private static final int METADATA_AVAILABILITY_INDEX = 10;
    private static final int NUMBER_METADATA_ITEMS = 11;

	// used for md5sum display
	static final byte[] HEX_CHAR_TABLE = {
		(byte)'0', (byte)'1', (byte)'2', (byte)'3',
		(byte)'4', (byte)'5', (byte)'6', (byte)'7',
		(byte)'8', (byte)'9', (byte)'a', (byte)'b',
		(byte)'c', (byte)'d', (byte)'e', (byte)'f'
    }; 
    
	// LOADER can handle file://, ftp://, http://  PROVIDER_URL resources
	public static final ResourceLoader LOADER = new DefaultResourceLoader();
		
	
	private CPathUtils() {
		throw new AssertionError("Not instantiable");
	}

	
    /**
     * Deletes a directory and all files there.
     * 
     * @param path
     * @return
     */
    public static boolean deleteDirectory(File path) {
    	cleanupDirectory(path);
        return( path.delete() );
    }
 
    
    /**
     * Empties the directory.
     * 
     * @param path
     * @return
     */
    public static void cleanupDirectory(File path) {
        if( path.exists() ) {
          File[] files = path.listFiles();
          for(int i=0; i<files.length; i++) {
             if(files[i].isDirectory()) {
            	 cleanupDirectory(files[i]);
             }
             else {
               files[i].delete();
             }
          }
        }
    }
    
    
    /**
     *  For the given url, returns a collection of Metadata Objects.
     *
     * @param url String
     * @return Collection<Metadata>
     */
    public static Collection<Metadata> readMetadata(final String url)
    {
        // order of lines/records in the Metadata table does matter (since 2013/03);
		// so List is used here instead of HashSet
		List<Metadata> toReturn = new ArrayList<Metadata>();

        // check args
        if (url == null) {
            throw new IllegalArgumentException("url must not be null");
        }

        // get data from service
        BufferedReader reader = null;
        try {
            // we'd like to read lines at a time
            reader = new BufferedReader(new InputStreamReader(
            	LOADER.getResource(url).getInputStream(), "UTF-8"));

            // are we ready to read?
            while (reader.ready()) 
            {
                // grab a line
                String line = reader.readLine();
                if("".equals(line.trim()))
                	continue;
                else if(line.trim().startsWith("#")) {
					LOGGER.info("readMetadata(), ignored line: " + line);
                	continue; //ignore/skip parsing
                }
                	
                /* for now, assume line is delimited into 9 columns by '\t' (tab);
                 * empty strings in the middle (the result of using \t\t) and 
                 * trailing empty string after the last tabulation (i.e., Converter 
                 * class name, if any), will be added to the tokens array as well.
                 */
                String[] tokens = line.split("\t",-1);
                
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("readMetadata(), token size: " + tokens.length);
					for (String token : tokens) {
						LOGGER.debug("readMetadata(), token: " + token);
					}
				}

                assert tokens.length == NUMBER_METADATA_ITEMS : "readMetadata(): " +
                		"wrong number of columns, " + tokens.length + " instead of "
                		+ NUMBER_METADATA_ITEMS + ", in the metadata record: " + line;

				// get metadata type
				Metadata.METADATA_TYPE metadataType = Metadata.METADATA_TYPE.valueOf(tokens[METADATA_TYPE_INDEX]);
				
				LOGGER.debug("readMetadata(): make a Metadata bean.");

                // create a metadata bean
                Metadata metadata = new Metadata(
                		tokens[METADATA_IDENTIFIER_INDEX], 
                		tokens[METADATA_NAME_INDEX],
                		tokens[METADATA_DESCRIPTION_INDEX], 
                		tokens[METADATA_DATA_URL_INDEX],
                        tokens[METADATA_HOMEPAGE_URL_INDEX], 
                        tokens[METADATA_ICON_URL_INDEX],
                        metadataType,
						tokens[METADATA_CLEANER_CLASS_NAME_INDEX],
						tokens[METADATA_CONVERTER_CLASS_NAME_INDEX],
						tokens[METADATA_PUBMEDID_INDEX],		
                		tokens[METADATA_AVAILABILITY_INDEX]);
                
				if (LOGGER.isInfoEnabled()) {
					LOGGER.info("readMetadata(): adding Metadata: "
					+ "identifier=" + metadata.getIdentifier() 
					+ "; name=" + metadata.getName()
					+ "; date/comment=" + metadata.getDescription()
					+ "; location=" + metadata.getUrlToData()
					+ "; icon=" + metadata.getIconUrl()
					+ "; type=" + metadata.getType()
					+ "; cleaner=" + metadata.getCleanerClassname() 
					+ "; converter=" + metadata.getConverterClassname()
					+ "; pubmedId=" + metadata.getPubmedId() 
					+ "; availability=" + metadata.getAvailability()
					);
				}
					
				// add metadata object toc collection we return
				toReturn.add(metadata);
            } 
        } catch (java.io.UnsupportedEncodingException e) {
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(reader);
        }

        return toReturn;
    }


    /**
     * For the given Metadata, unpacks and reads the corresponding 
     * original zip data archive, creating new {@link Content} objects 
     * in the metadata's dataFile collection.
     * Skips for system files/directory entries.
     *
     * @see Metadata#getDataArchiveName()
	 * @param metadata Metadata
     * @throws RuntimeException if an IO error occurs
     */
    public static void analyzeAndOrganizeContent(final Metadata metadata) 
    {
		Collection<Content> contentCollection = new HashSet<Content>();
		ZipInputStream zis = null;
		try {
			zis = new ZipInputStream((metadata.getUrlToData()
				.startsWith("classpath:")) //a hack for easy junit tests
					? LOADER.getResource(metadata.getUrlToData()).getInputStream() 
						: new FileInputStream(metadata.getDataArchiveName()));		
			// interate over zip entries
			ZipEntry entry = null;
            while ((entry = zis.getNextEntry()) != null) 
            {
            	String entryName = entry.getName();
           		LOGGER.info("analyzeAndOrganizeContent(), processing zip entry: " + entryName);

				//skip some sys/tmp files (that MacOSX creates sometimes)
				if(entry.isDirectory() || entryName.contains("__MACOSX") 
						|| entryName.startsWith(".") || entryName.contains("/.") 
						|| entryName.contains("\\.")) 
				{
            		LOGGER.info("analyzeAndOrganizeContent(), skipped " + entryName);
					continue;
				}
				
				// create pathway data object
				LOGGER.info("analyzeAndOrganizeContent(), adding new Content: " 
					+ entryName + " of " + metadata.getIdentifier());
				Content content = new Content(metadata, entryName);
				// add object to return collection
				contentCollection.add(content);
				
				OutputStream gzos = new GZIPOutputStream(new FileOutputStream(content.originalFile()));
				copy(zis, gzos);
				gzos.close();
            }           
		} catch (IOException e) {
			throw new RuntimeException("analyzeAndOrganizeContent(), " +
					"failed reading from: " + metadata.getIdentifier() , e);
		} finally {
			closeQuietly(zis);
		}
		
		if(contentCollection != null && !contentCollection.isEmpty())
			metadata.getContent().addAll(contentCollection);
		else
			LOGGER.warn("analyzeAndOrganizeContent(), no data found for " + metadata);
    }
    
    /**
     * Uncompresses the zip input stream,
     * all entries if it's a multi-entry archive,
     * and writes to the output stream.
     * 
     * Skips for system files/directory entries.
     * 
     * Does not close the streams.
     * 
     * @param is
     * @param os
     */
    public static void unzip(ZipInputStream zis, OutputStream os) 
    {
		try {		
			// interate over zip entries
			ZipEntry entry = null;
            while ((entry = zis.getNextEntry()) != null) 
            {
            	String entryName = entry.getName();
				//skip some sys/tmp files (that MacOSX creates sometimes)
				if(entry.isDirectory() || entryName.contains("__MACOSX") 
						|| entryName.startsWith(".") || entryName.contains("/.") 
						|| entryName.contains("\\.")) 
				{
					continue;
				}
				
				copy(zis, os); //does not close os
            }           
		} catch (IOException e) {
			throw new RuntimeException("unzip(), failed", e);
		} finally {
			closeQuietly(os);
		}
    }

    
   /**
    * Close the InputStream quietly.
    * @param is
    */
    private static void closeQuietly(final InputStream is) {
    	try{is.close();}catch(Exception e){LOGGER.warn("is.close() failed." + e);}
    }

    /**
     * Close the OutputStream quietly.
     * @param os
     */
     private static void closeQuietly(final OutputStream os) {
         try{os.close();}catch(Exception e){LOGGER.warn("os.close() failed." + e);}
     }
    
   /**
    * Close the reader quietly.
    * @param reader
    */
    private static void closeQuietly(final Reader reader) {
    	try{reader.close();}catch(Exception e){LOGGER.warn("reader.close() failed." + e);}
    }
       
    
    /**
     * Writes or overwrites from the array to target file.
     * @param src
     * @param file
     * @throws RuntimeException when there was an IO problem
     */
    public static void write(byte[] src, String file) {
    	FileOutputStream os = null;
    	try {
    		os = new FileOutputStream(file);
    		os.write(src);
    		os.flush();
    	} catch (IOException e) {
    		throw new RuntimeException("write: failed writing byte[] to " 
    			+ " to " + file, e);
    	} finally {closeQuietly(os);}
    }

	public static void cleanupIndexDir(String db) {
		cleanupDirectory(new File(CPathSettings.getInstance().homeDir() + File.separator + db));
		LOGGER.info("Emptied the index dir:" + db);
	}

	
	/**
	 * Replaces the URI of a BioPAX object
	 * using java reflection. Normally, one should avoid this;
	 * please use when absolutely necessary and with great care. 
	 * 
	 * @param model
	 * @param el
	 * @param newRDFId
	 */
	public static  void replaceID(Model model, BioPAXElement el, String newRDFId) {
		if(el.getRDFId().equals(newRDFId))
			return; // no action required
		
		model.remove(el);
		try {
			Method m = BioPAXElementImpl.class.getDeclaredMethod("setRDFId", String.class);
			m.setAccessible(true);
			m.invoke(el, newRDFId);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		model.add(el);
	}
	
	
	/**
	 * Loads the BioPAX model from a Gzip archive 
	 * previously created by the same cpath2 instance.
	 * 
	 * @param biopaxModelName  - e.g., 'All', 'Warehouse', or a Metadata's standard name.
	 * @return big BioPAX model
	 */
	public static Model importFromTheArchive(String biopaxModelName) {

		final String archive = CPathSettings.getInstance().biopaxExportFileName(biopaxModelName); 
		
		Model model = null;

		try {
			//read from e.g. ..All.BIOPAX.owl.gz archive
			LOGGER.info("Loading the BioPAX Model from " + archive);
			model = (new SimpleIOHandler(BioPAXLevel.L3))
					.convertFromOWL(new GZIPInputStream(new FileInputStream(archive)));
		} 
		catch (IOException e) {
			LOGGER.error("Failed to import model from " + archive, e);
		}

		return model;
	}
	
	
	/**
	 * Downloads a file (content) from a URI
	 * and saves in the cpath2 home directory. The content
	 * can be anything, but only single-file GZIP archives 
	 * can be optionally expanded with this method, before saved
	 * (e.g., this is how we grab GeoIP GeoLiteCity database).
	 * 
	 * @param srcUrl remote URL
	 * @param destFile name or relative path and name
	 * @param unpack if true, expands the archive
	 * @param replace
	 * @return bytes saved or 0 if existed before file weren't replaced
	 * @throws RuntimeException when there was an IOException
	 */
	public static long download(String srcUrl, String destFile, 
			boolean unpack, boolean replace) {
		
		File localFile = new File(destFile);
		
		if(localFile.exists() && !replace) {
			LOGGER.info("Keep existing " + destFile);
			return 0L;
		}
		
		Resource resource = LOADER.getResource(srcUrl);
        long size = 0; 
        
        try {
        
        if(resource.isReadable()) {
        	size = resource.contentLength();
        	LOGGER.info(srcUrl + " content length= " + size);
        }       
        if(size < 0) 
        	size = 100 * 1024 * 1024 * 1024;
        
        //downoad to a tmp file
        ReadableByteChannel source = Channels.newChannel(resource.getInputStream());      
       	File tmpf = File.createTempFile("cpath2_", ".download");
       	tmpf.deleteOnExit();
        FileOutputStream dest = new FileOutputStream(tmpf);        
        size = dest.getChannel().transferFrom(source, 0, size);
        dest.close();
        LOGGER.info(size + " bytes downloaded from " + srcUrl);
        
        if(unpack) {
        	GZIPInputStream ginstream = new GZIPInputStream(new FileInputStream(tmpf));
        	FileOutputStream outstream = new FileOutputStream(localFile);
        	byte[] buf = new byte[1024]; 
        	int len;
        	while ((len = ginstream.read(buf)) > 0) 
        		outstream.write(buf, 0, len);
        	ginstream.close();
        	outstream.close();
        } else {
        	if(replace)
        		if(localFile.exists() && !localFile.delete())
            		throw new RuntimeException("Failed to delete old " 
            			+ localFile.getAbsolutePath());
        	if(!tmpf.renameTo(localFile))
        		throw new RuntimeException("Failed to move " 
        			+ tmpf.getAbsolutePath() + " to " + localFile.getAbsolutePath());
        }
        
        } catch(IOException e) {
        	throw new RuntimeException("download(). failed", e);
        }
              
        return size;
	}
	

	/**
	 * Reads from the input and writes to the output stream
	 * 
	 * @param is
	 * @param os
	 * @throws IOException
	 */
	public static void copy(InputStream is, OutputStream os) throws IOException {		
		IOUtils.copy(is, os);
		os.flush();
		//do not close streams	(can be re-used outside)
	}	
		
}
