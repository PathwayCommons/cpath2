package cpath.service;

import cpath.service.api.Analysis;
import cpath.service.api.CPathService;
import cpath.service.api.Searcher;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jpa.Metadata;
import cpath.service.jpa.Metadata.METADATA_TYPE;

import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.pattern.miner.BlacklistGenerator3;
import org.biopax.paxtools.pattern.util.Blacklist;
import org.biopax.validator.api.Validator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * The cPath2 console application for a pathway data manager
 * to build a new cPath2 instance (metadata, BioPAX model, full-text index, downloads)
 */
@Configuration
@Profile({"admin"})
public class ConsoleConfiguration implements CommandLineRunner {
  private static final Logger LOG = LoggerFactory.getLogger(ConsoleConfiguration.class);

  @Autowired
  private CPathService service;

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private Environment environment;

  public enum Cmd {
    // command types
    METADATA("-metadata"),
    PREMERGE("-premerge"),
    MERGE("-merge"),
    INDEX("-index"),
    EXPORT("-export"),
    ANALYSIS("-run-analysis"),;
    //name to use as the application's command line argument
    private String command;

    Cmd(String command) {
      this.command = command;
    }

    @Override
    public String toString() {
      return command;
    }
  }

  final static String javaRunPaxtools = "nohup $JAVA_HOME/bin/java -Xmx32g -jar paxtools.jar";


  public static void main(String[] args) {
    SpringApplication consoleApp = new SpringApplication(ConsoleConfiguration.class);

    if (args[0].equals(Cmd.PREMERGE.toString())) {
      consoleApp.setAdditionalProfiles("premerge"); //enables biopax-validator configuration
    }

//ConfigurableApplicationContext context =
    consoleApp.run(args);
//context.getBean(CPathService.class).init();//do not (that's for the web app)
  }

  @Override
  public void run(String... params) throws Exception
  {
    if (!Charset.defaultCharset().equals(Charset.forName("UTF-8")))
      LOG.error("Default Charset, " + Charset.defaultCharset() +
          " (is NOT 'UTF-8'); problems with input data are possible...");

    final String[] activeProfiles = environment.getActiveProfiles();
    final boolean isAdminProfile = Arrays.stream(activeProfiles).anyMatch("admin"::equals);
    final boolean isProductionProfile = Arrays.stream(activeProfiles).anyMatch("prod"::equals);
    LOG.info(String.format("Active profiles: %s\nAdmin mode: %s",
          Arrays.toString(activeProfiles), (isAdminProfile)?"enabled":"disabled")
    );

    // Cleanup arguments - remove empty/null strings from the end, which were
    // possibly the result of calling this method from a shell script
    final List<String> filteredArguments = new ArrayList<>();
    for (String a : params)
      if (a != null && !a.isEmpty() && !a.equalsIgnoreCase("null"))
        filteredArguments.add(a);
    final String[] args = filteredArguments.toArray(new String[]{});

    if (args.length == 0 || args[0].isEmpty()) {
      LOG.warn("No command line arguments.");
      if(isProductionProfile) System.err.println(usage());
      return;
    } else LOG.debug("Command-line arguments were: " + Arrays.toString(args));

    // process command line args and do smth.
    if (args[0].equals(Cmd.INDEX.toString())) {

      index();

    } else if (args[0].equals(Cmd.METADATA.toString())) {
      if (args.length == 1) {
        fetchMetadata("file:" + service.settings().getMetadataLocation());
      } else {
        fetchMetadata(args[1]);
      }
    } else if (args[0].equals(Cmd.PREMERGE.toString())) {
      boolean rebuildWarehouse = false;
      boolean force = false;
      if (args.length > 1) {
        for (int i = 1; i < args.length; i++) {
          if (args[i].equalsIgnoreCase("--buildWarehouse"))
            rebuildWarehouse = true;
          if (args[i].equalsIgnoreCase("--force"))
            force = true;
        }
      }
      runPremerge(rebuildWarehouse, force);
    } else if (args[0].equals(Cmd.MERGE.toString())) {

      runMerge();

    } else if (args[0].equals(Cmd.EXPORT.toString())) {
      //(the first args[0] is the command name
      if (args.length < 2) {
        LOG.info("Default mode: creating datasources.txt, " +
            "summary.txt and data archives in the downloads dir...");
        createDownloads();
      } else {
        String[] uris = new String[]{};
        String[] datasources = new String[]{};
        String[] types = new String[]{};
        boolean absoluteUris = false;
        for (int i = 2; i < args.length; i++) {
          if (args[i].equalsIgnoreCase("--output-absolute-uris"))
            absoluteUris = true;
          else if (args[i].toLowerCase().startsWith("--datasources="))
            datasources = args[i].substring(14).split(",");
          else if (args[i].toLowerCase().startsWith("--types="))
            types = args[i].substring(8).split(",");
          else if (args[i].toLowerCase().startsWith("--uris="))
            uris = args[i].substring(7).split(",");
          else
            LOG.error("Skipped unrecognized argument: " + args[i]);
        }
        exportData(args[1], uris, absoluteUris, datasources, types);
      }
    } else if (args[0].equals(Cmd.ANALYSIS.toString())) {
      if (args.length < 2)
        fail(args, "No Analysis implementation class provided.");
      else if (args.length == 2)
        executeAnalysis(args[1], true);
      else if (args.length > 2 && "--update".equalsIgnoreCase(args[2]))
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
  public void executeAnalysis(String analysisClass, boolean readOnly) {
    Analysis analysis = null;
    try {
      Class<Analysis> c = (Class<Analysis>) Class.forName(analysisClass);
      analysis = c.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    Model model = CPathUtils.importFromTheArchive(service.settings().mainModelFile());
    analysis.execute(model);

    if (!readOnly) { //replace the main BioPAX model archive
      try {
        new SimpleIOHandler(BioPAXLevel.L3).convertToOWL(model,
            new GZIPOutputStream(new FileOutputStream(service.settings().mainModelFile())));
      } catch (Exception e) {
        throw new RuntimeException("Failed updating the main BioPAX archive!", e);
      }

      LOG.warn("The main BioPAX model was modified; "
          + "do not forget to re-index, update counts, re-export other files, etc.");
    }

  }

  private void fail(String[] args, String details) {
    throw new IllegalArgumentException(
        "Invalid cpath2 command: " + Arrays.toString(args) + "; " + details);
  }


  /**
   * Builds a new BioPAX full-text index,creates the black list or ubiquitous molecules,
   * and calculates/updates the total no. of pathways, interactions, physical entities in the main db.
   *
   * @throws IOException
   * @throws IllegalStateException when not in maintenance mode
   */
  public void index() throws IOException {
    LOG.info("index: indexing...");
    service.index();

    // Updates counts of pathways, etc. and saves in the Metadata table.
    // This depends on the full-text index, which must have been created already (otherwise, results will be wrong).
    LOG.info("Updating pathway/interaction/participant counts per data source...");
    List<Metadata> pathwayMetadata = new ArrayList<>();
    for (Metadata md : service.metadata().findAll())
      if (!md.isNotPathwayData())
        pathwayMetadata.add(md);
    // update counts for each non-warehouse metadata entry
    for (Metadata md : pathwayMetadata) {
      Model m = service.loadBiopaxModelByDatasource(md); //to count objects, by type
      String name = md.standardName();
      md.setNumPathways(m.getObjects(Pathway.class).size());
      LOG.info(name + " - pathways: " + md.getNumPathways());
      md.setNumInteractions(m.getObjects(Interaction.class).size());
      LOG.info(name + " - interactions: " + md.getNumInteractions());
      md.setNumPhysicalEntities(m.getObjects(PhysicalEntity.class).size() + m.getObjects(Gene.class).size());
      LOG.info(name + " - participants: " + md.getNumPhysicalEntities());
    }
    service.metadata().saveAll(pathwayMetadata);

    LOG.info("Generating the blacklist.txt...");
    //Generates, if not exist, the blacklist.txt -
    //to exclude/keep ubiquitous small molecules (e.g. ATP)
    //from graph query and output format converter results.
    BlacklistGenerator3 gen = new BlacklistGenerator3();
    Blacklist blacklist = gen.generateBlacklist(service.getModel());
    // Write all the blacklisted ids to the output
    if (blacklist != null)
      blacklist.write(new FileOutputStream(service.settings().blacklistFile()));

    LOG.info("index: all done.");
  }

  /**
   * Performs cpath2 Merge stage.
   *
   * @throws IllegalStateException when not maintenance mode
   */
  public void runMerge() {
    //disable 2nd level hibernate cache (ehcache)
    // otherwise the merger eventually fails with a weird exception
    // (this probably depends on the cache config. parameters)
    System.setProperty("net.sf.ehcache.disabled", "true");
    Merger merger = new Merger(service);
    merger.merge();
  }


  /**
   * Executes the premerge stage:
   * organize, clean, convert, validate, normalize pathway/interaction data,
   * and create BioPAX utility class objects warehouse and id-mapping.
   *
   * @param rebuildWarehouse
   * @throws IllegalStateException when not maintenance mode
   */
  public void runPremerge(boolean rebuildWarehouse, boolean force) {
    LOG.info("runPremerge: initializing (DAO, validator, premerger)...");
    //test that officially supported organisms are specified (throws a runtime exception otherwise)
    service.settings().getOrganismTaxonomyIds();
    LOG.info("runPremerge: this instance is configured to integrate and query " +
        " bio data about following organisms: " + Arrays.toString(service.settings().getOrganisms()));

    System.setProperty("hibernate.hbm2ddl.auto", "update");
    System.setProperty("net.sf.ehcache.disabled", "true");

    Validator validator = applicationContext.getBean(Validator.class);
    PreMerger premerger = new PreMerger(service, validator, force);
    premerger.premerge();

    // create the Warehouse BioPAX model (in the downloads dir) and id-mapping db table
    if (rebuildWarehouse)
      premerger.buildWarehouse();

    //back to read-only schema mode (useful when called from the web Main page)
    System.setProperty("hibernate.hbm2ddl.auto", "validate");
  }

  /**
   * Helper function to get provider metadata.
   *
   * @param location String PROVIDER_URL or local file.
   * @throws IOException, IllegalStateException (when not maintenance mode)
   */
  public void fetchMetadata(final String location) throws IOException {
    System.setProperty("hibernate.hbm2ddl.auto", "update");
    // grab the data
    service.addOrUpdateMetadata(location);
    //back to read-only schema mode (useful when called from the web admin app)
    System.setProperty("hibernate.hbm2ddl.auto", "validate");
  }


  /**
   * Exports a cpath2 BioPAX sub-model or full model to the specified file.
   *
   * @param output             - output BioPAX file name (path)
   * @param uris               - optional, the list of valid (existing) URIs to extract a sub-model
   * @param outputAbsoluteUris - if true, all URIs in the BioPAX elements
   *                           and properties will be absolute (i.e., no local relative URIs,
   *                           such as rdf:ID="..." or rdf:resource="#...", will be there in the output file.)
   * @param datasources        filter by data source if 'uris' is not empty
   * @param types              filter by biopax type if 'uris' is not empty
   * @throws IOException, IllegalStateException (in maintenance mode)
   */
  public void exportData(final String output, String[] uris, boolean outputAbsoluteUris,
                         String[] datasources, String[] types) throws IOException {
    if (uris == null)
      uris = new String[]{};
    if (datasources == null)
      datasources = new String[]{};
    if (types == null)
      types = new String[]{};

    //load the model
    Model model = CPathUtils.importFromTheArchive(service.settings().mainModelFile());
    LOG.info("Loaded the BioPAX Model");

    if (uris.length == 0 && (datasources.length > 0 || types.length > 0)) {

      // initialize the search engine
      Searcher searcher = new SearchEngine(model, service.settings().indexDir());

      Collection<String> selectedUris = new HashSet<>();

      if (types.length > 0) {
        //collect biopax object URIs of the specified types and sub-types, and data sources if specified
        //(child biopax elements will be auto-included during the export to OWL)
        for (String bpInterfaceName : types) {
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

      uris = selectedUris.toArray(new String[]{});
    }

    OutputStream os = new FileOutputStream(output);
    // export a sub-model from the main biopax database
    SimpleIOHandler sio = new SimpleIOHandler(BioPAXLevel.L3);
    sio.absoluteUris(outputAbsoluteUris);
    sio.convertToOWL(model, os, uris);
  }


  private Class<? extends BioPAXElement> biopaxTypeFromSimpleName(String type) {
    // 'type' (a BioPAX L3 interface class name) is case insensitive
    for (Class<? extends BioPAXElement> c : SimpleEditorMap.L3
        .getKnownSubClassesOf(BioPAXElement.class)) {
      if (c.getSimpleName().equalsIgnoreCase(type)) {
        if (c.isInterface() && BioPAXLevel.L3.getDefaultFactory().getImplClass(c) != null)
          return c; // interface
      }
    }
    throw new IllegalArgumentException("Illegal BioPAX class name '" + type);
  }


  private String usage() {
    final String NEWLINE = System.getProperty("line.separator");
    StringBuilder toReturn = new StringBuilder();
    toReturn.append("Usage: <-command_name> [<command_args...>] " +
        "(- parameters within the square braces are optional.)" + NEWLINE);
    toReturn.append("commands:" + NEWLINE);
    toReturn.append(Cmd.METADATA.toString() + " <url> (fetch Metadata configuration (default: " +
        "use metadata.conf file in current directory))" + NEWLINE);
    toReturn.append(Cmd.PREMERGE.toString() + " [--buildWarehouse] [--force]" +
        "(organize, clean, convert, normalize input data;" +
        " create metadata db and create or rebuild the BioPAX utility type objects Warehouse)" + NEWLINE);
    toReturn.append(Cmd.MERGE.toString() + " (merge all pathway data; overwrites the main biopax model archive)" + NEWLINE);
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
    return toReturn.toString();
  }


  /**
   * Create cpath2 downloads
   * (exports the db to various formats)
   *
   * @throws IOException, IllegalStateException (when not in maintenance mode), InterruptedException
   */
  public void createDownloads() throws IOException, InterruptedException {
    LOG.info("createDownloads(), started...");

    //load the main model
    LOG.info("loading the Main BioPAX Model...");
    Model model = CPathUtils.importFromTheArchive(service.settings().mainModelFile());
    LOG.info("loaded.");

    // create an imported data summary file.txt (issue#23)
    PrintWriter writer = new PrintWriter(service.settings().downloadsDir() + File.separator + "datasources.txt");
    String date = new SimpleDateFormat("d MMM yyyy").format(Calendar.getInstance().getTime());
    writer.println(StringUtils.join(Arrays.asList(
        "#CPATH2:", service.settings().getName(), "version", service.settings().getVersion(), date), " "));
    writer.println("#Columns:\t" + StringUtils.join(Arrays.asList(
        "ID", "DESCRIPTION", "TYPE", "HOMEPAGE", "PATHWAYS", "INTERACTIONS", "PARTICIPANTS"), "\t"));
    Iterable<Metadata> allMetadata = service.metadata().findAll();
    for (Metadata m : allMetadata) {
      writer.println(StringUtils.join(Arrays.asList(
          CPathUtils.getMetadataUri(model, m), m.getDescription(), m.getType(), m.getUrlToHomepage(),
          m.getNumPathways(), m.getNumInteractions(), m.getNumPhysicalEntities()), "\t"));
    }
    writer.flush();
    writer.close();
    LOG.info("generated datasources.txt");

    //export the list of unique UniProt primary accession numbers
    LOG.info("creating the list of primary uniprot IDs...");
    Set<String> acs = new TreeSet<>();
    //exclude publication xrefs
    Set<Xref> xrefs = new HashSet<>(model.getObjects(UnificationXref.class));
    xrefs.addAll(model.getObjects(RelationshipXref.class));
    long left = xrefs.size();
    for (Xref x : xrefs) {
      String id = x.getId();
      if (CPathUtils.startsWithAnyIgnoreCase(x.getDb(), "uniprot")
          && id != null && !acs.contains(id))
        acs.addAll(service.map(id, "UNIPROT"));
      if (--left % 10000 == 0)
        LOG.info(left + " xrefs to map...");
    }
    writer = new PrintWriter(service.settings().downloadsDir() + File.separator + "uniprot.txt");
    writer.println(String.format("#PathwayCommons v%s - primary UniProt accession numbers:",
        service.settings().getVersion()));
    for (String ac : acs)
      writer.println(ac);
    writer.close();
    LOG.info("generated uniprot.txt");

    LOG.info("init the full-text search engine...");
    final Searcher searcher = new SearchEngine(model, service.settings().indexDir());
    // generate/find special BioPAX archives:
    // by-organism (if many),
    createBySpeciesBiopax(model, searcher);
    // and - detailed pathway data (exclude all PSI-MI sources):
    createDetailedBiopax(model, searcher, allMetadata);

    //auto-generate export.sh script (to run Paxtools commands for exporting BioPAX to other formats)
    LOG.info("writing 'export.sh' script to convert the BioPAX models to SIF, GSEA, SBGN...");
    final String commonPrefix = service.settings().exportArchivePrefix(); //e.g., PathwayCommons8
    writer = new PrintWriter(service.settings().exportScriptFile());
    writer.println("#!/bin/sh");
    writer.println("# An auto-generated script for converting the BioPAX data archives");
    writer.println("# in the downloads directory to other formats.");
    writer.println("# There must be blacklist.txt and paxtools.jar files already.");
    writer.println("# Change to the downloads/ and run as:");
    writer.println("# sh export.sh &");

    for (Metadata md : allMetadata) {
      if (!md.isNotPathwayData()) //skip warehouse metadata
        writeScriptCommands(service.settings().biopaxFileName(md.getIdentifier()), writer, md.getNumPathways() > 0);
    }
    //write commands to the script file for 'All'(main) and 'Detailed' BioPAX input files:
    writeScriptCommands(service.settings().biopaxFileName("Detailed"), writer, true);
    writeScriptCommands(service.settings().biopaxFileName("All"), writer, true);
    //rename properly those SIF files that were cut from corresponding extended SIF (.txt) ones
    writer.println("rename 's/txt\\.sif/sif/' *.txt.sif");
    writer.println(String.format("gzip %s.*.txt %s.*.sif %s.*.gmt %s.*.xml",
        commonPrefix, commonPrefix, commonPrefix, commonPrefix));

    //generate pathways.txt (parent-child) and physical_entities.json (URI-to-IDs mapping) files
    writer.println(String.format("%s %s '%s' '%s' %s 2>&1 &", javaRunPaxtools, "summarize",
        service.settings().biopaxFileName("All"), "pathways.txt", "--pathways"));
    writer.println("wait");
    writer.println(String.format("%s %s '%s' '%s' %s 2>&1 &", javaRunPaxtools, "summarize",
        service.settings().biopaxFileName("All"), "physical_entities.json", "--uri-ids"));
    writer.println("wait");
    writer.println("gzip pathways.txt *.json");
    writer.println("echo \"All done.\"");
    writer.close();

    LOG.info("createDownloads: done.");
  }

  private void writeScriptCommands(String bpFilename, PrintWriter writer, boolean exportToGSEA) {
    //make output file name prefix that includes datasource and ends with '.':
    final String prefix = bpFilename.substring(0, bpFilename.indexOf("BIOPAX."));
    final String commaSepTaxonomyIds = StringUtils.join(service.settings().getOrganismTaxonomyIds(), ',');

    if (exportToGSEA) {
      writer.println(String.format("%s %s '%s' '%s' %s 2>&1 &", javaRunPaxtools, "toGSEA", bpFilename,
          prefix + "hgnc.gmt", "'hgnc symbol' 'organisms=" + commaSepTaxonomyIds + "'"));//'hgnc symbol' - important
      writer.println(String.format("%s %s '%s' '%s' %s 2>&1 &", javaRunPaxtools, "toGSEA", bpFilename,
          prefix + "uniprot.gmt", "'uniprot' 'organisms=" + commaSepTaxonomyIds + "'"));
      writer.println("wait"); //important
      writer.println("echo \"Done converting " + bpFilename + " to GSEA.\"");
    }

    writer.println(String.format("%s %s '%s' '%s' %s 2>&1 &", javaRunPaxtools, "toSIF", bpFilename,
        prefix + "hgnc.txt", "seqDb=hgnc -extended -andSif exclude=neighbor_of"));//'hgnc symbol' or 'hgnc' does not matter

    //TODO: UniProt based extended SIF can be huge and takes too long to generate... won't make it now

    writer.println("wait"); //important
    writer.println("echo \"Done converting " + bpFilename + " to SIF.\"");
  }

  private Collection<String> findAllUris(Searcher searcher,
                                         Class<? extends BioPAXElement> type, String[] ds, String[] org) {
    Collection<String> uris = new ArrayList<>();

    SearchResponse resp = searcher.search("*", 0, type, ds, org);
    int page = 0;
    while (!resp.isEmpty()) {
      for (SearchHit h : resp.getSearchHit())
        uris.add(h.getUri());
      //next page
      resp = searcher.search("*", ++page, type, ds, org);
    }

    LOG.info("findAllUris(in " + type.getSimpleName() + ", ds: " + Arrays.toString(ds) + ", org: " + Arrays.toString(org) + ") "
        + "collected " + uris.size());

    return uris;
  }

  private void createDetailedBiopax(final Model mainModel, Searcher searcher, Iterable<Metadata> allMetadata) {
    //collect BioPAX pathway data source names
    final Set<String> pathwayDataSources = new HashSet<>();
    for (Metadata md : allMetadata) {
      if (md.getType() == METADATA_TYPE.BIOPAX || md.getType() == METADATA_TYPE.SBML)
        pathwayDataSources.add(md.standardName());
    }
    final String archiveName = service.settings().biopaxFileNameFull("Detailed");
    exportBiopax(mainModel, searcher, archiveName, pathwayDataSources.toArray(new String[]{}), null);
  }

  private void createBySpeciesBiopax(final Model mainModel, Searcher searcher) {
    // export by organism (name)
    Set<String> organisms = service.settings().getOrganismTaxonomyIds();
    if (organisms.size() > 1) {
      LOG.info("splitting the main BioPAX model by organism, into " + organisms.size() + " BioPAX files...");
      for (String organism : organisms) {
        String archiveName = service.settings().biopaxFileNameFull(organism);
        exportBiopax(mainModel, searcher, archiveName, null, new String[]{organism});
      }
    } else {
      LOG.info("won't generate any 'by organism' archives, for only one " +
          service.settings().getOrganisms() + " is listed in the properties file");
    }
  }


  private void exportBiopax(
      final Model mainModel, final Searcher searcher,
      final String biopaxArchive, final String[] datasources,
      final String[] organisms) {
    // check file exists
    if (!(new File(biopaxArchive)).exists()) {
      LOG.info("creating new " + biopaxArchive);
      try {
        //find all entities (all child elements will be then exported too)
        Collection<String> uris = new HashSet<>();
        uris.addAll(findAllUris(searcher, Pathway.class, datasources, organisms));
        uris.addAll(findAllUris(searcher, Interaction.class, datasources, organisms));
        uris.addAll(findAllUris(searcher, Complex.class, datasources, organisms));
        // export objects found above to a new biopax archive
        if (!uris.isEmpty()) {
          OutputStream os = new GZIPOutputStream(new FileOutputStream(biopaxArchive));
          SimpleIOHandler sio = new SimpleIOHandler(BioPAXLevel.L3);
          sio.convertToOWL(mainModel, os, uris.toArray(new String[]{}));
          LOG.info("successfully created " + biopaxArchive);
        } else {
          LOG.info("no pathways/interactions found; skipping " + biopaxArchive);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      LOG.info("skipped due to file already exists: " + biopaxArchive);
    }
  }

}
