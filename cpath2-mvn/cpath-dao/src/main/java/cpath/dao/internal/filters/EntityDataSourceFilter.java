package cpath.dao.internal.filters;

import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.Provenance;

import cpath.dao.filters.SearchFilterRange;


/**
 * Defines a filter by data sources for {@link Entity}
 * 
 * @author rodche
 *
 */
@SearchFilterRange(Entity.class)
public class EntityDataSourceFilter extends 
	SearchFilterAdapter<Entity, String>
{
	@Override
	public boolean apply(Entity searchResult) {
		for(Provenance prov : searchResult.getDataSource()) {
			if(this.values.contains(prov.getRDFId()))
				return true;
		}
		return false;
	}

}
