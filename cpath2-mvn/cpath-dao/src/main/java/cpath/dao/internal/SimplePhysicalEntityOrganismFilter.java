package cpath.dao.internal;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.Dna;
import org.biopax.paxtools.model.level3.DnaRegion;
import org.biopax.paxtools.model.level3.Protein;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;

import cpath.dao.SearchFilter;

/**
 * Defines a filter by organism for {@link SimplePhysicalEntity},
 * i.e.: {@link Protein},  {@link Dna}, {@link DnaRegion}, etc.
 * (not for complexes)
 * 
 * @author rodche
 *
 */
public class SimplePhysicalEntityOrganismFilter implements
		SearchFilter<SimplePhysicalEntity> 
{

	@Override
	public boolean accepted(BioPAXElement searchResult,
			SimplePhysicalEntity... values) {
		// TODO Auto-generated method stub
		return false;
	}

}
