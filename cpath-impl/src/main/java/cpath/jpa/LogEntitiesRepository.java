package cpath.jpa;


import org.springframework.data.querydsl.QueryDslPredicateExecutor;


/**
 * Main repository interface to be used by spring-data 
 * (methods follow spring-data naming and signature conventions).
 * 
 * @author rodche
 *
 */
public interface LogEntitiesRepository extends BaseRepository<LogEntity, Long>, 
	QueryDslPredicateExecutor<LogEntity>, LogEntitiesRepositoryCustom {
	
	LogEntity findByEventNameAndGeolocCountryAndGeolocRegionAndGeolocCityAndDate(String name, String country, String region, String city, String date);
		
}
