package cpath.cleaner.internal;

import cpath.importer.Cleaner;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Named;
import org.biopax.paxtools.model.level3.Xref;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

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
			String id = xr.getId().trim();
			
			if (id == null) continue;
			
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
			named.setDisplayName(s);
		}
	}

}
