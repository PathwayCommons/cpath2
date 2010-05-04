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
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.controller.AbstractTraverser;
import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.controller.PropertyFilter;
import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.impl.ModelImpl;
import org.biopax.paxtools.io.simpleIO.SimpleEditorMap;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;

import cpath.dao.PaxtoolsDAO;

import java.util.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static org.biopax.paxtools.impl.BioPAXElementImpl.*;

/**
 * Class which implements PaxtoolsModelQuery interface via persistence.
 */
@Transactional
@Repository
public class PaxtoolsHibernateDAO  implements PaxtoolsDAO {
	private final static String[] ALL_FIELDS = 
	{
		SEARCH_FIELD_AVAILABILITY,
		SEARCH_FIELD_COMMENT,
		SEARCH_FIELD_KEYWORD,
		SEARCH_FIELD_NAME,
		SEARCH_FIELD_TERM,
		SEARCH_FIELD_XREF_DB,
		SEARCH_FIELD_XREF_ID,
	};
	
	//private final static int BATCH_SIZE = 100;
	private static EditorMap editorMap3 = new SimpleEditorMap(BioPAXLevel.L3);
    private static Log log = LogFactory.getLog(PaxtoolsHibernateDAO.class);
	private SessionFactory sessionFactory;
	private SimpleMerger merger;
	
	@Transactional(propagation=Propagation.NESTED)
	public void init() {
		if(getModel()==null)  {
			if(log.isDebugEnabled())
				log.debug("Creating initial persistent model...");
			Session session = getSessionFactory().getCurrentSession();	
			Model model = new ModelImpl(BioPAXLevel.L3.getDefaultFactory());
			model.getNameSpacePrefixMap().put("", "http://pathwaycommons.org#");
			Long id = (Long) session.save(model);
			if(log.isDebugEnabled())
				log.debug("Model saved; id=" + id);
		}
		//List<?> list = session.createQuery("from ModelImpl").list();
	}

	
	/**
	 * @param simpleMerger the simpleMerger to set
	 */
	public void setMerger(SimpleMerger simpleMerger) {
		this.merger = simpleMerger;
	}
	
	
	// get/set methods used by spring
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	
	private Model getModel() {
		Session session = getSessionFactory().getCurrentSession();
		Model model = (Model) session.get(ModelImpl.class, 1L);
		return model;
	}
	
	
	/**
	 * Persists the given model to the db.
	 *
	 * @param model Model
	 */
	@Transactional(propagation=Propagation.REQUIRES_NEW)
	public void importModel(final Model model) {
		Session session = getSessionFactory().getCurrentSession();
		Model mainModel = (Model) session.get(ModelImpl.class, 1L);
		merger.merge(mainModel, model);
		session.update(mainModel);
	}

	
	/* (non-Javadoc)
	 * @see cpath.dao.PaxtoolsDAO#createIndex()
	 */
	@Override
	public void createIndex() {
		Session session = getSessionFactory().getCurrentSession();
		FullTextSession fullTextSession = Search.getFullTextSession(session);
		try {
			fullTextSession.createIndexer().startAndWait();
		} catch (InterruptedException e) {
			throw new RuntimeException("Faild to re-build index.");
		}
	}
	
	/**
	 * Persists the given model to the db.
	 *
	 * @param biopaxFile File
	 * @param createIndex boolean
	 * @throws FileNoteFoundException
	 */
	public void importModel(File biopaxFile) throws FileNotFoundException {

		if(log.isInfoEnabled())
		log.info("Creating biopax model using: " + biopaxFile.getAbsolutePath());

		// create a simple reader
		SimpleReader simple = new SimpleReader(BioPAXLevel.L3);
		
		// convert file to model
		Model model = simple.convertFromOWL(new FileInputStream(biopaxFile));

		if(log.isInfoEnabled())
			log.info("Model converted from OWL contains " + 
					model.getObjects().size() + " objects.");
		
		// save model
		importModel(model);

	}

	
    /**
     * This method returns the biopax element with the given id,
     * returns null if the object with the given id does not exist
     * in this model.
	 *
     * @param id String
	 * @param eager boolean indicating eager (as opposed to lazy) fetching
     * @return BioPAXElement
     */
	@Transactional(readOnly=true) // a hint to the driver to eventually optimize :)
    public BioPAXElement getByID(final String id, final boolean eager, boolean stateless) {
		BioPAXElement toReturn = null;
		
		String namedQuery = (eager) 
			? "org.biopax.paxtools.impl.elementByRdfIdEager"
				: "org.biopax.paxtools.impl.elementByRdfId";
		
		/*
		 * if(stateless) { StatelessSession session=...} did not work - 
		 * org.hibernate.SessionException: collections cannot be fetched by a stateless session...
		*/
		
		Session session = getSessionFactory().getCurrentSession();
		Query query = session.getNamedQuery(namedQuery);
		query.setString("rdfid", id);
		toReturn = (BioPAXElement)query.uniqueResult();
		
		return (stateless) ? detach(toReturn) : toReturn;
	}
	

	@Transactional(readOnly=true)
    public <T extends BioPAXElement> Set<T> getObjects(
    		final Class<T> filterBy, final boolean eager, final boolean stateless) {
		List<T> results = null;
		String query = "from " + filterBy.getCanonicalName();
		if(eager) 
			query += " fetch all properties";
		
		if (stateless) {
			StatelessSession session = getSessionFactory().openStatelessSession();
			results = session.createQuery(query).list();
			session.close();
		}
		else {
			Session session = getSessionFactory().getCurrentSession();
			results = session.createQuery(query).list();
		}
		
		Set<T> toReturn = new HashSet<T>();
		
		if(stateless) {
			toReturn.addAll(detach(results));
		} else {
			toReturn.addAll(results);
		}
		
		return toReturn;
	}


	 /**
	 * Searches the lucene index 
	 * and returns a set of objects 
	 * of the given class in the model that
	 * match the given query string, 
	 * 
     * @param query String
     * @param filterBy class to be used as a filter.
     * @return Set<BioPAXElement>
     */
	@Transactional(readOnly=true)
    public <T extends BioPAXElement> List<T> search(String query, Class<T> filterBy, boolean stateless) {

		log.info("query: " + query + ", filterBy: " + filterBy);

		// set to return
		List<T> toReturn = new ArrayList<T>();

        // create native lucene query
		MultiFieldQueryParser parser = new MultiFieldQueryParser(ALL_FIELDS, new StandardAnalyzer());
		org.apache.lucene.search.Query luceneQuery = null;
		try {
			luceneQuery = parser.parse(query);
		} catch (ParseException e) {
			log.info("parse exception: " + e.getMessage());
			return toReturn;
		}
		
		// get full text session
		FullTextSession fullTextSession = Search
			.getFullTextSession(getSessionFactory().getCurrentSession());
		// wrap Lucene query in a org.hibernate.Query
		/*
		 * TODO enable filtering by interface class
		 * Despite lucene search accepts concrete proxy/impl of BioPAX classes, this can be overridden as follows:
		 * - first, search in basic impl. class, i.e., fullTextSession.createFullTextQuery(luceneQuery, Level3ElementProxy.class)
		 * - finally, apply filter results using BioPAX interface and org.biopax.paxtools.util.ClassFilterSet
		 */
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery(luceneQuery, filterBy);
		// execute search
		List<T> results = hibQuery.list();
		
		return ((stateless && results.size()>0) ? detach(results) : results); // force detach
	}

    
    /**
     * Returns a copy of the BioPAX element with all 
     * its data properties set, but object properties -
     * stubbed with corresponding elements having only RDFID 
     * not empty.
     * 
     * TODO another method, such as detach(bpe, depth), may be also required
     * 
     * @param bpe
     * @return
     */
	@Transactional(readOnly=true)
	private BioPAXElement detach(BioPAXElement bpe) {
		
		if(bpe == null) return null;
		
		final BioPAXElement toReturn = BioPAXLevel.L3.getDefaultFactory()
			.reflectivelyCreate(bpe.getModelInterface());
		toReturn.setRDFId(bpe.getRDFId());
		AbstractTraverser traverser = new AbstractTraverser(
				editorMap3, 
				new PropertyFilter() {
					@Override
					public boolean filter(PropertyEditor editor) {
						return (!editor.getProperty().equals("nextStep"));
					}
				}) 
		{	
			@Override
			protected void visit(Object value, BioPAXElement bpe, Model m,
					PropertyEditor editor) 
			{
				editor.setPropertyToBean(toReturn, value);
			}
		};
		
		traverser.traverse(bpe, null);
		
		return toReturn;
	}

	/**
	 * Detaches a collection of BioPAX elements.
	 * 
	 * @see #detach(BioPAXElement)
	 * 
	 * @param <T> a BioPAX element subclass
	 * @param elements collection or persistent elements
	 * @return
	 */
	private <T extends BioPAXElement> List<T> detach(Collection<T> elements) {
		List<T> toReturn = new ArrayList<T>();
		for(T el : elements) {
			toReturn.add((T) detach(el));
		}
		return toReturn;
	}

	/* (non-Javadoc)
	 * @see org.biopax.paxtools.model.Model#add(org.biopax.paxtools.model.BioPAXElement)
	 */
	@Override
	public void add(BioPAXElement aBioPAXElement) {
		getModel().add(aBioPAXElement);
	}

	/* (non-Javadoc)
	 * @see org.biopax.paxtools.model.Model#addNew(java.lang.Class, java.lang.String)
	 */
	@Override
	public <T extends BioPAXElement> T addNew(Class<T> aClass, String id) {
		return getModel().addNew(aClass, id);
	}

	/* (non-Javadoc)
	 * @see org.biopax.paxtools.model.Model#contains(org.biopax.paxtools.model.BioPAXElement)
	 */
	@Override
	@Transactional(readOnly=true)
	public boolean contains(BioPAXElement aBioPAXElement) {
		return getModel().contains(aBioPAXElement);
	}

	/* (non-Javadoc)
	 * @see org.biopax.paxtools.model.Model#containsID(java.lang.String)
	 */
	@Override
	@Transactional(readOnly=true)
	public boolean containsID(String id) {
		return getModel().containsID(id);
	}

	/* (non-Javadoc)
	 * @see org.biopax.paxtools.model.Model#getByID(java.lang.String)
	 */
	@Override
	@Transactional(readOnly=true)
	public BioPAXElement getByID(String id) {
		return getModel().getByID(id);
	}

	/* (non-Javadoc)
	 * @see org.biopax.paxtools.model.Model#getIdMap()
	 */
	@Override
	public Map<String, BioPAXElement> getIdMap() {
		throw new UnsupportedOperationException("Deprecated and removed method");
	}

	/* (non-Javadoc)
	 * @see org.biopax.paxtools.model.Model#getLevel()
	 */
	@Override
	@Transactional(readOnly=true)
	public BioPAXLevel getLevel() {
		return getModel().getLevel();
	}

	/* (non-Javadoc)
	 * @see org.biopax.paxtools.model.Model#getNameSpacePrefixMap()
	 */
	@Override
	public Map<String, String> getNameSpacePrefixMap() {
		return getModel().getNameSpacePrefixMap();
	}

	/* (non-Javadoc)
	 * @see org.biopax.paxtools.model.Model#getObjects()
	 */
	@Override
	@Transactional(readOnly=true)
	public Set<BioPAXElement> getObjects() {
		return getModel().getObjects();
	}

	/* (non-Javadoc)
	 * @see org.biopax.paxtools.model.Model#getObjects(java.lang.Class)
	 */
	@Override
	@Transactional(readOnly=true)
	public <T extends BioPAXElement> Set<T> getObjects(Class<T> filterBy) {
		return getModel().getObjects(filterBy);
	}

	/* (non-Javadoc)
	 * @see org.biopax.paxtools.model.Model#isAddDependencies()
	 */
	@Override
	@Transactional(readOnly=true)
	public boolean isAddDependencies() {
		return getModel().isAddDependencies();
	}

	/* (non-Javadoc)
	 * @see org.biopax.paxtools.model.Model#remove(org.biopax.paxtools.model.BioPAXElement)
	 */
	@Override
	public void remove(BioPAXElement aBioPAXElement) {
		getModel().remove(aBioPAXElement);
	}

	/* (non-Javadoc)
	 * @see org.biopax.paxtools.model.Model#setAddDependencies(boolean)
	 */
	@Override
	public void setAddDependencies(boolean value) {
		getModel().setAddDependencies(value);
	}

	/* (non-Javadoc)
	 * @see org.biopax.paxtools.model.Model#setFactory(org.biopax.paxtools.model.BioPAXFactory)
	 */
	@Override
	public void setFactory(BioPAXFactory factory) {
		getModel().setFactory(factory);
	}

	/* (non-Javadoc)
	 * @see org.biopax.paxtools.model.Model#updateID(java.lang.String, java.lang.String)
	 */
	@Override
	public void updateID(String oldID, String newID) {
		getModel().updateID(oldID, newID);
	}
	
}

