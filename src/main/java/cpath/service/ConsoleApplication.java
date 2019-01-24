package cpath.service;

import cpath.service.api.Analysis;
import cpath.service.api.CPathService;
import cpath.service.api.Searcher;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jpa.Metadata;
import cpath.service.jpa.Metadata.METADATA_TYPE;

import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * The cPath2 console application for a pathway data manager
 * to build a new cPath2 instance (metadata, BioPAX model, full-text index, downloads)
 */
@Component
@Profile({"admin"})
public class ConsoleApplication implements CommandLineRunner {
  private static final Logger LOG = LoggerFactory.getLogger(ConsoleApplication.class);
  private static final String javaRunPaxtools = "nohup $JAVA_HOME/bin/java -Xmx60g -jar paxtools.jar";

  @Autowired
  private CPathService service;

  /**
   * Validator bean is available when "premerge" profile is activated;
   * Used in {@link #premerge()}
   */
  @Autowired(required = false)
  private Validator validator;

  enum Stage {
    PREMERGE,
    MERGE,
    POSTMERGE
  }

  @Override
  public void run(String... args) throws Exception {
    if (!Charset.defaultCharset().equals(Charset.forName("UTF-8")))
      LOG.error("Default Charset, " + Charset.defaultCharset() +
        " (is NOT 'UTF-8'); problems with input data are possible...");

    Options options = new Options();
    Option o = Option.builder("b").longOpt("build")
      .desc("PREMERGE: parse metadata.json, expand input archives, clean, convert, normalize the data, create the " +
        "Warehouse model; MERGE: merge the warehouse with all the normalized files into by-provider and main models, " +
        "build the full-text index of the main BioPAX model, generate blacklist.txt; POSTMERGE: creates a couple of " +
        "summary files and a script for converting the main BioPAX model to SIF, GMT, TXT formats.")
      .hasArg().argName("from-stage").optionalArg(true).type(Stage.class).build();
    options.addOption(o);
    o = Option.builder("a").longOpt("analyze")
      .desc("use a class that implements cpath.service.api.Analysis<Model> interface to analyse the integrated " +
        "BioPAX model (the class and its dependencies are expected to be found on the classpath)")
      .hasArg().argName("class").build();
    options.addOption(o);
    o = Option.builder("e").longOpt("export")
      .desc("export the main BioPAX model or sub-model defined by additional filters (see: -F)")
      .hasArg().argName("filename").build();
    options.addOption(o);
    o = Option.builder("F").longOpt("F")
      .desc("filters for the export option, e.g., -Furis=<uri,..> -Fdatasources=<nameOrUri,..> -Ftypes=<interface,..> " +
        "(when 'uris' is defined, other options are ignored)")
      .argName("property=value").hasArgs().valueSeparator().numberOfArgs(2).build();
    options.addOption(o);
    o = Option.builder("s").longOpt("server")
      .desc("run as web (service) app").build();
    options.addOption(o);

    CommandLine cmd;
    try {
      cmd = new DefaultParser().parse(options, args);
    } catch (ParseException e) {
      new HelpFormatter().printHelp("cPath2", options);
      return;
    }

    // process command line args and do smth.
    if (cmd.hasOption("build")) {
      Stage stage;
      try {
        stage = Stage.valueOf(cmd.getOptionValue("build").toUpperCase());
      } catch (Exception e) {
        stage = Stage.PREMERGE;
      }
      switch ((stage != null) ? stage : Stage.PREMERGE) {
        case PREMERGE:
          premerge();
        case MERGE:
          new Merger(service).merge();
          index();
        case POSTMERGE:
          postmerge();
      }
    } else if (cmd.hasOption("export")) {
      String[] uris = new String[]{};
      String[] datasources = new String[]{};
      String[] types = new String[]{};

      if (cmd.hasOption("F")) {
        Properties properties = cmd.getOptionProperties("F");
        if (properties.contains("uris")) {
          uris = properties.getProperty("uris").split(",");
        }
        if (uris.length == 0) { //use filters iif no uris
          datasources = properties.getProperty("datasources", "").split(",");
          types = properties.getProperty("types", "").split(",");
        }
      }
      exportData(cmd.getOptionValue("export"), uris, datasources, types);
    } else if (cmd.hasOption("analyze")) {
      executeAnalysis(cmd.getOptionValue("analyze"), true);
    } else {
      new HelpFormatter().printHelp("cPath2", options);
    }
  }

  /**
   * Runs a class that analyses or modifies the main BioPAX model.
   *
   * @param analysisClass a class that implements {@link Analysis}
   * @param readOnly      whether this is to modify and replace the BioPAX Model or not
   */
  private void executeAnalysis(String analysisClass, boolean readOnly) {
    Analysis<Model> analysis;
    try {
      Class c = Class.forName(analysisClass);
      analysis = (Analysis<Model>) c.getDeclaredConstructor().newInstance();
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

  /*
   * Builds a new BioPAX full-text index,creates the black list or ubiquitous molecules,
   * and calculates/updates the total no. of pathways, interactions, physical entities in the main db.
   */
  private void index() throws IOException {
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
    if (blacklist != null) {
      blacklist.write(service.settings().blacklistFile());
    }

    LOG.info("index: all done.");
  }

  /*
   * Executes the premerge stage:
   * organize, clean, convert, validate, normalize pathway/interaction data,
   * and create BioPAX utility class objects warehouse and id-mapping.
   */
  private void premerge() {

    fetchMetadata();

    LOG.info("premerge: initializing DAO, validator, etc...");
    //test that officially supported organisms are specified (throws a runtime exception otherwise)
    service.settings().getOrganismTaxonomyIds();
    LOG.info("premerge: this instance is configured to integrate and query " +
      " bio data about following organisms: " + Arrays.toString(service.settings().getOrganisms()));

    System.setProperty("hibernate.hbm2ddl.auto", "update");
    System.setProperty("net.sf.ehcache.disabled", "true");

    PreMerger premerger = new PreMerger(service, validator);
    premerger.premerge();

    // create the Warehouse BioPAX model (in the downloads dir) and id-mapping db table
    if (!Files.exists(Paths.get(service.settings().warehouseModelFile())))
      premerger.buildWarehouse();

    //back to read-only schema mode (useful when called from the web Main page)
    System.setProperty("hibernate.hbm2ddl.auto", "validate");
  }

  /*
   * Loads data providers' metadata.
   */
  private void fetchMetadata() {
    System.setProperty("hibernate.hbm2ddl.auto", "update");
    // grab the data
    // load the test metadata and create warehouse
    for (Metadata mdata : CPathUtils.readMetadata(service.settings().getMetadataLocation()))
      service.metadata().save(mdata);
    //back to read-only schema mode (useful when called from the web admin app)
    System.setProperty("hibernate.hbm2ddl.auto", "validate");
  }


  /**
   * Exports a cpath2 BioPAX sub-model or full model to the specified file.
   *
   * @param output      - output BioPAX file name (path)
   * @param uris        - optional, the list of valid (existing) URIs to extract a sub-model
   * @param datasources filter by data source if 'uris' is not empty
   * @param types       filter by biopax type if 'uris' is not empty
   * @throws IOException, IllegalStateException (in maintenance mode)
   */
  private void exportData(final String output, String[] uris, String[] datasources, String[] types) throws IOException {
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
    sio.absoluteUris(true); // write full URIs
    sio.convertToOWL(model, os, uris);
    IOUtils.closeQuietly(os);//though, convertToOWL must have done this already
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

  private void postmerge() throws IOException {
    LOG.info("postmerge(), started...");

    //load the main model
    LOG.info("loading the Main BioPAX Model...");
    Model model = CPathUtils.importFromTheArchive(service.settings().mainModelFile());
    LOG.info("loaded.");

    // create an imported data summary file.txt (issue#23)
    PrintWriter writer = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(
      Paths.get(service.settings().downloadsDir(), "datasources.txt")), StandardCharsets.UTF_8)
    );
    String date = new SimpleDateFormat("d MMM yyyy").format(Calendar.getInstance().getTime());
    writer.println(String.join(" ", Arrays
      .asList("#CPATH2:", service.settings().getName(), "version", service.settings().getVersion(), date)));
    writer.println("#Columns:\t" + String.join("\t", Arrays.asList(
      "ID", "DESCRIPTION", "TYPE", "HOMEPAGE", "PATHWAYS", "INTERACTIONS", "PARTICIPANTS")));
    Iterable<Metadata> allMetadata = service.metadata().findAll();
    for (Metadata m : allMetadata) {
      //we use StringUtils.join instead String.join as there are only only char sequence objects
      writer.println(StringUtils.join(Arrays.asList(
        CPathUtils.getMetadataUri(model, m), m.getDescription(), m.getType(), m.getUrlToHomepage(),
        m.getNumPathways(), m.getNumInteractions(), m.getNumPhysicalEntities()), "\t")
      );
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
    writer = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(
      Paths.get(service.settings().downloadsDir(), "uniprot.txt")), StandardCharsets.UTF_8)
    );
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
    writer = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(
      Paths.get(service.settings().exportScriptFile())), StandardCharsets.UTF_8));
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

    LOG.info("postmerge: done.");
  }

  private void writeScriptCommands(String bpFilename, PrintWriter writer, boolean exportToGSEA) {
    //make output file name prefix that includes datasource and ends with '.':
    final String prefix = bpFilename.substring(0, bpFilename.indexOf("BIOPAX."));
    final String commaSepTaxonomyIds = String.join(",", service.settings().getOrganismTaxonomyIds());

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
        ArrayUtils.toString(service.settings().getOrganisms()) + " is listed in the properties file");
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
