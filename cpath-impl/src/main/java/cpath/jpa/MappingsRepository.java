package cpath.jpa;


import java.util.List;

import org.springframework.data.repository.CrudRepository;


/**
 * A spring-data repository (auto-instantiated) of Mapping entities 
 * (all methods here follow the spring-data naming and signature conventions,
 * and therefore do not require to be implemented by us; these will be auto-generated).
 * 
 * @author rodche
 */
public interface MappingsRepository extends CrudRepository<Mapping, Long> {

	/**
	 * All Mappings 'To' the given type of ID.
	 * 
	 * @param dest
	 * @return
	 */
	List<Mapping> findByDestIgnoreCase(String dest);
	
	/**
	 * Mappings 'To' the given identifier.
	 * 
	 * @param dest
	 * @param destId
	 * @return
	 */
	List<Mapping> findByDestIgnoreCaseAndDestId(String dest, String destId);
	
	
	/**
	 * Mappings 'From' the given id (any kind) 'To' the target type of ID.
	 * 
	 * @param srcId
	 * @param dest to map to
	 * @return
	 */
	List<Mapping> findBySrcIdAndDestIgnoreCase(String srcId, String dest);
	
	
	/**
	 * Mappings 'From' the given source db/id 'To' the target type of ID.
	 * 
	 * @param src
	 * @param srcId
	 * @param dest to map to
	 * @return
	 */
	List<Mapping> findBySrcIgnoreCaseAndSrcIdAndDestIgnoreCase(String src, String srcId, String dest);
		
	
	/**
	 * This is to find a unique mapping entry,
	 * e.g. to check if it exists before saving.
	 * 
	 * @param src
	 * @param srcId
	 * @param dest
	 * @param destId
	 * @return
	 */
	Mapping findBySrcIgnoreCaseAndSrcIdAndDestIgnoreCaseAndDestId(String src, String srcId, String dest, String destId);
	
}
