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
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.proxy.level3.BioPAXElementProxy;
import org.biopax.paxtools.proxy.level3.BioPAXFactoryForPersistence;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import cpath.dao.LuceneQuery;
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
public class PaxtoolsHibernateDAO extends HibernateDaoSupport implements PaxtoolsDAO {

    private static Log log = LogFactory.getLog(PaxtoolsHibernateDAO.class);
	private LuceneQuery luceneQuery;

	// get/set methods used by spring
	public LuceneQuery getLuceneQuery() { return luceneQuery; }
	
	public void setLuceneQuery(LuceneQuery luceneQuery) {
		this.luceneQuery = luceneQuery;
	}

	/**
	 * Persists the given model to the db.
	 *
	 * @param model Model
	 */
	@Transactional
	public void importModel(final Model model) {
		// indexing will not kick off until a commit occurs
		Session session = getSessionFactory().openSession();
		for (BioPAXElement bpe : model.getObjects()) {
			log.info("Saving biopax element, rdfID: " + bpe.getRDFId());
			session.merge(bpe);
		}
	}

	/**
	 * Persists the given model to the db.
	 *
	 * @param biopaxFile File
	 * @throws FileNoteFoundException
	 */
	public void importModel(File biopaxFile) throws FileNotFoundException {

		log.info("Creating biopax model using: " + biopaxFile.getAbsolutePath());

		// create a simple reader
		SimpleReader simple = new SimpleReader(new BioPAXFactoryForPersistence(), BioPAXLevel.L3);
		
		// convert file to model
		Model model = simple.convertFromOWL(new FileInputStream(biopaxFile));

		// import the model
		importModel(model);
	}

    /**
     * This method returns the biopax element with the given id,
     * returns null if the object with the given id does not exist
     * in this model.
	 *
     * @param id String
     * @return BioPAXElement
     */
    public <T extends BioPAXElement> T getByID(final String id) {

		return (T)getHibernateTemplate().get(BioPAXElementProxy.class, id);
	}

    /**
     * This method returns a set of objects in the model of the given class.
     * Contents of this set should not be modified.
	 *
     * @param filterBy class to be used as a filter.
     * @return an unmodifiable set of objects of the given class.
     */
    public <T extends BioPAXElement> Set<T> getObjects(final Class<T> filterBy) {

		List results = getHibernateTemplate().loadAll(filterBy);
		return (results.size() > 0) ? new HashSet(results) : new HashSet();
	}

	/**
	 * Given a unification xref, returns a matching biopax element.
	 *
	 * @param unificationXref UnificationXref
	 * @return BioPAXElement
	 */
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
	 * Given a query string, returns a set of objects in the model that
	 * match the string.
	 *
	 * @param query String
     * @param filterBy class to be used as a filter.
	 * @return an unmodifiable set of objects that match the query.
	 */
	public <T extends BioPAXElement> Set<T> getByQueryString(String query, Class<T> filterBy) {

		// outta here
		return luceneQuery.search(query, filterBy);
	}
}