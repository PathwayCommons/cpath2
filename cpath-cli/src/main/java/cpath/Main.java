package cpath;

import static cpath.config.CPathSettings.*;

import cpath.config.CPathSettings;
import cpath.service.Merger;
import cpath.service.PreMerger;
import cpath.jpa.Metadata;
import cpath.jpa.Metadata.METADATA_TYPE;
import cpath.jpa.MetadataRepository;
import cpath.service.*;
import cpath.service.jaxb.*;

import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.pattern.miner.BlacklistGenerator3;
import org.biopax.paxtools.pattern.util.Blacklist;
import org.biopax.validator.api.Validator;
import org.h2.tools.Csv;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.Assert;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * The cPath2 console application for a pathway data manager
 * to create or re-build a cPath2 instance
 * (metadata db, BioPAX model, full-text index, downloads)
 */
public final class Main {
	private static final Logger LOG = LoggerFactory.getLogger(Main.class);
	private static final CPathSettings cpath = CPathSettings.getInstance();

    public static enum Cmd {
        // command types
        METADATA("-metadata"),
		PREMERGE("-premerge"),
		MERGE("-merge"),
    	INDEX("-index"),
		EXPORT("-export"),
        ANALYSIS("-run-analysis"),
		PACKAGE("-pack"),
		;
        //name to use as the application's command line argument
        private String command;
        
        // contructor
        Cmd(String command) { this.command = command; }

        @Override
        public String toString() { return command; }
    }
    
 
    /**
     * The big deal main.
     * 
     * @param params String[]
     */    
    public static void main(String[] params) throws Exception {
    	// "CPATH2_HOME" system option must be set (except for unit testing)
    	Assert.hasText(cpath.property(HOME_DIR)); 

    	if(!Charset.defaultCharset().equals(Charset.forName("UTF-8")))
			LOG.error("Default Charset, " + Charset.defaultCharset() +
					" (is NOT 'UTF-8'); problems with input data are possible...");

    	// Cleanup arguments - remove empty/null strings from the end, which were
    	// possibly the result of calling this method from a shell script
    	final List<String> filteredArguments = new ArrayList<String>();
    	for(String a : params)
    		if(a != null && !a.isEmpty() && !a.equalsIgnoreCase("null"))
				filteredArguments.add(a);
    	final String[] args = filteredArguments.toArray(new String[]{});
    	LOG.debug("Command-line arguments were: " + Arrays.toString(args));

        if (args.length == 0 || args[0].isEmpty()) {
            LOG.error("No cPath2 command name nor arguments provided; exit.");
			System.err.println(Main.usage());
            System.exit(-1);
        }

		if (args[0].equals(Cmd.INDEX.toString())) {
			
			index();
			
		}
		else if (args[0].equals(Cmd.METADATA.toString())) {
			if (args.length == 1) {
				fetchMetadata("file:" + cpath.property(PROP_METADATA_LOCATION));
			} else {
				fetchMetadata(args[1]);
			}
		}
		else if (args[0].equals(Cmd.PREMERGE.toString())) {
			boolean rebuildWarehouse = false;
			boolean force = false;
			if (args.length > 1) {
				for(int i=1;i<args.length;i++) {
					if(args[i].equalsIgnoreCase("--buildWarehouse"))
						rebuildWarehouse = true;
					if(args[i].equalsIgnoreCase("--force"))
						force = true;
				}
			}
			runPremerge(rebuildWarehouse, force);
		}
		else if (args[0].equals(Cmd.MERGE.toString())) {

			runMerge();

		}
		else if (args[0].equals(Cmd.EXPORT.toString())) {
			//(the first args[0] is the command name
			if (args.length < 2) {
				LOG.info("Default mode: creating datasources.txt, summary.txt and data archives in the downloads dir...");
				createDownloads();
			} else {
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
		}
		else if (args[0].equals(Cmd.ANALYSIS.toString())) {
			if (args.length < 2) 
				fail(args, "No Analysis implementation class provided.");	
			else if(args.length == 2)
				executeAnalysis(args[1], true);
			else if(args.length > 2 && "--update".equalsIgnoreCase(args[2]))
				executeAnalysis(args[1], false);
		
		}
		else if (args[0].equals(Cmd.PACKAGE.toString())) {
			pack();
		}
		else {
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
 					new GZIPOutputStream(new FileOutputStream(cpath.mainModelFile())));
 			} catch (Exception e) {
 				throw new RuntimeException("Failed updating the main BioPAX archive!", e);
 			}
 			
 			LOG.warn("The main BioPAX model was modified; "
 				+ "do not forget to re-index, update counts, re-export other files, etc.");
 		}
 			
	}

    public static void pack() throws IOException {
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");

		//backup and purge the cpath2 intermediate id-mapping db to save disk space
// 		String h2db = cpath.dataDir() + File.separator + "cpath2.h2.db"; //make a copy?
 		String idmap = cpath.dataDir() + File.separator + "idmapping.csv";
 		Connection conn = null;
 		try {
			Class.forName("org.h2.Driver");
			conn = DriverManager.getConnection("jdbc:h2:" + cpath.homeDir() + "/cpath2", "sa", "");
			//backup
			new Csv().write(conn, idmap, "select * from mappings", "UTF-8");
//			//clear all mapping data TODO: it hangs...
//			conn.createStatement().executeUpdate("delete from mappings;");
//			conn.commit();
		}catch (Exception e) {
 			LOG.error("pack(), rolling back due to an exception...", e);
			try {conn.rollback(); conn.close();} catch (Exception ex) {}
			throw new RuntimeException(e);
		} finally {
			try {conn.close();} catch (Exception e) {}
		}

		//TODO: generate downloads.zip (move almost all files from cpath2 /data and /downloads folders to the archive)
	}

	private static void fail(String[] args, String details) {
        throw new IllegalArgumentException(
        	"Invalid cpath2 command: " +  Arrays.toString(args)
        	+ "; " + details);		
	}
	
    
	/**
     * Builds a new BioPAX full-text index,creates the black list or ubiquitous molecules,
	 * and calculates/updates the total no. of pathways, interactions, physical entities in the main db.
     * 
	 * @throws IOException
     * @throws IllegalStateException when not in maintenance mode
     */
    public static void index() throws IOException {
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] { "classpath:META-INF/spring/applicationContext-jpa.xml" });
		final CPathService service = context.getBean(CPathService.class);

		LOG.info("index: indexing...");
		service.index();

		context.close();

		LOG.info("Generating the blacklist.txt...");
		//Generates, if not exist, the blacklist.txt -
		//to exclude/keep ubiquitous small molecules (e.g. ATP)
		//from graph query and output format converter results.
		BlacklistGenerator3 gen = new BlacklistGenerator3();
		Blacklist blacklist = gen.generateBlacklist(service.getModel());
		// Write all the blacklisted ids to the output
		if(blacklist != null)
			blacklist.write(new FileOutputStream(cpath.blacklistFile()));

		LOG.info("index: all done.");
 	}

    /**
     * Performs cpath2 Merge stage.
     * 
     * @throws IllegalStateException when not maintenance mode
     */
    public static void runMerge() {
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

		//create CPathService bean that provides access to JPA repositories (metadata, mapping)
		CPathService service = context.getBean(CPathService.class);
		Merger merger = new Merger(service);
		merger.merge();
		//at the end, it saves the resulting integrated main biopax model to a special file at known location.
		
		context.close();		
	}

	
    /**
     * Executes the premerge stage:
     * organize, clean, convert, validate, normalize pathway/interaction data,
	 * and create BioPAX utility class objects warehouse and id-mapping.
     *
	 * @param rebuildWarehouse
     * @throws IllegalStateException when not maintenance mode
     */
	public static void runPremerge(boolean rebuildWarehouse, boolean force) {
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");		
		
        LOG.info("runPremerge: initializing (DAO, validator, premerger)...");
		//test that officially supported organisms are specified (throws a runtime exception otherwise)
		cpath.getOrganismTaxonomyIds();
		LOG.info("runPremerge: this instance is configured to integrate and query " +
				" bio data about following organisms: " + Arrays.toString(cpath.getOrganisms()));

		System.setProperty("hibernate.hbm2ddl.auto", "update");
		System.setProperty("net.sf.ehcache.disabled", "true");
		ClassPathXmlApplicationContext context =
            new ClassPathXmlApplicationContext(new String [] { 	
            		"classpath:META-INF/spring/applicationContext-jpa.xml", 
            		"classpath:META-INF/spring/applicationContext-validator.xml"
            		});
		CPathService service = context.getBean(CPathService.class);
		Validator validator = (Validator) context.getBean("validator");
        PreMerger premerger = new PreMerger(service, validator, force);
        premerger.premerge();

		// create the Warehouse BioPAX model (in the downloads dir) and id-mapping db table
		if(rebuildWarehouse)
			premerger.buildWarehouse();

		//shutdown the Spring context (services and databases)
        context.close();

		//back to read-only schema mode (useful when called from the web Main page)
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
		
		//load the model
		Model model = CPathUtils.loadMainBiopaxModel();
		LOG.info("Loaded the BioPAX Model");

		if(uris.length == 0 && (datasources.length > 0 || types.length > 0)) {
					
			// initialize the search engine
			Searcher searcher = new SearchEngine(model, cpath.indexDir());

			Collection<String> selectedUris = new HashSet<String>();
			
			if(types.length>0) {
				//collect biopax object URIs of the specified types and sub-types, and data sources if specified
				//(child biopax elements will be auto-included during the export to OWL)
				for(String bpInterfaceName : types) {
					selectedUris.addAll(findAllUris(searcher, 
							biopaxTypeFromSimpleName(bpInterfaceName), datasources, null)); 
				}
			} else {
				//collect all Entity URIs filtered by the not empty data sources list
				//(child Gene, PhysicalEntity, UtilityClass biopax elements will be auto-included 
				// during the export to OWL; we do not want to export dangling Genes, PEs, etc., except for Complexes...)
				selectedUris.addAll(findAllUris(searcher, Pathway.class, datasources, null));
				selectedUris.addAll(findAllUris(searcher, Interaction.class, datasources, null)); 
				selectedUris.addAll(findAllUris(searcher, Complex.class, datasources, null)); 
			}
		
			uris = selectedUris.toArray(new String[] {});
		}
		
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
		toReturn.append(Cmd.METADATA.toString() + " <url> (fetch Metadata configuration (default: " +
				"use metadata.conf file in current directory))" + NEWLINE);
		toReturn.append(Cmd.PREMERGE.toString() + " [--buildWarehouse] [--force]" +
				"(organize, clean, convert, normalize input data;" +
				" create metadata db and create or rebuild the BioPAX utility type objects Warehouse)" + NEWLINE);
		toReturn.append(Cmd.MERGE.toString() + " (merge all pathway data; overwrites the main biopax model archive)"+ NEWLINE);
		toReturn.append(Cmd.INDEX.toString() + " (build new full-text index of the main merged BioPAX db;" +
				"create blacklist.txt in the downloads directory; re-calculates the no. pathways, molecules and " +
        		"interactions per data source)" + NEWLINE);
		toReturn.append(Cmd.EXPORT.toString()
				+ " [filename] [--uris=<uri,uri,..>] [--output-absolute-uris] [--datasources=<nameOrUri,..>] [--types=<interface,..>]" +
				"(when no arguments provided, it generates the default detailed pathway data and organism-specific " +
				"BioPAX archives and datasources.txt in the downloads sub-directory; plus, " +
				"summary.txt, and convert.sh script (for exporting the BioPAX files to various formats with Paxtools). " +
				"If [filename] is provided, it only exports the main BioPAX model or a sub-model " +
				"(if the list of URIs or filter option is provided): " +
				"when --output-absolute-uris flag is present, all URIs there in the output BioPAX will be absolute; " +
				"when --datasources or/and --types flag is set, and 'uri' list is not, then the result model " +
				"will contain BioPAX elements that pass the filter by data source and/or type)" + NEWLINE);
		toReturn.append(Cmd.ANALYSIS.toString() + " <classname> [--update] (execute custom code within the cPath2 BioPAX database; " +
				"if --update is set, one then should re-index and generate new 'downloads')" + NEWLINE);
		toReturn.append(Cmd.PACKAGE.toString() + " (final cleaning and packaging: moves all result and intermediate data " +
				"files into <prefix>.downloads.zip and only keeps data and files required for running the cpath2 server; " +
				"so, please backup )" + NEWLINE);
		return toReturn.toString();
	}


    /**
     * Create cpath2 downloads 
     * (exports the db to various formats)
     * 
     * @throws IOException, IllegalStateException (when not in maintenance mode), InterruptedException
     */
	public static void createDownloads() throws IOException, InterruptedException
    {
		LOG.info("createDownloads(), started...");
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Maintenance mode is not enabled.");

		ClassPathXmlApplicationContext context = 
			new ClassPathXmlApplicationContext(new String[] {
					"classpath:META-INF/spring/applicationContext-jpa.xml"});
		
		MetadataRepository metadataRepository = (MetadataRepository) context.getBean(MetadataRepository.class);
		// create an imported data summary file.txt (issue#23)
		PrintWriter writer = new PrintWriter(cpath.downloadsDir() + File.separator + "datasources.txt");
		String date = new SimpleDateFormat("d MMM yyyy").format(Calendar.getInstance().getTime());
		writer.println(StringUtils.join(Arrays.asList(
			"#CPATH2:", getInstance().getName(), "version", getInstance().getVersion(), date), " "));
		writer.println("#Columns:\t" + StringUtils.join(Arrays.asList(
			"ID", "DESCRIPTION", "TYPE", "HOMEPAGE", "PATHWAYS", "INTERACTIONS", "PARTICIPANTS"), "\t"));		
		Iterable<Metadata> allMetadata = metadataRepository.findAll();
		for(Metadata m : allMetadata) {
			writer.println(StringUtils.join(Arrays.asList(
				m.getUri(), m.getDescription(), m.getType(), m.getUrlToHomepage(), 
				m.getNumPathways(), m.getNumInteractions(), m.getNumPhysicalEntities()), "\t"));
		}		
		writer.flush();
		writer.close();	
		// destroy the Spring context, release some resources
		context.close();
		LOG.info("successfully generated the datasources.txt file.");
			
		//load the main model
		LOG.info("loading the Main BioPAX Model...");
		Model model = CPathUtils.loadMainBiopaxModel();
		LOG.info("successfully read the Main BioPAX Model");

		LOG.info("init the full-text search engine...");
		final Searcher searcher = new SearchEngine(model, cpath.indexDir());
		// generate/find special BioPAX archives:
		// by-organism (if many),
 		createBySpeciesBiopax(model, searcher);
		// and - detailed pathway data (exclude all PSI-MI sources):
		createDetailedBiopax(model, searcher, allMetadata);

		//auto-generate export.sh script (to run Paxtools commands for exporting BioPAX to other formats)
		LOG.info("writing 'export.sh' script to convert the BioPAX models to SIF, GSEA, SBGN...");
		final String commonPrefix = cpath.exportArchivePrefix(); //e.g., PathwayCommons8.
		writer = new PrintWriter(cpath.exportScriptFile());
		writer.println("#!/bin/sh");
		writer.println("# An auto-generated script for converting the BioPAX data archives");
		writer.println("# in the CPATH2_HOME/downloads directory to other formats.");
		writer.println("# There must be blacklist.txt and paxtools.jar files already.");
		writer.println("# Change to the downloads/ and run as:");
		writer.println("# sh export.sh &");
		for(Metadata md : allMetadata) {
			if(!md.isNotPathwayData()) //skip warehouse metadata
				writeScriptCommands(cpath.biopaxFileName(md.getIdentifier()), writer, md.getNumPathways()>0);
		}
		//write commands to the script file for 'All'(main) and 'Detailed' BioPAX input files:
		writeScriptCommands(cpath.biopaxFileName("Detailed"), writer, true);
		writeScriptCommands(cpath.biopaxFileName("All"), writer, true);
		//rename properly those SIF files that were cut from corresponding extended SIF (.txt) ones
		writer.println("rename 's/txt\\.sif/sif/' *.txt.sif");
		writer.println(String.format("gzip %s*.txt %s*.sif %s*.gmt %s*.xml",
				commonPrefix, commonPrefix, commonPrefix, commonPrefix));
		writer.println("echo \"All done.\"");
		writer.close();

		LOG.info("createDownloads: done.");
	}

	private static void writeScriptCommands(String bpFilename, PrintWriter writer, boolean exportToGSEA) {
		final String javaRunPaxtools = "nohup $JAVA_HOME/bin/java -Xmx32g -jar paxtools.jar";
		//make output file name prefix that includes datasource and ends with '.':
		final String prefix = bpFilename.substring(0, bpFilename.indexOf("BIOPAX."));
		final String commaSepTaxonomyIds = StringUtils.join(cpath.getOrganismTaxonomyIds(),',');

		if(exportToGSEA) {
			writer.println(String.format("%s %s '%s' '%s' %s 2>&1 &", javaRunPaxtools, "toGSEA", bpFilename,
				prefix + "hgnc.gmt", "'hgnc symbol' 'organisms=" + commaSepTaxonomyIds + "'"));//'hgnc symbol' - important
			writer.println(String.format("%s %s '%s' '%s' %s 2>&1 &", javaRunPaxtools, "toGSEA", bpFilename,
				prefix + "uniprot.gmt", "'uniprot' 'organisms=" + commaSepTaxonomyIds + "'"));
			writer.println("wait"); //important
			writer.println("echo \"Done converting "+bpFilename+" to GSEA.\"");
		}

		writer.println(String.format("%s %s '%s' '%s' %s 2>&1 &", javaRunPaxtools, "toSIF", bpFilename,
			prefix + "hgnc.txt", "seqDb=hgnc -extended -andSif exclude=neighbor_of"));//'hgnc symbol' or 'hgnc' does not matter

		//TODO: UniProt based extended SIF can be huge and takes too long to generate... won't make it now

		writer.println("wait"); //important
		writer.println("echo \"Done converting " + bpFilename + " to SIF.\"");
	}

	private static Collection<String> findAllUris(Searcher searcher, 
    		Class<? extends BioPAXElement> type, String[] ds, String[] org) 
    {
    	Collection<String> uris = new ArrayList<String>();
    	
    	SearchResponse resp = searcher.search("*", 0, type, ds, org);
    	int page = 0;
    	while(!resp.isEmpty()) {
    		for(SearchHit h : resp.getSearchHit())
    			uris.add(h.getUri());
    		//next page
    		resp = searcher.search("*", ++page, type, ds, org);
    	}
    	
    	LOG.info("findAllUris(in "+type.getSimpleName()+", ds: "+Arrays.toString(ds)+", org: "+Arrays.toString(org)+") "
    			+ "collected " + uris.size());
    	
    	return uris;
    }

	private static void createDetailedBiopax(final Model mainModel, Searcher searcher, Iterable<Metadata> allMetadata)
	{
		//collect BioPAX pathway data source names
		final Set<String> pathwayDataSources = new HashSet<String>();
		for(Metadata md : allMetadata) {
			if (md.getType() == METADATA_TYPE.BIOPAX)  //TODO: consider 'SBML' type as well
				pathwayDataSources.add(md.standardName());
		}
		final String archiveName = cpath.biopaxFileNameFull("Detailed");
		exportBiopax(mainModel, searcher, archiveName, pathwayDataSources.toArray(new String[]{}), null);
	}

	private static void createBySpeciesBiopax(final Model mainModel, Searcher searcher) {
    	// export by organism (name)
		Set<String> organisms = cpath.getOrganismTaxonomyIds();
        if(organisms.size()>1) {
        	LOG.info("splitting the main BioPAX model by organism, into " + organisms.size() + " BioPAX files...");
        	for(String organism :  organisms) {
        		String archiveName = cpath.biopaxFileNameFull(organism);
				exportBiopax(mainModel, searcher, archiveName, null, new String[]{organism});
        	}
        } else {
        	LOG.info("won't generate any 'by organism' archives, for only one " +
				Arrays.toString(cpath.getOrganisms()) + " is listed in the cpath2.properties");
        }
	}

	
	private static void exportBiopax(final Model mainModel, final Searcher searcher,
									 final String biopaxArchive, final String[] datasources, final String[] organisms){
        // check file exists
        if(!(new File(biopaxArchive)).exists()) {
        	LOG.info("creating new " + 	biopaxArchive);
			try {
				//find all entities (all child elements will be then exported too)
				Collection<String> uris = new HashSet<String>();
				uris.addAll(findAllUris(searcher, Pathway.class, datasources, organisms));
				uris.addAll(findAllUris(searcher, Interaction.class, datasources, organisms));
				uris.addAll(findAllUris(searcher, Complex.class, datasources, organisms));
				// export objects found above to a new biopax archive
				if(!uris.isEmpty()) {
					OutputStream os = new GZIPOutputStream(new FileOutputStream(biopaxArchive));
					SimpleIOHandler sio = new SimpleIOHandler(BioPAXLevel.L3);
					sio.convertToOWL(mainModel, os, uris.toArray(new String[]{}));
					LOG.info("successfully created " + 	biopaxArchive);
				} else {
					LOG.info("no pathways/interactions found; skipping " + 	biopaxArchive);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
        } else {
        	LOG.info("skipped due to file already exists: " + biopaxArchive);
        }   		
	}

}
