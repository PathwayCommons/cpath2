package cpath.service;

import java.nio.file.Path;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cpath.service.metadata.Index;
import cpath.service.metadata.Mapping;
import cpath.service.metadata.Mappings;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.FSDirectory;
import org.biopax.paxtools.controller.*;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.level3.Process;
import org.biopax.paxtools.normalizer.Resolver;
import org.biopax.paxtools.util.ClassFilterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;

/**
 * A full-text searcher/indexer for BioPAX L3 models.
 *
 * Only Entity and EntityReference BioPAX types (incl. child types) get indexed (since 12/2015).
 *
 * @author rodche
 */
public class IndexImpl implements Index, Mappings {
	static final Logger LOG = LoggerFactory.getLogger(IndexImpl.class);

	private Model model;
	private int maxHitsPerPage;
	private final Analyzer analyzer;
	private IndexWriter indexWriter;
	private SearcherManager searcherManager;
	public final static int DEFAULT_MAX_HITS_PER_PAGE = 100;

	/**
	 * Constructor.
	 *
	 * @param model         the BioPAX Model to index or search
	 * @param indexLocation index directory location
	 * @param readOnly
	 */
	public IndexImpl(Model model, String indexLocation, boolean readOnly) {
		this.model = model;
		maxHitsPerPage = DEFAULT_MAX_HITS_PER_PAGE;
		//refs issue #269
		KeywordAnalyzer ka = new KeywordAnalyzer();
		Map<String,Analyzer> analyzersPerField = Map.of(
				FIELD_NAME, ka,
				FIELD_XREFID, ka,
				FIELD_URI, ka,
				FIELD_PATHWAY, ka,
				FIELD_DSTDB, ka,
				FIELD_DSTID, ka,
				FIELD_SRCDB, ka,
				FIELD_SRCID, ka
		);
		analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET), analyzersPerField);
		try {
			Path indexFile = Paths.get(indexLocation);
			if(readOnly) {
				searcherManager = new SearcherManager(FSDirectory.open(indexFile), new SearcherFactory());
			} else {
				indexWriter = new IndexWriter(FSDirectory.open(indexFile), new IndexWriterConfig(analyzer));
				searcherManager = new SearcherManager(indexWriter, new SearcherFactory());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		BooleanQuery.setMaxClauseCount(10000); //Integer.MAX_VALUE
	}

	public void setMaxHitsPerPage(int maxHitsPerPage) {
		this.maxHitsPerPage = maxHitsPerPage;
	}

	public int getMaxHitsPerPage() {
		return maxHitsPerPage;
	}

	public SearchResponse search(String query, int page, Class<? extends BioPAXElement> type,
								 String[] datasources, String[] organisms) {
		SearchResponse response;
		LOG.debug("search: '" + query + "', page: " + page
			+ ", filterBy: " + ((type!=null)?type.getSimpleName():"N/A")
			+ "; extra filters: ds in (" + Arrays.toString(datasources)
			+ "), org. in (" + Arrays.toString(organisms) + ")");
		IndexSearcher searcher = null;
		try {	
			QueryParser queryParser = new MultiFieldQueryParser(DEFAULT_FIELDS, analyzer);
			queryParser.setAllowLeadingWildcard(true);//we want leading wildcards enabled (e.g. *sulin)
			Query q;
			//find and transform top docs to search hits (beans), considering pagination...
			if(!query.trim().equals("*")) { //if not "*" query, which is not supported out-of-the-box, then
				//create the lucene query
				q = queryParser.parse(query);
				LOG.debug("parsed lucene query is " + q.getClass().getSimpleName());
				//create filter: type AND (d OR d...) AND (o OR o...)
				Query filter = createFilter(type, datasources, organisms);
				//final query with filter
				if(filter != null) {
					q = new BooleanQuery.Builder().add(q, Occur.MUST).add(filter, Occur.FILTER).build();
				}
			} else { //find ALL objects of a particular BioPAX class (+ filters by organism, datasource)
				if(type == null) {
					type = Level3Element.class;
				}
				//replace q="*" with a search for the class or its subclass name in the TYPE field
				BooleanQuery.Builder starQuery = new BooleanQuery.Builder();
				for(Class<? extends BioPAXElement> subType : SimpleEditorMap.L3.getKnownSubClassesOf(type)) {
					starQuery.add(new TermQuery(new Term(FIELD_TYPE, subType.getSimpleName().toLowerCase())), Occur.SHOULD);
				}
				Query filter = createFilter(null, datasources, organisms);
				//combine star and filter queries into one special boolean
				q = (filter!=null)
					? new BooleanQuery.Builder().add(starQuery.build(),Occur.MUST).add(filter,Occur.FILTER).build()
						: starQuery.build();
			}

			searcher = searcherManager.acquire();
			TopDocs topDocs;
			if(page>0) {
				//get the required hits page if page>0
				TopScoreDocCollector collector = TopScoreDocCollector
						.create(maxHitsPerPage*(page+1), maxHitsPerPage*(page+1));
				searcher.search(q, collector);
				topDocs = collector.topDocs(page * maxHitsPerPage, maxHitsPerPage);
			} else {
				//get the first page of the top hits
				topDocs = searcher.search(q, maxHitsPerPage);
			}
			//transform docs to hits (optionally use a highlighter, e.g., if debugging...)
			response = transform(q, searcher, topDocs);
		} catch (ParseException e) {
			throw new RuntimeException("getTopDocs: failed to parse the search query: " + e);
		} catch (IOException e) {
			throw new RuntimeException("getTopDocs: failed: " + e);
		} finally {
			try {
				searcherManager.release(searcher);
			} catch (IOException e) {}
		}
	
		response.setPageNo(page);
		return response;
	}

	
	// Transform Lucene docs to hits (xml/java beans)
	private SearchResponse transform(Query query, IndexSearcher searcher, TopDocs topDocs) throws IOException
	{	
		if(topDocs == null) {
			throw new IllegalArgumentException("topDocs is null");
		}
		SearchResponse response = new SearchResponse();
		response.setMaxHitsPerPage(getMaxHitsPerPage());
		long numTotalHits = topDocs.totalHits.value; //todo: call searcher.count(q) instead or it's the same?
		response.setNumHits(numTotalHits);
		List<SearchHit> hits = response.getSearchHit();//empty list to be filled from top docs
		assert hits!=null && hits.isEmpty();
		LOG.debug("transform, no. TopDocs to process:" + topDocs.scoreDocs.length);
		for(ScoreDoc scoreDoc : topDocs.scoreDocs) {
			SearchHit hit = new SearchHit();
			Document doc = searcher.doc(scoreDoc.doc);
			String uri = doc.get(FIELD_URI);
			BioPAXElement bpe = model.getByID(uri);
			if(bpe == null) {
				continue; //was a hit from another model
			}
			
			// use a highlighter (get matching fragments)
			if (LOG.isDebugEnabled()) {
				// to use a Highlighter, store.YES must be enabled for 'keyword' field
				QueryScorer scorer = new QueryScorer(query, FIELD_KEYWORD);
				//the following fixes scoring/highlighting for all-field wildcard (like q=insulin*)
				//but not for term/prefix queries (q=name:insulin*, q=pathway:brca2)_.
				scorer.setExpandMultiTermQuery(true);
				SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<span class='hitHL'>", "</span>");
				Highlighter highlighter = new Highlighter(formatter, scorer);
				highlighter.setTextFragmenter(new SimpleSpanFragmenter(scorer, 80));
				final String text = String.join(" ", ModelUtils.getKeywords(bpe,2, keywordsFilter));
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
						
			// extract organisms (URIs only)
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
			
			//no. processes, participants in the subnetwork
			if(doc.getField(FIELD_N_PROCESSES) != null) {
				hit.setNumProcesses(doc.getField(FIELD_N_PROCESSES).numericValue().intValue());
			}
			if(doc.getField(FIELD_N_PARTICIPANTS) != null) {
				hit.setNumParticipants(doc.getField(FIELD_N_PARTICIPANTS).numericValue().intValue());
			}
			hits.add(hit);
		}
				
		//add the Provenance's standardName(s) to the search response
		if(!hits.isEmpty()) {
			for(String puri : response.provenanceUris()) {
				Provenance p = (Provenance) model.getByID(puri);
				response.getProviders().add((p.getStandardName()!=null)?p.getStandardName():p.getDisplayName());
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

	
	/**
	 * Creates or updates a Lucene Document that corresponds to a BioPAX object.
	 * It does not check whether the document already exists.
	 * <p>
	 * Some fields also include biopax data type property values not only from
	 * the biopax object but also from its child elements, up to some depth
	 * (using key-value pairs in the pre-computed bpe.annotations map):
	 * <p>
	 * 'uri' - biopax object's absolute URI, index=yes, analyze=no, store=yes;
	 * <p>
	 * 'name' - names, analyze=yes, store=yes;
	 * <p>
	 * 'keyword' - infer from this bpe and its child objects' data properties,
	 * such as Score.value, structureData, structureFormat, chemicalFormula,
	 * availability, term, comment, patoData, author, source, title, url, published,
	 * up to given depth/level; and also all 'pathway' field values are included here;
	 * analyze=yes, store=yes;
	 * <p>
	 * 'xrefid' - Xref.id values - standard biological IDs - from a biopax object and some of its child objects;
	 * analyze=no, store=no;
	 * <p>
	 * 'datasource', 'organism' and 'pathway' - infer from this bpe and its child objects
	 * up to given depth/level, analyze=no, store=yes;
	 * <p>
	 * 'numprocesses', 'numparticipants' - number of child processes,
	 * participants; integer values as string; analyze=no, store=yes.
	 *
	 * @param bpe BioPAX element
	 */
	public void save(BioPAXElement bpe) {
		//traverse the element to collect more keywords, e.g. names, IDs, from its child elements
		Fetcher fetcher = new Fetcher(SimpleEditorMap.L3, Fetcher.nextStepFilter);
		//disable traversing into sub-pathways
		fetcher.setSkipSubPathways(true);
		// get or infer some important values if possible from this, child or parent objects:
		Set<String> keywords = ModelUtils.getKeywords(bpe, 2, keywordsFilter);
		//exclude from the index any autogenerated comments (e.g. by normalizer, merger)
		keywords = keywords.stream()
				.filter(s -> !s.startsWith("REPLACED") && !s.contains("ADDED"))
				.collect(Collectors.toSet());

		// create a new document
		final Document doc = new Document();
		// using StringField and KeywordAnalyser (when searching) for 'uri' field
		final String uri = bpe.getUri();
    // save URI: indexed, not analyzed, stored
		doc.add(new StringField(FIELD_URI, uri, Field.Store.YES));

		//index the last part of the uri (e.g., 'hsa00010' or like 'ProteinReference_ca123bd44...'); todo: why?..
		String luri = (uri.endsWith("/")) ? uri.substring(0, uri.length()-1) : uri;
		luri = luri.replaceAll(".*[/#]", "").trim();
		doc.add(new StringField(FIELD_URI, luri, Field.Store.NO));

		// index and store but not analyze/tokenize biopax class name (lowcase as we use StandardAnalyzer for searching/filtering in this field):
		doc.add(new StringField(FIELD_TYPE, bpe.getModelInterface().getSimpleName().toLowerCase(), Field.Store.YES));

		// extra index fields
		addPathways(ModelUtils.getParentPathways(bpe), doc);
		addOrganisms(ModelUtils.getOrganisms(bpe), doc);
		addDatasources(ModelUtils.getDatasources(bpe), doc);
		for (String keyword : keywords) {
			doc.add(new TextField(FIELD_KEYWORD, keyword.toLowerCase(), Field.Store.NO));
		}

		//set <numparticipants> (PEs/Genes), <numprocesses> (interactions/pathways), <size> index fields:
		if(bpe instanceof org.biopax.paxtools.model.level3.Process) {
			int numProc = fetcher.fetch(bpe, Process.class).size(); //except itself
			int numPeAndG = fetcher.fetch(bpe, PhysicalEntity.class).size()
					+ fetcher.fetch(bpe, Gene.class).size();
			doc.add(new StoredField(FIELD_N_PARTICIPANTS, numPeAndG));
			doc.add(new StoredField(FIELD_N_PROCESSES, numProc));
		} else if(bpe instanceof Complex) {
			int numPEs = fetcher.fetch(bpe, PhysicalEntity.class).size();
			doc.add(new StoredField(FIELD_N_PARTICIPANTS, numPEs));
		}

		// Add more xref IDs to the index using id-mapping
		final Set<String> ids = CPathUtils.getXrefIds(bpe);
		Pattern isoformIdPattern = Pattern.compile(Resolver.getNamespace("uniprot.isoform", true).getPattern());
		Pattern uniprotIdPattern = Pattern.compile(Resolver.getNamespace("uniprot", true).getPattern());
		// also collect ChEBI and UniProt IDs and then use id-mapping to associate the bpe with more IDs:
		final List<String> uniprotIds = new ArrayList<>();
		final List<String> chebiIds = new ArrayList<>();
		for(String id : ids) {
			//Note: ChEBI IDs will be always with 'CHEBI:' prefix; see CPathUtils.getXrefIds impl.
			if(id.startsWith("CHEBI:")) {
				chebiIds.add(id);
			} else if(isoformIdPattern.matcher(id).find()) {
				//cut the isoform num. suffix
				uniprotIds.add(id.replaceFirst("-\\d+$", ""));
			} else if(uniprotIdPattern.matcher(id).find()) {
				uniprotIds.add(id);
			}
		}
		//id-mapping to find some other ids that map to the chebi/uniprot ones that we collected from the bpe.
		addSupportedIdsThatMapToChebi(chebiIds, ids);
		addSupportedIdsThatMapToUniprotId(uniprotIds, ids);
		for (String id : ids) {
			//index as: not analyzed, not tokenized; we use KeywordAnalyzer when searching this field...
			//doc.add(new StringField(FIELD_XREFID, id.toLowerCase(), Field.Store.NO));//todo: why did we have it? (ID is normally case-sensitive)
			doc.add(new StringField(FIELD_XREFID, id, Field.Store.NO));
			//also store a lower-case prefix (banana, e.g. 'chebi:1234' version of the id)
			if(StringUtils.contains(id,":")) {
				doc.add(new StringField(FIELD_XREFID,
						StringUtils.lowerCase(StringUtils.substringBefore(id, ":"))
						+ ":" + StringUtils.substringAfter(id, ":"), Field.Store.NO));
			}
		}

		// name (store both the original and lowercase names due to use of StringField and KeywordAnalyser)
		if(bpe instanceof Named) {
			Named named = (Named) bpe;
			if(named.getStandardName() != null) {
				String stdName = named.getStandardName().trim();
				doc.add(new StringField(FIELD_NAME, stdName.toLowerCase(), Field.Store.NO));
				doc.add(new StringField(FIELD_NAME, stdName, Field.Store.NO));
			}
			if(named.getDisplayName() != null && !named.getDisplayName().equalsIgnoreCase(named.getStandardName())) {
				String dspName = named.getDisplayName().trim();
				doc.add(new StringField(FIELD_NAME, dspName.toLowerCase(), Field.Store.NO));
				doc.add(new StringField(FIELD_NAME, dspName, Field.Store.NO));
			}
			for(String name : named.getName()) {
				if(!name.equalsIgnoreCase(named.getDisplayName()) && !name.equalsIgnoreCase(named.getStandardName())) {
					name = name.trim();
					doc.add(new StringField(FIELD_NAME, name.toLowerCase(), Field.Store.NO));
					doc.add(new StringField(FIELD_NAME, name, Field.Store.NO));
				}
				doc.add(new TextField(FIELD_KEYWORD, name.toLowerCase(), Field.Store.NO));
			}
		}

		// save/update the lucene document
		try {
			indexWriter.updateDocument(new Term(FIELD_URI, uri), doc);
		} catch (Exception e) {
			throw new RuntimeException("Failed to index: " + bpe.getUri(), e);
		}
	}

	@Override
	public void save(Model model) {
		setModel(model);
		final int numObjectsToIndex = model.getObjects(Entity.class).size()
				+ model.getObjects(EntityReference.class).size()
				+ model.getObjects(Provenance.class).size();
		LOG.info("index(), objects to save: " + numObjectsToIndex);
		final AtomicInteger numLeft = new AtomicInteger(numObjectsToIndex);
		for(BioPAXElement bpe : model.getObjects()) {
			if(bpe instanceof Entity || bpe instanceof EntityReference || bpe instanceof Provenance) {
				save(bpe);
				int left = numLeft.decrementAndGet();
				if (left % 10000 == 0) {
					commit();
					LOG.info("build(), objects to save: " + left);
				}
			}
		}
		commit();
		//force refreshing the index state (for new readers)
		refresh();
		LOG.info("build(), all done.");
	}

	@Override
	public void commit() {
		try {
			indexWriter.commit();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() {
		try {
			if (indexWriter != null && indexWriter.isOpen()) {
				indexWriter.commit();
				indexWriter.close();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public synchronized void refresh() {
		try {
			searcherManager.maybeRefreshBlocking();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void drop() {
		if(indexWriter==null) {
			throw new IllegalStateException("read-only index");
		}
		try {
			Query q = new FieldExistsQuery(FIELD_KEYWORD);
			indexWriter.deleteDocuments(q);
			indexWriter.commit();
			indexWriter.deleteUnusedFiles();
			setModel(null);
			LOG.info("dropped (deleted) BioPAX index");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void addDatasources(Set<Provenance> set, Document doc) {
		for (Provenance p : set) {
			doc.add(new TextField(FIELD_DATASOURCE, p.getUri(), Field.Store.YES));
			//index names (including the datasource identifier from metadata json config; see premerge/merge)
			//different data sources can have the same name e.g. 'intact'; tokenized - to search by partial name
			for (String s : p.getName()) {
				doc.add(new TextField(FIELD_DATASOURCE, s.toLowerCase(), Field.Store.NO));
			}
		}
	}

	private void addOrganisms(Set<BioSource> set, Document doc) {	
		for(BioSource bs : set) {
			doc.add(new TextField(FIELD_ORGANISM,  bs.getUri(), Field.Store.YES));
				
			// add organism names
			for(String s : bs.getName()) {
				doc.add(new TextField(FIELD_ORGANISM, s.toLowerCase(), Field.Store.NO));
			}
			// add taxonomy
			for(UnificationXref x : 
				new ClassFilterSet<>(bs.getXref(), UnificationXref.class)) {
				if(x.getId() != null) {
					doc.add(new TextField(FIELD_ORGANISM, x.getId().toLowerCase(), Field.Store.NO));
				}
			}
			// include tissue type terms
			if (bs.getTissue() != null) {
				for (String s : bs.getTissue().getTerm()) {
					doc.add(new TextField(FIELD_ORGANISM, s.toLowerCase(), Field.Store.NO));
				}
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
      final String uri = pw.getUri();
			//URI, index=yes, analyze=no, store=yes (this is to find child objects, participants or processes, by pathway URI/name/id)
      // we want searching by URI or its ending part (id) be case-sensitive
			doc.add(new StringField(FIELD_PATHWAY, uri, Field.Store.YES));
			//also, extract and index the last part of the uri (e.g., 'hsa00010' or 'r-hsa-201451')
			String id = uri.replaceAll(".*[/#:]", "").trim();
			doc.add(new StringField(FIELD_PATHWAY, id, Field.Store.NO));
			// add names to the 'pathway' (don't store); will be case-insensitive (if using StandardAnalyser)
			// (this allows to find a biopax element, e.g., protein, by a parent pathway name: pathway:<query_str>)
			for (String s : pw.getName()) {
				doc.add(new StringField(FIELD_PATHWAY, s.toLowerCase(), Field.Store.NO));
			}
			// add unification xref IDs too (case-sensitive)
			for (UnificationXref x : new ClassFilterSet<>(pw.getXref(), UnificationXref.class)) {
				if (x.getId() != null) {
					doc.add(new StringField(FIELD_PATHWAY, x.getId().trim(), Field.Store.NO));
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
		//AND type (including all biopax subtypes)
		if(type != null) { //add biopax class filter
			BooleanQuery.Builder query = new BooleanQuery.Builder().
				add(new TermQuery(new Term(FIELD_TYPE, type.getSimpleName().toLowerCase())), Occur.SHOULD);//OR
			//also add all biopax subclasses of the type
			for(Class<? extends BioPAXElement> subType : SimpleEditorMap.L3.getKnownSubClassesOf(type)) {
				query.add(new TermQuery(new Term(FIELD_TYPE, subType.getSimpleName().toLowerCase())), Occur.SHOULD);//OR
			}		
			builder.add(query.build(), Occur.MUST);
		}

		BooleanQuery filter = builder.build();
		return (filter==null || filter.clauses().isEmpty()) ? null : filter;
	}

	/*
	 * Values are joint with OR, but if a value
	 * has whitespace symbols, it also makes a sub-query,
	 * in which terms are joint with AND. This is to filter
	 * by datasource/organism's full name, partial name, uri,
	 * using multiple datasources/organisms.
	 * For example, 
	 * "search?q=*&datasource=intact complex&type..." - will get only IntAct Complex objects;
	 * "search?q=*&datasource=intact&type..." - will consider both IntAct and IntAct Complex
	 * "search?q=*&datasource=intact biogrid&type..." means "to occur in both intact and biogrid" 
	 *  (can be a canonical protein reference; it is not equivalent to "search?q=*&datasource=intact&datasource=biogrid&...",
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
						//common words, e.g. 'of', 'and', 'for' won't occur as tokens (see the analyzer constructor arg)
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

	private void addSupportedIdsThatMapToChebi(List<String> chebiIds, final Set<String> resultIds) {
		//find other IDs that map to the ChEBI ID
		for(String id: chebiIds) {
			List<Mapping> mappings = findByDstDbIgnoreCaseAndDstId("CHEBI", id);
			if (mappings != null) {
				//collect (for 'xrefid' full-text index field) only ID types that we want biopax graph queries support
				for (Mapping mapping : mappings) {
					if (mapping.getSrcDb().equals("PUBCHEM-COMPOUND")
							|| mapping.getSrcDb().equals("CHEBI")
							|| mapping.getSrcDb().equals("DRUGBANK")
							|| mapping.getSrcDb().startsWith("KEGG")
							|| mapping.getSrcDb().startsWith("CHEMBL")
							|| mapping.getSrcDb().startsWith("PHARMGKB")
					) resultIds.add(mapping.getSrcId());
					//(prefix 'CID:' is included in pubchem-compound ids)
				}
			}
		}
	}

	private void addSupportedIdsThatMapToUniprotId(List<String> uniprotIds, final Set<String> resultIds) {
		//find other IDs that map to the UniProt AC
		for(String id: uniprotIds) {
			List<Mapping> mappings = findByDstDbIgnoreCaseAndDstId("UNIPROT", id);
			if (mappings != null) {
				//collect (for 'xrefid' full-text index field) only ID types that we want graph queries support
				for (Mapping mapping : mappings) {
					if (mapping.getSrcDb().startsWith("UNIPROT")
							|| mapping.getSrcDb().startsWith("HGNC")
							|| mapping.getSrcDb().equalsIgnoreCase("NCBI GENE")
							|| mapping.getSrcDb().equalsIgnoreCase("REFSEQ")
							|| mapping.getSrcDb().equalsIgnoreCase("IPI")
							|| mapping.getSrcDb().startsWith("ENSEMBL")
					) resultIds.add(mapping.getSrcId());
				}
			}
		}
	}

	public Model getModel() {
		return model;
	}

	public void setModel(Model model) {
		this.model = model;
	}

	@Override
	public List<Mapping> findByDstDbIgnoreCaseAndDstId(String dstDb, String dstId) {
		return mapBy(FIELD_DSTDB, dstDb, FIELD_DSTID, dstId);
	}

	@Override
	public List<Mapping> findBySrcIdInAndDstDbIgnoreCase(List<String> srcIds, String dstDb) {
		List<Mapping> mappings = new ArrayList<>();
		//query for docs that match any of the srcIds
		BooleanQuery.Builder anyId = new BooleanQuery.Builder();
		for(String id : srcIds) {
			anyId.add(new TermQuery(new Term(FIELD_SRCID, id)), Occur.SHOULD);
		}
		//query for docs that match any of srcIds AND the dstDb
		Query q = new BooleanQuery.Builder()
			.add(new TermQuery(new Term(FIELD_DSTDB, dstDb.toUpperCase())), Occur.MUST)
			.add(anyId.build(), Occur.MUST)
			.build();
		IndexSearcher searcher = null;
		try {
			searcher = searcherManager.acquire();
			int nHits = searcher.count(q);
			if(nHits > 0) {
				TopDocs topDocs = searcher.search(q, nHits);
				for (ScoreDoc sd : topDocs.scoreDocs) {
					Document d = searcher.doc(sd.doc);
					Mapping m = new Mapping(d.get(FIELD_SRCDB), d.get(FIELD_SRCID), d.get(FIELD_DSTDB), d.get(FIELD_DSTID));
					mappings.add(m);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				searcherManager.release(searcher);
			} catch (IOException e) {}
		}
		return mappings;
	}

	private List<Mapping> mapBy(String fieldDb, String db, String fieldId, String id) {
		List<Mapping> mappings = new ArrayList<>();
		//query for docs that match the src/dst db and id
		Query q = new BooleanQuery.Builder()
				.add(new TermQuery(new Term(fieldDb, db.toUpperCase())), Occur.MUST)
				.add(new TermQuery(new Term(fieldId, id)), Occur.MUST)
				.build();
		IndexSearcher searcher = null;
		try {
			searcher = searcherManager.acquire();
			int nHits = searcher.count(q);
			if(nHits > 0) {
				TopDocs topDocs = searcher.search(q, nHits);
				for (ScoreDoc sd : topDocs.scoreDocs) {
					Document d = searcher.doc(sd.doc);
					Mapping m = new Mapping(d.get(FIELD_SRCDB), d.get(FIELD_SRCID), d.get(FIELD_DSTDB), d.get(FIELD_DSTID));
					mappings.add(m);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				searcherManager.release(searcher);
			} catch (IOException e) {}
		}
		return mappings;
	}

	@Override
	public void save(Mapping mapping) {
		final Document doc = new Document();
		doc.add(new StringField(FIELD_SRCDB, mapping.getSrcDb().toUpperCase(), Field.Store.YES));
		doc.add(new StringField(FIELD_SRCID, mapping.getSrcId(), Field.Store.YES));
		doc.add(new StringField(FIELD_DSTDB, mapping.getDstDb().toUpperCase(), Field.Store.YES));
		doc.add(new StringField(FIELD_DSTID, mapping.getDstId(), Field.Store.YES));
		doc.add(new StringField(FIELD_DOCID, mapping.docId(), Field.Store.NO));
		doc.add(new StringField(FIELD_TYPE, "mapping", Field.Store.NO));
		try {
			indexWriter.updateDocument(new Term(FIELD_DOCID, mapping.docId()), doc);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		//call commit(), refresh() after several save(mapping)
	}
}
