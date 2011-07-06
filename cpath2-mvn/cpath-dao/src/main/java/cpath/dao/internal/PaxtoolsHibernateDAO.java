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

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.util.Version;
import org.biopax.paxtools.impl.BioPAXElementImpl;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.Complex;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.Named;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.Provenance;
import org.biopax.paxtools.model.level3.RelationshipTypeVocabulary;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.SequenceEntityReference;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;
import org.biopax.paxtools.model.level3.XReferrable;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.paxtools.util.IllegalBioPAXArgumentException;
import org.biopax.paxtools.controller.*;
import org.biopax.paxtools.controller.ModelUtils.RelationshipType;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.hibernate.*;
import org.hibernate.search.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;

import cpath.config.CPathSettings;
import cpath.dao.Analysis;
import cpath.dao.PaxtoolsDAO;
import cpath.dao.filters.SearchFilter;
import cpath.dao.filters.SearchFilterRange;
import cpath.service.jaxb.SearchHitType;
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
	
	private final int BATCH_SIZE = 20;
	private final int BATCH_INDEXING_SIZE = 200;

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

	/**
	 *  A supplementary {@link Analysis} (algorithm) to be executed 
	 *  when detaching elements (element(s) as a sub-model) from this model
	 */
	private Analysis getTheseElements;
	
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
		
		// this simply implements how to get a list of elements from the list of URIs
		this.getTheseElements = new Analysis() {
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
	
//	//Now that were able to make MassIndexer work (in a separate class),
//	//this older interface has been removed (worked reliably but waaay too slow!)
//	
//	@Transactional(propagation=Propagation.REQUIRED)
//	public void createIndex() {
//		if(log.isInfoEnabled())
//			log.info("Begin indexing...");
//		
//		// manually re-index
//		FullTextSession fullTextSession = Search.getFullTextSession(session());
//		fullTextSession.setFlushMode(FlushMode.MANUAL);
//		fullTextSession.setCacheMode(CacheMode.IGNORE);
//		//Scrollable results will avoid loading too many objects in memory
//		ScrollableResults results = fullTextSession.createCriteria( BioPAXElementImpl.class )
//		    .setFetchSize(BATCH_INDEXING_SIZE)
//		    .scroll( ScrollMode.FORWARD_ONLY );
//		int index = 0;
//		while( results.next() ) {
//		    index++;
//		    fullTextSession.index( results.get(0) ); //index each element
//		    if (index % BATCH_INDEXING_SIZE == 0) {
//		        fullTextSession.flushToIndexes(); //apply changes to indexes
//		        fullTextSession.clear(); //free memory since the queue is processed
//		        if(log.isDebugEnabled())
//					log.debug("Indexed " + index);
//		    }
//		}
//		if(log.isInfoEnabled())
//			log.info("Ended indexing.");
//	}

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

	 
	// non @Transactional here 
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
			// First, insert new elements using a stateless session!
			if(log.isDebugEnabled())
				log.debug("merge(model): inserting BioPAX elements (stateless)...");
			StatelessSession stls = getSessionFactory().openStatelessSession();
			int i = 0;
			Transaction tx = stls.beginTransaction();
			for (BioPAXElement bpe : model.getObjects()) {
				if(stls.get(bpe.getClass(), bpe.getRDFId()) == null) {
					stls.insert(bpe);
					i++;
				}
				//else stls.update(bpe); // TODO ? may be not required at all (unless we want overwrite)
			}
			tx.commit();
			stls.close();
			
			if (log.isDebugEnabled()) {
				log.debug("merge(model): inserted " + i + " objects");
			}
			
			//run batch update in a separate transaction;
			//this saves object relationships and collections
			update(model);
		}
	}

	
	@Transactional(propagation=Propagation.REQUIRED)
	public void update(final Model model) 
	{	
		Session ses = session();
		
		int i = 0;
		for (BioPAXElement bpe : model.getObjects()) {			
			// save/update
			merge(bpe);
			
			i++;
			if(i % BATCH_SIZE == 0) {
				ses.flush();
				ses.clear();
				if (log.isDebugEnabled()) {
					log.debug("update(model): saved " + i);
				}
			}
		}
		
		if (log.isDebugEnabled()) {
			log.debug("update(model): total merged " + i + " objects");
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
//		if (!level.hasElement(aBioPAXElement)) {
//			throw new IllegalBioPAXArgumentException(
//					"Given object is of wrong level");
//		} else 
		if (rdfId == null) {
			throw new IllegalBioPAXArgumentException(
					"null ID: every object must have an RDF ID");
		} else {
			if (log.isDebugEnabled())
				log.debug("merge(aBioPAXElement): merging " + rdfId + " " 
					+ aBioPAXElement.getModelInterface().getSimpleName());
			
			// - many elements are affected, because of cascades...
			session().merge(aBioPAXElement);
		}
	}
	
	
	@Override
	@Transactional(propagation=Propagation.REQUIRED, readOnly=true)
	public List<SearchHitType> findElements(
			String query, 
			Class<? extends BioPAXElement> filterByType,
			SearchFilter<? extends BioPAXElement,?>... extraFilters) 
	{
		// collect matching elements here
		List<SearchHitType> results = new ArrayList<SearchHitType>();
		
		// a shortcut
		if (query == null || "".equals(query) 
				|| query.trim().startsWith("*")) // see: Lucene query syntax
		{
			// do nothing, return the empty list
			return results;
		} 
		
		// otherwise, we continue and do real job -
		if (log.isInfoEnabled())
			log.info("find: " + query + ", filterBy: " + filterByType
					+ "; extra filters: " + Arrays.toString(extraFilters));
			
		// create a native Lucene query
		org.apache.lucene.search.Query luceneQuery = null;
		try {
			luceneQuery = multiFieldQueryParser.parse(query);
		} catch (ParseException e) {
			log.info("parser exception: " + e.getMessage());
			return results;
		}

		// get a full text session
		FullTextSession fullTextSession = Search.getFullTextSession(session());
		FullTextQuery hibQuery;
		
		if(filterByType != null) {
			// full-text query cannot filter by interfaces; 
			// so let's translate them to the annotated entity classes
			Class<?> filterClass = getEntityClass(filterByType);
			hibQuery = fullTextSession.createFullTextQuery(luceneQuery, filterClass);
			//NOTE:  unlike for findEntities method, 'filterClass' is applied right here!
		} else {
			hibQuery = fullTextSession.createFullTextQuery(luceneQuery);
		}
		
		int count = hibQuery.getResultSize(); //"cheap" operation (Hib. does not init objects)
		if (log.isInfoEnabled())
			log.info("Query '" + query + "' (filters not shown), results size = " + count);

		// using the projection (to get some more statistics/fields)
		if(CPathSettings.isDebug())
			hibQuery.setProjection(FullTextQuery.THIS, FullTextQuery.SCORE,
				FullTextQuery.EXPLANATION);
		else
			hibQuery.setProjection(FullTextQuery.THIS);
		
		// execute search
		// TODO try pagination...
		final int max = 50;
		int l = 0;
		hibQuery.setMaxResults(max);
		while (l < count) {
			hibQuery.setFirstResult(l);
			List hibQueryResults = hibQuery.list(); // gets up to 'max' records
			for (Object row : hibQueryResults) {
				Object[] cols = (Object[]) row;
				// get the matching element
				BioPAXElement bpe = (BioPAXElement) cols[0];
				String id = bpe.getRDFId();

				if (log.isDebugEnabled()) {
					log.debug("Hit (before filters applied) uri: " + id + " ("
							+ bpe + " - " + bpe.getModelInterface() + ")");
				}

				// extra filtering
				if (filter(bpe, extraFilters)) {

					if (CPathSettings.isDebug()) {
						float score = (Float) cols[1];
						bpe.getAnnotations().put("score", score);
						Explanation expl = (Explanation) cols[2];
						bpe.getAnnotations()
								.put("explanation", expl.toString());
						if (log.isDebugEnabled()) {
							log.debug("Hit score=" + cols[1]
									+ "; explanation: " + cols[2]);
						}
					}

					//initialize(bpe); // not required: we're not going to return this outside the transaction
					results.add(bpeToSearcHit(bpe));
				}

				l++;
			}
		}
		
		if (log.isInfoEnabled()) {
			log.info("Using query '" + query 
				+ "', after filtering results size = " + results.size());
		}
		
		return results;
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
	 *   in fact, it is going to delegate the search to {@link #findElements(String, Class, SearchFilter...)} 
	 *   method, with the second parameter set to NULL.
	 * - second, the list is iterated over to replace each utility class object with
	 *   one or many of its nearest parent {@link Entity} class elements if possible
	 *   (it takes to use PaxtoolsAPI, e.g., inverse properties or path accessors...);
	 *   TODO possibly to implement the same using HQL?..
	 * - next, 'filterByTypes' are now applied (the second time; the first was in the first step);
	 * - finally, 'extraFilters' are applied
	 * 
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED, readOnly=true)
	public List<SearchHitType> findEntities(
			String query, 
			Class<? extends BioPAXElement> filterByType,
			SearchFilter<? extends BioPAXElement,?>... extraFilters) 
	{
		List<SearchHitType> results = new ArrayList<SearchHitType>();
		
		// - otherwise, we continue and do real job -		
		if (log.isInfoEnabled())
			log.info("findEntities called");

		// search in all classes (the first pass)
		// a shortcut
		if (query == null || "".equals(query) 
				|| query.trim().startsWith("*")) // see: Lucene query syntax
		{
			// do nothing, return the empty list
			return results;
		} 
		
		// otherwise, we continue and do real job -
		if (log.isInfoEnabled())
			log.info("find: " + query + ", filterBy: " + filterByType
					+ "; extra filters: " + Arrays.toString(extraFilters));
			
		// create a native Lucene query
		org.apache.lucene.search.Query luceneQuery = null;
		try {
			luceneQuery = multiFieldQueryParser.parse(query);
		} catch (ParseException e) {
			log.info("parser exception: " + e.getMessage());
			return results;
		}

		// get a full text session
		FullTextSession fullTextSession = Search.getFullTextSession(session());
		//no class filter use here (first pass)
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery(luceneQuery); 
		
		int count = hibQuery.getResultSize(); //"cheap" operation (Hib. does not init objects)
		if (log.isInfoEnabled())
			log.info("Query '" + query + "' (no filter/lookup applied yet), results size = " + count);

		// using the projection (to get some more statistics/fields)
		if(CPathSettings.isDebug())
			hibQuery.setProjection(FullTextQuery.THIS, FullTextQuery.SCORE,
				FullTextQuery.EXPLANATION);
		else
			hibQuery.setProjection(FullTextQuery.THIS);
		
		// execute search
		// TODO try pagination...
		final int max = 50;
		int l = 0;
		hibQuery.setMaxResults(max);
		while (l < count) {
			hibQuery.setFirstResult(l);
			List hibQueryResults = hibQuery.list(); // gets up to 'max' records
			for (Object row : hibQueryResults) {
				Object[] cols = (Object[]) row;
				// get the matching element
				BioPAXElement bpe = (BioPAXElement) cols[0];
				String id = bpe.getRDFId();

				if (log.isDebugEnabled()) {
					log.debug("Hit (before filters applied) uri: " + id + " ("
							+ bpe + " - " + bpe.getModelInterface() + ")");
				}

				// Filter and "upgrade" (lookup) to corresponding parent Entities
				//TODO may be implement this in HQL instead...
				// currently, lookup for entities works for some utility calsses only... TODO test it
				if(!(bpe instanceof Entity)) { // a UtilityClass object
					if(bpe instanceof Xref) {
						for(XReferrable xReferrable : ((Xref)bpe).getXrefOf()) {
							if(xReferrable instanceof Entity) {
								addEntity(results, (Entity)xReferrable, bpe, filterByType, extraFilters);
							} 
							else if(xReferrable instanceof EntityReference) {
								Set<SimplePhysicalEntity> spes = ((EntityReference) xReferrable).getEntityReferenceOf();
								for(SimplePhysicalEntity spe : spes) {
									addEntity(results,spe, bpe, filterByType, extraFilters);
								}
							}
						}
					}
					else if(bpe instanceof EntityReference) {
						Set<SimplePhysicalEntity> spes = ((EntityReference) bpe).getEntityReferenceOf();
						for(SimplePhysicalEntity spe : spes) {
							addEntity(results, spe, bpe, filterByType, extraFilters);
						}
					}
				} else {
					addEntity(results, (Entity)bpe, null, filterByType, extraFilters);
				}
				//TODO for other non-entities
				l++;
			}
		}
		
		if (log.isInfoEnabled()) {
			log.info("Using query for entities '" + query 
					+ "', final results size = " + results.size());
		}
		
		return results;
	}
	
	private void addEntity(List<SearchHitType> list, Entity ent,
			BioPAXElement actualHit,
			Class<? extends BioPAXElement> filterByType, SearchFilter<? extends BioPAXElement, ?>[] extraFilters) 
	{
		addIfPassAndNew(list, ent, actualHit, filterByType, extraFilters);
		// TODO not sure whether to go here for complexes or not...
		if (ent instanceof PhysicalEntity) {
			addComplexes(list, (PhysicalEntity) ent, actualHit, filterByType, extraFilters);
		}
	}

	private void addIfPassAndNew(List<SearchHitType> list, Entity ent,
			BioPAXElement actualHit,
			Class<? extends BioPAXElement> filterByType, 
			SearchFilter<? extends BioPAXElement, ?>[] extraFilters) 
	{
		if(!list.contains(ent) && passFilters(ent, filterByType, extraFilters))
		{
			if(actualHit != null && !ent.equals(actualHit)) {
				if(CPathSettings.isDebug()) {
					ent.getAnnotations().put("score", actualHit.getAnnotations().get("score"));
					ent.getAnnotations().put("explanation", actualHit.getAnnotations().get("explanation"));
				}
				ent.getAnnotations().put("actualHitUri", actualHit.getRDFId());
			}
			list.add(bpeToSearcHit(ent));
		}
	}

	private void addComplexes(List<SearchHitType> list, PhysicalEntity pe,
			BioPAXElement actualHit, 
			Class<? extends BioPAXElement> filterByType, 
			SearchFilter<? extends BioPAXElement,?>... extraFilters) 
	{
		Set<Complex> complexes = pe.getComponentOf();
		for (Complex c : complexes) {
			addIfPassAndNew(list, c, actualHit, filterByType, extraFilters);
		}
	}

	/**
	 * Apply search filters
	 * 
	 * @param bpe
	 * @param filterByType
	 * @param extraFilters
	 * @return
	 */
	private boolean passFilters(BioPAXElement bpe, 
			Class<? extends BioPAXElement> filterByType, 
			SearchFilter<? extends BioPAXElement,?>... extraFilters) 
	{
		if( ( filterByType == null || filterByType.isInstance(bpe) )	
			&& filter(bpe, extraFilters) 
		){ 
			return true;
		}
		else {
			return false;
		}
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
	@Transactional(propagation=Propagation.REQUIRED)
	public void remove(BioPAXElement aBioPAXElement)
	{
		BioPAXElement bpe = getByID(aBioPAXElement.getRDFId());
		session().delete(bpe); // affects many elements, because of cascade=ALL!
	}


	@Override
	public <T extends BioPAXElement> T addNew(Class<T> type, String id)
	{
		T bpe = factory.create(type, id);
		add(bpe); // many elements, because of cascade=ALL
		return bpe;
	}


	@Override
	@Transactional(readOnly=true)
	public boolean contains(BioPAXElement bpe)
	{
		return getByID(bpe.getRDFId()) != null;
	}


	@Override
	@Transactional(readOnly=true)
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
	public Set<BioPAXElement> getObjects()
	{
		return getObjects(BioPAXElement.class);
	}


	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public <T extends BioPAXElement> Set<T> getObjects(Class<T> clazz)
	{
		String query = "from " + clazz.getCanonicalName();
		List<T> results = session().createQuery(query).list();
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
	@Transactional(propagation=Propagation.REQUIRES_NEW)
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
	public Model getValidSubModel(Collection<String> ids) {
		// run the analysis (in a new transaction)
		return runAnalysis(this.getTheseElements, ids.toArray());
	}
	
	
	/* 
	 * All properties and inverse properties 
	 * (not very deep) initialization
	 * 
	 */
	@Transactional(propagation=Propagation.REQUIRED, readOnly=true)
	@Override
	public void initialize(Object obj) 
	{
		if(obj instanceof BioPAXElement) {		
			//just reassociate:
			session().buildLockRequest(LockOptions.NONE).lock(obj);
			Hibernate.initialize(obj);
	
			BioPAXElement element = (BioPAXElement) obj;

			// init. biopax properties
			Set<PropertyEditor> editors = simpleIO.getEditorMap()
				.getEditorsOf(element);
			if (editors != null) {
				for (PropertyEditor editor : editors) {
					Set<?> value = editor.getValueFromBean(element);
					Hibernate.initialize(value); //yup, it inits collections as well ;)
					for(Object v : value) {
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
					Hibernate.initialize(value);
					//values the set can be BioPAX elements only
					for(Object v : value) {
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
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Model runAnalysis(Analysis analysis, Object... args) {
		// perform
		Set<BioPAXElement> result = analysis.execute(this, args);
		
		if(log.isDebugEnabled())
			log.debug("runAnalysis: finished; now detaching the sub-moodel...");
		
		// auto-complete/detach
		if(result != null) {
			if(log.isDebugEnabled())
				log.debug("runAnalysis: running auto-complete...");
			Completer c = new Completer(simpleIO.getEditorMap());
			result = c.complete(result, null); //null - because the (would be) model is never used there anyway
			if(log.isDebugEnabled())
				log.debug("runAnalysis: cloning...");
			Cloner cln = new Cloner(simpleIO.getEditorMap(), factory);
			Model submodel = cln.clone(null, result);
			
			if(log.isDebugEnabled())
				log.debug("runAnalysis: returned");
			return submodel; // new (sub-)model
		} 
		
		if(log.isDebugEnabled())
			log.debug("runAnalysis: returned NULL");
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
	 * Converts the returned by a query BioPAX elements to 
	 * simpler "hit" java beans (serializable to XML, etc..) 
	 * 
	 * @param data
	 * @return
	 */
	private List<SearchHitType> toSearchHits(List<? extends BioPAXElement> data) {
		List<SearchHitType> hits = new ArrayList<SearchHitType>(data.size());
		
		for(BioPAXElement bpe : data) {
			SearchHitType hit = bpeToSearcHit(bpe);
			hits.add(hit);
		}
		
		return hits;
	}
	
	/**
	 * Converts a returned by a query BioPAX element to 
	 * the search hit java bean (serializable to XML) 
	 * 
	 * @param bpe
	 * @return
	 */
	private SearchHitType bpeToSearcHit(BioPAXElement bpe) {
			SearchHitType hit = new SearchHitType();
			hit.setUri(bpe.getRDFId());
			hit.setBiopaxClass(bpe.getModelInterface().getSimpleName());
			// add lucene info
			if(CPathSettings.isDebug()) {
				//TODO setExcerpt must contain the matched text...
				hit.setExcerpt(StringEscapeUtils.escapeXml(
					bpe.getAnnotations().get("explanation").toString()));
			}
			
			if(bpe.getAnnotations().get("actualHitUri") != null)
				hit.setActualHitUri(StringEscapeUtils.escapeXml(
						bpe.getAnnotations().get("actualHitUri").toString()));
			
			// add standard and display names if any -
			if(bpe instanceof Named) {
				Named named = (Named)bpe;
				String std = named.getStandardName();
				if( std != null)
					hit.getName().add(std);
				String dsp = named.getDisplayName();
				if(dsp != null && !dsp.equalsIgnoreCase(std))
					hit.getName().add(dsp);
			}
			
			// add organisms and data sources
			if(bpe instanceof Entity) {
				// add data sources (URIs)
				for(Provenance pro : ((Entity)bpe).getDataSource()) {
					hit.getDataSource().add(pro.getRDFId());
				}
				
				// add organisms and pathways (URIs);
				// at the moment, this apply to Entities only -
				HashSet<String> organisms = new HashSet<String>();
				HashSet<String> processes = new HashSet<String>();
				for(Xref x : ((Entity)bpe).getXref()) 
				{
					if((x instanceof RelationshipXref) && ((RelationshipXref) x).getRelationshipType() != null) 
					{
						RelationshipXref rx = (RelationshipXref) x;
						RelationshipTypeVocabulary cv = rx.getRelationshipType();
						//initialize(cv); // not required - this methos is called within an active transaction
						String autoId = ModelUtils
							.relationshipTypeVocabularyUri(RelationshipType.ORGANISM.name());
						if(cv.getRDFId().equalsIgnoreCase(autoId))
						{
							organisms.add(rx.getId());
						} 
// this feature is disabled for now...
//						else if(cv.getRDFId().equalsIgnoreCase(ModelUtils
//							.relationshipTypeVocabularyUri(RelationshipType.PROCESS.name()))) 
//						{
//							processes.add(rx.getId());
//						}	
					}
				}
				
				if(!organisms.isEmpty())
					hit.getOrganism().addAll(organisms);
				
//				if(!processes.isEmpty())
//					hit.getPathway().addAll(processes);
			} else
			// set organism for some of EntityReference
			if(bpe instanceof SequenceEntityReference) {
				BioSource bs = ((SequenceEntityReference)bpe).getOrganism(); 
				if(bs != null)
					hit.getOrganism().add(bs.getRDFId());
			}
		
		return hit;
	}
}


