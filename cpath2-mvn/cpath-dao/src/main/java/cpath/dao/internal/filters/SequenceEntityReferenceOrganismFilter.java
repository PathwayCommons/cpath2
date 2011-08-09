package cpath.dao.internal.filters;

import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.DnaReference;
import org.biopax.paxtools.model.level3.DnaRegionReference;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.SequenceEntityReference;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;

import cpath.dao.filters.SearchFilterRange;


/**
 * Defines a filter by organism for sequence entity references,
 * i.e. not {@link SmallMoleculeReference}, such as: 
 * {@link ProteinReference},  {@link DnaReference}, {@link DnaRegionReference}, etc.
 * This filter checks BioPAX 'organism' property of the entity reference.
 * 
 * @author rodche
 *
 */
@SearchFilterRange(SequenceEntityReference.class)
public class SequenceEntityReferenceOrganismFilter 
	extends SearchFilterAdapter<SequenceEntityReference, String> 
{

	public SequenceEntityReferenceOrganismFilter() {
		super(SequenceEntityReference.class);
	}
	
	@Override
	public boolean apply(SequenceEntityReference seqEntRef) 
	{
		if (values.isEmpty())
			return true;

		BioSource bs = seqEntRef.getOrganism();
		if (bs != null && this.values.contains(bs.getRDFId())) {
			return true;
		}

		return false;
	}

}
