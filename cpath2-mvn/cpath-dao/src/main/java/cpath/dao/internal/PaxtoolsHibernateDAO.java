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
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.proxy.level3.BioPAXFactoryForPersistence;
import org.biopax.paxtools.proxy.level3.Level3ElementProxy;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import cpath.dao.PaxtoolsDAO;

import java.util.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Class which implements PaxtoolsModelQuery interface via persistence.
 */
@Transactional
@Repository
public class PaxtoolsHibernateDAO  implements PaxtoolsDAO {
	private final static String SEARCH_FIELD_AVAILABILITY = "availability";
	private final static String SEARCH_FIELD_COMMENT = "comment";
	private final static String SEARCH_FIELD_KEYWORD = "keyword";
	private final static String SEARCH_FIELD_NAME = "name";
	private final static String SEARCH_FIELD_TERM = "term";
	private final static String SEARCH_FIELD_XREF_DB = "xref_db";
	private final static String SEARCH_FIELD_XREF_ID = "xref_id";
	private final static String[] ALL_FIELDS = {SEARCH_FIELD_AVAILABILITY,
												SEARCH_FIELD_COMMENT,
												SEARCH_FIELD_KEYWORD,
												SEARCH_FIELD_NAME,
												SEARCH_FIELD_TERM,
												SEARCH_FIELD_XREF_DB,
												SEARCH_FIELD_XREF_ID};
	private final static int BATCH_SIZE = 100;
	
	
    private static Log log = LogFactory.getLog(PaxtoolsHibernateDAO.class);
	private SessionFactory sessionFactory;

	// get/set methods used by spring
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	// a shortcut to get current session
	private Session getSession() {
		return getSessionFactory().getCurrentSession();
	}
	
	/**
	 * Persists the given model to the db.
	 * 
	 * TODO take care of Model, as, in fact, now persisted and indexed here are individual objects
	 * 
	 * TODO Use either manual "batch" inserts or StatelessSession (less convenient); still must create lucene indexes!
	 *
	 * @param model Model
	 * @param createIndex boolean
	 */
	@Transactional
	public void importModel(final Model model, final boolean createIndex) {
		/*
		Session session = getSessionFactory().getCurrentSession();
		FullTextSession fullTextSession = Search.getFullTextSession(session);
		int index = 0;
		for (BioPAXElement bpe : model.getObjects()) {
			if (log.isInfoEnabled())
				log.info("Saving biopax element, rdfID: " + bpe.getRDFId());
			index++;
			session.saveOrUpdate(bpe);
			//session.save(bpe);
			if (index % BATCH_SIZE == 0) {
				session.flush();
				session.clear();
				if (createIndex) {
					fullTextSession.flushToIndexes(); // apply changes to indexes
					fullTextSession.clear(); // clear since the queue is processed
				}
			}
			session.flush();
			session.clear();
			if (createIndex) {
				fullTextSession.flushToIndexes();
				fullTextSession.clear();
			}
		}
		*/
	
		Session session = getSession();
		FullTextSession fullTextSession = Search.getFullTextSession(session);
		session.save(model);
		fullTextSession.flushToIndexes();
	
		//StatelessSession session = getSessionFactory().openStatelessSession();
		//session.insert(model);
		//session.close();
		//FullTextSession fullTextSession = Search.getFullTextSession(getSession());
		//fullTextSession.index(model);
	}

	
	/**
	 * Persists the given model to the db.
	 *
	 * @param biopaxFile File
	 * @param createIndex boolean
	 * @throws FileNoteFoundException
	 */
	public void importModel(File biopaxFile, final boolean createIndex) throws FileNotFoundException {

		log.info("Creating biopax model using: " + biopaxFile.getAbsolutePath());

		// create a simple reader
		SimpleReader simple = new SimpleReader(new BioPAXFactoryForPersistence(), BioPAXLevel.L3);
		
		// convert file to model
		Model model = simple.convertFromOWL(new FileInputStream(biopaxFile));

		// import the model
		importModel(model, createIndex);
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
    public BioPAXElement getByID(final String id, final boolean eager) {
		// TODO 26-FEB-2010  BioPAXElementProxy changed to use auto-generated id instead of RDFId! Will wse session.getNamedQuery(..) for this!
    	Session session = getSession();
		String namedQuery = (eager) ? "org.biopax.paxtools.proxy.level3.elementByRdfId" :
			"org.biopax.paxtools.proxy.level3.elementByRdfIdEager";
		Query query = session.getNamedQuery(namedQuery);
		query.setString("rdfid", id);
		return (BioPAXElement)query.uniqueResult();
	}
	
	
	@Transactional(readOnly=true)
    public BioPAXElement getByID(final Long id, boolean eager) {
		if(eager) {
			return (BioPAXElement)getSession().load(Level3ElementProxy.class, id);
		} else {
			return (BioPAXElement)getSession().get(Level3ElementProxy.class, id);
		}
	}


	@Transactional(readOnly=true)
    public <T extends BioPAXElement> Set<T> getObjects(final Class<T> filterBy, boolean eager) {
		List results = null;
		
		if (eager) {
			results = getSession().createQuery("from " + filterBy.getCanonicalName() + " fetch all properties").list();
		}
		else {
			results = getSession().createQuery("from " + filterBy.getCanonicalName()).list();
		}
		
		return (results.size() > 0) ? new HashSet(results) : new HashSet();
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
    public <T extends BioPAXElement> List<T> search(String query, Class<T> filterBy) {

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
		FullTextSession fullTextSession = Search.getFullTextSession(getSessionFactory().getCurrentSession());
		// wrap Lucene query in a org.hibernate.Query
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery(luceneQuery, filterBy);
		// execute search
		List<T> results = hibQuery.list();
		
		if(results != null && !results.isEmpty()) {
			if(log.isInfoEnabled())	
				log.info("we have " + results.size() + " results.");
		 	toReturn = Collections.synchronizedList(results);
		} else {
			if(log.isInfoEnabled())	
			 	log.info("we have no results");
		}
  	
		return toReturn;
	}

	/* (non-Javadoc)
	 * @see cpath.dao.PaxtoolsDAO#search(java.lang.String, java.lang.Class)
	 */
	@Override
	@Transactional(readOnly=true)
	public List<String> searchForIds(String query,
			Class<? extends BioPAXElement> filterBy) {
		List<String> ids = new ArrayList<String>();
		List<? extends BioPAXElement> results = search(query, filterBy);
		for(BioPAXElement e : results) {
			ids.add(e.getRDFId());
		}
		
		return ids;
	}

}

