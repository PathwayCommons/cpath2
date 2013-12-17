/**
 * 
 */
package cpath.admin.fix;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.Pathway;
import org.biopax.paxtools.model.level3.Provenance;
import org.springframework.util.Assert;

import cpath.dao.Analysis;

/**
 * @author rodche
 *
 */
final class FixHumanCycPathwayOrganismAnalysis implements Analysis {
	
	@Override
    public void execute(Model model) 
    {	
		// find Homo sapiens
		BioSource hs = (BioSource) model.getByID("http://identifiers.org/taxonomy/9606");
		Assert.notNull(hs, "Homo sapiens (9606) BioSource not found.");
		
		//find HumanCyc
		Provenance hcyc = null;
		for(Provenance provenance : model.getObjects(Provenance.class)) {
			if("HumanCyc".equals(provenance.getStandardName())) {
				hcyc = provenance;
				break;
			}
		}
		Assert.notNull(hcyc, "HumanCyc data source not found.");
		
		//set the organism for all the HumanCyc pathways if not already set
		for(Pathway pathway : model.getObjects(Pathway.class)) {
			if(pathway.getDataSource().contains(hcyc) && pathway.getOrganism() == null)
				pathway.setOrganism(hs);
		}

    }
}
