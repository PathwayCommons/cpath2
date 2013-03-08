package cpath.dao.internal;


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
	

    @Override
    @Transactional(propagation=Propagation.REQUIRED)
	public void saveMetadata(final Metadata metadata) {
		Session session = getSession();
		session.merge(metadata);
		session.flush();
		session.clear();
		
		if(log.isInfoEnabled())
			log.info("metadata object " + metadata.getIdentifier() +
				"has been sucessessfully saved or merged.");
    }


    @Override
    @Transactional(propagation=Propagation.REQUIRED)
    public Metadata getMetadataByIdentifier(final String identifier) {
		Session session = getSession();
		Query query = session.getNamedQuery("cpath.warehouse.beans.providerByIdentifier");
		query.setParameter("identifier", identifier);
		Metadata m = (Metadata)query.uniqueResult();
		if(m != null) {
			Hibernate.initialize(m);
			Hibernate.initialize(m.getPathwayData());
		}	
		return m;
    }


    @SuppressWarnings("unchecked")
	@Transactional(propagation=Propagation.REQUIRED)
    @Override
    public Collection<Metadata> getAllMetadata() {
		Session session = getSession();
		Query query = session.getNamedQuery("cpath.warehouse.beans.allProvider");
		List<Metadata> toReturn = query.list();
		return (toReturn.size() > 0) ? new ArrayList<Metadata>(toReturn) : Collections.EMPTY_SET;
	}
    
    
    @Override
    @Transactional(propagation=Propagation.REQUIRED)
	public void savePathwayData(final PathwayData pathwayData) {

		Session session = getSession();

		log.info("Saving PathwayData with identifier: " 
			+ pathwayData.getIdentifier() +
				" and file: " + pathwayData.getFilename());
			
		session.merge(pathwayData);

		// contains large data fields; so it's better to flush
		session.flush();
		session.clear();
		
		if(log.isInfoEnabled())
			log.info("pathway data object has been sucessessfully saved or updated.");
    }

    
	@Override
    @SuppressWarnings("unchecked") //the named query returns PathwayData list
	@Transactional(propagation=Propagation.REQUIRED)
    public Collection<PathwayData> getAllPathwayData() {
		Session session = getSession();
		Query query = session.getNamedQuery("cpath.warehouse.beans.allPathwayData");
		List<PathwayData> toReturn = query.list();
		return (toReturn.size() > 0) ? new ArrayList<PathwayData>(toReturn) : Collections.EMPTY_SET;
    }
    
    
	@Override
	@SuppressWarnings("unchecked") //the named query returns PathwayData list
	@Transactional(propagation=Propagation.REQUIRED)
    public Collection<PathwayData> getPathwayDataByIdentifier(final String identifier) {
		Session session = getSession();
		Query query = session.getNamedQuery("cpath.warehouse.beans.pathwayByIdentifier");
		query.setParameter("identifier", identifier);
		List<PathwayData> toReturn = query.list();
		return (toReturn.size() > 0) ? new ArrayList<PathwayData>(toReturn) : Collections.EMPTY_SET;
    }
	
    
	@Override
	@Transactional
	public PathwayData getPathwayData(Integer pathwayId) {
		PathwayData pd = (PathwayData) getSession().get(PathwayData.class, pathwayId);
		if(pd != null) {
			Hibernate.initialize(pd);
		} 
		
		return pd;
	}
	
	
	@Override
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
	
	
	@Override
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
	
	
	@Override
	@Transactional
	public Map<Integer, String> getPathwayDataInfo(String metadataIdentifier) {
		Collection<PathwayData> pathwayData = getPathwayDataByIdentifier(metadataIdentifier);
		Map<Integer,String> map = new TreeMap<Integer,String>();
		for(PathwayData pd : pathwayData)
			map.put(pd.getId(), pd.getFilename() 
				+ " (" + pd.getIdentifier() 
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

	
	@Transactional
	@Override
	public Metadata getMetadata(Integer id) {
		Session session = getSession();		
		Metadata m = (Metadata) session.get(Metadata.class, id);
		if(m != null) {
			Hibernate.initialize(m);
			Hibernate.initialize(m.getPathwayData());
		}
		
		return m;
	}
	
}
