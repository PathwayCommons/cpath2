package cpath.dao.internal;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.XReferrable;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.validator.utils.Normalizer;


class AbstractPaxtoolsDAO {
	private final static Log log = LogFactory.getLog(AbstractPaxtoolsDAO.class);

	protected Set<String> getByXref(Model model, Set<? extends Xref> xrefs,
			Class<? extends XReferrable> clazz) {
		Set<String> toReturn = new HashSet<String>();
		
		for (Xref xref : xrefs) {			
			// Find the corresponding persistent Xref by ID.
			
			// - generate URI from xref properties the same way it's done
			// during the cpath2 warehouse data import; it takes care to
			// resolve official db synonyms to primary names (using Miriam registry);
			// ignore 'idVersion', i.e., set it null (TODO think of uniprot isoforms later)
			if(xref.getDb() == null || xref.getId() == null) {
				log.warn("getByXref: " + xref + " db or id is null! Skipping.");
				continue;
			}
			String xurn = Normalizer.generateURIForXref(xref.getDb(), 
				xref.getId(), null, (Class<? extends Xref>) xref.getModelInterface());
			
			// now try to get it from the warehouse
			Xref x = (Xref) model.getByID(xurn);
			if (x != null) {
				// collect owners's ids (of requested type only)
				for (XReferrable xr : x.getXrefOf()) {
					if (clazz.isInstance(xr)) {
						toReturn.add(xr.getRDFId());
					}
				}
			} else {
				if(log.isDebugEnabled())
					log.debug("getByXref: using normalized ID:" + xurn 
					+ " " + "no matching xref found for: " +
					xref + " - " + xref.getRDFId() + ". Skipping.");
			}
		}
		
		return toReturn;
	}

}
