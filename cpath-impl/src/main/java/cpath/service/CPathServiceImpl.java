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
import java.util.zip.GZIPInputStream;

import org.biopax.paxtools.controller.*;
import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.normalizer.MiriamLink;
import org.biopax.paxtools.pattern.miner.BlacklistGenerator2;
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
import cpath.dao.CPathUtils;
import cpath.dao.LogUtils;
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
					paxtoolsModel.setXmlBase(CPathSettings.getInstance().getXmlBase());
					log.info("Main BioPAX model (in-memory) is now ready for queries.");
					searcher = new SearchEngine(paxtoolsModel, 
							CPathSettings.getInstance().indexDir());
					((SearchEngine) searcher).setMaxHitsPerPage(
						Integer.parseInt(CPathSettings.getInstance().getMaxHitsPerPage()));
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
		return (format==OutputFormat.SBGN)
			? (new BiopaxConverter(getBlacklist())).convert(m, format, true) //apply layout
				:  (new BiopaxConverter(getBlacklist())).convert(m, format); //default: 'uniprot' for GSEA; 'HGNC' for SIF.
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
	 * Mapping to BioPAX object (currently - Xref only) URIs.
	 *
	 * It does not "understand" RefSeq Versions and UniProt Isoforms 
	 * (one has to submit canonical identifiers, i.e, ones without "-#" or ".#").
	 *
	 * @param identifiers - a list of genes/protein or molecules as: \
	 * 		HGNC symbols, UniProt, RefSeq, ENS* and NCBI Gene identifiers; or\
	 * 		CHEBI, InChIKey, ChEMBL, DrugBank, CID: (PubChem), SID: (PubChem), KEGG Compound, PharmGKB, or chem. name.
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
			if(identifier.toLowerCase().startsWith("http://"))
			{	// then, it must be an existing BioPAX object URI (seems, the user hopes so)
				uris.add(identifier);
				//also, if it's a canonical Identifiers.org URI, -
				if(identifier.startsWith("http://identifier.org/")) {
					//extract the id from the URI
					String id = CPathUtils.idfromNormalizedUri(identifier);
					if(!q.toString().contains(id)) q.append("xrefid:\"").append(id).append("\" ");
				} else //no id-mapping required
					continue; //go to next identifier
			}
		 	// id-mapping step is not required anymore (new full-text index links lots of IDs to BioPAX objects)
		}

		if (q.length() > 0) {
			//find existing URIs by ids using full-text search (collect all hits, because the query is very specific.
			final String query = q.toString().trim();

// from 2015/12/17 6.2.0-SNAPSHOT, we do not have to collect (URIs of) Xrefs anymore...
//			int page = 0; // will use search pagination
//			SearchResponse resp = (SearchResponse) search(query, page, Xref.class, null, null);
//			log.debug("findUrisByIds, hits: " + resp.getNumHits());
//			while (!resp.isEmpty()) {
//				log.debug("Retrieving xref search results, page #" + page);
//				for (SearchHit h : resp.getSearchHit()) {
//					if("UnificationXref".equalsIgnoreCase(h.getBiopaxClass())
//							|| "RelationshipXref".equalsIgnoreCase(h.getBiopaxClass())) {
//						//exclude some RX types if the rel.type is set
//						if("RelationshipXref".equalsIgnoreCase(h.getBiopaxClass())) {
//							RelationshipXref rx = null;
//							rx = (RelationshipXref) paxtoolsModel.getByID(h.getUri());
//							//which RX types to use/keep...
//							//('identity', etc. RX types were created when building the Warehouse and in the merging)
//							if(rx.getRelationshipType()==null
//									|| rx.getRelationshipType().getTerm().contains("identity")
//									|| rx.getRelationshipType().getTerm().contains("secondary-ac"))
//								uris.add(h.getUri());
//
//						} else
//							uris.add(h.getUri());
//					}
//				}
//				// go next page
//				resp = (SearchResponse) search(query, ++page, Xref.class, null, null);
//			}

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
			.getResource("file:" + CPathSettings.getInstance().blacklistFile());
		
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
			log.warn("loadBlacklist, " + CPathSettings.getInstance().blacklistFile() 
				+ " is not found");
		}
	}

	
	private boolean paxtoolsModelReady() {
		return paxtoolsModel != null;
	}	
	
	
	@Override
	public Set<String> map(String fromDb, String fromId, String toDb) {
    	Assert.hasText(fromId);
    	Assert.hasText(toDb);
    	
    	List<Mapping> maps;    	
    	if(fromDb == null || fromDb.isEmpty()) {
			// map without specifying a src db; avoid no-prefix numerical IDs
			// (it's risky if no-prefix integer IDs exist, such as pubchem cid, sid;
			// though, for biopolymers we could expect that only NCBI Gene ID is used for id-mapping...)
			if(!fromId.matches("^\\d+$"))
    			maps = mappingsRepository.findBySrcIdAndDestIgnoreCase(fromId, toDb);
			else
				return Collections.emptySet();
    	} else {
    		//if possible, use a "canonical" id instead isoform, version, kegg gene...
    		// (e.g., uniprot.isoform, P04150-2 pair becomes uniprot, P04150)
    		String id = fromId;
    		String db = fromDb;
    		
    		//normalize the name
    		try {
				String stdDb = MiriamLink.getName(db);
				if(stdDb != null) // be safe
					db = stdDb.toUpperCase();
			} catch (IllegalArgumentException e) {
			}		
    		
    		if(db.toUpperCase().startsWith("UNIPROT") || db.equalsIgnoreCase("SWISSPROT")) {
    			//e.g., 'uniprot isoform', 'UNIPROT', 'UniProt Knowledgebase', or 'SwissProt'
    			db = "UNIPROT";
				//always use UniProt ID instead of the isoform ID for mapping
				int idx = id.lastIndexOf('-');
				if(idx > 0)	id = id.substring(0, idx);
    		}
    		else if(db.equalsIgnoreCase("refseq")) {
    			//strip, e.g., refseq:NP_012345.2 to refseq:NP_012345
    			int idx = id.lastIndexOf('.');
    			if(idx > 0)
    				id = id.substring(0, idx);
    		} 
    		else if(db.toLowerCase().startsWith("kegg") && id.matches(":\\d+$")) {
    			int idx = id.lastIndexOf(':');
    			if(idx > 0) {
    				id = id.substring(idx + 1); //it's NCBI Gene ID;
    				db = "NCBI GENE";
    			}
    		}
    		else if(db.equalsIgnoreCase("GENEID") || db.equalsIgnoreCase("ENTREZ GENE")) {
    			db = "NCBI GENE";
    		} else if(db.toUpperCase().contains("PUBCHEM") && 
    				(db.toUpperCase().contains("SUBSTANCE") || db.toUpperCase().contains("SID"))) {
    			db = "PUBCHEM-SUBSTANCE";
    		} else if(db.toUpperCase().contains("PUBCHEM") && 
    				(db.toUpperCase().contains("COMPOUND") || db.toUpperCase().contains("CID"))) {
    			db = "PUBCHEM-COMPOUND";
    		}
    		
    		maps = mappingsRepository.findBySrcIgnoreCaseAndSrcIdAndDestIgnoreCase(db, id, toDb);
    	}
    	
    	Set<String> results = new TreeSet<String>();   	
    	for(Mapping m : maps) {
    		results.add(m.getDestId());
    	}
    	
		return results;
	}


	@Override
	public Set<String> map(String identifier) {
		if(identifier.startsWith("http://"))
			throw new AssertionError("URI is not allowed here; use ID");
		
		if(identifier.toUpperCase().startsWith("CHEBI:")) {
			// chebi -> to primary chebi id
			return map("CHEBI", identifier, "CHEBI");
		} else if(identifier.length() == 25 || identifier.length() == 27) {
			// InChIKey identifier (25 or 27 chars long) -> to primary chebi id
			return map(null, identifier, "CHEBI"); //null - for looking in InChIKey, names, etc.
		} else if(identifier.toUpperCase().startsWith("CID:")) {
			// - a hack to tell PubChem ID from NCBI Gene ID in graph queries
			return map("PubChem-compound", identifier.substring(4), "CHEBI");
		} else if(identifier.toUpperCase().startsWith("SID:")) {
			// - a hack to tell PubChem ID from NCBI Gene ID in graph queries
			return map("PubChem-substance", identifier.substring(4), "CHEBI");
		} else if(identifier.toUpperCase().startsWith("PUBCHEM:")) { 
			// - a hack to tell PubChem ID from NCBI Gene ID in graph queries
			return map("PubChem-compound", identifier.substring(8), "CHEBI");	
		} else {
			// gene/protein name, id, etc. -> to primary uniprot AC
			Set<String> ret = new TreeSet<String>();
			ret.addAll(map(null, identifier, "UNIPROT"));
			if(ret.isEmpty()) //ChEMBL, DrugBank, chem. names, etc to ChEBI
				ret.addAll(map(null, identifier, "CHEBI"));
			return ret;
		}
	}


	@Override
	public void saveIfUnique(Mapping mapping) {
		if(!exists(mapping)) {
			mappingsRepository.save(mapping);
		} else {
			//ignore
		}
	}

	private boolean exists(Mapping m) {
		return mappingsRepository
			.findBySrcIgnoreCaseAndSrcIdAndDestIgnoreCaseAndDestId(
					m.getSrc(), m.getSrcId(), m.getDest(), m.getDestId()) 
						!= null;
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
	public LogEntity count(String date, LogEvent event, String ipAddr) 
	{		
		// find or create a record, count+1
		LogEntity t = null;
		try {
			t = (LogEntity) logEntitiesRepository
				.findByEventNameIgnoreCaseAndAddrAndDate(event.getName(), ipAddr, date);
		} catch (DataAccessException e) {
			log.error("count(), findByEventNameIgnoreCaseAndAddrAndDate " +
				"failed to update for event: " + event.getName() + 
				", IP: " + ipAddr + ", date: " + date, e);
		}
		
		if(t == null) {			
			t = new LogEntity(date, event, ipAddr);
		}
		
		t.setCount(t.getCount() + 1);
		
		log.info(t.toString());
		
		return logEntitiesRepository.save(t);
	}

	@Override
	public LogEntity update(String date, LogEvent event, String ipAddr, Long newCount) 
	{		
		// find or create a record
		LogEntity t = null;
		try {
			t = (LogEntity) logEntitiesRepository
				.findByEventNameIgnoreCaseAndAddrAndDate(event.getName(), ipAddr, date);
		} catch (DataAccessException e) {
			log.error("count(), findByEventNameIgnoreCaseAndAddrAndDate " +
				"failed to update for event: " + event.getName() + 
				", IP: " + ipAddr + ", date: " + date, e);
		}
		
		if(t == null) {			
			t = new LogEntity(date, event, ipAddr);
		}
		
		t.setCount(newCount);
		
		return logEntitiesRepository.save(t);
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
	public Metadata init(Metadata metadata) { 
		
    	metadata.cleanupOutputDir();
    	metadata.setNumInteractions(null);
    	metadata.setNumPathways(null);
    	metadata.setNumPhysicalEntities(null);   	
    	metadata.getContent().clear();
    	
		return save(metadata);
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
    	metadata.cleanupOutputDir();
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


	public synchronized boolean ready() {
		return (metadataRepository != null 
				&& mappingsRepository != null
				&& searcher != null
				&& paxtoolsModelReady());
	}


	public void log(String fileName, String ipAddr) {
		log(logEventsFromFilename(fileName), ipAddr);
	}
	
	/**
	 * Creates a list of events to update counts for -
	 * name, format, provider - from the data file 
	 * (in the batch downloads or another directory).
	 * 
	 * @param filename see {@link CPathSettings#biopaxExportFileName(String)} for how it's created.
	 * @return
	 */
	public Set<LogEvent> logEventsFromFilename(String filename) {
		Set<LogEvent> set = new HashSet<LogEvent>();
		final CPathSettings cpath2 = CPathSettings.getInstance();
		
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
		if(!CPathSettings.getInstance().isAdminEnabled())
			throw new IllegalStateException("Admin mode must be enabled to run index().");

		if(paxtoolsModel==null)
			paxtoolsModel = CPathUtils.loadMainBiopaxModel();
		// set for this service

		log.info("Associating more identifies with BioPAX model objects' using child elements' xrefs and id-mapping...");
		addOtherIdsAsAnnotations(4);

		//Build the full-text (lucene) index
		SearchEngine searchEngine = new SearchEngine(getModel(), CPathSettings.getInstance().indexDir());
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

		log.info("Generating the blacklist.txt...");
		//Generates cpath2 graph query blacklist file (to exclude ubiquitous small molecules, like ATP)
		BlacklistGenerator2 gen = new BlacklistGenerator2();
		Blacklist blacklist = gen.generateBlacklist(getModel());
		// Write all the blacklisted ids to the output
		if(blacklist != null)
			blacklist.write(new FileOutputStream(CPathSettings.getInstance().blacklistFile()));

		log.info("index(), all done.");
	}

	private void addOtherIdsAsAnnotations(int depth) {
		for(BioPAXElement bpe : getModel().getObjects()) {
			Set<String> ids = new HashSet<String>();

			//for Entity or ER, also collect IDs from child UX/RXs and map to other IDs (use idMapping)
			if (bpe instanceof Entity || bpe instanceof EntityReference || bpe instanceof PathwayStep) {
				// Collect relevant bio IDs from this and child elements of particular type: ER, Gene, PE;
				// add other IDs via id-mapping.
				Set<BioPAXElement> children = new Fetcher(
					SimpleEditorMap.get(paxtoolsModel.getLevel()), Fetcher.nextStepFilter,
						//exclude unwanted child objects, such as CVs and other utility classes
						new org.biopax.paxtools.util.Filter<PropertyEditor>() {
							@Override
							public boolean filter(PropertyEditor ed) {
								return EntityReference.class.isAssignableFrom(ed.getRange())
										|| Gene.class.isAssignableFrom(ed.getRange())
										|| PhysicalEntity.class.isAssignableFrom(ed.getRange());
							}
						}).fetch(bpe, depth);

				//include this object itself if it's about a bio macromolecule of chemical/metabolite
				if (bpe instanceof PhysicalEntity || bpe instanceof EntityReference || bpe instanceof Gene)
					children.add(bpe);

				for(BioPAXElement child : children) {
					//as the fetcher uses specific filters, every element can be safely cast to XReferrable
					XReferrable el = (XReferrable) child;
					for(Xref x : el.getXref()) {
						if (!(x instanceof PublicationXref) && x.getId()!=null && x.getDb()!=null) {
							//btw, we should've removed or auto-fix up all null db/id xrefs already (in the premerge)
							ids.add(x.getId());

							//add more IDs using "reverse" mapping and canonical uniprot/chebi xrefs (those added by Merger)
							List<Mapping> mappings = findAllThatMapTo(x.getDb(), x.getId());
							if (mappings != null)
								for (Mapping mapping : mappings)
									ids.add(mapping.getSrcId());
						}
					}
				}
			} else if(bpe instanceof UnificationXref || bpe instanceof RelationshipXref) {
				Xref x = (Xref) bpe;
				ids.add(x.getId());
				//add more IDs via mapping
				List<Mapping> mappings = findAllThatMapTo(x.getDb(), x.getId());
				if (mappings != null)
					for (Mapping mapping : mappings)
						ids.add(mapping.getSrcId());

			}

			if(!ids.isEmpty())
				bpe.getAnnotations().put(SearchEngine.FIELD_XREFID, ids);
		}
	}

	private List<Mapping> findAllThatMapTo(String db, String id) {
		//add more IDs using "reverse" mapping and canonical uniprot/chebi xrefs (those added by Merger)
		List<Mapping> mappings = null;
		if ("chebi".equalsIgnoreCase(db)) {
			//find other IDs that map to the ChEBI ID
			mappings = mappingsRepository.findByDestIgnoreCaseAndDestId("CHEBI", id);
		} else if ("uniprot knowledgebase".equalsIgnoreCase(db)) {
			//find other IDs that map to the UniProt AC
			mappings = mappingsRepository.findByDestIgnoreCaseAndDestId("UNIPROT", id);
		}
		return mappings;
	}
}

