/**
 ** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
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
import cpath.dao.CPathUtils;
import cpath.jpa.MappingsRepository;

import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.util.Filter;
import org.biopax.paxtools.controller.*;
import org.biopax.validator.utils.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Semantic merge of a normalized BioPAX model into the target model, 
 * using cpath2 BioPAX data warehouse and id-mapping.
 * 
 * Note: this class is probably not thread-safe (so,
 * create a new instance for each execution).
 * 
 */
public class MergerAnalysis implements Analysis {

    private static final Logger log = LoggerFactory.getLogger(MergerAnalysis.class);

	private final Model source;
	private final String description;
	private final MappingsRepository mappingsRepository;
	private final String xmlBase;
	private final SimpleMerger simpleMerger;
	
	public MergerAnalysis(String description, Model source, 
			MappingsRepository mappingsRepository, String xmlBase) 
	{
		this.description = description;
		this.source = source;
		this.mappingsRepository = mappingsRepository;
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
	 * @param targetModel - target BioPAX Model (where to finally merge new data).
	 * @throws ClassCastException
	 */
	@Override
	public void execute(final Model targetModel) {	
		
		final String srcModelInfo = "source: " + description;		
		/* 
		 * Replace not normalized (during 'premerge') URIs in the source model 
		 * with generated new ones (also add a bp:comment about original URIs)
		 */
		log.info("Assigning new URIs (xml:base=" + xmlBase + 
				"*) to not normalized BioPAX elements (" + 
				srcModelInfo + ", xml:base=" + source.getXmlBase() + ")...");
		//wrap source.getObjects() in a new set to avoid concurrent modif. excep.
		for(BioPAXElement bpe : new HashSet<BioPAXElement>(source.getObjects())) {
			String currUri = bpe.getRDFId();
			
			// skip already normalized
			if(currUri.startsWith(xmlBase) || currUri.startsWith("http://identifiers.org/")) 
				continue; 
			
			// Generate new consistent URI for not generated not previously normalized objects:
			String newRDFId = Normalizer.uri(xmlBase, null, currUri, bpe.getModelInterface());
			// Replace URI
			CPathUtils.replaceID(source, bpe, newRDFId);
			// save original URI in comments
			if(bpe instanceof Level3Element) //though it's always true (by current design)
				((Level3Element) bpe).addComment("Original URI " + currUri);
		}
		
		// The following hack can improve graph queries and full-text search relevance
		// for generic and poorly defined physical entities (e.g., those lacking entity reference)
		log.info("Adding canonical UniProt/ChEBI " +
				"relationship xrefs to physical entities using their existing xrefs " +
				"and id-mapping (" + srcModelInfo + ")...");		
		/* 
		 * Using existing xrefs and id-mapping, add primary uniprot/chebi RelationshipXref 
		 * to all simple PE (SM, Protein, Dna, Rna,..) and Gene if possible (skip complexes).
		 * This might eventually result in mutually exclusive identifiers, 
		 * but we'll keep those and just log a warning for future (data) fix;
		 * - not a big deal as long as we do not merge data based on these new xrefs,
		 * but just index/search/query (this especially helps 
		 * when no entity references defined for a molecule).
		 */		
		for(Entity pe : new HashSet<Entity>(source.getObjects(Entity.class))) 
		{
			if(pe instanceof PhysicalEntity) {
				if(pe instanceof SimplePhysicalEntity) {
					if(pe instanceof SmallMolecule) {
						addCanonicalRelXrefs((SmallMolecule) pe, "CHEBI");
					} else {
						// for Protein, Dna, DnaRegion, Rna*...
						addCanonicalRelXrefs((PhysicalEntity) pe, "UNIPROT");
					}						
				} else if(pe instanceof Complex) {
					continue; // skip complexes
				} else {
					// do for base PEs
					addCanonicalRelXrefs((PhysicalEntity) pe, "UNIPROT");
					addCanonicalRelXrefs((PhysicalEntity) pe, "CHEBI");
				}
			} else if(pe instanceof Gene) {
				addCanonicalRelXrefs(pe, "UNIPROT");
			}
		}
			
		// find matching utility class elements in the target DB
		// (use a temporary in-memory model to merge/reuse existing objects)
		final Model mem = BioPAXLevel.L3.getDefaultFactory().createModel();
		mem.setXmlBase(xmlBase);
		log.info("Searching in the Warehouse for equivalent utility class elements ("+srcModelInfo+")...");
		final Map<UtilityClass, UtilityClass> replacements = new HashMap<UtilityClass, UtilityClass>();
		
		// match some UtilityClass objects to existing ones (previously imported, warehouse)
		for (UtilityClass bpe: new HashSet<UtilityClass>(source.getObjects(UtilityClass.class))) 
		{
			UtilityClass replacement = null;
			
			// Find the best replacement ER:
			if (bpe instanceof ProteinReference) {
				replacement = findOrCreateProteinReference((ProteinReference)bpe, targetModel);
			} 
			else if (bpe instanceof SmallMoleculeReference) {
				replacement = findOrCreateSmallMoleculeReference((SmallMoleculeReference)bpe, targetModel);
			} 
			else { //e.g., BioSource, CV, or Provenance - simply match by URI (no id-mapping or additional checks)
				replacement = (UtilityClass) targetModel.getByID(bpe.getRDFId());
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
		log.info("Replacing objects ("+srcModelInfo+")...");	
		ModelUtils.replace(source, replacements);

		//in addition, explicitly remove old (replaced) objects from the model
		for(UtilityClass old : replacements.keySet()) {
			source.remove(old);
		}
		
		// cleaning up dangling objects
		log.info("Removing dangling objects ("+srcModelInfo+")...");
		final Set<BioPAXElement> removed = ModelUtils
			.removeObjectsIfDangling(source, UtilityClass.class);
		
		//important
		removed.addAll(replacements.keySet());
		
		/* the assertion wouldn't hold after removeObjectsIfDangling call
		 * if not old (replaced) objects were then explicitely removed: 
		 * since BioPAXElementImpl.equals and hashCode were overridden,
		 * removeObjectsIfDangling behaves differently... */
		assert removed.containsAll(replacements.keySet()) 
			: "not all replaced actually became dangling and were removed";	
		
		// post-fix
		log.info("Migrate some properties, such as original entityFeature and xref ("+srcModelInfo+")...");
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
		log.info("In-memory merging ("+srcModelInfo+")...");
		mem.merge(source);
		
		// fix dangling inverse properties (some objects there
		// in the source model might still refer to the removed ones)
		fixInverseProperties(removed);	
		
		log.info("Merging from the in-memory ( "+srcModelInfo+") into the persistent BioPAX DB/model...");
		// merge to the target model (insert new objects and update relationships)
		targetModel.merge(mem);
		
		log.info("Merge is done ("+srcModelInfo+").");			
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
	 * @param db map identifiers to (e.g., CHEBI, UNIPROT)
	 * @throws AssertionError when bpe is neither Gene nor PhysicalEntity
	 */
	private void addCanonicalRelXrefs(Named bpe, String db) 
	{
		if(!(bpe instanceof Gene || bpe instanceof PhysicalEntity))
			throw new AssertionError("Not Gene or PE: " + bpe);
			
		// map and generate/add xrefs
		Set<String> mappingSet = idMappingByXrefs(bpe, db, UnificationXref.class);
		addRelXref(bpe, db, mappingSet);
		
		mappingSet = idMappingByXrefs(bpe, db, RelationshipXref.class);
		addRelXref(bpe, db, mappingSet);
		
		//map by display and standard names
		mappingSet = mappingsRepository.map(null, bpe.getDisplayName(), db);
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
	 * @param target
	 * @param type
	 * @return the replacement object or null if none can found
	 */
	private ProteinReference findOrCreateProteinReference(final ProteinReference orig, final Model target) 
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
 
		// Otherwise, use id-mapping
		
		// if nothing's found in the db by original or normalized URI, 
		// 2) try id-mapping (to uniprot ac). 
		if (uri.startsWith(standardPrefix)) {
			String id = uri.substring(uri.lastIndexOf('/')+1);
			String db = null;	
			
			//a hack/shortcut for normalized PRs
			if(orig instanceof ProteinReference) {
				if(uri.toLowerCase().contains("uniprot.isoform")) {
					db = "uniprot isoform";
				} else if(uri.toLowerCase().contains("uniprot")) {
					db = "uniprot";
				} else if(uri.toLowerCase().contains("refseq")) {
					db = "refseq";	
				} else if(uri.toLowerCase().contains("kegg") && id.contains(":")) {
					db = "NCBI Gene"; //KEGG actually uses NCBI Gene (aka Entrez Gene)
				}
			}
			
			if(db == null) //then detect db by id matching in all xrefs
				db = dbById(id, orig.getXref());
			
			// do id-mapping
			Set<String> mp = mappingsRepository.map(db, id, "UNIPROT"); 			
			if(!mp.isEmpty()) {
				toReturn = (ProteinReference) getByIdsFromModel(mp, canonicalPrefix, target);
			}
		}
				
		// if yet nothing's found, 
		// 3) try using (already normalized) all Unification Xrefs and id-mapping (to uniprot ac). 
		if (toReturn == null) {
			Set<String> mappingSet = idMappingByXrefs(orig, "UNIPROT", UnificationXref.class);
			if(!mappingSet.isEmpty()) {
				toReturn = (ProteinReference) getByIdsFromModel(mappingSet, canonicalPrefix, target);		
			}
		}	
		
		// if nothing's found in the warehouse by URI and unif. xrefs, - 
		// 4) try relationship xrefs and id-mapping 
		if (toReturn == null) {
			Set<String> mappingSet = idMappingByXrefs(orig,"UNIPROT", RelationshipXref.class);
			if(!mappingSet.isEmpty()) {
				toReturn = (ProteinReference) getByIdsFromModel(mappingSet, canonicalPrefix, target);
			}	
		}
				
		//5) finally, map by display name (CALM_HUMAN, etc.)!
		if (toReturn == null) {
			Set<String> mp = mapByName(orig, "UNIPROT", target);		
			if(!mp.isEmpty()) {
				toReturn = (ProteinReference) getByIdsFromModel(mp, canonicalPrefix, target);
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
	 * @param mapTo
	 * @param xrefType
	 * @return
	 */
	private Set<String> idMappingByXrefs(final XReferrable orig,
			String mapTo, final Class<? extends Xref> xrefType) 
	{
		final Set<String> mappedTo = new TreeSet<String>();
		
		for (Xref x : orig.getXref()) {
			if (xrefType.isInstance(x)) {
				Set<String> mp = mappingsRepository.map(x.getDb(), x.getId(), mapTo);
				if (!mp.isEmpty()) {
					mappedTo.addAll(mp);
					if(mp.size() > 1) //one xref maps to several primary ACs
						log.warn("idMappingByXrefs: ambiguous; id: " +
								x.getId() + " maps to: " + mp);
				}
			}
		}
		
		if(mappedTo.size() > 1) // xrefs map to different primary ACs
			log.warn("idMappingByXrefs: ambiguous; " + orig.getRDFId() +
				" (using its " + xrefType.getSimpleName() + "s) maps to: " 
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
	 * @param target
	 * @param type
	 * @return the replacement object or null if none can found
	 */
	private SmallMoleculeReference findOrCreateSmallMoleculeReference(final SmallMoleculeReference orig, final Model target) 
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
		// 2) try id-mapping (to chebi ac). 
		if (uri.startsWith(standardPrefix)) {
			String id = uri.substring(uri.lastIndexOf('/')+1);	
			String db = dbById(id, orig.getXref()); //find by id			
			Set<String> mp = mappingsRepository.map(db, id, "CHEBI");
			if(!mp.isEmpty()) {
				toReturn = (SmallMoleculeReference) getByIdsFromModel(mp, canonicalPrefix, target);
			}
		}
				
		// if yet nothing's found, 
		// 3) try using (already normalized) all Unification Xrefs and id-mapping (to primary chebi). 
		if (toReturn == null) {
			Set<String> mappingSet = idMappingByXrefs(orig, "CHEBI", UnificationXref.class);
			if(!mappingSet.isEmpty()) {
				toReturn = (SmallMoleculeReference) getByIdsFromModel(mappingSet, canonicalPrefix, target);
			}	
		}	
		
		// if nothing's found in the db by URI or unif. xrefs, - 
		// 4) try using relationship xrefs and id-mapping. 
		if (toReturn == null) {
			Set<String> mappingSet = idMappingByXrefs(orig, "CHEBI", RelationshipXref.class);
			if(!mappingSet.isEmpty()) {
				//not needed if we replace the ER
//				addRelXref(orig, "CHEBI", mappingSet);
				toReturn = (SmallMoleculeReference) getByIdsFromModel(mappingSet, canonicalPrefix, target);
			}	
		}
		
		//5) map by display and standard name (e..g, 'ethanol') to ChEBI ID
		if (toReturn == null) {		
			Set<String> mp = mapByName(orig, "CHEBI", target);
			if(!mp.isEmpty()) {
				toReturn = (SmallMoleculeReference) getByIdsFromModel(mp, canonicalPrefix, target);
			}
		}
		
		return toReturn;
	}


	@SuppressWarnings("unchecked")
	private Set<String> mapByName(EntityReference orig, String toDb, Model target) {
		Set<String> mp = new TreeSet<String>();
		
		String name = orig.getDisplayName();
		if(name != null)
			mp.addAll(mappingsRepository.map(null, name.toLowerCase(), toDb));

		name = orig.getStandardName();
		if(name != null)
			mp.addAll(mappingsRepository.map(null, name.toLowerCase(), toDb));
			
		if(mp.isEmpty() && //and only for PRs and SMRs -
				(orig instanceof ProteinReference || orig instanceof SmallMoleculeReference)) 
		{
			//To find a warehouse SMR(s) with exactly the same name (case-insensitive).			
			//first, collect all orig names, lowercase
			Set<String> origNames = new HashSet<String>();
			for(String n : orig.getName()) 
				origNames.add(n.toLowerCase());
			//then, check existing entity references of the same as orig's class
			for(EntityReference er : target
				.getObjects((Class<? extends EntityReference>) orig.getModelInterface())) 
			{
				for(String s : er.getName()) {
					if(origNames.contains(s.toLowerCase())) {
						//extract the accession from URI, add
						mp.add(CPathUtils.idfromNormalizedUri(er.getRDFId()));
						break;
					}
				}
			}
		}
		
		return mp;
	}


	private EntityReference getByIdsFromModel(Set<String> mp, String uriPrefix,
			Model target) 
	{
		String uri = uriPrefix + mp.iterator().next();
		EntityReference toReturn = (EntityReference) target.getByID(uri);
		if(toReturn!= null && mp.size() > 1)
			log.warn("ambiguous id-mapping using URI; picked " + uri + " of " + mp);
		
		return toReturn;
	}
	
}