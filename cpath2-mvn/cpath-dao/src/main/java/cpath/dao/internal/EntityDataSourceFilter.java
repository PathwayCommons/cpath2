package cpath.dao.internal;


import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.Provenance;

import cpath.dao.SearchFilter;


/**
 * Defines a filter by data sources for {@link Entity}
 * 
 * @author rodche
 *
 */
public class EntityDataSourceFilter implements SearchFilter<Provenance>{

	@Override
	public boolean accepted(BioPAXElement searchResult, Provenance... values) {
		// TODO Auto-generated method stub
		return false;
	}

}
