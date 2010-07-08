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

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;

import java.util.List;
import java.util.Set;
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
     * This method returns the biopax element with the given id,
     * returns null if the object with the given id does not exist
     * in this model.
	 *
     * @param id of the object to be retrieved.
     * @param eager boolean indicating eager (as opposed to lazy) fetching
	 * @return BioPAXElement
	 * @deprecated may be made private; 'eager' is of no use outside here?; use getById
     */
    BioPAXElement getElement(String id, boolean eager);

    
    /**
     * This method returns a set of objects in the model of the given class.
     * Contents of this set should not be modified.
	 *
     * @param filterBy class to be used as a filter.
     * @param eager boolean indicating eager (as opposed to lazy) fetching
	 * @return an unmodifiable set of objects of the given class.
	 *
	 * @deprecated may be made private; 'eager' is of no use outside here?; use getObjects
     */
    <T extends BioPAXElement> Set<T> getElements(Class<T> filterBy, boolean eager);


	 /**
	 * Searches the lucene index and returns the set of objects 
	 * of the given class in the model that match the query string.
	 * 
     * @param query String
	 * @param filterBy class to be used as a filter.
     * @return ordered by relevance list of elements
     * 
     * @deprecated use 'find' and 'getById' combination instead
     */
    <T extends BioPAXElement> List<T> search(String query, Class<T> filterBy);

    
	 /**
	 * Searches the lucene index and returns the set of IDs 
	 * of the BioPAX elements of given class in the model 
	 * that match the query string.
	 * 
     * @param query String
	 * @param filterBy class to be used as a filter.
     * @return ordered by the element's relevance list of rdfIds
     */
    List<String> find(String query, Class<? extends BioPAXElement> filterBy);

    
    /**
     * Writes the complete model as BioPAX (OWL)
     * 
     * @param outputStream
     */
    void exportModel(OutputStream outputStream);   
}