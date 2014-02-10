/**
 ** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either pathwayDataVersion 2.1 of the License, or
 ** any later pathwayDataVersion.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** both UofT and MSKCC have no obligations to provide maintenance, 
 ** support, updates, enhancements or modifications.  In no event shall
 ** UofT or MSKCC be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** UofT or MSKCC have been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this software; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA;
 ** or find it at http://www.fsf.org/ or http://www.gnu.org.
 **/

package cpath.importer.internal;

import cpath.dao.MetadataDAO;
import cpath.dao.PaxtoolsDAO;
import cpath.importer.Merger;
import cpath.warehouse.beans.*;

import org.biopax.paxtools.model.*;
import org.biopax.paxtools.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * This class is responsible for semantic Merging 
 * of the normalized original provider's pathway data 
 * into the main persistent Paxtools BioPAX model.
 */
public final class MergerImpl implements Merger {

    private static final Logger log = LoggerFactory.getLogger(MergerImpl.class);

	// where to merge pathway data
    private final PaxtoolsDAO mainDAO; //also implements Model interface
    
    // cpath2 repositories
	private final MetadataDAO metadataDAO;
    
    // configuration/flags
	private final String provider;
	private final boolean force;	
	private final String xmlBase;

	/**
	 * Constructor.
	 *
	 * This constructor was added to be used in a test context. At least called by
	 * cpath.importer.internal.CPathInMemoryModelMergerTest.testMerger().
	 * 
	 * @param dest final big Model (i.e., {@link PaxtoolsHibernateDAO})
	 * @param metadataDAO MetadataDAO
	 * @param provider merge pathway data from this provider only
	 * @param force whether to forcibly merge BioPAX data the validation reported critical about or skip.
	 * @throws AssertionError when dest is not instanceof {@link Model};
	 */
	public MergerImpl(final PaxtoolsDAO dest, final MetadataDAO metadataDAO, 
			String provider, boolean force) 
	{
		if(!(dest instanceof Model))
			throw new AssertionError(
			"The first parameter must be an instance of " +
			"org.biopax.paxtools.Model or cpath.dao.PaxtoolsDAO.");
	
		this.mainDAO = dest;
		this.metadataDAO = metadataDAO;
		this.xmlBase = ((Model)dest).getXmlBase();
		this.provider = provider;
		this.force = force;
	}
	
	
	/**
	 * {@inheritDoc}
	 * 
	 */
	public void merge() {
		
		//load the persistent model (initially - only warehouse) into mem.
		final Model model = MergerImpl.load(mainDAO);
		model.setXmlBase(xmlBase);
		
		// build models and merge from pathwayData.premergeData
		Collection<Metadata> providersMetadata = new ArrayList<Metadata>();
		
		if (provider != null)
			providersMetadata.add(metadataDAO.getMetadataByIdentifier(provider));
		else
			providersMetadata = metadataDAO.getAllMetadata();

		for (Metadata metadata : providersMetadata) {
			log.info("Start merging " + metadata);
			for (PathwayData pwdata : metadata.getPathwayData()) {
				
				final String description = pwdata.toString() + " - " + pwdata.status();
				log.info("Merging pathway data: " + description);

				if (pwdata.getValid() == null
						|| pwdata.getNormalizedData() == null
						|| pwdata.getNormalizedData().length == 0) {
					// must run pre-merge first!
					log.warn("Do '-premerge' first. Skipping "
							+ description);
					continue;
				} else if (pwdata.getValid() == false) {
					// has BioPAX errors
					log.warn("There were critical BioPAX errors in " + " - " + description);
					if (!force) {
						log.warn("Skipping " + description);
						continue;
					} else {
						log.warn("FORCE merging (ignoring all "
								+ "validation issues) for " + description);
					}
				}

				// import the BioPAX L3 pathway data into the in-memory paxtools model
				InputStream inputStream = new ByteArrayInputStream(
						pwdata.getNormalizedData());
				
				Model source = (new SimpleIOHandler(BioPAXLevel.L3))
						.convertFromOWL(inputStream);

				// merge/persist the new biopax; run within a transaction:
				Merge merge = new Merge(pwdata.toString(), 
						source, metadataDAO, xmlBase);				
				merge.execute(model);
			}
			
			log.info("Done merging " + metadata);
		}

		log.info("Merging from the in-memory into the persistent BioPAX DB...");
		// final merge (persist): insert new objects and update relationships
		mainDAO.merge(model); //this takes quite a while (hours)...
		
		log.info("Complete.");
	}


	public static Model load(PaxtoolsDAO mainDAO) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		mainDAO.exportModel(bos); //bos becomes closed after this
		InputStream inputStream = new ByteArrayInputStream(bos.toByteArray());
		bos = null;
		SimpleIOHandler reader = new SimpleIOHandler(BioPAXLevel.L3);
		return reader.convertFromOWL(inputStream);
	}
	
}