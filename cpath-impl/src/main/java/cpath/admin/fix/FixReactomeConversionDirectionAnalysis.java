/**
 * 
 */
package cpath.admin.fix;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Conversion;
import org.biopax.paxtools.model.level3.ConversionDirectionType;
import org.biopax.paxtools.model.level3.Provenance;
import org.springframework.util.Assert;

import cpath.dao.Analysis;

/**
 * @author rodche
 *
 */
final class FixReactomeConversionDirectionAnalysis implements Analysis {
	
	@Override
    public void execute(Model model) 
    {	
		//find HumanCyc
		Provenance reac = null;
		for(Provenance provenance : model.getObjects(Provenance.class)) {
			if("Reactome".equals(provenance.getStandardName())) {
				reac = provenance;
				break;
			}
		}
		Assert.notNull(reac, "Reactome data source not found.");
		
		//set left-to-right direction for all the Reactome's conversions if not already set
		for(Conversion c : model.getObjects(Conversion.class)) {
			if(c.getDataSource().contains(reac) && c.getConversionDirection() == null)
				c.setConversionDirection(ConversionDirectionType.LEFT_TO_RIGHT);
		}

    }
}
