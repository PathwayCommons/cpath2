// $Id$
//------------------------------------------------------------------------------
/** Copyright (c) 2010 Memorial Sloan-Kettering Cancer Center.
 **
 ** This library is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** Memorial Sloan-Kettering Cancer Center
 ** has no obligations to provide maintenance, support,
 ** updates, enhancements or modifications.  In no event shall
 ** Memorial Sloan-Kettering Cancer Center
 ** be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** Memorial Sloan-Kettering Cancer Center
 ** has been advised of the possibility of such damage.  See
 ** the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this library; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 **/
package cpath.admin;

import static cpath.config.CPathSettings.*;
import cpath.config.CPathSettings;
import cpath.dao.*;
import cpath.importer.Fetcher;
import cpath.importer.Merger;
import cpath.importer.Premerger;
import cpath.importer.internal.FetcherImpl;
import cpath.importer.internal.MergerImpl;
import cpath.importer.internal.PremergeImpl;
import cpath.service.ErrorResponse;
import cpath.service.OutputFormat;
import cpath.service.internal.BiopaxConverter;
import cpath.service.jaxb.*;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;

import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.validator.api.Validator;
import org.biopax.validator.api.beans.ValidatorResponse;
import org.biopax.validator.api.ValidatorUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import static cpath.service.OutputFormat.*;

/**
 * Class which provides cpath2 command line
 * access for the system administrators and 
 * user scripts.
 */
public final class Admin {
	private static final Log LOG = LogFactory.getLog(Admin.class);

    // Cmd Enum
    public static enum Cmd {
        // command types
    	CREATE_DATABASES("-create-db"),
        FETCH_METADATA("-fetch-metadata"),
		FETCH_DATA("-fetch-data"),
		CREATE_WAREHOUSE("-create-warehouse"),
		UPDATE_MAPPING("-update-mapping"),
		PREMERGE("-premerge"),
		MERGE("-merge"),
    	CREATE_INDEX("-create-index"),
        CREATE_BLACKLIST("-create-blacklist"),
        CREATE_DOWNLOADS("-create-downloads"),
        CLEAR_CACHE("-clear-cache"),
        UPDATE_COUNTS("-update-counts"),
		EXPORT("-export"),
		EXPORT_VALIDATION("-export-validation"),
        CONVERT("-convert"),
		;

        // string ref for readable name
        private String command;
        
        // contructor
        Cmd(String command) { this.command = command; }

        // method to get enum readable name
        public String toString() { return command; }
    }
    
 
    /**
     * The big deal main.
     * 
     * @param params String[]
     */    
    public static void main(String[] params) throws Exception {
    	// "CPATH2_HOME" env. var must be set (mostly for logging)
        String home = System.getenv(HOME_VARIABLE_NAME);
    	if (home==null) {
            System.err.println("Please set " + HOME_VARIABLE_NAME 
            	+ " environment variable " +
            	" (point to a directory where cpath.properties, etc. files are placed)");
            System.exit(-1);
    	}
    	
    	// the JVM option must be set to the same value as well!
    	if (!home.equals(homeDir())) {
            System.err.println("Please set the java property " + HOME_VARIABLE_NAME 
            	+ ", i.e., run with -D" + HOME_VARIABLE_NAME + "=" + home + " option.");
            System.exit(-1);
    	}
    	
    	// configure logging
    	PropertyConfigurator.configure(home + File.separator + "log4j.properties");

    	if(!Charset.defaultCharset().equals(Charset.forName("UTF-8")))
    		if(LOG.isWarnEnabled())
    			LOG.warn("Default Charset, " + Charset.defaultCharset() 
    				+ " (is NOT 'UTF-8'...)");
    	
    	
    	// Cleanup arguments by removing empty/null strings from the end
    	// - possible result of calling this method from a script
    	List<String> argl = new ArrayList<String>();
    	for(String a : params) {
    		if(a == null || a.isEmpty() || a.equalsIgnoreCase("null"))
    			break;
    		else
    			argl.add(a);
    	}
    	final String[] args = argl.toArray(new String[]{});
//		System.out.println(Arrays.toString(args));
  			
    	LOG.debug("Command-line arguments were: " + Arrays.toString(args));
    	
    	// sanity check
        if (args.length == 0 || args[0].isEmpty()) {
            System.err.println("Missing args to Admin.");
			System.err.println(Admin.usage());
            System.exit(-1);
        }
    	

        // create the TMP dir inside the home dir if it does not exist yet
		File dir = new File(localDataDir());
		if(!dir.exists()) {
			dir.mkdir();
		}   	
    	
	       if(args[0].equals(Cmd.CREATE_DATABASES.toString())) {
				if (args.length > 1)
					// agrs[1] contains comma-separated db names
					createDatabases(args[1].split(","));
				else 
					createDatabases(null);
	        } 
	        else if(args[0].equals(Cmd.CREATE_INDEX.toString())) {
				index();
	        } 
	        else if (args[0].equals(Cmd.FETCH_METADATA.toString())) {
				if (args.length == 1) {
					fetchMetadata("file:" + property(PROP_METADATA_LOCATION));
				} else {
					fetchMetadata(args[1]);
				}
	        }
			else if (args[0].equals(Cmd.FETCH_DATA.toString())) {
				if (args.length >= 2) 
					fetchData(args[1]);
				else // command without extra parameter
					fetchData(null);
			}
			else if (args[0].equals(Cmd.CREATE_WAREHOUSE.toString())) {
				if (args.length > 1) 
					createWarehouse(args[1]);
				else // command without extra parameter
					createWarehouse(null);
			}
			else if (args[0].equals(Cmd.UPDATE_MAPPING.toString())) {
					updateMapping();
			}
			else if (args[0].equals(Cmd.PREMERGE.toString())) {
				if (args.length > 1) 
					runPremerge(args[1]);
				else // command without extra parameter
					runPremerge(null);
			}
			else if (args[0].equals(Cmd.MERGE.toString())) {
				boolean force = false;
				String provider = null;
				for (int i = 1; i < args.length; i++) {
					if ("--force".equalsIgnoreCase(args[i])) {
						force = true;
					} else {
						//use only one, the first id, and ignore others
						if(provider == null) 
							provider = args[i];
					}
				}
				
				runMerge(provider, force);
			}
			else if(args[0].equals(Cmd.EXPORT.toString())) {
				if (args.length < 3)
					fail(args,"must provide at least two arguments.");
				else if(args.length == 3)
					exportData(args[1], args[2], new String[]{});
				else 
					exportData(args[1], args[2], args[3].split(","));
	        } 
			else if(args[0].equals(Cmd.EXPORT_VALIDATION.toString())) {
				if (args.length < 3)
					fail(args,"must provide at least two arguments for this command.");
				
	        	if(args[2].endsWith(".html"))
	        		exportValidation(args[1], new FileOutputStream(args[2]), true);
	        	else
	        		exportValidation(args[1], new FileOutputStream(args[2]), false);
	        } 
			else if(args[0].equals(Cmd.CREATE_BLACKLIST.toString())) {
	            createBlacklist();
	        } 
			else if(args[0].equals(Cmd.CONVERT.toString())) {
				if (args.length < 4)
					fail(args,"provide at least three arguments.");
				
	            OutputStream fos = new FileOutputStream(args[2]);
	            OutputFormat outputFormat = OutputFormat.valueOf(args[3]);
	            convert(args[1], outputFormat, fos);
	        } 
			else if(args[0].equals(Cmd.CREATE_DOWNLOADS.toString())) {
	            createDownloads();
	        } 
			else if(args[0].equals(Cmd.UPDATE_COUNTS.toString())) {
				updateCounts();
	        } 
			else if(args[0].equals(Cmd.CLEAR_CACHE.toString())) {
				clearCache();
	        } 
			else {
				System.err.println(usage());
	        }    
		
		// required because MySQL Statement 
		// Cancellation Timer thread is still running
		System.exit(0);
    }    
    
    
    private static void fail(String[] args, String details) {
        throw new IllegalArgumentException(
        	"Invalid cpath2 command: " +  Arrays.toString(args)
        	+ "; " + details);		
	}


	/**
     * Builds new cpath2 full-text index.
     * 
     * @throws IllegalStateException when not in maintenance mode
     */
    public static void index() {
		if(!isMaintenanceEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
   		// re-build the full-text index
   		// it gets the main DB name from cpath.properties (via CPathSettings class)
		
		String indexDir = property(PROP_MAIN_DB); 
		LOG.info("Removing full-text index directory : " + indexDir);
		File dir = new File(CPathSettings.homeDir() + File.separator + indexDir);
		CPathUtils.deleteDirectory(dir);
		
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"classpath:applicationContext-dao.xml");    	
 		PaxtoolsDAO mainDAO = ((PaxtoolsDAO)context.getBean("paxtoolsDAO"));
 		mainDAO.index();
 		context.close(); 
 	}

    
	/**
	 * Updates counts of pathways, etc. and saves in the Metadata db.
	 * 
     * This depends on the full-text index, which must have been created already
     * (otherwise, results will be wrong).
     * 
     * @throws IllegalStateException when not in maintenance mode
     */
    public static void updateCounts() {
		if(!isMaintenanceEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
   		// re-build the full-text index
   		// it gets the DB name from the environment variables (set in cpath.properties)
		ClassPathXmlApplicationContext context = 
			new ClassPathXmlApplicationContext("classpath:applicationContext-dao.xml");
     	
 		PaxtoolsDAO mainDAO = ((PaxtoolsDAO)context.getBean("paxtoolsDAO"));
     	
     	//update counts of pathways, interactions, molecules
     	MetadataDAO mdao = (MetadataDAO) context.getBean("metadataDAO");
     	for(Metadata md : mdao.getAllMetadataInitialized()) {
     		
     		if(md.getType().isNotPathwayData())
     			continue;
     		
     		String name = md.standardName();
     		String[] filterBy = md.getName().toArray(new String[]{});
     		
     		SearchResponse sr = mainDAO.search("*", 0, Pathway.class, filterBy, null);
     		md.setNumPathways(sr.getNumHits());
     		LOG.info(name + ", pathways: " + md.getNumPathways());
     		
     		sr = mainDAO.search("*", 0, Interaction.class, filterBy, null);
     		md.setNumInteractions(sr.getNumHits());
     		LOG.info(name + ", interactions: " + md.getNumPathways());
     		
     		sr = mainDAO.search("*", 0, PhysicalEntity.class, filterBy, null);
     		md.setNumPhysicalEntities(sr.getNumHits());
     		LOG.info(name + ", physical entities: " + md.getNumPathways());
     		
     		mdao.saveMetadata(md);
     	}
     	
     	context.close(); 
	}
    
 
    /**
     * Deletes the mid-tier cache directories 
     * (web service layer queries: 'get', 'traverse', and 'graph' are cached) 
     * 
     */
    public static void clearCache() {
		if(!isMaintenanceEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
     	//remove the disk cache
     	File cacheDir = new File(CPathSettings.cacheDir());
		LOG.info("Removing cache directory : " + cacheDir.getAbsolutePath());
     	CPathUtils.deleteDirectory(cacheDir);
    }
    
    
    /**
     * Creates a new set of cpath2 database schemas;
     * 
     * @param names
     * @throws IllegalStateException when cpath2 is not in the maintenance mode.
     */
	public static void createDatabases(String[] names) {
		if(!isMaintenanceEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
		
    	if(names != null) {
    		for(String db : names) {
    			String dbName = db.trim();
    			CPathUtils.createDatabase(dbName);
    		}
    	} else { // create all (as specified in the current cpath.properties)
    		CPathUtils.createDatabase(property(PROP_MAIN_DB));
    	}
	}
	
	/**
     * Generates cpath2 graph query blacklist file
     * (to exclude ubiquitous small molecules, like ATP).
     * 
     * Algorithm:
     * Get all SmallMoleculeReferences
     * Calculate the degrees (i.e. num of reactions and num of complexes it is associated with)
     * if it is bigger than the overall threshold and lower than the regulation threshold
     *     add it (and its members/entities/member entities to the list)
     *     
     * @throws IOException, IllegalStateException (when not in maintenance mode)
     */
    public static void createBlacklist() throws IOException {
		if(!isMaintenanceEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
    	
    	// Extract blacklist values, if can't, then use the default values       
		ClassPathXmlApplicationContext context = 
			new ClassPathXmlApplicationContext("classpath:applicationContext-dao.xml");
        PaxtoolsDAO paxtoolsDAO = ((PaxtoolsDAO)context.getBean("paxtoolsDAO"));

        // os will be used directly inside the runAnalysis call
        Analysis blacklisting = new BlacklistingAnalysis();
        paxtoolsDAO.run(blacklisting);
        
        context.close(); 
    }

    
    /**
     * Performs cpath2 Merge stage.
     * 
     * @param provider
     * @param force
     * @throws IllegalStateException when not maintenance mode
     */
    public static void runMerge(String provider, boolean force) {
		if(!isMaintenanceEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
    	
		//disable 2nd level hibernate cache (ehcache)
		// otherwise the merger eventually fails with a weird exception
		// (this probably depends on the cache config. parameters)
		System.setProperty("net.sf.ehcache.disabled", "true");
//		System.setProperty("hibernate.hbm2ddl.auto", "update");
		// pc dao

		ClassPathXmlApplicationContext context = 
				new ClassPathXmlApplicationContext(new String[] 
				{"classpath:applicationContext-dao.xml",
					"classpath:applicationContext-dao.xml"});
		
		final PaxtoolsDAO paxtoolsDAO = (PaxtoolsDAO)context.getBean("paxtoolsDAO");

		LOG.info("runMerge: provider=" + provider + "; --force=" + force);
			
		MetadataDAO mdao = (MetadataDAO)context.getBean("metadataDAO");
			
		String datasource = (provider == null || provider.isEmpty()) ? null : provider;
		Merger merger = new MergerImpl(paxtoolsDAO, mdao, datasource, force);
		
		// go!
		merger.merge();
		
		context.close(); 
	}

	
    /**
     * Performs cpath2 Premerger stage:
     * pathway data cleaning, converting, validating, normalizing.
     * 
     * @param provider
     * @throws IllegalStateException when not maintenance mode
     */
	public static void runPremerge(String provider) {
		if(!isMaintenanceEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");		
		System.setProperty("hibernate.hbm2ddl.auto", "update");
		System.setProperty("net.sf.ehcache.disabled", "true");
		ClassPathXmlApplicationContext context =
            new ClassPathXmlApplicationContext(new String [] { 	
            		"classpath:applicationContext-dao.xml", 
            		"classpath:applicationContext-validator.xml", 
					"classpath:applicationContext-cvRepository.xml"});
		MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");
		Validator validator = (Validator) context.getBean("validator");
		// only metadataDAO is required for the Premerge (main/warehouse biopax DAO is not needed)
        Premerger premerger = new PremergeImpl(metadataDAO, null, validator, provider);
        LOG.info("runPremerge: provider=" + provider);
        premerger.premerge();
        
        context.close(); 
	}

	
    /**
     * Creates cpath2 Warehouse and id-mapping.
     * 
     * @param provider
     * @throws IllegalStateException when not maintenance mode
     */
	public static void createWarehouse(String provider) {
		if(!isMaintenanceEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
//		System.setProperty("hibernate.hbm2ddl.auto", "update");
		System.setProperty("net.sf.ehcache.disabled", "true");
		ClassPathXmlApplicationContext context =
            new ClassPathXmlApplicationContext("classpath:applicationContext-dao.xml");
		MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");
		PaxtoolsDAO dao = (PaxtoolsDAO) context.getBean("paxtoolsDAO");
		//CVs are not required now (null)
        Premerger premerger = new PremergeImpl(metadataDAO, dao, null, provider);
        premerger.buildWarehouse();        
        context.close(); 
	}

	
	/**
	 * Updates id-mapping tables using the warehouse data (xrefs).
	 */
	public static void updateMapping() {
		if(!isMaintenanceEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
//		System.setProperty("net.sf.ehcache.disabled", "true");
//		System.setProperty("hibernate.hbm2ddl.auto", "update");
		clearCache();
		ClassPathXmlApplicationContext context =
            new ClassPathXmlApplicationContext("classpath:applicationContext-dao.xml");
		MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");
		PaxtoolsDAO dao = (PaxtoolsDAO) context.getBean("paxtoolsDAO");
        Premerger premerger = new PremergeImpl(metadataDAO, dao, null, null);
        premerger.updateIdMapping(true);       
        context.close(); 
	}
	
	/**
     * Helper function to get provider metadata.
     *
     * @param location String PROVIDER_URL or local file.
     * @throws IOException, IllegalStateException (when not maintenance mode)
     */
    public static void fetchMetadata(final String location) throws IOException {
		if(!isMaintenanceEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
		
		System.setProperty("hibernate.hbm2ddl.auto", "update");	
		ClassPathXmlApplicationContext context =
            new ClassPathXmlApplicationContext("classpath:applicationContext-dao.xml");
        MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");
        Fetcher fetcher = new FetcherImpl(false);
        
        
        // grab the data
        Collection<Metadata> metadata = fetcher.readMetadata(location);
        
        // process metadata
        for (Metadata mdata : metadata) {
        	Metadata m = metadataDAO.getMetadataByIdentifier(mdata.getIdentifier());
        	if(m != null) {
        		LOG.info("fetchMetadata: overwriting metadata " +
        			"(this won't change the counts and pathwayData collection): " 
        				+ m.getIdentifier());
        		m.setDescription(mdata.getDescription());
        		m.setName(mdata.getName());
        		m.setIcon(mdata.getIcon());
        		m.setType(mdata.getType());
        		m.setUrlToData(mdata.getUrlToData());
        		m.setUrlToHomepage(mdata.getUrlToHomepage());
        		m.setConverterClassname(mdata.getConverterClassname());
        		m.setCleanerClassname(mdata.getCleanerClassname());
        		//m.setNumInteractions, etc.. - won't modify; one should run -update-counts
        		//m.setPathwayData - won't touch either; one should run -premerge
        		metadataDAO.saveMetadata(m);
        	}  else {
        		metadataDAO.saveMetadata(mdata);
        	}
        }
        
        context.close(); 
    }


    /**
     * Downloads pathway or warehouse (protein, small molecule) data
     * from the location specified in the metadata.
	 *
     * @param provider String
     * @throws IOException, IllegalStateException (when not maintenance mode)
     */
    public static void fetchData(final String provider) throws IOException {
		if(!isMaintenanceEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
    	
//		System.setProperty("hibernate.hbm2ddl.auto", "update");
		ClassPathXmlApplicationContext context =
            new ClassPathXmlApplicationContext("classpath:applicationContext-dao.xml");
        MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");
        Fetcher fetcher = new FetcherImpl(true);
    	
		// get metadata
		Collection<Metadata> metadataCollection = getMetadata(metadataDAO, provider);
		// sanity check
		if (metadataCollection == null || metadataCollection.isEmpty()) {
			if(provider != null)
				LOG.error("Unknown provider identifier: " + provider);
			else
				LOG.error("No metadata found in the db.");
			
			context.close(); 
			return;
		}
		
		// interate over all metadata
		for (Metadata metadata : metadataCollection) {			
			try {
				fetcher.fetchData(metadata); 
			} catch (Exception e) {
				LOG.error("Failed fetching data for " + metadata.toString() 
					+ ". Skipping...", e);
				continue;
			}
			
			LOG.info("FETCHING DONE : " + metadata.getIdentifier());
		}
		
		context.close(); 
	}

    
	/**
	 * Given a provider, returns a collection of Metadata.
	 *
	 * @param provider String
	 * @return Collection<Metadata>
	 */
	private static Collection<Metadata> getMetadata(final MetadataDAO metadataDAO, final String provider) 
	{
		Collection<Metadata> toReturn = new HashSet<Metadata>();
		if (provider == null || provider.isEmpty()) {
			toReturn = metadataDAO.getAllMetadataInitialized();
		} else {
			Metadata md = metadataDAO.getMetadataByIdentifier(provider);
			if(md != null)
				toReturn.add(md);
		}
		return toReturn;
	}


	/**
	 * Extracts a cpath2 BioPAX sub-model
	 * and writes to the specified file/format.
	 * 
	 * @param src pathway_id from the Metadata db, pathwayData table, or null
	 * @param output
	 * @param uris
	 * @throws IOException, IllegalStateException (in maintenance mode)
	 */
	public static void exportData(final String src, final String output, String[] uris) 
			throws IOException 
	{	
		if(isMaintenanceEnabled())
			throw new IllegalStateException("Maintenance mode.");
		
		if(uris == null) 
			uris = new String[]{};
		
		ClassPathXmlApplicationContext ctx = 
			new ClassPathXmlApplicationContext("classpath:applicationContext-dao.xml");
				
		if(src == null || src.isEmpty() || src.equals("--main_db")) {			
			OutputStream os = new FileOutputStream(output);
			// export a sub-model from the main biopax database
	        PaxtoolsDAO dao = ((PaxtoolsDAO)ctx.getBean("paxtoolsDAO"));
			dao.exportModel(os, uris);
			ctx.close(); 
		} else {			
			// get original pathway data by pathway_id 
			// from the Metadata db, pathwayData table
			// (ok to do even in the maintenance mode)
			Integer pk = null;
			try {
				pk = Integer.valueOf(src);
				if(LOG.isDebugEnabled())
					LOG.debug("Export from the original data," +
						" pathway_id=" + src);
			} catch (NumberFormatException e) {
				if(LOG.isDebugEnabled())
					LOG.debug("Export from the database: " + src);
			}			
			
			MetadataDAO dao = (MetadataDAO) ctx.getBean("metadataDAO");
			PathwayData pdata = dao.getPathwayData(pk);
			
			if(pdata != null) {
				// get premergeData (OWL text)
				byte[] data = pdata.getPremergeData();
				OutputStream os = new FileOutputStream(output);
				if (data != null && data.length > 0) {
					if (uris.length > 0) { // extract a sub-model
						SimpleIOHandler handler = new SimpleIOHandler(); // auto-detect Level
						Model model = handler.convertFromOWL(new ByteArrayInputStream(data));
						//handler.setFactory(model.getLevel().getDefaultFactory());
						handler.convertToOWL(model, os, uris);
					} else { 
						/*	write all (premergeData -
							cleaned/converted/validated/normalized from
							the original provider's file )	*/
						os.write(data);
						os.flush();
					}
				}
				else {
					// no data found
					LOG.error("Data not found! Have you run '-premerge' for "
						+ pdata + "?");
				}
			} 
			else {
				LOG.error("Record not found: pathway_id=" + pk);
			}
			
			ctx.close();
		}
	}	
	
	
	
	/**
	 * Exports from the PathwayData entity(-ies) and 
	 * writes the BioPAX validation report (as XML or HTML).
	 * 
	 * @param key an existing cpath2 pathwayData.pathway_id or metadata.identifier (provider).
	 * @param out output stream
	 * @param asHtml write as HTML report (transform from the XML)
	 * @throws IOException
	 */
	public static void exportValidation(final String key, final OutputStream out, 
			boolean asHtml) throws IOException {
		
		ClassPathXmlApplicationContext context =
	        new ClassPathXmlApplicationContext("classpath:applicationContext-dao.xml");
	    MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");
	    
	    ValidatorResponse report = null;
		Integer pk = null;
		try {
			pk = Integer.valueOf(key);
		} catch (NumberFormatException e) {}
		
		if(pk != null) {
			PathwayData pathwayData = metadataDAO.getPathwayData(pk);
			if (pathwayData != null) {
				if(LOG.isInfoEnabled())
		    		LOG.info("Getting validation report for pathway_id: " + pk 
		    			+ " (" + pathwayData.getMetadata().getIdentifier() + ") "
		    			+ "...");
				report = metadataDAO.validationReport(null, pk);
			} else {
				if(LOG.isInfoEnabled())
		    		LOG.info("Getting validation report: pathway_id: " + pk + " does not exist.");
				System.err.println("Getting validation report for pathway_id=" + pk + ": not found.");
				context.close();
				return;
			}
		} else {
			if(LOG.isInfoEnabled())
	    		LOG.info("Getting validation report for data source: " + key + "...");
			report = metadataDAO.validationReport(key, null);
		}

    	if(report == null) {
    		System.err.println("No validation report found or error.");
    	} else {
    		OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
    		Source xsl = (asHtml) 
    			? new StreamSource((new DefaultResourceLoader())
    					.getResource("classpath:html-result.xsl").getInputStream())
    			: null;

			ValidatorUtils.write(report, writer, xsl); 
    		writer.flush();
    	}
    	
    	context.close();
	}	
	
		
	private static String usage() 
	{
		final String NEWLINE = System.getProperty ( "line.separator" );
		StringBuilder toReturn = new StringBuilder();
		toReturn.append("Usage: <-command_name> [<command_args...>] " +
				"(- parameters within the square braces are optional.)" + NEWLINE);
		toReturn.append("commands:" + NEWLINE);
		// data import (instance creation) pipeline :
		toReturn.append(Cmd.CREATE_DATABASES.toString() + " [name] " +
				"(drops, creates an empty database; this destroys all data there)" + NEWLINE);
		toReturn.append(Cmd.FETCH_METADATA.toString() + " <url>" + NEWLINE);
		toReturn.append(Cmd.FETCH_DATA.toString() + " [<metadataId>]" + NEWLINE);
		toReturn.append(Cmd.CREATE_WAREHOUSE.toString() + " [<metadataId>]" + NEWLINE);
		toReturn.append(Cmd.UPDATE_MAPPING.toString() + " (re-builds id-mapping tables using cpath2 warehouse data)"+ NEWLINE);
		toReturn.append(Cmd.PREMERGE.toString() + " [<metadataId>]" + NEWLINE);
		toReturn.append(Cmd.MERGE.toString() + " [<metadataId>] [--force]"+ NEWLINE);
		toReturn.append(Cmd.CREATE_INDEX.toString() + NEWLINE);
        toReturn.append(Cmd.CREATE_BLACKLIST.toString() + " (creates blacklist.txt in the cpath2 home directory)" + NEWLINE);
        toReturn.append(Cmd.CLEAR_CACHE.toString() + " ()" + NEWLINE);
        toReturn.append(Cmd.UPDATE_COUNTS.toString() + " ()" + NEWLINE);
        toReturn.append(Cmd.CREATE_DOWNLOADS.toString() + " (creates cpath2 BioPAX DB archives using several " +
        	"data formats, and also split by data source, organism)"  + NEWLINE);        
        // other useful (utility) commands
		toReturn.append(Cmd.EXPORT.toString() + " <source> <output> [<uri,uri,..>]" +
			" (<source> can be either --main_db or a pathway_id, i.e., 'premerged' data PK in the pathwayData table)" + NEWLINE);
		toReturn.append(Cmd.EXPORT_VALIDATION.toString() 
			+ " <provider>|<pathway_id> <output_file[.xml|.html]> (<provider> - metadata identifier or <pathway_id> - see above; "
			+ "output_file will contain the Validator Response XML unless '.html' file extension is used, " +
			"in wich case the XML is there auto-transformed to offline HTML+Javascript content)" + NEWLINE);
		toReturn.append(Cmd.CONVERT.toString() + " <biopax-file(.owl|.gz)> <output-file> <output format>" + NEWLINE);

		return toReturn.toString();
	}

	
    /**
     * Converts a BioPAX file to other formats.
     *       
     * @param biopaxFile
     * @param outputFormat
     * @param output
     * @throws IOException
     */
	public static void convert(String biopaxFile, OutputFormat outputFormat, 
			OutputStream output) throws IOException 
	{
		InputStream is = biopaxStream(biopaxFile);
		Resource blacklist = new DefaultResourceLoader().getResource("file:" + blacklistFile());
		ServiceResponse res = (new BiopaxConverter(blacklist)).convert(is, outputFormat);
		if (res instanceof ErrorResponse) {
			System.err.println(res.toString());
		} else {
			String data = (String) ((DataResponse)res).getData();
			output.write(data.getBytes("UTF-8"));
			output.flush();
		}
	}
	
	
	private static void convertToExtSif(String biopaxFile,
			OutputStream edgeStream, OutputStream nodeStream) throws IOException 
	{
		InputStream is = biopaxStream(biopaxFile);		
		Model m = (new SimpleIOHandler()).convertFromOWL(is);
		Resource blacklist = new DefaultResourceLoader().getResource("file:" + blacklistFile());
		(new BiopaxConverter(blacklist)).convertToExtendedBinarySIF(m, edgeStream, nodeStream);
	}

	
    @SuppressWarnings("resource")
	private static InputStream biopaxStream(String biopaxFile) throws IOException {
		return (biopaxFile.endsWith(".gz"))
			? new GZIPInputStream(new FileInputStream(biopaxFile)) 
			: new FileInputStream(biopaxFile);
	}

    
    /**
     * Create cpath2 downloads 
     * (exports the db to various formats)
     * 
     * @param organisms names or taxonomy ids
     * @throws IOException, IllegalStateException (when not in maintenance mode)
     */
	public static void createDownloads() 
    		throws IOException
    {	
		if(!isMaintenanceEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
		
		// create the TMP dir inside the home dir if it does not exist yet
		File f = new File(downloadsDir());
		if(!f.exists()) 
			f.mkdir();
    	
		ClassPathXmlApplicationContext context = 
			new ClassPathXmlApplicationContext("classpath:applicationContext-dao.xml");
		PaxtoolsDAO dao = (PaxtoolsDAO) context.getBean("paxtoolsDAO");
		MetadataDAO mdao = (MetadataDAO) context.getBean("metadataDAO");
		
    	// 1) export everything
		createArchives("all", dao, null, null);
    	
    	// 2) export by organism
        LOG.info("create-downloads: preparing data 'by organism' archives...");
        String[] organisms = CPathSettings.organisms();
        for(String org : organisms) {
        	//make one archives set (diff. formats) per name
        	//TODO: in the future, after simple changes (also - to cpath.properties), can join several species data in one archive
        	createArchives(org.toLowerCase(), dao, null, new String[]{org});
        }
		
		// 3) export by datasource
        LOG.info("create-downloads: preparing 'by datasource' archives...");
        for(Metadata md : mdao.getAllMetadataInitialized()) {
        	if(!md.getType().isNotPathwayData()) {
        		// display name or, if exists, - standard name
        		String name = md.standardName(); 
        		createArchives(name, dao, md.getName().toArray(new String[]{}), null);
        	}
        }
        
        context.close();
	}


	private static Collection<String> findAllUris(PaxtoolsDAO db, 
    		Class<? extends BioPAXElement> type, String[] ds, String[] org) 
    {
    	Collection<String> uris = new ArrayList<String>();
    	
    	// using PaxtoolsDAO (no service-tier cache) instead CPathService here
    	SearchResponse resp = db.search("*", 0, type, ds, org);
    	int page = 0;
		while(!resp.isEmpty()) {
			for(SearchHit h : resp.getSearchHit())
				uris.add(h.getUri());
			//next page
			resp = db.search("*", ++page, type, ds, org);
		}
    	
    	return uris;
    }
    

    private static void createArchives(String filePrefix, PaxtoolsDAO dao, 
    	String[] datasources, String[] organisms) throws IOException 
    {
    	// grab the BioPAX first -
        final String biopaxDataArchive = downloadsDir() 
        		+ File.separator + property(PROVIDER_NAME) 
        		+ " " + filePrefix + ".BIOPAX.owl.gz";
        
        // check file exists
        if(!(new File(biopaxDataArchive)).exists()) {	
        	Collection<String> uris = new HashSet<String>();
        	
        	//find all pathways and interactions only (all child elements will be then exported too)
        	uris.addAll(findAllUris(dao, Pathway.class, datasources, organisms));
        	uris.addAll(findAllUris(dao, Interaction.class, datasources, organisms));
        	
        	// save entire data, compressed, in several formats
        	LOG.info("create-downloads: preparing '" + 	biopaxDataArchive);
        	//it is important to use dao.exportModel(out) and not service.fetchBiopaxModel(),
        	// because the latter w/o args will return empty result, whereas the former - ALL data.
        	dao.exportModel(new GZIPOutputStream(
        		new FileOutputStream(biopaxDataArchive)), uris.toArray(new String[]{}));
        } else
        	LOG.info(biopaxDataArchive + " already exists; skip creating it " +
        		"again (delete existing files if you want to start over)");

        //quickly test whether there will be pathways (to then skip exporting to GSEA)
        SearchResponse resp = dao.search("*", 0, Pathway.class, datasources, organisms);
        boolean hasPathways = (resp.getNumHits() > 0);
        OutputFormat[] formats = (hasPathways) 
        	? new OutputFormat[]{BINARY_SIF, EXTENDED_BINARY_SIF, GSEA}
        	: new OutputFormat[]{BINARY_SIF, EXTENDED_BINARY_SIF};
        
		for(OutputFormat outf : formats) {
			createArchive(biopaxDataArchive, outf, filePrefix);
		}
	}
	
	private static void createArchive(String biopaxDataArchive, OutputFormat format, 
			String prefix) throws IOException 
	{	
		if(format == OutputFormat.BIOPAX)
			throw new AssertionError("Converting BioPAX to BioPAX");
	
		String archiveName = downloadsDir() + File.separator 
				+ property(PROVIDER_NAME) + " " 
				+ prefix + "." + formatAndExt(format) + ".gz";
		LOG.info("create-downloads: generating " + archiveName);
		
		
        if(!(new File(archiveName)).exists()) {
    		//Extended SIF will be here split in two separate files (edges and nodes)
    		if(format == EXTENDED_BINARY_SIF) {
    			//write edges and nodes into separate archives
    			GZIPOutputStream edgeStream = new GZIPOutputStream(new FileOutputStream(archiveName));
    			GZIPOutputStream nodeStream = new GZIPOutputStream(new FileOutputStream(
    				homeDir() + File.separator 
    					+ DOWNLOADS_SUBDIR + File.separator 
    					+ property(PROVIDER_NAME) + " " 
    					+ prefix + "." + format + ".nodes.tsv.gz"));
    		
    			convertToExtSif(biopaxDataArchive, edgeStream, nodeStream);
    			
    			IOUtils.closeQuietly(edgeStream);
    			IOUtils.closeQuietly(edgeStream);
    		} else {    	
    			GZIPOutputStream zos = new GZIPOutputStream(new FileOutputStream(archiveName));
    			convert(biopaxDataArchive, format, zos);
        		IOUtils.closeQuietly(zos);
    		}
    		
    		LOG.info("create-downloads: successully created " + archiveName);
    		
        } else
        	LOG.info(archiveName + " exists; skip creating it " +
            	"(delete if you want to start over)");
	}

	
	private static String formatAndExt(OutputFormat format) {
		switch (format) {
		case BIOPAX:
			return format + ".owl";
		case GSEA:
			return format + ".gmt";
		case SBGN:
			return format + ".xml";
		default:
			return format + ".tsv";
		}
	}

}