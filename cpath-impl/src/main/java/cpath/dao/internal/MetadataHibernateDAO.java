package cpath.dao.internal;


import cpath.dao.CPathUtils;
import cpath.dao.MetadataDAO;
import cpath.warehouse.beans.Mapping;
import cpath.warehouse.beans.Mapping.Type;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;

import org.biopax.validator.api.beans.ValidatorResponse;
import org.biopax.validator.api.ValidatorUtils;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.*;


/**
 * Implementation of MetadatDAO interface.
 * 
 * transactions are read-only for all public non-transient methods, 
 * unless a method has own @Transactional annotation 
 */
@Transactional(readOnly=true) 
@Repository
class MetadataHibernateDAO  implements MetadataDAO {

    private static Logger log = LoggerFactory.getLogger(MetadataHibernateDAO.class);

    private SessionFactory sessionFactory;
    
    public void setSessionFactory(SessionFactory sessionFactory) {
    	this.sessionFactory = sessionFactory;
    }
	  
    
    @Override
    @Transactional
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
		Hibernate.initialize(metadata.getPathwayData());
		
		return metadata;
    }


    @Override
    public Metadata getMetadataByIdentifier(final String identifier) {
		Session session = sessionFactory.getCurrentSession();
		Metadata metadata = (Metadata) session.createCriteria(Metadata.class)
			.add(Restrictions.eq("identifier", identifier))
				//prevents duplicate (esp. when fetchType=EAGER child collections are there)
				.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY) 
					.uniqueResult();
		
		//initialize collections
		Hibernate.initialize(metadata.getName());
		Hibernate.initialize(metadata.getPathwayData());
		
		return metadata;
    }

    
    @SuppressWarnings("unchecked")
    @Override
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
			Hibernate.initialize(md.getPathwayData());
		}
		
		return list;
	}

	
	@Override
	public ValidatorResponse validationReport(String provider, String file) {
		ValidatorResponse response = new ValidatorResponse();
		Metadata metadata = getMetadataByIdentifier(provider);
		for (PathwayData pathwayData : metadata.getPathwayData()) {
			String current = pathwayData.getFilename();			
			
			if(file != null && !file.equals(current))
				continue;
			
			try {
				byte[] xmlResult = pathwayData.getValidationReport();
				// unmarshal and add
				ValidatorResponse resp = (ValidatorResponse) ValidatorUtils.getUnmarshaller()
					.unmarshal(new BufferedInputStream(new ByteArrayInputStream(xmlResult)));
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


	@Transactional
	public void deleteAllIdMappings() {
		log.debug("deleteAllIdMappings: purge all...");
		Session ses = sessionFactory.getCurrentSession();
		ses.createQuery("delete Mapping").executeUpdate();
		ses.createSQLQuery("delete from mapping_map").executeUpdate();
		ses.flush(); //might not required due to the new transaction, but.. let it be
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

    
	@Override
	public List<Mapping> getMappings(Type type) {
		Session session = sessionFactory.getCurrentSession();
		Query query = session.getNamedQuery("cpath.warehouse.beans.mappingsByType");
		query.setParameter("type", type);
		return (List<Mapping>) query.list();
	}

    
	@Override
	@Cacheable(value = "idmappingCache")
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
	public void addOrUpdateMetadata(String location) {
    	for (Metadata mdata : CPathUtils.readMetadata(location))
      		saveMetadata(mdata);
 	}

    
    @Transactional //not read-only
	@Override
	public Metadata init(Metadata metadata) {    	
    	metadata.cleanupOutputDir();
    	metadata.setNumInteractions(null);
    	metadata.setNumPathways(null);
    	metadata.setNumPhysicalEntities(null);
    	metadata.getPathwayData().clear(); 
		metadata = saveMetadata(metadata);
		
		//initialize collections
		Hibernate.initialize(metadata.getName());
		Hibernate.initialize(metadata.getPathwayData());
		
		return metadata;
	}
}
