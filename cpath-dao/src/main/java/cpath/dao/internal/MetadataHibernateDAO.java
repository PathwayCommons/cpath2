package cpath.dao.internal;


import cpath.dao.CPathUtils;
import cpath.dao.MetadataDAO;
import cpath.warehouse.beans.Mapping;
import cpath.warehouse.beans.Mapping.Type;
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
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.*;


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
	  
    
    @Override
    @Transactional
	public void saveMetadata(final Metadata metadata) {
		Session session = sessionFactory.getCurrentSession();
		session.merge(metadata);
		session.flush();
		session.clear();		
		if(log.isInfoEnabled())
			log.info("metadata object " + metadata.getIdentifier() +
				" has been sucessessfully saved or merged.");
    }


    @Override
    @Transactional
    public Metadata getMetadataByIdentifier(final String identifier) {
		Session session = sessionFactory.getCurrentSession();
		Metadata m = (Metadata) session.createCriteria(Metadata.class)
			.add(Restrictions.eq("identifier", identifier))
			.addOrder(Order.asc("id"))
				.uniqueResult();
		if(m != null) {
			Hibernate.initialize(m.getPathwayData());
//			Hibernate.initialize(m.getName()); // not needed - name is EAGER collection
		}	
		return m;
    }

    
    @SuppressWarnings("unchecked")
	@Transactional
    @Override
    public Collection<Metadata> getAllMetadata() {
		Session session = sessionFactory.getCurrentSession();
		List<Metadata> toReturn = session.createCriteria(Metadata.class)
			.addOrder(Order.asc("id")).list();
		if (toReturn.isEmpty()) 
			return Collections.EMPTY_SET;
		else {
			for(Metadata m : toReturn) {
				Hibernate.initialize(m.getPathwayData());
//				Hibernate.initialize(m.getName()); //no need (name is EAGER collection)
			}
			return toReturn;
		}
	}
   
    
	@Override
	@Transactional
	public PathwayData getPathwayData(Integer pathwayId) {
		Session ses = sessionFactory.getCurrentSession();
		PathwayData pd = (PathwayData) ses.get(PathwayData.class, pathwayId);
		return pd;
	}	
	
	
	@Override
	@Transactional
	public PathwayData getPathwayData(final String provider, final String filename) {		
		Session session = sessionFactory.getCurrentSession();
		
		PathwayData pd = (PathwayData) session.createCriteria(PathwayData.class)
			.add(Restrictions.eq("metadata.identifier", provider))
			.add(Restrictions.eq("filename", filename)).uniqueResult();
		
		return pd;
	}
	
	
	@Override
	@Transactional(readOnly=true)
	public ValidatorResponse validationReport(String provider, Integer pathwayDataPk) {
		ValidatorResponse response = null;

		if (provider != null && pathwayDataPk == null) {
			// get validationResults from PathwayData beans
			Session session = sessionFactory.getCurrentSession();
			List<PathwayData> pathwayDataCollection = (List<PathwayData>) session
				.createCriteria(PathwayData.class)
				.add(Restrictions.eq("metadata.identifier", provider))
				.addOrder(Order.asc("pathway_id"))
				.list();					
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
		} 
		else {
			PathwayData pathwayData = getPathwayData(pathwayDataPk);
			if (pathwayData != null) {
				try {
					byte[] xmlResult = pathwayData.getValidationResults();
					response = (ValidatorResponse) ValidatorUtils.getUnmarshaller()
						.unmarshal(new BufferedInputStream(new ByteArrayInputStream(xmlResult)));
				} catch (Throwable e) {
					log.error(e);
				}
			}
		}

		return response;
	}


	@Transactional
	public void deleteAllIdMappings() {
		log.debug("deleteAllIdMappings: purge all...");
		Session ses = sessionFactory.getCurrentSession();
		ses.createQuery("delete from mapping").executeUpdate();
		ses.createQuery("delete from mapping_map").executeUpdate();
		ses.flush(); //might not required due to the new transaction, but.. let it be
	}
	

	/**
	 * {@inheritDoc}
	 * 
	 * Returns the initialized 
	 * (i.e, with its lazy collection usable outside active sessions) 
	 * Metadata object by PK.
	 */
	@Transactional
	@Override
	public Metadata getMetadata(Integer id) {
		Session session = sessionFactory.getCurrentSession();		
		Metadata m = (Metadata) session.get(Metadata.class, id);
		if(m != null) {
//			Hibernate.initialize(m); //not needed
			Hibernate.initialize(m.getPathwayData());
//			Hibernate.initialize(m.getName()); // no need: name is EAGER
		}
		
		return m;
	}
	
	
	@Transactional
	@Override
	public void deleteMetadata(Integer id) {
		Session session = sessionFactory.getCurrentSession();		
		Metadata m = (Metadata) session.get(Metadata.class, id);
		if(m != null) {
			session.delete(m);
			session.flush();
		}
	}
	
	
	@Transactional
	@Override
	public void deletePathwayData(Integer id) {
		Session session = sessionFactory.getCurrentSession();		
		PathwayData m = (PathwayData) session.get(PathwayData.class, id);
		if(m != null) {
			session.delete(m);
			session.flush();
		}
	}
	
	
	/**
     * This method uses knowledge about the id-mapping
     * internal design (id -> to primary id table, no db names used) 
     * and about some of bio identifiers to increase the possibility
     * of successful (not null) mapping result.
     * 
     * Notes.
     * 
     * Might in the future, if we store all mappings in the same table, 
     * and therefore have to deal with several types of numerical identifiers,
     * which requires db name to be part of the primary key, this method can 
     * shield from the implementation details of making the key (i.e, we might 
     * want to use pubchem:123456, SABIO-RK:12345 as pk, despite the prefixes
     * are normally not part of the identifier).
     * 
     * @param db
     * @param id
     * @return suggested id to be used in a id-mapping call
     */
     static String suggest(String db, String id) {
    	
    	if(db == null || db.isEmpty() || id == null || id.isEmpty())
    		return id;
    	
		// our warehouse-specific hack for matching uniprot.isoform, kegg, refseq xrefs,
		// e.g., ".../uniprot.isoform/P04150-2" becomes  ".../uniprot/P04150"
		if(db.equalsIgnoreCase("uniprot isoform") || db.equalsIgnoreCase("uniprot.isoform")) {
			int idx = id.lastIndexOf('-');
			if(idx > 0)
				id = id.substring(0, idx);
//			db = "UniProt";
		}
		else if(db.equalsIgnoreCase("refseq")) {
			//also want refseq:NP_012345.2 to become refseq:NP_012345
			int idx = id.lastIndexOf('.');
			if(idx > 0)
				id = id.substring(0, idx);
		} 
		else if(db.toLowerCase().startsWith("kegg") && id.matches(":\\d+$")) {
			int idx = id.lastIndexOf(':');
			if(idx > 0)
				id = id.substring(idx + 1);
//			db = "NCBI Gene";
		}
    		
    	return id;
    }

     
    @Transactional
	@Override
	public void saveMapping(Mapping mapping) {
    	Session session = sessionFactory.getCurrentSession();
    	session.save(mapping);
    	session.flush();
	}

    
    @Transactional
	@Override
	public List<Mapping> getMappings(Type type) {
		Session session = sessionFactory.getCurrentSession();
		Query query = session.getNamedQuery("cpath.warehouse.beans.mappingsByType");
		query.setParameter("type", type);
		return (List<Mapping>) query.list();
	}

    
    @Transactional(readOnly=true)
	@Override
	public Set<String> mapIdentifier(String identifier, Type type, String idType) {
    	//if possible, suggest a canonical id, i.e, 
    	// more chances it would map to uniprot or chebi primary accession.
    	final String id = suggest(idType, identifier);
    	
    	//(lazy) load mapping tables
    	List<Mapping> maps = getMappings(type);
    	
    	Set<String> results = new TreeSet<String>();
    	
    	for(Mapping m : maps) {
    		String ac = m.getMap().get(id);
    		if(ac != null && !ac.isEmpty())
    			results.add(ac);
    	}
    	
		return results;
	}
    

	@Transactional
    @Override
	public void importMetadata(String location) {
     // process metadata
     for (Metadata mdata : CPathUtils.readMetadata(location)) {
     	Metadata m = getMetadataByIdentifier(mdata.getIdentifier());
     	if(m != null) {
     		log.info("readMetadata: updating metadata: " 
     			+ m.getIdentifier() + " from " + location);
     		m.setDescription(mdata.getDescription());
     		m.setName(mdata.getName());
     		m.setIcon(mdata.getIcon());
     		m.setType(mdata.getType());
     		m.setUrlToData(mdata.getUrlToData());
     		m.setUrlToHomepage(mdata.getUrlToHomepage());
     		m.setConverterClassname(mdata.getConverterClassname());
     		m.setCleanerClassname(mdata.getCleanerClassname());
     		//m.setNumInteractions, etc.. - won't modify; one should run -update-counts
     		//m.setPathwayData - won't touch either; one should run -premerge
     		saveMetadata(m);
     	}  else {
     		saveMetadata(mdata);
     	}
     }			
	}

}
