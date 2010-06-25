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

import org.biopax.paxtools.model.level3.UtilityClass;
import org.biopax.paxtools.model.level3.Xref;


/**
 * cPathSquared Warehouse Interface
 * 
 * @author rodch
 */
public interface CPathWarehouse extends CvRepository {
	
	/**
	 * Gets the standard BioPAX utility class object from cPath Warehouse
	 * (e.g., CellVocabulary, SmallMoleculeReference, or ProteinReference).
	 * 
	 * @param <T> UtilityClass or its subclass (e.g., ProteinReference)
	 * @param urn
	 * @param clazz
	 * @return
	 */
	<T extends UtilityClass> T getObject(String urn, Class<T> utilityClazz);
	
	
	/**
	 * Finds the object (of BioPAX utility class) in cPath Warehouse,
	 * e.g., CellVocabulary, SmallMoleculeReference, or ProteinReference, 
	 * using the set of xrefs to search by.
	 * 
	 * @param <T> UtilityClass or its subclass
	 * @param xrefs query set
	 * @param clazz
	 * @return
	 */
	<T extends UtilityClass> T getObject(final Set<? extends Xref> xrefs, Class<T> utilityClazz);
	
	
	/**
	 * Gets the primary URN of the (BioPAX utility class) element by another identifier.
	 * This method's implementation will guess the identifier type and search in Warehouse.
	 * 
	 * @param id any identifier
	 * @param utilityClass 
	 * @return URI of the matching object in the Warehouse
	 */
	String getPrimaryURI(String id, Class<? extends UtilityClass> utilityClass);


	/**
	 * Create Lucene Index
	 */
	void createIndex();
		
}
