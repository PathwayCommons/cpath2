// $Id$
//------------------------------------------------------------------------------
/** Copyright (c) 2010 Memorial Sloan-Kettering Cancer Center.
 **
 ** This library is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** Memorial Sloan-Kettering Cancer Center
 ** has no obligations to provide maintenance, support,
 ** updates, enhancements or modifications.  In no event shall
 ** Memorial Sloan-Kettering Cancer Center
 ** be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** Memorial Sloan-Kettering Cancer Center
 ** has been advised of the possibility of such damage.  See
 ** the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this library; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 **/
package cpath.fetcher.internal;

// imports
import cpath.fetcher.CPathFetcher;

import cpath.warehouse.beans.Cv;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;

import cpath.fetcher.cv.CvFetcher;
import cpath.fetcher.metadata.ProviderMetadataService;
import cpath.fetcher.pathway.ProviderPathwayDataService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.validator.impl.CvTermsRule;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of CPathFetcher interface.
 */
public final class CPathFetcherImpl implements CPathFetcher {

    private static Log log = LogFactory.getLog(CPathFetcherImpl.class);
    
    private ProviderMetadataService providerMetadataService;
    private ProviderPathwayDataService providerPathwayDataService;
    private CvFetcher cvFetcher;

    /**
     * Constructor
     * 
     * @param providerMetadataService
     * @param providerPathwayDataService
     * @param cvFetcher
     */
	public CPathFetcherImpl(ProviderMetadataService providerMetadataService,
							ProviderPathwayDataService providerPathwayDataService,
							CvFetcher cvFetcher) {
		this.providerMetadataService = providerMetadataService;
		this.providerPathwayDataService = providerPathwayDataService;
		this.cvFetcher = cvFetcher;
    }
    
    /*
     * (non-Javadoc)
     * @see cpath.fetcher.CPathFetcher#getProviderMetadata(java.lang.String)
     */
    @Override
    public Collection<Metadata> getProviderMetadata(final String url) throws IOException {

		log.info("CPathFetcherImpl.getProviderMetadata(), redirecting to ProviderMetadata.getProviderMetadata()");
        return providerMetadataService.getProviderMetadata(url);
    }

    /*
     * (non-Javadoc)
     * @see cpath.fetcher.CPathFetcher#getProviderPathwayData(cpath.warehouse.beans.Metadata)
     */
    @Override
    public Collection<PathwayData> getProviderPathwayData(final Metadata metadata) throws IOException {

		log.info("CPathFetcherImpl.getProviderPathwayData(), redirecting to ProviderPathwayData.getProviderPathwayData()");
        return providerPathwayDataService.getProviderPathwayData(metadata);
    }

	/* (non-Javadoc)
	 * @see cpath.fetcher.CPathFetcher#fetchBiopaxCVs()
	 */
	@Override
	public Set<Cv> fetchBiopaxCVs() {
		Set<Cv> allCv = new HashSet<Cv>();
		
		for(CvTermsRule cvRule : cvFetcher.getCvRules()) {
			allCv.addAll(cvFetcher.fetchBiopaxCVs(cvRule));
		}
		
		return allCv;
	}
}
