package cpath.dao.internal.filters;


import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.Gene;


/**
 * Defines a filter by organism for {@link Gene}
 * 
 * @author rodche
 *
 */
public class GeneOrganismFilter extends SearchFilterAdapter<Gene, BioSource>{

	@Override
	public boolean accepted(Gene searchResult) {
		// TODO Auto-generated method stub
		return false;
	}

}
