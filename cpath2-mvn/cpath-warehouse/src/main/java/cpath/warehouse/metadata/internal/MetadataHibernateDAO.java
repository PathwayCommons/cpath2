package cpath.warehouse.metadata.internal;

// imports
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.metadata.MetadataDAO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implemenation of MetadatDAO interface.
 */
@Repository
public final class MetadataHibernateDAO  implements MetadataDAO {

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
			existing.setIsPSI(metadata.isPSI());
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
     * @see cpath.warehouse.metadata.MetadataDAO#getByID
     */
    public Metadata getByIdentifier(final String identifier) {

		Session session = getSession();
		Query query = session.getNamedQuery("cpath.warehouse.beans.providerByIdentifier");
		query.setParameter("identifier", identifier);
		return (Metadata)query.uniqueResult();
    }
}
