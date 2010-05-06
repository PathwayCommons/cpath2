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
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.controller.*;
import org.biopax.paxtools.impl.ModelImpl;
import org.biopax.paxtools.io.simpleIO.SimpleEditorMap;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.hibernate.*;
import org.hibernate.search.*;
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
public class PaxtoolsHibernateDAO  extends ModelImpl implements PaxtoolsDAO {
	private static final long serialVersionUID = 1L;

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
	
	private static EditorMap editorMap3 = new SimpleEditorMap(BioPAXLevel.L3);
    private static Log log = LogFactory.getLog(PaxtoolsHibernateDAO.class);
	private SessionFactory sessionFactory;
	private SimpleMerger merger;
	
	
	protected PaxtoolsHibernateDAO() {
		super(BioPAXLevel.L3.getDefaultFactory());
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
	
	
	@Transactional
	public void init() {
		Set<BioPAXElement> elements = getElements(BioPAXElement.class, false, false);
		this.idMap.clear();
		for(BioPAXElement e : elements) {
			this.idMap.put(e.getRDFId(), e);
		}
		getNameSpacePrefixMap().put("", "http://pathwaycommons.org#");
	}
	
	
	/* (non-Javadoc)
	 * @see cpath.dao.PaxtoolsDAO#createIndex()
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRES_NEW)
	public void createIndex() {
		Session session = getSessionFactory().getCurrentSession();
		FullTextSession fullTextSession = Search.getFullTextSession(session);
		try {
			fullTextSession.createIndexer().startAndWait();
		} catch (InterruptedException e) {
			throw new RuntimeException("Faild to re-build index.");
		}
	}
	

	@Transactional
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

	
	@Transactional(propagation=Propagation.REQUIRED)
	public void importModel(final Model model) {
		for(BioPAXElement e : model.getObjects()) {
			this.add(e);
		}
		/* did not work either...
		merger.merge(this, model);
		*/
	}
	

    public BioPAXElement getElement(final String id, final boolean eager, boolean stateless) {
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
	

    public <T extends BioPAXElement> Set<T> getElements(final Class<T> filterBy, 
    		final boolean eager, final boolean stateless) 
    {	
		String query = "from " + filterBy.getCanonicalName();
		if(eager) 
			query += " fetch all properties";
		
		List<T> results = null;
		/*
		if (stateless) {
			StatelessSession session = getSessionFactory().openStatelessSession();
			results = session.createQuery(query).list();
			session.close();
		}
		else {
		*/
			Session session = getSessionFactory().getCurrentSession();
			results = session.createQuery(query).list();
		//}
		
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

		/*
		 * TODO enable filtering by interface class
		 * Despite lucene search accepts concrete proxy/impl of BioPAX classes, this can be overridden as follows:
		 * - first, search in basic impl. class, i.e., fullTextSession.createFullTextQuery(luceneQuery, Level3ElementProxy.class)
		 * - finally, apply filter results using BioPAX interface and org.biopax.paxtools.util.ClassFilterSet
		 */
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery(luceneQuery, filterBy);
		// execute search
		List<T> results = hibQuery.list();
		
		return ((stateless && results.size()>0) ? detach(results) : results); // force "real detaching"
	}

    
	/* (non-Javadoc)
	 * @see org.biopax.paxtools.model.Model#add(org.biopax.paxtools.model.BioPAXElement)
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public void add(BioPAXElement aBioPAXElement) {
		/* 
		 * Causes org.hibernate.InstantiationException: Cannot instantiate abstract class or interface:
		 *  org.biopax.paxtools.impl.level3.XReferrableImpl
		 */
		// BioPAXElement e = (BioPAXElement) getSessionFactory().getCurrentSession().merge(aBioPAXElement);
		
		Session session = getSessionFactory().getCurrentSession();
		session.save(aBioPAXElement);
		//session.flush();
		//session.refresh(aBioPAXElement);
		super.add(aBioPAXElement);
	}


	/* (non-Javadoc)
	 * @see org.biopax.paxtools.model.Model#remove(org.biopax.paxtools.model.BioPAXElement)
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public void remove(BioPAXElement aBioPAXElement) {
		BioPAXElement e = idMap.get(aBioPAXElement.getRDFId()); // gets persistent one
		super.remove(e); // removes from the idMap
		getSessionFactory().getCurrentSession().delete(e);
	}

	
	// ------ private methods --------
	
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
}

