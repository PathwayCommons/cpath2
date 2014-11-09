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
import cpath.importer.Merger;
import cpath.importer.PreMerger;
import cpath.jpa.Metadata;
import cpath.service.CPathService;
import cpath.service.ErrorResponse;
import cpath.service.Indexer;
import cpath.service.OutputFormat;
import cpath.service.SearchEngine;
import cpath.service.internal.BiopaxConverter;
import cpath.service.jaxb.*;

import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.pattern.miner.BlacklistGenerator;
import org.biopax.paxtools.pattern.util.Blacklist;
import org.biopax.validator.api.Validator;
import org.h2.tools.Csv;
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
        FETCH_METADATA("-fetch-metadata"),
		FETCH_DATA("-fetch-data"),
		PREMERGE("-premerge"),
		CREATE_WAREHOUSE("-create-warehouse"),			
		MERGE("-merge"),
    	INDEX("-index"),
        CREATE_BLACKLIST("-create-blacklist"),
        CREATE_DOWNLOADS("-create-downloads"),
        CLEAR_CACHE("-clear-cache"),
        UPDATE_COUNTS("-update-counts"),
		EXPORT("-export"),
        CONVERT("-convert"),
        EXPORT_LOG("-export-log"),
        IMPORT_LOG("-import-log"),
        ANALYSIS("-run-analysis"), //TODO keep or remove (normally, should never be used)?..
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
  		argl = null;
    	
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

		if (args[0].equals(Cmd.INDEX.toString())) {
			
			createIndex();
			
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
			for (int i = 1; i < args.length; i++) {
				if ("--force".equalsIgnoreCase(args[i])) {
					force = true;
				}
			}

			runMerge(force);
			
		} else if (args[0].equals(Cmd.EXPORT.toString())) {
			//(the first args[0] is the command name, the second - must be output file)
			if (args.length < 2)
				fail(args, "must provide at least an output file name (other parameters are optional).");
			else {
				String[] uris = new String[] {};
				String[] datasources = new String[] {};
				String[] types = new String[] {};
				boolean absoluteUris = false;
							
				for(int i=2; i < args.length; i++) {
					if(args[i].equalsIgnoreCase("--output-absolute-uris"))
						absoluteUris = true;
					else if(args[i].toLowerCase().startsWith("--datasources="))
						datasources = args[i].substring(14).split(",");
					else if(args[i].toLowerCase().startsWith("--types="))
						types = args[i].substring(8).split(",");
					else if(args[i].toLowerCase().startsWith("--uris="))
						uris = args[i].substring(7).split(",");
					else 
						LOG.error("Skipped unrecognized argument: " + args[i]);
				}
				
				exportData(args[1], uris, absoluteUris, datasources, types);
			}

			
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
			
		} else if (args[0].equals(Cmd.ANALYSIS.toString())) {	
			
			if (args.length < 2) 
				fail(args, "No Analysis implementation class provided.");	
			else if(args.length == 2)
				executeAnalysis(args[1], true);
			else if(args.length > 2 && "--update".equalsIgnoreCase(args[2]))
				executeAnalysis(args[1], false);
		
		} else {
			System.err.println(usage());
		}

		// required because MySQL Statement
		// Cancellation Timer thread is still running
		System.exit(0);
    }    

    
    /**
     * Executes a code that uses or edits the main BioPAX model.
     * 
     * @param analysisClass a class that implements {@link Analysis} 
     * @param readOnly
     */
    public static void executeAnalysis(String analysisClass, boolean readOnly) {
    	
    	if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
    	
    	Analysis analysis = null;
		try {
			Class<Analysis> c = (Class<Analysis>) Class.forName(analysisClass);
			analysis = c.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    	
		Model model = CPathUtils.loadMainBiopaxModel();
 		analysis.execute(model);
 		
 		if(!readOnly) { //replace the main BioPAX model archive
 			try {			
 				new SimpleIOHandler(BioPAXLevel.L3).convertToOWL(model, 
 					new GZIPOutputStream(new FileOutputStream(
 							CPathSettings.getInstance().mainModelFile())));
 			} catch (Exception e) {
 				throw new RuntimeException("Failed updating the main BioPAX archive!", e);
 			}
 			
 			LOG.warn("The main BioPAX model was modified; "
 				+ "do not forget to re-index, update counts, re-export other files, etc.");
 		}
 			
	}


	//clean/update service access counts by location,date in the DB from available .log files
    public static void exportLog(boolean clear) throws IOException {
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
 		
 		//backup cpath2 access db to a CSV file
 		CPathSettings cps = getInstance();
 		String filename = cpath.dataDir() + File.separator + "logentity.csv";
 		Connection conn = null;
 		try {
			Class.forName("org.h2.Driver");
			conn = DriverManager.getConnection(cpath.property(PROP_DB_CONNECTION) 
					+ cps.getMainDb()+"_meta", cpath.property(PROP_DB_USER), cpath.property(PROP_DB_PASSW));
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
 		String backup = cpath.dataDir() + File.separator + "logentity.csv.bak";
 		Connection conn = null;
 		try {
 			Class.forName("org.h2.Driver");
			conn = DriverManager.getConnection(cpath.property(PROP_DB_CONNECTION) 
				+ cpath.getMainDb() + "_meta", cpath.property(PROP_DB_USER), cpath.property(PROP_DB_PASSW));
			
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
     * Builds new biopax full-text index.
     * 
	 * @throws IOException 
     * @throws IllegalStateException when not in maintenance mode
     */
    public static void createIndex() throws IOException {
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
		
		// cleanup the index directory
		CPathSettings cpath = CPathSettings.getInstance();
		File dir = new File(cpath.indexDir());
		LOG.info("createIndex: cleaning up the full-text index directory");
		CPathUtils.cleanupDirectory(dir);
		
		Model model = CPathUtils.loadMainBiopaxModel();
		Indexer indexer = new SearchEngine(model, cpath.indexDir());
		indexer.index();
		
 		LOG.info("createIndex: done.");
 	}

    
	/**
	 * Updates counts of pathways, etc. and saves in the Metadata table.
	 * 
     * This depends on the full-text index, which must have been created already
     * (otherwise, results will be wrong).
     * 
     * @throws IllegalStateException when not in maintenance mode
     */
    public static void updateCounts() {
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
		
        ClassPathXmlApplicationContext context = 
			new ClassPathXmlApplicationContext(new String[] {
					"classpath:META-INF/spring/applicationContext-jpa.xml"
					});
     	   	
     	//update counts of pathways, interactions, molecules
        CPathService service = (CPathService) context.getBean(CPathService.class);
     	
     	List<Metadata> pathwayMetadata = new ArrayList<Metadata>();
        for(Metadata md : service.metadata().findAll())
        	if(!md.isNotPathwayData())
        		pathwayMetadata.add(md);
     	
        for(Metadata md : pathwayMetadata) {
     		String name = md.standardName();
     		String[] filterByDatasourceNames = new String[]{md.getUri()};
     		
     		SearchResponse sr = (SearchResponse) service.search("*", 0, Pathway.class, filterByDatasourceNames, null);
     		md.setNumPathways(sr.getNumHits());
     		LOG.info(name + ", pathways: " + sr.getNumHits());
     		
     		sr = (SearchResponse) service.search("*", 0, Interaction.class, filterByDatasourceNames, null);
     		md.setNumInteractions(sr.getNumHits());
     		LOG.info(name + ", interactions: " + sr.getNumHits());
     		
     		sr = (SearchResponse) service.search("*", 0, PhysicalEntity.class, filterByDatasourceNames, null);
     		md.setNumPhysicalEntities(sr.getNumHits());
     		LOG.info(name + ", physical entities: " + sr.getNumHits());
     	}
     	
     	service.metadata().save(pathwayMetadata);
     	
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
     * @throws RuntimeException (when I/O errors), 
     * 			IllegalStateException (when not in maintenance mode)
     */
    public static void createBlacklist() throws IOException {
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");

		Model model = CPathUtils.loadMainBiopaxModel();

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

    
    /**
     * Performs cpath2 Merge stage.
     * @param force
     * 
     * @throws IllegalStateException when not maintenance mode
     */
    public static void runMerge(boolean force) {
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");
    	
		//disable 2nd level hibernate cache (ehcache)
		// otherwise the merger eventually fails with a weird exception
		// (this probably depends on the cache config. parameters)
		System.setProperty("net.sf.ehcache.disabled", "true");
		ClassPathXmlApplicationContext context = 
			new ClassPathXmlApplicationContext(new String[] {
					"classpath:META-INF/spring/applicationContext-jpa.xml"
			});
		
		LOG.info("runMerge: --force=" + force);
		CPathService service = context.getBean(CPathService.class);
		Merger merger = new Merger(service, force);		
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
        
		ClassPathXmlApplicationContext context =
            new ClassPathXmlApplicationContext(new String [] { 	
            		"classpath:META-INF/spring/applicationContext-jpa.xml", 
            		"classpath:META-INF/spring/applicationContext-validator.xml"
            		});
		CPathService service = context.getBean(CPathService.class);
		Validator validator = (Validator) context.getBean("validator");
        PreMerger premerger = new PreMerger(service, validator, provider);       
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
            new ClassPathXmlApplicationContext(new String [] { 	
            		"classpath:META-INF/spring/applicationContext-jpa.xml"
            		});
		CPathService service = context.getBean(CPathService.class);
		PreMerger premerger = new PreMerger(service, null, null);
        premerger.buildWarehouse();     
        context.close(); 
        
        //back to read-only schema mode (useful when called from the web admin app)
        System.setProperty("hibernate.hbm2ddl.auto", "validate");
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
            new ClassPathXmlApplicationContext(new String [] { 	
            		"classpath:META-INF/spring/applicationContext-jpa.xml", 
            		});
		CPathService service = context.getBean(CPathService.class);
        // grab the data
        service.addOrUpdateMetadata(location);       
        context.close(); 
        
        //back to read-only schema mode (useful when called from the web admin app)
        System.setProperty("hibernate.hbm2ddl.auto", "validate");
    }


	/**
	 * Exports a cpath2 BioPAX sub-model or full model to the specified file.
	 * 
	 * @param output - output BioPAX file name (path)
	 * @param uris - optional, the list of valid (existing) URIs to extract a sub-model
	 * @param outputAbsoluteUris - if true, all URIs in the BioPAX elements 
	 * and properties will be absolute (i.e., no local relative URIs, 
	 * such as rdf:ID="..." or rdf:resource="#...", will be there in the output file.) 
	 * @param datasources filter by data source if 'uris' is not empty
	 * @param types filter by biopax type if 'uris' is not empty
	 * 
	 * @throws IOException, IllegalStateException (in maintenance mode)
	 */
	public static void exportData(final String output, String[] uris, boolean outputAbsoluteUris, 
			String[] datasources, String[] types) throws IOException 
	{	
		if(uris == null) 
			uris = new String[]{};
		if(datasources == null) 
			datasources = new String[]{};
		if(types == null) 
			types = new String[]{};
		
		if(uris.length == 0 && (datasources.length > 0 || types.length > 0)) {
			ClassPathXmlApplicationContext context = 
					new ClassPathXmlApplicationContext(new String[] {
							"classpath:META-INF/spring/applicationContext-jpa.xml"
					});

			CPathService service = (CPathService) context.getBean(CPathService.class);
			Collection<String> selectedUris = new HashSet<String>();
			
			if(types.length>0) {
				//collect biopax object URIs of the specified types and sub-types, and data sources if specified
				//(child biopax elements will be auto-included during the export to OWL)
				for(String bpInterfaceName : types) {
					selectedUris.addAll(findAllUris(service, 
							biopaxTypeFromSimpleName(bpInterfaceName), datasources, null)); 
				}
			} else {
				//collect all Entity URIs filtered by the not empty data sources list
				//(child Gene, PhysicalEntity, UtilityClass biopax elements will be auto-included 
				// during the export to OWL; we do not want to export dangling Genes, PEs, etc., except for Complexes...)
				selectedUris.addAll(findAllUris(service, Pathway.class, datasources, null));
				selectedUris.addAll(findAllUris(service, Interaction.class, datasources, null)); 
				selectedUris.addAll(findAllUris(service, Complex.class, datasources, null)); 
			}

			context.close(); 
			
			uris = selectedUris.toArray(new String[] {});
		}
		
		Model model = CPathUtils.loadMainBiopaxModel();
		OutputStream os = new FileOutputStream(output);
		// export a sub-model from the main biopax database
		SimpleIOHandler sio = new SimpleIOHandler(BioPAXLevel.L3);
		sio.absoluteUris(outputAbsoluteUris);
		sio.convertToOWL(model, os, uris);
	}	
	
			
	private static Class<? extends BioPAXElement> biopaxTypeFromSimpleName(String type) 
	{	
		// 'type' (a BioPAX L3 interface class name) is case insensitive 
		for(Class<? extends BioPAXElement> c : SimpleEditorMap.L3
				.getKnownSubClassesOf(BioPAXElement.class)) 
		{
			if(c.getSimpleName().equalsIgnoreCase(type)) {
				if(c.isInterface() && BioPAXLevel.L3.getDefaultFactory().getImplClass(c) != null)
					return c; // interface
			}
		}
		throw new IllegalArgumentException("Illegal BioPAX class name '" + type);
	}


	private static String usage() 
	{
		final String NEWLINE = System.getProperty ( "line.separator" );
		StringBuilder toReturn = new StringBuilder();
		toReturn.append("Usage: <-command_name> [<command_args...>] " +
				"(- parameters within the square braces are optional.)" + NEWLINE);
		toReturn.append("commands:" + NEWLINE);
		// data import (instance creation) pipeline :
		toReturn.append(Cmd.FETCH_METADATA.toString() + " <url>" + NEWLINE);
		toReturn.append(Cmd.PREMERGE.toString() + " [<metadataId>]" + NEWLINE);
		toReturn.append(Cmd.CREATE_WAREHOUSE.toString() + NEWLINE);			
		toReturn.append(Cmd.MERGE.toString() + " [--force] (merge all pathway data; overwrites the main biopax model archive)"+ NEWLINE);
		toReturn.append(Cmd.INDEX.toString() + " (to build new full-text index of the main merged BioPAX db)" + NEWLINE);
        toReturn.append(Cmd.CREATE_BLACKLIST.toString() + " (creates blacklist.txt in the downloads directory)" + NEWLINE);
        toReturn.append(Cmd.CLEAR_CACHE.toString() + " (removes the cache directory)" + NEWLINE);
        toReturn.append(Cmd.UPDATE_COUNTS.toString() + " (re-calculates pathway, molecule, " +
        		"interaction counts per data source)" + NEWLINE);
        toReturn.append(Cmd.CREATE_DOWNLOADS.toString() + " (creates cpath2 BioPAX DB archives using several " +
        	"data formats, and also split by data source, organism)"  + NEWLINE);        
        // other useful (utility) commands
		toReturn.append(Cmd.EXPORT.toString() + " <output> [--uris=<uri,uri,..>] [--output-absolute-uris] [--datasources=<nameOrUri,..>] [--types=<interface,..>]" +
				"(writes to the output file the main BioPAX model, or a sub-model if the list of URIs or filter option is provided; " +
				"when --output-absolute-uris flag is present, all URIs there in the output BioPAX will be absolute; " +
				"when --datasources or/and --types flag is set, and 'uri' list is not, then the result model " +
				"will contain BioPAX elements that pass the filter by data source and/or type)" + NEWLINE); 
		toReturn.append(Cmd.CONVERT.toString() + " <biopax-file(.owl|.gz)> <output-file> <output format>" + NEWLINE);
		toReturn.append(Cmd.EXPORT_LOG.toString() + " [--clear] (export cpath2 assess log to the " +
				"CSV file in the data directory and, optionally, clear the table)" + NEWLINE);
		toReturn.append(Cmd.IMPORT_LOG.toString() + " [filename] (import cpath2 assess log from the specified " +
				"CSV file or from the default, if exists, in the data dir.)" + NEWLINE);
		toReturn.append(Cmd.ANALYSIS.toString() + " <classname> [--update] (execute custom code within the cPath2 BioPAX database; " +
				"if --update is set, one then should re-index and generate new 'downloads')" + NEWLINE);
		
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
		BiopaxConverter converter = new BiopaxConverter(new Blacklist(blacklist.getInputStream()));
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
		
		// create downloads directory inside the home dir. if not exist
		File f = new File(cpath.downloadsDir());
		if(!f.exists()) 
			f.mkdir();
    		
		ClassPathXmlApplicationContext context = 
			new ClassPathXmlApplicationContext(new String[] {
					"classpath:META-INF/spring/applicationContext-jpa.xml"});
		CPathService service = (CPathService) context.getBean(CPathService.class);
		
		//0) create an imported data summary file.txt (issue#23)
		PrintWriter writer = new PrintWriter(cpath.downloadsDir() + File.separator 
				+ "datasources.txt");
		String date = new SimpleDateFormat("d MMM yyyy").format(Calendar.getInstance().getTime());
		writer.println(StringUtils.join(Arrays.asList(
			"#CPATH2:", getInstance().getName(), "version", getInstance().getVersion(), date), " "));
		writer.println("#Columns:\t" + StringUtils.join(Arrays.asList(
			"ID", "DESCRIPTION", "TYPE", "HOMEPAGE", "PATHWAYS", "INTERACTIONS", "PHYS.ENTITIES"), "\t"));
		
		Iterable<Metadata> allMetadata = service.metadata().findAll();
		for(Metadata m : allMetadata) {
			writer.println(StringUtils.join(Arrays.asList(
				m.getUri(), m.getDescription(), m.getType(), m.getUrlToHomepage(), 
				m.getNumPathways(), m.getNumInteractions(), m.getNumPhysicalEntities()), "\t"));
		}		
		writer.flush();
		writer.close();		
		LOG.info("create-downloads: successfully generated the datasources summary file.");
		
		// generate/find all BioPAX archives:
		List<String> biopaxArchives = exportBiopax(service, allMetadata);
		
		context.close(); //DAO is not needed anymore
		    	
    	// 2) export to all other formats
        for(String biopaxArchive : biopaxArchives) {
        	//load model and convert to other formats
        	InputStream biopaxStream = null;
        	try {
        		biopaxStream = biopaxStream(biopaxArchive);
        	} catch (IOException e) {
        		LOG.error("Failed to read " + biopaxArchive + 
        				"; skipped (wasn't created before?)", e );
        		continue;
        	}
    		Model m = (new SimpleIOHandler()).convertFromOWL(biopaxStream);
    		for(OutputFormat format : EXPORT_TO_FORMATS) {
    			int idx = biopaxArchive.indexOf(".BIOPAX");
    			String prefix = biopaxArchive.substring(0, idx);
    			export(m, format, prefix);
    		}
    		m = null;
        } 	
	}


	private static Collection<String> findAllUris(CPathService service, 
    		Class<? extends BioPAXElement> type, String[] ds, String[] org) 
    {
    	Collection<String> uris = new ArrayList<String>();
    	
    	//call PaxtoolsDAO.search (skip service-tier cache) instead CPathService.search
    	SearchResponse resp = (SearchResponse) service.search("*", 0, type, ds, org);
    	int page = 0;
		while(!resp.isEmpty()) {
			for(SearchHit h : resp.getSearchHit())
				uris.add(h.getUri());
			//next page
			resp = (SearchResponse) service.search("*", ++page, type, ds, org);
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
	
	
	private static List<String> exportBiopax(CPathService service, Iterable<Metadata> allMetadata) 
			throws IOException 
	{
		List<String> files = new ArrayList<String>();
		
		// generate the complete biopax db export (all processes, no filters)
		String archiveName = cpath.mainModelFile();
		files.add(archiveName); //the archive already there exists (made during Merge step)
		
		//load the main model into RAM
		Model mainModel = CPathUtils.loadMainBiopaxModel();
		
    	// export by organism
        LOG.info("create-downloads: preparing data 'by organism' archives...");
        for(String org :  cpath.getOrganisms()) {
        	// generate archives for current organism
        	// hack: org.toLowerCase() is to tell by-organism from by-datasource archives (for usage stats...) 
        	archiveName = cpath.biopaxExportFileName(org.toLowerCase());
        	exportBiopax(mainModel, service, archiveName, null, new String[]{org});
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
        		//skip previously done files (this metadata has the same std. name as previously processed one)
        		if(!files.contains(archiveName)) {
        			exportBiopax(mainModel, service, archiveName,  md.getName().toArray(new String[]{}), null);
        			files.add(archiveName);
        		}
        	}
        }	
        
        return files;
	}	

	
	private static void exportBiopax(Model mainModel, CPathService service, String biopaxArchive,
			String[] datasources, String[] organisms) throws IOException
	{
        // check file exists
        if(!(new File(biopaxArchive)).exists()) {
        	LOG.info("create-downloads: creating new " + 	biopaxArchive);
        	
        	//find all entities (all child elements will be then exported too)    	  	
       		Collection<String> uris = new HashSet<String>();
           	uris.addAll(findAllUris(service, Pathway.class, datasources, organisms));  	
           	uris.addAll(findAllUris(service, Interaction.class, datasources, organisms));
           	uris.addAll(findAllUris(service, Complex.class, datasources, organisms));
        	
    		// export objects found above to a new biopax archive        	
        	if(!uris.isEmpty()) {
        		OutputStream os = new GZIPOutputStream(new FileOutputStream(biopaxArchive));
        		SimpleIOHandler sio = new SimpleIOHandler(BioPAXLevel.L3);
//        		sio.absoluteUris(true);
        		sio.convertToOWL(mainModel, os, uris.toArray(new String[]{}));	
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
			return format + ".tsv";
		default://fail - biopax is treated specially, not here
			throw new IllegalArgumentException(format.toString() + " not allowed.");
		}
	}

}
