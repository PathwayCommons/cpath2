package cpath.jpa;

import java.util.Set;



/**
 * @author rodche
 *
 */
interface MappingsRepositoryCustom {
	

	/**
	 * Guess the identifier type (chemical vs gene/protein) 
	 * and returns other Is it maps to.
	 * 
	 * @param identifier
	 * @return
	 */
	Set<String> map(String identifier);
	
	
	/**
     * Maps an identifier to primary ID(s) of a given type.
     * 
     * Normally, the result set contains only one ID.
     * If the result contains more than one value, which does not
     * necessarily an error, then it's up to other methods to decide 
     * how to proceed; e.g., one should not probably merge different 
     * data objects if the mapping is known to be umbiguous,
     * but it's usually ok to generate relationship xrefs 
     * or use the resulting IDs in a BioPAX graph query.
     * 
     * @param fromDb data collection name or null (to use all source ID types)
     * @param fromId the source ID
     * @param toDb standard (MIRIAM) preferred name of the target ID type (e.g., 'UniProt')
     * 
     * @return a set of primary IDs of the type; normally one or none elements
     */
    Set<String> map(String fromDb, String fromId, String toDb);

}
