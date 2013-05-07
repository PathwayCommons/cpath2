/**
 ** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either pathwayDataVersion 2.1 of the License, or
 ** any later pathwayDataVersion.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** both UofT and MSKCC have no obligations to provide maintenance, 
 ** support, updates, enhancements or modifications.  In no event shall
 ** UofT or MSKCC be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** UofT or MSKCC have been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this software; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA;
 ** or find it at http://www.fsf.org/ or http://www.gnu.org.
 **/

package cpath.importer.internal;

import cpath.dao.Analysis;
import cpath.dao.MetadataDAO;
import cpath.warehouse.beans.*;

import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.util.Filter;
import org.biopax.paxtools.controller.*;
import org.biopax.validator.utils.Normalizer;
import org.springframework.transaction.annotation.Transactional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * Semantic merge of a normalized BioPAX model into the target model, 
 * using cpath2 BioPAX data warehouse and id-mapping.
 * 
 * Note: this class is probably not thread-safe (so,
 * create a new instance for each execution).
 * 
 */
@Transactional
class MergerAnalysis implements Analysis {

    private static final Log log = LogFactory.getLog(MergerAnalysis.class);

	private final Model source;
	private final String description;
	private final MetadataDAO metadataDAO;
	private final String xmlBase;
	private final SimpleMerger simpleMerger;
	
    private Model target;
    private Model mem;
	
	
	MergerAnalysis(String description, Model source, 
			MetadataDAO metadataDAO, String xmlBase) 
	{
		this.description = description;
		this.source = source;
		this.metadataDAO = metadataDAO;
		this.xmlBase = xmlBase;
		this.simpleMerger = new SimpleMerger(SimpleEditorMap.L3);
	}
	
	
	/**
	 * {@inheritDoc}
	 * 
	 * Merges a new pathway model into persistent main model: 
	 * inserts new objects and updates object properties
	 * (and should not break inverse properties).
	 * It re-uses previously merged UtilityClass objects, such as 
	 * canonical EntityReferences, CVs (warehouse data), to replace 
	 * equivalent ones in the original pathway data.
	 * 
	 * @param model - target BioPAX Model.
	 * @throws ClassCastException
	 */
	@Override
	public void execute(Model model) {		
		// The following hack can improve graph queries and full-text search relevance
		// for generic and poorly defined physical entities (e.g., those lacking entity reference)
		log.info("Generating canonical UniProt/ChEBI " +
				"rel. xrefs for physical entities using existing xrefs " +
				"and id-mapping...");		
		/* Using xrefs and id-mapping, add primary uniprot/chebi RelationshipXref 
		 * to all simple PE (SM, Protein, Dna,..) and Gene if possible (skip complexes).
		 * This might eventually result in mutually exclusive identifiers, 
		 * but we'll keep those and just log a warning for future (data) fix, -
		 * for this is not a big deal as long as we are not merging data 
		 * but only use in search/query.
		 */		
		for(Entity pe : new HashSet<Entity>(source.getObjects(Entity.class))) 
		{
			if(pe instanceof PhysicalEntity) {
				if(pe instanceof SimplePhysicalEntity) {
					if(pe instanceof SmallMolecule) {
						addCanonicalRelXrefs((SmallMolecule) pe, Mapping.Type.CHEBI);
					} else {
						// for Protein, Dna, DnaRegion, Rna*...
						addCanonicalRelXrefs((PhysicalEntity) pe, Mapping.Type.UNIPROT);
					}						
				} else if(pe instanceof Complex) {
					continue; // skip complexes
				} else {
					// do for base PEs
					addCanonicalRelXrefs((PhysicalEntity) pe, Mapping.Type.UNIPROT);
					addCanonicalRelXrefs((PhysicalEntity) pe, Mapping.Type.CHEBI);
				}
			} else if(pe instanceof Gene) {
				addCanonicalRelXrefs((XReferrable) pe, Mapping.Type.UNIPROT);
			}
		}
		
		//set the target and in-memory (temp.) models to be used by private methods	
		target = model;
		
		// find matching utility class elements in the target DB
		// (use a temporary in-memory model to merge/reuse existing objects)
		mem = BioPAXLevel.L3.getDefaultFactory().createModel();
		mem.setXmlBase(xmlBase);
		log.info("Source model: " + description + ", xml:base=" + source.getXmlBase() 
			+ ". Looking for existing equivalent utility class elements...");
		final Map<UtilityClass, UtilityClass> replacements = new HashMap<UtilityClass, UtilityClass>();
		
		// match some utility class objects to existing ones (previously imported)
		for (UtilityClass bpe: new HashSet<UtilityClass>(source.getObjects(UtilityClass.class))) 
		{
			UtilityClass replacement = null;
			
			// Find the best replacement ER:
			if (bpe instanceof ProteinReference) {
				replacement = findOrCreateProteinReference((ProteinReference)bpe);
			} 
			else if (bpe instanceof SmallMoleculeReference) {
				replacement = findOrCreateSmallMoleculeReference((SmallMoleculeReference)bpe);
			} 
			else { //e.g., BioSource, CV, or Provenance - simply match by URI (no id-mapping or additional checks)
				replacement = (UtilityClass) target.getByID(bpe.getRDFId());
			}
				
			if (replacement != null) {
				UtilityClass r = (UtilityClass) mem.getByID(replacement.getRDFId());
				if(r != null) //re-use
					replacement = r;
				else //merge (incl. all children) in memory
					simpleMerger.merge(mem, replacement);
					
				replacements.put(bpe, replacement);
			}
		}

		// Replace objects in the source model
		log.info("Replacing objects...");	
		ModelUtils.replace(source, replacements);
	
		// cleaning up	
		log.info("Removing dangling objects...");
		final Set<BioPAXElement> removed = ModelUtils
			.removeObjectsIfDangling(source, UtilityClass.class);
	
		assert removed.containsAll(replacements.keySet()) 
			: "not all replaced actually became dangling and were removed";		
//		if(log.isDebugEnabled()) {
//		  for(UtilityClass ruc : replacements.keySet()) {
//			if(source.contains(ruc)) {
//				log.debug("replaced object was not removed from the model : " + ruc.getRDFId());
//				if(ruc instanceof EntityReference)
//					log.debug("is ER of : " + ((EntityReference)ruc).getEntityReferenceOf());
//				if(ruc instanceof Xref)
//					log.debug("is xref of : " + ((Xref)ruc).getXrefOf());
//			}
//		  }	
//		}
		
		// post-fix
		log.info("Migrate some properties (original entityFeature and xref)...");
		for (UtilityClass old : replacements.keySet()) {
			if(old instanceof EntityReference) {
				for (EntityFeature ef : new HashSet<EntityFeature>(
						((EntityReference) old).getEntityFeature())) {
					// move entity features of the replaced ER to the new one
					((EntityReference) old).removeEntityFeature(ef); //(this is to avoid paxtools warnings)			
					((EntityReference) replacements.get(old)).addEntityFeature(ef);
					if(!mem.containsID(ef.getRDFId()))
						mem.add(ef);	
				}
			}
				
			// move PublicationXrefs and RelationshipXrefs (otherwise we lost some original xrefs)
			if(old instanceof XReferrable) {
				for(Xref x : new HashSet<Xref>(((XReferrable) old).getXref())) {
					if(!(x instanceof UnificationXref)) {
						((XReferrable) old).removeXref(x);
						//add x to the replacement (duplicate URI xref will be quietly ignored)
						((XReferrable) replacements.get(old)).addXref(x);
						if(!mem.containsID(x.getRDFId()))
							mem.add(x);					
					}
				}
			}
		}	
								
		// merge source with the in-memory model
		// (makes the model complete, self-integral)
		log.info("In-memory merging...");
		mem.merge(source);
		
		// fix dangling inverse properties (some objects there
		// in the source model might still refer to the removed ones)
		fixInverseProperties(removed);	
		
		log.info("Merging (insert/update) into the DB...");
		// merge to the target model (insert new objects and update relationships)
		target.merge(mem);
		
		log.info("Merge is done; flushing...");			
	}

		
	/*
	 * Clears all object properties of old (replaced) objects
	 * before the source and target models are merged, because 
	 * some of their child objects that are also used by other 
	 * biopax elements (to stay) in the model can still refer 
	 * to the old ones via inverse object properties (xrefOf, etc.)
	 *  
	 * @param removed
	 */
	private void fixInverseProperties(final Set<BioPAXElement> removed) {		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Traverser traverser = new Traverser(SimpleEditorMap.L3, 
			new Visitor() {			
				@Override
				public void visit(BioPAXElement bpe, Object v, Model model, PropertyEditor editor) {
					editor.removeValueFromBean(v, bpe);
				}
			}, 
			new Filter<PropertyEditor>() {
				@Override
				public boolean filter(PropertyEditor object) {
					return object instanceof ObjectPropertyEditor;
				}
			}
		);
		
		for(BioPAXElement element : removed)
			traverser.traverse(element, null);
	}


	/**
	 * Performs id-mapping from the  
	 * unification and relationship xrefs 
	 * of a physical entity or gene to the primary/canonical
	 * id (uniprot or chebi), creates new relationship xrefs,
	 * and adds them back to the entity.
	 * 
	 * @param bpe a {@link Gene} or {@link PhysicalEntity}
	 * @param mappType
	 * @throws AssertionError when bpe is neither Gene nor PhysicalEntity
	 */
	private void addCanonicalRelXrefs(XReferrable bpe, Mapping.Type mappType) 
	{
		if(!(bpe instanceof Gene || bpe instanceof PhysicalEntity))
			throw new AssertionError("Not Gene or PE: " + bpe);
		
		String db = (Mapping.Type.CHEBI ==  mappType) ?  "chebi" : "uniprot";
			
		// map and generate/add xrefs
		Set<String> mappingSet = idMappingByXrefs(bpe, mappType, UnificationXref.class);
		addRelXref(bpe, db, mappingSet);
		
		mappingSet = idMappingByXrefs(bpe, mappType, RelationshipXref.class);
		addRelXref(bpe, db, mappingSet);
	}


	/**
	 * Finds or creates relationship xrefs
	 * using id-mapping results;
	 * adds them to the object and model.
	 * 
	 * @param bpe a gene, physical entity or entity reference
	 * @param db database name for all (primary/canonical) xrefs; 'uniprot' or 'chebi'
	 * @param mappingSet
	 * @throws AssertionError when bpe is neither Gene nor PhysicalEntity nor EntityReference
	 */
	private void addRelXref(XReferrable bpe, String db, Set<String> mappingSet) 
	{	
		if(!(bpe instanceof Gene || bpe instanceof PhysicalEntity || bpe instanceof EntityReference))
			throw new AssertionError("Not Gene or PE: " + bpe);
		
		for(String ac : mappingSet) {
			// find or create
			String rxUri = Normalizer.uri(xmlBase, db, ac, RelationshipXref.class);
			RelationshipXref rx = (RelationshipXref) source.getByID(rxUri);	
			if(rx == null) { 	
				rx = source.addNew(RelationshipXref.class, rxUri);
				rx.setDb(db);
				rx.setId(ac);
			}
			bpe.addXref(rx);
		}
	}


	/**
	 * Finds previously created or generates (searching in the db) 
	 * a new {@link ProteinReference} BioPAX element that is equivalent 
	 * to the original one and has standard URI and properties, 
	 * which allows to simply merge it with other semantically equivalent ones, by ID (URI).
	 * 
	 * @param orig
	 * @param type
	 * @return the replacement object or null if none can found
	 */
	private ProteinReference findOrCreateProteinReference(ProteinReference orig) 
	{				
		ProteinReference toReturn = null;	
		
		final String standardPrefix = "http://identifiers.org/";
		final String canonicalPrefix = standardPrefix + "uniprot/";
		
		String uri = orig.getRDFId();
		
		// 1) try to match an existing PR by URI
		if(uri.startsWith(canonicalPrefix)) {
			toReturn = (ProteinReference) target.getByID(uri);
			if(toReturn != null)
				return toReturn;
		}
 
		// otherwise - try more - with id-mapping
		
		/* getting here also means biopax normalization was
		 * not quite successful, due to lack of uniprot unif. xref, 
		 * having geneId/ensemble (relationship xrefs),
		 * or using a non standard gene/protein db names.
		 */
		
		// if nothing's found in the db by original or normalized URI, 
		// 2) try id-mapping (to uniprot ac). 
		if (uri.startsWith(standardPrefix)) {
			String id = uri.substring(uri.lastIndexOf('/')+1);
			String db = null;	
			
			//a hack for proteins (with suboptimal xrefs...)
			if(orig instanceof ProteinReference) {
				if(uri.contains("uniprot.isoform")) {
					db = "uniprot isoform";
				} else if(uri.contains("refseq")) {
					db = "refseq";
				} else if(uri.contains("kegg") && id.contains(":")) {
					db = "kegg genes"; //uses entrez gene ids
				}
			}
			
			if(db == null)
				db = dbById(id, orig.getXref()); //find by id
			
			// do id-mapping
			Set<String> mp = metadataDAO.mapIdentifier(id, Mapping.Type.UNIPROT, db); 
			if(!mp.isEmpty()) {
				//TODO log if > 1 ac
				id = mp.iterator().next();
				toReturn = (ProteinReference) target.getByID(canonicalPrefix + id);
			}
		}
				
		// if yet nothing's found, 
		// 3) try using (already normalized) all Unification Xrefs and id-mapping (to uniprot ac). 
		if (toReturn == null) {
			Set<String> mappingSet = idMappingByXrefs(orig, Mapping.Type.UNIPROT, UnificationXref.class);
			if(!mappingSet.isEmpty()) {
				// use only the first result (a warning logged already)
				toReturn = (ProteinReference) target
					.getByID(canonicalPrefix + mappingSet.iterator().next());			
			}
		}	
		
		// if nothing's found in the warehouse by URI and unif. xrefs, - 
		// 4) try relationship xrefs and id-mapping 
		if (toReturn == null) {
			Set<String> mappingSet = idMappingByXrefs(orig, Mapping.Type.UNIPROT, RelationshipXref.class);
			if(!mappingSet.isEmpty()) {
				// use only the first result (a warning logged already)
				toReturn = (ProteinReference) target
					.getByID(canonicalPrefix + mappingSet.iterator().next());
			}	
		}
		
		return toReturn;
	}
	
	
	/**
	 * Using id-mapping and specific type xrefs 
	 * of the object, finds primary identifier(s);
	 * normally, only one or none is returned 
	 * (you decide what to do when there're many;
	 * this flags error in the biopax model) 
	 * 
	 * @param orig
	 * @param mappClass
	 * @param xrefType
	 * @return
	 */
	private Set<String> idMappingByXrefs(final XReferrable orig,
			Mapping.Type mappClass, final Class<? extends Xref> xrefType) 
	{
		 //collect unique mapping results (only one is normally expected)
		final Set<String> mappedTo = new HashSet<String>();
		
		for (Xref x : orig.getXref()) {
			if (xrefType.isInstance(x)) {
				Set<String> mp = metadataDAO.mapIdentifier(x.getId(), mappClass, x.getDb());
				if (!mp.isEmpty()) {
					mappedTo.add(mp.iterator().next());
					//TODO log warn when >1 ac
				}
			}
		}
		
		if(mappedTo.size() > 1)
			log.warn("Ambiguous id-mapping of " + orig.getRDFId() + 
				"; using " + xrefType.getSimpleName() + "s got: " 
					+ mappedTo);

		return mappedTo;
	}


	/**
	 * Finds a {@link UnificationXref} by id 
	 * and returns its db value or null.
	 * 
	 * @param id
	 * @param xref
	 * @return
	 */
	private String dbById(String id, Set<Xref> xref) {
		for(Xref x : xref)
			if(x instanceof UnificationXref)
				if(id.equals(x.getId()))
					return x.getDb();
		
		return null;
	}


	/**
	 * Finds previously created or generates (searching in the data warehouse) 
	 * a new {@link SmallMoleculeReference} BioPAX element that is equivalent 
	 * to the original one and has standard URI and properties, 
	 * which allows to simply merge it with other semantically equivalent ones, by ID (URI).
	 * 
	 * @param orig
	 * @param type
	 * @return the replacement object or null if none can found
	 */
	private SmallMoleculeReference findOrCreateSmallMoleculeReference(SmallMoleculeReference orig) 
	{				
		SmallMoleculeReference toReturn = null;	
		
		final String standardPrefix = "http://identifiers.org/";
		final String canonicalPrefix = standardPrefix + "chebi/";
		
		String uri = orig.getRDFId();
		
		// 1) try to re-use existing object
		if(uri.startsWith(canonicalPrefix)) {
			toReturn = (SmallMoleculeReference) target.getByID(uri);
			if(toReturn != null)
				return toReturn;
		}
 
		// otherwise - try more - with id-mapping
		
		/* getting here also means biopax normalization was 
		 * not quite successful, due to lack of chebi unif. xrefs, 
		 * having a pubchem/kegg (relationship) xref instead,
		 * or using a non standard chemical db name.
		 */
		
		// if nothing's found in the db by original or normalized URI, 
		// 2) try id-mapping (to uniprot ac). 
		if (uri.startsWith(standardPrefix)) {
			String id = uri.substring(uri.lastIndexOf('/')+1);	
			String db = dbById(id, orig.getXref()); //find by id
// can later optimize by using an in-memory map instead of DAO -
//			id = IdMappingFactory.suggest(db, id);
//			id = chemIdMap.get(id);	
			Set<String> mp = metadataDAO.mapIdentifier(id, Mapping.Type.CHEBI, db);
			if(!mp.isEmpty()) {
				//TODO log warn when > 1 ac
				id = mp.iterator().next();
				toReturn = (SmallMoleculeReference) target.getByID(canonicalPrefix + id);
			}
		}
				
		// if yet nothing's found, 
		// 3) try using (already normalized) all Unification Xrefs and id-mapping (to primary chebi). 
		if (toReturn == null) {
			Set<String> mappingSet = idMappingByXrefs(orig, Mapping.Type.CHEBI, UnificationXref.class);
			if(!mappingSet.isEmpty()) {
				// use only the first result (a warning logged already)
				toReturn = (SmallMoleculeReference) target
					.getByID(canonicalPrefix + mappingSet.iterator().next());
			}	
		}	
		
		// if nothing's found in the db by URI or unif. xrefs, - 
		// 4) try using relationship xrefs and id-mapping. 
		if (toReturn == null) {
			/* Not merging SMRs based on their rel. xrefs 
			 * (currently for molecules, we might have ambiguous xrefs generated from ChEBI SDF file),
			 * but we, at least, we can generate rel. xrefs to primary chebi id here.
			 */
			Set<String> mappingSet = idMappingByXrefs(orig, Mapping.Type.CHEBI, RelationshipXref.class);
			if(!mappingSet.isEmpty()) {
				//add the primary chebi rel.xrefs to this ER
				addRelXref(orig, "chebi", mappingSet);
			}	
		}
		
		return toReturn;
	}
	
}