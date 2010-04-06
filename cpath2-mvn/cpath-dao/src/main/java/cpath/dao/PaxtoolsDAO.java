// $Id$
//------------------------------------------------------------------------------
/** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center.
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

// imports
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.UnificationXref;

import java.util.Set;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * An interface which provides methods to query a Paxtools model.
 */
public interface PaxtoolsDAO {

	/**
	 * Persists the given model to the db.
	 *
	 * @param model Model
	 * @param createIndex boolean
	 */
	void importModel(Model model, boolean createIndex);

	/**
	 * Persists the given model to the db.
	 *
	 * @param biopaxFile File
	 * @param createIndex boolean
	 * @throws FileNoteFoundException
	 */
	void importModel(File biopaxFile, boolean createIndex) throws FileNotFoundException;

    /**
     * This method returns the biopax element with the given id,
     * returns null if the object with the given id does not exist
     * in this model.
	 *
     * @param id of the object to be retrieved.
	 * @param eager boolean indicating eager (as opposed to lazy) fetching
     * @return BioPAXElement
     */
    BioPAXElement getByID(String id, boolean eager);

    /**
     * This method returns a set of objects in the model of the given class.
     * Contents of this set should not be modified.
	 *
     * @param filterBy class to be used as a filter.
	 * @param eager boolean indicating eager (as opposed to lazy) fetching
     * @return an unmodifiable set of objects of the given class.
     */
    <T extends BioPAXElement> Set<T> getObjects(Class<T> filterBy, boolean eager);

	/**
	 * Given a unification xref, returns a matching biopax element.
	 *
	 * @param unificationXref UnificationXref
	 * @return BioPAXElement
	 */
	<T extends BioPAXElement> T getByUnificationXref(UnificationXref unificationXref);

	/**
     * Search the lucene index for given string and returns
	 * and returns a set of objects in the model of the given class
	 * 
     * @param query String
     * @param filterBy class to be used as a filter.
     * @return List<BioPAXElement>
     */
    <T extends BioPAXElement> Set<T> search(String query, Class<T> filterBy);
}