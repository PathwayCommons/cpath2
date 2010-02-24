package cpath.warehouse.metadata.internal;

// imports
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.metadata.MetadataDAO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
        log.info("Saving metadata object, CV: " + metadata.getCV());
        session.save(metadata);
	}

    /**
     * (non-Javadoc)
     * @see cpath.warehouse.metadata.MetadataDAO#getByCV
     */
    public Metadata getByCV(final String cv) {
		return (Metadata)getSession().get(Metadata.class, cv);
	}
}
