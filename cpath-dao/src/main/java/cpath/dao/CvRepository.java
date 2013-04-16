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

package cpath.dao;

import java.util.Set;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.ControlledVocabulary;

/**
 * 
 * @author rodche
 *
 */
public interface CvRepository {
	
	/**
	 * Gets a CV
	 * 
	 * @param <T>
	 * @param uri e.g., urn:miriam:obo.go:GO%3A0005654 or http://identifiers.org/obo.go/GO:0005654
	 * @param cvClass
	 * @return
	 */
	<T extends ControlledVocabulary> T getControlledVocabulary(String uri, Class<T> cvClass);
	
	/**
	 * Gets a CV by ontology name and term accession
	 * 
	 * @param <T>
	 * @param db OBO ontology name, synonym, or URI (e.g., "Gene Ontology", "go", "urn:miriam:obo.go", or "http://identifiers.org/obo.go/")
	 * @param id term accession number (identifier)
	 * @param cvClass
	 * @return
	 */
	<T extends ControlledVocabulary> T getControlledVocabulary(String db, String id, Class<T> cvClass);

	
	/*
	 * CVs Hierarchy Access
	 */
	
	Set<String> getDirectChildren(String urn);
	
	Set<String> getDirectParents(String urn);
	
	Set<String> getAllChildren(String urn);
	
	Set<String> getAllParents(String urn);
	
	boolean isChild(String parentUrn, String urn);
	
}
