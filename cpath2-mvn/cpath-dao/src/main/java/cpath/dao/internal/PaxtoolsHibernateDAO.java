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
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.proxy.level3.BioPAXElementProxy;
import org.biopax.paxtools.proxy.level3.BioPAXFactoryForPersistence;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import cpath.dao.PaxtoolsDAO;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Class which implements PaxtoolsModelQuery interface via persistence.
 */
@Repository
@Transactional
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
		Session session = getSessionFactory().getCurrentSession();
		FullTextSession fullTextSession = Search.getFullTextSession(session);

		int index = 0;
		for (BioPAXElement bpe : model.getObjects()) {
			if (log.isInfoEnabled())
				log.info("Saving biopax element, rdfID: " + bpe.getRDFId());
			index++;
			session.save(bpe);
			if (index % BATCH_SIZE == 0) {
				session.flush();
				session.clear();
				if (createIndex) {
					fullTextSession.flushToIndexes(); // apply changes to indexes
					fullTextSession.clear(); // clear since the queue is processed
				}
			}

			if (createIndex) {
				fullTextSession.flushToIndexes();
			}
		}
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
     * @return BioPAXElement
     */
	@Transactional(readOnly=true) // a hint to the driver to eventually optimize :)
    public BioPAXElement getByID(final String id) {
		// TODO 26-FEB-2010  BioPAXElementProxy changed to use auto-generated id instead of RDFId! Will wse session.getNamedQuery(..) for this!
    	Session session = getSession();
		Query query = session.getNamedQuery("org.biopax.paxtools.proxy.level3.elementByRdfId");
		query.setString("rdfid", id);
		return (BioPAXElement)query.uniqueResult();

    	/*
		Set<BioPAXElementProxy> returnClasses = search(id, BioPAXElementProxy.class);
		if(!returnClasses.isEmpty()) {
			return null;
		} else {
			return returnClasses.iterator().next();
		}
		*/
		
	}
	
	@Transactional(readOnly=true) // a hint to the driver to eventually optimize :)
    public BioPAXElement getByID(final Long id) {
		return (BioPAXElement)getSession().get(BioPAXElementProxy.class, id);
	}

    /**
     * This method returns a set of objects in the model of the given class.
     * Contents of this set should not be modified.
	 *
     * @param filterBy class to be used as a filter.
     * @return an unmodifiable set of objects of the given class.
     */
	@Transactional(readOnly=true)
    public <T extends BioPAXElement> Set<T> getObjects(final Class<T> filterBy) {
		List results = getSession().createQuery("from " + filterBy.getCanonicalName()).list();
		return (results.size() > 0) ? new HashSet(results) : new HashSet();
	}

	/**
	 * Given a unification xref, returns a matching biopax element.
	 *
	 * @param unificationXref UnificationXref
	 * @return BioPAXElement
	 */
	@Transactional(readOnly=true)
	public <T extends BioPAXElement> T getByUnificationXref(UnificationXref unificationXref) {

		// setup the query
		//Query query = session.createQuery("from org.biopax.paxtools.proxy.level3.UnificationXrefProxy as ux where ux.db = ? and ux.id = ?");
		//query.setParameter(0, unificationXref.getDb());
		//query.setParameter(1, unificationXref.getId());

		// get the result
		//UnificationXrefProxy uXref= (UnificationXrefProxy)query.uniqueResult();

	 	// no match, bail
		//if (uXref == null) {
		//	log.info("We cannot find a match for UnificationXref, DB: " + unificationXref.getDb() +
		//			 ", ID: " + unificationXref.getId());
		//	return null;
		//}
		// we have a match
		//else {
		//	log.info("We found a match for UnificationXref, DB: " + unificationXref.getDb() +
		//			 ", ID: " + unificationXref.getId());
			// get set of matches to the given unification xref
		//	Set<XReferrable> match = uXref.isXrefOf();
			// this has to be size one or db is corrupt
		//	log.info("UnificationXref.isXRefOf() returns + " + match.size() + "BioPAXElements");
		//	assert match.size() == 1;
		//	// outta here
		//	return (BioPAXElement)(match.toArray())[0];
		//	//return match.toArray(new BioPAXElement[match.size()])[0];
		//}

		return null;
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
    public <T extends BioPAXElement> Set<T> search(String query, Class<T> filterBy) {

		log.info("query: " + query + ", filterBy: " + filterBy);

		// set to return
		Set toReturn = new HashSet();

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
		List results = (List)hibQuery.list();
		
		if (results != null) {
			log.info("we have " + results.size() + " results.");
			toReturn.addAll(results);
		}
		else {
			log.info("we have no results");
		}
  
		// outta here
		return toReturn;
	}

}

