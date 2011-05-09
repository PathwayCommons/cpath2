package cpath.importer.internal;

import java.util.Set;

import org.biopax.paxtools.controller.*;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Pathway;

import cpath.dao.Analysis;

/**
 * This is to infer parent pathways for 
 * all entities and save this information 
 * by adding special relationship xrefs.
 * 
 * TODO hold on using this tool ;), because the Normalizer does this in pre-merge...
 * 
 * @author rodche
 */
@Deprecated
public class PathwayMembershipRobot implements Analysis {
	
	@Override
	public Set<BioPAXElement> execute(Model model, Object... args) 
	{	
		ModelUtils modelUtils = new ModelUtils(model);
		modelUtils.generateEntityProcessXrefs(Pathway.class, null);
		return null;
	}
}
