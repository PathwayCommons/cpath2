package cpath.cleaner.internal;

// imports
import cpath.cleaner.Cleaner;

import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;

/**
 * Implementation of Cleaner interface for use when data
 * does not need to be cleaned.
 */
public class BaseCleanerImpl implements Cleaner {

	/**
	 * (non-Javadoc>
	 * @see cpath.cleaner.Cleaner#clean(java.lang.String)
	 */
	public String clean(final String pathwayData) {
		return pathwayData;
	}
	
	/**
	 * Given a paxtools model for an entire owl file
	 * returns the tax id for the organism.  This method 
	 * assumes that at least one pathway in model is annotated
	 * with a biosource (although all should be).  It also assumes
	 * that all pathways share the the same biosource.  With that in 
	 * mind, it returns the first tax id encountered while iterating 
	 * over each pathways biosource, else returns null.
	 * 
	 * @param model Model
	 * @return String
	 */
	protected String getTaxID(final Model model) {

		for (Pathway pathway : model.getObjects(Pathway.class)) {
			BioSource bioSource = pathway.getOrganism();
			if (bioSource != null) {
				for (Xref xref : bioSource.getXref()) {
					if (xref instanceof UnificationXref) {
						if (xref.getDb().contains("taxonomy")) {
							return xref.getId();
						}
					}
				}
			}
		}
		
		// outta here
		return null;
	}
	
	/**
	 * Given a BioPAXElement, returns an RDF Id by
	 * apply the suffix '_X', where X is an integer to
	 * the current id.  X is derived by setting it to 1
	 * and incrementing it (if necessary) until it does
	 * not clash with an existing RDF Id.  "_" + taxID will
	 * be appended to rdf id to prevent clashes between across 
	 * species files supplied by same data provider.
	 * 
	 * @param model Model
	 * @param bpe BioPAXElement
	 * @param taxID String
	 * @return String
	 */
	@Deprecated
	protected String getRDFIdReplacement(final Model model, final BioPAXElement bpe, final String taxID) {
		
		int inc = 0;
		String toReturn = "";
		
		String currentID = bpe.getRDFId().toUpperCase();
		do {
			toReturn = currentID + "_" + ++inc + "_" + taxID;
		} while (model.containsID(toReturn));
		
		// outta here
		return toReturn;
	}
}
