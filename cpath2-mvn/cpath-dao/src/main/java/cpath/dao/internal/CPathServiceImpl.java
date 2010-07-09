/**
 ** Copyright (c) 2010 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** both UofT and MSKCC have no obligations to provide maintenance, 
 ** support, updates, enhancements or modifications.  In no event shall
 ** UofT or MSKCC be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** UofT or MSKCC have been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this software; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA;
 ** or find it at http://www.fsf.org/ or http://www.gnu.org.
 **/


package cpath.dao.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import javax.validation.constraints.NotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.util.Version;
import org.biopax.paxtools.controller.Completer;
import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.hibernate.Session;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import cpath.dao.CPathService;
import cpath.dao.PaxtoolsDAO;

/**
 * Service tier class - to uniformly access 
 * BioPAX model (DAO) from console and web service 
 * applications (PaxtoolsDAO - either 'main' storage, 
 * or Warehouses's proteins and molecules)
 * 
 * @author rodche
 *
 */
@Service
@Repository
public class CPathServiceImpl implements CPathService {
	private static final Log log = LogFactory.getLog(CPathServiceImpl.class);
	
	@NotNull
	private PaxtoolsDAO dao;
	
	private final SimpleReader reader; // to allow BioPAX OWL in queries
	private final SimpleExporter exporter;
	private final SimpleMerger merger;
	private final StandardAnalyzer analyzer;
	private final MultiFieldQueryParser multiFieldQueryParser;
	
	public CPathServiceImpl(PaxtoolsDAO paxtoolsDAO) {
		this.reader = new SimpleReader(BioPAXLevel.L3);
		this.exporter = new SimpleExporter(BioPAXLevel.L3);
		this.merger = new SimpleMerger(reader.getEditorMap());
		this.dao = paxtoolsDAO;
		this.analyzer = new StandardAnalyzer(Version.LUCENE_29);
		this.multiFieldQueryParser = new MultiFieldQueryParser(
				Version.LUCENE_29, PaxtoolsHibernateDAO.ALL_FIELDS, this.analyzer);
	}


	@Override
	public Map<ResultMapKey, Object> list(
			Class<? extends BioPAXElement> biopaxClass, boolean countOnly) {
		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();

		try {
			if (countOnly) {
				Integer count = (Integer) ((PaxtoolsHibernateDAO) dao)
					.session().createQuery("select count(*) from "
						+ biopaxClass.getCanonicalName()).uniqueResult();
				map.put(ResultMapKey.COUNT, count.intValue());
			} else {
				Collection<String> data = dao.find("*", biopaxClass);
				map.put(ResultMapKey.DATA, data);
				map.put(ResultMapKey.COUNT, Integer.valueOf(data.size()));
			}
		} catch (Exception e) {
			map.put(ResultMapKey.ERROR, e.toString());
		}
		
		return map;
	}

	
	@Override
	public String toOWL(Model model) {
		if(model == null) return null;
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			exporter.convertToOWL(model, out);
		} catch (IOException e) {
			log.error(e);
		}
		return out.toString();
	}

	
	@Override
	public Map<ResultMapKey, Object> element(String id, OutputFormat format) {
		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();
		try {
			switch (format) {
			case BIOPAX:
				map = asBiopax(id);
			case BINARY_SIF:
				// TODO
				break;

			default:
			}
		} catch (Exception e) {
			map.put(ResultMapKey.ERROR, e.toString());
		}
		return map;
	}
	

	@Override
	//TODO does not do pagination, projections, no optimization..
	public Map<ResultMapKey, Object> list(String queryStr,
			Class<? extends BioPAXElement> biopaxClass) 
	{
		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();
		try {
			List<String> results =dao.find(queryStr, biopaxClass);
			map.put(ResultMapKey.DATA, results);
			map.put(ResultMapKey.COUNT, Integer.valueOf(results.size()));
		} catch (Exception e) {
			map.put(ResultMapKey.ERROR, e.toString());
		}

		return map;
	}	
	
	
	private int count(String search, Class<?> filterBy) {
		Session session = ((PaxtoolsHibernateDAO) dao).session();
		FullTextSession fullTextSession = Search.getFullTextSession(session);
		org.apache.lucene.search.Query luceneQuery = null;
		try {
			luceneQuery = multiFieldQueryParser.parse(search);
		}
		catch (ParseException e) {
			throw new RuntimeException("Lucene query parser exception", e);
		}
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery(luceneQuery,
				filterBy);
		return hibQuery.getResultSize();
	}
	
	
	private List<Object> fulltextSearch(String q, boolean countOnly, 
			int firstResult, int maxResults, Class<? extends BioPAXElement> filterBy, 
			Object... projections) 
	{	
		org.apache.lucene.search.Query luceneQuery = null;
		try {
			luceneQuery = multiFieldQueryParser.parse(q);
		}
		catch (ParseException e) {
			throw new RuntimeException("Lucene query parser exception", e);
		}

		Session session = ((PaxtoolsHibernateDAO)dao).session();
		// get full text session
		FullTextSession fullTextSession = Search.getFullTextSession(session);
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery(luceneQuery, filterBy);
		
		// TODO use pagination (add args)
		hibQuery.setFirstResult(firstResult);
		hibQuery.setMaxResults(maxResults);
		
		// TODO use projection (to get lucene score and other meta info)...
		//...
		
		// execute search
		return hibQuery.list();
	}

	
	Map<ResultMapKey, Object> asBiopax(String id) {
		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();
		
		Model m = dao.getLevel().getDefaultFactory().createModel();
		// find element by id
		BioPAXElement element = dao.getByID(id);
		if (element != null) {
			// auto-complete!
			// warn: completer instance has internal set of elements that is not cleared!
			Completer completer = new Completer(reader.getEditorMap());
			Set<BioPAXElement> elements = completer.complete(Collections
					.singletonList(element), dao);
			for (BioPAXElement bpe : elements) {
				m.add(bpe);
			}
			map.put(ResultMapKey.MODEL, m);
			map.put(ResultMapKey.DATA, toOWL(m));
		}
		
		return map;
	}
}
