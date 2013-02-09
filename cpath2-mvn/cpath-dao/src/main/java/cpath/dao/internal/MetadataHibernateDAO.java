package cpath.dao.internal;

// imports
import cpath.dao.IdMapping;
import cpath.dao.IdMappingFactory;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.validator.api.beans.Validation;
import org.biopax.validator.api.beans.ValidatorResponse;
import org.biopax.validator.api.ValidatorUtils;

import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Implementation of MetadatDAO interface.
 */
@Repository
class MetadataHibernateDAO  implements MetadataDAO {

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
			existing.setName(metadata.getName());
			existing.setDescription(metadata.getDescription());
			existing.setUrlToData(metadata.getUrlToData());
			existing.setUrlToHomepage(metadata.getUrlToHomepage());
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
		
		session.flush();
		session.clear();
		
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
    @SuppressWarnings("unchecked")
	@Transactional(propagation=Propagation.REQUIRED)
    public Collection<Metadata> getAllMetadata() {
		Session session = getSession();
		Query query = session.getNamedQuery("cpath.warehouse.beans.allProvider");
		List<Metadata> toReturn = query.list();
		return (toReturn.size() > 0) ? new ArrayList<Metadata>(toReturn) : Collections.EMPTY_SET;
	}
    
    
    /**
     * (non-Javadoc)
     * @see cpath.warehouse.metadata.MetadataDAO#importPathwayData;
     */
    @Transactional(propagation=Propagation.REQUIRED)
	public void importPathwayData(final PathwayData pathwayData) {

		Session session = getSession();
		// check for existing object
		PathwayData existing = getPathwayDataByIdentifierAndVersionAndFilenameAndDigest(pathwayData.getIdentifier(), 
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
			existing.setValid(pathwayData.getValid());
			session.update(existing);
		}
		
		// pathwayData contains very large data fields; so it's better to flush/free memory...
		session.flush();
		session.clear();
		
		if(log.isInfoEnabled())
			log.info("pathway data object has been sucessessfully saved or updated.");
    }

	
    @SuppressWarnings("unchecked") //the named query returns PathwayData list
	@Transactional(propagation=Propagation.REQUIRED)
    public Collection<PathwayData> getAllPathwayData() {
		Session session = getSession();
		Query query = session.getNamedQuery("cpath.warehouse.beans.allPathwayData");
		List<PathwayData> toReturn = query.list();
		return (toReturn.size() > 0) ? new ArrayList<PathwayData>(toReturn) : Collections.EMPTY_SET;
    }
    
    
    /*
     * (non-Javadoc)
     * @see cpath.warehouse.metadata.MetadataDAO#getPathwayDataByIdentifier;
     */
	@SuppressWarnings("unchecked") //the named query returns PathwayData list
	@Transactional(propagation=Propagation.REQUIRED)
    public Collection<PathwayData> getPathwayDataByIdentifier(final String identifier) {

		Session session = getSession();
		Query query = session.getNamedQuery("cpath.warehouse.beans.pathwayByIdentifier");
		query.setParameter("identifier", identifier);
		List<PathwayData> toReturn = query.list();
		return (toReturn.size() > 0) ? new ArrayList<PathwayData>(toReturn) : Collections.EMPTY_SET;
    }

    /*
     * (non-Javadoc)
     * @see cpath.warehouse.metadata.MetadataDAO#getPathwayDataByIdentifierAndVersion;
     */
    @SuppressWarnings("unchecked") //the named query returns PathwayData list
	@Transactional(propagation=Propagation.REQUIRED)
    public Collection<PathwayData> getPathwayDataByIdentifierAndVersion(final String identifier, final String version) {

		Session session = getSession();
		Query query = session.getNamedQuery("cpath.warehouse.beans.pathwayByIdentifierAndVersion");
		query.setParameter("identifier", identifier);
		query.setParameter("version", version);
		List<PathwayData> toReturn = query.list();
		return (toReturn.size() > 0) ? new ArrayList<PathwayData>(toReturn) : Collections.EMPTY_SET;
    }

    /**
     * (non-Javadoc)
     * @see cpath.warehouse.metadata.MetadataDAO#getPathwayDataByIdentifierAndVersionAndFilenameAndDigest;
     */
    @Transactional(propagation=Propagation.REQUIRED)
    public PathwayData getPathwayDataByIdentifierAndVersionAndFilenameAndDigest(final String identifier, 
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

    
	/* (non-Javadoc)
	 * @see cpath.warehouse.MetadataDAO#getPathwayData;
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

	
	
	@Transactional
	public ValidatorResponse getValidationReport(Integer pathwayDataPk) {
		ValidatorResponse resp = null;
		
		PathwayData pathwayData = getPathwayData(pathwayDataPk);
		if(pathwayData != null) {
			try {
				byte[] xmlResult = pathwayData.getValidationResults();	
				resp = (ValidatorResponse) ValidatorUtils
					.getUnmarshaller().unmarshal(new BufferedInputStream(new ByteArrayInputStream(xmlResult)));
			} catch (Throwable e) {
                log.error(e);
			}
		} 
		
		return resp;
	}
	
	
	@Transactional
	public ValidatorResponse getValidationReport(String metadataIdentifier) {
		ValidatorResponse response = null;
		
		// get validationResults from PathwayData beans
		Collection<PathwayData> pathwayDataCollection = getPathwayDataByIdentifier(metadataIdentifier);
		if (!pathwayDataCollection.isEmpty()) {
			// a new container to collect separately stored file validations
			response = new ValidatorResponse();
			for (PathwayData pathwayData : pathwayDataCollection) {				
				try {
					byte[] xmlResult = pathwayData.getValidationResults();
					// unmarshal and add
					ValidatorResponse resp = (ValidatorResponse) ValidatorUtils.getUnmarshaller()
						.unmarshal(new BufferedInputStream(new ByteArrayInputStream(xmlResult)));
					assert resp.getValidationResult().size() == 1; // current design (of the premerge pipeline)
					Validation validation = resp.getValidationResult().get(0); 
					if(validation != null)
						response.getValidationResult().add(validation);
				} catch (Exception e) {
                    log.error(e);
				}
			}
		} 
		
		return response;
	}
	
	
	@Transactional
	public Map<Integer, String> getPathwayDataInfo(String metadataIdentifier) {
		Collection<PathwayData> pathwayData = getPathwayDataByIdentifier(metadataIdentifier);
		Map<Integer, String> map = new TreeMap<Integer, String>();
		for(PathwayData pd : pathwayData)
			map.put(pd.getId(), pd.getFilename() 
				+ " (" + pd.getIdentifier() 
				+ "; version: " + pd.getVersion() 
				+ "; passed: " + pd.getValid() + ")");

		return map;
	}

	
	@Transactional
	@Override
	public void importIdMapping(Map<String, String> idMap, Class<? extends IdMapping> type) {
		Session ses = getSession();
		for(Map.Entry<String, String> ent : idMap.entrySet()) {
			ses.merge(IdMappingFactory.newIdMapping(type, ent.getKey(), ent.getValue()));
		}
	}

	
	@Transactional
	@Override
	public IdMapping getIdMapping(String db, String id, Class<? extends IdMapping> type) {
		id = IdMappingFactory.suggest(db, id);
		return (IdMapping) getSession().get(type, id);
	}

	
	@Transactional
	@Override
	public Collection<IdMapping> getAllIdMappings(Class<? extends IdMapping> type) {
		String queryName = IdMappingFactory.getAllMappingsQueryName(type);
		Query query = getSession().getNamedQuery(queryName);
		List<? extends IdMapping> toReturn = query.list();
		return (!toReturn.isEmpty()) 
				? new ArrayList<IdMapping>(toReturn) 
					: Collections.EMPTY_SET;
	}

	
	@Transactional
	@Override
	public Map<String,String> getIdMap(Class<? extends IdMapping> type) {
		Collection<IdMapping> entries = getAllIdMappings(type);
		Map<String,String> mapMap = new HashMap<String, String>(entries.size());
		for(IdMapping m : entries)
			mapMap.put(m.getIdentifier(), m.getAccession());
		
		return mapMap;
	}
	
}
