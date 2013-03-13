/**
 * 
 */
package cpath.importer.internal;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.PublicationXref;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.paxtools.util.ClassFilterSet;

import cpath.dao.Analysis;
import cpath.dao.PaxtoolsDAO;
import cpath.warehouse.WarehouseDAO;

/**
 * This is a package-private utility class designed 
 * to be used with a cpath2 BioPAX data Warehouse
 * (i.e by {@link PaxtoolsDAO#runAnalysis(Analysis, Object...)}
 * method) in order to create or update the id-mapping tables by
 * analyzing all unification and relationship xrefs of the warehouse
 * small molecule - and protein references.
 * 
 * @author rodche
 *
 */
final class WarehouseBasedIdMapping implements Analysis {

	private static final Log log = LogFactory.getLog(WarehouseBasedIdMapping.class);
	
	private final Map<String,String> geneIdMap;
	private final Map<String,String> chemIdMap;

	
	/**
	 * Constructor.
	 * 
	 * @param geneIdMap an empty Map<String,String> to store gene/protein id-mappings
	 * @param chemIdMap an empty Map<String,String> to store chemicals id-mappings
	 * 
	 * @throws IllegalArgumentException when null is a parameter's value
	 */
	WarehouseBasedIdMapping(Map<String,String> geneIdMap, Map<String,String> chemIdMap) {
		if(geneIdMap == null)
			throw new IllegalArgumentException("geneIdMap is null");
		if(chemIdMap == null)
			throw new IllegalArgumentException("chemIdMap is null");
		if(!geneIdMap.isEmpty())
			log.warn("geneIdMap is not empty");
		if(!geneIdMap.isEmpty())
			log.warn("geneIdMap is not empty");
		
		this.geneIdMap = geneIdMap;
		this.chemIdMap = chemIdMap;
	}
	
	
	/**
	 * {@inheritDoc}
	 *
	 * @param model - in production, it is expected to be an instance of 
	 * {@link WarehouseDAO}, {@link PaxtoolsDAO} (cpath2 warehouse).
	 */
	@Override
	public Set<BioPAXElement> execute(Model model, Object... args) {
		final Set<String> genesExclude = new HashSet<String>();
		final Set<String> chemsExclude = new HashSet<String>();
		
		// for each ER, using xrefs, map other identifiers to the primary accession
		for(EntityReference er : model.getObjects(EntityReference.class)) 
		{	
			if(er instanceof ProteinReference) {
				// in this first pass, it skips for gene synonyms
				addProtMappingsFirstPass((ProteinReference) er, genesExclude);
			} else if(er instanceof SmallMoleculeReference) {
				// use only unification xrefs (chebi,pubchem,inchikey) for chemicals id-mapping
				addChemMappings((SmallMoleculeReference) er, chemsExclude);
			}
		}
		
		//second pass for PRs (adds unambiguous gene synonyms only)
		genesExclude.clear(); //have to start clean here!
		for(ProteinReference pr : model.getObjects(ProteinReference.class)) 
			addGeneSynonyms(pr, genesExclude);
		
		return null; //no return value required
	}

	
	/*
	 * Adds id-mapping for a protein (to the primary uniprot id);
	 * it skips gene synonyms (only gene names are used in this pass)
	 */
	private void addProtMappingsFirstPass(ProteinReference pr, Set<String> exclude) {
		
		//extract the primary id from the standard (identifiers.org) URI
		final String ac = pr.getRDFId().substring(pr.getRDFId().lastIndexOf('/')+1);
		
		for(Xref x : pr.getXref()) {
			//by (warehouse) design, there are various unif. and rel. xrefs added by the data converter
			// always skip publication xrefs and illegal xrefs
			if(!(x instanceof PublicationXref) && x.getDb() != null) {
				String id = x.getId();
				
				//detect and skip gene synonyms (a hack, depends on the uniprot converter impl.):
				//ignore gene names that are not in the 'name' property of the PR
				if("HGNC Symbol".equalsIgnoreCase(x.getDb())
					&& !pr.getName().contains(id)) {
					log.debug("skip synonym: " + id + " (maps to " + ac + ")");
					continue;
				}
								
				//ban an identifier associated with several different proteins
				if(exclude.contains(id)) { //skip
					log.info("already excluded: " + id);
				} else if(geneIdMap.containsKey(id) 
						&& !geneIdMap.get(id).equals(ac)) {
					
					log.info("excluding ambiguous identifier " + id + 
						" that maps at least to " + ac + " and " + geneIdMap.get(id));
					geneIdMap.remove(id);				
					exclude.add(id);
				} else {
					geneIdMap.put(id, ac);
				}
			}
		}		
	}	
	
	
	/*
	 * Adds only gene synonyms to the id-mapping table. 
	 * It carefully excludes gene synonyms clashing with each other or gene names
	 * (i.e., those that ambiguously map to different uniprot ACs) without
	 * discarding previously added map entries (- primary gene names to uniprot ACs mappings).
	 * 
	 * When this method is called for the first time (first PR in the warehouse),
	 * the 'processed' set must be empty!
	 * 
	 */
	private void addGeneSynonyms(ProteinReference pr, Set<String> processed) {
		
		//extract the primary id from the standard (identifiers.org) URI
		final String ac = pr.getRDFId().substring(pr.getRDFId().lastIndexOf('/')+1);
		
		Set<RelationshipXref> xrefs = 
			new ClassFilterSet<Xref, RelationshipXref>(pr.getXref(), RelationshipXref.class);
		
		for(RelationshipXref x : xrefs) {
			String id = x.getId();
			//do only for gene synonyms 
			//(detection depends on the uniprot converter impl.):
			if(x.getDb() != null 
				&& "HGNC Symbol".equalsIgnoreCase(x.getDb())
				&& !pr.getName().contains(id)) //synonyms are not among names
			{	
				if(geneIdMap.containsKey(id) && !geneIdMap.get(id).equals(ac)) {	
					final String mappedTo = geneIdMap.get(id);
					if(processed.contains(id)) {
						//safe to remove (won't delete any primary gene name mapping)
						log.info("excluding ambiguous synonym " + 
							id + " that maps at least to " + ac + " and " + mappedTo);
						geneIdMap.remove(id);
					} else {
						log.info("ignoring " + id + " to " + ac
							+ " mapping, for it was previously mapped to " +
							mappedTo + " as a preferred gene name");
					}
				} else {
					if(!processed.contains(id)) {
						//if not already processed
						geneIdMap.put(id, ac);
						processed.add(id);
					}
				}
			}
		}		
	}
	
	
	/*
	 * Add id-mapping (to primary chebi id) for a molecule, by unification xrefs
	 */
	private void addChemMappings(SmallMoleculeReference smr, Set<String> exclude) {
		Set<UnificationXref> xrefs = 
			new ClassFilterSet<Xref, UnificationXref>(smr.getXref(), UnificationXref.class);
		
		//extract the primary id from the standard (identifiers.org) URI
		final String ac = smr.getRDFId().substring(smr.getRDFId().lastIndexOf('/')+1);
		
		for(Xref x : xrefs) {
			if(x.getDb() != null) {
				String id = x.getId();
				//ban an identifier associated with several different proteins
				if(exclude.contains(id)) {
					log.info("already excluded: " + id);
				} else if(chemIdMap.containsKey(id) && !chemIdMap.get(id).equals(ac)) {
					
					log.info("excluding ambiguous identifier " + id + 
						" that maps at least to " + ac + 
						" and " + chemIdMap.get(id));
					chemIdMap.remove(id);					
					exclude.add(id);
				} else {
					chemIdMap.put(id, ac);
				}
			}
		}		
	}	
}
