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

import cpath.admin.fix.FixHumanCycPathwayOrganismAnalysis;
import cpath.admin.fix.FixReactomeConversionDirectionAnalysis;
import cpath.config.CPathSettings;
import cpath.dao.*;
import cpath.importer.Merger;
import cpath.importer.Premerger;
import cpath.importer.internal.MergerImpl;
import cpath.importer.internal.PremergeImpl;
import cpath.service.ErrorResponse;
import cpath.service.OutputFormat;
import cpath.service.internal.BiopaxConverter;
import cpath.service.jaxb.*;
import cpath.warehouse.beans.Metadata;

import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.pattern.miner.BlacklistGenerator;
import org.biopax.paxtools.pattern.util.Blacklist;
import org.biopax.validator.api.Validator;
import org.h2.tools.Csv;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


import static cpath.service.OutputFormat.*;

/**
 * Class which provides cpath2 command line
 * access for the system administrators and 
 * user scripts.
 */
public final class Admin {
	private static final Logger LOG = LoggerFactory.getLogger(Admin.class);
	
	private static final OutputFormat[] EXPORT_TO_FORMATS = new OutputFormat[]{BINARY_SIF, EXTENDED_BINARY_SIF, GSEA};
	
	private static final CPathSettings cpath = CPathSettings.getInstance();

    // Cmd Enum
    public static enum Cmd {
        // command types
    	INIT("-init"),
        FETCH_METADATA("-fetch-metadata"),
		FETCH_DATA("-fetch-data"),
		PREMERGE("-premerge"),
		CREATE_WAREHOUSE("-create-warehouse"),			
		MERGE("-merge"),
    	CREATE_INDEX("-create-index"),
        CREATE_BLACKLIST("-create-blacklist"),
        CREATE_DOWNLOADS("-create-downloads"),
        CLEAR_CACHE("-clear-cache"),
        UPDATE_COUNTS("-update-counts"),
		EXPORT("-export"),
        CONVERT("-convert"),
        EXPORT_LOG("-export-log"),
        IMPORT_LOG("-import-log"),
        AFTERMERGE("-aftermerge"),
		;

        // string ref for readable name
        private String command;
        
        // contructor
        Cmd(String command) { this.command = command; }

        // method to get enum readable name
        @Override
        public String toString() { return command; }
    }
    
 
    /**
     * The big deal main.
     * 
     * @param params String[]
     */    
    public static void main(String[] params) throws Exception {
    	// "CPATH2_HOME" env. var must be set (mostly for logging)
    	cpath.property(HOME_DIR); //throws IllegalStateEx. is not set.

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
    	

        // create the data dir. inside the home dir. if it does not exist
		File dir = new File(cpath.dataDir());
		if(!dir.exists()) {
			dir.mkdir();
		}

		if (args[0].equals(Cmd.CREATE_INDEX.toString())) {
			
			index();
			
		} else if (args[0].equals(Cmd.INIT.toString())) {
			
				initDb();
				
		} else if (args[0].equals(Cmd.FETCH_METADATA.toString())) {
			
			if (args.length == 1) {
				fetchMetadata("file:" + cpath.property(PROP_METADATA_LOCATION));
			} else {
				fetchMetadata(args[1]);
			}
			
		} else if (args[0].equals(Cmd.CREATE_WAREHOUSE.toString())) {
			
				createWarehouse();
			
		} else if (args[0].equals(Cmd.PREMERGE.toString())) {
			
			if (args.length > 1)
				runPremerge(args[1]);
			else
				// command without extra parameter
				runPremerge(null);
			
		} else if (args[0].equals(Cmd.MERGE.toString())) {
			boolean force = false;
			String provider = null;
			for (int i = 1; i < args.length; i++) {
				if ("--force".equalsIgnoreCase(args[i])) {
					force = true;
				} else {
					// use only one, the first id, and ignore others
					if (provider == null)
						provider = args[i];
				}
			}

			runMerge(provider, force);
			
		} else if (args[0].equals(Cmd.EXPORT.toString())) {
			
			if (args.length < 2)
				fail(args, "must provide at least one arguments.");
			else if (args.length == 2)
				exportData(args[1], new String[] {});
			else
				exportData(args[1], args[2].split(","));
			
		} else if (args[0].equals(Cmd.CREATE_BLACKLIST.toString())) {
			
			createBlacklist();
			
		} else if (args[0].equals(Cmd.CONVERT.toString())) {
			if (args.length < 4)
				fail(args, "provide at least three arguments.");
			OutputStream fos = new FileOutputStream(args[2]);
			OutputFormat outputFormat = OutputFormat.valueOf(args[3]);	
			Model model = (new SimpleIOHandler()).convertFromOWL(biopaxStream(args[1])); 
			convert(model, outputFormat, fos);
			
		} else if (args[0].equals(Cmd.CREATE_DOWNLOADS.toString())) {
			
			createDownloads();
			
		} else if (args[0].equals(Cmd.UPDATE_COUNTS.toString())) {
			
			updateCounts();
			
		} else if (args[0].equals(Cmd.CLEAR_CACHE.toString())) {
			
			clearCache();
		
		} else if (args[0].equals(Cmd.EXPORT_LOG.toString())) {	
			if (args.length == 2 && "--clear".equalsIgnoreCase(args[1]))
				exportLog(true);
			else
				exportLog(false);	
		} else if (args[0].equals(Cmd.IMPORT_LOG.toString())) {	
			
			if (args.length < 2) 
				fail(args, "No input file provided.");	
			else 
				importLog(args[1]);
			
		} else if (args[0].equals(Cmd.AFTERMERGE.toString())) {	
			
			aftermerge(); //TODO make it configurable, specific
		
		} else {
			System.err.println(usage());
		}

		// required because MySQL Statement
		// Cancellation Timer thread is still running
		System.exit(0);
    }    

    
    /**
     * This drops and creates empty cpath2 database tables and 
     * cache directories.
     */
    private static void initDb() {
    	if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
    	  	
		System.setProperty("hibernate.hbm2ddl.auto", "update");
		System.setProperty("net.sf.ehcache.disabled", "true");
		ClassPathXmlApplicationContext context =
            new ClassPathXmlApplicationContext(new String [] { 	
            		"classpath:META-INF/spring/applicationContext-dao.xml", 
            		"classpath:META-INF/spring/applicationContext-log.xml"
            	});
 				
		CPathUtils.cleanupIndexDir(CPathSettings.getInstance().getMainDb());
 		
 		context.close();
	}
    
    
    private static void aftermerge() {
    	if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
    	
    	ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"classpath:META-INF/spring/applicationContext-dao.xml");    	
 		PaxtoolsDAO mainDAO = ((PaxtoolsDAO)context.getBean("paxtoolsDAO"));
 		
 		mainDAO.run(new FixHumanCycPathwayOrganismAnalysis());
 		
 		mainDAO.run(new FixReactomeConversionDirectionAnalysis());
 		
 		context.close();
	}


	//clean/update service access counts by location,date in the DB from available .log files
    public static void exportLog(boolean clear) throws IOException {
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
 		
 		//backup cpath2 access db to a CSV file
 		CPathSettings cps = getInstance();
 		String filename = cpath.homeDir() + File.separator + "logentity.csv";
 		Connection conn = null;
 		try {
			Class.forName("org.h2.Driver");
			conn = DriverManager.getConnection(cpath.property(PROP_DB_CONNECTION) 
					+ cps.getMainDb(), cpath.property(PROP_DB_USER), cpath.property(PROP_DB_PASSW));
//			conn = DriverManager.getConnection(property(PROP_DB_CONNECTION) + "accesslog", 
//					property(PROP_DB_USER), property(PROP_DB_PASSW));
			new Csv()
				.write(conn, filename, "select * from logentity", "UTF-8");
			LOG.info("Saved current access log DB to " + filename);
			
			if(clear) {
				//purge existing access time and location history
				conn.createStatement().executeUpdate("delete from logentity;");
				conn.commit();
				LOG.info("Cleared access log DB");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {conn.close();} catch (Exception e) {}
		}
	}

    public static void importLog(String filename) throws IOException {
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
 		
 		//load cpath2 access db from a CSV file
 		String backup = cpath.homeDir() + File.separator + "logentity.csv.bak";
 		Connection conn = null;
 		try {
 			Class.forName("org.h2.Driver");
			conn = DriverManager.getConnection(cpath.property(PROP_DB_CONNECTION) 
				+ cpath.getMainDb(), cpath.property(PROP_DB_USER), cpath.property(PROP_DB_PASSW));
// 			conn = DriverManager.getConnection(property(PROP_DB_CONNECTION) + "accesslog", 
// 					property(PROP_DB_USER), property(PROP_DB_PASSW));
			
			//backup
			new Csv()
				.write(conn, backup, "select * from logentity", "UTF-8");
			LOG.info("Saved current access log DB to " + backup);
			
			//clear all existing data
			conn.createStatement().executeUpdate("delete from logentity;");
			LOG.info("Cleared access log DB");
			
			conn.createStatement().executeUpdate("insert into logentity " +
	 				"select * from CSVREAD('"+ filename +"')");	 		
	 		conn.commit();
	        LOG.info("Imported access log entries from " + filename);
		} catch (Exception e) {
			try {conn.rollback(); conn.close();} catch (Exception ex) {}
			throw new RuntimeException(e);
		} finally {
			try {conn.close();} catch (Exception e) {}
		}
 		
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
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
   		// re-build the full-text index
   		// it gets the main DB name from cpath2.properties (via CPathSettings class)
		
		String indexDir = cpath.property(PROP_MAIN_DB); 
		LOG.info("Cleaning up the full-text index directory: " + indexDir);
		File dir = new File(cpath.homeDir() + File.separator + indexDir);
		CPathUtils.cleanupDirectory(dir);
		
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"classpath:META-INF/spring/applicationContext-dao.xml");    	
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
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
   		// re-build the full-text index
   		// it gets the DB name from the environment variables (set in cpath2.properties)
		ClassPathXmlApplicationContext context = 
			new ClassPathXmlApplicationContext("classpath:META-INF/spring/applicationContext-dao.xml");
     	
 		PaxtoolsDAO mainDAO = ((PaxtoolsDAO)context.getBean("paxtoolsDAO"));
     	
     	//update counts of pathways, interactions, molecules
     	MetadataDAO mdao = (MetadataDAO) context.getBean("metadataDAO");
     	for(Metadata md : mdao.getAllMetadata()) {
     		
     		if(md.isNotPathwayData())
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
     * Purges all cache directories.
     */
    public static void clearCache() {
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
     	//remove the disk cache
     	File cacheDir = new File(cpath.cacheDir());
		LOG.info("Removing cache directory : " + cacheDir.getAbsolutePath());
     	CPathUtils.deleteDirectory(cacheDir);
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
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
    	
    	// Extract blacklist values, if can't, then use the default values       
		ClassPathXmlApplicationContext context = 
			new ClassPathXmlApplicationContext("classpath:META-INF/spring/applicationContext-dao.xml");
        PaxtoolsDAO paxtoolsDAO = ((PaxtoolsDAO)context.getBean("paxtoolsDAO"));

        // os will be used directly inside the runAnalysis call
        Analysis blacklisting = new Analysis() {     	
        	@Override
            public void execute(Model model) 
            {	
        		BlacklistGenerator gen = new BlacklistGenerator();
        		Blacklist blacklist = gen.generateBlacklist(model);
   		
        		// Write all the blacklisted ids to the output
        		try {		
        			blacklist.write(new FileOutputStream(cpath.blacklistFile()));
        		} catch (FileNotFoundException e) {
        			throw new RuntimeException("Failed creating the file: " 
        					+ cpath.blacklistFile(), e);
        		} 
            }
        };
        
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
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
    	
		//disable 2nd level hibernate cache (ehcache)
		// otherwise the merger eventually fails with a weird exception
		// (this probably depends on the cache config. parameters)
		System.setProperty("net.sf.ehcache.disabled", "true");
//		System.setProperty("hibernate.hbm2ddl.auto", "update");
		// pc dao

		ClassPathXmlApplicationContext context = 
			new ClassPathXmlApplicationContext("classpath:META-INF/spring/applicationContext-dao.xml");
		
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
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");		
		
        LOG.info("runPremerge: provider=" + provider + " - initializing (DAO, validator, premerger)...");
        
		System.setProperty("hibernate.hbm2ddl.auto", "update");
		System.setProperty("net.sf.ehcache.disabled", "true");
		ClassPathXmlApplicationContext context =
            new ClassPathXmlApplicationContext(new String [] { 	
            		"classpath:META-INF/spring/applicationContext-dao.xml", 
            		"classpath:META-INF/spring/applicationContext-validator.xml", 
					"classpath:META-INF/spring/applicationContext-cvRepository.xml"});
		MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");
		Validator validator = (Validator) context.getBean("validator");
		// only metadataDAO is required for the Premerge (main/warehouse biopax DAO is not needed)
        Premerger premerger = new PremergeImpl(metadataDAO, null, validator, provider);
        LOG.info("runPremerge: provider=" + provider + " - running...");
        premerger.premerge();
        
        context.close(); 
	}

	
    /**
     * Creates cpath2 Warehouse and id-mapping tables.
     * 
     * @throws IllegalStateException when not maintenance mode
     */
	public static void createWarehouse() {
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
		System.setProperty("hibernate.hbm2ddl.auto", "update");
		System.setProperty("net.sf.ehcache.disabled", "true");
		ClassPathXmlApplicationContext context =
            new ClassPathXmlApplicationContext("classpath:META-INF/spring/applicationContext-dao.xml");
		MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");
		PaxtoolsDAO dao = (PaxtoolsDAO) context.getBean("paxtoolsDAO");
        Premerger premerger = new PremergeImpl(metadataDAO, dao, null, null);
        premerger.buildWarehouse();        
        context.close(); 
	}

	
	/**
     * Helper function to get provider metadata.
     *
     * @param location String PROVIDER_URL or local file.
     * @throws IOException, IllegalStateException (when not maintenance mode)
     */
    public static void fetchMetadata(final String location) throws IOException {
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
		
		System.setProperty("hibernate.hbm2ddl.auto", "update");	
		ClassPathXmlApplicationContext context =
            new ClassPathXmlApplicationContext("classpath:META-INF/spring/applicationContext-dao.xml");
        MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");              
        // grab the data
        metadataDAO.addOrUpdateMetadata(location);
        context.close(); 
    }


	/**
	 * Extracts a cpath2 BioPAX sub-model
	 * and writes to the specified file.
	 * @param output
	 * @param uris
	 * 
	 * @throws IOException, IllegalStateException (in maintenance mode)
	 */
	public static void exportData(final String output, String[] uris) 
			throws IOException 
	{	
		if(cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode.");
		
		if(uris == null) 
			uris = new String[]{};
		
		ClassPathXmlApplicationContext ctx = 
			new ClassPathXmlApplicationContext("classpath:META-INF/spring/applicationContext-dao.xml");
							
		OutputStream os = new FileOutputStream(output);
		// export a sub-model from the main biopax database
	    PaxtoolsDAO dao = ((PaxtoolsDAO)ctx.getBean("paxtoolsDAO"));
	    dao.exportModel(os, uris);
		ctx.close(); 
	}	
	
			
	private static String usage() 
	{
		final String NEWLINE = System.getProperty ( "line.separator" );
		StringBuilder toReturn = new StringBuilder();
		toReturn.append("Usage: <-command_name> [<command_args...>] " +
				"(- parameters within the square braces are optional.)" + NEWLINE);
		toReturn.append("commands:" + NEWLINE);
		// data import (instance creation) pipeline :
		toReturn.append(Cmd.INIT.toString() + " (creates a new empty database; this destroys all previous data)" + NEWLINE);
		toReturn.append(Cmd.FETCH_METADATA.toString() + " <url>" + NEWLINE);
		toReturn.append(Cmd.PREMERGE.toString() + " [<metadataId>]" + NEWLINE);
		toReturn.append(Cmd.CREATE_WAREHOUSE.toString() + NEWLINE);			
		toReturn.append(Cmd.MERGE.toString() + " [<metadataId>] [--force]"+ NEWLINE);
		toReturn.append(Cmd.CREATE_INDEX.toString() + NEWLINE);
        toReturn.append(Cmd.CREATE_BLACKLIST.toString() + " (creates blacklist.txt in the cpath2 home directory)" + NEWLINE);
        toReturn.append(Cmd.CLEAR_CACHE.toString() + " (removes the cache directory)" + NEWLINE);
        toReturn.append(Cmd.UPDATE_COUNTS.toString() + " (re-calculates pathway, molecule, " +
        		"interaction counts per data source)" + NEWLINE);
        toReturn.append(Cmd.CREATE_DOWNLOADS.toString() + " (creates cpath2 BioPAX DB archives using several " +
        	"data formats, and also split by data source, organism)"  + NEWLINE);        
        // other useful (utility) commands
		toReturn.append(Cmd.EXPORT.toString() + " <output> [<uri,uri,..>]" + NEWLINE);
		toReturn.append(Cmd.CONVERT.toString() + " <biopax-file(.owl|.gz)> <output-file> <output format>" + NEWLINE);
		toReturn.append(Cmd.EXPORT_LOG.toString() + " [--clear] (export cpath2 assess log to the " +
				"CSV file in the data directory and, optionally, clear the table)" + NEWLINE);
		toReturn.append(Cmd.IMPORT_LOG.toString() + " [filename] (import cpath2 assess log from the specified " +
				"CSV file or from the default, if exists, in the data dir.)" + NEWLINE);
		toReturn.append(Cmd.AFTERMERGE.toString() + " (apply custom post-fixes to the cpath2 DB; " +
				"requires re-indexing and re-generationg of the data dump archives, i.e., downloads, after this is done)" + NEWLINE);
		
		return toReturn.toString();
	}

	
    /**
     * Converts a BioPAX file to other formats.
     *       
     * @param model
     * @param outputFormat
     * @param output
     * @throws IOException
     */
	public static void convert(Model model, OutputFormat outputFormat, 
			OutputStream output) throws IOException 
	{
		Resource blacklist = new DefaultResourceLoader().getResource("file:" + cpath.blacklistFile());
		BiopaxConverter converter = new BiopaxConverter(blacklist);
		converter.mergeEquivalentInteractions(true);
		ServiceResponse res = converter.convert(model, outputFormat);
		if (res instanceof ErrorResponse) {
			System.err.println(res.toString());
		} else {
			String data = (String) ((DataResponse)res).getData();
			output.write(data.getBytes("UTF-8"));
			output.flush();
		}
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
     * @throws IOException, IllegalStateException (when not in maintenance mode)
     */
	public static void createDownloads() 
    		throws IOException
    {	
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
		
		// create the TMP dir inside the home dir if it does not exist yet
		File f = new File(cpath.downloadsDir());
		if(!f.exists()) 
			f.mkdir();
    	
		//copy the blacklist.txt
		FileUtils.copyFileToDirectory(new File(cpath.blacklistFile()), f);
		LOG.info("create-downloads: copied the blacklist.txt file...");
		
		ClassPathXmlApplicationContext context = 
			new ClassPathXmlApplicationContext("classpath:META-INF/spring/applicationContext-dao.xml");
		MetadataDAO mdao = (MetadataDAO) context.getBean("metadataDAO");
		PaxtoolsDAO dao = (PaxtoolsDAO) context.getBean("paxtoolsDAO");
		
		final List<Metadata> allMetadata = mdao.getAllMetadata();
		//0) create an imported data summary file.txt (issue#23)
		PrintWriter writer = new PrintWriter(cpath.downloadsDir() + File.separator 
				+ "datasources.txt");
		String date = new SimpleDateFormat("d MMM yyyy").format(Calendar.getInstance().getTime());
		writer.println(StringUtils.join(Arrays.asList(
			"#CPATH2:", getInstance().getName(), "version", getInstance().getVersion(), date), " "));
		writer.println("#Columns:\t" + StringUtils.join(Arrays.asList(
			"ID", "DESCRIPTION", "TYPE", "HOMEPAGE", "PATHWAYS", "INTERACTIONS", "PHYS.ENTITIES"), "\t"));
		for(Metadata m : allMetadata) {
			writer.println(StringUtils.join(Arrays.asList(
				m.getUri(), m.getDescription(), m.getType(), m.getUrlToHomepage(), 
				m.getNumPathways(), m.getNumInteractions(), m.getNumPhysicalEntities()), "\t"));
		}		
		writer.flush();
		writer.close();		
		LOG.info("create-downloads: successfully generated the datasources summary file.");
		
		// generate/find all BioPAX archives:
		List<String> biopaxArchives = exportBiopax(dao, allMetadata);
		
		context.close(); //DAO is not needed anymore
		    	
    	// 2) export to all other formats
        for(String biopaxArchive : biopaxArchives) {
        	//load model and convert to other formats
    		Model m = (new SimpleIOHandler()).convertFromOWL(biopaxStream(biopaxArchive));
    		for(OutputFormat format : EXPORT_TO_FORMATS) {
    			int idx = biopaxArchive.indexOf(".BIOPAX");
    			String prefix = biopaxArchive.substring(0, idx);
    			export(m, format, prefix);
    		}
    		m = null;
        } 	
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

	
	private static void export(Model m, OutputFormat format, String prefix) 
			throws IOException 
	{	
		if(format == OutputFormat.BIOPAX)
			throw new IllegalArgumentException(format.name() + " is not allowed here.");

		if(format == OutputFormat.GSEA && m.getObjects(Pathway.class).isEmpty()) {
			LOG.info("create-downloads: " + prefix + 
					" BioPAX model has no Pathways; so, skipping for GSEA archive...");
			return;
		}
	
		String archiveName = prefix + "." + formatAndExt(format) + ".gz";
		
        if(!(new File(archiveName)).exists()) {
    		LOG.info("create-downloads: generating new " + archiveName);
    		//note: extended SIF will be one file (edges, nodes)
   			GZIPOutputStream zos = new GZIPOutputStream(new FileOutputStream(archiveName));
   			convert(m, format, zos);
       		IOUtils.closeQuietly(zos);  		
    		LOG.info("create-downloads: successully created " + archiveName);    		
        } else
        	LOG.info("create-downloads: skip for existing " + archiveName);
	}
	
	
	private static List<String> exportBiopax(final PaxtoolsDAO dao, Collection<Metadata> allMetadata) 
			throws IOException 
	{
		List<String> files = new ArrayList<String>();
		
		// generate the complete biopax db export (all processes, no filters)
		String archiveName = cpath.biopaxExportFileName("All");
		exportBiopax(dao, archiveName, null, null);
		files.add(archiveName);
		
    	// export by organism
        LOG.info("create-downloads: preparing data 'by organism' archives...");
        for(String org :  cpath.getOrganisms()) {
        	// generate archives for current organism
        	// hack: org.toLowerCase() is to tell by-organism from by-datasource archives (for usage stats...) 
        	archiveName = cpath.biopaxExportFileName(org.toLowerCase());
        	exportBiopax(dao, archiveName, null, new String[]{org});
        	files.add(archiveName);
        }
		
		// export by datasource
        LOG.info("create-downloads: preparing 'by datasource' archives...");    
        
        for(Metadata md : allMetadata) {
        	// generate archives for current pathway datasource;
        	if(!md.isNotPathwayData()) {
        		// use standard name and not the metadata ID in the file name, 
        		// because we want all data from same DB merge into one file,
        		// e.g., Reactome human, mouse, fungi data together, though might imported them via separate metadata)
        		archiveName = cpath.biopaxExportFileName(md.standardName());
        		exportBiopax(dao, archiveName,  md.getName().toArray(new String[]{}), null);
        		files.add(archiveName);
        	}
        }	
        
        return files;
	}	

	
	private static void exportBiopax(PaxtoolsDAO dao, String biopaxArchive,
			String[] datasources, String[] organisms) throws IOException
	{
        // check file exists
        if(!(new File(biopaxArchive)).exists()) {
        	LOG.info("create-downloads: creating new " + 	biopaxArchive);
        	
        	//find all pathways and interactions only (all child elements will be then exported too)    	  	
       		Collection<String> uris = new HashSet<String>();
           	uris.addAll(findAllUris(dao, Pathway.class, datasources, organisms));
           	uris.addAll(findAllUris(dao, Interaction.class, datasources, organisms));   	
        	
    		// export objects found above to a new biopax archive        	
        	if(!uris.isEmpty()) {
       			dao.exportModel(new GZIPOutputStream(
               			new FileOutputStream(biopaxArchive)), uris.toArray(new String[]{}));
       			LOG.info("create-downloads: successfully created " + 	biopaxArchive);
        	} else {
        		LOG.info("create-downloads: no pathways/interactions found; skipping " + 	biopaxArchive);
        	}
        } else {
        	LOG.info("create-downloads: found previously generated " + biopaxArchive);
        }   		
	}


	private static String formatAndExt(OutputFormat format) {
		switch (format) {
		case GSEA:
			return format + ".gmt";
		case SBGN:
			return format + ".xml";
		case BINARY_SIF:
			return format + ".tsv";
		case EXTENDED_BINARY_SIF:
			return format + ".edges.tsv";
		default://fail - biopax is treated specially, not here
			throw new IllegalArgumentException(format.toString() + " not allowed.");
		}
	}

}
