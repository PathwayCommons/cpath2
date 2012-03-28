package cpath.cleaner.internal;

import cpath.config.CPathSettings;

import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.io.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import java.io.*;

/**
 * Implementation of Cleaner interface for Panther Pathway (2012) data. 
 * The main problem is - use of PANTHER_HIT_* (rdf:ID) UnificationXrefs, 
 * which actually should be RelationshipXref (otherwise, different PRs become equivalent/replaced).
 * (Other problems were: too many duplicate/equivalent CV and Evidence objects -
 * won't fix here)
 * 
 * @author rodche
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
		ModelUtils modelUtils = new ModelUtils(model);

		
		// replace URIs (in Panther, they can be too long or the same URI for different objects in different files, or 
		// two URIs differ only in string capitalization, etc...)
		Set<Level3Element> sourceElements = new HashSet<Level3Element>(model.getObjects(Level3Element.class));
		for (Level3Element l3e : sourceElements) {
			String newRDFId = l3e.getRDFId().substring(0, 255) + "/" + System.currentTimeMillis();
			replaceID(model, l3e, newRDFId);
		}
		
		
		// create a Relationship type CV "Homology"
		final RelationshipTypeVocabulary rcv = model.addNew(RelationshipTypeVocabulary.class, 
			ModelUtils.BIOPAX_URI_PREFIX + "RelationshipTypeVocabulary:BY_HOMOLOGY");
		rcv.addTerm("by homology");
		rcv.addComment(CPathSettings.CPATH2_GENERATED_COMMENT);
		final UnificationXref rux = model.addNew(UnificationXref.class, 
			ModelUtils.BIOPAX_URI_PREFIX + "UnificationXref:MOLECULAR+INTERACTIONS+ONTOLOGY_MI%3A2163");
		rux.setDb("MOLECULAR INTERACTIONS ONTOLOGY");
		rux.setId("MI:2163");
		rcv.addXref(rux);
		
		// replace UnificationXref with ID like PANTHER_HIT_* with RelationshipXref
		Set<UnificationXref> uxrefs = new HashSet<UnificationXref>(model.getObjects(UnificationXref.class));
		Map<Xref,Xref> subs = new HashMap<Xref, Xref>();
		for (UnificationXref ux : uxrefs) {
			// 
			if (ux.getRDFId().contains("PANTHER_HIT_") && "PANTHER".equalsIgnoreCase(ux.getDb())) {
				RelationshipXref rx = model.getLevel()
					.getDefaultFactory().create(RelationshipXref.class, ux.getRDFId());
				rx.setDb(ux.getDb());
				// also, split wrong IDs like PTHR12804:SF0 into valid one and version (Miriam template)
				String id = ux.getId();
				if(id.contains(":")) {
					String idv[] = ux.getId().split(":");
					id = idv[0];
					rx.setIdVersion(idv[1]);
				}
				rx.setId(id);
				//set relationshipType (create vocabulary)
				rx.setRelationshipType(rcv);
				
				subs.put(ux, rx);
			}
		}
		
		// replace/repair/clean
		modelUtils.replace(subs);
		model.repair();
		modelUtils.removeObjectsIfDangling(UnificationXref.class);
				
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
