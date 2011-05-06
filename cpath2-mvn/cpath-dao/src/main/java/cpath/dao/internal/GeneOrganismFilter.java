package cpath.dao.internal;


import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.BioSource;

import cpath.dao.SearchFilter;


/**
 * Defines a filter by organism for {@link Gene}
 * 
 * @author rodche
 *
 */
public class GeneOrganismFilter implements SearchFilter<BioSource>{

	@Override
	public boolean accepted(BioPAXElement searchResult, BioSource... values) {
		// TODO Auto-generated method stub
		return false;
	}

}
