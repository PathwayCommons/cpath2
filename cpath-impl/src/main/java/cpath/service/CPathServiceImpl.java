/**
 ** Copyright (c) 2010 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** both UofT and MSKCC have no obligations to provide maintenance, 
 ** support, updates, enhancements or modifications.  In no event shall
 ** UofT or MSKCC be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** UofT or MSKCC have been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this software; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA;
 ** or find it at http://www.fsf.org/ or http://www.gnu.org.
 **/


package cpath.service;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.biopax.paxtools.controller.*;
import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.normalizer.MiriamLink;
import org.biopax.paxtools.pattern.util.Blacklist;
import org.biopax.paxtools.query.QueryExecuter;
import org.biopax.paxtools.query.algorithm.Direction;
import org.biopax.paxtools.query.algorithm.LimitType;
import org.biopax.paxtools.query.wrapperL3.DataSourceFilter;
import org.biopax.paxtools.query.wrapperL3.Filter;
import org.biopax.paxtools.query.wrapperL3.OrganismFilter;
import org.biopax.paxtools.query.wrapperL3.UbiqueFilter;
import org.biopax.validator.api.ValidatorUtils;
import org.biopax.validator.api.beans.ValidatorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cpath.config.CPathSettings;
import cpath.jpa.Content;
import cpath.jpa.LogEntitiesRepository;
import cpath.jpa.LogEntity;
import cpath.jpa.LogEvent;
import cpath.jpa.LogType;
import cpath.jpa.Mapping;
import cpath.jpa.MappingsRepository;
import cpath.jpa.Metadata;
import cpath.jpa.MetadataRepository;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.ServiceResponse;
import cpath.service.jaxb.TraverseResponse;

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
	LogEntitiesRepository logEntitiesRepository;
	
	@Autowired
    MetadataRepository metadataRepository;
	
	@Autowired
    MappingsRepository mappingsRepository;
	
	private SimpleIOHandler simpleIO;
	
	//init. on first access to getBlacklist(); so do not use it directly
	private Blacklist blacklist; 
	
	//init. on first access when proxy model mode is enabled (so do not use the var. directly!)
	private Model paxtoolsModel;

	private final Pattern isoformIdPattern = Pattern.compile(MiriamLink.getDatatype("uniprot isoform").getPattern());
	private final Pattern refseqIdPattern = Pattern.compile(MiriamLink.getDatatype("refseq").getPattern());

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
	public ServiceResponse fetch(final OutputFormat format, final String... uris) {
		if (uris.length == 0)
			return new ErrorResponse(NO_RESULTS_FOUND,
					"No URIs were specified for the query");
		
		if(!paxtoolsModelReady()) 
			return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try later)...");
		
		// extract/convert a sub-model
		try {
			final String[] mappedUris = findUrisByIds(uris);
			Set<BioPAXElement> elements = urisToBpes(paxtoolsModel, mappedUris);
			
			if(elements.isEmpty()) {
				return new ErrorResponse(NO_RESULTS_FOUND,
						"No BioPAX objects found by URI(s): " + Arrays.toString(uris));
			}
					
			//auto-complete (add important child/parent elements)	
			elements = (new Completer(simpleIO.getEditorMap())).complete(elements, paxtoolsModel); 
			assert !elements.isEmpty() : "Completer.complete() produced empty set from not empty";

			Cloner cloner = new Cloner(this.simpleIO.getEditorMap(), this.simpleIO.getFactory());
			Model m = cloner.clone(paxtoolsModel, elements);
			m.setXmlBase(paxtoolsModel.getXmlBase());
			return convert(m, format);
			
		} catch (Exception e) {
			return new ErrorResponse(INTERNAL_ERROR, e);
		}

    }


	private Filter[] createFilters(String[] organisms, String[] datasources) {
		ArrayList<Filter> filters = new ArrayList<Filter>();
		
		if(getBlacklist() != null)
			filters.add(new UbiqueFilter(getBlacklist().getListed()));
		
		if(organisms != null && organisms.length > 0)
			filters.add(new OrganismFilter(organisms));
		
		if(datasources != null && datasources.length > 0)
			filters.add(new DataSourceFilter(datasources));
		
		return filters.toArray(new Filter[]{});
	}

	
	@Override
	public ServiceResponse getNeighborhood(final OutputFormat format, 
		final String[] sources, Integer limit, Direction direction, 
		final String[] organisms, final String[] datasources)
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
			if(elements.isEmpty()) {
				return new ErrorResponse(NO_RESULTS_FOUND,
						"No BioPAX objects found by URI(s): " + Arrays.toString(src));
			}

			// Execute the query, get result elements
			elements = QueryExecuter.runNeighborhood(elements, paxtoolsModel,
					limit, direction, createFilters(organisms, datasources));

			if(elements != null && !elements.isEmpty()) {
				// auto-complete (gets a reasonable size sub-model)
				elements = (new Completer(simpleIO.getEditorMap())).complete(elements, paxtoolsModel);
				Cloner cloner = new Cloner(this.simpleIO.getEditorMap(), this.simpleIO.getFactory());
				Model m = cloner.clone(paxtoolsModel, elements);
				m.setXmlBase(paxtoolsModel.getXmlBase());
				return convert(m, format);
			} else {
				return new ErrorResponse(NO_RESULTS_FOUND,
						"No results found by URI(s): " + Arrays.toString(src));
			}
		} catch (Exception e) {
			return new ErrorResponse(INTERNAL_ERROR, e);
		}

	}

	
	@Override
	public ServiceResponse getPathsBetween(final OutputFormat format, 
			final String[] sources, final Integer limit, 
			final String[] organisms, final String[] datasources)
	{	
		if(!paxtoolsModelReady()) 
			return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try later)...");
		
		final String[] src = findUrisByIds(sources);
		
		// execute the paxtools graph query
		try {
			// init source elements
			Set<BioPAXElement> elements = urisToBpes(paxtoolsModel, src);
			if(elements.isEmpty()) {
				return new ErrorResponse(NO_RESULTS_FOUND,
						"No BioPAX objects found by URI(s): " + Arrays.toString(src));
			}

			// Execute the query, get result elements
			elements = QueryExecuter.runPathsBetween(elements, paxtoolsModel, limit,
					createFilters(organisms, datasources));

			// auto-complete (gets a reasonable size sub-model)
			if(elements != null) {
				elements = (new Completer(simpleIO.getEditorMap())).complete(elements, paxtoolsModel);
				Cloner cloner = new Cloner(this.simpleIO.getEditorMap(), this.simpleIO.getFactory());
				Model m = cloner.clone(paxtoolsModel, elements);
				m.setXmlBase(paxtoolsModel.getXmlBase());
				return convert(m, format);
			} else {
				return new ErrorResponse(NO_RESULTS_FOUND,
						"No results found by URI(s): " + Arrays.toString(src));
			}
		} catch (Exception e) {
			return new ErrorResponse(INTERNAL_ERROR, e);
		}

	}

	
	@Override
	public ServiceResponse getPathsFromTo(final OutputFormat format, 
		final String[] sources, final String[] targets, final Integer limit,
		final String[] organisms, final String[] datasources)
	{
		if(!paxtoolsModelReady()) 
			return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try later)...");
		
		final String[] src = findUrisByIds(sources);
		final String[] tgt = findUrisByIds(targets);
		
		// execute the paxtools graph query	
		try {
			// init source and target elements
			Set<BioPAXElement> source = urisToBpes(paxtoolsModel, src);
			if(source.isEmpty()) {
				return new ErrorResponse(NO_RESULTS_FOUND,
						"No source BioPAX objects found by URI(s): " + Arrays.toString(src));
			}
			Set<BioPAXElement> target = urisToBpes(paxtoolsModel, tgt);
			if(target.isEmpty()) {
				return new ErrorResponse(NO_RESULTS_FOUND,
						"No target BioPAX objects found by URI(s): " + Arrays.toString(tgt));
			}

			// Execute the query
			Set<BioPAXElement> elements = (target==null || target.isEmpty()) 
					? QueryExecuter.runPathsBetween(source, paxtoolsModel, limit,
							createFilters(organisms, datasources))
							: QueryExecuter.runPathsFromTo(source, target, 
									paxtoolsModel, LimitType.NORMAL, limit,
									createFilters(organisms, datasources));

					if(elements != null) {
						// auto-complete (gets a reasonable size sub-model)
						elements = (new Completer(simpleIO.getEditorMap())).complete(elements, paxtoolsModel);
						Cloner cloner = new Cloner(this.simpleIO.getEditorMap(), this.simpleIO.getFactory());
						Model m = cloner.clone(paxtoolsModel, elements);
						m.setXmlBase(paxtoolsModel.getXmlBase());
						return convert(m, format);
					} else {
						return new ErrorResponse(NO_RESULTS_FOUND,
								"No results found; source: " + Arrays.toString(src)
								+  ", target: " + Arrays.toString(tgt));
					}	
		} catch (Exception e) {
			return new ErrorResponse(INTERNAL_ERROR, e);
		}

	}
	

	private ServiceResponse convert(Model m, OutputFormat format) {
		BiopaxConverter biopaxConverter = new BiopaxConverter(getBlacklist());
		ServiceResponse toReturn;

		if (format==OutputFormat.GSEA)
			toReturn = biopaxConverter.convert(m, format, "uniprot", false); //uniprot IDs, pathway entries only, etc.
		else
			toReturn = biopaxConverter.convert(m, format); //using default config. (ID type, layout, etc.)

		return toReturn;
	}


	@Override
	public ServiceResponse getCommonStream(final OutputFormat format, 
		final String[] sources, final Integer limit, Direction direction,
		final String[] organisms, final String[] datasources)
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
			if(elements.isEmpty()) {
				return new ErrorResponse(NO_RESULTS_FOUND, "No BioPAX objects found by URIs: " + Arrays.toString(src));
			}

			// Execute the query, get result elements
			elements = QueryExecuter
					.runCommonStreamWithPOI(elements, paxtoolsModel, direction, limit,
							createFilters(organisms, datasources));

			if(elements != null) {
				// auto-complete (gets a reasonable size sub-model)
				elements = (new Completer(simpleIO.getEditorMap())).complete(elements, paxtoolsModel);
				Cloner cloner = new Cloner(this.simpleIO.getEditorMap(), this.simpleIO.getFactory());
				Model m = cloner.clone(paxtoolsModel, elements);
				m.setXmlBase(paxtoolsModel.getXmlBase());
				return convert(m, format);
			} else {
				return new ErrorResponse(NO_RESULTS_FOUND,
						"No results found by URI(s): " + Arrays.toString(src));
			}
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
	 * @return URIs of matching Xrefs
	 */
	private String[] findUrisByIds(String[] identifiers)
	{
		if (identifiers.length == 0)
			return identifiers;
		
		final Set<String> uris = new TreeSet<String>();

		// id-mapping: get primary IDs where possible; 
		// build a Lucene query string (will be eq. to xrefid:"A" OR xrefid:"B" OR ...)
		final StringBuilder q = new StringBuilder();
		for (String identifier : identifiers) {
			if(identifier.toLowerCase().startsWith("http://")) {
				// it must be an existing BioPAX object URI (seems, the user hopes so)
				uris.add(identifier);
				//also, if it's a canonical Identifiers.org URI, -
				if(identifier.startsWith("http://identifier.org/")) {
					//extract the id from the URI
					String id = CPathUtils.idfromNormalizedUri(identifier);
					if(!q.toString().contains(id))
						q.append("xrefid:\"").append(id).append("\" ");
				}
			}
			else {
				//id-mapping step is not required (new full-text index associates IDs of supported types with BioPAX objects)
				if (!q.toString().contains(identifier))
					q.append("xrefid:\"").append(identifier).append("\" ");
			}
		}

		if (q.length() > 0) {
			//find existing URIs by ids using full-text search (collect all hits, because the query is very specific.
			final String query = q.toString().trim();
			//search for Gene/PEs (instead of, as it used to be in older versions, searching for xrefs)
			findAllUris(uris, query, PhysicalEntity.class);
			findAllUris(uris, query, Gene.class);
		}
				
		log.debug("findUrisByIds, seeds: " + uris + " were found by IDs: " + Arrays.toString(identifiers));

		return uris.toArray(new String[]{});
	}

	private void findAllUris(Set<String> collectedUris, String query, Class<? extends BioPAXElement> biopaxTypeFilter) {
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
	public ServiceResponse traverse(String propertyPath, String... sourceUris) {
		
		if(!paxtoolsModelReady()) 
			return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try later)...");
		
		TraverseResponse res = new TraverseResponse();
		res.setPropertyPath(propertyPath);
				
		try {
			TraverseAnalysis traverseAnalysis = new TraverseAnalysis(res, sourceUris);
			traverseAnalysis.execute(paxtoolsModel);
			return res;
		} catch (IllegalArgumentException e) {
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
	 */
	@Override
	public ServiceResponse topPathways(final String[] organisms, final String[] datasources) {
		
		if(!paxtoolsModelReady() || searcher == null) 
			return new ErrorResponse(MAINTENANCE,"Waiting for the initialization to complete (try later)...");
		
		SearchResponse topPathways = new SearchResponse();
		final List<SearchHit> hits = topPathways.getSearchHit(); //empty list
		int page = 0; // will use search pagination
		
		SearchResponse r = null;
		try {
			r = searcher.search("*", page, Pathway.class, datasources, organisms);
			
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
				if(h.getPathway().isEmpty() || 
						(h.getPathway().size()==1 
							&& h.getPathway().get(0).equalsIgnoreCase(h.getUri())
						)
					) hits.add(h); //add to the list
				processed++;
			}
			
			if(processed >= numPathways)
				break; //may save us one uselss query
			
			// go next page
			try {
				r = searcher.search("*", ++page, Pathway.class, datasources, organisms);
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
	private Set<BioPAXElement> urisToBpes(Model model, String[] ids)
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
				log.info("loadBlacklist, loaded: " 
						+ blacklistResource.getDescription());
			} catch (IOException e) {
				log.error("loadBlacklist, failed using: " 
					+ blacklistResource.getDescription(), e);
			}
		} else {
			log.warn("loadBlacklist, " + cpath.blacklistFile()
				+ " is not found");
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
			//'total' should not be here (it auto-counts)
			Assert.isTrue(event.getType() != LogType.TOTAL); 
			count(LogUtils.today(), event, ipAddr);
		}
		
		//total counts (is not sum of the above); counts once per request/response
		count(LogUtils.today(), LogEvent.TOTAL, ipAddr);
	}
	
	
	@Override
	public void count(String date, LogEvent event, String ipAddr)
	{		
		//SKIP for original data, validation, blacklist and old version file downloads;
		//by design, this does not affect the total no. events (LogEvent.TOTAL) on that date;
		//this affects only the total no. in the LogType.FILE category
		if(event.getType() == LogType.FILE &&
				!event.getName().startsWith(cpath.exportArchivePrefix()))
		{
			return;
		}

		// find or create a record, count+1
		LogEntity t = null;
		try {
			t = logEntitiesRepository.findByEventNameIgnoreCaseAndAddrAndDate(event.getName(), ipAddr, date);
		} catch (DataAccessException e) {
			log.error("count(), findByEventNameIgnoreCaseAndAddrAndDate " +
				"failed to update event log: " + event.getName() + ", IP: " + ipAddr + ", date: " + date, e);
		}

		if(t == null) {			
			t = new LogEntity(date, event, ipAddr);
		}
		
		t.setCount(t.getCount() + 1);
		log.info(t.toString());

		logEntitiesRepository.save(t);
	}


	@Override
	public ValidatorResponse validationReport(String provider, String file) {
		ValidatorResponse response = new ValidatorResponse();
		Metadata metadata = metadataRepository.findByIdentifier(provider);
		for (Content content : metadata.getContent()) {
			String current = content.getFilename();			
			
			if(file != null && !file.equals(current))
				continue; 
			//file==null means all files			
			
			try {
				// unmarshal and add
				ValidatorResponse resp = (ValidatorResponse) ValidatorUtils.getUnmarshaller()
					.unmarshal(new GZIPInputStream(new FileInputStream(content.validationXmlFile())));
				assert resp.getValidationResult().size() == 1;				
				response.getValidationResult().addAll(resp.getValidationResult());				
			} catch (Exception e) {
				log.error("validationReport: failed converting the XML response to objects", e);
			}
			
			if(current.equals(file))
				break;
		}

		return response;
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
		File dir = new File(metadata.outputDir());
		if(dir.exists() && dir.isDirectory()) {
			CPathUtils.cleanupDirectory(dir);
			dir.delete();
		}
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


	@Override
	public LogEntitiesRepository log() {
		return logEntitiesRepository;
	}


	public void log(String fileName, String ipAddr) {
		log(logEventsFromFilename(fileName), ipAddr);
	}
	

	public Set<LogEvent> logEventsFromFilename(String filename) {
		Set<LogEvent> set = new HashSet<LogEvent>();
		final CPathSettings cpath2 = cpath;

		set.add(new LogEvent(LogType.FILE, filename));
		
		// extract the data provider's standard name from the filename
		if(filename.startsWith(cpath2.exportArchivePrefix())) {
			String scope = LogUtils.fileSrcOrScope(filename);
			if(scope != null) {
				String providerStandardName = null;
				Metadata md = metadataRepository.findByIdentifier(scope);
				if(md != null) { //use the standardName for logging
					providerStandardName = md.standardName();
				} else if(!metadataRepository.findByNameContainsIgnoreCase(scope).isEmpty()) { // found any (ignoring case)?
					providerStandardName = scope.toLowerCase(); //it's by design (how archives are created) standardName
				}
				
				if(providerStandardName != null) {
					set.add(new LogEvent(LogType.PROVIDER, providerStandardName));
				} else {
					//that's probably a by-organism or one of special sub-model archives
					log.debug("'" + scope + "' in " + filename + " does not match any "
						+ "identifier or standard name of currently used data providers");
				}
			} else {
				log.error("Didn't recognize scope of datafile: " + filename);
			}
			
			// extract the format
			OutputFormat outputFormat = LogUtils.fileOutputFormat(filename);
			if(outputFormat!=null)
				set.add(LogEvent.from(outputFormat));

		}
		
		return set;
	}


	public void index() throws IOException {
		if(!cpath.isAdminEnabled())
			throw new IllegalStateException("Admin mode is not enabled");

		if(paxtoolsModel==null)
			paxtoolsModel = CPathUtils.loadMainBiopaxModel();
		// set for this service

		log.info("Associating more identifies with BioPAX model objects' using child elements' xrefs and id-mapping...");
		addOtherIdsAsAnnotations(3);

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

			SearchResponse sr = (SearchResponse) searcher.search("*", 0,
					Pathway.class, dsUrisFilter, null);
			md.setNumPathways(sr.getNumHits());
			log.info(name + " - pathways: " + sr.getNumHits());

			sr = (SearchResponse) searcher.search("*", 0, Interaction.class,
					dsUrisFilter, null);
			md.setNumInteractions(sr.getNumHits());
			log.info(name + " - interactions: " + sr.getNumHits());

			Integer count;
			sr = (SearchResponse) searcher.search("*", 0, PhysicalEntity.class,
					dsUrisFilter, null);
			count = sr.getNumHits();
			sr = (SearchResponse) searcher.search("*", 0, Gene.class,
					dsUrisFilter, null);
			count += sr.getNumHits();
			md.setNumPhysicalEntities(count);
			log.info(name + " - molecules, complexes and genes: " + count);
		}

		metadataRepository.save(pathwayMetadata);

		log.info("index(), all done.");
	}

	@Override
	public synchronized Metadata clear(Metadata metadata) {
		CPathUtils.cleanupDirectory(new File(metadata.outputDir()));
		metadata.setNumInteractions(null);
		metadata.setNumPathways(null);
		metadata.setNumPhysicalEntities(null);
		metadata.getContent().clear();
		metadata.setPremerged(null);
		return save(metadata);
	}

	private void addOtherIdsAsAnnotations(final int depth) {
	//Can't use multiple threads (spring-data-jpa/hibernate errors occur in production, with filesystem H2 db...)
		for(final BioPAXElement bpe : getModel().getObjects()) {
			if(!(bpe instanceof Entity || bpe instanceof EntityReference))
				continue; //skip for UtilityClass but EntityReference
			final Set<String> ids = new HashSet<String>();
			//for Entity or ER, also collect IDs from child UX/RXs and map to other IDs (use idMapping)
			Set<BioPAXElement> children =
					new Fetcher(SimpleEditorMap.get(paxtoolsModel.getLevel()), Fetcher.nextStepFilter,
							//exclude unwanted child objects, such as CVs and other utility classes
							new org.biopax.paxtools.util.Filter<PropertyEditor>() {
								@Override
								public boolean filter(PropertyEditor ed) {
									return EntityReference.class.isAssignableFrom(ed.getRange())
											|| Gene.class.isAssignableFrom(ed.getRange())
											|| PhysicalEntity.class.isAssignableFrom(ed.getRange());
								}
							}).fetch(bpe, depth);

			//include this object itself if it's about a bio macromolecule of chemical
			if (bpe instanceof PhysicalEntity || bpe instanceof EntityReference || bpe instanceof Gene)
				children.add(bpe);

			final List<String> uniprotIds = new ArrayList<String>();
			final List<String> chebiIds = new ArrayList<String>();

			for(BioPAXElement child : children) {
				//as the fetcher uses specific filters, every element can be safely cast to XReferrable
				XReferrable el = (XReferrable) child;
				for(Xref x : el.getXref()) {
					if (!(x instanceof PublicationXref) && x.getId()!=null && x.getDb()!=null) {
						ids.add(x.getId());
						if(x.getDb().equalsIgnoreCase("CHEBI")) {
							if (!chebiIds.contains(x.getId())) chebiIds.add(x.getId());
						} else if(x.getDb().toUpperCase().startsWith("UNIPROT")) {
							String id = x.getId();
							if(id.contains("-")) // then cut the isoform num. suffix
								id = id.replaceFirst("-\\d+$", "");
							if(!uniprotIds.contains(x.getId())) uniprotIds.add(id);
						}
					}
				}
			}

			addSupportedIdsThatMapToChebi(chebiIds, ids);
			addSupportedIdsThatMapToUniprotId(uniprotIds, ids);

			if(!ids.isEmpty()) {
				bpe.getAnnotations().put(SearchEngine.FIELD_XREFID, ids);
				if(log.isDebugEnabled())
					log.debug("addOtherIdsAsAnnotations, " + bpe.getModelInterface().getSimpleName()
							+ " (" + bpe.getUri() + ") maps to: " + ids);
			}
		}
	}

	void addSupportedIdsThatMapToChebi(List<String> chebiIds, final Set<String> resultIds) {
		//find other IDs that map to the ChEBI ID
		List<Mapping> mappings = mappingsRepository.findByDestIgnoreCaseAndDestIdIn("CHEBI", chebiIds);
		if(mappings != null) {
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

	void addSupportedIdsThatMapToUniprotId(List<String> uniprotIds, final Set<String> resultIds) {
		//find other IDs that map to the UniProt AC
		List<Mapping> mappings = mappingsRepository.findByDestIgnoreCaseAndDestIdIn("UNIPROT", uniprotIds);
		if(mappings != null) {
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

