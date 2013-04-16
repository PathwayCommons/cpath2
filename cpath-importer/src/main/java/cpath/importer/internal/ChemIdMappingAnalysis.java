/**
 * 
 */
package cpath.importer.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.paxtools.util.ClassFilterSet;

import cpath.dao.Analysis;

/**
 * This is a package-private utility class, designed 
 * to be used with cpath2 BioPAX data warehouse
 * (run in a transaction) in order to create the 
 * chemicals id-mapping table from  
 * analyzing all unification and relationship 
 * xrefs of the warehouse small molecule references.
 * 
 * @author rodche
 *
 */
final class ChemIdMappingAnalysis implements Analysis {

	private static final Log log = LogFactory.getLog(ChemIdMappingAnalysis.class);
		
	final Map<String,String> idMap;
	final Map<String,Set<String>> ambiguousIdMap;

	
	/**
	 * Constructor.
	 */
	ChemIdMappingAnalysis() {
		this.idMap = new TreeMap<String, String>();
		this.ambiguousIdMap = new TreeMap<String,Set<String>>();
	}
	
	
	/**
	 * {@inheritDoc}
	 *
	 */
	@Override
	public Set<BioPAXElement> execute(Model model) {
		final Set<String> exclude = new HashSet<String>();
		
		// for each SmallMoleculeReference, using xrefs, map other identifiers to the primary accession
		for(SmallMoleculeReference smr : model.getObjects(SmallMoleculeReference.class)) 
		{	
			// By design (warehouse), only unif. xrefs added 
			// by the ChEBI Converter are safe to use for id-mapping
			Set<UnificationXref> xrefs = 
				new ClassFilterSet<Xref, UnificationXref>(smr.getXref(), UnificationXref.class);
				
			//extract the primary id from the standard (identifiers.org) URI
			final String ac = smr.getRDFId().substring(smr.getRDFId().lastIndexOf('/')+1);
				
			for(UnificationXref x : xrefs) {
				if(x.getDb() != null) {
					String id = x.getId();
					//ban an identifier associated with several different proteins
					if(exclude.contains(id)) {
						Set<String> ambiguous = ambiguousIdMap.get(id);
						ambiguous.add(ac);
						log.debug("excluded " + id + ": maps to " + ambiguous);
					} else if(idMap.containsKey(id) && !idMap.get(id).equals(ac)) {
						Set<String> ambiguous = new HashSet<String>(2);
						ambiguous.add(idMap.get(id));
						ambiguous.add(ac);
						ambiguousIdMap.put(id, ambiguous);
						idMap.remove(id);
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
	 * (identifier -> CHEBI accession)
	 * to be generated during the {@link Analysis} execute.
	 * 
	 * @return
	 */
	public Map<String, String> getIdMap() {
		return Collections.unmodifiableMap(idMap);
	}

	/**
	 * Unmodifiable map that contains
	 * all ambiguous id mappings
	 * to be created during the {@link Analysis} execute.
	 * 
	 * @return
	 */
	public Map<String, Set<String>> getAmbiguousIdMap() {
		return Collections.unmodifiableMap(ambiguousIdMap);
	}
	
}
