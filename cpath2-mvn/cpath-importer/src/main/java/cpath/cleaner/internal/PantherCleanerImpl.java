package cpath.cleaner.internal;

import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.io.*;

import java.util.Set;
import java.util.HashSet;

import java.io.*;

/**
 * 
 * @author rodche
 * 
 */
final class PantherCleanerImpl extends BaseCleanerImpl {
    
	@Override
	public String clean(final String pathwayData) 
	{	
		// create bp model from pathwayData
		InputStream inputStream =
			new BufferedInputStream(new ByteArrayInputStream(pathwayData.getBytes()));
		SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
		Model model = simpleReader.convertFromOWL(inputStream);

		// replace PANTHER with either: PANTHER Family, PANTHER Pathway, or PANTHER Node.	
		Set<Xref> xrefs = new HashSet<Xref>(model.getObjects(Xref.class));
		for (Xref xref : xrefs) {
			if (xref.getDb() != null && xref.getId() != null 
					&& xref.getDb().trim().equalsIgnoreCase("PANTHER")) 
			{
				String id = xref.getId().trim();				
				if(id.startsWith("PTHR")) {
					xref.setDb("PANTHER Family");
				} else if(id.startsWith("PTN")) {
					xref.setDb("PANTHER Node");
				} else if(id.startsWith("P")) {
					xref.setDb("PANTHER Pathway");
				}
			}
		}
		
		// convert model back to OutputStream for return
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			simpleReader.convertToOWL(model, outputStream);
		}
		catch (Exception e) {
			throw new RuntimeException("clean(), Exception thrown while cleaning pathway data!", e);
		}

		return outputStream.toString();
	}
}
