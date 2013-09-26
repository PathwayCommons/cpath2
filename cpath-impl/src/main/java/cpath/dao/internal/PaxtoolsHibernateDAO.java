// $Id$
//------------------------------------------------------------------------------
/** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center.
 **
 ** This library is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** Memorial Sloan-Kettering Cancer Center
 ** has no obligations to provide maintenance, support,
 ** updates, enhancements or modifications.  In no event shall
 ** Memorial Sloan-Kettering Cancer Center
 ** be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** Memorial Sloan-Kettering Cancer Center
 ** has been advised of the possibility of such damage.  See
 ** the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this library; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 **/
package cpath.dao.internal;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.util.Version;

import static org.biopax.paxtools.impl.BioPAXElementImpl.*;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.PathwayStep;
import org.biopax.paxtools.util.IllegalBioPAXArgumentException;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.controller.ObjectPropertyEditor;
import org.biopax.paxtools.controller.PathAccessor;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.impl.BioPAXElementImpl;
import org.biopax.paxtools.impl.level3.L3ElementImpl;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.hibernate.*;
import org.hibernate.criterion.Projections;
import org.hibernate.search.*;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;
import org.springframework.util.Assert;

import cpath.config.CPathSettings;
import cpath.dao.Analysis;
import cpath.dao.PaxtoolsDAO;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.TraverseEntry;
import cpath.service.jaxb.TraverseResponse;
import java.util.*;
import java.io.*;
import java.lang.reflect.Modifier;

/**
 * Paxtools BioPAX main Model and DAO (BioPAX objects repository)
 * 
 * This is one of most important classes on which cpath2 system is based.
 * 
 */
@Repository
class PaxtoolsHibernateDAO implements Model, PaxtoolsDAO
{
	private static final long serialVersionUID = 1L;
	
	//no. updated/saved objects before flushing the session
	private final static int BATCH_SIZE = 20;
	
	//no. indexed objects before flushing the full-text session
	private final static int IDX_BATCH_SIZE = 10000;
	
	private int maxHitsPerPage;
	
	public final static String[] DEFAULT_SEARCH_FIELDS =
		{
			// auto-generated index fields (from the annotations in paxtools-core)
			FIELD_AVAILABILITY,
			FIELD_COMMENT, // biopax comments
			FIELD_KEYWORD, //anything, e.g., names, terms, comments, incl. - from child elements 
			FIELD_NAME, // standardName, displayName, other names
			FIELD_TERM, // CV terms
			FIELD_XREFDB, //xref.db
			FIELD_XREFID, //xref.id (incl. direct child xref's id, if any)
			FIELD_ECNUMBER,
			FIELD_SEQUENCE,
			// plus, filter fields (TODO experiment with not including these fields into default search fields; use for filtering only)
			FIELD_ORGANISM,
			FIELD_DATASOURCE,
			FIELD_PATHWAY, // i.e., helps find an object by a parent pathway name or filter a search results by pathway ;) 
		};

	private static Logger log = LoggerFactory.getLogger(PaxtoolsHibernateDAO.class);
	private SessionFactory sessionFactory;
	private final Map<String, String> nameSpacePrefixMap;
	private final BioPAXLevel level;
	private final BioPAXFactory factory;
	private SimpleIOHandler simpleIO;
	private boolean addDependencies = false;
	private final String xmlBase;

	
	protected PaxtoolsHibernateDAO()
	{
		this.level = BioPAXLevel.L3;
		this.factory = level.getDefaultFactory();
		// no namespace!
		this.nameSpacePrefixMap = new HashMap<String, String>();
		this.simpleIO = new SimpleIOHandler(BioPAXLevel.L3);
		this.simpleIO.mergeDuplicates(true);
		this.simpleIO.normalizeNameSpaces(false);
		//may (recommended) or may not always use absolute URIs when writing RDF/XML
		this.simpleIO.absoluteUris(Boolean.parseBoolean(
			(CPathSettings.property(CPathSettings.PROP_ABSOLUTE_URI_ENABLED)))); 
		//- seems, - query parser turns search keywords to lower case and expects index field values are also lower case...		
		this.xmlBase = CPathSettings.xmlBase(); //set default xml:base
		this.maxHitsPerPage = Integer.parseInt(CPathSettings.property(CPathSettings.PROP_MAX_SEARCH_HITS_PER_PAGE));
	}


	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public BioPAXIOHandler getReader() {
		return simpleIO;
	}
	

	@Transactional
	public void importModel(File biopaxFile) throws FileNotFoundException
	{
		if (log.isInfoEnabled()) {
			log.info("Creating biopax model using: " + biopaxFile.getAbsolutePath());
		}

		// convert file to model
		Model m = simpleIO.convertFromOWL(new FileInputStream(biopaxFile));
		merge(m);
	}


	/**
	 * {@inheritDoc}
	 * 
	 * Not annotated as transactional, because
	 * it manually opens own internal transaction
	 * using stateless session.
	 * 
	 */
	@Override
	public void insert(final Model model)
	{
		/* 
		 * Level3 property/propertyOf ORM mapping and getters/setters 
		 * are to be properly implemented for this to work.
		 * Persistence annotations must move to new setter/getter pair 
		 * that do NOT call inverse prop. 'add' methods in the setter.
		 */
		if(model != null && !model.getObjects().isEmpty()) {
			// First, insert new elements using a stateless session!
			log.debug("insert(model): inserting BioPAX elements (stateless)...");
			
			int i = 0;
			StatelessSession stls = sessionFactory.openStatelessSession();
			Query q = stls.getNamedQuery("org.biopax.paxtools.impl.BioPAXElementExists");
			stls.createSQLQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
			
			Transaction tx = stls.beginTransaction();
			for (BioPAXElement bpe : model.getObjects()) {
				boolean exists = 
					q.setString("md5uri", ((BioPAXElementImpl)bpe).getPk()).uniqueResult() != null;
				if(!exists) {
					stls.insert(bpe);
					i++;
				}
			}
			
			tx.commit();
			stls.close();
			
			log.debug("insert(model): inserted " + i + " objects");
		}
	}
	
	
	/** 
	 * 
	 * This opens a new stateless session, which starts a new 
	 * transaction to insert new objects (by URI).
	 * 
	 */
	@Transactional
	@Override
	public void merge(final Model model)
	{
		//clear all caches
		evictCaches();
		
		// insert all using a stateless session
		insert(model);	
		
		// update relationships and values using 
		// a new transaction (and stateful session)
		log.debug("merge(model): starting update(model) - a new transaction...");
		update(model);
	}

	
	/**
	 * 
	 * @throws HibernateException if an object rerefs to not saved objects
	 */
	@Transactional
	public void update(final Model model) 
	{	
		Session ses = sessionFactory.getCurrentSession();
		
		int i = 0;
		for (BioPAXElement bpe : model.getObjects()) {			
			// save/update
			try {
				ses.merge(bpe);
			} catch (Exception e) {
				throw new IllegalBioPAXArgumentException("update(model): Failed merging: " + bpe.getRDFId(), e);
			}
			
			i++;
			if(i % BATCH_SIZE == 0) {
				ses.flush();
				ses.clear();
				log.debug("update(model): saved " + i);
			}
		}
		ses.flush();
		ses.clear();
		
		log.info("update(model): total merged " + i + " objects");
	}
	
	
	/**
	 * Updates or persists the object. 
	 * 
	 * Unless cascades are properly enabled in the model ORM,
	 * this method will probably fail if child elements 
	 * (values of BioPAX object properties and inverse
	 * properties and their childs, etc..)
	 * were not persisted already. If so, consider making a 
	 * (self-consistent) model that contains all children and
	 * some parents (where there an inverse property is involved)
	 * and merging it with {@link #merge(Model)} method instead.
	 * 
	 * 
	 * @throws HibernateException if the objects refers to unsaved objects
	 */
	@Transactional
	public void merge(BioPAXElement aBioPAXElement) {
		String rdfId = aBioPAXElement.getRDFId();
		
		if (log.isDebugEnabled())
			log.debug("merge(aBioPAXElement): merging " + rdfId + " " 
				+ aBioPAXElement.getModelInterface().getSimpleName());
	
		sessionFactory.getCurrentSession().merge(aBioPAXElement);
	}

	
	/**
	 * 
	 * @throws IllegalArgumentException if query is null
	 */
	@Transactional(readOnly=true) 
	@Override
	public SearchResponse search(String query, int page,
			Class<? extends BioPAXElement> filterByType, String[] dsources,
			String[] organisms) 
	{
		Assert.notNull(query, "Search string cannot be NULL");
		
		// collect matching elements here
		SearchResponse searchResponse = new SearchResponse();
		searchResponse.setMaxHitsPerPage(maxHitsPerPage);

		if (log.isDebugEnabled())
			log.debug("search: " + query + ", page: " + page 
					+ ", filterBy: " + filterByType
					+ "; extra filters: ds in (" + Arrays.toString(dsources)
					+ "), org. in (" + Arrays.toString(organisms) + ")");


		Session session = sessionFactory.getCurrentSession();
		// create a new full text session from current session
		FullTextSession fullTextSession = Search.getFullTextSession(session);
		
		//set read-only for all entities
		fullTextSession.setDefaultReadOnly(true);
		
		// lucene query builder below does not understand interfaces and abstract types...
//		Class<?> filterClass = (filterByType != null) ? filterByType : BioPAXElement.class;
		
		// convert the filter query class into concrete BioPAX persistent class
		Class<?> filterClass = getEntityClass(filterByType);
		assert filterClass != null; //not null, as BioPAXelementImpl.class is the default fall-back
		
		// build a new Lucene query
		org.apache.lucene.search.Query luceneQuery = null;
		// "*" query special case (find all) -
		if(query != null && query.equals("*")) {		
			if(Modifier.isAbstract(filterClass.getModifiers()) 
					|| !filterClass.isAnnotationPresent(Indexed.class)) 
				throw new IllegalBioPAXArgumentException("With '*' (all) search query, " +
					"one must use 'type=' filter with a specific, instantiable BioPAX type");
			
			QueryBuilder queryBuilder = fullTextSession.getSearchFactory()
				.buildQueryBuilder().forEntity(filterClass).get();
			
			luceneQuery = queryBuilder.all().createQuery();
		} else {
			MultiFieldQueryParser multiFieldQueryParser = new MultiFieldQueryParser(
					Version.LUCENE_36, DEFAULT_SEARCH_FIELDS, new StandardAnalyzer(Version.LUCENE_36));
			try {
				luceneQuery = multiFieldQueryParser.parse(query);
			} catch (ParseException e) {
				log.error("parser exception: " + e.getMessage());
				throw new IllegalBioPAXArgumentException("search: query parser error, " +
						"query: '"+query+"'", e);
			}
		}

		// create a full-text query from the Lucene one
		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery(luceneQuery, filterClass);

		// set read-only
		fullTextQuery.setReadOnly(true);

		// use Projection (allows extracting/highlighting matching text, etc.)
		if (CPathSettings.explainEnabled())
			fullTextQuery.setProjection(FullTextQuery.THIS, FullTextQuery.DOCUMENT,
					FullTextQuery.SCORE, FullTextQuery.EXPLANATION);
		else
			fullTextQuery.setProjection(FullTextQuery.THIS, FullTextQuery.DOCUMENT);

		// enable filters
		if (dsources != null && dsources.length > 0)
			fullTextQuery.enableFullTextFilter(FILTER_BY_DATASOURCE)
					.setParameter("values", dsources);
		if (organisms != null && organisms.length > 0)
			fullTextQuery.enableFullTextFilter(FILTER_BY_ORGANISM)
					.setParameter("values", organisms);

		// get hits count (cheap operation)
		int count = fullTextQuery.getResultSize(); 
		log.debug("Query '" + query + "', results size = " + count);
		
		searchResponse.setNumHits(count);
		searchResponse.setMaxHitsPerPage(maxHitsPerPage);
		searchResponse.setPageNo(page);
		
		// now consider pagination
		int startWith = page * maxHitsPerPage;
		//do unless there are no hits or the page no. was too large (i.e., startWith >= count)
		if(count > 0 && startWith < count) {
			//skip up to the first desired one
			fullTextQuery.setFirstResult(startWith);
			// set max no. hits to retrieve
			fullTextQuery.setMaxResults(maxHitsPerPage);
			
			// create/init the highlighter and transformer
			Highlighter highlighter = null;
			if(!query.equals("*")) {
				// create a highlighter (store.YES must be enabled on 'keyword' search field, etc.!)
				//TODO shall I rewrite the luceneQuery?
				QueryScorer scorer = new QueryScorer(luceneQuery, FIELD_KEYWORD);   
				SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<span class='hitHL'>", "</span>");
				highlighter = new Highlighter(formatter, scorer);
				highlighter.setTextFragmenter(new SimpleSpanFragmenter(scorer, 80));
			}
			// set the result to search hit beans transformer
			fullTextQuery.setResultTransformer(new SearchHitsTransformer(highlighter));
						
			// do search and get (auto-transformed) hits
			List<SearchHit> searchHits = (List<SearchHit>) fullTextQuery.list();
			
			searchResponse.setSearchHit(searchHits);
		} 
		
		return searchResponse;
	}
	
	
	@Override
	@Transactional
	public void add(BioPAXElement aBioPAXElement)
	{
		String rdfId = aBioPAXElement.getRDFId();		
		
		if (!level.hasElement(aBioPAXElement))
		{
			throw new IllegalBioPAXArgumentException(
					"Given object is of wrong level");
		}
		else if (rdfId == null)
		{
			throw new IllegalBioPAXArgumentException(
					"null ID: every object must have an RDF ID");
		}
		else
		{
			log.debug("adding/persisting new " + rdfId + " " 
				+ aBioPAXElement.getModelInterface().getSimpleName());
			sessionFactory.getCurrentSession().persist(aBioPAXElement); 
		}
	}


	@Override
	@Transactional
	public void remove(BioPAXElement aBioPAXElement)
	{
		BioPAXElement bpe = getByID(aBioPAXElement.getRDFId());
		sessionFactory.getCurrentSession().delete(bpe); // affects many elements, because of cascade=ALL!
	}


	@Override
	@Transactional
	public <T extends BioPAXElement> T addNew(Class<T> type, String id)
	{
		T bpe = factory.create(type, id);
		add(bpe);
		return bpe;
	}


	/**
	 * Returns true iif the DB contains a biopax object with the same URI,
	 * and that object is equivalent (same biopax class and eqv. properties) 
	 * to the query one. These two objects are not necessarily equal.
	 * 
	 */
	@Transactional(readOnly=true)
	@Override
	public boolean contains(BioPAXElement bpe)
	{
		return containsID(bpe.getRDFId()) 
			&& bpe.isEquivalent(getByID(bpe.getRDFId()));
	}


	/**
	 * Note:
	 * Contains ID 'true' does not necessarily mean the object is ready to load 
	 * at the moment, i.e., it could have been just inserted with a stateless session,
	 * and not updated (relationships not saved)
	 */
	@Transactional(readOnly=true)
	@Override
	public boolean containsID(String id)
	{
		boolean ret;

		Session ses = sessionFactory.getCurrentSession();
		String pk = 
			(CPathSettings.digestUriEnabled()) ? id : ModelUtils.md5hex(id);
		
		ret = (ses.getNamedQuery("org.biopax.paxtools.impl.BioPAXElementExists")
				.setString("md5uri", pk).uniqueResult() != null);
		
		return ret;
	}


	@Override
	@Transactional
	public BioPAXElement getByID(String id) 
	{
		if(id == null || "".equals(id)) 
			throw new IllegalArgumentException("getByID: id cannot be null or empty string");
		
		Session ses = sessionFactory.getCurrentSession();
		
		BioPAXElement toReturn = (BioPAXElement) ses
			.get(BioPAXElementImpl.class, ModelUtils.md5hex(id));
		
		// For db debug mode only (when md5hex.uri.enabled=true), try "as is" 
		// (i.e., if the id is already the precomputed MD5 hex pK value)
		if(toReturn == null && CPathSettings.digestUriEnabled())
			toReturn = (BioPAXElement) ses.get(BioPAXElementImpl.class, id);

		return toReturn; // null means no such element
	}
	
	
	@Override
	public BioPAXLevel getLevel()
	{
		return level;
	}


	@Override
	public Map<String, String> getNameSpacePrefixMap()
	{
		return Collections.unmodifiableMap(nameSpacePrefixMap);
	}


	@Transactional
	@Override
	public Set<BioPAXElement> getObjects()
	{
		return getObjects(BioPAXElement.class);
	}


	@Transactional
	@Override
	public <T extends BioPAXElement> Set<T> getObjects(Class<T> clazz)
	{
		String query = "from " + clazz.getCanonicalName();
		List<T> results = sessionFactory.getCurrentSession().createQuery(query).list();
		Set<T> toReturn = new HashSet<T>(results);
		return toReturn;
	}


	@Override
	public boolean isAddDependencies() {
		return addDependencies;
	}


	@Override
	public void setAddDependencies(boolean addDependencies)
	{
		this.addDependencies = addDependencies;
	}


	@Override
	public void setFactory(BioPAXFactory factory)
	{
		throw new UnsupportedOperationException(
			"Internal BioPAX Factory cannot be modified.");//would be unsafe!
	}

	
	/**
	 * Gets a Hibernate annotated entity class 
	 * (implementation) by BioPAX Model interface class 
	 * 
	 */
	Class<? extends BioPAXElement> getEntityClass(
			Class<? extends BioPAXElement> filterBy) 
	{
		Class<? extends BioPAXElement> clazz = 
			(filterBy == null || BioPAXElement.class.equals(filterBy))
				? BioPAXElementImpl.class : factory.getImplClass(filterBy);
		
		if(clazz == null) {
			clazz = BioPAXElementImpl.class; // fall-back (to no filtering)
			log.error("Expected argument: a BioPAX interface"
					+ "; actual argument: " + filterBy.getCanonicalName());
		}

		return clazz;
	}


	/**
	 * When an non-empty list of IDs (RDFId, URI) is provided,
	 * it will use {@link SimpleIOHandler#convertToOWL(Model, OutputStream, String...)}
	 * method to extract listed elements (with all children and properties)model
	 * and write as RDF/XML to the output stream.
	 */
	@Transactional(readOnly=true)
	@Override
	public void exportModel(OutputStream outputStream, String... ids) 
	{
		Session ses = sessionFactory.getCurrentSession();
		ses.setDefaultReadOnly(true);
//      ses.enableFetchProfile("mul_properties_join");
        simpleIO.convertToOWL(this, outputStream, ids);
//      ses.disableFetchProfile("mul_properties_join");
	}
	
	
	/* 
	 * All object collection properties and inverse properties 
	 * (not very deep) initialization
	 */
	@Transactional
	@Override
	public void initialize(Object obj) 
	{
		if(obj instanceof BioPAXElement) {		
			//just re-associate:
			sessionFactory.getCurrentSession().buildLockRequest(LockOptions.NONE).lock(obj);
//			Hibernate.initialize(obj);	
			BioPAXElement element = (BioPAXElement) obj;

			// init. biopax properties
			Set<PropertyEditor> editors = simpleIO.getEditorMap()
				.getEditorsOf(element);
			if (editors != null) {
				for (PropertyEditor editor : editors) {
					Set<?> value = editor.getValueFromBean(element);
//					Hibernate.initialize(value);
					for(Object v : value) {
						if(v instanceof Collection)
							Hibernate.initialize(v); 
					}
				}
			}

			// init. inverse object properties (xxxOf)
			Set<ObjectPropertyEditor> invEditors = simpleIO.getEditorMap()
				.getInverseEditorsOf(element);
			if (invEditors != null) {
				for (ObjectPropertyEditor editor : invEditors) {
					// does collections as well!
					Set<?> value = editor.getInverseAccessor().getValueFromBean(element);
//					Hibernate.initialize(value);
					//values the set can be biopax elements or sets of BPEs only
					for(Object v : value) {
						if(v instanceof Collection)
							Hibernate.initialize(v); 
					}
				}
			}
		}
	}

	
	/**
	 * {@inheritDoc}
	 * 
	 * Runs within a new read/write transaction 
	 * (can modify and save entity states).
	 * 
	 */
	@Override
	@Transactional(readOnly=false)
	public void run(Analysis analysis) {
		log.debug("run: started");
		analysis.execute(this);
		log.debug("run: finished");
	}

	
	/**
	 * {@inheritDoc}
	 * 
	 * Executes a graph query within a read-only 
	 * (- set on the class level) transaction.
	 * 
	 */
	@Transactional(readOnly=true)
	@Override
	public void runReadOnly(Analysis analysis) {
		Session ses = sessionFactory.getCurrentSession();
		ses.setDefaultReadOnly(true);
//		ses.enableFetchProfile("mul_properties_join");
//		ses.enableFetchProfile("inverse_mul_properties_join");	
		
		log.debug("runReadOnly: started");
		
		// perform the analysis algorithm
		analysis.execute(this);		
		
		log.debug("runReadOnly: finished");
		
//		ses.disableFetchProfile("mul_properties_join");
//		ses.disableFetchProfile("inverse_mul_properties_join");
	}

	
	@Override
	public void replace(BioPAXElement existing, BioPAXElement replacement) {
		throw new UnsupportedOperationException("not supported"); //TODO if required...
	}

	
	@Override
	public void repair() {
		throw new UnsupportedOperationException("not supported");
	}
	
	
	@Override
	public void setXmlBase(String base) {
		throw new UnsupportedOperationException("Unmodifiable xml:base is: " + xmlBase);
	}

	
	@Override
	public String getXmlBase() {
		return xmlBase;
	}


	/**
	 * It generates results only for those URIs where
	 * the property path apply, although the values set 
	 * can be empty.
	 * 
	 * @throws 
	 */
	@Transactional(readOnly=true)
	@Override
	public TraverseResponse traverse(String propertyPath, String... uris) {
		
		sessionFactory.getCurrentSession().setDefaultReadOnly(true);
		
		TraverseResponse resp = new TraverseResponse();
		resp.setPropertyPath(propertyPath);
		
		PathAccessor pathAccessor = null; 
		try {
			pathAccessor = new PathAccessor(propertyPath, getLevel());
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to parse " +
				"the BioPAX property path: " + propertyPath, e);
		}
		
		for(String uri : uris) {
			BioPAXElement bpe = getByID(uri);
			try {
				Set<?> v = pathAccessor.getValueFromBean(bpe);
				TraverseEntry entry = new TraverseEntry();
				entry.setUri(uri);
				if(!pathAccessor.isUnknown(v)) {
//					entry.getValue().addAll(v);
					for(Object o : v) {
						if(o instanceof BioPAXElement) 
							entry.getValue().add(((BioPAXElement) o).getRDFId());
						else
							entry.getValue().add(String.valueOf(o));
					}
				}
				// add (it might have no values, but the path is correct)
				resp.getTraverseEntry().add(entry); 
			} catch (IllegalBioPAXArgumentException e) {
				// log, ignore if the path does not apply
				if(log.isDebugEnabled())
					log.debug("Failed to get values at: " + 
						propertyPath + " from the element: " + uri, e);
			}
		}
		
		return resp;
	}

	
	/* 
	 Hibernate MassIndexer sucks. 
	 It throws "lazy" or "no session" exceptions, despite I tried many things, such as -
	 EAGER via ORM annotations (in paxtools-core), fetch profile owerride (JOIN), etc.
	 Despite all attempts, it seems cannot handle fetch profiles/sessions/transactions properly... 
	 It worked only for very simple search fields and field bridges that do not require any deep traversing 
	 into the object graph in order to get values for the index field (e.g., get pathways,
	 ogranisms, datasources)
	*/
	@Transactional
	public void index() {
		Session ses = sessionFactory.getCurrentSession();	
		FullTextSession fullTextSession = Search.getFullTextSession(ses);
		fullTextSession.setFlushMode(FlushMode.MANUAL);
		Number numRows = (Number) fullTextSession.createCriteria(L3ElementImpl.class)
			.setProjection(Projections.rowCount()).uniqueResult();
		int total = numRows.intValue();
		log.info("index(), there are " + total + " biopax objects to be indexed.");
		List<String> keys = fullTextSession.createCriteria(L3ElementImpl.class)
			.setProjection(Projections.id()).list();		
		int i = 0; // a local counter to determine when periodically flush/clear
		for(String key : keys ) {				
			L3ElementImpl bpe = (L3ElementImpl) fullTextSession.get(L3ElementImpl.class, key);
//			log.debug("index(), " + bpe.getRDFId());
			
			// add data type property values from up to 3nd-level child elements.
			bpe.getKeywords().addAll(ModelUtils.getKeywords(bpe, 3));
			// Infer/associate datasources for some utility class objects
			// (Entity class instances have @Field annotated 'dataSource' 
			// property and will be indexed without this additional step)
			if(bpe instanceof EntityReference || bpe instanceof PathwayStep)
				bpe.getDatasources().addAll(ModelUtils.getDatasources(bpe));
			// Infer/associate organisms with all Entity and EntityReference objects
			// (we want to do this for some of biopax types that do not have 
			// 'organism' property)
			if(bpe instanceof Entity || bpe instanceof EntityReference)
				bpe.getOrganisms().addAll(ModelUtils.getOrganisms(bpe));
			
			// infer parent pathways
			if(bpe instanceof Entity || bpe instanceof EntityReference || bpe instanceof PathwayStep)
				bpe.getParentPathways().addAll(ModelUtils.getParentPathways(bpe));
			
		    fullTextSession.index( bpe );
		    
		    i++;
		    if (i % IDX_BATCH_SIZE == 0) {
		    	//apply changes to indexes
		        fullTextSession.flushToIndexes();
		        //free memory since the queue is processed
		        fullTextSession.clear(); 
				log.info("index(), indexed: " + i);
		    }
		}
		
		fullTextSession.flushToIndexes();
		fullTextSession.clear();
		
		log.info("index(), done.");
	}
	
	
	@Override
	@Transactional
	public void evictCaches() {
		sessionFactory.getCache().evictEntityRegions();
		sessionFactory.getCache().evictCollectionRegions();
		sessionFactory.getCache().evictDefaultQueryRegion();
	}

}
