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
import org.biopax.paxtools.controller.*;
import org.biopax.validator.utils.Normalizer;
import org.springframework.transaction.annotation.Transactional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * Semantic merge of a normalized BioPAX model into the target model, 
 * using cpath2 BioPAX data warehouse and id-mapping.
 */
@Transactional
class MergerAnalysis implements Analysis {

    private static final Log log = LogFactory.getLog(MergerAnalysis.class);

	private final Model source;
	
	private final String description;
    
	private final MetadataDAO metadataDAO;
    
    private final SimpleMerger simpleMerger;
	
    private final String xmlBase;
	
	
	MergerAnalysis(String description, Model source, 
			MetadataDAO metadataDAO, String xmlBase) 
	{
		this.description = description;
		this.source = source;
		this.metadataDAO = metadataDAO;
		this.simpleMerger = new SimpleMerger(SimpleEditorMap.L3);
		this.xmlBase = xmlBase;
	}
	
	
	/**
	 * {@inheritDoc}
	 * 
	 * Merges a new pathway model into persistent main model: 
	 * inserts new objects and updates object properties
	 * (and should not break inverse properties).
	 * It re-uses previously merged canonical UtilityClass objects 
	 * (e.g., EntityReference) to replace equivalent ones in the pathway data.
	 * 
	 * @param args - two parameters: a BioPAX Model and its description.
	 * 
	 * @throws ClassCastException
	 */
	@Override
	public Set<BioPAXElement> execute(Model model) {
		log.debug("execute: begin merging model " + description + 
			", xml:base=" + source.getXmlBase());		
		
		//We suppose, the pathwayModel is self-integral, 
		//i.e, - no external refs and implicit children
		//(this is almost for sure true if it's just came from a string/file)
		
		//Create a new in-memory "replacements" Model, 
		// to merge new things into this one first
		Model generatedModel = BioPAXLevel.L3.getDefaultFactory().createModel();
		generatedModel.setXmlBase(xmlBase);
		
		// find matching utility class elements in the db
		log.info("Looking to re-use/merge with " +
			"semantically equivalent utility class elements " +
			"in the Model");

		final Map<EntityReference, EntityReference> replacements = new HashMap<EntityReference, EntityReference>();
		
		// match some utility class objects to existing ones (previously imported)
		for (EntityReference bpe: new HashSet<EntityReference>(source.getObjects(EntityReference.class))) 
		{
			EntityReference replacement = null;
			// Find the best replacement
			if (bpe instanceof ProteinReference) {
				replacement = findOrCreateProteinReference((ProteinReference)bpe, model, generatedModel);
			} else if (bpe instanceof SmallMoleculeReference) {
				replacement = findOrCreateSmallMoleculeReference((SmallMoleculeReference)bpe, model, generatedModel);
			}
				
			if (replacement != null) {	
				final String id = replacement.getRDFId();
				if(model.containsID(id)) {
					// just put the existing object to the replacements map and continue;
					// skip in-memory merging, - preserves existing inverse BioPAX properties
					replacements.put(bpe, replacement);
				} else {
					if(!generatedModel.containsID(id)) {//- just pulled from the db? -
						// clear the AA sequence (save space and time; not really very useful...)
						if(replacement instanceof ProteinReference)
							((ProteinReference) replacement).setSequence(null);
					
						// in-memory merge to reuse same child xrefs, etc.
						simpleMerger.merge(generatedModel, replacement);
					} 
					
					// associate, continue
					replacements.put(bpe, (EntityReference) generatedModel.getByID(id));
				}
			}
		}
				
		// post-fix for some potentially troubling
		// inverse properties of the original things scheduled for replacement
		log.info("Migrating some properties (entityFeature and xref)...");
		for (EntityReference old : replacements.keySet()) {
			// fix for entityFeature/entityFeatureOf
			for (EntityFeature ef : new HashSet<EntityFeature>(old.getEntityFeature())) {
				// old ER is going to be replaced; migrate its features to the new ER
//				if(ef.getEntityFeatureOf() == old) 
				old.removeEntityFeature(ef);				
				EntityReference replacement = replacements.get(old);
				replacement.addEntityFeature(ef);
			}
				
			// remove xrefs from old ERs, then
			// copy all Pub. and Rel. xrefs to new ERs (this is to keep original xrefs too)
			for(Xref x : new HashSet<Xref>(old.getXref())) {
				old.removeXref(x);
				if(!(x instanceof UnificationXref)) {
					EntityReference rep = replacements.get(old);
					//first, look for the same xref in the target in-memory model
					Xref xr = (Xref) generatedModel.getByID(x.getRDFId());
					if(xr != null)
						rep.addXref(xr);
					else {
						rep.addXref(x);
						generatedModel.add(x);
					}
				}
			}				
		}			
		
		// The following can improve graph queries and full-text search relevance -
		log.info("Generating canonical UniProt/ChEBI " +
				"rel. xrefs for physical entities using existing xrefs " +
				"and id-mapping...");
		
		/* Using xrefs and id-mapping, add primary uniprot/chebi RelationshipXref 
		 * to all PE (SM, Protein, Dna,..) and Gene if possible;
		 * skip complexes, and skip if pe.entityReference.xref is not empty 
		 * (so id-mapping is done when mapping/replacing entity references).
		 * 
		 * This might eventually result in mutually exclusive identifiers, 
		 * but we'll keep those and just log a warning for future (data) fix, -
		 * for this is not a big deal as long as we are not merging data 
		 * but only use in search/query.
		 */		
		for(Entity pe : new HashSet<Entity>(source.getObjects(Entity.class))) 
		{
			if(pe instanceof PhysicalEntity) {
				if(pe instanceof SimplePhysicalEntity) {
					EntityReference er = ((SimplePhysicalEntity) pe).getEntityReference();
					if(er != null && !er.getXref().isEmpty())
						continue;
					if(pe instanceof SmallMolecule) {
						addCanonicalRelXrefs((SmallMolecule) pe, Mapping.Type.CHEBI, generatedModel);
					} else {
						// for Protein, Dna, DnaRegion, Rna*...
						addCanonicalRelXrefs((PhysicalEntity) pe, Mapping.Type.UNIPROT, generatedModel);
					}						
				} else if(pe instanceof Complex) {
					continue; // skip complexes
				} else {
					// do for base PEs
					addCanonicalRelXrefs((PhysicalEntity) pe, Mapping.Type.UNIPROT, generatedModel);
					addCanonicalRelXrefs((PhysicalEntity) pe, Mapping.Type.CHEBI, generatedModel);
				}
			} else if(pe instanceof Gene) {
				addCanonicalRelXrefs((XReferrable) pe, Mapping.Type.UNIPROT, generatedModel);
			}
		}		
					
		// DO replace (object refs) in the original pathwayModel
		log.info("Replacing utility objects with matching ones...");	
		ModelUtils.replace(source, replacements);	
		
		log.info("Removing replaced/dangling objects...");	
		ModelUtils.removeObjectsIfDangling(source, UtilityClass.class);
		
		//force re-using of matching by id Xrefs, CVs, etc.. from the generated model
		log.info("Merging original model into the in-memory generated one...");
		simpleMerger.merge(generatedModel, source); 
		log.info("Done in-memory merging...");
		
		
		log.info("Detaching and persisting the updated source model...");
		// a) get completely detached in-memory model (fixes dangling properties...)
		// b) merge to the target model (insert new objects and update relationships)
		model.merge(ModelUtils.writeRead(generatedModel));
		
		log.info("Merge is complete, exiting...");				
		return null; // ignore (not needed)
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
	 * @param model where to add new xrefs
	 * @throws AssertionError when bpe is neither Gene nor PhysicalEntity
	 */
	private void addCanonicalRelXrefs(XReferrable bpe, Mapping.Type mappType, Model model) 
	{
		if(!(bpe instanceof Gene || bpe instanceof PhysicalEntity))
			throw new AssertionError("Not Gene or PE: " + bpe);
		
		String db = (Mapping.Type.CHEBI ==  mappType) ?  "chebi" : "uniprot";
			
		// map and generate/add xrefs
		Set<String> mappingSet = idMappingByXrefs(bpe, mappType, UnificationXref.class);
		addRelXref(bpe, db, mappingSet, model);
		
		mappingSet = idMappingByXrefs(bpe, mappType, RelationshipXref.class);
		addRelXref(bpe, db, mappingSet, model);
	}


	/**
	 * Finds or creates relationship xrefs
	 * from the id-mapping results 
	 * and adds them to the object (and new model).
	 * 
	 * @param bpe a gene, physical entity or entity reference
	 * @param db database name for all (primary/canonical) xrefs; 'uniprot' or 'chebi'
	 * @param mappingSet
	 * @param model an in-memory model where to add new xrefs
	 * @throws AssertionError when bpe is neither Gene nor PhysicalEntity nor EntityReference
	 */
	private void addRelXref(XReferrable bpe, String db,
			Set<String> mappingSet, Model model) 
	{	
		if(!(bpe instanceof Gene || bpe instanceof PhysicalEntity || bpe instanceof EntityReference))
			throw new AssertionError("Not Gene or PE: " + bpe);
		
		for(String ac : mappingSet) {
			// find or create
			String rxUri = Normalizer.uri(xmlBase, db, ac, RelationshipXref.class);
			RelationshipXref rx = (RelationshipXref) model.getByID(rxUri);
			if(rx == null) {
				rx = model.addNew(RelationshipXref.class, rxUri);
				rx.setDb(db);
				rx.setId(ac);
			}				
			if(rx != null)
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
	 * @param mainModel
	 * @param generatedModel
	 * @return the replacement object or null if none can found
	 */
	private ProteinReference findOrCreateProteinReference(ProteinReference orig, Model mainModel, Model generatedModel) 
	{				
		ProteinReference toReturn = null;	
		
		final String standardPrefix = "http://identifiers.org/";
		final String canonicalPrefix = standardPrefix + "uniprot/";
		
		String uri = orig.getRDFId();
		
		// 1) try to re-use previously matched (in the current merge run) object
		// because we did validate/normalize all the data in Premerger stage and 
		// can expect a quick result in most cases...
		// canonical (warehouse) ERs have such URIs only
		if(uri.startsWith(canonicalPrefix)) {
			toReturn = getById(uri, ProteinReference.class, generatedModel, mainModel);
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
				toReturn = getById(canonicalPrefix + id, 
					ProteinReference.class, generatedModel, mainModel);
			}
		}
				
		// if yet nothing's found, 
		// 3) try using (already normalized) all Unification Xrefs and id-mapping (to uniprot ac). 
		if (toReturn == null) {
			Set<String> mappingSet = idMappingByXrefs(orig, Mapping.Type.UNIPROT, UnificationXref.class);
			if(!mappingSet.isEmpty()) {
				// use only the first result (a warning logged already)
				toReturn = getById(canonicalPrefix + mappingSet.iterator().next(), 
					ProteinReference.class, generatedModel, mainModel);			
			}
		}	
		
		// if nothing's found in the warehouse by URI and unif. xrefs, - 
		// 4) try relationship xrefs and id-mapping 
		if (toReturn == null) {
			Set<String> mappingSet = idMappingByXrefs(orig, Mapping.Type.UNIPROT, RelationshipXref.class);
			if(!mappingSet.isEmpty()) {
				// use only the first result (a warning logged already)
				toReturn = getById(canonicalPrefix + mappingSet.iterator().next(), 
					ProteinReference.class, generatedModel, mainModel);
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
	 * @param mainModel
	 * @param generatedModel
	 * @return the replacement object or null if none can found
	 */
	private SmallMoleculeReference findOrCreateSmallMoleculeReference(SmallMoleculeReference orig, Model mainModel, Model generatedModel) 
	{				
		SmallMoleculeReference toReturn = null;	
		
		final String standardPrefix = "http://identifiers.org/";
		final String canonicalPrefix = standardPrefix + "chebi/";
		
		String uri = orig.getRDFId();
		
		// 1) try to re-use previously matched (in the current merge run) object
		// because we did validate/normalize all the data in Premerger stage and 
		// can expect a quick result in most cases...
		// warehouse ERs have such URIs only
		if(uri.startsWith(canonicalPrefix)) {
			toReturn = getById(uri, 
					SmallMoleculeReference.class, generatedModel, mainModel);
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
				toReturn = getById(canonicalPrefix + id, 
					SmallMoleculeReference.class, generatedModel, mainModel);
			}
		}
				
		// if yet nothing's found, 
		// 3) try using (already normalized) all Unification Xrefs and id-mapping (to primary chebi). 
		if (toReturn == null) {
			Set<String> mappingSet = idMappingByXrefs(orig, Mapping.Type.CHEBI, UnificationXref.class);
			if(!mappingSet.isEmpty()) {
				// use only the first result (a warning logged already)
				toReturn = getById(canonicalPrefix + mappingSet.iterator().next(), 
					SmallMoleculeReference.class, generatedModel, mainModel);
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
				addRelXref(orig, "chebi", mappingSet, generatedModel);
			}	
		}
		
		return toReturn;
	}
	
	
	/**
	 * Finds or generates a biopax object by first looking in
	 * the in-memory (tmp) model, next - main (is the target one too)
	 * The last one can be null (to skip looking there). 
	 * 
	 * @param id
	 * @param type
	 * @param tmp
	 * @param main
	 * @return object or null
	 */
	private <T extends UtilityClass> T getById(final String id, final Class<T> type, 
			final Model tmp, final Model main) 
	{
		assert id != null;
		
		// get from the in-memory model
		T t = type.cast(tmp.getByID(id));
		if (t == null && main != null) {
			// second, try - in the main model
			t = type.cast(main.getByID(id));
		}
		
		return t;
	}
	
}