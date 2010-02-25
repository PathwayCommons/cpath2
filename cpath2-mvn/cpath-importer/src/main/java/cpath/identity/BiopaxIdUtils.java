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

package cpath.identity;

import uk.ac.ebi.miriam.lib.MiriamLink;

/**
 * This service object helps 
 * generate RDF IDs (for BioPAX UtilityClass elements),
 * TODO create standard external data URLs,
 * TODO find the primary ID, and
 * TODO map between different types of ID, etc.
 * 
 * @author rodch
 *
 */
public final class BiopaxIdUtils {
	
	private MiriamLink miriamLink;
	
	public BiopaxIdUtils(MiriamLink miriamLink) {
		this.miriamLink = miriamLink;
	}
		
	/**
	 * Looks up URN by (xref's) db and id.
	 * 
	 * @param db name or synonym of a (Miriam) data type
	 * @param id entity identifier within the data type
	 * @return
	 */
	public String getURI(String db, String id) {
		return miriamLink.getURI(db, id);
	}
	
	
	/**
	 * 
	 * @param name deprecated URI, name, or synonym of a data type
	 * @return 
	 */
	public String getDataTypeURN(String name) {
		return miriamLink.getOfficialDataTypeURI(name);
	}
	
	
	public String getPrimaryId() {
		// TODO add implementation
		return null;
	}
}
