package cpath.cleaner.internal;

import cpath.importer.Cleaner;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HumanCycCleanerImpl implements Cleaner
{
	@Override
	public String clean(String pathwayData)
	{
		try
		{
			SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
			Model model = simpleReader.convertFromOWL(new BufferedInputStream(
				new ByteArrayInputStream(pathwayData.getBytes())));

			cleanXrefIDs(model);
			cleanXrefDBName(model);
			cleanHtmlInDisplayNames(model);
			fix_RDH14_NT5C1B_fusion(model);
			cleanMultipleUnificationXrefs(model);

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			simpleReader.convertToOWL(model, outputStream);
			return outputStream.toString();
		}
		catch (Exception e)
		{
			System.out.println();
			throw new RuntimeException("Exception in HumanCycCleanerImpl.clean", e);
		}
	}

	/**
	 * Some UnificationXref ids starts with a blank, and some are like "CAS: 103-82-2".
	 * Xrefs to PDB contains small letters.
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
	 * HumanCyc refers to GenBank as Entrez.
	 */
	protected void cleanXrefDBName(Model model)
	{
		for (Xref xr : model.getObjects(Xref.class))
		{
			if (xr.getDb() != null && xr.getDb().equals("Entrez")) xr.setDb("GenBank");
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
		EntityReference er = (EntityReference) model.getByID("" +
			"http://biocyc.org/biopax/biopax-level3ProteinReference147987");

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
		toKeep.put("http://biocyc.org/biopax/biopax-level3ProteinReference149952", "P23141");
		toKeep.put("http://biocyc.org/biopax/biopax-level3ProteinReference133651", "P18669");
		toKeep.put("http://biocyc.org/biopax/biopax-level3ProteinReference133617", "P18669");
		toKeep.put("http://biocyc.org/biopax/biopax-level3ProteinReference143113", "O43820");
		toKeep.put("http://biocyc.org/biopax/biopax-level3ProteinReference150907", "P50224");
		toKeep.put("http://biocyc.org/biopax/biopax-level3ProteinReference144950", "P18545");
		toKeep.put("http://biocyc.org/biopax/biopax-level3ProteinReference161465", "P21127");
		toKeep.put("http://biocyc.org/biopax/biopax-level3ProteinReference125453", "Q9UJY2");
		toKeep.put("http://biocyc.org/biopax/biopax-level3ProteinReference165402", "O95825");
		toKeep.put("http://biocyc.org/biopax/biopax-level3ProteinReference143290", "Q99519");
	}

	protected void cleanMultipleUnificationXrefs(Model model)
	{
		for (String id : toKeep.keySet())
		{
			EntityReference er = (EntityReference) model.getByID(id);
			keepOnlyThis(er, toKeep.get(id));

			for (SimplePhysicalEntity pe : er.getEntityReferenceOf())
			{
				keepOnlyThis(pe, toKeep.get(id));
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
}
