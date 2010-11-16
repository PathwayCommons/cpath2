package cpath.importer.internal;

// imports
import cpath.importer.Premerge;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.Metadata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.validator.Validator;

import java.util.Collection;

/**
 * Provides Premerge dispatch services.
 */
public class PremergeDispatcher extends Thread {

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

	private Validator validator;
	
	
	/**
     * Constructor.
     * 
     * @param metadataDAO MetadataDAO
     */
	public PremergeDispatcher(final MetadataDAO metadataDAO, 
			final Validator validator) 
	{
		// init members
		this.metadataDAO = metadataDAO;
		this.synObj = new Object();
		this.validator = validator;
	}

	/**
	 * Used by Premerge instances to notify of completion.
	 *
	 * @param metadata Metadata
	 */
	public void premergeComplete(final Metadata metadata) {
		synchronized (synObj) {
			++premergesComplete;
			if(log.isInfoEnabled())
				log.info("premergeComplete(), Premerge complete for provider "
					+ metadata.getIdentifier() + ".");
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

		// iterate over all metadata
		for (Metadata metadata : metadataCollection) {
			// only process interaction or pathway data
			if (metadata.getType() == Metadata.TYPE.PSI_MI ||
				metadata.getType() == Metadata.TYPE.BIOPAX) 
			{
				++numPremerges;
				if(log.isInfoEnabled())
					log.info("run(), spawning Premerge for provider " 
						+ metadata.getIdentifier());

				// a new premerge runs in a new thread
				Premerge premerge = new PremergeImpl(metadataDAO, validator);
				premerge.setDispatcher(this);
				premerge.setMetadata(metadata);
				premerge.premerge();
			}
		}

		// wait for premerges to complete
		while (true) {

			synchronized(synObj) {
				if (premergesComplete == numPremerges) {
					if(log.isInfoEnabled())
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

		if(log.isInfoEnabled())
			log.info("run(), exiting...");
	}
}
