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

import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;

import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.TraverseResponse;

import java.util.Collection;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.Set;

/**
 * BioPAX data access (both model and repository).
 */
public interface PaxtoolsDAO extends Model {	
	
	/**
	 * Persists the given model to the db.
	 *
	 * @param biopaxFile File
	 * @throws FileNotFoundException biopaxFile not present
	 */
	void importModel(File biopaxFile) throws FileNotFoundException;

	

	 /**
	 * Full-text search for BioPAX elements.
	 * 
     * @param query String (keywords or Lucene query string)
	 * @param page page number (when the number of hits exceeds a threshold)
	 * @param filterByType - class filter for the full-text search (actual effect may depend on the concrete implementation!)
	 * @param datasources  - filter by datasource(s)
	 * @param organisms - filter by organism(s)
	 * @return ordered by the element's relevance list of hits
    */
   SearchResponse search(String query, int page,
   		Class<? extends BioPAXElement> filterByType, String[] dsources, String[] organisms);

    
    
    /**
     * Exports the entire model (if no IDs are given) 
     * or a sub-model as BioPAX (OWL)
     * 
     * @param outputStream
     * @param ids (optional) build a sub-model from these IDs and export it
     */
    void exportModel(OutputStream outputStream, String... ids);   
    
    
    /**
     * Creates a "valid" sub-model from the BioPAX elements
     * 
     * @param ids a set of valid RDFId
     * @return
     */
    Model getValidSubModel(Collection<String> ids);
 
        
    /**
     * Initializes the properties and inverse properties, 
     * including collections!
     * 
     */
    void initialize(Object element);

    
    /**
     * Merges the source model into this one.
     * 
     * @param source a model to merge
     */
    void merge(Model source);
    
    
    /**
     * Merges (adds and updates) a BioPAX element to the model.
     * 
     * @param bpe
     */
    void merge(BioPAXElement bpe);

    
    /**
     * Updates
     * 
     * @param model
     */
    void update(Model model);
    
    
    /**
     * Executes custom algorithms (or plugins)
     * 
     * @param analysis defines a job/query to perform
     * @param args - optional parameters for the algorithm
     * @return a detached (cloned) BioPAX sub-model
     */
    Model runAnalysis(Analysis analysis, Object... args);

    
    /**
     * Finds top (root) pathways in the entire BioPAX model.
     * (It's good idea to cache this method's result permanently,
     * may be - in the middle tier)
     * 
     * @return
     */
    SearchResponse getTopPathways();
    
    
    /**
     * Accesses and collects BioPAX property values 
     * at the end of the path (applied to every element in the list)
     * 
     * @param propertyPath
     * @param uris
     * @return source element uri, path, and corresponding values
     */
    TraverseResponse traverse(String propertyPath, String... uris);
    
    /**
     * Create or re-build the full-text index.
     */
    void index();
}