package cpath.warehouse.internal;

// imports
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.Search;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashSet;
import java.util.Collection;

/**
 * Implemenation of MetadatDAO interface.
 */
@Repository
public class MetadataHibernateDAO  implements MetadataDAO {

    // log
    private static Log log = LogFactory.getLog(MetadataHibernateDAO.class);

    // session factory prop/methods used by spring
    private SessionFactory sessionFactory;
    
    public SessionFactory getSessionFactory() { return sessionFactory; }
    
    public void setSessionFactory(SessionFactory sessionFactory) {
    	this.sessionFactory = sessionFactory;
    }
	
    // a shortcut to get current session
    private Session getSession() {
    	return getSessionFactory().getCurrentSession();
    }
	
    /**
     * (non-Javadoc)
     * @see cpath.warehouse.metadata.MetadataDAO#importMetadata;
     */
    @Transactional(propagation=Propagation.REQUIRED)
	public void importMetadata(final Metadata metadata) {

		Session session = getSession();
		// check for existing object
		Metadata existing = getMetadataByIdentifier(metadata.getIdentifier());
		if (existing != null) {
			if(log.isInfoEnabled())
				log.info("Metadata object with identifier: " + metadata.getIdentifier() 
					+ " already exists, manually merging.");
			existing.setVersion(metadata.getVersion());
			existing.setReleaseDate(metadata.getReleaseDate());
			existing.setURLToData(metadata.getURLToData());
			existing.setIcon(metadata.getIcon());
			existing.setType(metadata.getType());
			session.update(existing);
		}
		else {
			if(log.isInfoEnabled())
				log.info("Metadata object with identifier: " 
					+ metadata.getIdentifier() + " does not exist, saving.");
			session.save(metadata);
		}
		
		if(log.isInfoEnabled())
			log.info("metadata object has been sucessessfully saved or merged.");
    }

    /**
     * (non-Javadoc)
     * @see cpath.warehouse.MetadataDAO#getByID
     */
    @Transactional(propagation=Propagation.REQUIRED)
    public Metadata getMetadataByIdentifier(final String identifier) {
		Session session = getSession();
		Query query = session.getNamedQuery("cpath.warehouse.beans.providerByIdentifier");
		query.setParameter("identifier", identifier);
		return (Metadata)query.uniqueResult();
    }

    /**
     * This method returns all Metadata objects in warehouse.
	 *
     * @return Collection<Metadata>
     */
    @Transactional(propagation=Propagation.REQUIRED)
    public Collection<Metadata> getAll() {
		Session session = getSession();
		Query query = session.getNamedQuery("cpath.warehouse.beans.allProvider");
		List toReturn = query.list();
		return (toReturn.size() > 0) ? new HashSet(toReturn) : new HashSet();
	}
    
    
    @Transactional(propagation=Propagation.REQUIRED)
	public void importPathwayData(final PathwayData pathwayData) {

		Session session = getSession();
		// check for existing object
		PathwayData existing = getByIdentifierAndVersionAndFilenameAndDigest(pathwayData.getIdentifier(), 
				pathwayData.getVersion(), pathwayData.getFilename(), pathwayData.getDigest());
		
		if (existing == null) {
			if(log.isInfoEnabled())
				log.info("Saving PathwayData with identifier: " 
					+ pathwayData.getIdentifier() +
					 " and file: " + pathwayData.getFilename() +
					 " and version: " + pathwayData.getVersion() +
					 " and digest: " + pathwayData.getDigest());
			
			session.save(pathwayData);
		}
		else {
			if(log.isInfoEnabled())
				log.info("Updating PathwayData with identifier: "
					+ pathwayData.getIdentifier() +
					 " and file: " + pathwayData.getFilename() +
					 " and version: " + pathwayData.getVersion() + 
					 " and digest: " + pathwayData.getDigest());
			
			existing.setPathwayData(pathwayData.getPathwayData());
			existing.setValidationResults(pathwayData.getValidationResults());
			existing.setPremergeData(pathwayData.getPremergeData());
			session.update(existing);
		}
		
		if(log.isInfoEnabled())
			log.info("pathway data object has been sucessessfully saved or updated.");
    }


    @Transactional(propagation=Propagation.REQUIRED)
    public Collection<PathwayData> getPathwayDataByIdentifier(final String identifier) {

		Session session = getSession();
		Query query = session.getNamedQuery("cpath.warehouse.beans.pathwayByIdentifier");
		query.setParameter("identifier", identifier);
		List toReturn = query.list();
		return (toReturn.size() > 0) ? new HashSet(toReturn) : new HashSet();
    }


    @Transactional(propagation=Propagation.REQUIRED)
    public Collection<PathwayData> getByIdentifierAndVersion(final String identifier, final String version) {

		Session session = getSession();
		Query query = session.getNamedQuery("cpath.warehouse.beans.pathwayByIdentifierAndVersion");
		query.setParameter("identifier", identifier);
		query.setParameter("version", version);
		List toReturn = query.list();
		return (toReturn.size() > 0) ? new HashSet(toReturn) : new HashSet();
    }


    @Transactional(propagation=Propagation.REQUIRED)
    public PathwayData getByIdentifierAndVersionAndFilenameAndDigest(final String identifier, 
    		final String version, final String filename, final String digest) 
    {
		Session session = getSession();
		Query query = session.getNamedQuery("cpath.warehouse.beans.pathwayByIdentifierAndVersionAndFilenameAndDigest");
		query.setParameter("identifier", identifier);
		query.setParameter("version", version);
		query.setParameter("filename", filename);
		query.setParameter("digest", digest);
		return (PathwayData)query.uniqueResult();
    }
    
    
	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public void createIndex() {
		FullTextSession fullTextSession = Search.getFullTextSession(getSession());
		MassIndexer indexer = fullTextSession.createIndexer();
		try {
			indexer.batchSizeToLoadObjects(20)
				.purgeAllOnStart(true)
				.optimizeOnFinish(true)
				.startAndWait();
		} catch (InterruptedException e) {
			throw new RuntimeException("Index re-build is interrupted.");
		}
	}

	/* (non-Javadoc)
	 * @see cpath.warehouse.MetadataDAO#getById(java.lang.Integer)
	 */
	@Override
	@Transactional
	public PathwayData getPathwayData(Integer pathwayId) {
		PathwayData pd = (PathwayData) getSession().get(PathwayData.class, pathwayId);
		if(pd != null) {
			Hibernate.initialize(pd);
			return pd;
		} else {
			return null;
		}
	}
}
