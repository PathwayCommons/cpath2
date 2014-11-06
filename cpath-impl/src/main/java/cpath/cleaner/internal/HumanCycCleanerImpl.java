package cpath.cleaner.internal;

import cpath.importer.Cleaner;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HumanCycCleanerImpl implements Cleaner
{
	private static final Logger LOG = LoggerFactory.getLogger(HumanCycCleanerImpl.class);
	
	public void clean(InputStream data, OutputStream cleanedData)
	{
		try
		{
			SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
			Model model = simpleReader.convertFromOWL(data);
			removeUnificationXrefInPhysicalEntities(model);
			cleanXrefIDs(model);
			cleanXrefDBName(model);
			cleanHtmlInDisplayNames(model);
//			fix_RDH14_NT5C1B_fusion(model); //the problem is not present in 17.1 HymanCyc biopax
			cleanMultipleUnificationXrefs(model);
			// set organism to all pathways, where it's null
			setOrganismHomoSapiens(model);
			simpleReader.convertToOWL(model, cleanedData);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Exception in HumanCycCleanerImpl.clean", e);
		}
	}

	private void setOrganismHomoSapiens(Model model) {
		Set<BioSource> organisms = model.getObjects(BioSource.class);
		if(organisms.size() > 1) {
			LOG.error("Skip setting 'Homo sapiens' for HumanCyc Pathways, " +
					"for there are several BioSources: " + organisms);
			return;
		} else if(organisms.isEmpty()) {
			LOG.warn("Will create a new 'Homo sapiens' BioSource " +
					"and set it for all no-organism Pathways in HumanCyc.");
		}
		
		//there is only one BioSource (must be human); use it -
		BioSource human = organisms.iterator().next();
		for(Pathway pathway : model.getObjects(Pathway.class)) {
			if(pathway.getOrganism() == null)
				pathway.setOrganism(human);
		}
	}

	/**
	 * Some UnificationXref ids starts with a blank, and some are like "CAS: 103-82-2".
	 * Xrefs to PDB contains small letters.
	 *  - not anymore in the 17.1 BioCyc release
	 */
	protected void cleanXrefIDs(Model model)
	{
		for (Xref xr : model.getObjects(Xref.class))
		{
			String id = xr.getId();

			if (id == null) continue;

			id = id.trim();

			if (id.contains(": "))
			{
				id = id.substring(id.indexOf(": ") + 2);
			}

			if (xr.getDb().equals("PDB") || xr.getDb().equals("Protein Data Bank"))
			{
				id = id.toUpperCase();
			}

			xr.setId(id);
		}
	}

	/**
	 * HumanCyc refers to GenBank proteins as "Entrez".
	 * We change those xrefs to use "Protein GenBank Identifier"
	 * see {@code http://www.ebi.ac.uk/ontology-lookup/browse.do?ontName=MI&termId=MI%3A0851&termName=protein%20genbank%20identifier}
	 * 
	 */
	protected void cleanXrefDBName(Model model)
	{
		for (Xref xr : model.getObjects(Xref.class))
		{
			if(xr.getDb() == null)
				LOG.warn("Xref.db is NULL : " + xr.getRDFId());
			else if(xr.getDb().equals("Entrez")) 
				xr.setDb("Protein GenBank Identifier");
			else if(xr.getDb().equalsIgnoreCase("NCBI Taxonomy")) 
				xr.setDb("taxonomy");
		}
	}
	
	protected void removeUnificationXrefInPhysicalEntities(Model model)
	{
		for (PhysicalEntity pe : model.getObjects(PhysicalEntity.class))
		{
			for (Xref xref : new HashSet<Xref>(pe.getXref()))
			{
				if (xref instanceof UnificationXref)
				{
					pe.removeXref(xref);
				}
			}
		}
	}
	
	protected void cleanHtmlInDisplayNames(Model model)
	{
		for (Named named : model.getObjects(Named.class))
		{
			String s = named.getDisplayName();
			if (s != null)
			{
				s = removeHTML(s);
				named.setDisplayName(s);
			}
			
			s = named.getStandardName();
			if (s != null)
			{
				s = removeHTML(s);
				named.setStandardName(s);
			}
		}
	}

	/*no need to use this with the 17.1 BioCyc biopax release*/
	private String removeHTML(String s)
	{
		s = s.replaceAll("</i>", "").replaceAll("<i>","");
		s = s.replaceAll("</I>", "").replaceAll("<I>","");
		s = s.replaceAll("</SUB>", "").replaceAll("<SUB>","");
		s = s.replaceAll("</sub>", "").replaceAll("<sub>","");
		s = s.replaceAll("</sup>", "").replaceAll("<sup>","");
		s = s.replaceAll("</SUP>", "").replaceAll("<SUP>","");
		return s;
	}

	/**
	 * The protein NT5C1B contains info of RDH14. This is due to an old incorrect prediction in 
	 * TrEMBL. It is fixed in Uniprot, but remained in HumanCyc.
	 * 
	 * @param model
	 */
	protected void fix_RDH14_NT5C1B_fusion(Model model)
	{
		EntityReference er = (EntityReference) model.getByID(model.getXmlBase()+"ProteinReference147987");

		if (er != null)
		{
			clearRDH14(er);

			for (SimplePhysicalEntity pe : er.getEntityReferenceOf())
			{
				clearRDH14(pe);
			}
		}
	}

	protected void clearRDH14(Named nd)
	{
		nd.setDisplayName("NT5C1B");
		nd.setStandardName("NT5C1B");

		// We need only one unification xref. Clear all others. 
		for (Xref xref : new HashSet<Xref>(nd.getXref()))
		{
			if (xref instanceof UnificationXref && !xref.getRDFId().equals("UnificationXref147997"))
			{
				nd.removeXref(xref);
			}
		}

		for (String name : new HashSet<String>(nd.getName()))
		{
			if (name.startsWith("RETINOL") || name.startsWith("PANCREAS"))
			{
				nd.removeName(name);
			}
		}
	}

	static Map<String, String> toKeep = new HashMap<String, String>();
	static
	{
		toKeep.put("Q9HBH5", "Q96P26");
		toKeep.put("Q9UKY3", "P23141");
		toKeep.put("Q8N0Y7", "P18669");
		toKeep.put("Q12894", "O43820");
		toKeep.put("Q9BQ83", "P50224");
		toKeep.put("Q13956", "P18545");
		toKeep.put("Q9UQ88", "P21127");
		toKeep.put("P61550", "Q9UJY2");
		toKeep.put("Q9NYP3", "O95825");
		toKeep.put("Q96K59", "Q99519");
	}

	protected void cleanMultipleUnificationXrefs(Model model)
	{
		for (EntityReference er : model.getObjects(EntityReference.class))
		{
			for (String key : toKeep.keySet())
			{
				String val = toKeep.get(key);

				if (hasRef(er, key) && hasRef(er, val))
				{
					keepOnlyThis(er, val);
				}
			}
		}
	}
	
	protected void keepOnlyThis(XReferrable ele, String ref)
	{
		Set<UnificationXref> removeSet = new HashSet<UnificationXref>();

		for (Xref xref : ele.getXref())
		{
			if (xref instanceof UnificationXref)
			{
				removeSet.add((UnificationXref) xref);

				if (!xref.getId().equals(ref)) 
				{
					removeSet.add((UnificationXref) xref);
				}
			}
		}

		for (UnificationXref xref : removeSet)
		{
			ele.removeXref(xref);
		}
	}
	
	protected boolean hasRef(XReferrable ele, String ref)
	{
		for (Xref xref : ele.getXref())
		{
			if (xref.getId() != null && xref.getId().equals(ref)) return true;
		}
		return false;
	}
}
