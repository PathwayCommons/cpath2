package cpath.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.biopax.paxtools.controller.*;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.level3.Process;
import org.biopax.paxtools.util.ClassFilterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.config.CPathSettings;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;

/**
 * A full-text searcher/indexer for BioPAX L3 models.
 *
 * Only Entity and EntityReference BioPAX types get indexed (since 12/2015).
 *
 * @author rodche
 */
public class SearchEngine implements Indexer, Searcher {
	private static final Logger LOG = LoggerFactory.getLogger(SearchEngine.class);
	
	// search fields
	public static final String FIELD_URI = "uri";
	public static final String FIELD_KEYWORD = "keyword"; //anything, e.g., names, terms, comments, incl. - from child elements 
	public static final String FIELD_NAME = "name"; // standardName, displayName, other names
	public static final String FIELD_XREFID = "xrefid"; //xref.id
	public static final String FIELD_PATHWAY = "pathway"; //pathways and parent pathways to be inferred from entire biopax model
	public static final String FIELD_SIZE = "size"; //since cPath2 v7.., size == numparticipants + numprocesses
	public static final String FIELD_N_PARTICIPANTS = "participants"; // num. of PEs or Genes in a process or Complex
	public static final String FIELD_N_PROCESSES = "processes"; // is same as 'size' used to be before cPath2 v7

	// Full-text search/filter fields (case sensitive) -
	//index organism names, cell/tissue type (term), taxonomy id, but only store BioSource URIs	
	public static final String FIELD_ORGANISM = "organism";
	//index data source names, but only URIs are stored in the index
	public static final String FIELD_DATASOURCE = "datasource";
	public static final String FIELD_TYPE = "type";
	
	//Default fields to use with the MultiFieldQueryParser;
	//one can still search in other fields directly, like - pathway:some_keywords datasource:"pid"
	public final static String[] DEFAULT_FIELDS = //to use with the MultiFieldQueryParser
	{
			FIELD_KEYWORD, //data type properties (name, id, term, comment) of this and child elements;
			FIELD_XREFID,
			FIELD_NAME,
	};

	private final Model model;
	private int maxHitsPerPage;
	private final Analyzer analyzer;
	private final Path indexFile;
	private SearcherManager searcherManager;

	public final static int DEFAULT_MAX_HITS_PER_PAGE = 100;

	/**
	 * Constructor.
	 *
	 * @param model the BioPAX Model to index or search
	 * @param indexLocation index directory location
	 */
	public SearchEngine(Model model, String indexLocation) {
		this.model = model;
		this.indexFile = Paths.get(indexLocation);
		initSearcherManager();
		this.maxHitsPerPage = DEFAULT_MAX_HITS_PER_PAGE;

		//refs issue #269
		Map<String,Analyzer> analyzersPerField = new HashedMap();
		analyzersPerField.put(FIELD_NAME, new KeywordAnalyzer());
		analyzersPerField.put(FIELD_XREFID, new KeywordAnalyzer());
		this.analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), analyzersPerField);
	}

	private void initSearcherManager() {
		try {
			if(Files.exists(indexFile))
				this.searcherManager = 
					new SearcherManager(MMapDirectory.open(indexFile), new SearcherFactory());
			else 
				LOG.info(indexFile + " does not exist.");
		} catch (IOException e) {
			LOG.warn("Could not create a searcher: " + e);
		}
	}
	
	public void setMaxHitsPerPage(int maxHitsPerPage) {
		this.maxHitsPerPage = maxHitsPerPage;
	}

	/**
	 * The max no. hits to return per results page (pagination).
	 * @return
	 */
	public int getMaxHitsPerPage() {
		return maxHitsPerPage;
	}

	public SearchResponse search(String query, int page,
			Class<? extends BioPAXElement> filterByType, String[] datasources,
			String[] organisms) 
	{
		SearchResponse response = null;
		
		LOG.debug("search: '" + query + "', page: " + page
			+ ", filterBy: " + ((filterByType!=null)?filterByType.getSimpleName():"N/A")
			+ "; extra filters: ds in (" + Arrays.toString(datasources)
			+ "), org. in (" + Arrays.toString(organisms) + ")");
		
		IndexSearcher searcher = null;
	
		try {	
			QueryParser queryParser = new MultiFieldQueryParser(DEFAULT_FIELDS, analyzer);
			queryParser.setAllowLeadingWildcard(true);//we want leading wildcards enabled (e.g. *sulin)
//			queryParser.setAutoGeneratePhraseQueries(false); //TODO: try it
			searcher = searcherManager.acquire();
			
			//find and transform top docs to search hits (beans), considering pagination...
			if(!query.trim().equals("*")) { //if not "*" query, which is not supported out-of-the-box, then
				//create the lucene query
				Query userQuery = queryParser.parse(query);
				//do NOT rewrite (Lucene 4.1), or scoring/highlighting won't work for wildcard queries...
				//luceneQuery = searcher.rewrite(luceneQuery);
				LOG.debug("parsed lucene query is " + userQuery.getClass().getSimpleName());
				//create filter: type AND (d OR d...) AND (o OR o...)
				Query filter = createFilter(filterByType, datasources, organisms);
				//final query with filter
				Query q = (filter!=null)
					? new BooleanQuery.Builder().add(userQuery,Occur.MUST).add(filter,Occur.FILTER).build()
						: userQuery;
				//get the first page of top hits
				TopDocs topDocs = searcher.search(q, maxHitsPerPage);

				//get the required hits page if page>0
				if(page>0) {
					TopScoreDocCollector collector = TopScoreDocCollector.create(maxHitsPerPage*(page+1));
					searcher.search(q, collector);
					topDocs = collector.topDocs(page * maxHitsPerPage, maxHitsPerPage);
				}
				
				//transform docs to hits (optionally use a highlighter, e.g., if debugging...)
				response = transform(userQuery, searcher, topDocs);
	
			} else { //find ALL objects of a particular BioPAX class (+ filters by organism, datasource)
				if(filterByType==null) 
					filterByType = Level3Element.class;

				//replace q="*" with a search for the class or its sub-class name in the TYPE field
				BooleanQuery.Builder starQuery = new BooleanQuery.Builder();
				for(Class<? extends BioPAXElement> subType : SimpleEditorMap.L3.getKnownSubClassesOf(filterByType)) {
					starQuery.add(new TermQuery(new Term(FIELD_TYPE, subType.getSimpleName().toLowerCase())), Occur.SHOULD);
				}
				Query filter = createFilter(null, datasources, organisms);
				//combine star and filter queries into one special boolean
				Query q = (filter!=null)
						? new BooleanQuery.Builder().add(starQuery.build(),Occur.MUST).add(filter,Occur.FILTER).build()
							: starQuery.build();
				//get the first page of top hits
				TopDocs topDocs = searcher.search(q, maxHitsPerPage);
				//get the required hits page if page>0
				if(page>0) {
					TopScoreDocCollector collector = TopScoreDocCollector.create(maxHitsPerPage*(page+1));
					searcher.search(q, collector);
					topDocs = collector.topDocs(page * maxHitsPerPage, maxHitsPerPage);	
				}
				
				//convert
				response = transform(q, searcher, topDocs);
			}
		} catch (ParseException e) {
			throw new RuntimeException("getTopDocs: failed to parse the search query: " + e);
		} catch (IOException e) {
			throw new RuntimeException("getTopDocs: failed: " + e);
		} finally {
			try {
				if(searcher!=null) {
					searcherManager.release(searcher);
				}
			} catch (IOException e) {}	
		}
	
		response.setPageNo(page);
		
		return response;
	}

	
	// Transform Lucene docs to hits (xml/java beans)
	private SearchResponse transform(Query query, IndexSearcher searcher, TopDocs topDocs) throws IOException
	{	
		if(topDocs == null)
			throw new IllegalArgumentException("topDocs is null");
		
		SearchResponse response = new SearchResponse();
		response.setMaxHitsPerPage(maxHitsPerPage);
		response.setNumHits(topDocs.totalHits);	
		List<SearchHit> hits = response.getSearchHit();//empty list
		assert hits!=null && hits.isEmpty();
		LOG.debug("transform, no. TopDocs to process:" + topDocs.scoreDocs.length);
		for(ScoreDoc scoreDoc : topDocs.scoreDocs) {			
			SearchHit hit = new SearchHit();
			Document doc = searcher.doc(scoreDoc.doc);
			String uri = doc.get(FIELD_URI);
			BioPAXElement bpe = model.getByID(uri);
			
			// use a highlighter (get matching fragments)
			if (CPathSettings.getInstance().isDebugEnabled()) {
				// to use a Highlighter, store.YES must be enabled for 'keyword' field
				QueryScorer scorer = new QueryScorer(query, FIELD_KEYWORD);
				//the following fixes scoring/highlighting for all-field wildcard (like q=insulin*)
				//but not for term/prefix queries (q=name:insulin*, q=pathway:brca2)_.
				scorer.setExpandMultiTermQuery(true);
//TODO use PostingsHighlighter once it's stable;
//TODO see http://lucene.apache.org/core/6_4_1/highlighter/org/apache/lucene/search/postingshighlight/PostingsHighlighter.html
				SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<span class='hitHL'>", "</span>");
				Highlighter highlighter = new Highlighter(formatter, scorer);
				highlighter.setTextFragmenter(new SimpleSpanFragmenter(scorer, 80));
				final String text = StringUtils.join(ModelUtils.getKeywords(bpe,2, keywordsFilter), " ");
				try {
					TokenStream tokenStream = analyzer.tokenStream("", new StringReader(text));
					String res = highlighter.getBestFragments(tokenStream, text, 7, "...");
					if(res != null && !res.isEmpty())
						hit.setExcerpt(res);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				String excerpt = hit.getExcerpt();
				if(excerpt == null) {excerpt = "";}
				hit.setExcerpt(excerpt + " -SCORE- " + scoreDoc.score +
						" -EXPLANATION- " + searcher.explain(query, scoreDoc.doc));
			}
			
			hit.setUri(uri);
			hit.setBiopaxClass(bpe.getModelInterface().getSimpleName());
			
			// add standard and display names if any -
			if (bpe instanceof Named) {
				Named named = (Named) bpe;
				String std = named.getStandardName();
				if (std != null)
					hit.setName(std);
				else
					hit.setName(named.getDisplayName());
				
				// a hack for BioSource (store more info)
				if(bpe instanceof BioSource) {
					for(String name : named.getName())
						hit.getOrganism().add(name);
					String txid = getTaxonId((BioSource)named);
					if(txid != null)
						hit.getOrganism().add(txid);
				}
				
				// a hack for Provenance: save/return other names 
				// (to be used as filter by data source values)
				if(bpe instanceof Provenance) {
					for(String name : named.getName())
						hit.getDataSource().add(name);
				}	
			}
						
			// extract organisms (URI only) 
			if(doc.get(FIELD_ORGANISM) != null) {
				Set<String> uniqueVals = new TreeSet<>();
				for(String o : doc.getValues(FIELD_ORGANISM)) {
					//note: only URIS are stored in the index					
					uniqueVals.add(o);
				}
				hit.getOrganism().addAll(uniqueVals);
			}
			
			// extract values form the index
			if(doc.get(FIELD_DATASOURCE) != null) {
				Set<String> uniqueVals = new TreeSet<>();
				for(String d : doc.getValues(FIELD_DATASOURCE)) {
					//note: only URIS are stored in the index
					uniqueVals.add(d);
				}
				hit.getDataSource().addAll(uniqueVals);
			}	
			
			// extract only (parent) pathway URIs
			//only URIs were stored "as is" (names, ids were indexed but not stored in the index doc.)
			if(doc.get(FIELD_PATHWAY) != null) {
				Set<String> uniqueVals = new TreeSet<>();
				for(String d : doc.getValues(FIELD_PATHWAY)) {
					uniqueVals.add(d);
				}
				uniqueVals.remove(uri); //exclude itself
				hit.getPathway().addAll(uniqueVals);
			}
			
			//no. processes, participants in the sub-network
			if(doc.get(FIELD_SIZE)!=null)
				hit.setSize(Integer.valueOf(doc.get(FIELD_SIZE)));
			if(doc.get(FIELD_N_PROCESSES)!=null)
				hit.setNumProcesses(Integer.valueOf(doc.get(FIELD_N_PROCESSES))); //TODO: try w/o Integer.valueOf
			if(doc.get(FIELD_N_PARTICIPANTS)!=null)
				hit.setNumParticipants(Integer.valueOf(doc.get(FIELD_N_PARTICIPANTS)));

			hits.add(hit);
		}
				
		//add the Provenance's standardName(s) to the search response
		if(!hits.isEmpty()) {
			for(String puri : response.provenanceUris()) {
				Provenance p = (Provenance) model.getByID(puri);
				response.getProviders()
					.add((p.getStandardName()!=null)?p.getStandardName():p.getDisplayName());
			}
		}
		
		return response;
	}

	public static final org.biopax.paxtools.util.Filter<DataPropertyEditor> keywordsFilter = (editor) -> {
		final String prop = editor.getProperty();
		//to include in the index, as keywords, only the following properties
		// (basically, to exclude float type properties, embedded xml, db names, etc.):
		return (prop.equalsIgnoreCase("author")
				|| prop.equalsIgnoreCase("chemicalFormula")
				|| prop.equalsIgnoreCase("comment")
				|| prop.equalsIgnoreCase("controlType")
				|| prop.equalsIgnoreCase("eCNumber")
				|| prop.equalsIgnoreCase("id")
				|| prop.equalsIgnoreCase("name") //it includes the following two as well
				|| prop.equalsIgnoreCase("term")
				|| prop.equalsIgnoreCase("title")
		);
	};

	public void index() {

		IndexWriter iw;
		try {
			//close the searcher manager if the old index exists
			if(searcherManager != null) {
				searcherManager.close();
				searcherManager = null;
			}

			CPathUtils.cleanupDirectory(indexFile.toString(), true);

			IndexWriterConfig conf = new IndexWriterConfig(analyzer);
			iw = new IndexWriter(FSDirectory.open(indexFile), conf);
			//cleanup
			iw.deleteAll();
			iw.commit();
		} catch (IOException e) {
			throw new RuntimeException("Failed to create a new IndexWriter.", e);
		}		
		final IndexWriter indexWriter = iw;

		ExecutorService exec = Executors.newFixedThreadPool(30);

		final int numObjectsToIndex = model.getObjects(Entity.class).size()
				+ model.getObjects(EntityReference.class).size();
		LOG.info("index(), there are " + numObjectsToIndex + " Entity or EntityReference objects to index.");

		final AtomicInteger numLeft = new AtomicInteger(numObjectsToIndex);

		final Fetcher fetcher = new Fetcher(SimpleEditorMap.L3, Fetcher.nextStepFilter);
		//disable traversing into sub-pathways when searching for child elements (worth doing for e.g., KEGG model)!
		fetcher.setSkipSubPathways(true);

		for(final BioPAXElement bpe : model.getObjects())
		{
			//Skip for UtilityClass but EntityReference and Provenance -
			//TODO: indexing of only processes (not physical entities, genes) would help most our use cases...
			if(!(bpe instanceof Entity || bpe instanceof EntityReference || bpe instanceof Provenance))
				continue;

			// prepare & index each element in a separate thread
			exec.execute(() -> {
					// get or infer some important values if possible from this, child or parent objects:
					Set<String> keywords = ModelUtils.getKeywords(bpe, 2, keywordsFilter);

					// a hack to remove special (debugging) biopax comments
					for(String s : new HashSet<>(keywords)) {
						//exclude additional comments generated by normalizer, merger, etc.
						if(s.startsWith("REPLACED ") || s.contains("ADDED"))
							keywords.remove(s);
					}
					
					bpe.getAnnotations().put(FIELD_KEYWORD, keywords);
					bpe.getAnnotations().put(FIELD_DATASOURCE, ModelUtils.getDatasources(bpe));
					bpe.getAnnotations().put(FIELD_ORGANISM, ModelUtils.getOrganisms(bpe));
					bpe.getAnnotations().put(FIELD_PATHWAY, ModelUtils.getParentPathways(bpe));
					
					//set <numparticipants> (PEs/Genes), <numprocesses> (interactions/pathways), <size> index fields:
					if(bpe instanceof org.biopax.paxtools.model.level3.Process) {
						int numProc = fetcher.fetch(bpe, Process.class).size(); //except itself
						int numPeAndG = fetcher.fetch(bpe, PhysicalEntity.class).size()
								+ fetcher.fetch(bpe, Gene.class).size();
						bpe.getAnnotations().put(FIELD_SIZE, Integer.toString(numProc + numPeAndG));
						bpe.getAnnotations().put(FIELD_N_PARTICIPANTS, Integer.toString(numPeAndG));
						bpe.getAnnotations().put(FIELD_N_PROCESSES, Integer.toString(numProc));
					} else if(bpe instanceof Complex) {
						int numPEs = fetcher.fetch(bpe, PhysicalEntity.class).size();
						bpe.getAnnotations().put(FIELD_SIZE, Integer.toString(numPEs));
						bpe.getAnnotations().put(FIELD_N_PARTICIPANTS, Integer.toString(numPEs));
					}

					index(bpe, indexWriter);
					
					//count, log a progress message
					int left = numLeft.decrementAndGet();
					if(left % 10000 == 0)
						LOG.info("index(), biopax objects left to index: " + left);
			});
		}
		
		exec.shutdown(); //stop accepting new tasks	
		try { //wait
			exec.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted!", e);
		}
		
		try {
			indexWriter.close(); //wait for pending op., auto-commit, close.
		} catch (IOException e) {
			throw new RuntimeException("Failed to close IndexWriter.", e);
		} 
		
		//finally, create a new searcher manager
		initSearcherManager();
	}

	// internal methods
	
	/*
	 * Creates a new Lucene Document that corresponds to a BioPAX object.
	 * It does not check whether the document exists (should not be there,
	 * because the {@link #index()} method cleans up the index)
	 * 
	 * Some fields also include biopax data type property values not only from 
	 * the biopax object but also from its child elements, up to some depth 
	 * (using key-value pairs in the pre-computed bpe.annotations map):
	 * 
	 *  'uri' - biopax object's absolute URI, index=no, analyze=no, store=yes;
	 * 
	 *  'name' - names, analyze=yes, store=yes; boosted;
	 * 
	 *  'keyword' - infer from this bpe and its child objects' data properties,
	 *            such as Score.value, structureData, structureFormat, chemicalFormula, 
	 *            availability, term, comment, patoData, author, source, title, url, published, 
	 *            up to given depth/level; and also all 'pathway' field values are included here; 
	 *            analyze=yes, store=yes;
	 *
	 *  'xrefid'  - Xref.id values - standard biological IDs - from a biopax object and some its child objects;
	 *  			analyze=no, store=no;
	 *  
	 *  'datasource', 'organism' and 'pathway' - infer from this bpe and its child objects 
	 *  									  	up to given depth/level, analyze=no, store=yes;
	 *  
	 *  'size', 'numprocesses', 'numparticipants' - number of child processes,
	 *  											participants; integer values as string; analyze=no, store=yes.
	*/
	void index(BioPAXElement bpe, IndexWriter indexWriter) {		
		// create a new document
		final Document doc = new Document();

		// save URI 'as is' (not indexed, not analyzed)
		Field field = new StoredField(FIELD_URI, bpe.getUri());
		doc.add(field);
		// lowercase URI and index, but store=no, analyze=no (this is to find an object exactly by its URI)
		field = new StringField(FIELD_URI, bpe.getUri().toLowerCase(), Field.Store.NO);
		doc.add(field);
		
		// index and store but not analyze/tokenize the biopax class name:
		field = new StringField(FIELD_TYPE, bpe.getModelInterface().getSimpleName().toLowerCase(), Field.Store.YES);
		doc.add(field);
		
		// make index fields from the annotations map (of pre-calculated/inferred values)
		if(!bpe.getAnnotations().isEmpty())
		{
			if(bpe.getAnnotations().containsKey(FIELD_PATHWAY)) {
				addPathways((Set<Pathway>)bpe.getAnnotations().get(FIELD_PATHWAY), doc);
			}

			if(bpe.getAnnotations().containsKey(FIELD_ORGANISM)) {
				addOrganisms((Set<BioSource>)bpe.getAnnotations().get(FIELD_ORGANISM), doc);
			}

			if(bpe.getAnnotations().containsKey(FIELD_DATASOURCE)) {
				addDatasources((Set<Provenance>)bpe.getAnnotations().get(FIELD_DATASOURCE), doc);
			}

			if(bpe.getAnnotations().containsKey(FIELD_KEYWORD)) {
				for (String keyword : (Set<String>)bpe.getAnnotations().get(FIELD_KEYWORD)) {
					Field f = new TextField(FIELD_KEYWORD, keyword.toLowerCase(), Field.Store.NO);
					doc.add(f);
				}
			}

			if(bpe.getAnnotations().containsKey(FIELD_SIZE)) {
				field = new StoredField(FIELD_SIZE,
						Integer.parseInt((String)bpe.getAnnotations().get(FIELD_SIZE)));
				doc.add(field);
			}

			if(bpe.getAnnotations().containsKey(FIELD_N_PARTICIPANTS)) {
				field = new StoredField(FIELD_N_PARTICIPANTS,
						Integer.parseInt((String)bpe.getAnnotations().get(FIELD_N_PARTICIPANTS)));
				doc.add(field);
			}

			if(bpe.getAnnotations().containsKey(FIELD_N_PROCESSES)) {
				field = new StoredField(FIELD_N_PROCESSES,
						Integer.parseInt((String)bpe.getAnnotations().get(FIELD_N_PROCESSES)));
				doc.add(field);
			}

			// Add xref IDs to the index (IDs are prepared and stored in advance
			// in the annotations map, under FIELD_XREFID key)
			Set<String> ids = (bpe.getAnnotations().containsKey(FIELD_XREFID))
				? (Set<String>)bpe.getAnnotations().get(FIELD_XREFID) :CPathUtils.getXrefIds(bpe);
			for (String id : ids) {
				//index as not analyzed, not tokenized
				field = new StringField(FIELD_XREFID, id.toLowerCase(), Field.Store.NO);
				doc.add(field);
				field = new StringField(FIELD_XREFID, id, Field.Store.NO);
				doc.add(field);
			}
		}

		bpe.getAnnotations().remove(FIELD_KEYWORD);
		bpe.getAnnotations().remove(FIELD_DATASOURCE);
		bpe.getAnnotations().remove(FIELD_ORGANISM);
		bpe.getAnnotations().remove(FIELD_PATHWAY);
		bpe.getAnnotations().remove(FIELD_SIZE);
		bpe.getAnnotations().remove(FIELD_N_PARTICIPANTS);
		bpe.getAnnotations().remove(FIELD_N_PROCESSES);
		bpe.getAnnotations().remove(FIELD_XREFID);

		// name
		if(bpe instanceof Named) {
			Named named = (Named) bpe;
			if(named.getStandardName() != null) {
				field = new StringField(FIELD_NAME, named.getStandardName().toLowerCase(), Field.Store.NO);
				doc.add(field);
				field = new StringField(FIELD_NAME, named.getStandardName(), Field.Store.NO);
				doc.add(field);
			}
			if(named.getDisplayName() != null && !named.getDisplayName().equalsIgnoreCase(named.getStandardName())) {
				field = new StringField(FIELD_NAME, named.getDisplayName().toLowerCase(), Field.Store.NO);
				doc.add(field);
				field = new StringField(FIELD_NAME, named.getDisplayName(), Field.Store.NO);
				doc.add(field);
			}
			for(String name : named.getName()) {
				if(!name.equalsIgnoreCase(named.getDisplayName()) && !name.equalsIgnoreCase(named.getStandardName())) {
					field = new StringField(FIELD_NAME, name.toLowerCase(), Field.Store.NO);
					doc.add(field);
					field = new StringField(FIELD_NAME, name, Field.Store.NO);
					doc.add(field);
				}
				field = new TextField(FIELD_KEYWORD, name.toLowerCase(), Field.Store.NO);
				field.setBoost(2.0f);
				doc.add(field);
			}
		}

		// write
		try {
			indexWriter.addDocument(doc);
		} catch (IOException e) {
			throw new RuntimeException("Failed to index; " + bpe.getUri(), e);
		}
	}

	private void addDatasources(Set<Provenance> set, Document doc) {
		for (Provenance p : set) {
			// Index (!) and store URI (untokenized) -
			// required to accurately calculate no. entities or to filter by data source
			// (different data sources might share same names)
			String u = p.getUri();
			doc.add(new StringField(FIELD_DATASOURCE, u, Field.Store.YES));

			if(u.startsWith("http://")) {
				//index the identifier
				if (u.endsWith("/"))
					u = u.substring(0, u.length() - 1);
				u = u.replaceAll(".*/", "");
				doc.add(new StringField(FIELD_DATASOURCE, u.toLowerCase(), Field.Store.NO));
			}

			// index names
			for (String s : p.getName())
				doc.add(new TextField(FIELD_DATASOURCE, s.toLowerCase(), Field.Store.NO));
		}
	}

	private void addOrganisms(Set<BioSource> set, Document doc) {	
		for(BioSource bs : set) {
			// store URI as is (not indexed, not tokinized)
			doc.add(new StoredField(FIELD_ORGANISM, bs.getUri()));
				
			// add organism names
			for(String s : bs.getName()) {
				doc.add(new TextField(FIELD_ORGANISM, s.toLowerCase(), Field.Store.NO));
			}
			// add taxonomy
			for(UnificationXref x : 
				new ClassFilterSet<Xref,UnificationXref>(bs.getXref(), UnificationXref.class)) {
				if(x.getId() != null)
					doc.add(new TextField(FIELD_ORGANISM, x.getId().toLowerCase(), Field.Store.NO));
			}
			// include tissue type terms
			if (bs.getTissue() != null) {
				for (String s : bs.getTissue().getTerm())
					doc.add(new TextField(FIELD_ORGANISM, s.toLowerCase(), Field.Store.NO));
			}
			// include cell type terms
			if (bs.getCellType() != null) {
				for (String s : bs.getCellType().getTerm()) {
					doc.add(new TextField(FIELD_ORGANISM, s.toLowerCase(), Field.Store.NO));
				}
			}
		}
	}

	private void addPathways(Set<Pathway> set, Document doc) {
		for(Pathway pw : set) {
			//store the URI 'as is' (don't lowercase, index/analyze=no, store=yes - required to report hits, e.g., as xml)
			doc.add(new StoredField(FIELD_PATHWAY, pw.getUri()));

			//lowercase URI, index=yes, analyze=no, store=no (this is to find child objects by absolute pathway URI)
			doc.add(new StringField(FIELD_PATHWAY, pw.getUri().toLowerCase(), Field.Store.NO));

			// add names to the 'pathway' (don't store) and 'keywords' (store) indexes
			// (this allows to find a biopax element, e.g., protein, by a parent pathway name: pathway:<query_str>)
			for (String s : pw.getName()) {
				doc.add(new TextField(FIELD_PATHWAY, s.toLowerCase(), Field.Store.NO));
			}
			
			// add unification xref IDs too
			for (UnificationXref x : new ClassFilterSet<>(
					pw.getXref(), UnificationXref.class)) {
				if (x.getId() != null) {
					// index in both 'pathway' (don't store) and 'keywords' (store)
					doc.add(new TextField(FIELD_PATHWAY, x.getId().toLowerCase(), Field.Store.NO));
				}
			}
		}
	}

	
	private String getTaxonId(BioSource bioSource) {
		String id = null;
		if(!bioSource.getXref().isEmpty()) {
			Set<UnificationXref> uxs = new ClassFilterSet<>(bioSource.getXref(), UnificationXref.class);
			for(UnificationXref ux : uxs) {
				if("taxonomy".equalsIgnoreCase(ux.getDb())) {
					id = ux.getId();
					break;
				}
			}
		}
		return id;
	}
	
	/*
	 * Creates a search filter like 
	 * type AND (datasource OR datasource...) 
	 *      AND (organism OR organism OR...)
	 * 
	 * Both names (partial or full) and URIs should work as filter values.
	 */	
	private Query createFilter(Class<? extends BioPAXElement> type, String[] datasources, String[] organisms)
	{
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		
		//AND datasources	
		if (datasources != null && datasources.length > 0) {
			builder.add(subQuery(datasources, FIELD_DATASOURCE), Occur.MUST);
		}
		//AND organisms
		if (organisms != null && organisms.length > 0) {
			builder.add(subQuery(organisms, FIELD_ORGANISM), Occur.MUST);
		}		
		//AND type	
		if(type != null) { //add biopax class filter
			BooleanQuery.Builder query = new BooleanQuery.Builder().
				add(new TermQuery(new Term(FIELD_TYPE, type.getSimpleName().toLowerCase())), Occur.SHOULD);//OR
			//for each biopax subclass (interface), add the name to the filter query
			for(Class<? extends BioPAXElement> subType : SimpleEditorMap.L3.getKnownSubClassesOf(type)) {
				query.add(new TermQuery(new Term(FIELD_TYPE, subType.getSimpleName().toLowerCase())), Occur.SHOULD);//OR
			}		
			builder.add(query.build(), Occur.MUST);
		}

		BooleanQuery filter = builder.build();
		//TODO: use LRUQueryCache with the filter somewhere, e.g.: Query q = queryCache.doCache(filter, defaultCachingPolicy);

		if(!filter.clauses().isEmpty()) {
			return filter;
		} else 
			return null;
	}

	/*
	 * Values are joint with OR, but if a value
	 * has whitespace symbols, it also make a sub-query,
	 * in which terms are joint with AND. This is to filter
	 * by datasource/organism's full name, partial name, uri,
	 * using multiple datasources/organisms.
	 * For example, 
	 * "search?q=*&datasource=intact complex&type..." - will get only IntAct Complex objects;
	 * "search?q=*&datasource=intact&type..." - will consider both IntAct and IntAct Complex
	 * "search?q=*&datasource=intact biogrid&type..." means "to occur in both intact and biogrid" 
	 *  (can be a canonical protein reference; it's is not equivalent to "search?q=*&datasource=intact&datasource=biogrid&...",
	 *  which means "to occur either in intact, incl. IntAct Complex, or in biogrid or in all these")
	 * "search?q=*&datasource=intact complex biogrid&..." - won't match anything.
	 */
	private Query subQuery(String[] filterValues, String filterField)
	{
		BooleanQuery.Builder query = new BooleanQuery.Builder();
		final Pattern pattern = Pattern.compile("\\s");		
		for(String v : filterValues) {
			//if v has whitespace chars (several words), make a "word1 AND word2 AND..." subquery
			if(pattern.matcher(v).find()) {
				BooleanQuery.Builder bq = new BooleanQuery.Builder();
				try {
					//use the same analyser as when indexing
					TokenStream tokenStream = analyzer.tokenStream(filterField, new StringReader(v));
					CharTermAttribute chattr = tokenStream.addAttribute(CharTermAttribute.class);
					tokenStream.reset();
					while(tokenStream.incrementToken()) {
						//'of', 'and', 'for',.. never occur as tokens (this is how the analyzer works)
						String token = chattr.toString();
						bq.add(new TermQuery(new Term(filterField, token)), Occur.MUST);
					}
					tokenStream.end(); 
					tokenStream.close();
				} catch (IOException e) {
					//should never happen as we use StringReader
					throw new RuntimeException("Failed to open a token stream; "
							+ "field:" + filterField + ", value:" + v,e);
				}
				query.add(bq.build(), Occur.SHOULD);
			} else {
				query.add(new TermQuery(new Term(filterField, v.toLowerCase())), Occur.SHOULD);
			}			
		}
		
		return query.build();
	}
}
