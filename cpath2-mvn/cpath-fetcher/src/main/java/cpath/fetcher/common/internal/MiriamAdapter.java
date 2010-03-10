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

package cpath.fetcher.common.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.miriam.lib.MiriamLink;

/**
 * This helps generate proper RDF IDs.
 * 
 * @author rodch
 *
 */
public final class MiriamAdapter extends MiriamLink {
	private final static Log log = LogFactory.getLog(MiriamAdapter.class);
	
	public MiriamAdapter() {
		if(!isLibraryUpdated() && log.isInfoEnabled()) {
			log.info("There is a new version of the MiriamLink available!");
		}
	}

	
	
	/**
	 * Looks up URN by (xref's) db and id.
	 * 
	 * @param db name or synonym of a (Miriam) data type
	 * @param id entity identifier within the data type
	 * @return
	 */
	@Override
	public String getURI(String db, String id) {
		String urn = null;
		
		try{
			if(checkRegExp(id, db)) {
				urn = super.getURI(db, id);
			} else {
				log.fatal("Invalid Id : " +
					id + " for " + db + "; pattern=" 
					+ getDataTypePattern(db));
			}
		} catch (Exception e) {
			log.fatal("Cannot get URN by : " +
				db + " and " + id , e);
		}
		
		return (urn==null || "".equals(urn)) ? null : urn; 
	}
	
	
	/**
	 * 
	 * @param name deprecated URI, name, or synonym of a data type
	 * @return 
	 */
	public String getDataTypeURN(String name) {
		String urn = null;
		
		try{
			urn = super.getDataTypeURI(name);
			if(urn == null || "".equals(urn)) {
				urn = super.getOfficialDataTypeURI(name);
			}
		} catch (Exception e) {
			log.error("Cannot get URN by : " +
				name , e);
		}
		
		return (urn==null || "".equals(urn)) ? null : urn; 
	}
	
	
	
}
