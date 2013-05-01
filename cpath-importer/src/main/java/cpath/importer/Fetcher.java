/**
 ** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
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

package cpath.importer;

import java.io.IOException;
import java.util.Collection;

import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;

/**
 * @author rodche
 *
 */
public interface Fetcher {

    /**
     *  For the given url, returns a collection of Metadata Objects.
     *
     * @param url String
     * @return Collection<Metadata>
     * @throws IOException if an IO error occurs
     */
    Collection<Metadata> readMetadata(final String url) throws IOException;
    
       
    /**
     *  For the given Metadata, unpacks and reads the corresponding 
     *  data file(s), creating new {@link PathwayData} objects; adds 
     *  them to the metadata's pathwayData collection.
     *
	 * @param metadata Metadata
     * @throws IOException if an IO error occurs
     */
    void readPathwayData(final Metadata metadata) throws IOException;
    
    
    /**
     * Given Metadata, it downloads and stores the resources locally;
     * it does not replace existing files (name: metadata.getLocalDataFile()),
	 * which allows for manual correction and re-importing of previously fetched data
     * 
     * @param metadata
     * @throws IOException
     */
    void fetchData(final Metadata metadata) throws IOException;


    /**
     * Saves modified (in the premerge stage)
     * pathway data and validation results.
     * 
     * @param metadata
     */
	void savePathwayData(Metadata metadata);
    	
}
