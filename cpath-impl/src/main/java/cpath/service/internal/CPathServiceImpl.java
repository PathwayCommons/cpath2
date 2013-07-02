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


package cpath.service.internal;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

import org.biopax.paxtools.controller.Cloner;
import org.biopax.paxtools.controller.Completer;
import org.biopax.paxtools.controller.Fetcher;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.Pathway;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.paxtools.query.QueryExecuter;
import org.biopax.paxtools.query.algorithm.Direction;
import org.biopax.paxtools.query.algorithm.LimitType;
import org.biopax.paxtools.query.wrapperL3.DataSourceFilter;
import org.biopax.paxtools.query.wrapperL3.Filter;
import org.biopax.paxtools.query.wrapperL3.OrganismFilter;
import org.biopax.paxtools.query.wrapperL3.UbiqueFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import cpath.config.CPathSettings;
import cpath.dao.Analysis;
import cpath.dao.MetadataDAO;
import cpath.dao.PaxtoolsDAO;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.ServiceResponse;
import cpath.service.jaxb.TraverseResponse;
import cpath.service.CPathService;
import cpath.service.ErrorResponse;
import cpath.service.OutputFormat;
import static cpath.config.CPathSettings.PROVIDER_NAME;
import static cpath.config.CPathSettings.downloadsDir;
import static cpath.config.CPathSettings.property;
import static cpath.service.Status.*;

import cpath.warehouse.beans.Mapping;

/**
 * Service tier class - to uniformly access 
 * persisted BioPAX model (DAO) from console 
 * and web service controllers. This can be 
 * configured (instantiated) to access any cpath2
 * persistent BioPAX data storage (PaxtoolsDAO), 
 * i.e., - either the "main" cpath2 db or a Warehouse one (proteins or molecules)
 * 
 * @author rodche
 */
@Service
public class CPathServiceImpl implements CPathService {
	private static final Logger log = LoggerFactory.getLogger(CPathServiceImpl.class);
	
	private PaxtoolsDAO mainDAO;
	
	private MetadataDAO metadataDAO;
	
	private SimpleIOHandler simpleIO;
	
	private Cloner cloner;
//	private Completer completer;
	
	//init. on first access to getBlacklist(); so do not use it directly
	private Set<String> blacklist; 
	
	//init. on first access when proxy model mode is enabled (so do not use the var. directly!)
	private Model proxyModel;
    
	//lazy initialize the following three fields on first request (prevents web server startup timeout)
  	//only includes pathway data providers configured/created from the cpath2 metadata configuration (not all provenances...)
  	private volatile SearchResponse topPathways;

    // this is probably required for the ehcache to work
	public CPathServiceImpl() {
	}

	
    /**
     * Constructor.
     */
	public CPathServiceImpl(PaxtoolsDAO mainDAO, MetadataDAO metadataDAO) 
	{
		this.mainDAO = mainDAO;
		this.metadataDAO = metadataDAO;
		this.simpleIO = new SimpleIOHandler(BioPAXLevel.L3);
		this.simpleIO.mergeDuplicates(true);
		this.cloner = new Cloner(this.simpleIO.getEditorMap(), this.simpleIO.getFactory());
//		this.completer = new Completer(simpleIO.getEditorMap());
	}
	
	
	@Override
	@Cacheable(value = "findElementsCache")
	public ServiceResponse search(String queryStr, 
			int page, Class<? extends BioPAXElement> biopaxClass, String[] dsources, String[] organisms) 
	{
		ServiceResponse serviceResponse;
		
		try {
			// do search
			SearchResponse hits = mainDAO.search(queryStr, page, biopaxClass, dsources, organisms);
			
			if(hits.isEmpty()) {//no hits
				hits = new SearchResponse();
				hits.setPageNo(page);
			}
			
			hits.setComment("Search '" + queryStr  + "' in " + 
				((biopaxClass == null) ? "all types" : biopaxClass.getSimpleName()) 
				+ "; ds: " + Arrays.toString(dsources)+ "; org.: " + Arrays.toString(organisms));
			
			serviceResponse = hits;
			
		} catch (Exception e) {
			serviceResponse = new ErrorResponse(INTERNAL_ERROR, e);
			log.error("search: ", e);
		}
		
		return serviceResponse;
	}
		

	@Override
	@Cacheable(value = "elementByIdCache")
	public ServiceResponse fetch(final OutputFormat format, final String... uris) {
		if (uris.length == 0)
			return new ErrorResponse(NO_RESULTS_FOUND,
					"No URIs were specified for the query");
		
		final String[] mappedUris = findUrisByIds(uris);
		
		final ServiceResponse[] callback = new ServiceResponse[1];
		
		// extract/convert a sub-model within a hibernate read-only transaction 
		Analysis analysis = new Analysis() {			
			@Override
			public void execute(Model model) {
				try {
					Set<BioPAXElement> elements = urisToBpes(model, mappedUris);
					elements = (new Completer(simpleIO.getEditorMap())).complete(elements, model);
					Model m = cloner.clone(model, elements);
					callback[0] = (new BiopaxConverter(getBlacklist()))
						.convert(m, format, true);
				} catch (Exception e) {
					callback[0] = new ErrorResponse(INTERNAL_ERROR, e);
				}
			}
		};
		
		if(isProxyModel())
			analysis.execute(proxyModel);
		else
			mainDAO.runReadOnly(analysis);
		
		return callback[0];
    }


	private Filter[] createFilters(String[] organisms, String[] datasources) {
		ArrayList<Filter> filters = new ArrayList<Filter>();
		
		if(getBlacklist() != null && !getBlacklist().isEmpty())
			filters.add(new UbiqueFilter(getBlacklist()));
		
		if(organisms != null && organisms.length > 0)
			filters.add(new OrganismFilter(organisms));
		
		if(datasources != null && datasources.length > 0)
			filters.add(new DataSourceFilter(datasources));
		
		return filters.toArray(new Filter[]{});
	}

	
	@Override
	@Cacheable(value = "getNeighborhoodCache")
	public ServiceResponse getNeighborhood(final OutputFormat format, 
		final String[] sources, final Integer limit, Direction direction, 
		final String[] organisms, final String[] datasources)
	{
		final String[] src = findUrisByIds(sources);

		if(direction == null) {
			direction = Direction.BOTHSTREAM;	
		}
		
		final ServiceResponse[] callback = new ServiceResponse[1];
		
		// execute the paxtools graph query in a read-only db transaction
		final Direction dir = direction;		
		Analysis analysis = new Analysis() {			
			@Override
			public void execute(Model model) {
				try {
					// init source elements
					Set<BioPAXElement> elements = urisToBpes(model, src);
					// Execute the query, get result elements
					elements = QueryExecuter.runNeighborhood(elements, model,
							limit, dir, createFilters(organisms, datasources));
					// auto-complete (gets a reasonable size sub-model)
					elements = (new Completer(simpleIO.getEditorMap())).complete(elements, model);
					Model m = cloner.clone(model, elements);
					callback[0] = (new BiopaxConverter(getBlacklist()))
							.convert(m, format, true);
				} catch (Exception e) {
					callback[0] = new ErrorResponse(INTERNAL_ERROR, e);
				}
			}
		};
		
		if(isProxyModel()) 
			analysis.execute(proxyModel);
		else
			mainDAO.runReadOnly(analysis);
		
		return callback[0];
	}

	
	@Override
	@Cacheable(value = "getPathsBetweenCache")
	public ServiceResponse getPathsBetween(final OutputFormat format, 
			final String[] sources, final Integer limit, 
			final String[] organisms, final String[] datasources)
	{
		final String[] src = findUrisByIds(sources);

		final ServiceResponse[] callback = new ServiceResponse[1];
		
		// execute the paxtools graph query in a read-only db transaction
		Analysis analysis = new Analysis() {			
			@Override
			public void execute(Model model) {
				try {
					// init source elements
					Set<BioPAXElement> elements = urisToBpes(model, src);
					// Execute the query, get result elements
					elements = QueryExecuter.runPathsBetween(elements, model, limit,
							createFilters(organisms, datasources));
					// auto-complete (gets a reasonable size sub-model)
					elements = (new Completer(simpleIO.getEditorMap())).complete(elements, model);
					Model m = cloner.clone(model, elements);
					callback[0] = (new BiopaxConverter(getBlacklist()))
							.convert(m, format, true);
				} catch (Exception e) {
					callback[0] = new ErrorResponse(INTERNAL_ERROR, e);
				}
			}
		};
		
		if(isProxyModel()) 
			analysis.execute(proxyModel);
		else
			mainDAO.runReadOnly(analysis);
		
		return callback[0];
	}

	
	@Override
	@Cacheable(value = "getPathsFromToCache")
	public ServiceResponse getPathsFromTo(final OutputFormat format, 
		final String[] sources, final String[] targets, final Integer limit,
		final String[] organisms, final String[] datasources)
	{
		final String[] src = findUrisByIds(sources);
		final String[] tgt = findUrisByIds(targets);

		
		final ServiceResponse[] callback = new ServiceResponse[1];
		
		// execute the paxtools graph query in a read-only db transaction		
		Analysis analysis = new Analysis() {			
			@Override
			public void execute(Model model) {
				try {
					// init source and target elements
					Set<BioPAXElement> source = urisToBpes(model, src);
					Set<BioPAXElement> target = urisToBpes(model, tgt);
					
					// Execute the query
					Set<BioPAXElement> elements = (target==null || target.isEmpty()) 
						? QueryExecuter.runPathsBetween(source, model, limit,
								createFilters(organisms, datasources))
						: QueryExecuter.runPathsFromTo(source, target, 
							model, LimitType.NORMAL, limit,
								createFilters(organisms, datasources));
						
					// auto-complete (gets a reasonable size sub-model)
					elements = (new Completer(simpleIO.getEditorMap())).complete(elements, model);
					Model m = cloner.clone(model, elements);
					callback[0] = (new BiopaxConverter(getBlacklist()))
							.convert(m, format, true);
				} catch (Exception e) {
					callback[0] = new ErrorResponse(INTERNAL_ERROR, e);
				}
			}
		};
		
		if(isProxyModel()) 
			analysis.execute(proxyModel);
		else
			mainDAO.runReadOnly(analysis);
		
		return callback[0];
	}
	

	@Override
	public ServiceResponse getCommonStream(final OutputFormat format, 
		final String[] sources, final Integer limit, Direction direction,
		final String[] organisms, final String[] datasources)
	{
		final String[] src = findUrisByIds(sources);

		if (direction == Direction.BOTHSTREAM) {
			return new ErrorResponse(BAD_REQUEST, 
				"Direction cannot be BOTHSTREAM for the COMMONSTREAM query");
		} else if(direction == null) {
			direction = Direction.DOWNSTREAM;	
		}
					
		final ServiceResponse[] callback = new ServiceResponse[1];
		
		// execute the paxtools graph query in a read-only db transaction
		final Direction dir = direction;
		Analysis analysis = new Analysis() {			
			@Override
			public void execute(Model model) {
				try {
					// init source elements
					Set<BioPAXElement> elements = urisToBpes(model, src);
					// Execute the query, get result elements
					elements = QueryExecuter
						.runCommonStreamWithPOI(elements, model, dir, limit,
							createFilters(organisms, datasources));
					// auto-complete (gets a reasonable size sub-model)
					elements = (new Completer(simpleIO.getEditorMap())).complete(elements, model);
					Model m = cloner.clone(model, elements);
					callback[0] = (new BiopaxConverter(getBlacklist()))
							.convert(m, format, true);
				} catch (Exception e) {
					callback[0] = new ErrorResponse(INTERNAL_ERROR, e);
				}
			}
		};
		
		if(isProxyModel()) 
			analysis.execute(proxyModel);
		else
			mainDAO.runReadOnly(analysis);
		
		return callback[0];
	}

	
	/**
	 * Mapping to BioPAX URIs.
	 *
	 * TODO it does not "understand" RefSeq Versions and UniProt Isoforms 
	 * (one has to submit canonical identifiers, i.e, ones without "-#" or ".#").
	 * 
	 * 
	 * @param identifiers a list of gene names, UniProt, RefSeq, ENS* and NCBI Gene identifiers; or CHEBI and InChIKey.
	 * @return URIs
	 */
	private String[] findUrisByIds(String[] identifiers)
	{
		if (identifiers.length == 0)
			return identifiers;

		Set<String> uris = new TreeSet<String>();

		// id-mapping: get primary IDs where possible; 
		// build a Lucene query string (will be eq. to xrefid:"A" OR xrefid:"B" OR ...)
		final StringBuilder q = new StringBuilder();
		for (String identifier : identifiers) {
			if(identifier.startsWith("http://") || identifier.startsWith("urn:")) {
				// it must be an existing URI; skip id-mapping 
				//TODO could map secondary URIs to primary ones too (generally, users should not guess URIs nor submit non-existing ones to the query)
				uris.add(identifier);
			} else {
				// do gene/protein id-mapping;
				// mapping can be ambiguous, but this is OK for queries (unlike when merging data)
				Set<String> m = map(identifier);
				if (!m.isEmpty()) {
					for(String ac : m) {
						// add to the query string; 
						// quotation marks around the query id are required
						q.append("xrefid:\"").append(ac).append("\" "); 
						log.debug("findUrisByIds, mapped " + identifier + " -> " + ac);
					}
				}
				else {
					q.append("xrefid:\"").append(identifier).append("\" "); 
					log.debug("findUrisByIds, no primary id found (will use 'as is'): " + identifier);
				}				
			}
		}
		
		/* 
		 * find existing Xref URIs by ids using cpath2 full-text search
		 * and pagination; iterate until all hits/pages are read,
		 * because our query is very specific - uses field and class -
		 * we want all hits)
		*/
		if (q.length() > 0) {
			final String query = q.toString();
			log.debug("findUrisByIds, will run: " + query);
			int page = 0; // will use search pagination
			SearchResponse resp = mainDAO.search(query, page, Xref.class, null, null);
			log.debug("findUrisByIds, hits: " + resp.getNumHits());
			while (!resp.isEmpty()) {
				log.debug("Retrieving xref search results, page #" + page);
				for (SearchHit h : resp.getSearchHit())
					uris.add(h.getUri());
				// go next page
				resp = mainDAO.search(query, ++page, Xref.class, null, null);
			}
		}
				
		return uris.toArray(new String[]{});
	}

	
	/*
	 * Detects the identifier type (chemical vs gene/protein) 
	 * and executes corresponding id-mapping method.
	 * 
	 * @param identifier
	 * @return
	 * @throws AssertionError when the identifier is suspected to be a full URI
	 */
	private Set<String> map(String identifier) {
		if(identifier.startsWith("http://"))
			throw new AssertionError("URI is not allowed here");
		
		if(identifier.startsWith("CHEBI:") || identifier.length() == 25 || identifier.length() == 27) {
			// chebi or InChIKey identifier (25 or 27 chars long) -> to primary chebi id
			return metadataDAO.mapIdentifier(identifier, Mapping.Type.CHEBI, null);
		} else if(identifier.startsWith("PUBCHEM:")) { //TODO explain in the web service docs
			// - a hack to tell PubChem ID from NCBI Gene ID in graph queries
			return metadataDAO.mapIdentifier(identifier.replaceFirst("PUBCHEM:", ""), Mapping.Type.CHEBI, null);
		} else {
			// gene/protein name, id, etc. -> to primary uniprot AC
			return metadataDAO.mapIdentifier(identifier, Mapping.Type.UNIPROT, null);
		}
	}

	
	@Override
	@Cacheable(value = "traverseCache")
	public ServiceResponse traverse(String propertyPath, String... sourceUris) {
		try {
			// get results from the DAO
			TraverseResponse results = mainDAO.traverse(propertyPath, sourceUris);
			return results;
		} catch (IllegalArgumentException e) {
			log.error("Failed to init path accessor: ", e);
			return new ErrorResponse(BAD_REQUEST, e.getMessage());
		} catch (Exception e) {
			log.error("Failed. ", e);
			return new ErrorResponse(INTERNAL_ERROR, e);
		}
	}

	
	/**
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
	 * Note:
	 * This method generates the list of top pathways on the first call, 
	 * and then all next calls return the same list (much faster).
	 * (it uses lazy initialization, "double-check idiom", 
	 * to prevent possible server startup timeout)
	 * 
	 */
	@Override
	public SearchResponse topPathways() {
		if(topPathways == null) { //first check (no locking)
			synchronized (this) {
				if(topPathways == null) { //second check (with locking)
					topPathways = new SearchResponse();
					final List<SearchHit> hits = topPathways.getSearchHit(); //empty
					int page = 0; // will use search pagination
					SearchResponse searchResponse = mainDAO.search("*", page, Pathway.class, null, null);
					while(!searchResponse.isEmpty()) {
						log.debug("Retrieving top pathways search results, page #" + page);
						//remove pathways having 'pathway' index field not empty, 
						//i.e., keep only pathways where 'pathway' index field
						// is empty (no controlledOf and pathwayComponentOf values)
						for(SearchHit h : searchResponse.getSearchHit()) {
							//contains only itself
							if(h.getPathway().size() == 1 && h.getPathway().get(0).equals(h.getUri())) 
								hits.add(h); //add to topPathways list
						}
						// go next page
						searchResponse = mainDAO.search("*", ++page, Pathway.class, null, null);
					}
					// final touches...
					topPathways.setNumHits(hits.size());
					//TODO update the following comment if implementation has changed
					topPathways.setComment("Top Pathways (technically, each has empty index " +
							"field 'pathway'; that also means, they are neither components of " +
							"other pathways nor controlled of any process)");
					topPathways.setMaxHitsPerPage(hits.size());
					topPathways.setPageNo(0);
				}
			}
		} 
		
		return topPathways;
	}


	public Set<String> getBlacklist() {
		if(blacklist == null) 
			loadBlacklist();
		
		return blacklist;
	}
	
	public void setBlacklist(Set<String> blacklist) {
		this.blacklist = blacklist;
	}

	
	/*
	 * This utility method prepares the source or target 
	 * object sets for a graph query.
	 * 
	 * @param main source model
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
	
	
	/*
	 * Builds a complete self-integral biopax
	 * sub-model starting from the set of elements
	 * and adding all their children. 
	 * 
	 * @param model
	 * @param elements
	 * @return
	 */
	@Deprecated //we use Cloner instead
	private Model fullSubModel(Model model, Set<BioPAXElement> elements) 
	{
		Model m = model.getLevel().getDefaultFactory().createModel();
		m.setXmlBase(model.getXmlBase());

		@SuppressWarnings("unchecked")
		Fetcher fetcher = new Fetcher(SimpleEditorMap.get(model.getLevel()),
				Fetcher.nextStepFilter); 
		for (BioPAXElement bpe : elements) 
				fetcher.fetch(bpe, m);

		return m;
	}
	
	
	private synchronized void loadBlacklist() 
	{
		this.blacklist = new HashSet<String>();
		
		Resource blacklistResource = new DefaultResourceLoader()
			.getResource("file:" + CPathSettings.blacklistFile());
		
		if(blacklistResource.exists()) {
			
			Scanner scanner;
			try {
				scanner = new Scanner(blacklistResource.getFile());
			} catch (IOException e) {
				throw new RuntimeException("loadBlacklist, failed opening file: " 
					+ blacklistResource.getFilename(), e);
			}
			
			while(scanner.hasNextLine())
				blacklist.add(scanner.nextLine().trim());
			scanner.close();
			
			if(log.isInfoEnabled())
				log.info("loadBlacklist, loaded " + blacklist.size()
				+ " URIs for a (graph queries) 'blacklist' resource: " 
				+ blacklistResource.getDescription());
		} else {
			log.warn("loadBlacklist, " + CPathSettings.blacklistFile() 
				+ " is not found");
		}
	}
	
	
	/*
	 * Exports all from mainDAO into the in-memory proxy model
	 * (only once, on demand).
	 */
	private synchronized void initProxyModelOnce() {
		//first check (no locking)
		if(proxyModel == null && CPathSettings.isProxyModelEnabled()) { 
			//prevent queries from using the proxy until it's ready
			CPathSettings.getInstance().setProxyModelEnabled(false);
					
			//fork the model loading... (this takes quite a while)
			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.execute(
				new Runnable() {
				@Override
				public void run() {
					log.info("initProxyModel: loading entire biopax Model...");
//					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//					mainDAO.exportModel(outputStream); //this takes HOURS...
//					log.info("initProxyModel: Model was successfully written to RDF/XML stream...");
					try {
//						proxyModel = simpleIO.convertFromOWL(new ByteArrayInputStream(
//							outputStream.toString().getBytes("UTF-8")));
//						outputStream = null;
						
						final String archive = downloadsDir() 
				        	+ File.separator + property(PROVIDER_NAME) 
				        		+ " " + "all.BIOPAX.owl.gz";
						proxyModel = simpleIO.convertFromOWL(new GZIPInputStream(new FileInputStream(archive)));
												
						//allow queries use the proxy
						CPathSettings.getInstance().setProxyModelEnabled(true);
						log.info("initProxyModel: in-memory proxy Model is now ready for queries");
					} 
					catch (IOException e) {
//					catch (UnsupportedEncodingException e) {
						log.error("initProxyModel: failed, proxy model is now disabled.", e);
					}
				}
			});
			executor.shutdown();
			//won't wait till it's done!
		}
	}
	

	/**
	 * @return true iif the proxy mode is on and the in-memory model ready (initialized)
	 */
	private boolean isProxyModel() {
		if(proxyModel == null && CPathSettings.isProxyModelEnabled()) {
			initProxyModelOnce(); //it runs in a separate thread and temporarily disables the proxy model mode
			//the proxy is being built but not ready yet
			log.debug("isProxyModel: proxy mode was enabled, model not initialized yet (initializing in the background)");
			return false;
		}
		else if(proxyModel == null && !CPathSettings.isProxyModelEnabled()) {
			log.debug("isProxyModel: proxy mode is disabled, model not initialized");
			return false;
		}
		else if(proxyModel != null && CPathSettings.isProxyModelEnabled()) {
			log.debug("isProxyModel: true!");
			return true;
		}
		else {//proxyModel != null && !CPathSettings.isProxyModelEnabled()
			log.debug("isProxyModel: proxy mode is currently off, but the in-memory model is ready");
			return false;
		}
	}	
}

