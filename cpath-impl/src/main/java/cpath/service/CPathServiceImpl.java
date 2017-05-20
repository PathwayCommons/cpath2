package cpath.service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cpath.config.CPathSettings;
import cpath.jpa.*;
import cpath.service.jaxb.*;

import static cpath.service.Status.*;


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
	
	Searcher searcher;

	@Autowired
    MetadataRepository metadataRepository;
	
	@Autowired
    MappingsRepository mappingsRepository;
	
	private SimpleIOHandler simpleIO;
	
	//init on first access to getBlacklist(); so do not use it directly
	private Blacklist blacklist; 
	
	//on first access when proxy model mode is enabled (so do not use the var. directly!)
	private Model paxtoolsModel;

	private final Pattern isoformIdPattern = Pattern.compile(MiriamLink.getDatatype("uniprot isoform").getPattern());
	private final Pattern refseqIdPattern = Pattern.compile(MiriamLink.getDatatype("refseq").getPattern());
	private final Pattern uniprotIdPattern = Pattern.compile(MiriamLink.getDatatype("uniprot knowledgebase").getPattern());

	private final static CPathSettings cpath = CPathSettings.getInstance();

	/**
	 * Constructor
	 */
	public CPathServiceImpl() {
		this.simpleIO = new SimpleIOHandler(BioPAXLevel.L3);
		this.simpleIO.mergeDuplicates(true);
	}


	/**
	 * Loads the main BioPAX model, etc.
	 * This is not required (useless) during the data import (premerge, merge, etc.);
	 * it is called after the web service is started.
	 */
	synchronized public void init() {
		//fork the model loading (which takes quite a while)
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.execute(
			new Runnable() {
			@Override
			public void run() {
				paxtoolsModel = CPathUtils.loadMainBiopaxModel();
				// set for this service
				if(paxtoolsModel != null) {
					paxtoolsModel.setXmlBase(cpath.getXmlBase());
					log.info("Main BioPAX model (in-memory) is now ready for queries.");
					searcher = new SearchEngine(paxtoolsModel, 
							cpath.indexDir());
					((SearchEngine) searcher).setMaxHitsPerPage(
						Integer.parseInt(cpath.getMaxHitsPerPage()));
				}
			}
		});
		executor.shutdown();
		//won't wait (nothing else to do)
		loadBlacklist();
	}

	public Model getModel() {
		return paxtoolsModel;
	}
	public void setModel(Model paxtoolsModel) {
		this.paxtoolsModel = paxtoolsModel;
	}
	
	public Searcher getSearcher() {
		return searcher;
	}
	public void setSearcher(Searcher searcher) {
		this.searcher = searcher;
	}
	
	
	@Override
	public ServiceResponse search(String queryStr, 
			int page, Class<? extends BioPAXElement> biopaxClass, String[] dsources, String[] organisms) 
	{
		if(!paxtoolsModelReady() || searcher == null) 
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


	@Override
	public ServiceResponse fetch(final OutputFormat format, Map<String, String> formatOptions,
								 boolean subPathways, final String... uris)
	{
		if(!paxtoolsModelReady()) 
			return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try later)...");
		
		// extract/convert a sub-model
		try {
			final String[] mappedUris = findUrisByIds(uris);

			Set<BioPAXElement> elements = urisToBpes(paxtoolsModel, mappedUris);
			Model m = autoCompleteAndClone(elements, subPathways);

			//name the sub-model - can be useful when converted to GSEA, etc...
			if(m!= null && !m.getObjects().isEmpty()) {
				if(mappedUris.length==1) {
					String uri = mappedUris[0];
					m.setUri(uri);
					BioPAXElement b = m.getByID(uri);
					if(b instanceof Named) {
						m.setName(((Named) b).getDisplayName() + " " + ArrayUtils.toString(uris));
					} else {
						m.setName(ArrayUtils.toString(uris));
					}
				} else {
					String desc = ArrayUtils.toString(uris);
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
		ArrayList<Filter> filters = new ArrayList<Filter>();
		
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
	
	@Override
	public ServiceResponse getNeighborhood(final OutputFormat format,
										   Map<String, String> formatOptions,
										   final String[] sources,
										   Integer limit,
										   Direction direction,
										   final String[] organisms,
										   final String[] datasources,
										   boolean subPathways)
	{
		if(!paxtoolsModelReady()) 
			return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try later)...");
		
		final String[] src = findUrisByIds(sources);

		if(direction == null) {
			direction = Direction.UNDIRECTED;
		}

		// execute the paxtools graph query
		try {
			// init source elements
			Set<BioPAXElement> elements = urisToBpes(paxtoolsModel, src);

			// Execute the query, get result elements
			elements = QueryExecuter.runNeighborhood(elements, paxtoolsModel,
					limit, direction, createFilters(organisms, datasources));
			Model m = autoCompleteAndClone(elements, subPathways);
			if( m != null) {
				String desc = ArrayUtils.toString(sources);
				m.setUri("PC_graph_neighborhood_" + desc.hashCode());
				m.setName(desc);
			}
			return convert(m, format, formatOptions); //m==null is ok
		} catch (Exception e) {
			return new ErrorResponse(INTERNAL_ERROR, e);
		}
	}

	
	@Override
	public ServiceResponse getPathsBetween(final OutputFormat format,
										   Map<String, String> formatOptions, final String[] sources, final Integer limit,
										   final String[] organisms, final String[] datasources, boolean subPathways)
	{	
		if(!paxtoolsModelReady()) 
			return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try later)...");
		
		final String[] src = findUrisByIds(sources);
		
		// execute the paxtools graph query
		try {
			// init source elements
			Set<BioPAXElement> elements = urisToBpes(paxtoolsModel, src);

			// Execute the query, get result elements
			elements = QueryExecuter.runPathsBetween(elements, paxtoolsModel, limit,
					createFilters(organisms, datasources));
			Model m = autoCompleteAndClone(elements,subPathways);
			if(m != null) {
				String desc = ArrayUtils.toString(sources);
				m.setUri("PC_graph_pathsbetween_" + desc.hashCode());
				m.setName(desc);
			}

			return convert(m, format, formatOptions);
		} catch (Exception e) {
			return new ErrorResponse(INTERNAL_ERROR, e);
		}
	}

	
	@Override
	public ServiceResponse getPathsFromTo(final OutputFormat format,
										  Map<String, String> formatOptions, final String[] sources, final String[] targets, final Integer limit,
										  final String[] organisms, final String[] datasources, boolean subPathways)
	{
		if(!paxtoolsModelReady()) 
			return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try later)...");
		
		final String[] src = findUrisByIds(sources);
		final String[] tgt = findUrisByIds(targets);
		
		// execute the paxtools graph query	
		try {
			// init source and target elements
			Set<BioPAXElement> source = urisToBpes(paxtoolsModel, src);
			Set<BioPAXElement> target = urisToBpes(paxtoolsModel, tgt);

			Model m = null;
			if(!source.isEmpty() && !target.isEmpty())
			{
				// Execute the query
				Set<BioPAXElement> elements = (target == null || target.isEmpty())
					? QueryExecuter.runPathsBetween(source, paxtoolsModel, limit, createFilters(organisms, datasources))
					: QueryExecuter.runPathsFromTo(source, target,
							paxtoolsModel, LimitType.NORMAL, limit, createFilters(organisms, datasources));

				m = autoCompleteAndClone(elements,subPathways);
				if(m != null) {
					String desc = ArrayUtils.toString(sources) + "-to-" + ArrayUtils.toString(targets);
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

		if(format != OutputFormat.BIOPAX && m != null) {
			// remove all Pathway objects (these, esp. sub-pathways, are incomplete due to detaching from PC
			// and ain't really useful for converting to text formats)
			for(Pathway p : new HashSet<Pathway>(m.getObjects(Pathway.class))) {
				m.remove(p);
			}
		}

		return biopaxConverter.convert(m, format, (options!=null)?options:Collections.emptyMap());
	}


	@Override
	public ServiceResponse getCommonStream(final OutputFormat format,
										   Map<String, String> formatOptions, final String[] sources, final Integer limit, Direction direction,
										   final String[] organisms, final String[] datasources, boolean subPathways)
	{
		if(!paxtoolsModelReady()) 
			return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try again later)...");
		
		final String[] src = findUrisByIds(sources);

		if (direction == Direction.BOTHSTREAM) {
			return new ErrorResponse(BAD_REQUEST, "Direction cannot be BOTHSTREAM for the COMMONSTREAM query");
		} else if(direction == null) {
			direction = Direction.DOWNSTREAM;
		}
		
		// execute the paxtools graph query
		try {
			// init source elements
			Set<BioPAXElement> elements = urisToBpes(paxtoolsModel, src);

			// Execute the query, get result elements
			elements = QueryExecuter
					.runCommonStreamWithPOI(elements, paxtoolsModel, direction, limit,
							createFilters(organisms, datasources));
			Model m = autoCompleteAndClone(elements,subPathways);
			if(m != null) {
				String desc = ArrayUtils.toString(sources);
				m.setUri("PC_graph_commonstream_" + desc.hashCode());
				m.setName(desc);
			}

			return convert(m, format, null);
		} catch (Exception e) {
			return new ErrorResponse(INTERNAL_ERROR, e);
		}
	}

	
	/**
	 * Mapping IDs to BioPAX entity URIs.
	 *
	 *
	 * @param identifiers - a list of genes/protein or molecules as: \
	 * 		HGNC symbols, UniProt, RefSeq and NCBI Gene IDs; or \
	 * 		CHEBI, InChIKey, ChEMBL, DrugBank, PubChem Compound, KEGG Compound, PharmGKB.
	 * @param types filter search to get back URIs of given biopax types and sub-types
	 * @return URIs of matching Xrefs
	 */
	private String[] findUrisByIds(String[] identifiers, Class<? extends BioPAXElement>... types)
	{
		if (identifiers.length == 0)
			return identifiers; //empty array
		
		final Set<String> uris = new TreeSet<String>();

		// id-mapping: get primary IDs where possible; 
		// build a Lucene query string (will be eq. to xrefid:"A" OR xrefid:"B" OR ...)
		final StringBuilder q = new StringBuilder();
		for (String identifier : identifiers)
		{
			if(identifier.startsWith("http://")) {
				// must be valid URI of some existing BioPAX object in our model
				uris.add(identifier);
			} else {
				// replace ':' with "?" for this to match (due to use of Lucene StandardAnalyzer, not-analyzed 'xrefid' field and multi-field query parser)
				identifier = identifier.replaceAll(":","?");
				if (!q.toString().contains(identifier)) {
					q.append("xrefid:").append(identifier).append(" ");
				}
			}
		}

		if (q.length() > 0) {
			//find existing URIs by ids using full-text search (collect all hits, because the query is very specific.
			final String query = q.toString().trim();
			//find URIs of giving BioPAX classes
			if(types.length==0)
				types = new Class[]{PhysicalEntity.class, Gene.class};
			for(Class type : types) {
				findAllUris(uris, query, type);
			}
			log.debug("findUrisByIds, seeds: " + uris + " were found by IDs: " + Arrays.toString(identifiers));
		}

		return uris.toArray(new String[]{});
	}

	void findAllUris(Set<String> collectedUris, String query, Class<? extends BioPAXElement> biopaxTypeFilter) {
		log.debug("findAllUris, search in " + biopaxTypeFilter.getSimpleName() + " using query: " + query);
		int page = 0; // will use search pagination; collect all hits from all result pages
		SearchResponse resp = (SearchResponse) search(query, page, biopaxTypeFilter, null, null);
		while (!resp.isEmpty()) {
			for (SearchHit h : resp.getSearchHit()) collectedUris.add(h.getUri());
			// go to next page
			resp = (SearchResponse) search(query, ++page, biopaxTypeFilter, null, null);
		}
	}

	@Override
	public ServiceResponse traverse(String propertyPath, String... uris) {
		
		if(!paxtoolsModelReady()) 
			return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try later)...");
		
		TraverseResponse res = new TraverseResponse();
		res.setPropertyPath(propertyPath);

		try {
			//both IDs and absolute URIs now work!
			int idx = propertyPath.indexOf('/');
			if(idx <= 0){
				throw new IllegalBioPAXArgumentException("Path does not start from a BioPAX type name.");
			}
			//BioPAX type at the beginning of the path -
			Class<? extends BioPAXElement> type = BioPAXLevel.L3.getInterfaceForName(propertyPath.substring(0, idx));
			String[] sourceUris =  findUrisByIds(uris, type); // apply id-mapping to get URIs if necessary
			TraverseAnalysis traverseAnalysis = new TraverseAnalysis(res, sourceUris);
			traverseAnalysis.execute(paxtoolsModel);
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
	 * "Top pathway" can mean different things...
	 * 
	 * 1) One may want simply collect pathways which are not 
	 * values of any BioPAX property (i.e., a "graph-theoretic" approach, 
	 * used by {@link ModelUtils#getRootElements(org.biopax.paxtools.model.Model, Class)} method) and by
	 * BioPAX normalizer, which (in the cPath2 "premerge" stage),
	 * for all Entities, generates relationship xrefs to their "parent" pathways.
	 * 
	 * 2) Another approach would be to check whether specific (inverse) 
	 * properties, such as controlledOf, pathwayComponentOf and stepProcessOf, are empty.
	 * 
	 * Here we follow the second method.
	 *
	 * Also, let's exclude "pathways" having two or less components, none of which is a non-empty pathway.
	 */
	@Override
	public ServiceResponse topPathways(String q, final String[] organisms, final String[] datasources) {
		
		if(!paxtoolsModelReady() || searcher == null) 
			return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try later)...");

		if(q==null || q.isEmpty()) q = "*"; //for backward compatibility

		SearchResponse topPathways = new SearchResponse();
		final List<SearchHit> hits = topPathways.getSearchHit(); //empty list
		int page = 0; // will use search pagination
		
		SearchResponse r = null;
		try {
			r = searcher.search(q, page, Pathway.class, datasources, organisms);
		} catch(Exception e) {
			log.error("topPathways() failed", e);
			return new ErrorResponse(INTERNAL_ERROR, e);
		}
		
		//go over all hits, all pages
		final int numPathways = r.getNumHits();
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
								&& !((Pathway)component).getPathwayComponent().isEmpty())
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
		topPathways.setNumHits(hits.size());
		topPathways.setComment("Top Pathways (technically, each has empty index " +
				"field 'pathway'; that also means, they are neither components of " +
				"other pathways nor controlled of any process)");
		topPathways.setMaxHitsPerPage(hits.size());
		topPathways.setPageNo(0);
			
		return topPathways;
	}


	public Blacklist getBlacklist() {	
		return blacklist;
	}
	
	public void setBlacklist(Blacklist blacklist) {
		this.blacklist = blacklist;
	}

	
	/**
	 * This utility method prepares the source or target 
	 * object sets for a graph query.
	 * 
	 * @param model source model
	 * @param ids specific source or target set of IDs
	 * @return related biopax elements
	 */
	private static Set<BioPAXElement> urisToBpes(Model model, String[] ids)
	{
		Set<BioPAXElement> elements = new HashSet<BioPAXElement>();

		for(Object id : ids) {
			BioPAXElement e = model.getByID(id.toString());

			if(e != null)
				elements.add(e);
			else
				log.warn("urisToBpes: unknown/broken URI: " + id);
		}

		return elements;
	}
	

	private synchronized void loadBlacklist() 
	{	
		Resource blacklistResource = new DefaultResourceLoader()
			.getResource("file:" + cpath.blacklistFile());
		
		if(blacklistResource.exists()) {			
			try {
				this.blacklist = new Blacklist(blacklistResource.getInputStream());
				log.info("loadBlacklist, loaded: " + blacklistResource.getDescription());
				Assert.notEmpty(blacklist.getListed());
			} catch (IOException e) {
				log.error("loadBlacklist, failed using: " 
					+ blacklistResource.getDescription(), e);
			}
		} else {
			log.warn("loadBlacklist, " + cpath.blacklistFile() + " is not found");
		}
	}


	private boolean paxtoolsModelReady() {
		return paxtoolsModel != null;
	}


	public Set<String> map(String fromId, final String toDb) {
		return map(Arrays.asList(fromId), toDb);
	}


	public Set<String> map(Collection<String> fromIds, final String toDb) {
		Assert.hasText(toDb);
		Assert.isTrue("CHEBI".equalsIgnoreCase(toDb) || "UNIPROT".equalsIgnoreCase(toDb));

		if(fromIds.isEmpty()) {
			log.debug("map(), the argument 'fromIds' is an empty collection.");
			return Collections.emptySet();
		}

		List<String> sourceIds = new ArrayList<String>();
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
				//remove the version number, such as ".1"
				fromId = fromId.replaceFirst("\\.\\d+$", "");
			}

			sourceIds.add(fromId); //collect
		}

		final List<Mapping> mappings = (sourceIds.size()==1)
			? mappingsRepository.findBySrcIdAndDestIgnoreCase(sourceIds.get(0), toDb)
				: mappingsRepository.findBySrcIdInAndDestIgnoreCase(sourceIds, toDb);

		final Set<String> results = new TreeSet<String>();
		for(Mapping m : mappings) {
			if(toDb.equalsIgnoreCase(m.getDest()))
				results.add(m.getDestId());
		}
		return results;
	}


	@Override
	public void log(Collection<LogEvent> events, String ipAddr) {
		for(LogEvent event : events) {
			log.info(String.format("%s, %s, %s", ipAddr, event.getType(), event.getName()));
		}
	}

	@Override
	public Metadata save(Metadata metadata) {
		log.info("Saving metadata: " + metadata.getIdentifier());

		if(metadata.getId() != null) { //update
			metadata = metadataRepository.save(metadata);
		} else {
			Metadata existing = metadataRepository.findByIdentifier(metadata.getIdentifier());
			if(existing != null)  {//update (except for the Content list, which should not be touched unless in Premerge)
				existing.setAvailability(metadata.getAvailability());
				existing.setCleanerClassname(metadata.getCleanerClassname());
				existing.setConverterClassname(metadata.getConverterClassname());
				existing.setDescription(metadata.getDescription());
				existing.setName(metadata.getName());
				existing.setIconUrl(metadata.getIconUrl());
				existing.setNumInteractions(metadata.getNumInteractions());
				existing.setNumPathways(metadata.getNumPathways());
				existing.setNumPhysicalEntities(metadata.getNumPhysicalEntities());
				existing.setPubmedId(metadata.getPubmedId());
				existing.setType(metadata.getType());
				existing.setUrlToData(metadata.getUrlToData());
				existing.setUrlToHomepage(metadata.getUrlToHomepage());
				metadata = existing;
				//the jpa managed (persistent) entity will be auto-updated/flashed
			}

			metadata = metadataRepository.save(metadata);
		}

		return metadata;
    }

	@Override
	public void delete(Metadata metadata) {
    	metadataRepository.delete(metadata);
		CPathUtils.cleanupDirectory(metadata.outputDir(), true);
		try {
			Files.delete(Paths.get(metadata.outputDir()));
		} catch (IOException e) {}
	}


	@Override
	public void addOrUpdateMetadata(String location) {
    	for (Metadata mdata : CPathUtils.readMetadata(location))
    		save(mdata);
 	}

	@Override
	public MappingsRepository mapping() {
		return mappingsRepository;
	}


	@Override
	public MetadataRepository metadata() {
		return metadataRepository;
	}

	public void index() throws IOException {
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Admin mode is not enabled");

		if(paxtoolsModel==null)
			paxtoolsModel = CPathUtils.loadMainBiopaxModel();
		// set for this service

		log.info("Associating more biological IDs with BioPAX objects using nested Xrefs and id-mapping...");
		addIdsAsBiopaxAnnotations();

		//Build the full-text (lucene) index
		SearchEngine searchEngine = new SearchEngine(getModel(), cpath.indexDir());
		searchEngine.index();

		// Updates counts of pathways, etc. and saves in the Metadata table.
     	// This depends on the full-text index, which must have been created already (otherwise, results will be wrong).
		setSearcher(searchEngine);
		log.info("Updating pathway/interaction/participant counts - per data source...");
		// Prepare a list of all pathway type metadata to update
		List<Metadata> pathwayMetadata = new ArrayList<Metadata>();
		for (Metadata md : metadataRepository.findAll())
			if (!md.isNotPathwayData())
				pathwayMetadata.add(md);

		// for each non-warehouse metadata entry, update counts of pathways, etc.
		for (Metadata md : pathwayMetadata) {
			String name = md.standardName();
			String[] dsUrisFilter = new String[] { md.getUri() };

			SearchResponse sr = searcher.search("*", 0, Pathway.class, dsUrisFilter, null);
			md.setNumPathways(sr.getNumHits());
			log.info(name + " - pathways: " + sr.getNumHits());

			sr = searcher.search("*", 0, Interaction.class, dsUrisFilter, null);
			md.setNumInteractions(sr.getNumHits());
			log.info(name + " - interactions: " + sr.getNumHits());

			Integer count;
			sr = searcher.search("*", 0, PhysicalEntity.class, dsUrisFilter, null);
			count = sr.getNumHits();
			sr = searcher.search("*", 0, Gene.class, dsUrisFilter, null);
			count += sr.getNumHits();
			md.setNumPhysicalEntities(count);
			log.info(name + " - molecules, complexes and genes: " + count);
		}

		metadataRepository.save(pathwayMetadata);

		log.info("index(), all done.");
	}

	@Override
	public synchronized Metadata clear(Metadata metadata) {
		CPathUtils.cleanupDirectory(metadata.outputDir(), true);
		metadata.setNumInteractions(null);
		metadata.setNumPathways(null);
		metadata.setNumPhysicalEntities(null);
		metadata.getContent().clear();
		metadata.setPremerged(null);
		return save(metadata);
	}

	private void addIdsAsBiopaxAnnotations()
	{
		for(final BioPAXElement bpe : getModel().getObjects()) {
			if(!(bpe instanceof Entity || bpe instanceof EntityReference))
				continue; //skip for UtilityClass but EntityReference

			final Set<String> ids = CPathUtils.getXrefIds(bpe);

			// in addition, collect ChEBI and UniProt IDs and then
			// use id-mapping to associate the bpe with more IDs:
			final List<String> uniprotIds = new ArrayList<String>();
			final List<String> chebiIds = new ArrayList<String>();
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

	void addSupportedIdsThatMapToChebi(List<String> chebiIds, final Set<String> resultIds) {
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

	void addSupportedIdsThatMapToUniprotId(List<String> uniprotIds, final Set<String> resultIds) {
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

}

