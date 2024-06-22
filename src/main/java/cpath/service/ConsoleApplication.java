package cpath.service;

import cpath.service.api.Analysis;
import cpath.service.metadata.Index;
import cpath.service.api.Service;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.service.metadata.Datasource;

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
      .desc("use a class that implements cpath.service.api.Analysis<Model> interface to analyse the " +
        "BioPAX model (the class and its dependencies are expected to be on the classpath)")
      .hasArg().argName("class").build();
    options.addOption(o);
    o = Option.builder("m").longOpt("modify")
        .desc("use a class that implements cpath.service.api.Analysis<Model> interface to modify the " +
            "BioPAX model and re-index (the class and its dependencies are expected to be on the classpath)")
        .hasArg().argName("class").build();
    options.addOption(o);
    o = Option.builder("i").longOpt("index")
        .desc("re-index the BioPAX model").build();
    options.addOption(o);
    o = Option.builder("e").longOpt("export")
      .desc("export the main BioPAX model or sub-model defined by additional filters (see: -F)")
      .hasArg().argName("filename").build();
    options.addOption(o);
    o = Option.builder("F").longOpt("F")
      .desc("filters for the export option, e.g., -Furis=<uri,..> -Fdatasources=<name,..> -Ftypes=<interface,..> " +
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
      analyzeModel(cmd.getOptionValue("analyze"));
    }
    else if (cmd.hasOption("modify")) {
      modifyModel(cmd.getOptionValue("modify"));
    }
    else if (cmd.hasOption("index")) {
      reindex();
    }
    else {
      new HelpFormatter().printHelp("cPath2", options);
    }
  }

  /*
   * Runs a class that analyses the main BioPAX model.
   *
   * @param analysisClass a class that implements {@link Analysis}
   */
  private void analyzeModel(String analysisClass) {
    Analysis<Model> analysis;
    try {
      Class c = Class.forName(analysisClass);
      analysis = (Analysis<Model>) c.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    Model model = CPathUtils.importFromTheArchive(service.settings().mainModelFile());
    analysis.execute(model);
  }

  /*
   * Runs a class that analyses and modifies the main BioPAX model and index.
   *
   * @param analysisClass a class that implements {@link Analysis} and can edit the data.
   */
  private void modifyModel(String analysisClass) throws IOException {
    Analysis<Model> analysis;
    try {
      Class c = Class.forName(analysisClass);
      analysis = (Analysis<Model>) c.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    //load current model from the file
    Model model = CPathUtils.importFromTheArchive(service.settings().mainModelFile());
    // and apply the changes
    LOG.info("Running class: {}...", analysisClass);
    analysis.execute(model);
    // export the modified model to the file
    LOG.info("Over-writing model: {}...", service.settings().mainModelFile());
    new SimpleIOHandler(BioPAXLevel.L3).convertToOWL(model,
        new GZIPOutputStream(new FileOutputStream(service.settings().mainModelFile())));
    //init the lucene index as read-write
    service.initIndex(model, service.settings().indexDir(), false);
    //re-index the model
    service.index().save(model);
  }

  private void reindex() throws IOException {
    Model model = CPathUtils.importFromTheArchive(service.settings().mainModelFile());
    service.initIndex(model, service.settings().indexDir(), false);
    service.index().save(model);
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
    PreMerger premerger = new PreMerger(service, validator);
    premerger.premerge();
    // create the Warehouse BioPAX model and id-mapping db table
    if (!Files.exists(Paths.get(service.settings().warehouseModelFile()))) {
      premerger.buildWarehouse(); //also makes/saves id-mapping docs in the lucene index via service.mapping() DAO
    }
  }

  private void merge() {
    if (!Files.exists(Paths.get(service.settings().mainModelFile()))) {
      Merger biopaxMerger = new Merger(service);
      //each normalized datasource model is further improved with Warehouse and id-mapping and merged into the main...
      biopaxMerger.merge(); //also saves it
    } else {
      LOG.info("Found {}...", service.settings().mainModelFile());
    }

    if(service.getBlacklist() == null) {
      if(service.getModel()==null) {
        service.init();
      }
      LOG.info("Generating the list of ubiquitous small molecules, {}...", service.settings().blacklistFile());
      //Generate the blacklist.txt to exclude/keep ubiquitous small molecules (e.g. ATP)
      //from graph query and output format converter results.
      BlacklistGenerator3 gen = new BlacklistGenerator3();
      Blacklist blacklist = gen.generateBlacklist(service.getModel());
      // Write all the blacklisted ids to the output
      if (blacklist != null) {
        service.setBlacklist(blacklist);
        blacklist.write(service.settings().blacklistFile());
      }
    } else { //means - service.init() loaded it earlier
      LOG.info("Found: {} - ok", service.settings().blacklistFile());
    }
  }

  /**
   * Exports a cpath2 BioPAX sub-model or full model to the specified file.
   *
   * @param output      - output BioPAX file name (path)
   * @param uris        - optional, the list of valid (existing) URIs to extract a sub-model
   * @param datasources filter by datasource (name or identifier) if 'uris' is not empty
   * @param types       filter by BioPAX type if 'uris' is not empty
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
    IOUtils.closeQuietly(os, null);//though, convertToOWL must have done this already
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

    // Update the counts of pathways, interactions, participants per data source and save.
    LOG.info("updating pathway/interaction/participant counts per data source...");
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

    // Generate datasources.txt summary file (issue#23)
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
    LOG.info("generated datasources.txt");

    if(service.getModel() == null) {
      service.init(); // load/reload the main model, index, etc.
    }

    //this was to integrate with UniProt portal/data - to add/update their external links to PathwayCommons apps...
    LOG.info("creating the list of primary uniprot ACs...");
    Set<String> acs = new TreeSet<>();
    service.getModel().getObjects(Xref.class)
        .stream()
        .filter(x -> !(x instanceof PublicationXref)) //except for publication xrefs
        .forEach(x -> {
          String id = x.getId();
          if (CPathUtils.startsWithAnyIgnoreCase(x.getDb(), "uniprot")
              && id != null && !acs.contains(id)) {
            acs.addAll(service.map(List.of(id), "UNIPROT"));
          }
        });
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

    LOG.info("postmerge: done.");
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

}
