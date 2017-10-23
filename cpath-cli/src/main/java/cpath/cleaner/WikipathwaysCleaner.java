package cpath.cleaner;

import cpath.service.Cleaner;
import org.biopax.paxtools.controller.ModelUtils;
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

/**
 * Implementation of Cleaner interface for Wikipathways data.
 */
final class WikipathwaysCleaner implements Cleaner {
	
	// logger
    private static Logger log = LoggerFactory.getLogger(WikipathwaysCleaner.class);

	private static final Map<String, String> fixXrefDbMap = new HashMap<String, String>();

	static {
		fixXrefDbMap.put("ncbigene","NCBI Gene");
		fixXrefDbMap.put("kegg.compound","KEGG Compound");
		fixXrefDbMap.put("kegg.genes","KEGG Genes");
		fixXrefDbMap.put("lipidmaps","LIPID MAPS");
		fixXrefDbMap.put("hgnc.symbol","HGNC Symbol");
		fixXrefDbMap.put("mirbase","miRBase Sequence"); //e.g. MI0000001 (not MIMAT0000001, which is "miRBase mature sequence")
	}

    public void clean(InputStream data, OutputStream cleanedData)
	{	
		// create bp model from dataFile
		SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
		Model model = simpleReader.convertFromOWL(data);
		log.info("Cleaning NetPath data, please be patient...");

		for(Xref x : new HashSet<Xref>(model.getObjects(Xref.class))) {

			//skip for PublicationXref
			if(x instanceof PublicationXref) continue;

			//Go ahead - for a unification or relationship Xref

			if(x.getDb() != null && x.getId() != null) {
				final String db = x.getDb().toLowerCase();

				if(fixXrefDbMap.containsKey(db)) {
					x.setDb(fixXrefDbMap.get(db)); //use the corresponding standard value
				}

				//If a UnificationXref has db:uniprot* but belongs to not a ProteinReference object,
				//then we're replacing it there with a similar RelationshipXref:
				if(x instanceof UnificationXref && x.getDb().toLowerCase().startsWith("uniprot")) {
					for(XReferrable owner : new HashSet<XReferrable>(x.getXrefOf())) {
						if(!(owner instanceof ProteinReference)) {
							owner.removeXref(x);
							owner.addXref(BaseCleaner.getOrCreateRx(x, model));
							log.info(String.format("Replaced the UX %s of %s (%s) with a similar RX.",
								x, owner.getModelInterface().getSimpleName(), owner.getUri()));
						}
					}
				}

			} else {
				//delete bad UX or RX:
				model.remove(x);
				for(XReferrable owner : new HashSet<XReferrable>(x.getXrefOf())) {
					owner.removeXref(x);
				}
				log.info("Deleted an incomplete/invalid xref both from the model and 'xref' properties: " + x);
			}
		}

		ModelUtils.removeObjectsIfDangling(model, UtilityClass.class);
		
		// serialize the model, return
		try {
			simpleReader.convertToOWL(model, cleanedData);
		} catch (Exception e) {
			throw new RuntimeException("clean(), Exception thrown while saving cleaned WikiPathways data", e);
		}		
	}
}
