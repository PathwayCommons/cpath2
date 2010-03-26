package cpath.premerge;

// imports
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.metadata.MetadataDAO;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;

/**
 * Provides Premerge dispatch services.
 */
public final class PremergeDispatcher extends Thread implements ApplicationContextAware {

    // log
    private static Log log = LogFactory.getLog(PremergeDispatcher.class);

    // ref to metadata dao 
	private MetadataDAO metadataDAO;

	// used for synchronization
	private final Object synObj;

	// number of premerges
	private int numPremerges;

	// number of premerges complete
	private int premergesComplete;

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
		this.synObj = new Object();
	}

	/**
	 * Used by Premerge instances to notify of completion.
	 *
	 * @param metadata Metadata
	 */
	public void premergeComplete(final Metadata metadata) {

		synchronized (synObj) {
			++premergesComplete;
			log.info("premergeComplete(), Premerge complete for provider " + metadata.getIdentifier() + ".");
		}
	}

	/**
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
    @Override
    public void run() {

		// grab all metadata
		Collection<Metadata> metadataCollection = metadataDAO.getAll();

		// set number of premerges to dispatch
		for (Metadata metadata : metadataCollection) {
			if (metadata.getType().equals(Metadata.TYPE.PSI_MI) ||
				metadata.getType().equals(Metadata.TYPE.BIOPAX)) {
				++numPremerges;
			}
		}
		log.info("run(), Spawning " + numPremerges + " Premerge instances.");

		// iterate over all metadata
		for (Metadata metadata : metadataCollection) {

			// only process interaction or pathway data
			if (metadata.getType().equals(Metadata.TYPE.PSI_MI) ||
				metadata.getType().equals(Metadata.TYPE.BIOPAX)) {
				log.info("run(), spawning Premerge for provider " + metadata.getIdentifier());
				Premerge premerge = (Premerge)applicationContext.getBean("premerge");
				premerge.setDispatcher(this);
				premerge.setMetadata(metadata);
				premerge.premerge();
			}
		}

		// wait for premerges to complete
		while (true) {

			synchronized(synObj) {
				if (premergesComplete == numPremerges) {
					log.info("run(), All Premerge(s) have completed.");
					break;
				}
			}

			// sleep for a bit
			try {
				sleep(100);
			}
			catch (InterruptedException e){
				e.printStackTrace();
				break;
			}
		}

		log.info("run(), exiting...");
	}
}
