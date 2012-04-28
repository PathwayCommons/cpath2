package cpath.cleaner.internal;

import cpath.importer.Cleaner;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;

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
			cleanHtmlInDisplayNames(model);
			fix_RDH14_NT5C1B_fusion(model);

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

			if (xr.getDb().equals("PDB") || xr.equals("Protein Data Bank"))
			{
				id = id.toUpperCase();
			}

			xr.setId(id);
		}
	}

	protected void cleanHtmlInDisplayNames(Model model)
	{
		for (Named named : model.getObjects(Named.class))
		{
			String s = named.getDisplayName();
			if (s == null) continue;

			s = s.replaceAll("</i>", "").replaceAll("<i>","");
			s = s.replaceAll("</SUB>", "").replaceAll("<SUB>","");
			s = s.replaceAll("</sub>", "").replaceAll("<sub>","");
			named.setDisplayName(s);
		}
	}

	/**
	 * The protein NT5C1B contains info of RDH14. This is due to an old incorrect prediction in 
	 * TrEMBL. It is fixed in Uniprot, but remained in HumanCyc.
	 * 
	 * @param model
	 */
	protected void fix_RDH14_NT5C1B_fusion(Model model)
	{
		EntityReference er = (EntityReference) model.getByID("ProteinReference147987");
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
				nd.getXref().remove(xref);
			}
		}

		for (String name : new HashSet<String>(nd.getName()))
		{
			if (name.startsWith("RETINOL") || name.startsWith("PANCREAS"))
			{
				nd.getName().remove(name);
			}
		}
	}
}
