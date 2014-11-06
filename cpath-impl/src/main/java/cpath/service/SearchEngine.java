package cpath.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;
import org.biopax.paxtools.controller.Fetcher;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.Interaction;
import org.biopax.paxtools.model.level3.Named;
import org.biopax.paxtools.model.level3.Pathway;
import org.biopax.paxtools.model.level3.PathwayStep;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.Process;
import org.biopax.paxtools.model.level3.Provenance;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.XReferrable;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.paxtools.util.ClassFilterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.dao.CPathUtils;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;

/**
 * A full-text searcher/indexer for BioPAX L3 models.
 * 
 * TODO get it done (it's not ready and not tested yet).
 * 
 * @author rodche
 */
public class SearchEngine implements Indexer, Searcher {
	private static final Logger LOG = LoggerFactory.getLogger(SearchEngine.class);
	
	// search fields
	public static final String FIELD_URI = "uri";
	public static final String FIELD_KEYWORD = "keyword"; //anything, e.g., names, terms, comments, incl. - from child elements 
	public static final String FIELD_NAME = "name"; // standardName, displayName, other names
	public static final String FIELD_TERM = "term"; // CV terms
	public static final String FIELD_XREFDB = "xrefdb"; //xref.db
	public static final String FIELD_XREFID = "xrefid"; //xref.id
	public static final String FIELD_ECNUMBER = "ecnumber";
	public static final String FIELD_SEQUENCE = "sequence";
	public static final String FIELD_PATHWAY = "pathway"; //parent/owner pathways; to be inferred from the whole biopax model
	public static final String FIELD_SIZE = "size";
	// Full-text search/filter fields (case sensitive) -
	public static final String FIELD_ORGANISM = "organism";
	public static final String FIELD_DATASOURCE = "datasource";
	public static final String FIELD_TYPE = "type";

	
	public final static String[] DEFAULT_FIELDS =
	{
			FIELD_KEYWORD, //anything, e.g., names, terms, comments, incl. - from child elements 
			FIELD_NAME, // standardName, displayName, other names
			FIELD_TERM, // CV terms
			FIELD_XREFDB, //xref.db
			FIELD_XREFID, //xref.id (incl. direct child xref's id, if any)
			FIELD_ECNUMBER,
			FIELD_SEQUENCE,
			FIELD_PATHWAY, // to find or filter by a parent pathway (name or uri)
			FIELD_ORGANISM, // can be useful in a query for a cell/tissue type (term), in addition to filtering by taxonomy/organism, as normally
			FIELD_SIZE, // e.g., find entities with a given no. child/associated processes...
//			FIELD_DATASOURCE, // this one is merely for filtering
			FIELD_TYPE, //one can filter by all sub-classes and/or query for a particular biopax type only
	};
	
	
	private final Model model;
	private int maxHitsPerPage;
	private final File indexFile;
	private final Analyzer analyzer;

	public final static Version LUCENE_VERSION = Version.LUCENE_36;
	public final static int DEFAULT_MAX_HITS_PER_PAGE = 100;
	
	/**
	 * Main Constructor.
	 * @param indexLocation
	 */
	public SearchEngine(Model model, String indexLocation) {
		this.model = model;
		this.indexFile = new File(indexLocation);
		this.maxHitsPerPage = DEFAULT_MAX_HITS_PER_PAGE;
		this.analyzer = new StandardAnalyzer(LUCENE_VERSION);
	}

	
	public void setMaxHitsPerPage(int maxHitsPerPage) {
		this.maxHitsPerPage = maxHitsPerPage;
	}
	
	public int getMaxHitsPerPage() {
		return maxHitsPerPage;
	}
	

	public SearchResponse search(String query, int page,
			Class<? extends BioPAXElement> filterByType, String[] datasources,
			String[] organisms) 
	{
		SearchResponse response = null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("search: " + query + ", page: " + page 
					+ ", filterBy: " + filterByType
					+ "; extra filters: ds in (" + Arrays.toString(datasources)
					+ "), org. in (" + Arrays.toString(organisms) + ")");
	
		try {
			IndexReader reader = IndexReader.open(MMapDirectory.open(indexFile));
			IndexSearcher searcher = new IndexSearcher(reader);
			TopScoreDocCollector collector = TopScoreDocCollector.create(maxHitsPerPage, true);  
			int startIndex = page * maxHitsPerPage;
			//TODO try another query parser?
			QueryParser queryParser = new MultiFieldQueryParser(LUCENE_VERSION, DEFAULT_FIELDS, analyzer);
			
			//TODO handle q="*" queries separately

			Query luceneQuery = queryParser.parse(query);
//			luceneQuery = searcher.rewrite(luceneQuery); //TODO why to rewrite a Lucene query?..
			Filter filter = createFilter(filterByType, datasources, organisms);
			searcher.search(luceneQuery, filter, collector);
			TopDocs topDocs = collector.topDocs(startIndex, maxHitsPerPage);
			
			// create/init the highlighter and transformer
			Highlighter highlighter = null;
			if(!query.equals("*")) {
				// a highlighter (store.YES must be enabled for 'keyword' field)
				QueryScorer scorer = new QueryScorer(luceneQuery, FIELD_KEYWORD); 
				SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<span class='hitHL'>", "</span>");
				highlighter = new Highlighter(formatter, scorer);
				highlighter.setTextFragmenter(new SimpleSpanFragmenter(scorer, 80));
			}
			
			response = transform(searcher, highlighter, topDocs);
			
			//set total no. hits	
			response.setNumHits(collector.getTotalHits());		
			
		} catch (ParseException e) {
			throw new RuntimeException("getTopDocs: failed to parse the query string.", e);
		} catch (IOException e) {
			throw new RuntimeException("getTopDocs: failed.", e);
		}

		return response;
	}

	
	// Highlight, transform Lucene docs to hits (xml/java beans)
	private SearchResponse transform(IndexSearcher searcher, Highlighter highlighter, TopDocs topDocs) 
			throws CorruptIndexException, IOException {
		
		SearchResponse response = new SearchResponse();
		response.setMaxHitsPerPage(maxHitsPerPage);
		List<SearchHit> hits = response.getSearchHit();//empty list
		assert hits!=null && hits.isEmpty();
		
		for(ScoreDoc scoreDoc : topDocs.scoreDocs) {
			
			SearchHit hit = new SearchHit();
			Document doc = searcher.doc(scoreDoc.doc);
			String uri = doc.get(FIELD_URI);
			BioPAXElement bpe = model.getByID(uri);
			
			LOG.debug("transform: doc:" + scoreDoc.doc + ", uri:" + uri);
			
			// use the highlighter (get matching fragments)
			if (highlighter != null) {
				final List<String> frags = new ArrayList<String>();
				try {
					if (doc.getFieldable(FIELD_KEYWORD) != null) {
						final String text = StringUtils.join(doc.getValues(FIELD_KEYWORD), " ");					
						for(String fr : highlighter.getBestFragments(analyzer, FIELD_KEYWORD, text, 5)) {
							frags.add(fr);
						}
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				if(!frags.isEmpty())
					hit.setExcerpt(frags.toString());
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
			if(doc.getFieldable(FIELD_ORGANISM) != null) {
				Set<String> uniqueVals = new TreeSet<String>();
				for(String o : doc.getValues(FIELD_ORGANISM)) {
					//note: only URIS are stored in the index					
					uniqueVals.add(o);
				}
				hit.getOrganism().addAll(uniqueVals);
			}
			
			// extract values form the index
			if(doc.getFieldable(FIELD_DATASOURCE) != null) {
				Set<String> uniqueVals = new TreeSet<String>();
				for(String d : doc.getValues(FIELD_DATASOURCE)) {
					//note: only URIS are stored in the index
					uniqueVals.add(d);
				}
				hit.getDataSource().addAll(uniqueVals);
			}	
			
			// extract only pathway URIs 
			//(because names and IDs used to be stored in the index field as well)
			if(doc.getFieldable(FIELD_PATHWAY) != null) {
				Set<String> uniqueVals = new TreeSet<String>();
				for(String d : doc.getValues(FIELD_PATHWAY)) {
					//note: only URIS were stored in the index (though all names/ids were indexed)
					uniqueVals.add(d);
				}
				hit.getPathway().addAll(uniqueVals);
			}
			
			//no. processes in the sub-network
			if(doc.get(FIELD_SIZE)!=null)
				hit.setSize(Integer.valueOf(doc.get(FIELD_SIZE))); 
			
//			if(CPathSettings.getInstance().isDebugEnabled()) {
//				String excerpt = hit.getExcerpt();
//				if(excerpt == null) excerpt = "";
//				hit.setExcerpt(excerpt + " -SCORE- " + scoreDoc.score 
//						+ " -EXPLANATION- " + searcher.explain(luceneQuery, scoreDoc.doc));
//			}
			
			
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


	public void index() {		
		LOG.info("index(), there are " + model.getObjects().size() 
				+ " biopax objects to be indexed.");
		
		//drop/cleanup the index if exists
		if(indexExists(indexFile)) {
			LOG.info("Erasing the biopax index directory...");
			CPathUtils.cleanupDirectory(indexFile);
		}	
		
		IndexWriter iw;		
		try {
			Directory directory = FSDirectory.open(indexFile);
			IndexWriterConfig conf = new IndexWriterConfig(LUCENE_VERSION, analyzer); //default cfg
			iw = new IndexWriter(directory, conf);
		} catch (IOException e) {
			throw new RuntimeException("Failed to create a new IndexWriter.", e);
		}
		final IndexWriter indexWriter = iw;
		
		ExecutorService exec = Executors.newFixedThreadPool(30);
		
		for(final BioPAXElement bpe : model.getObjects()) {	
			// prepare & index each element in a separate thread
			exec.execute(new Runnable() {
				public void run() {					
					// (hack) infer values from this and child objects' data properties and store in the annotations map:
					// - add data type property values from up to 3nd-level child elements.
					bpe.getAnnotations().put(FIELD_KEYWORD, ModelUtils.getKeywords(bpe, 3));
					// - associate datasources for some utility class objects
					// (Entity class instances have @Field annotated 'dataSource' 
					// property and will be indexed without this additional step)
					if(bpe instanceof EntityReference || bpe instanceof PathwayStep)
						bpe.getAnnotations().put(FIELD_DATASOURCE, ModelUtils.getDatasources(bpe));
					// - associate organisms with all Entity and EntityReference objects
					// (we want to do this for some of biopax types that do not have 
					// 'organism' property)
					if(bpe instanceof Entity || bpe instanceof EntityReference)
						bpe.getAnnotations().put(FIELD_ORGANISM, ModelUtils.getOrganisms(bpe));

					// - infer parent pathways
					if(bpe instanceof Entity || bpe instanceof EntityReference || bpe instanceof PathwayStep)
						bpe.getAnnotations().put(FIELD_PATHWAY, ModelUtils.getParentPathways(bpe));
					
					// save no. member interactions and pathways
					if(bpe instanceof org.biopax.paxtools.model.level3.Process) {
						int size = new Fetcher(SimpleEditorMap.L3, Fetcher.nextStepFilter)
						.fetch(bpe, Process.class).size() + 1; //+1 counts itself						
						bpe.getAnnotations().put(FIELD_SIZE, Integer.toString(size)); 
					}

					index(bpe, indexWriter); 
				}
			});		    
		}
		
		exec.shutdown(); //stop accepting new tasks	
		try {
			exec.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted!", e);
		}
		
		try {
			indexWriter.close();
		} catch (IOException e) {
			throw new RuntimeException("Failed to close IndexWriter.", e);
		}
	}
	
	
	// internal methods
	
	/**
	 * Creates a new Lucene Document that corresponds to a BioPAX object.
	 * 
	 * It does not check though whether the document exists (should not be there
	 * because the {@link #index(Model)} method does clean up the index directory if exists)
	 * 
	 * BioPAX L3 properties and how to index them. -
	 * 
	 *  uri (aka rdfID) - 'uri' - biopax object's URI, index=no, analyze=no, store=yes;
	 *  standardName, name, displayName - 'name' - boost 3.0, 2.0, 2.5, respectively; analyze, do not store;
	 *  xref - 'xrefid' - boost 1.5; analyze=no (untokenized), store=no
	 *  db, id - 'xrefid', 'xrefdb' - boost 1.1; id - analyze=no (untokenized), store=no
	 *  sequence - 'sequence' - analyze=yes, store=no
	 *  organism - 'organism' - analyze=no, store=yes
	 *  dataSource - 'datasource' - analyze=no, store=yes
	 *  eCNumber - 'ecnumber' - analyze=no, store=no
	 *  term - 'term' - all terms joined by space - analyze=yes, store=no
	 * 
	 * Fields that also include all data type property values 
	 * from current and its child elements (store/use key-value pairs 
	 * in the bpe.annotations map):
	 * 
	 *  'keyword' - infer from current and its child objects' data properties,
	 *            such as Score.value, structureData, structureFormat, chemicalFormula, 
	 *            availability, comment, patoData, author, source, title, url, published, 
	 *            up to given depth/level, analyze=yes, store=yes;
	 *  
	 *  'datasource', 'organism', 'pathway' - infer from current and its child objects up to given depth/level, analyze=no, store=yes;
	 *  
	 *  'size' - number of child processes, an integer as string; analyze=no, store=yes
	 * 
	 * As per BioPAX type, boost:
	 *  Pathway - boost 1.7 (for all index field);
	 *  Interaction - 1.5;
	 *  PhysicalEntity - 1.3;
	 *  Xref - 1.1;
	 * 
	*/
	void index(BioPAXElement bpe, IndexWriter indexWriter) {		
		// create a new document
		final Document doc = new Document();
		
		// save URI
		Field field = new Field(FIELD_URI, bpe.getRDFId(), Field.Store.YES, Field.Index.NO);
		doc.add(field);
		
		// save biopax type name
		field = new Field(FIELD_TYPE, bpe.getModelInterface().getSimpleName().toLowerCase(), 
				Field.Store.YES, Field.Index.NOT_ANALYZED);
		doc.add(field);
		
		// make index fields from the annotations map (of pre-calculated/inferred values)
		if(!bpe.getAnnotations().isEmpty()) {
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
				addKeywords((Set<String>)bpe.getAnnotations().get(FIELD_KEYWORD), doc);
			}
			if(bpe.getAnnotations().containsKey(FIELD_SIZE)) {
				field = new Field(FIELD_SIZE, (String)bpe.getAnnotations()
					.get(FIELD_SIZE), Field.Store.YES, Field.Index.NOT_ANALYZED);
				doc.add(field);
			}
		}
		bpe.getAnnotations().remove(FIELD_KEYWORD);
		bpe.getAnnotations().remove(FIELD_DATASOURCE);
		bpe.getAnnotations().remove(FIELD_ORGANISM);
		bpe.getAnnotations().remove(FIELD_PATHWAY);
		bpe.getAnnotations().remove(FIELD_SIZE);
			
		// name
		if(bpe instanceof Named) {
			Named named = (Named) bpe;
			if(named.getDisplayName() != null) {
				field = new Field(FIELD_NAME, named.getDisplayName(), Field.Store.NO, Field.Index.ANALYZED);
				field.setBoost(2.5f);
				doc.add(field);
			}
			if(named.getStandardName() != null) {
				field = new Field(FIELD_NAME, named.getStandardName(), Field.Store.NO, Field.Index.ANALYZED);
				field.setBoost(3.0f);
				doc.add(field);
			}
			if(!named.getName().isEmpty()) {
				field = new Field(FIELD_NAME, StringUtils.join(named.getName(), " "), Field.Store.NO, Field.Index.ANALYZED);
				field.setBoost(2.0f);
				doc.add(field);
			}
		}
		
		// XReferrable.xref - build 'xrefid' index field from all Xrefs)
		if(bpe instanceof XReferrable) {
			XReferrable xr = (XReferrable) bpe;
			for(Xref xref : xr.getXref()) {
				if (xref.getId() != null) {
					//the filed is not_analyzed; so in order to make search case-insensitive 
					//(when searcher uses standard analyzer), we turn the value to lowercase.
					field = new Field(FIELD_XREFID, xref.getId().toLowerCase(), Field.Store.NO, Field.Index.NOT_ANALYZED);
					field.setBoost(1.5f);
					doc.add(field);
				}
			}
		}
		
		// Xref db/id (these are for a precise search by standard bio ID)
		if(bpe instanceof Xref) {
			Xref xref = (Xref) bpe;
			if (xref.getId() != null) {
				field = new Field(FIELD_XREFID, xref.getId().toLowerCase(), Field.Store.NO, Field.Index.NOT_ANALYZED);
				doc.add(field);
			}
			if (xref.getDb() != null) {
				field = new Field(FIELD_XREFDB, xref.getDb().toLowerCase(), Field.Store.NO, Field.Index.NOT_ANALYZED);
				doc.add(field);
			}
		}
		
		
		// boost entire document based on biopax type
		if(bpe instanceof Pathway)
			doc.setBoost(1.7f);
		else if(bpe instanceof Interaction)
			doc.setBoost(1.5f);
		else if(bpe instanceof PhysicalEntity)
			doc.setBoost(1.3f);
		else if(bpe instanceof Xref)
			doc.setBoost(1.1f);
		
		// write
		try {
			indexWriter.addDocument(doc);
		} catch (IOException e) {
			throw new RuntimeException("Failed to index; " + bpe.getRDFId(), e);
		}
	}

	
	private void addKeywords(Set<String> keywords, Document doc) {
		for (String keyword : keywords) {
			doc.add(new Field(FIELD_KEYWORD, keyword.toLowerCase(), Field.Store.YES, Field.Index.ANALYZED));
		}
	}

	private void addDatasources(Set<Provenance> set, Document doc) {
		for (Provenance p : set) {
			// do not do .toLowerCase() for the URI!
			doc.add(new Field(FIELD_DATASOURCE, p.getRDFId(), Field.Store.YES, Field.Index.NO));
			// index names as well
			for (String s : p.getName())
				doc.add(new Field(FIELD_DATASOURCE, s.toLowerCase(), Field.Store.NO, Field.Index.NOT_ANALYZED));
		}
	}

	private void addOrganisms(Set<BioSource> set, Document doc) {	
		for(BioSource bs : set) {
			// store URI as is, don't index
			doc.add(new Field(FIELD_ORGANISM, bs.getRDFId(), Field.Store.YES, Field.Index.NO));
				
			// add organism names
			for(String s : bs.getName()) {
				doc.add(new Field(FIELD_ORGANISM, s.toLowerCase(), Field.Store.NO, Field.Index.NOT_ANALYZED));
			}
			// add taxonomy
			for(UnificationXref x : 
				new ClassFilterSet<Xref,UnificationXref>(bs.getXref(), UnificationXref.class)) {
				if(x.getId() != null)
					doc.add(new Field(FIELD_ORGANISM, x.getId().toLowerCase(), Field.Store.NO, Field.Index.NOT_ANALYZED));
			}
			// include tissue type terms
			if (bs.getTissue() != null) {
				for (String s : bs.getTissue().getTerm())
					doc.add(new Field(FIELD_ORGANISM, s.toLowerCase(), Field.Store.NO, Field.Index.NOT_ANALYZED));
			}
			// include cell type terms
			if (bs.getCellType() != null) {
				for (String s : bs.getCellType().getTerm()) {
					doc.add(new Field(FIELD_ORGANISM, s.toLowerCase(), Field.Store.NO, Field.Index.NOT_ANALYZED));
				}
			}
		}
	}

	private void addPathways(Set<Pathway> set, Document doc) {
		for(Pathway pw : set) {
			//add URI as is (do not lowercase; do not index; store=yes - required to report hits, e.g., as xml)
			doc.add(new Field(FIELD_PATHWAY, pw.getRDFId(), Field.Store.YES, Field.Index.NO));
			// add names to the index as well
			for (String s : pw.getName()) {
				doc.add(new Field(FIELD_PATHWAY, s.toLowerCase(), Field.Store.NO, Field.Index.ANALYZED));
			}
			// add unification xref IDs too
			for (UnificationXref x : new ClassFilterSet<Xref, UnificationXref>(
					pw.getXref(), UnificationXref.class)) {
				if (x.getId() != null)
					doc.add(new Field(FIELD_PATHWAY, x.getId().toLowerCase(), Field.Store.NO, Field.Index.NOT_ANALYZED));
			}
		}
	}

	
	private String getTaxonId(BioSource bioSource) {
		String id = null;
		if(!bioSource.getXref().isEmpty()) {
			Set<UnificationXref> uxs = new 
				ClassFilterSet<Xref,UnificationXref>(bioSource.getXref(), 
						UnificationXref.class);
			for(UnificationXref ux : uxs) {
				if("taxonomy".equalsIgnoreCase(ux.getDb())) {
					id = ux.getId();
					break;
				}
			}
		}
		return id;
	}
	
	
	private Filter createFilter(Class<? extends BioPAXElement> type, 
			String[] datasources, String[] organisms) {
		
		/* Only standard names and IDs will work as filter values; other will return no result 
		 * (also, the filter fields are not tokenized/analyzed, by design)
		 */
		BooleanQuery query = new BooleanQuery();
		if (datasources != null && datasources.length > 0)
		for(String fv : datasources) {
			String term = fv.trim();
//			query.add(new TermQuery(new Term(FIELD_DATASOURCE, term)),
//					Occur.SHOULD); // SHOULD here means "OR"
			query.add(new TermQuery(new Term(FIELD_DATASOURCE, term.toLowerCase())),
				Occur.SHOULD);
		}
		
		if (organisms != null && organisms.length > 0)
		for(String fv : organisms) {
			String term = fv.trim();
//			query.add(new TermQuery(new Term(FIELD_ORGANISM, term)),
//					Occur.SHOULD); // SHOULD here means "OR"
			query.add(new TermQuery(new Term(FIELD_ORGANISM, term.toLowerCase())),
				Occur.SHOULD);
		}
		
		//add biopax class filter if makes sense
		if(type != null && type != BioPAXElement.class) {
			query.add(new TermQuery(new Term(FIELD_TYPE, type.getSimpleName().toLowerCase())), Occur.SHOULD);
			//for each biopax/paxtools subclass (interface) of this one,
			//add the interface name as filter value
			for(Class<? extends BioPAXElement> subType : SimpleEditorMap.L3.getKnownSubClassesOf(type)) {
				query.add(new TermQuery(new Term(FIELD_TYPE, subType.getSimpleName().toLowerCase())), Occur.SHOULD);
			}
		}
		
		if(!query.clauses().isEmpty())
			return new CachingWrapperFilter( new QueryWrapperFilter(query) ); //TODO why CachingWrapperFilter, QueryWrapperFilter?
		else 
			return null;
	}	

	
	/**
	 * Tests if the full-text index (directory, files) exists.
	 * @param path
	 * @return
	 */
    boolean indexExists(File path) {
    	try {
    		FSDirectory directory = FSDirectory.open(path);
    		try {
    			return IndexReader.indexExists(directory);
    		} finally {
    			directory.close();
    		}
    	} catch (IOException e) {
    		return false;
    	}
    }
	
}
