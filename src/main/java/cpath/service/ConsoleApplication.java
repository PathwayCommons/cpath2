package cpath.service;

import cpath.service.api.Analysis;
import cpath.service.metadata.Index;
import cpath.service.api.Service;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.service.metadata.Datasource;
import cpath.service.metadata.Datasource.METADATA_TYPE;

import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
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
  private static final String javaRunPaxtools = "nohup $JAVA_HOME/bin/java -Xmx64g " +
      "-Dpaxtools.normalizer.use-latest-registry=true -Dpaxtools.core.use-latest-genenames=true -jar paxtools.jar";

  @Autowired
  private Service service;

  /**
   * Validator bean is available when "premerge" profile is activated;
   * Used in {@link #premerge()}
   */
  @Autowired(required = false)
  private Validator validator;

  enum Stage {
    PREMERGE,
    MERGE,
    POSTMERGE;

    static Stage toType(String stage) {
      return Arrays.stream(Stage.values()).filter(s -> s.name().equalsIgnoreCase(stage)).findFirst().orElse(PREMERGE);
    }
  }

  @Override
  public void run(String... args) throws Exception {
    if (!Charset.defaultCharset().equals(Charset.forName("UTF-8"))) {
      LOG.error("Default Charset " + Charset.defaultCharset() + " is NOT 'UTF-8'");
    }
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

    // process command line args
    CommandLine cmd;
    try {
      cmd = new DefaultParser().parse(options, args);
    } catch (ParseException e) {
      new HelpFormatter().printHelp("cPath2", options);
      return;
    }

    if (cmd.hasOption("build")) {
      //Perform the data build from given stage (or from "premerge" when no value provided) to the end.
      String optVal = cmd.getOptionValue("build");
      Stage stage = Stage.toType(optVal);
      switch ((stage != null) ? stage : Stage.PREMERGE) {
        case PREMERGE:
          premerge(); //and continue to "merge"
        case MERGE:
          merge(); //and continue to "postmerge"
        case POSTMERGE:
          postmerge(); //the final stage
      }
    }
    else if (cmd.hasOption("export")) {
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
    }
    else if (cmd.hasOption("analyze")) {
      executeAnalysis(cmd.getOptionValue("analyze"), true);
    }
    else {
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
   * Executes the premerge stage:
   * organize, clean, convert, validate, normalize pathway/interaction data,
   * and create BioPAX utility class objects warehouse and id-mapping.
   */
  private void premerge() {
    LOG.info("premerge: initializing DAO, validator, etc...");
    //check that organisms are specified; throw an exception otherwise
    service.settings().getOrganismTaxonomyIds();
    LOG.info("premerge: this instance is configured to integrate and query " +
      " bio data about following organisms: " + Arrays.toString(service.settings().getOrganisms()));
//    System.setProperty("net.sf.ehcache.disabled", "true"); //(there is no JPA/Hibernate/H2 anymore)
    PreMerger premerger = new PreMerger(service, validator);
    premerger.premerge();
    // create the Warehouse BioPAX model and id-mapping db table
    if (!Files.exists(Paths.get(service.settings().warehouseModelFile()))) {
      premerger.buildWarehouse();
    }
  }

  private void merge() {
    Merger biopaxMerger = new Merger(service);
    biopaxMerger.merge();

    LOG.info("Indexing BioPAX Model (this may take an hour or so)...");
    service.index().save(service.getModel());

    LOG.info("Generating blacklist.txt...");
    //Generates, if not exist, the blacklist.txt -
    //to exclude/keep ubiquitous small molecules (e.g. ATP)
    //from graph query and output format converter results.
    BlacklistGenerator3 gen = new BlacklistGenerator3();
    Blacklist blacklist = gen.generateBlacklist(service.getModel());
    // Write all the blacklisted ids to the output
    if (blacklist != null) {
      blacklist.write(service.settings().blacklistFile());
    }
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
      Index index = new IndexImpl(model, service.settings().indexDir(), false);
      Collection<String> selectedUris = new HashSet<>();
      if (types.length > 0) {
        //collect biopax object URIs of the specified types and subtypes, and data sources if specified
        //(child biopax elements will be auto-included during the export to OWL)
        for (String bpInterfaceName : types) {
          selectedUris.addAll(findAllUris(index,
            biopaxTypeFromSimpleName(bpInterfaceName), datasources, null));
        }
      } else {
        //collect all Entity URIs filtered by the not empty data sources list
        //(child Gene, PhysicalEntity, UtilityClass biopax elements will be auto-included
        // during the export to OWL; we do not want to export dangling Genes, PEs, etc., except for Complexes...)
        selectedUris.addAll(findAllUris(index, Pathway.class, datasources, null));
        selectedUris.addAll(findAllUris(index, Interaction.class, datasources, null));
        selectedUris.addAll(findAllUris(index, Complex.class, datasources, null));
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
    // 'type' (a BioPAX L3 interface class name) is case-insensitive
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
    LOG.info("postmerge: started");
    // Updates counts of pathways, etc. and saves in the Metadata table.
    // This depends on the full-text index created already
    LOG.info("updating pathway/interaction/participant counts per data source...");
    // update counts for each non-warehouse metadata entry
    for (Datasource ds : service.metadata().getDatasources()) {
      ds.getFiles().clear(); //do not export to json
      if(ds.getType().isNotPathwayData()) {
        continue;
      }
      Model model = service.loadBiopaxModelByDatasource(ds);
      ds.setNumPathways(model.getObjects(Pathway.class).size());
      ds.setNumInteractions(model.getObjects(Interaction.class).size());
      ds.setNumPhysicalEntities(model.getObjects(PhysicalEntity.class).size() + model.getObjects(Gene.class).size());
    }
    CPathUtils.saveMetadata(service.metadata(), service.settings().getMetadataLocation()); //update the json file

    //init the service - load main model and index
    service.init();
    final Model mainModel = service.getModel();
    LOG.info("loaded main model:{} biopax elements", mainModel.getObjects().size());

    // create an imported data summary file.txt (issue#23)
    PrintWriter writer = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(
      Paths.get(service.settings().downloadsDir(), "datasources.txt")), StandardCharsets.UTF_8)
    );
    String date = new SimpleDateFormat("d MMM yyyy").format(Calendar.getInstance().getTime());
    writer.println(String.join(" ", Arrays
      .asList("#CPATH2:", service.settings().getName(), "version", service.settings().getVersion(), date)));
    writer.println("#Columns:\t" + String.join("\t", Arrays.asList(
      "ID", "DESCRIPTION", "TYPE", "HOMEPAGE", "PATHWAYS", "INTERACTIONS", "PARTICIPANTS")));
    for (Datasource d : service.metadata().getDatasources()) {
      String record = StringUtils.join(Arrays.asList(
          service.settings().getXmlBase()+d.getIdentifier(), d.getDescription(), d.getType(), d.getHomepageUrl(),
          d.getNumPathways(), d.getNumInteractions(), d.getNumPhysicalEntities()), "\t");
      writer.println(record);
      LOG.info(record);
    }
    writer.flush();
    writer.close();
    LOG.info("done datasources.txt");

    LOG.info("creating the list of primary uniprot ACs...");
    Set<String> acs = new TreeSet<>();
    //exclude publication xrefs
    Set<Xref> xrefs = new HashSet<>(mainModel.getObjects(UnificationXref.class));
    xrefs.addAll(mainModel.getObjects(RelationshipXref.class));
    for (Xref x : xrefs) {
      String id = x.getId();
      if (CPathUtils.startsWithAnyIgnoreCase(x.getDb(), "uniprot")
        && id != null && !acs.contains(id)) {
        acs.addAll(service.map(List.of(id), "UNIPROT"));
      }
    }
    writer = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(
      Paths.get(service.settings().downloadsDir(), "uniprot.txt")), StandardCharsets.UTF_8)
    );
    writer.println(String.format("#PathwayCommons v%s - primary UniProt accession numbers:",
      service.settings().getVersion()));
    for (String ac : acs) {
      writer.println(ac);
    }
    writer.close();
    LOG.info("generated uniprot.txt");

    LOG.info("init the full-text search engine...");
    final Index index = new IndexImpl(mainModel, service.settings().indexDir(), false);
    // generate the "Detailed" pathway data file:
    createDetailedBiopax(mainModel, index);

    // generate the export.sh script (to run Paxtools commands for exporting the BioPAX files to other formats)
    LOG.info("writing 'export.sh' script to convert the BioPAX models to SIF, GSEA, SBGN...");
    final String commonPrefix = service.settings().exportArchivePrefix(); //e.g., PathwayCommons13
    writer = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(
      Paths.get(service.settings().exportScriptFile())), StandardCharsets.UTF_8));
    writer.println("#!/bin/sh");
    writer.println("# An auto-generated script for converting the BioPAX data archives");
    writer.println("# in the downloads directory to other formats.");
    writer.println("# There must be blacklist.txt and paxtools.jar files already.");
    writer.println("# Change to the downloads/ and run as:");
    writer.println("# sh export.sh &");

    //write commands to the script file for 'All' and 'Detailed' BioPAX input files:
    writeScriptCommands(service.settings().biopaxFileName("Detailed"), writer, true);
    writeScriptCommands(service.settings().biopaxFileName("All"), writer, true);

    //rename SIF files that were cut from corresponding extended SIF (.txt) ones
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
        prefix + "hgnc.gmt", "'hgnc.symbol' 'organisms=" + commaSepTaxonomyIds + "'"));//'hgnc symbol' - important
      writer.println(String.format("%s %s '%s' '%s' %s 2>&1 &", javaRunPaxtools, "toGSEA", bpFilename,
        prefix + "uniprot.gmt", "'uniprot' 'organisms=" + commaSepTaxonomyIds + "'"));
      writer.println("wait"); //important
      writer.println("echo \"Done converting " + bpFilename + " to GSEA.\"");
    }
    writer.println(String.format("%s %s '%s' '%s' %s 2>&1 &", javaRunPaxtools, "toSIF", bpFilename,
      prefix + "hgnc.txt", "seqDb=hgnc -extended -andSif exclude=neighbor_of"));
    //UniProt ID based extended SIF files can be huge, take too long to generate; skip for now.
    writer.println("wait"); //important
    writer.println("echo \"Done converting " + bpFilename + " to SIF.\"");
  }

  private Collection<String> findAllUris(Index index, Class<? extends BioPAXElement> type, String[] ds, String[] org) {
    Collection<String> uris = new ArrayList<>();
    SearchResponse resp = index.search("*", 0, type, ds, org);
    int page = 0;
    while (!resp.isEmpty()) {
      for (SearchHit h : resp.getSearchHit())
        uris.add(h.getUri());
      //next page
      resp = index.search("*", ++page, type, ds, org);
    }
    LOG.info("findAllUris(in " + type.getSimpleName() + ", ds: " + Arrays.toString(ds) + ", org: " + Arrays.toString(org) + ") "
      + "collected " + uris.size());
    return uris;
  }

  private void createDetailedBiopax(final Model mainModel, Index index) {
    //collect BioPAX pathway data source names
    final Set<String> pathwayDataSources = new HashSet<>();
    for (Datasource md : service.metadata().getDatasources()) {
      if (md.getType() == METADATA_TYPE.BIOPAX) {
        pathwayDataSources.add(md.standardName());
      }
    }
    final String archiveName = service.settings().biopaxFileNameFull("Detailed");
    exportBiopax(mainModel, index, archiveName, pathwayDataSources.toArray(new String[]{}), null);
  }

  private void exportBiopax(Model mainModel, Index index, String biopaxArchive,
                            String[] datasources, String[] organisms) {
    // check file exists
    if (!(new File(biopaxArchive)).exists()) {
      LOG.info("creating new " + biopaxArchive);
      try {
        //find all entities (all child elements will be then exported too)
        Collection<String> uris = new HashSet<>();
        uris.addAll(findAllUris(index, Pathway.class, datasources, organisms));
        uris.addAll(findAllUris(index, Interaction.class, datasources, organisms));
        uris.addAll(findAllUris(index, Complex.class, datasources, organisms));
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
