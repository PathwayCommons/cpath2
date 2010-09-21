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
import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.hibernate.*;
import org.hibernate.search.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;


import cpath.config.CPathSettings;
import cpath.dao.PaxtoolsDAO;
import cpath.warehouse.WarehouseDAO;

import java.util.*;
import java.io.*;

import static org.biopax.paxtools.impl.BioPAXElementImpl.*;


/**
 * Paxtools/BioPAX DAO class.
 *
 */
@Transactional
@Repository
public class PaxtoolsHibernateDAO implements PaxtoolsDAO, WarehouseDAO
{
	private static final long serialVersionUID = 1L;

	public final static String[] ALL_FIELDS =
		{
			SEARCH_FIELD_ID,
			SEARCH_FIELD_AVAILABILITY,
			SEARCH_FIELD_COMMENT,
			SEARCH_FIELD_KEYWORD,
			SEARCH_FIELD_NAME,
			SEARCH_FIELD_TERM,
			SEARCH_FIELD_XREF_DB,
			SEARCH_FIELD_XREF_ID,
		};

	private static Log log = LogFactory.getLog(PaxtoolsHibernateDAO.class);
	private SessionFactory sessionFactory;
	private final Map<String, String> nameSpacePrefixMap;
	private final BioPAXLevel level;
	private final BioPAXFactory factory;
	private BioPAXIOHandler reader;
	private boolean addDependencies = false;
	MultiFieldQueryParser multiFieldQueryParser;

	protected PaxtoolsHibernateDAO()
	{
		this.level = BioPAXLevel.L3;
		this.factory = level.getDefaultFactory();
		this.nameSpacePrefixMap = new HashMap<String, String>();
		nameSpacePrefixMap.put("", CPathSettings.CPATH_URI_PREFIX);
		reader = new SimpleReader(BioPAXLevel.L3);
		multiFieldQueryParser = new MultiFieldQueryParser(
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
		return reader;
	}

	public void setReader(BioPAXIOHandler reader) {
		this.reader = reader;
	}

	
	Session session() {
		return sessionFactory.getCurrentSession();
	}
	

	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public void createIndex() {
		FullTextSession fullTextSession = Search.getFullTextSession(session());
		MassIndexer indexer = fullTextSession.createIndexer();
		try {
			int attempts = 0;
			indexer.batchSizeToLoadObjects(50)
				.purgeAllOnStart(true)
				//.optimizeOnFinish(true)
				.startAndWait();
		} catch (InterruptedException e) {
			throw new RuntimeException("Index re-build is interrupted.");
		}
	}

	//not transactional (but it's 'merge' method that creates a new transaction)
	public void importModel(File biopaxFile) throws FileNotFoundException
	{
		if (log.isInfoEnabled()) {
			log.info("Creating biopax model using: " + biopaxFile.getAbsolutePath());
		}

		// convert file to model
		Model m = reader.convertFromOWL(new FileInputStream(biopaxFile));
		merge(m);
	}
	

	@Transactional(propagation=Propagation.REQUIRED)
	@Override
	public void merge(final Model model)
	{
		/* TODO level3 property/propertyOf mapping have to be fixed for this to work!
		 * Annotations must move to new setter/getter pair that would not call
		 * the inverse prop. 'add' from the setter.
		 */
		if(model != null && !model.getObjects().isEmpty()) {
			/* using SimpleMerger is unsafe and, probably, not required at all :) 
			 * Session manages RDFId-based merge to some extent...
			 */
			
			// idea: update props with existing values before the merge
			//ElementUpdater updater = new ElementUpdater();
			
			Set<BioPAXElement> sourceElements = model.getObjects();
			for (BioPAXElement bpe : sourceElements) {
				if (!containsID(bpe.getRDFId())) { // adding a new element
					// migrate object properties to existing objects (prevents duplicate PK!)
					//updater.update(this, bpe);
					// it's save to merge now
					add(bpe); // is a CASCADE merge, in fact!
				}
			}
		}
	}


	/*
	 * 'filterBy' is a BioPAX interface, not the implementation.
	 */ 
	@Transactional(propagation=Propagation.REQUIRED)
	public <T extends BioPAXElement> List<T> search(String query, Class<T> filterBy)
	{
		if (log.isInfoEnabled())
			log.info("search: " + query + ", filterBy: " + filterBy);
		// unfortunately, fulltextquery cannot filter by interfaces (only likes annotated entity classes)...
		Class<? extends BioPAXElement> filterClass = getEntityClass(filterBy);
		// set to return
		List<T> toReturn = new ArrayList<T>();
		// create native lucene query
		org.apache.lucene.search.Query luceneQuery = null;
		try {
			luceneQuery = multiFieldQueryParser.parse(query);
		} catch (ParseException e) {
			log.info("parse exception: " + e.getMessage());
			return toReturn;
		}

		// get full text session
		FullTextSession fullTextSession = Search.getFullTextSession(session());
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery(luceneQuery, filterClass);
		// execute search
		List results = hibQuery.list();
		
		for(Object entry: results) {
			Hibernate.initialize(entry);
			toReturn.add((T)entry);
		}
		
		return results;
	}

	

	/* (non-Javadoc)
	 * @see cpath.dao.PaxtoolsDAO#find(java.lang.String, java.lang.Class)
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public List<String> find(String query, Class<? extends BioPAXElement> filterBy) 
	{
		// set to return
		List<String> toReturn = new ArrayList<String>();
		
		if(query == null || "".equals(query) || "*".equals(query.trim())) {
			Query q = session().createQuery("select rdfid from " 
					+ filterBy.getCanonicalName());
			toReturn = q.list();
			return toReturn;
		}
		
		if (log.isInfoEnabled())
			log.info("find (IDs): " + query + ", filterBy: " + filterBy);

		// fulltextquery cannot filter by interfaces (only likes annotated entity classes)...
		Class<? extends BioPAXElement> filterClass = getEntityClass(filterBy);
		// create native lucene query
		org.apache.lucene.search.Query luceneQuery = null;
		try {
			luceneQuery = multiFieldQueryParser.parse(query);
		} catch (ParseException e) {
			log.info("parser exception: " + e.getMessage());
			return toReturn;
		}

		// get full text session
		FullTextSession fullTextSession = Search.getFullTextSession(session());
		//fullTextSession.createFilter(arg0, arg1); // how to use this, btw?!
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery(luceneQuery, filterClass);
		// TODO use count
		int count = hibQuery.getResultSize();
		if(log.isDebugEnabled())
			log.debug("Query '" + query + "' results size = " + count);
		
		// TODO later, use pagination properly (this is stub!!!)
		hibQuery.setFirstResult(0);
		hibQuery.setMaxResults(10);
		
		// use projection!
		hibQuery.setProjection("RDFId", FullTextQuery.SCORE, 
				FullTextQuery.EXPLANATION //, FullTextQuery.THIS
			);
		// execute search
		List results = hibQuery.list();
		for(Object row: results) {
			Object[] cols = (Object[]) row;
			String id = (String) cols[0];
  			float score = (Float) cols[1];
  			Explanation expl = (Explanation) cols[2];
  			//BioPAXElement bpe = (BioPAXElement) cols[3];
  			if(log.isDebugEnabled())
  				log.debug("found: " + id + "; score=" + score 
  						+ "; expl.: " + expl);
  			toReturn.add(id);
		}

		return toReturn;
	}
	
	
	@Override
	@Transactional(propagation=Propagation.REQUIRED)
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
			if (log.isDebugEnabled())
			{
				log.debug("adding " + rdfId);
			}
			
			/* seems, unlike 'save' or 'persist', 'saveOrUpdate' 
			 * does resolve duplicate key issues (because of 
			 * children elements cascade=All mappings)
			 * Element becomes persistent; saveOrUpdate throws exception 
			 * when aBioPAXElement (or one of its children) 
			 * has been previously saved...
			 */
			//session().saveOrUpdate(aBioPAXElement);
			session().merge(aBioPAXElement); 
			// - aBioPAXElement is saved/updated, but this remains detached/transient!
			// - many elements, because of cascade=ALL
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
		session().delete(bpe); // many elements, because of cascade=ALL
	}


	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public <T extends BioPAXElement> T addNew(Class<T> type, String id)
	{
		T bpe = factory.reflectivelyCreate(type);
		bpe.setRDFId(id);
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
	/*
	 * TODO experimental (see also #initialize(BioPAXElement))
	 */
	public BioPAXElement getByIdInitialized(String id) {
		BioPAXElement toReturn = getByID(id);
		
		/*
		 * Interesting, inverse prop editor (XrefOf) is not
		 * available during ElementInitializer,
		 * "manually" initialize (some of?) inverse props here.
		 */
		if (toReturn != null) {
			
			if (toReturn instanceof Xref) {
				Hibernate.initialize(((Xref)toReturn).getXrefOf()); // cool! TODO required for all inverse properties...
			}
			
			// initialize
			ElementInitializer initializer = new ElementInitializer();
			initializer.initialize(this, toReturn); 

		}

		return toReturn; // null means no such element
	}

	
	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public BioPAXElement getByID(String id) 
	{
		if(id == null || "".equals(id)) 
			throw new RuntimeException("getElement(null) is called!");

		BioPAXElement toReturn = null;
		// rdfid is the Primary Key see BioPAXElementImpl)
		toReturn = (BioPAXElement) session().get(BioPAXElementImpl.class, id);

		return toReturn; // null means no such element
	}
	
	
	@Override
	@Transactional(propagation=Propagation.REQUIRED, readOnly=true)
	public BioPAXElement getObject(String id) 
	{
		if(id == null || "".equals(id)) 
			throw new RuntimeException("getElement(null) is called!");

		BioPAXElement toReturn = null;
		
		// get the element
		BioPAXElement bpe = getByID(id);
		
		// check it has inverse props set ("cloning" may break it...)
		if(bpe instanceof Xref) {
			assert(!((Xref)bpe).getXrefOf().isEmpty());
			if(log.isDebugEnabled()) {
				log.debug(id + " is xrefOf " + 
					((Xref)bpe).getXrefOf().iterator().next().toString()
				);
			}
		}
		
		
		if (bpe != null) { 
			// clone
			ElementCloner cloner = new ElementCloner();
			Model model = cloner.clone(this, bpe); 
			toReturn = model.getByID(id);
		}
		
		return toReturn; // null means no such element
	}


	@Override
	public Map<String, BioPAXElement> getIdMap()
	{
		throw new UnsupportedOperationException(
			"Discontinued method; use a combination of " +
			"containsID(id), getById(id), getObjects() instead.");
	}


	@Override
	public BioPAXLevel getLevel()
	{
		return level;
	}


	@Override
	public Map<String, String> getNameSpacePrefixMap()
	{
		return nameSpacePrefixMap;
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
		//if (eager) query += " fetch all properties";
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
		throw new UnsupportedOperationException("Internal BioPAX Factory cannot be modified.");
		/* unsafe
		if (factory.getLevel() == this.level) {
			this.factory = factory;
		} else {
			throw new IllegalAccessError("Cannot use this Biopax factory!");
		}
		*/
	}


	@Override
	@Transactional(propagation=Propagation.MANDATORY)
	public void updateID(String oldId, String newId)
	{
		throw new UnsupportedOperationException(
				"updateID is not supported by this Model.");
		/* unsafe...
		BioPAXElement bpe = getByID(oldId);
		bpe.setRDFId(newId);
		session().refresh(bpe); // TODO is refresh required?
		*/
	}

	
	/*
	 * Gets Hibernate annotated entity class 
	 * by the BioPAX Model interface class 
	 * 
	 * TODO improve, generalize...
	 */
	private Class<? extends BioPAXElement> getEntityClass(
			Class<? extends BioPAXElement> filterBy) {
		
		Class<? extends BioPAXElement> filterClass = BioPAXElementImpl.class; // fall-back
		
		if (!BioPAXElement.class.equals(filterBy)) { // otherwise use BioPAXElementImpl
			if (filterBy.isInterface()) {
				try {
					filterClass = (Class<? extends BioPAXElement>) factory
						.reflectivelyCreate(filterBy).getClass();
				} catch (IllegalBioPAXArgumentException e) {
					// throw new IllegalArgumentException(
					log.error("Expected a BioPAX model interface "
								+ "of the instantiable BioPAX class or the base interface, "
								+ "'BioPAXElement'; but it was: "
								+ filterBy.getCanonicalName(), e);
					filterClass = BioPAXElementImpl.class;
				}
			} else {
				// throw new IllegalArgumentException(
				log.error("Not a BioPAX model interface: "
						+ filterBy.getCanonicalName());
			}
		}

		return filterClass;
	}

	/* (non-Javadoc)
	 * @see cpath.dao.PaxtoolsDAO#exportModel(java.io.OutputStream)
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public void exportModel(OutputStream outputStream, String... ids) {
		
		Model model = (ids.length > 0) 
			? getValidSubModel(Arrays.asList(ids)) : this; // no ids? - export everything!
		
		try {
			new SimpleExporter(level).convertToOWL(model, outputStream);
		} catch (Exception e) {
			throw new RuntimeException("Failed to export Model.", e);
		}
	}

	/* (non-Javadoc)
	 * @see cpath.warehouse.WarehouseDAO#getObject(java.lang.String, java.lang.Class)
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED, readOnly=true)
	public <T extends BioPAXElement> T getObject(String urn, Class<T> clazz) 
	{
		BioPAXElement bpe = getObject(urn);
		if(clazz.isInstance(bpe)) {
			return (T) bpe;
		} else {
			if(bpe != null) log.error("getObject(" +
				urn + ", " + clazz.getSimpleName() + 
				"): returned object has different type, " 
				+ bpe.getModelInterface());
			return null;
		}
	}


	/* (non-Javadoc)
	 * @see cpath.dao.WarehouseDAO#getByXref(java.util.Set, java.lang.Class)
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED, readOnly=true)
	public Set<String> getByXref(Set<? extends Xref> xrefs, 
		Class<? extends XReferrable> clazz) 
	{
		Set<String> toReturn = new HashSet<String>();
		
		for (Xref xref : xrefs) {
			// load persistent Xref by RDFId
			Xref x = (Xref) getByID(xref.getRDFId());
			// collect owners's ids (of requested type only)
			for(XReferrable xr: x.getXrefOf()) {
				if(clazz.isInstance(xr)) {
					// SMR special case (for cpath Warehouse)
					if(xr instanceof SmallMoleculeReference) {
						Collection<EntityReference> parentERs =
							((EntityReference) xr).getMemberEntityReferenceOf();
						if(parentERs.isEmpty()) {
							toReturn.add(xr.getRDFId());
							log.warn("WarehouseDAO.getByXref is going to " +
								"return SmallMoleculeReference");
						} else {
							for(EntityReference er : parentERs) {
								assert(er instanceof SmallMoleculeReference);
								toReturn.add(er.getRDFId());
							}
						}
					} else {
						toReturn.add(xr.getRDFId());
					}
				}
			}
		}
		
		return toReturn;
	}
	
	

	/* (non-Javadoc)
	 * @see cpath.dao.PaxtoolsDAO#getValidSubModel(java.util.Collection)
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED, readOnly=true)
	public Model getValidSubModel(Collection<String> ids) {
		Set<BioPAXElement> bioPAXElements = new HashSet<BioPAXElement>();
		for(String id : ids) {
			BioPAXElement bpe = getByID(id);
			if(bpe != null)
				bioPAXElements.add(bpe);
		}
		
		Completer c = new Completer(reader.getEditorMap());
		bioPAXElements = c.complete(bioPAXElements, null); //null - this model is used explicitly there
		Cloner cln = new Cloner(reader.getEditorMap(), factory);
		Model model = cln.clone(null, bioPAXElements);
		return model;
	}
	

	/* (non-Javadoc)
	 * @see cpath.dao.PaxtoolsDAO#count(java.lang.String, java.lang.Class)
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED, readOnly=true)
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
	 * TODO experimental, very promising!
	 * 
	 * it's bidirectional not recursive 
	 * (i.e., does not inits property's or inv. props' properties)
	 * 
	 */
	@Transactional
	@Override
	public void initialize(BioPAXElement element) {
		session().update(element);
		
		// init. biopax properties
		Set<PropertyEditor> editors = reader.getEditorMap().getEditorsOf(
				element);
		if (editors != null)
			for (PropertyEditor editor : editors) {
				Hibernate.initialize(editor.getValueFromBean(element)); // does collections (when multiple cardinality editor) as well!
			}

		// init. inverse object properties (xxxOf)
		Set<ObjectPropertyEditor> invEditors = reader.getEditorMap()
				.getInverseEditorsOf(element);
		if (invEditors != null)
			for (ObjectPropertyEditor editor : invEditors) {
				Hibernate.initialize(editor.getInverseValueFromBean(element)); // does collections as well!
			}
	}
	
	
	/*
	 * Initializes all the properties and properties's properties, etc.
	 * 
	 * TODO experimental
	 */
	// TODO what about inverse props? Btw, Traverser (standard) does not follow xxxOf properties...
	@Transactional
	private class ElementInitializer implements Visitor {
		private Traverser traverser;
		
		public ElementInitializer() {
			traverser = new Traverser(reader.getEditorMap(), this);
		}

		@Transactional
		public void initialize(Model source, BioPAXElement toBeCloned) {
			//TODO don't understand why subModel (it seems useless..) is here?
			Model subModel = factory.createModel();
			Hibernate.initialize(toBeCloned);
			traverser.traverse(toBeCloned, subModel);
		}

		@Transactional
		public void visit(BioPAXElement domain, Object range, Model targetModel, PropertyEditor editor)	{

			if (!targetModel.containsID(domain.getRDFId())) {
                targetModel.addNew(domain.getModelInterface(), domain.getRDFId());
			}
			
			if (domain instanceof BioPAXElement) {
				BioPAXElement bpeDomain = (BioPAXElement)domain;
				Hibernate.initialize(bpeDomain);
				if (range instanceof BioPAXElement) {
					BioPAXElement bpeRange = (BioPAXElement)range;
					if (!targetModel.containsID(bpeRange.getRDFId())) {
						traverser.traverse(bpeRange, targetModel);
					}
				}
				Object beanValue = editor.getValueFromBean(bpeDomain);
				if (beanValue instanceof Collection) {
					for (Object collectionValue : (Collection)beanValue) {
						if (collectionValue instanceof BioPAXElement) {
							Hibernate.initialize(collectionValue);
						}
					}
				}
			}
		}
	}
	
	/*
	 * Special object copier.
	 * Clones all the properties and properties's properties, etc.
	 * 
	 */
	// TODO problem here: not all inverse xxxOf() property values are set for this and dependent elements (only those are set that occur on the traverse's path)!
	@Transactional
	private class ElementCloner implements Visitor {
		private Traverser traverser;
		
		public ElementCloner() {
			traverser = new Traverser(reader.getEditorMap(), this);
		}

		@Transactional
		public Model clone(Model source, BioPAXElement toBeCloned) {
			Model subModel = factory.createModel();
			Hibernate.initialize(toBeCloned);
			traverser.traverse(toBeCloned, subModel);
			return subModel;
		}

		@Transactional
		public void visit(BioPAXElement domain, Object range, Model targetModel, PropertyEditor editor)
		{
			if (!targetModel.containsID(domain.getRDFId())) {
                targetModel.addNew(domain.getModelInterface(), domain.getRDFId());
			}
			
			if (range instanceof BioPAXElement)
			{
				Hibernate.initialize(range);
				BioPAXElement bpe = (BioPAXElement) range;
				if (!targetModel.containsID(bpe.getRDFId())) {
					traverser.traverse(bpe, targetModel);
				}
				editor.setValueToBean(targetModel.getByID(bpe.getRDFId()), 
						targetModel.getByID(domain.getRDFId()));
			} else {
				editor.setValueToBean(range, targetModel.getByID(domain.getRDFId()));
			}
		}
	}
	
	
	/*
	 * Iteratively updates all the properties and properties's properties.
	 * 
	 * TODO experimental, idea: deeply update prop. values from the db
	 */
	@Transactional
	private class ElementUpdater implements Visitor {
		private Traverser traverser;
		
		public ElementUpdater() {
			traverser = new Traverser(reader.getEditorMap(), this);
		}

		@Transactional
		public void update(Model target, BioPAXElement toUpdate) {
			traverser.traverse(toUpdate, target);
		}

		@Transactional
		public void visit(BioPAXElement domain, Object range, Model target, 
				PropertyEditor editor)	
		{
			if(range instanceof BioPAXElement) {
				BioPAXElement value = (BioPAXElement) range;
				migrateToTarget(domain, target, editor, value);
				traverser.traverse(value, target);
			}
		}

		@Transactional
		private void migrateToTarget(BioPAXElement update, Model target, 
				PropertyEditor editor, BioPAXElement value) {
			if (value!=null) {
				BioPAXElement newValue = target.getByID(value.getRDFId());
				if(newValue != null) { // replace only if exists
					editor.removeValueFromBean(value,update);
					editor.setValueToBean(newValue, update);
				}
			}
		}
	}

}


