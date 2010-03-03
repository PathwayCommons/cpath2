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

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.UtilityClass;

import java.util.Set;


/**
 * cPathSquared Warehouse Interface
 * 
 * @author rodch
 *
 * TODO define warehouse methods
 */
public interface CPathWarehouse {

	static final String SEARCH_INDEX_NAME = "cpathwarehouse";
	
	/**
	 * Creates a new standard reference BioPAX object 
	 * (e.g., CellVocabulary or ProteinReference)
	 * 
	 * @param <T> UtilityClass or its subclass (e.g., ProteinReference)
	 * @param urn
	 * @param clazz
	 * @return
	 * 
	 * TODO maybe, remove this one
	 */
	<T extends UtilityClass> T createUtilityClass(String primaryUrn, Class<T> utilityClazz);
	
	/**
	 * Creates a new standard reference BioPAX object 
	 * (e.g., CellVocabulary or ProteinReference),
	 * auto-resolving URN to the proper UtilityClass
	 * 
	 * @param <T> UtilityClass or its subclass (e.g., ProteinReference)
	 * @param urn
	 * @return
	 */
	<T extends UtilityClass> T createUtilityClass(String primaryUrn);

	
	/**
	 * Creates a new standard BioPAX element within the given context.
	 * 
	 * @param <T> EntityReference or subclass
	 * @param primaryUrn
	 * @param EntityReference
	 * @return
	 */
	<T extends UtilityClass> T createUtilityClass(String primaryUrn, Class<? extends BioPAXElement> domain, String property);

	
	/**
	 * Gets the primary URN of the (BioPAX utility class) element by ID.
	 * 
	 * @param id
	 * @return
	 */
	String getPrimaryURI(String id);
	
	
	/**
	 * Gets primary IDs (URNs) of all the controlled vocabularies that
	 * are either indirect or direct children of the one identified by its URN.
	 * 
	 * @param urn id of a ControlledVocabulary
	 * @return set of ControlledVocabulary URNs
	 */
	Set<String> getAllChildrenOfCv(String urn);
	Set<String> getDirectChildrenOfCv(String urn);
	Set<String> getParentsOfCv(String urn);
}
