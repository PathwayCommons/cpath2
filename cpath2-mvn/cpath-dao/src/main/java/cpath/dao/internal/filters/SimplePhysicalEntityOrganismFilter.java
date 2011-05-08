package cpath.dao.internal.filters;

import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.Dna;
import org.biopax.paxtools.model.level3.DnaRegion;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;

import cpath.dao.filters.SearchFilterRange;


/**
 * Defines a filter by organism for {@link SimplePhysicalEntity},
 * i.e.: {@link Protein},  {@link Dna}, {@link DnaRegion}, etc.
 * (not for complexes)
 * 
 * @author rodche
 *
 */
@SearchFilterRange(SimplePhysicalEntity.class)
public class SimplePhysicalEntityOrganismFilter 
	extends SearchFilterAdapter<SimplePhysicalEntity, BioSource> 
{

	@Override
	public boolean apply(SimplePhysicalEntity searchResult) {
		// TODO Auto-generated method stub
		return false;
	}

}
