package cpath.dao.internal.filters;


import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.Provenance;


/**
 * Defines a filter by data sources for {@link Entity}
 * 
 * @author rodche
 *
 */
public class EntityDataSourceFilter extends SearchFilterAdapter<Entity, Provenance>{

	@Override
	public boolean accepted(Entity searchResult) {
		// TODO Auto-generated method stub
		return false;
	}

}
