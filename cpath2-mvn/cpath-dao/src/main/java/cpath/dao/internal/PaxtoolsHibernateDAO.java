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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.util.Version;
import org.biopax.paxtools.impl.BioPAXElementImpl;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.util.IllegalBioPAXArgumentException;
import org.biopax.paxtools.controller.*;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.hibernate.*;
import org.hibernate.search.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;

import cpath.dao.Analysis;
import cpath.dao.PaxtoolsDAO;
import cpath.dao.filters.SearchFilter;
import cpath.dao.filters.SearchFilterRange;
import cpath.warehouse.internal.WarehousePaxtoolsHibernateDAO;

import java.util.*;
import java.io.*;

import static org.biopax.paxtools.impl.BioPAXElementImpl.*;


/**
 * Paxtools BioPAX main Model and DAO
 * (not a warehouse)
 *
 */
@Transactional
@Repository
public class PaxtoolsHibernateDAO implements PaxtoolsDAO
{
	private static final long serialVersionUID = 1L;

	public final static String[] ALL_FIELDS =
		{
			SEARCH_FIELD_AVAILABILITY,
			SEARCH_FIELD_COMMENT,
			SEARCH_FIELD_KEYWORD,
			SEARCH_FIELD_NAME,
			SEARCH_FIELD_TERM,
			SEARCH_FIELD_XREF_DB,
			SEARCH_FIELD_XREF_ID,
			// not used/exist -
			//SEARCH_FIELD_ID, // do NOT search in RDFId!
		};

	private static Log log = LogFactory.getLog(PaxtoolsHibernateDAO.class);
	private SessionFactory sessionFactory;
	private final Map<String, String> nameSpacePrefixMap;
	private final BioPAXLevel level;
	private final BioPAXFactory factory;
	protected SimpleIOHandler simpleIO;
	private boolean addDependencies = false;
	protected MultiFieldQueryParser multiFieldQueryParser;

	protected PaxtoolsHibernateDAO()
	{
		this.level = BioPAXLevel.L3;
		this.factory = level.getDefaultFactory();
		// no namespace!
		this.nameSpacePrefixMap = new HashMap<String, String>();
		//this.nameSpacePrefixMap.put("", "urn:biopax:");
		this.simpleIO = new SimpleIOHandler(BioPAXLevel.L3);
		this.simpleIO.mergeDuplicates(true);
		this.simpleIO.normalizeNameSpaces(false);
		this.multiFieldQueryParser = new MultiFieldQueryParser(
			Version.LUCENE_29, ALL_FIELDS, 
				new StandardAnalyzer(Version.LUCENE_29));
	}

	// get/set methods used by spring
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public BioPAXIOHandler getReader() {
		return simpleIO;
	}


	protected Session session() {
		return sessionFactory.getCurrentSession();
	}
	

	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public void createIndex() {
		FullTextSession fullTextSession = Search.getFullTextSession(session());
		if(log.isInfoEnabled())
			log.info("Begin indexing...");
		/* - often gets stuck or crashes...
		try {
			fullTextSession.createIndexer()
				.purgeAllOnStart(true)
				//.batchSizeToLoadObjects( 10 )
				//.threadsForSubsequentFetching( 4 )
				//.threadsToLoadObjects( 2 )
				//.cacheMode(CacheMode.NORMAL) // defaults to CacheMode.IGNORE
				.startAndWait();
		} catch (InterruptedException e) {
			throw new RuntimeException("Index re-build is interrupted.");
		}
		*/
		
		// manually re-index
		final int BATCH_SIZE = 100;
		fullTextSession.setFlushMode(FlushMode.MANUAL);
		fullTextSession.setCacheMode(CacheMode.IGNORE);
		//Transaction transaction = fullTextSession.beginTransaction();
		//Scrollable results will avoid loading too many objects in memory
		ScrollableResults results = fullTextSession.createCriteria( BioPAXElementImpl.class )
		    .setFetchSize(BATCH_SIZE)
		    .scroll( ScrollMode.FORWARD_ONLY );
		int index = 0;
		while( results.next() ) {
		    index++;
		    fullTextSession.index( results.get(0) ); //index each element
		    if (index % BATCH_SIZE == 0) {
		        fullTextSession.flushToIndexes(); //apply changes to indexes
		        fullTextSession.clear(); //free memory since the queue is processed
		    }
		}
		//transaction.commit();
		if(log.isInfoEnabled())
			log.info("Ended indexing.");
	}

	//not transactional (but it's 'merge' method that creates a new transaction)
	public void importModel(File biopaxFile) throws FileNotFoundException
	{
		if (log.isInfoEnabled()) {
			log.info("Creating biopax model using: " + biopaxFile.getAbsolutePath());
		}

		// convert file to model
		Model m = simpleIO.convertFromOWL(new FileInputStream(biopaxFile));
		merge(m);
	}
	

	@Transactional(propagation=Propagation.REQUIRED)
	@Override
	public void merge(final Model model)
	{
		/* 
		 * Level3 property/propertyOf ORM mapping and getters/setters 
		 * are to be properly implemented for this to work!
		 * Persistence annotations must move to new setter/getter pair 
		 * that do NOT call inverse prop. 'add' methods in the setter.
		 */
		if(model != null && !model.getObjects().isEmpty()) {
			/* 
			 * Using SimpleMerger would be unsafe and, probably, is not required :) 
			 * Hibernate should handle RDFId-based, cascading merging well...
			 */
			// start from "top" elements only :)
			Set<BioPAXElement> sourceElements = new ModelUtils(model)
				.getRootElements(BioPAXElement.class);
			if(log.isInfoEnabled())
				log.info("Persisting a BioPAX Model that has " 
						+ sourceElements.size() 
						+ " 'root' elements (of total: "
						+ model.getObjects().size());
			for (BioPAXElement bpe : sourceElements) {
				if (log.isInfoEnabled()) {
					log.info("Merging (root) BioPAX element: " 
							 + bpe + " - " 
							 + bpe.getModelInterface().getSimpleName());
				}
				merge(bpe); // there are CASCADE annotations!..
			}
		}
	}

	
	/**
	 * Saves or merges the element 
	 * (use when it's updated or unsure if saved ever before...)
	 * 
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public void merge(BioPAXElement aBioPAXElement) {
		String rdfId = aBioPAXElement.getRDFId();
		if (!level.hasElement(aBioPAXElement)) {
			throw new IllegalBioPAXArgumentException(
					"Given object is of wrong level");
		} else if (rdfId == null) {
			throw new IllegalBioPAXArgumentException(
					"null ID: every object must have an RDF ID");
		} else {
			if (log.isDebugEnabled())
				log.debug("updating/merging " + rdfId);
			// - many elements are affected, because of cascades...
			session().merge(aBioPAXElement);
		}
	}
	

	/**
	 * Paxtools MAIN DAO implementation details. 
	 * 
	 * unlike {@link WarehousePaxtoolsHibernateDAO#find(String, Class[], SearchFilter...)},
	 * 'filterByTypes' parameter here works rather similar to the "extra filters", 
	 * but it goes the last in the filters chain! Plus, there is one extra step - 
	 * lookup for parent entities, i.e.:
	 * - first, a {@link FullTextQuery} query does not use any class filters and just returns 
	 *   matching objects (chances are, the list will contain many {@link UtilityClass} elements);
	 * - second, 'extraFilters' are applied (to exclude undesired elements earlier...);
	 * - next, the list is iterated over to replace each utility class object with
	 *   one or many of its nearest parent {@link Entity} class elements if possible
	 *   (it takes to use PaxtoolsAPI, e.g., inverse properties or path accessors...);
	 *   TODO possibly to implement the same using HQL?..
	 * - next, 'filterByTypes' are now applied;
	 * - [not sure] now, 'extraFilters' are applied AGAIN to generate the final result
	 * - the last step will be to convert the objects list to the list of their identifiers
	 * 
	 * @see WarehousePaxtoolsHibernateDAO#find(String, Class[], SearchFilter...)
	 * 
	 * TODO shall I return not only IDs but also always - organism, pathway, datasource?
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public List<String> find(
			String query, 
			Class<? extends BioPAXElement> filterByTypes[],
			SearchFilter<? extends BioPAXElement,?>... extraFilters) 
	{
		// a list of identifiers to return
		List<String> toReturn = new ArrayList<String>();

		// a shortcut!
		if (query == null || "".equals(query) 
				|| query.trim().startsWith("*")) // see: Lucene query syntax
		{
			// do nothing, return the empty list
			return toReturn;
		} 
		
		// - otherwise, we continue and do real job -
		if (log.isInfoEnabled())
			log.info("find (IDs): " + query + ", filterBy: " + Arrays.toString(filterByTypes)
					+ "; extra filters: " + extraFilters.toString());

		// collect matching elements here
		List<BioPAXElement> results = new ArrayList<BioPAXElement>();
		
		/* - won't use for the main DAO impl.
		// fulltext query cannot filter by interfaces (only likes annotated entity classes)...
		Class<?>[] filterClasses = new Class<?>[filterByTypes.length];
		for(int i = 0; i < filterClasses.length; i++) {
			filterClasses[i] = getEntityClass(filterByTypes[i]);
		}
		*/
			
		// create a native Lucene query
		org.apache.lucene.search.Query luceneQuery = null;
		try {
			luceneQuery = multiFieldQueryParser.parse(query);
		} catch (ParseException e) {
			log.info("parser exception: " + e.getMessage());
			return toReturn;
		}
		// get full text session and create the query
		FullTextSession fullTextSession = Search.getFullTextSession(session());
		// fullTextSession.createFilter(arg0, arg1); // TODO how to use this?
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery(luceneQuery);
			//, filterClasses); // - won't use for the main DAO impl.

		if (log.isDebugEnabled()) {
			log.debug("Query '" + query + "' results size = " 
					+ hibQuery.getResultSize());
		}

		/*
		// TODO use pagination? [later...]
		hibQuery.setFirstResult(0);
		hibQuery.setMaxResults(10);
		*/

		// define a projection -
		/*TODO do we really need to know score and explanation? 
		* (if not, can skip hibQuery.setProjection, and hibQuery.list()
		* will return the list of BioPAX elements)
		*/
		hibQuery.setProjection(FullTextQuery.THIS, FullTextQuery.SCORE,
			"RDFId", FullTextQuery.EXPLANATION);
		
		// execute search and get the list
		List hibQueryResults = hibQuery.list();
		
		// process the entries
		for (Object row : hibQueryResults) {
			// get the matching element
			//BioPAXElement bpe = (BioPAXElement) row; // when not using hibQuery.setProjection...
			
			Object[] cols = (Object[]) row; // only when hibQuery.setProjection -
			BioPAXElement bpe = (BioPAXElement) cols[0];
			String id = (String) cols[2]; //  was used above
			
			// (debug info...)
			if (log.isDebugEnabled()) {
				float score = (Float) cols[1];
				Explanation expl = (Explanation) cols[3];
				log.debug("found uri: " + id + " (" + bpe + " - "
						+ bpe.getModelInterface() + ")" + "; score="
						+ score + "; explanation: " + expl);
			}
			
			if (filter(bpe, extraFilters)) {
				results.add(bpe);
			}
		}
		
		// collect identifiers
		for(BioPAXElement bpe : results) {
			if(isInstanceofOneOf(bpe, filterByTypes))
				toReturn.add(bpe.getRDFId());
		}
		
		
		if (log.isDebugEnabled()) {
			log.debug("Query '" + query + "' final results size = " 
					+ toReturn.size());
		}
		
		return toReturn;
	}

	
	/**
	 * Checks the object against the filter list.
	 * 
	 * @param bpe an object
	 * @param extraFilters filters the object must pass (if aplicable)
	 * @return true if it passed all the filters (that apply) in the list
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected boolean filter(BioPAXElement bpe, SearchFilter[] extraFilters) 
	{
		for(SearchFilter sf : extraFilters) {
			SearchFilterRange filterRange = sf.getClass()
				.getAnnotation(SearchFilterRange.class);
			if(filterRange == null 
				|| filterRange.value().isInstance(bpe)) 
			{
				try {
					if(!sf.apply(bpe)) 
						return false;
				} catch (ClassCastException e) {
					// skip (not applicable for this element filter)
				}
			}
		}
		return true;
	}

	
	
	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public void add(BioPAXElement aBioPAXElement)
	{
		String rdfId = aBioPAXElement.getRDFId();
		
		if (log.isDebugEnabled())
			log.debug("about to add: " + rdfId);
		
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
			if (log.isDebugEnabled())
				log.debug("adding " + rdfId);
			session().persist(aBioPAXElement); 
			// - many elements are affected, because of cascades...
		}
	}


	/* (non-Javadoc)
	 * @see org.biopax.paxtools.model.Model#remove(org.biopax.paxtools.model.BioPAXElement)
	 */

	@Override
	@Transactional(propagation=Propagation.MANDATORY)
	public void remove(BioPAXElement aBioPAXElement)
	{
		BioPAXElement bpe = getByID(aBioPAXElement.getRDFId());
		session().delete(bpe); // affects many elements, because of cascade=ALL!
	}


	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public <T extends BioPAXElement> T addNew(Class<T> type, String id)
	{
		T bpe = factory.create(type, id);
		add(bpe); // many elements, because of cascade=ALL
		return bpe;
	}


	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public boolean contains(BioPAXElement bpe)
	{
		return getByID(bpe.getRDFId()) != null;
	}


	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public boolean containsID(String id)
	{
		return getByID(id) != null;
	}


	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public BioPAXElement getByID(String id) 
	{
		if(id == null || "".equals(id)) 
			throw new RuntimeException("getByID(null) is called!");

		BioPAXElement toReturn = null;
		// rdfid is the Primary Key see BioPAXElementImpl)
		toReturn = (BioPAXElement) session().get(BioPAXElementImpl.class, id);

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


	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public Set<BioPAXElement> getObjects()
	{
		return getObjects(BioPAXElement.class);
	}


	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public <T extends BioPAXElement> Set<T> getObjects(Class<T> clazz)
	{
		String query = "from " + clazz.getCanonicalName();
		List<T> results = null;
		results = session().createQuery(query).list();
		Set<T> toReturn = new HashSet<T>();
		
		for(Object entry: results) {
			Hibernate.initialize(entry);
			toReturn.add((T)entry);
		}
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
	protected Class<? extends BioPAXElement> getEntityClass(
			Class<? extends BioPAXElement> filterBy) 
	{
		Class<? extends BioPAXElement> clazz = (BioPAXElement.class.equals(filterBy))
			? BioPAXElementImpl.class : factory.getImplClass(filterBy);
		
		if(clazz == null) {
			clazz = BioPAXElementImpl.class; // fall-back (to no filtering)
			log.error("Expected argument: a BioPAX interface"
					+ "; actual argument: " + filterBy.getCanonicalName());
		}

		return clazz;
	}

	/* (non-Javadoc)
	 * @see cpath.dao.PaxtoolsDAO#exportModel(java.io.OutputStream)
	 */
	/**
	 * When an non-empty list of IDs (RDFId, URI) is provided,
	 * it will use {@link SimpleIOHandler#convertToOWL(Model, OutputStream, String...)} 
	 * which in turn uses {@link Fetcher#fetch(BioPAXElement, Model)}
	 * (rather than {@link #getValidSubModel(Collection)}) method
	 * to recursively extract each listed element (with all children and properties)
	 * and put into a new sub-model, which is then serialized and 
	 * written to the output stream. Note: using the Fetcher, there is a risk 
	 * (depending on the data stored) of pulling almost entire network 
	 * by providing one or a few IDs... Also implemented here is 
	 * that the Fetcher/traverser does not follow BioPAX
	 * property 'nextStep', which otherwise could lead to infinite loops.
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public void exportModel(OutputStream outputStream, String... ids) 
	{
		simpleIO.convertToOWL(this, outputStream, ids);
	}
	

	/** 
	 * 
	 * Creates a new detached BioPAX sub-model from the list of URIs
	 * using Paxtools's {@link Completer} and {@link Cloner} approach
	 * (thus ignoring those not in the same list); runs within a transaction.
	 * 
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED, readOnly=true)
	public Model getValidSubModel(Collection<String> ids) {
		/*
		Set<BioPAXElement> bioPAXElements = new HashSet<BioPAXElement>();
		for(String id : ids) {
			BioPAXElement bpe = getByID(id);
			if(bpe != null)
				bioPAXElements.add(bpe);
		}
		Completer c = new Completer(simpleIO.getEditorMap());
		bioPAXElements = c.complete(bioPAXElements, null); //null - this model is used explicitly there
		Cloner cln = new Cloner(simpleIO.getEditorMap(), factory);
		Model model = cln.clone(null, bioPAXElements);
		return model;
		*/
		
		//(re-written) using the internal {@link Analysis} class ;)
		Analysis getTheseElements = new Analysis() {
			@Override
			public Set<BioPAXElement> execute(Model model, Object... args) {
				Set<BioPAXElement> bioPAXElements = new HashSet<BioPAXElement>();
				for(Object id : args) {
					BioPAXElement bpe = getByID(id.toString());
					if(bpe != null)
						bioPAXElements.add(bpe);
				}
				return bioPAXElements;
			}
		};
		
		return runAnalysis(getTheseElements, ids.toArray());
	}
	

	/* (non-Javadoc)
	 * @see cpath.dao.PaxtoolsDAO#count(java.lang.String, java.lang.Class)
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED, readOnly=true)
	@Deprecated
	public Integer count(String query, Class<? extends BioPAXElement> filterBy) {
		Long toReturn;

		if ("".equals(query) || query == null) {
			Query q = session().createQuery(
					"select count(*) from " + filterBy.getCanonicalName());
			toReturn = (Long) q.uniqueResult();
		} else {
			Class<? extends BioPAXElement> filterClass = getEntityClass(filterBy);
			org.apache.lucene.search.Query luceneQuery = null;
			FullTextSession fullTextSession = Search
					.getFullTextSession(session());
			try {
				luceneQuery = multiFieldQueryParser.parse(query);
			} catch (ParseException e) {
				throw new RuntimeException("Lucene query parser exception", e);
			}
			FullTextQuery hibQuery = fullTextSession.createFullTextQuery(
					luceneQuery, filterClass);
			toReturn = new Long(hibQuery.getResultSize());
		}

		return toReturn.intValue();
	}
	
	
	/* 
	 * All properties and inverse properties 
	 * (not very deep) initialization
	 * 
	 */
	@Transactional
	@Override
	public void initialize(Object obj) {
		if(obj instanceof BioPAXElement) {
			BioPAXElement element = (BioPAXElement) obj;
			// re-associate with a session
			session().update(element);
			// init. biopax properties
			Set<PropertyEditor> editors = simpleIO.getEditorMap()
				.getEditorsOf(element);
			if (editors != null)
				for (PropertyEditor editor : editors) {
					Set value = editor.getValueFromBean(element);
					Hibernate.initialize(value); 
					for(Object v : value) {
						Hibernate.initialize(v); 
					}
				}

			// init. inverse object properties (xxxOf)
			Set<ObjectPropertyEditor> invEditors = simpleIO.getEditorMap()
				.getInverseEditorsOf(element);
			if (invEditors != null) {
				for (ObjectPropertyEditor editor : invEditors) {
					// does collections as well!
					Object value = editor.getInverseValueFromBean(element);
					Hibernate.initialize(value);
					if(value instanceof Collection)
					for(Object v : (Collection)value) {
						Hibernate.initialize(v); 
					}
				}
			}
		}
	}

	
	/* (non-Javadoc)
	 * @see cpath.dao.PaxtoolsDAO#runAnalysis(cpath.dao.Analysis, java.lang.Object[])
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public Model runAnalysis(Analysis analysis, Object... args) {
		Set<BioPAXElement> result = null;
		
		// perform
		result = analysis.execute(this, args);
		
		// auto-complete/detach
		if(result != null) {
			Completer c = new Completer(simpleIO.getEditorMap());
			result = c.complete(result, null); //null - because the (would be) model is never used there anyway
			Cloner cln = new Cloner(simpleIO.getEditorMap(), factory);
			return cln.clone(null, result); // new (sub-)model
		} 
		
		return null;
	}

	
	@Override
	public void replace(BioPAXElement existing, BioPAXElement replacement) {
		throw new UnsupportedOperationException("not supported");
	}

	
	@Override
	public void repair() {
		throw new UnsupportedOperationException("not supported");
	}
	
	
	/**
	 * Checks an object is instance of at least
	 * on of classes in the list.
	 * 
	 * @param classes
	 * @param obj
	 * @return
	 */
	protected boolean isInstanceofOneOf( 
			BioPAXElement obj, Class<? extends BioPAXElement>... classes) 
	{
			if(classes.length == 0)
				return true;
		
			for(Class<? extends BioPAXElement> c : classes) {
				if(c.isInstance(obj)) {
					return true;
				}
			}	
			return false;
	}
	
}


