package cpath.jpa;

import java.io.Serializable;

//import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/**
 * Base interface to expose only required spring-data repository methods
 * instead of all; i,e., the main repository interface will extend this 
 * rather than CrudRepository<LogEntity, Long>.
 * 
 * Methods here follow spring-data naming and signature conventions;
 * these must match exactly the names and signatures of spring-data ones.
 * 
 * @author rodche
 *
 */
@NoRepositoryBean
interface BaseRepository<T, ID extends Serializable> extends Repository<T, ID> {

	<S extends T> S save(S entity);
	
	<S extends T> Iterable<S> save(Iterable<S> entities);
	
	void deleteAll();
	
	long count();
		
}
