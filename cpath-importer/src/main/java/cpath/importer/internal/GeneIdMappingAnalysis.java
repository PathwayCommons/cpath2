/**
 * 
 */
package cpath.importer.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.PublicationXref;
import org.biopax.paxtools.model.level3.Xref;

import cpath.dao.Analysis;

/**
 * This is a package-private utility class, designed 
 * to be used with cpath2 BioPAX data warehouse
 * (run in a transaction) in order to create the 
 * gene/protein id-mapping table from  
 * analyzing all unification and relationship 
 * xrefs of the warehouse protein references.
 * 
 * @author rodche
 *
 */
final class GeneIdMappingAnalysis implements Analysis {

	private static final Log log = LogFactory.getLog(GeneIdMappingAnalysis.class);
	
	final Map<String,String> idMap;
	final Map<String,Set<String>> ambiguousIdMap;
	
	/**
	 * Constructor.
	 * 
	 */
	GeneIdMappingAnalysis() {
		this.idMap = new HashMap<String, String>();
		this.ambiguousIdMap = new HashMap<String,Set<String>>();
	}
	
	
	/**
	 * {@inheritDoc}
	 *
	 */
	@Override
	public Set<BioPAXElement> execute(Model model) {
		
		// for each ER, using xrefs, map other identifiers to the primary accession
		for(ProteinReference pr : model.getObjects(ProteinReference.class)) 
		{	
			//extract the primary id from the standard (identifiers.org) URI
			final String ac = pr.getRDFId().substring(pr.getRDFId().lastIndexOf('/')+1);
			
			for(Xref x : pr.getXref()) {
				// By design (warehouse), there are unif. and rel. xrefs added 
				// by the Uniprot Converter, and we will uses those, 
				// skipping publication and illegal xrefs:
				if(!(x instanceof PublicationXref) && x.getDb() != null) {
					String id = x.getId();					
					//ban an identifier associated with several proteins
					if(ambiguousIdMap.containsKey(id)) {
						Set<String> ambiguous = ambiguousIdMap.get(id);
						ambiguous.add(ac);
						log.debug("excluded " + id + ": maps to " + ambiguous);
					} else if(idMap.containsKey(id) && !idMap.get(id).equals(ac)) {					
						Set<String> ambiguous = new HashSet<String>(2);
						ambiguous.add(idMap.get(id));
						ambiguous.add(ac);
						ambiguousIdMap.put(id, ambiguous);
						idMap.remove(id); //exclude this id
						log.debug("excluded " + id + ": maps to " + ambiguous);
					} else {
						idMap.put(id, ac);
					}
				}
			}
		}

		return null; //no return value required
	}


	/**
	 * Unmodifiable id-mapping map
	 * (identifier -> UniProt accession)
	 * to be generated during the {@link Analysis} execute.
	 * 
	 * @return
	 */
	public Map<String, String> getIdMap() {
		return Collections.unmodifiableMap(idMap);
	}

	/**
	 * Unmodifiable map that contains
	 * all ambiguous id mappings.
	 * to be generated during the {@link Analysis} execute.
	 * 
	 * @return
	 */
	public Map<String, Set<String>> getAmbiguousIdMap() {
		return Collections.unmodifiableMap(ambiguousIdMap);
	}
	
}
