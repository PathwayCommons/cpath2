package cpath.warehouse.internal;

// imports
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.Metadata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

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
    @Transactional(propagation=Propagation.NESTED)
	public void importMetadata(final Metadata metadata) {

		Session session = getSession();
		// check for existing object
		Metadata existing = getByIdentifier(metadata.getIdentifier());
		if (existing != null) {
			log.info("Metadata object with identifier: " + metadata.getIdentifier() + " already exists, manually merging.");
			existing.setVersion(metadata.getVersion());
			existing.setReleaseDate(metadata.getReleaseDate());
			existing.setURLToPathwayData(metadata.getURLToPathwayData());
			existing.setIcon(metadata.getIcon());
			existing.setType(metadata.getType());
			session.update(existing);
		}
		else {
			log.info("Metadata object with identifier: " + metadata.getIdentifier() + " does not exist, saving.");
			session.save(metadata);
		}
		log.info("metadata object has been sucessessfully saved or merged.");
    }

    /**
     * (non-Javadoc)
     * @see cpath.warehouse.MetadataDAO#getByID
     */
    @Transactional(propagation=Propagation.NESTED)
    public Metadata getByIdentifier(final String identifier) {
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
    @Transactional(propagation=Propagation.NESTED)
    public Collection<Metadata> getAll() {
		Session session = getSession();
		Query query = session.getNamedQuery("cpath.warehouse.beans.allProvider");
		List toReturn = query.list();
		return (toReturn.size() > 0) ? new HashSet(toReturn) : new HashSet();
	}
    
    
}
