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
import org.apache.lucene.util.Version;
import org.biopax.paxtools.impl.level3.Level3FactoryImpl;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.util.IllegalBioPAXArgumentException;
import org.biopax.paxtools.controller.*;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.hibernate.*;
import org.hibernate.search.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;


import cpath.dao.PaxtoolsDAO;

import java.util.*;
import java.io.*;

import static org.biopax.paxtools.impl.BioPAXElementImpl.*;


@Transactional
@Repository
public class PaxtoolsHibernateDAO implements PaxtoolsDAO
{
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

	private static Log log = LogFactory.getLog(PaxtoolsHibernateDAO.class);
	private SessionFactory sessionFactory;
	private final Map<String, String> nameSpacePrefixMap;
	private final BioPAXLevel level;
	private BioPAXFactory factory;
	private BottomUpMerger merger;
	private BioPAXIOHandler reader;
	private SimpleExporter exporter;
	private boolean addDependencies = false;

	protected PaxtoolsHibernateDAO()
	{
		this.level = BioPAXLevel.L3;
		this.factory = level.getDefaultFactory();
		this.nameSpacePrefixMap = new HashMap<String, String>();
		nameSpacePrefixMap.put("", "http://pathwaycommons.org#");
		// set default Biopax reader, exporter, and merger
		reader = new SimpleReader(new internalFactory(),BioPAXLevel.L3);
		exporter = new SimpleExporter(level);
		merger = new BottomUpMerger(reader.getEditorMap());
	}

	/**
	 * @param simpleMerger the simpleMerger to set
	 */
	public void setMerger(BottomUpMerger simpleMerger)
	{
		this.merger = simpleMerger;
	}

	public BottomUpMerger getMerger()
	{
		return merger;
	}

	// get/set methods used by spring

	public SessionFactory getSessionFactory()
	{
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory)
	{
		this.sessionFactory = sessionFactory;
	}

	public SimpleExporter getExporter()
	{
		return exporter;
	}

	public void setExporter(SimpleExporter exporter)
	{
		this.exporter = exporter;
	}

	public BioPAXIOHandler getReader()
	{
		return reader;
	}

	public void setReader(BioPAXIOHandler reader)
	{
		this.reader = reader;
	}

	/* (non-Javadoc)
		 * @see cpath.dao.PaxtoolsDAO#createIndex()
		 */

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void createIndex()
	{
		FullTextSession fullTextSession = Search.getFullTextSession(session());
		try
		{
			fullTextSession.createIndexer().startAndWait();
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException("Faild to re-build index.");
		}
	}

    @Transactional
	public void importModel(File biopaxFile) throws FileNotFoundException
	{
		if (log.isInfoEnabled())
		{
			log.info("Creating biopax model using: " + biopaxFile.getAbsolutePath());
		}

		// convert file to model
		reader.convertFromOWL(new FileInputStream(biopaxFile));

	}

	@Transactional
	public void importModel(final Model model)
	{
		merger.merge(this, model);
		
		/*
		Session session = session();
		int i = 0;
		for(BioPAXElement e : model.getObjects()) {
			if (log.isInfoEnabled())
				log.info("Saving biopax element, rdfID: " 
						+ e.getRDFId());
			session.save(e);
			++i;
			if(i % 50 == 0) {
				session.flush();
				session.clear();
			}
		}
		*/
	}


	@Transactional
	public BioPAXElement getElement(final String id, final boolean eager,
	                                boolean forceDetach)
	{
		BioPAXElement toReturn = null;
		String namedQuery = (eager)
		                    ? "org.biopax.paxtools.impl.elementByRdfIdEager"
		                    : "org.biopax.paxtools.impl.elementByRdfId";
		Session session = session();


		try
		{
			toReturn = (BioPAXElement) session.getNamedQuery(namedQuery)
					.setString("rdfid", id).uniqueResult();

		}
		catch (Exception e)
		{
			log.error("getElement(" + id + ") failed. " + e);

		}
		

		return (toReturn != null && forceDetach) ? detach(toReturn) : toReturn;
	}


	@Transactional
	public <T extends BioPAXElement> Set<T> getElements(final Class<T> filterBy,
	                                                    final boolean eager,
	                                                    final boolean forceDetach)
	{
		String query = "from " + filterBy.getCanonicalName();
		if (eager)
		{
			query += " fetch all properties";
		}

		List<T> results = null;
		results = session().createQuery(query).list();

		Set<T> toReturn = new HashSet<T>();

		if (forceDetach)
		{
			toReturn.addAll(detach(results));
		}
		else
		{
			toReturn.addAll(results);
		}

		return toReturn;
	}


	/*
	 * TODO 'filterBy' to be a BioPAX interface, not a concrete class.
	 * Currently, search expects concrete annotated BioPAX classes only;
	 * this, probably, has to change as follows:
	 * - always search using the basic class
	 * - apply org.biopax.paxtools.util.ClassFilterSet
	 */

	public <T extends BioPAXElement> List<T> search(String query,
	                                                Class<T> filterBy, boolean forceDetach)
	{
		if (log.isInfoEnabled())
		{
			log.info("query: " + query + ", filterBy: " + filterBy);
		}

		// set to return
		List<T> toReturn = new ArrayList<T>();

		// create native lucene query
		MultiFieldQueryParser parser = new MultiFieldQueryParser(
				Version.LUCENE_29, ALL_FIELDS, new StandardAnalyzer(Version.LUCENE_29));
		org.apache.lucene.search.Query luceneQuery = null;
		try
		{
			luceneQuery = parser.parse(query);
		}
		catch (ParseException e)
		{
			log.info("parse exception: " + e.getMessage());
			return toReturn;
		}

		// get full text session
		FullTextSession fullTextSession = Search.getFullTextSession(session());
		FullTextQuery hibQuery = fullTextSession.createFullTextQuery(luceneQuery, filterBy);
		// execute search
		List<T> results = hibQuery.list();

		return ((forceDetach && results.size() > 0) ? detach(results) : results);
		// - experimental - forcing the "real detaching" by copying...
	}


	/* (non-Javadoc)
		 * @see org.biopax.paxtools.model.Model#add(org.biopax.paxtools.model.BioPAXElement)
		 */

	@Override
	@Transactional
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
			session().save(aBioPAXElement);

		}
	}


	/* (non-Javadoc)
	 * @see org.biopax.paxtools.model.Model#remove(org.biopax.paxtools.model.BioPAXElement)
	 */

	@Override
	public void remove(BioPAXElement aBioPAXElement)
	{
		/*
		// may work only for elements that were previously detached, i.e., returned by getById, search, etc. methods
		session().update(aBioPAXElement); // re-associate - make persistent
		session().delete(aBioPAXElement); 
		// TODO compare to another approach - find persistent one by RDFId, delete it...
		*/
		BioPAXElement bpe = getByID(aBioPAXElement.getRDFId());
		session().delete(bpe);
	}


	@Override
	@Transactional
	public <T extends BioPAXElement> T addNew(Class<T> type, String id)
	{
		T bpe = factory.reflectivelyCreate(type);
		bpe.setRDFId(id);
		add(bpe);
		return bpe;
	}


	@Override
	@Transactional
	public boolean contains(BioPAXElement bpe)
	{
		return containsID(bpe.getRDFId());
	}


	@Override
	@Transactional
	public boolean containsID(String id)
	{
		return getByID(id) != null;
	}


	@Override
	@Transactional
	public BioPAXElement getByID(String id)
	{
		return getElement(id, false, false);
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


	/*
	 * (non-Javadoc)
	 * @see org.biopax.paxtools.model.Model#getObjects()
	 */

	@Override
	public Set<BioPAXElement> getObjects()
	{
		return Collections.unmodifiableSet(
				getElements(BioPAXElement.class, true, false));
	}


	@Override
	public <T extends BioPAXElement> Set<T> getObjects(Class<T> clazz)
	{
		return Collections.unmodifiableSet(getElements(clazz, true, false));
	}


	@Override
	public boolean isAddDependencies()
	{
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
		if (factory.getLevel() == this.level)
		{
			this.factory = factory;
		}
		else
		{
			throw new IllegalAccessError("Cannot use this Biopax factory!");
		}
	}


	@Override
	@Transactional
	public void updateID(String oldId, String newId)
	{
		BioPAXElement bpe = getByID(oldId);
		bpe.setRDFId(newId);
		session().refresh(bpe); // TODO is refresh required?
	}


	// ------ private methods --------

	// gets current stateful session

	private Session session()
	{
		return getSessionFactory().getCurrentSession();
	}


	/**
	 * Returns a copy of the BioPAX element with all its data properties set, but object properties -
	 * stubbed with corresponding elements having only RDFID not empty.
	 * <p/>
	 * TODO another method, such as detach(bpe, depth), may be also required
	 *
	 * @param bpe
	 * @return
	 */
	private BioPAXElement detach(BioPAXElement bpe)
	{

		if (bpe == null)
		{
			return null;
		}

		final BioPAXElement toReturn = BioPAXLevel.L3.getDefaultFactory()
				.reflectivelyCreate(bpe.getModelInterface());
		toReturn.setRDFId(bpe.getRDFId());
		AbstractTraverser traverser = new AbstractTraverser(
				reader.getEditorMap(),
				new PropertyFilter()
				{
					@Override
					public boolean filter(PropertyEditor editor)
					{
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
	 * @param <T>      a BioPAX element subclass
	 * @param elements collection or persistent elements
	 * @return
	 * @see #detach(BioPAXElement)
	 */
	private <T extends BioPAXElement> List<T> detach(Collection<T> elements)
	{
		List<T> toReturn = new ArrayList<T>();
		for (T el : elements)
		{
			toReturn.add((T) detach(el));
		}
		return toReturn;
	}

	private class internalFactory extends Level3FactoryImpl
	{
		@Override
		public Model createModel()
		{
			return PaxtoolsHibernateDAO.this;
		}
	}
}


