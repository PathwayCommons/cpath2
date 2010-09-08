/**
 ** Copyright (c) 2010 Memorial Sloan-Kettering Cancer Center (MSKCC)
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

import java.util.Map;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;


/**
 * CPath^2 Service is a mid-tier or adapter between DAOs and web controllers. 
 * 
 * TODO Can be used in a console application or integration tests (web container is not required!)
 * 
 * TODO This is to implement several query methods 
 * that accept valid parameters and beans, handle exceptions,
 * and return results packed in the HashMap with predefined keys.
 * 
 * TODO Enables "second query" use case -
 * when no data found in the pathway data (main) storage,
 * it will hit Warehouse to find general information (about 
 * CVs, small molecules and proteins)
 * 
 * @author rodche
 *
 */
public interface CPathService {

	/**
	 * Enumeration: map keys for the cPath^2 service results 
	 */
	public static enum ResultMapKey {
		MODEL, // key to BioPAX (PaxTools) Model, if any is returned
		ELEMENT, // a BioPAXElement (detached from DAO)
		DATA, // key to query results (to be treated by the caller), e.g., id-list, image, etc.
		ERROR, // key to error string or object (e.g., toString() will be used to get message)
		COUNT, // key to "records" count, e.g. items in the ID-list or no. of BioPAX elements
		MISC; // key to, e.g., lucene search statistics, etc.
	}
	
	/**
	 * Enumeration of cPath service output formats
	 * 
	 * @author rodche
	 *
	 */
	public static enum OutputFormat {
		XML, // cpath legacy search output format
		BIOPAX,
		BINARY_SIF,
		SBML,
		GSEA,
		PC_GENE_SET,
		ID_LIST,
		// TODO think: do we need "image" formats at all in the new services?
		IMAGE, 
		IMAGE_MAP,
		IMAGE_MAP_THUMBNAIL,
		IMAGE_MAP_IPHONE,
		IMAGE_MAP_FRAMESET,
		;
	}
	
	
	/**
	 * 
	 * 
	 * @see ResultMapKey
	 * 
	 * @param id
	 * @param format
	 * @return
	 */
	Map<ResultMapKey, Object> element(String id, OutputFormat format);

	
	/**
	 * Full-text search for the BioPAX elements. 
	 * Returns the map result that contains the list of identifiers,
	 * count, and supplementary information.
	 * 
	 * @see ResultMapKey
	 * 
	 * @param queryStr if null or empty, list/count all elements (of the class)
	 * @param biopaxClass
	 * @param countOnly
	 * @return
	 */
	Map<ResultMapKey, Object> list(String queryStr, Class<? extends BioPAXElement> biopaxClass, boolean countOnly);

	
	/**
	 * Serializes Paxtools Model to BioPAX OWL.
	 * 
	 * @param element
	 * @return
	 */
	String toOWL(Model model);
}
