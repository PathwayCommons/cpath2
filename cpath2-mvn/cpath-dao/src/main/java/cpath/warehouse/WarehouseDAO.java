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

import java.util.Set;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;


/**
 * cPathSquared Warehouse Interface
 * 
 * @author rodch
 */
public interface WarehouseDAO {
	
	
	/**
	 * Gets a fully initialized (and detached) BioPAX object.
	 * 
	 * @param <T> BioPAXElement or its subclass (e.g., ProteinReference)
	 * @param uri
	 * @param clazz
	 * @return
	 */
	<T extends BioPAXElement> T createBiopaxObject(String uri, Class<T> clazz);
	
	
    /**
     * Gets a fully initialized (and detached) BioPAX sub-model 
     * that contains the BioPAX object and its children.
     * 
     * @param <T> BioPAXElement or its subclass (e.g., ProteinReference)
     * @param uri
     * @param clazz
     * 
     * @return model that contains the object and its dependents or null.
     */
	<T extends BioPAXElement> Model createSubModel(String uri, Class<T> clazz);
    
	
	/**
	 * Gets identifier(s) of the BioPAX object(s) stored in the warehouse 
	 * by using the xrefs as a query.
	 * 
	 * In fact, this method does sort of "id-mapping", which is based on the
	 * warehouse data though, i.e., on how and which Xrefs were generated for each
	 * protein or small molecule entry during the warehous was built, which can be 
	 * improved from one release to another (an may be still no perfect). And, 
	 * one can always create better BioPAX data alignment/merge algorithm on top of it.
	 * 
	 * @param xrefs query set of xrefs
	 * @param clazz subclass (of XReferable) of the requested object
	 * @return
	 */
	Set<String> findByXref(Set<? extends Xref> xrefs, Class<? extends XReferrable> clazz);
	
}
