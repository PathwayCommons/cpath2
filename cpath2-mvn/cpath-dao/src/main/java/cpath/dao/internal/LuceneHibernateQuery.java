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

// imports
import org.biopax.paxtools.model.BioPAXElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.hibernate.SessionFactory;
import org.hibernate.search.Search;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.springframework.transaction.annotation.Transactional;

import cpath.dao.LuceneQuery;

import java.util.Set;
import java.util.List;
import java.util.HashSet;

/**
 * Class used to manage lucene queries.
 */
public class LuceneHibernateQuery implements LuceneQuery {

    private static Log log = LogFactory.getLog(LuceneHibernateQuery.class);

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

	private SessionFactory sessionFactory;
	
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	
	
    /**
     * Search the lucene index for given string and returns
	 * and returns a set of objects in the model of the given class
	 * 
     * @param query String
     * @param filterBy class to be used as a filter.
     * @return Set<BioPAXElement>
     */
	@Transactional(readOnly=true)
    public <T extends BioPAXElement> Set<T> search(String query, final Class<T> filterBy) {

		log.info("query: " + query + ", filterBy: " + filterBy);

		// set to return
		Set toReturn = new HashSet();

        // create native lucene query
		MultiFieldQueryParser parser = new MultiFieldQueryParser(ALL_FIELDS, new StandardAnalyzer());
		org.apache.lucene.search.Query luceneQuery = null;
		try {
			luceneQuery = parser.parse(query);
		}
		catch (ParseException e) {
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
