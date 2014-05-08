package cpath.dao.internal;


import cpath.dao.CPathUtils;
import cpath.dao.MetadataDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Content;

import org.biopax.validator.api.beans.ValidatorResponse;
import org.biopax.validator.api.ValidatorUtils;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.FileInputStream;
import java.util.*;
import java.util.zip.GZIPInputStream;



/**
 * Implementation of MetadatDAO interface.
 * 
 * transactions are read-only for all public non-transient methods, 
 * unless a method has own @Transactional annotation 
 */
@Transactional
@Repository
class MetadataHibernateDAO  implements MetadataDAO {

    private static Logger log = LoggerFactory.getLogger(MetadataHibernateDAO.class);

    private SessionFactory sessionFactory;
    
    public void setSessionFactory(SessionFactory sessionFactory) {
    	this.sessionFactory = sessionFactory;
    }
	  
    public MetadataHibernateDAO() {    	
	} 
    
	public Metadata saveMetadata(Metadata metadata) {
		
		if(log.isInfoEnabled())
			log.info("saving metadata: " + metadata.getIdentifier());
    	
    	Session session = sessionFactory.getCurrentSession();
		
		if(metadata.getId() == null)
			session.persist(metadata);
		else
			metadata = (Metadata) session.merge(metadata);
		
		//initialize collections
		Hibernate.initialize(metadata.getName());
		Hibernate.initialize(metadata.getContent());
		
		return metadata;
    }

    
    public Metadata getMetadataByIdentifier(final String identifier) {
    	Assert.hasText(identifier);
    	
		Session session = sessionFactory.getCurrentSession();
		
		Metadata metadata = (Metadata) session.createCriteria(Metadata.class)
			.add(Restrictions.eq("identifier", identifier))
				//prevents duplicate (esp. when fetchType=EAGER child collections are there)
				.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY) 
					.uniqueResult();
		
		if(metadata != null) {
			//initialize collections
			Hibernate.initialize(metadata.getName());
			Hibernate.initialize(metadata.getContent());
		}
		
		return metadata;
    }

    
    @Transactional(readOnly=true)
    @SuppressWarnings("unchecked")
    public List<Metadata> getAllMetadata() {
		// safe to return as all collections are EAGER fetched
		List<Metadata> list = sessionFactory.getCurrentSession()
			.createCriteria(Metadata.class)
				//prevents duplicate (esp. when fetchType=EAGER child collections are there)
				.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY) 
					.list();
		log.debug("no. metadata records: " + list.size());
		
		//initialize collections
		for(Metadata md : list) {
			Hibernate.initialize(md.getName());
			Hibernate.initialize(md.getContent());
		}
		
		return list;
	}

	
    @Transactional(readOnly=true)
	public ValidatorResponse validationReport(String provider, String file) {
		ValidatorResponse response = new ValidatorResponse();
		Metadata metadata = getMetadataByIdentifier(provider);
		for (Content content : metadata.getContent()) {
			String current = content.getFilename();			
			
			if(file != null && !file.equals(current))
				continue;
			
			//file==null means all files			
			
			try {
				// unmarshal and add
				ValidatorResponse resp = (ValidatorResponse) ValidatorUtils.getUnmarshaller()
					.unmarshal(new GZIPInputStream(new FileInputStream(content.validationXmlFile())));
				assert resp.getValidationResult().size() == 1;				
				response.getValidationResult().addAll(resp.getValidationResult());				
			} catch (Exception e) {
				log.error("validationReport: failed converting the XML response to objects", e);
			}
			
			if(current.equals(file))
				break;
		}

		return response;
	}
    

	public void addOrUpdateMetadata(String location) {
    	for (Metadata mdata : CPathUtils.readMetadata(location))
      		saveMetadata(mdata);
 	}

    
	public Metadata init(Metadata metadata) {     	
    	metadata = getMetadataByIdentifier(metadata.getIdentifier());
    	
    	metadata.cleanupOutputDir();
    	metadata.setNumInteractions(null);
    	metadata.setNumPathways(null);
    	metadata.setNumPhysicalEntities(null);   	
    	metadata.getContent().clear(); 
    	
		return saveMetadata(metadata);
	}

    
	public void deleteMetadata(Metadata metadata) {
    	sessionFactory.getCurrentSession().delete(metadata);
    	metadata.cleanupOutputDir();
	}

}
