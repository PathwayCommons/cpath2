package cpath.dao.internal.filters;


import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.Gene;

import cpath.dao.filters.SearchFilterRange;


/**
 * Defines a filter by organism for {@link Gene}
 * 
 * @author rodche
 *
 */
@SearchFilterRange(Gene.class)
public class GeneOrganismFilter extends SearchFilterAdapter<Gene, BioSource>{

	@Override
	public boolean apply(Gene searchResult) {
		// TODO Auto-generated method stub
		return false;
	}

}
