package cpath.cleaner;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.util.ClassFilterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.service.api.Cleaner;

/**
 * Implementation of Cleaner interface for NetPath data. 
 */
final class NetPathCleaner implements Cleaner {
	private static Logger log = LoggerFactory.getLogger(NetPathCleaner.class);

	public void clean(InputStream data, OutputStream cleanedData) {
		// create bp model from dataFile
		SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
		Model model = simpleReader.convertFromOWL(data);
		log.info("Cleaning NetPath data, please be patient...");

		//fix bad/invalid URIs (there are several with spaces in them...)
		ModelUtils.fixInvalidUris(model);

		// Fix some CV xrefs
		// a CV must have one unification xref;
		// if there are also relationship and publication xrefs, it's a biopax error, but we'll keep as is (not critical);
		// So, if there is no unification xref but rel. xrefs (in fact, one or none in NetPath), we convert rel. to unif. xref.
		Set<ControlledVocabulary> cvs = new HashSet<>(model.getObjects(ControlledVocabulary.class));
		for (ControlledVocabulary cv : cvs) {
			log.info("Processing " + cv.toString() + "; xrefs: " + cv.getXref());

			//insert "L-" after "phospho-" in MFV terms (if it does not contain "phospho-L-" already)
			if (cv instanceof SequenceModificationVocabulary) {
				for (String t : new HashSet<>(cv.getTerm())) {
					if (t.contains("phospho-") && !t.contains("phospho-L-")) {
						//insert "L-", replace term
						cv.removeTerm(t);
						t = t.replace("phospho-", "phospho-L-");
						cv.addTerm(t);
						log.info("inserted 'L-' into a 'phospho-' CV term: " + cv);
					}
				}
			}

			Set<UnificationXref> urefs = new ClassFilterSet<>(new HashSet<>(cv.getXref()), UnificationXref.class);
			//skip if there is a unification xref
			if (!urefs.isEmpty()) {
				continue; //perhaps, will never happen (I manually checked a couple of orig. files)
			}

			Set<RelationshipXref> rxrefs = new ClassFilterSet<>(new HashSet<>(cv.getXref()), RelationshipXref.class);
			for (RelationshipXref x : rxrefs) {
				//remove and skip for bad xref (just in case there are any)
				if (x.getDb() == null || x.getId() == null) {
					cv.removeXref(x);
					model.remove(x);
					continue;
				}
				String id = x.getId();
				String uri = "UX_" + BaseCleaner.encode(x.getDb() + "_" + id);
				UnificationXref ux = (UnificationXref) model.getByID(uri);
				if (ux == null) {
					ux = model.addNew(UnificationXref.class, uri);
					ux.setDb(x.getDb());
					ux.setId(id);
				}
				cv.removeXref(x);
				cv.addXref(ux);
			}
		}

		//convert shared UnificationXrefs into RelationshipXrefs (in fact, some of those are just invalid db/id)
		for (UnificationXref x : new HashSet<>(model.getObjects(UnificationXref.class))) {
			if (x.getXrefOf().size() > 1) {
				//convert to RX, re-associate
				RelationshipXref rx = BaseCleaner.getOrCreateRx(x, model);
				for (XReferrable owner : new HashSet<>(x.getXrefOf())) {
					if (owner instanceof ControlledVocabulary) {
						continue; //CVs can use same UX, but that means they are to merge...
					}
					owner.removeXref(x);
					owner.addXref(rx);
				}
			}
		}

		//SPEs that are not component/participant are not needed
		ModelUtils.removeObjectsIfDangling(model, SimplePhysicalEntity.class);
		//xrefs, CVs et al. utility class individuals are not interesting for pathway analysis
		ModelUtils.removeObjectsIfDangling(model, UtilityClass.class);

		// convert model back to OutputStream for return
		try {
			simpleReader.convertToOWL(model, cleanedData);
		} catch (Exception e) {
			throw new RuntimeException("Failed to save cleaned NetPath model", e);
		}
	}
}
