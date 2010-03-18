package cpath.premerge;

// imports
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.metadata.MetadataDAO;

import org.springframework.context.ApplicationContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;

/**
 * Provides Premerge dispatch services.
 */
public final class PremergeDispatcher implements Runnable {

    // log
    private static Log log = LogFactory.getLog(PremergeDispatcher.class);

    // ref to metadata dao 
	private MetadataDAO metadataDAO;

	// ref to application context
	private ApplicationContext applicationContext;

	// this allows spring to inject it
	public void setApplicationContext(ApplicationContext applicationContext){
		this.applicationContext = applicationContext;
	}

	/**
     * Constructor.
     * 
     * @param metadataDAO MetadataDAO
     */
	public PremergeDispatcher(final MetadataDAO metadataDAO) {

		// init members
		this.metadataDAO = metadataDAO;
	}

    @Override
    public void run() {

		// grab all metadata
		Collection<Metadata> metadataCollection = metadataDAO.getAll();

		// iterate over all metadata
		for (Metadata metadata : metadataCollection) {
			log.info("run(), spawning premerge for provider " + metadata.getIdentifier());
			Premerge premerge = (Premerge)applicationContext.getBean("premerge");
			premerge.setMetadata(metadata);
			premerge.start();
		}
	}
}
