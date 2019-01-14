package cpath.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import cpath.analysis.TraverseAnalysis;
import cpath.service.api.*;
import org.biopax.paxtools.controller.*;
import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.level3.Process;
import org.biopax.paxtools.normalizer.MiriamLink;
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
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cpath.service.jpa.*;
import cpath.service.jaxb.*;

import javax.annotation.PostConstruct;

import static cpath.service.api.Status.*;


/**
 * Service tier class - to uniformly access 
 * persisted BioPAX model and metadata from console 
 * and web services.
 *
 * @author rodche
 */
@Service
public class CPathServiceImpl implements CPathService {
  private static final Logger log = LoggerFactory.getLogger(CPathServiceImpl.class);
  private static final Class<? extends BioPAXElement>[] DEFAULT_SEED_TYPES = new Class[]{PhysicalEntity.class, Gene.class};

  private Searcher searcher;

  @Autowired
  MetadataRepository metadataRepository;

  @Autowired
  MappingsRepository mappingsRepository;

  @Autowired
  private Settings settings;


  private SimpleIOHandler simpleIO;

  //init on first access to getBlacklist(); so do not use it directly
  private Blacklist blacklist;

  //on first access when proxy model mode is enabled (so do not use the var. directly!)
  private Model paxtoolsModel;

  private final Pattern isoformIdPattern = Pattern.compile(MiriamLink.getDatatype("uniprot isoform").getPattern());
  private final Pattern refseqIdPattern = Pattern.compile(MiriamLink.getDatatype("refseq").getPattern());
  private final Pattern uniprotIdPattern = Pattern.compile(MiriamLink.getDatatype("uniprot knowledgebase").getPattern());

  public CPathServiceImpl() {
    this.simpleIO = new SimpleIOHandler(BioPAXLevel.L3);
    this.simpleIO.mergeDuplicates(true);
  }

  /**
   * Loads the main BioPAX model, etc.
   * This is not required during the data import
   * (in premerge, merge, index, etc.);
   * call this only after the web service is up and running.
   */
  synchronized public void init() {
    if(paxtoolsModel == null) {
      paxtoolsModel = loadMainModel();
      if (paxtoolsModel != null) {
        paxtoolsModel.setXmlBase(settings.getXmlBase());
        log.info("Main BioPAX model (in-memory) is now ready for queries.");
        searcher = new SearchEngine(paxtoolsModel, settings.indexDir());
        ((SearchEngine) searcher).setMaxHitsPerPage(settings.getMaxHitsPerPage());
      }
    }
    loadBlacklist();
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
  }

  public ServiceResponse search(String queryStr,
                                int page, Class<? extends BioPAXElement> biopaxClass,
                                String[] dsources, String[] organisms)
  {
    if(modelNotReady() || searcher == null)
      return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try later)...");

    try {
      // do search
      SearchResponse hits = searcher.search(queryStr, page, biopaxClass, dsources, organisms);

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
      m.setXmlBase(paxtoolsModel.getXmlBase());
    }

    return m;
  }

  public ServiceResponse getNeighborhood(final OutputFormat format,
                                         Map<String, String> formatOptions,
                                         final String[] sources,
                                         Integer limit,
                                         Direction direction,
                                         final String[] organisms,
                                         final String[] datasources,
                                         boolean subPathways)
  {
    if(modelNotReady())
      return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try later)...");

    if(direction == null) {
      direction = Direction.UNDIRECTED; //TODO: use BOTHSTREAM (less data as it ignores MIs)?
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

  public ServiceResponse getPathsBetween(final OutputFormat format,
                                         Map<String, String> formatOptions, final String[] sources, final Integer limit,
                                         final String[] organisms, final String[] datasources, boolean subPathways)
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

  public ServiceResponse getPathsFromTo(final OutputFormat format, Map<String, String> formatOptions,
                                        final String[] sources, final String[] targets, final Integer limit,
                                        final String[] organisms, final String[] datasources, boolean subPathways)
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
          : QueryExecuter.runPathsFromToMultiSet(source, target, paxtoolsModel, LimitType.NORMAL, limit,
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
      // remove all Pathway objects from the result model (TODO: keep pathway name,uri somehow)
      // (- pathways become incomplete after detaching from main PC model;
      // these look confusing after converting to other format.)
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

  public ServiceResponse getCommonStream(final OutputFormat format,
                                         Map<String, String> formatOptions, final String[] sources,
                                         final Integer limit, Direction direction,
                                         final String[] organisms, final String[] datasources, boolean subPathways)
  {
    if(modelNotReady())
      return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try again later)...");

    if (direction == Direction.BOTHSTREAM) {
      return new ErrorResponse(BAD_REQUEST, "Direction cannot be BOTHSTREAM for the COMMONSTREAM query");
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
        m.setUri("PC_graph_commonstream_" + desc.hashCode());
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
   * Mapping IDs to BioPAX entity URIs.
   *
   * @param identifiers - a list of genes/protein or molecules as: \
   * 		HGNC symbols, UniProt, RefSeq and NCBI Gene IDs; or \
   * 		CHEBI, InChIKey, ChEMBL, DrugBank, PubChem Compound, KEGG Compound, PharmGKB.
   * @param types filter search to get back URIs of given biopax types and sub-types
   * @return URIs of matching Xrefs
   *
   * See also: issue #296
   */
  private String[] findUrisByIds(String[] identifiers, Class<? extends BioPAXElement>... types)
  {
    if (identifiers.length == 0)
      return identifiers; //empty array

    Set<String> uris = new TreeSet<>();

    StringBuilder q = new StringBuilder();
    for (String identifier : identifiers)
    {
      if(identifier.startsWith("http://")) {
        // must be valid URI of some existing BioPAX object in our model
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

      if(types.length==0) types = DEFAULT_SEED_TYPES; //BioPAX types to search in

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

    if(idOrUri.startsWith("http://")) {
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
   * Collect "top" pathways pathways (sort of) such as those having
   * controlledOf, pathwayComponentOf and stepProcessOf properties empty, and
   * excluding pathways with less than three components unless there is a non-trivial sub-pathway.
   */
  public ServiceResponse topPathways(String q, final String[] organisms, final String[] datasources) {

    if(modelNotReady() || searcher == null)
      return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try later)...");

    if(q==null || q.isEmpty()) //too much data
      return new ErrorResponse(BAD_REQUEST,"Query string was empty.");

    SearchResponse topPathways = new SearchResponse();
    final List<SearchHit> hits = topPathways.getSearchHit(); //empty list
    int page = 0; // will use search pagination

    SearchResponse r;
    try {
      r = searcher.search(q, page, Pathway.class, datasources, organisms);
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
        r = searcher.search(q, ++page, Pathway.class, datasources, organisms);
      } catch(Exception e) {
        log.error("topPathways() failed", e);
        return new ErrorResponse(INTERNAL_ERROR, e);
      }
    }

    // final touches...
    topPathways.setNumHits((long)hits.size());
    topPathways.setComment("Top Pathways (technically, each has empty index " +
      "field 'pathway'; that also means, they are neither components of " +
      "other pathways nor controlled of any process)");
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

  public Set<String> map(String fromId, final String toDb) {
    return map(Collections.singletonList(fromId), toDb);
  }

  public Set<String> map(Collection<String> fromIds, final String toDb) {
    Assert.hasText(toDb,"toDb must be not null, empty or blank");
    Assert.isTrue("CHEBI".equalsIgnoreCase(toDb) || "UNIPROT".equalsIgnoreCase(toDb),
      "toDb is not CHEBI or UNIPROT");

    if(fromIds.isEmpty()) {
      log.debug("map(), the argument 'fromIds' is an empty collection.");
      return Collections.emptySet();
    }

    List<String> sourceIds = new ArrayList<>();
    // let's guess the source db (id type) and take care of isoform ids;
    // it's risky if a no-prefix integer ID type (pubchem cid, sid) is used and no srcDb is provided;
    // nevertheless, for bio-polymers, we support the only 'NCBI Gene' (integer) ID type.
    for(String fromId : fromIds)
    {
      if (fromId.matches("^\\d+$") && !toDb.equalsIgnoreCase("UNIPROT")) {
        //an integer ID is expected to mean NCBI gene ID, and can be mapped only to UNIPROT;
        //so, skip this one (won't map to anything anyway)
        log.debug("map(), won't map " + fromId + " to " + toDb + " (ambiguous ID, unknown source)");
        continue;
      } else if (toDb.equalsIgnoreCase("UNIPROT") && isoformIdPattern.matcher(fromId).find() && fromId.contains("-")) {
        //it's certainly a uniprot isoform id; so we replace it with the corresponding accession number
        fromId = fromId.replaceFirst("-\\d+$", "");
      } else if (toDb.equalsIgnoreCase("UNIPROT") && refseqIdPattern.matcher(fromId).find() && fromId.contains(".")) {
        //remove the versiovaluesn number, such as ".1"
        fromId = fromId.replaceFirst("\\.\\d+$", "");
      }

      sourceIds.add(fromId); //collect
    }

    final List<Mapping> mappings = (sourceIds.size()==1)
      ? mappingsRepository.findBySrcIdAndDestIgnoreCase(sourceIds.get(0), toDb)
      : mappingsRepository.findBySrcIdInAndDestIgnoreCase(sourceIds, toDb);

    final Set<String> results = new TreeSet<>();
    for(Mapping m : mappings) {
      if(toDb.equalsIgnoreCase(m.getDest()))
        results.add(m.getDestId());
    }
    return results;
  }

  /*
   * Track core service events using Google Analytics Measurement Protocol
   */
  public void track(String ip, String category, String label)
  {
    log.info(String.format("%s, %s, %s", ip, category.toUpperCase(), String.valueOf(label).toLowerCase()));
  }

  public MappingsRepository mapping() {
    return mappingsRepository;
  }

  public MetadataRepository metadata() {
    return metadataRepository;
  }

  public void index()
  {
    init(); //very important - loads the model

    log.info("Associating bio IDs with BioPAX objects using nested Xrefs and id-mapping...");
    addIdsAsBiopaxAnnotations();

    ((Indexer)searcher).index();

    log.info("index(), all done.");
  }

  public void clear(Metadata metadata) {
    CPathUtils.cleanupDirectory(intermediateDataDir(metadata), true);
    metadata.setNumInteractions(null);
    metadata.setNumPathways(null);
    metadata.setNumPhysicalEntities(null);
    metadata.getFiles().clear();
  }

  private void addIdsAsBiopaxAnnotations()
  {
    for(final BioPAXElement bpe : getModel().getObjects()) {
      if(!(bpe instanceof Entity || bpe instanceof EntityReference))
        continue; //skip for UtilityClass but EntityReference

      final Set<String> ids = CPathUtils.getXrefIds(bpe);

      // in addition, collect ChEBI and UniProt IDs and then
      // use id-mapping to associate the bpe with more IDs:
      final List<String> uniprotIds = new ArrayList<>();
      final List<String> chebiIds = new ArrayList<>();
      for(String id : ids)
      {
        if(id.startsWith("CHEBI:")) {
          chebiIds.add(id);
        } else if(isoformIdPattern.matcher(id).find()) {
          //cut the isoform num. suffix
          id = id.replaceFirst("-\\d+$", "");
          uniprotIds.add(id);
        } else if(uniprotIdPattern.matcher(id).find()) {
          uniprotIds.add(id);
        }
      }
      addSupportedIdsThatMapToChebi(chebiIds, ids);
      addSupportedIdsThatMapToUniprotId(uniprotIds, ids);

      bpe.getAnnotations().put(SearchEngine.FIELD_XREFID, ids);
    }
  }

  private void addSupportedIdsThatMapToChebi(List<String> chebiIds, final Set<String> resultIds) {
    //find other IDs that map to the ChEBI ID
    for(String id: chebiIds) {
      List<Mapping> mappings = mappingsRepository.findByDestIgnoreCaseAndDestId("CHEBI", id);
      if (mappings != null) {
        //collect (for 'xrefid' full-text index field) only ID types that we want biopax graph queries support
        for (Mapping mapping : mappings) {
          if (mapping.getSrc().equals("PUBCHEM-COMPOUND")
            || mapping.getSrc().equals("CHEBI")
            || mapping.getSrc().equals("DRUGBANK")
            || mapping.getSrc().startsWith("KEGG")
            || mapping.getSrc().startsWith("CHEMBL")
            || mapping.getSrc().startsWith("PHARMGKB")
          ) resultIds.add(mapping.getSrcId());
          //(prefix 'CID:' is included in pubchem-compound ids)
        }
      }
    }
  }

  private void addSupportedIdsThatMapToUniprotId(List<String> uniprotIds, final Set<String> resultIds) {
    //find other IDs that map to the UniProt AC
    for(String id: uniprotIds) {
      List<Mapping> mappings = mappingsRepository.findByDestIgnoreCaseAndDestId("UNIPROT", id);
      if (mappings != null) {
        //collect (for 'xrefid' full-text index field) only ID types that we want graph queries support
        for (Mapping mapping : mappings) {
          if (mapping.getSrc().startsWith("UNIPROT")
            || mapping.getSrc().startsWith("HGNC")
            || mapping.getSrc().equalsIgnoreCase("NCBI GENE")
            || mapping.getSrc().equalsIgnoreCase("REFSEQ")
            || mapping.getSrc().equalsIgnoreCase("IPI")
            || mapping.getSrc().startsWith("ENSEMBL")
          ) resultIds.add(mapping.getSrcId());
        }
      }
    }
  }

  public Model loadMainModel() {
    return CPathUtils.importFromTheArchive(settings.mainModelFile());
  }

  public Model loadWarehouseModel() {
    return CPathUtils.importFromTheArchive(settings.warehouseModelFile());
  }

  public Model loadBiopaxModelByDatasource(Metadata datasource) {
    Path in = Paths.get(settings.biopaxFileNameFull(datasource.getIdentifier()));
    if (Files.exists(in)) {
      return CPathUtils.importFromTheArchive(in.toString());
    } else {
      log.debug("loadBiopaxModelByDatasource, file not found: " + in
        + " (not merged yet, or file was deleted)");
      return null;
    }
  }

  public String getDataArchiveName(Metadata metadata) {
    return Paths.get(settings.dataDir(),metadata.getIdentifier() + ".zip").toString();
  }


  public String intermediateDataDir(Metadata metadata) {
    return Paths.get(settings.dataDir(), metadata.getIdentifier()).toString();
  }

  public void unzipData(Metadata metadata) {
    try {
      String fname = (metadata.getUrlToData().startsWith("classpath:"))//a hack for test
        ? CPathUtils.LOADER.getResource(metadata.getUrlToData()).getFile().getPath()
          : getDataArchiveName(metadata);//production

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
        // create pathway data object
        log.info("unzipData(), adding " + entryName + " of " + metadata.getIdentifier());
        //create the original data file path/name, replacing all unsafe symbols with underscores
        String datafile = mapDataEntryToFile(metadata.getIdentifier(), entryName);
        metadata.addFile(datafile);
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
      throw new RuntimeException("unzipData(), failed reading from: " + metadata.getIdentifier() , e);
    }

    if(metadata.getFiles().isEmpty())
      log.warn("unzipData(), no data found for " + metadata);
  }

  private String mapDataEntryToFile(String metadataIdentifier, String zipEntryName) {
    return Paths.get(settings.dataDir(),metadataIdentifier,
      zipEntryName.replaceAll("[^a-zA-Z0-9.-]", "_") + ".gz").toString();
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

