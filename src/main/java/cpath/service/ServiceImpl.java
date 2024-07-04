package cpath.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.biopax.paxtools.controller.*;
import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.level3.Process;
import org.biopax.paxtools.normalizer.Resolver;
import org.biopax.paxtools.pattern.util.Blacklist;
import org.biopax.paxtools.query.QueryExecuter;
import org.biopax.paxtools.query.algorithm.Direction;
import org.biopax.paxtools.query.algorithm.LimitType;
import org.biopax.paxtools.query.wrapperL3.DataSourceFilter;
import org.biopax.paxtools.query.wrapperL3.Filter;
import org.biopax.paxtools.query.wrapperL3.OrganismFilter;
import org.biopax.paxtools.query.wrapperL3.UbiqueFilter;
import org.biopax.paxtools.util.IllegalBioPAXArgumentException;
import org.biopax.validator.api.ValidatorUtils;
import org.biopax.validator.api.beans.Validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import cpath.analysis.TraverseAnalysis;
import cpath.service.api.*;
import cpath.service.metadata.*;
import cpath.service.jaxb.*;

import static cpath.service.api.Status.*;

/**
 * Service tier class - to uniformly access 
 * persisted BioPAX model and metadata from console 
 * and web services.
 *
 * @author rodche
 */

@org.springframework.stereotype.Service
public class ServiceImpl implements Service {
  private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);
  private static final Class<? extends BioPAXElement>[] DEFAULT_SEED_TYPES = new Class[]{PhysicalEntity.class, Gene.class};

  IndexImpl index;

  Metadata metadata;

  @Autowired
  private Settings settings;

  private SimpleIOHandler simpleIO;

  //init on first access to getBlacklist(); so do not use it directly
  private Blacklist blacklist;

  //on first access when proxy model mode is enabled (so do not use the var. directly!)
  private Model paxtoolsModel;

  private final Pattern isoformIdPattern = Pattern.compile(Resolver.getNamespace("uniprot.isoform", true).getPattern());
  private final Pattern refseqIdPattern = Pattern.compile(Resolver.getNamespace("refseq", true).getPattern());

  public ServiceImpl() {
    this.simpleIO = new SimpleIOHandler(BioPAXLevel.L3);
    this.simpleIO.mergeDuplicates(true);
  }

  /**
   * Loads the main BioPAX model, full-text index, blacklist.
   * Call this after the web service is up and running or from the console app.
   */
  synchronized public void init() {
    if(paxtoolsModel == null) {
      paxtoolsModel = loadMainModel();
      if (paxtoolsModel != null) {
        paxtoolsModel.setXmlBase(settings().getXmlBase());
        log.info("Main BioPAX model (in-memory) is now ready for queries.");
      }
    }
    initIndex(paxtoolsModel, settings.indexDir(), true); //read-only (search) index
    index.setMaxHitsPerPage(settings.getMaxHitsPerPage());
    if(blacklist == null) {
      loadBlacklist();
    }
  }

  /**
   * Init or re-open the index.
   *
   * @param model
   * @param indexLocation
   * @param readOnly
   */
  @Override
  public void initIndex(Model model, String indexLocation, boolean readOnly) {
    if(index != null) {
      index.close();
    }
    index = new IndexImpl(model, indexLocation, readOnly);
  }

  public Settings settings() {return settings;}

  public void setSettings(Settings settings) {
    this.settings = settings;
  }

  public Model getModel() {
    return paxtoolsModel;
  }
  public void setModel(Model paxtoolsModel) {
    this.paxtoolsModel = paxtoolsModel;
    if(index != null) {
      index.setModel(paxtoolsModel);
    }
  }

  public Blacklist getBlacklist() {
    return blacklist;
  }

  public void setBlacklist(Blacklist blacklist) {
    this.blacklist = blacklist;
  }

  public ServiceResponse search(String queryStr,
                                int page, Class<? extends BioPAXElement> biopaxClass,
                                String[] dsources, String[] organisms) {
    if(modelNotReady() || index == null) {
      return new ErrorResponse(MAINTENANCE, "Waiting for the initialization to complete (try later)...");
    }
    try {
      // do search
      SearchResponse hits = index.search(queryStr, page, biopaxClass, dsources, organisms);
      hits.setComment("Search '" + queryStr  + "' in " +
        ((biopaxClass == null) ? "all types" : biopaxClass.getSimpleName())
        + "; ds: " + Arrays.toString(dsources)+ "; org.: " + Arrays.toString(organisms));
      return hits;
    } catch (Exception e) {
      log.error("search() failed - " + e);
      return new ErrorResponse(INTERNAL_ERROR, e);
    }
  }

  public ServiceResponse fetch(final OutputFormat format, Map<String, String> formatOptions,
                               boolean subPathways, final String... uris)
  {
    if(modelNotReady())
      return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try later)...");

    // extract/convert a sub-model
    try {
      Set<BioPAXElement> elements = seedBiopaxElements(uris);
      Model m = autoCompleteAndClone(elements, subPathways);
      //name the sub-model - can be useful when converted to GSEA, etc...
      if(m!= null && !m.getObjects().isEmpty()) {
        if(elements.size()==1) {
          String uri = elements.iterator().next().getUri();
          m.setUri(uri);
          BioPAXElement b = m.getByID(uri);
          if(b instanceof Named) {
            m.setName(((Named) b).getDisplayName() + " " + Arrays.toString(uris));
          } else {
            m.setName(Arrays.toString(uris));
          }
        } else {
          String desc = Arrays.toString(uris);
          m.setUri("PC_get_" + desc.hashCode());
          m.setName(desc);
        }
      }
      return convert(m, format, formatOptions);
    } catch (Exception e) {
      return new ErrorResponse(INTERNAL_ERROR, e);
    }
  }


  private Filter[] createFilters(String[] organisms, String[] datasources) {
    List<Filter> filters = new ArrayList<>();

    if(blacklist != null)
      filters.add(new UbiqueFilter(blacklist.getListed()));
    else
      log.warn("createFilters: blacklist is NULL, why..."); //normally, it's not null here

    if(organisms != null && organisms.length > 0)
      filters.add(new OrganismFilter(organisms));

    if(datasources != null && datasources.length > 0)
      filters.add(new DataSourceFilter(datasources));

    return filters.toArray(new Filter[]{});
  }


  // auto-complete and clone - makes a reasonable size detached (copy) sub-model
  private Model autoCompleteAndClone(final Set<BioPAXElement> elements, final boolean includeSubPathways)
  {
    if(elements == null || elements.isEmpty())
      return null;

    Completer completer = new Completer(simpleIO.getEditorMap());
    Cloner cloner = new Cloner(this.simpleIO.getEditorMap(), this.simpleIO.getFactory());

    completer.setSkipSubPathways(!includeSubPathways); //mind NOT (!) here
    Model m = cloner.clone(completer.complete(elements));
    if(m != null) {
      m.setXmlBase(settings().getXmlBase());
    }

    return m;
  }

  public ServiceResponse getNeighborhood(OutputFormat format,
                                         Map<String, String> formatOptions,
                                         String[] sources,
                                         Integer limit,
                                         Direction direction,
                                         String[] organisms,
                                         String[] datasources,
                                         boolean subPathways)
  {
    if(modelNotReady())
      return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try later)...");

    if(direction == null) {
      direction = Direction.UNDIRECTED;
    }

    // execute the paxtools graph query
    try {
      Set<Set<BioPAXElement>> elements = seedBiopaxElementGroups(sources);
      // Execute the query, get result elements
      Set<BioPAXElement> nhood = QueryExecuter.runNeighborhoodMultiSet(elements, paxtoolsModel,
        limit, direction, createFilters(organisms, datasources));
      Model m = autoCompleteAndClone(nhood, subPathways);
      if( m != null) {
        String desc = Arrays.toString(sources);
        m.setUri("PC_graph_neighborhood_" + desc.hashCode());
        m.setName(desc);
      }
      return convert(m, format, formatOptions); //m==null is ok
    } catch (Exception e) {
      return new ErrorResponse(INTERNAL_ERROR, e);
    }
  }

  public ServiceResponse getPathsBetween(OutputFormat format,
                                         Map<String, String> formatOptions,
                                         String[] sources,
                                         Integer limit,
                                         String[] organisms,
                                         String[] datasources,
                                         boolean subPathways)
  {
    if(modelNotReady())
      return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try later)...");

    // execute the paxtools graph query
    try {
      // init source elements
      Set<Set<BioPAXElement>> elements = seedBiopaxElementGroups(sources);
      // Execute the query, get result elements
      Set<BioPAXElement> result = QueryExecuter.runPathsBetweenMultiSet(elements, paxtoolsModel, limit,
        createFilters(organisms, datasources));
      Model m = autoCompleteAndClone(result,subPathways);
      if(m != null) {
        String desc = Arrays.toString(sources);
        m.setUri("PC_graph_pathsbetween_" + desc.hashCode());
        m.setName(desc);
      }
      return convert(m, format, formatOptions);
    } catch (Exception e) {
      return new ErrorResponse(INTERNAL_ERROR, e);
    }
  }

  public ServiceResponse getPathsFromTo(OutputFormat format,
                                        Map<String, String> formatOptions,
                                        String[] sources,
                                        String[] targets,
                                        LimitType limitType,
                                        Integer limit,
                                        String[] organisms,
                                        String[] datasources,
                                        boolean subPathways)
  {
    if(modelNotReady())
      return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try later)...");

    // execute the paxtools graph query
    try {
      // init source and target elements
      Set<Set<BioPAXElement>> source = seedBiopaxElementGroups(sources);
      Set<Set<BioPAXElement>> target = seedBiopaxElementGroups(targets);

      Model m = null;
      if(!source.isEmpty())
      {
        // Execute the query
        Set<BioPAXElement> elements = (target == null || target.isEmpty())
          ? QueryExecuter.runPathsBetweenMultiSet(source, paxtoolsModel, limit,
          createFilters(organisms, datasources))
          : QueryExecuter.runPathsFromToMultiSet(source, target, paxtoolsModel, limitType, limit,
          createFilters(organisms, datasources));

        m = autoCompleteAndClone(elements,subPathways);

        if(m != null) {
          String desc = Arrays.toString(sources) + "-to-" + Arrays.toString(targets);
          m.setUri("PC_graph_pathsfromto_" + desc.hashCode());
          m.setName(desc);
        }
      }

      return convert(m, format, formatOptions); //m==null is ok too
    } catch (Exception e) {
      return new ErrorResponse(INTERNAL_ERROR, e);
    }

  }

  private ServiceResponse convert(Model m, OutputFormat format, Map<String, String> options) {
    BiopaxConverter biopaxConverter = new BiopaxConverter(blacklist);

    if(options == null)
      options = new HashMap<>();

    if(format != OutputFormat.BIOPAX && m != null) {
      // remove all Pathway objects from the result model as
      // these become incomplete after detaching from main PC model
      // and look confusing after converting to other formats.
      for(Pathway p : new HashSet<>(m.getObjects(Pathway.class))) {
        m.remove(p);
      }

      if(format == OutputFormat.SBGN && !options.containsKey("layout")) {
        options.put("layout", Boolean.toString(settings().getSbgnLayoutEnabled()));
      }

      if(format == OutputFormat.GSEA && !options.containsKey("organisms")) {
        //pre-configured supported species (global settings) -
        //it's currently not about user query (filter) option for GSEA
        options.put("organisms", String.join(",", settings.getOrganismTaxonomyIds()));
      }
    }

    return biopaxConverter.convert(m, format, options);
  }

  public ServiceResponse getCommonStream(OutputFormat format,
                                         Map<String, String> formatOptions,
                                         String[] sources,
                                         Integer limit,
                                         Direction direction,
                                         String[] organisms,
                                         String[] datasources,
                                         boolean subPathways)
  {
    if(modelNotReady()) {
      return new ErrorResponse(MAINTENANCE, "Waiting for the initialization to complete (try again later)...");
    }

    if (direction == Direction.BOTHSTREAM || direction == Direction.UNDIRECTED) {
      return new ErrorResponse(BAD_REQUEST, "COMMONSTREAM graph query direction must be either UPSTREAM or DOWNSTREAM (default)");
    } else if(direction == null) {
      direction = Direction.DOWNSTREAM;
    }

    // execute the paxtools graph query
    try {
      // init source elements
      Set<Set<BioPAXElement>> elements = seedBiopaxElementGroups(sources);
      // Execute the query, get result elements
      Set<BioPAXElement> result = QueryExecuter
        .runCommonStreamWithPOIMultiSet(elements, paxtoolsModel, direction, limit,
          createFilters(organisms, datasources));
      Model m = autoCompleteAndClone(result, subPathways);
      if(m != null) {
        String desc = Arrays.toString(sources);
        //m.setXmlBase(settings().getXmlBase()); //already set in autoCompleteAndClone
        m.setUri(m.getXmlBase() + "commonstream_" + desc.hashCode());
        m.setName(desc);
      }
      return convert(m, format, null);
    } catch (Exception e) {
      return new ErrorResponse(INTERNAL_ERROR, e);
    }
  }


  /**
   * Map IDs to BioPAX entity URIs.
   *
   * This method is to replace {@link #findUrisByIds(String[], Class[])}
   * in the graph query methods; see issue #296 for more info.
   *
   * @param identifiers - a list of genes/protein or molecules as: \
   * 		HGNC symbols, UniProt, RefSeq and NCBI Gene IDs; or \
   * 		CHEBI, InChIKey, ChEMBL, DrugBank, PubChem Compound, \
   * 	    KEGG Compound, PharmGKB.
   * @return URIs of matching Xrefs
   *
   */
  private Set<Set<String>> mapToSeeds(String[] identifiers)
  {
    Set<Set<String>> sets = new HashSet<>();

    if(identifiers != null)
      for(String identifier: identifiers)
        sets.add(findUrisById(identifier));

    return sets;
  }

  /**
   * Mapping from URL/IDs to the BioPAX entity URIs.
   *
   * @param identifiers - a list of URIs or genes/protein/molecules IDs: \
   * 		HGNC symbols, UniProt, RefSeq and NCBI Gene IDs; or \
   * 		CHEBI, InChIKey, ChEMBL, DrugBank, PubChem Compound, KEGG Compound, PharmGKB.
   * @param types filter search to get back URIs of given biopax types and subtypes
   * @return URIs of matching Xrefs
   *
   * See also: issue #296
   */
  private String[] findUrisByIds(String[] identifiers, Class<? extends BioPAXElement>... types)
  {
    if(identifiers == null || identifiers.length == 0) {
      return new String[]{};
    }

    Set<String> uris = new TreeSet<>();

    StringBuilder q = new StringBuilder();
    for (String identifier : identifiers) {
      if(getModel().containsID(identifier)) {
        uris.add(identifier);
      } else {
        //Build a Lucene query (eq. to xrefid:"A" OR xrefid:"B" OR ...);
        //let's sanitize the ID by simply using double quotes around each identifier:
        if (!q.toString().contains(identifier)) {
          q.append("xrefid:\"").append(identifier).append("\" ");
        }
      }
    }

    if (q.length() > 0) {
      //find all entity URIs by IDs using a specific full-text search
      final String query = q.toString().trim();

      if(types.length==0) {
        types = DEFAULT_SEED_TYPES; //BioPAX types to search in
      }

      for(Class<? extends BioPAXElement> type : types) {
        findAllUris(uris, query, type);
      }
    }

    return uris.toArray(new String[]{});
  }

  /**
   * Mapping some ID to the BioPAX entity URI(s).
   *
   * @param idOrUri - a genes/protein or molecule identifier or uri.
   * @return URIs of matching Xrefs
   *
   * See also: issue #296
   */
  private Set<String> findUrisById(String idOrUri)
  {
    if (idOrUri == null)
      return Collections.emptySet();

    Set<String> uris = new TreeSet<>();

    if(idOrUri.startsWith(settings().getXmlBase())) {
      // must be valid URI of some existing BioPAX object in our model
      uris.add(idOrUri);
    } else {
      //Find all entity URIs by ID and specific Lucene query (eq. to xrefid:"A" OR xrefid:"B" OR ...
      //sanitize the ID by simply using double quotes around each id):
      String query = "xrefid:\""+idOrUri+"\"";
      for(Class<? extends BioPAXElement> type : DEFAULT_SEED_TYPES) {
        findAllUris(uris, query, type);
      }
    }

    return uris;
  }

  private void findAllUris(Set<String> collectedUris, String query, Class<? extends BioPAXElement> biopaxTypeFilter) {
    log.debug("findAllUris, search in " + biopaxTypeFilter.getSimpleName() + " using query: " + query);
    int page = 0; // will use search pagination; collect all hits from all result pages
    SearchResponse resp = (SearchResponse) search(query, page, biopaxTypeFilter, null, null);
    while (!resp.isEmpty())
    {
      for (SearchHit h : resp.getSearchHit())
        collectedUris.add(h.getUri());
      // go to next page
      resp = (SearchResponse) search(query, ++page, biopaxTypeFilter, null, null);
    }
  }

  public ServiceResponse traverse(String propertyPath, String... uris) {

    if(modelNotReady())
      return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try later)...");

    TraverseResponse res = new TraverseResponse();
    res.setPropertyPath(propertyPath);

    try {
      int idx = propertyPath.indexOf('/');
      if(idx <= 0){
        throw new IllegalBioPAXArgumentException("Path does not start from a BioPAX type name.");
      }
      //BioPAX type at the beginning of the path -
      Class<? extends BioPAXElement> type = BioPAXLevel.L3.getInterfaceForName(propertyPath.substring(0, idx));
      //Not only absolute URIs but also IDs (to search for biopax type objects) now work!
      String[] sourceUris =  findUrisByIds(uris, type);
      new TraverseAnalysis(res, sourceUris).execute(paxtoolsModel);
      return res;
    } catch (IllegalArgumentException e) { //- catches IllegalBioPAXArgumentException too
      log.error("traverse() failed to init path accessor. " + e);
      return new ErrorResponse(BAD_REQUEST, e.getMessage());
    } catch (Exception e) {
      log.error("traverse() failed. " + e);
      return new ErrorResponse(INTERNAL_ERROR, e);
    }

  }


  /**
   * {@inheritDoc}
   *
   * Collect "top" pathways (sort of) such as those having
   * controlledOf, pathwayComponentOf and stepProcessOf properties empty, and
   * excluding pathways with less than three components unless there is a non-trivial sub-pathway.
   */
  public ServiceResponse topPathways(String q, final String[] organisms, final String[] datasources) {

    if(modelNotReady() || index == null)
      return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try later)...");

    if(q==null || q.isEmpty()) //too much data
      return new ErrorResponse(BAD_REQUEST,"Query string was empty.");

    SearchResponse topPathways = new SearchResponse();
    final List<SearchHit> hits = topPathways.getSearchHit(); //empty list
    int page = 0; // will use search pagination

    SearchResponse r;
    try {
      r = index.search(q, page, Pathway.class, datasources, organisms);
    } catch(Exception e) {
      log.error("topPathways() failed", e);
      return new ErrorResponse(INTERNAL_ERROR, e);
    }

    //go over all hits, all pages
    final long numPathways = r.getNumHits();
    int processed = 0;
    while(!r.isEmpty()) {
      log.debug("Retrieving top pathways search results, page #" + page);
      //keep only pathways where 'pathway' index field
      //is empty (no controlledOf and pathwayComponentOf values)
      for(SearchHit h : r.getSearchHit()) {
        if( h.getPathway().isEmpty() ||
          (h.getPathway().size()==1
            && h.getPathway().get(0).equalsIgnoreCase(h.getUri())
          )
        ){
          if(h.getNumProcesses()>2) //skip e.g. CTD "pathways" that contain one-two interactions
            hits.add(h); //add to the list
          else { //add only if it has a child non-empty pathway
            Pathway hp = (Pathway) getModel().getByID(h.getUri());
            for(Process component : hp.getPathwayComponent()) {
              if(component instanceof Pathway
                && ((Pathway)component).getPathwayComponent().size()>2)
              {
                hits.add(h);
                break;
              }
            }
          }
        }
        processed++;
      }

      if(processed >= numPathways)
        break; //may save us one uselss query

      // go next page
      try {
        r = index.search(q, ++page, Pathway.class, datasources, organisms);
      } catch(Exception e) {
        log.error("topPathways() failed", e);
        return new ErrorResponse(INTERNAL_ERROR, e);
      }
    }

    topPathways.setNumHits((long)hits.size());
    topPathways.setComment("Top Pathways (neither components of other pathways nor controlled by any process)");
    topPathways.setMaxHitsPerPage(hits.size());
    topPathways.setPageNo(0);

    return topPathways;
  }

  /**
   * Prepares the seed objects for a get(fetch) or traverse query.
   *
   * @param ids an array of IDs to map
   * @return matched biopax elements
   */
  private Set<BioPAXElement> seedBiopaxElements(String[] ids) {
    return getByUri(findUrisByIds(ids));
  }

  /**
   * Prepares the sets of seed objects for a BioPAX graph query.
   *
   * @param ids an array of IDs to map
   * @return groups of seed biopax elements (- one group per input id/uri)
   */
  private Set<Set<BioPAXElement>> seedBiopaxElementGroups(String[] ids)
  {
    Set<Set<BioPAXElement>> ret = new HashSet<>();
    Set<Set<String>> uris = mapToSeeds(ids);
    for(Set<String> set : uris) {
      ret.add(getByUri(set.toArray(new String[0])));
    }

    return ret;
  }

  private Set<BioPAXElement> getByUri(String[] uris) {
    Set<BioPAXElement> elements = new HashSet<>();
    for (String uri : uris) {
      BioPAXElement e = paxtoolsModel.getByID(uri);
      if (e != null) elements.add(e);
    }

    return elements;
  }

  private synchronized void loadBlacklist()
  {
    Resource blacklistResource = new DefaultResourceLoader().getResource("file:" + settings.blacklistFile());
    if(blacklistResource.exists()) {
      try {
        this.blacklist = new Blacklist(blacklistResource.getInputStream());
        log.info("loadBlacklist, loaded: " + blacklistResource.getDescription());
        Assert.notEmpty(blacklist.getListed(),"The blacklist is empty");
      } catch (IOException e) {
        log.error("loadBlacklist, failed using: "
          + blacklistResource.getDescription(), e);
      }
    } else {
      log.warn("loadBlacklist, " + settings.blacklistFile() + " is not found");
    }
  }

  private boolean modelNotReady() {
    return paxtoolsModel == null;
  }

  /**
   * ID mapping from any ID but only to either CHEBI or UNIPROT primary id.
   * @param fromIds the source IDs
   * @param toDb only "CHEBI" or "UNIPROT" (case-insensitive)
   * @return
   */
  public Set<String> map(Collection<String> fromIds, String toDb) {
    Assert.hasText(toDb,"toDb must be not null, empty or blank");
    Assert.isTrue("CHEBI".equalsIgnoreCase(toDb) || "UNIPROT".equalsIgnoreCase(toDb),
      "toDb is not CHEBI or UNIPROT");

    if(fromIds.isEmpty()) {
      log.debug("map(), the argument 'fromIds' is an empty collection.");
      return Collections.emptySet();
    }

    List<String> sourceIds = new ArrayList<>();
    // let's guess the source db (id type) and take care of isoform ids;
    // it's risky if a no-prefix integer ID type (PubChem CID, SID) is used and no srcDb is provided;
    // for biopolymers, we support the only one integer ID type - 'NCBI Gene'
    for(String fromId : fromIds) {
      if (toDb.equalsIgnoreCase("chebi")) {
        if(fromId.matches("^\\d+$")) {
          fromId = "CHEBI:" + fromId;
        }
      }
      else if (toDb.equalsIgnoreCase("uniprot")) {
        if(isoformIdPattern.matcher(fromId).find() && fromId.contains("-")) {
          //it's certainly an uniprot isoform id; so we replace it with the corresponding accession number
          fromId = fromId.replaceFirst("-\\d+$", "");
        } else if (refseqIdPattern.matcher(fromId).find() && fromId.contains(".")) {
          //remove the version number, such as ".1"
          fromId = fromId.replaceFirst("\\.\\d+$", "");
        }
      }
      sourceIds.add(fromId); //collect
    }

    //use Mappings repository to execute the search and get results
    Set<String> results = mapping().findBySrcIdInAndDstDbIgnoreCase(sourceIds, toDb).stream()
            .map(Mapping::getDstId)
            .collect(Collectors.toCollection(TreeSet::new));

    return results;
  }

  public Mappings mapping() {
    return index;
  }

  public Metadata metadata() {
    if(metadata == null) {
      metadata = CPathUtils.readMetadata(settings().getMetadataLocation());
    }
    return metadata;
  }

  public Index index() {
    return index;
  }

  public void clear(Datasource datasource) {
    CPathUtils.cleanupDirectory(intermediateDataDir(datasource), true);
    datasource.setNumInteractions(0);
    datasource.setNumPathways(0);
    datasource.setNumPhysicalEntities(0);
    datasource.getFiles().clear();
  }

  public Model loadMainModel() {
    return CPathUtils.importFromTheArchive(settings.mainModelFile());
  }

  public Model loadWarehouseModel() {
    return CPathUtils.importFromTheArchive(settings.warehouseModelFile());
  }

  public Model loadBiopaxModelByDatasource(Datasource datasource) {
    Path in = Paths.get(settings.biopaxFileName(datasource.getIdentifier()));
    if (Files.exists(in)) {
      return CPathUtils.importFromTheArchive(in.toString());
    } else {
      log.debug("loadBiopaxModelByDatasource, file not found: " + in
        + " (not merged yet, or file was deleted)");
      return null;
    }
  }

  public String getDataArchiveName(Datasource datasource) {
    return Paths.get(settings.dataDir(), datasource.getIdentifier() + ".zip").toString();
  }


  public String intermediateDataDir(Datasource datasource) {
    return Paths.get(settings.dataDir(), datasource.getIdentifier()).toString();
  }

  public void unzipData(Datasource datasource) {
    try {
      String fname = (datasource.getDataUrl().startsWith("classpath:"))//a hack for test
        ? CPathUtils.LOADER.getResource(datasource.getDataUrl()).getFile().getPath()
          : getDataArchiveName(datasource);//production

      ZipFile zipFile = new ZipFile(fname);
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while(entries.hasMoreElements())
      {
        ZipEntry entry = entries.nextElement();
        String entryName = entry.getName();
        log.info("unzipData(), processing zip entry: " + entryName);
        //skip some sys/tmp files (that MacOSX creates sometimes)
        if(entry.isDirectory() || entryName.contains("__MACOSX") || entryName.startsWith(".")
          || entryName.contains("/.") || entryName.contains("\\."))
        {
          log.info("unzipData(), skipped " + entryName);
          continue;
        }
        //create the original data file path/name, replacing all unsafe symbols with underscores
        String datafile = CPathUtils.originalFile(intermediateDataDir(datasource), entryName);
        datasource.getFiles().add(datafile);
        // expand original contend and save to the gzip output file
        Path out = Paths.get(datafile);
        if(!Files.exists(out)) {
          CPathUtils.copy(zipFile.getInputStream(entry), new GZIPOutputStream(Files.newOutputStream(out)));
          //streams get auto-closed after copied
        }
      }
      //done all zip entries
      zipFile.close();
    } catch (IOException e) {
      throw new RuntimeException("unzipData(), failed reading from: " + datasource.getIdentifier() , e);
    }

    if(datasource.getFiles().isEmpty())
      log.warn("unzipData(), no data found for " + datasource);
  }

  public void saveValidationReport(Validation v, String reportFile) {
    try {
      Writer writer = new OutputStreamWriter(new GZIPOutputStream(
        Files.newOutputStream(Paths.get(reportFile))), StandardCharsets.UTF_8);
      ValidatorUtils.write(v, writer, null);
      writer.flush();
      writer.close();//important
    } catch (IOException e) {
      log.error("saveValidationReport: failed to save the report", e);
    }
  }

}

