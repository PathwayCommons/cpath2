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

package cpath.warehouse;

import java.util.Collection;
import java.util.Set;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.*;


/**
 * cPathSquared Warehouse Interface
 * 
 * @author rodch
 */
public interface WarehouseDAO {

	/**
	 * Gets fully initialized (and detached form the DAO) BioPAX object.
	 * 
	 * @param urn
	 * @return
	 */
	BioPAXElement getObject(String urn);
	
	
	/**
	 * Gets fully initialized (and detached form the DAO) BioPAX object.
	 * 
	 * @param <T> UtilityClass or its subclass (e.g., ProteinReference)
	 * @param urn
	 * @param clazz
	 * @return
	 */
	<T extends BioPAXElement> T getObject(String urn, Class<T> clazz);
	
	
	/**
	 * Gets fully initialized (and detached form the DAO) BioPAX object 
	 * using the set of xrefs to search by.
	 * 
	 * @param <T> UtilityClass or its subclass
	 * @param xrefs query set
	 * @param clazz XReferable
	 * @return
	 */
	<T extends XReferrable> Collection<T> getObjects(Set<? extends Xref> xrefs, Class<T> clazz);
	
}
