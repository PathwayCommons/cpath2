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
package cpath.dao;

import org.biopax.paxtools.io.sif.InteractionRule;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;

import cpath.warehouse.WarehouseDAO;

import java.util.Collection;
import java.util.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;

/**
 * BioPAX data access (both model and repository).
 */
public interface PaxtoolsDAO extends Model {
	
	/**
	 * Builds lucene index.
	 * 
	 */
	void createIndex();
	
	
	/**
	 * Persists the given model to the db.
	 *
	 * @param biopaxFile File
	 * @param createIndex boolean
	 * @throws FileNoteFoundException
	 */
	void importModel(File biopaxFile) throws FileNotFoundException;


	 /**
	 * Searches the lucene index and returns the set of objects 
	 * of the given class in the model that match the query string.
	 * 
	 * This method is more useful when run within a transaction/session,
	 * because the returned list is not guaranteed to be fully initialized
	 * (it may contain elements with incomplete (lazy) property values)
	 * 
     * @param query String
	 * @param filterBy class to be used as a filter.
     * @return ordered by relevance list of elements
     * 
     * @deprecated use {@link #find(String, Class)} and {@link #getValidSubModel(Collection)}, or {@link WarehouseDAO#getObject(String)}
     */
    <T extends BioPAXElement> List<T> search(String query, Class<T> filterBy);

    
	 /**
	 * Returns the set of IDs 
	 * of the BioPAX elements of given class 
	 * that match the query.
	 * 
     * @param query String
	 * @param filterBy class to be used as a filter.
     * @return ordered by the element's relevance list of rdfIds
     */
    List<String> find(String query, Class<? extends BioPAXElement> filterBy);

    
	 /**
	 * Returns the count 
	 * of the BioPAX elements of given class
	 * that match the full-text query string.
	 * 
     * @param query String
	 * @param filterBy class to be used as a filter.
     * @return count
     */
    Integer count(String query, Class<? extends BioPAXElement> filterBy);
    
    
    
    /**
     * Writes the complete model as BioPAX (OWL)
     * 
     * @param outputStream
     * @param ids (optional) build a sub-model from these IDs and export it
     */
    void exportModel(OutputStream outputStream, String... ids);   
    
    
    /**
     * Creates a "valid" sub-model from the BioPAX elements
     * (using paxtools's "auto-complete" and "clone" procedures)
     * 
     * @param ids a set of valid RDFId
     * @return
     */
    Model getValidSubModel(Collection<String> ids);
    
    
    /**
     * 
     * @param outputStream
     * @param rules
     * @param ids (optional) the list of BioPAX elements (IDs) to export
     */
    void exportBinaryInteractions(OutputStream outputStream, Collection<InteractionRule> rules, String... ids);
 
    
    /**
     * Gets initialized BioPAX element, i.e., 
     * with all the properties set for sure
     * (this matters when an implementation uses 
     * caching, transactions, etc.)
     * 
     * @param id
     * @return
     */
    BioPAXElement getByIdInitialized(String id);
    
}