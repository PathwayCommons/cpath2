package cpath.service;

import cpath.service.api.CPathService;
import cpath.service.api.RelTypeVocab;
import cpath.service.jpa.Content;
import cpath.service.jpa.Metadata;

import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.level3.Process;
import org.biopax.paxtools.normalizer.Normalizer;
import org.biopax.paxtools.util.ClassFilterSet;
import org.biopax.paxtools.util.Filter;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;


/**
 * This class is responsible for semantic Merging 
 * of the normalized original provider's pathway data 
 * into the main persistent Paxtools BioPAX model.
 *
 * @author Igor Rodchenkov
 */
public final class Merger {

    private static final Logger log = LoggerFactory.getLogger(Merger.class);

	private final String xmlBase;
	private final CPathService service;
	private final Set<String> supportedTaxonomyIds;
    private final Model warehouseModel;
    private final Model mainModel;


	/**
	 * Constructor.
	 * 
	 * @param service cpath2 service
	 */
	public Merger(CPathService service)
	{
		this.service = service;
		this.xmlBase = service.settings().getXmlBase();
		this.supportedTaxonomyIds = service.settings().getOrganismTaxonomyIds();
		
		this.warehouseModel = service.loadWarehouseModel();
		Assert.notNull(warehouseModel, "No BioPAX Warehouse");
		log.info("Successfully imported Warehouse BioPAX archive.");	
		
		this.mainModel = BioPAXLevel.L3.getDefaultFactory().createModel();
		this.mainModel.setXmlBase(xmlBase);
		log.info("Created a new empty main BioPAX model.");
	}

	/**
	 * Gets the main BioPAX model, where all other 
	 * by-datasource models are to be (or have been already) merged.
	 * @return the main BioPAX model
	 */
	public Model getMainModel() {
		return mainModel;
	}
	
	public void merge() {
		//using a SimpleMerger with Filter (ERs,Pathways) here (to merge ent. features, xrefs, comments, etc.)
		SimpleMerger simpleMerger = new SimpleMerger(SimpleEditorMap.L3, new Filter<BioPAXElement>() {		
			public boolean filter(BioPAXElement object) {
				return true; //to copy mul. cardinality obj. props of matching by uri elements
			}
		});
		
		// build models and merge from dataFile.premergeData
		Collection<Metadata> providersMetadata = new ArrayList<>();
		
		for(Metadata metadata : service.metadata().findAll())
			providersMetadata.add(metadata);

		for (Metadata metadata : providersMetadata)
		{
			if(metadata.isNotPathwayData()) {
				log.info("Skip for warehouse data: " + metadata);
				continue;
			}

			Model providerModel = service.loadBiopaxModelByDatasource(metadata);
			if(providerModel == null) {
				// merge all (normalized BioPAX) data files of the same provider into one-provider model:
				providerModel = merge(metadata);

				// Replace not normalized so far URIs with generated ours; add a bp:comment about original URIs
				log.info("Replacing original URIs with " + xmlBase + " based URIs...");
				replaceConflictingUris(providerModel, mainModel);
				replaceOriginalUris(providerModel, metadata.getIdentifier());

				//break all cyclic pathway inclusions via pathwayComponent property
				for(Pathway pathway : providerModel.getObjects(Pathway.class)) {
					breakPathwayComponentCycle(pathway);
				}

				//export to the biopax archive in the batch downloads dir.
				save(providerModel, metadata);
			} else {
				log.warn("merge(), loaded previously created " + metadata.getIdentifier() +
						" BioPAX model (delete it if you want to start over).");
			}

			//merge into the main model
			log.info("Merging the integrated '" + metadata.getIdentifier() +
					"' model into the main all-providers BioPAX model...");
			simpleMerger.merge(mainModel, providerModel);
		}

		ModelUtils.removeObjectsIfDangling(mainModel, UtilityClass.class);

		//create or replace the main BioPAX archive
		log.info("Creating the main ('All') BioPAX archive...");
		save();
		
		log.info("Complete.");
	}

	//remove bad unif. and rel. xrefs
	private void cleanupXrefs(Model m) {
		for(Xref x : new HashSet<>(m.getObjects(Xref.class))) {
			if(!(x instanceof PublicationXref)) {
				//remove bad xrefs from the model and properties
				if (x.getDb() == null || x.getDb().isEmpty() || x.getId() == null || x.getId().isEmpty()) {
					m.remove(x);
					//remove from properties
					for (XReferrable owner : new HashSet<>(x.getXrefOf())) {
						owner.removeXref(x);
					}
				} else {
					x.setDb(x.getDb().toLowerCase());
				}
			}
		}
	}

	private Model merge(Metadata metadata) {
		Model providerModel = service.loadBiopaxModelByDatasource(metadata);
		if(providerModel == null)
		{
			providerModel = BioPAXLevel.L3.getDefaultFactory().createModel();
			providerModel.setXmlBase(xmlBase);

			for (Content pwdata : metadata.getContent()) {
				final String description = pwdata.toString();
				if (!new File(service.normalizedFile(pwdata)).exists()) {
					log.warn("Skip '" + description + "' - no normalized data found (failed premerge)");
					continue;
				}

				log.info("Merging: " + description);

				// import the BioPAX L3 pathway data into the in-memory paxtools model
				InputStream inputStream = CPathUtils.gzipInputStream(service.normalizedFile(pwdata));
				if(inputStream == null) {
					log.error("Skipped " + description + " - " + "cannot read " + service.normalizedFile(pwdata));
					continue;
				}

				merge(description, (new SimpleIOHandler(BioPAXLevel.L3)).convertFromOWL(inputStream), providerModel);
			}

			ModelUtils.removeObjectsIfDangling(providerModel, UtilityClass.class);

			log.info("Normalizing generics (" + metadata.getIdentifier() + ")...");
			ModelUtils.normalizeGenerics(providerModel);

//			//merge some Entities within a data source (e.g., stateless vcam1 P19320 MI participants in hprd, intact, biogrid)
//			log.info("Merging equivalent physical entities...");
//			ModelUtils.mergeEquivalentPhysicalEntities(providerModel);
//			log.info("Merging equivalent interactions...");
//			ModelUtils.mergeEquivalentInteractions(providerModel);

			log.info("Done merging " + metadata);
		} else {
			log.info("Loaded previously created " + metadata.getIdentifier() + " BioPAX model.");
		}

		//quick-fix BioSource : set name if not set
		Map<String,String> orgMap = service.settings().getOrganismsAsTaxonomyToNameMap();
		for(BioSource org : providerModel.getObjects(BioSource.class)) {
			for (String tax : orgMap.keySet()) {
				// BioSource URIs are already normalized and contain identifiers.org/taxonomy
				// (if it was possible to do) but may also contain a suffix after "_" (cell type, tissue terms)
				if(org.getUri().startsWith("http://identifiers.org/taxonomy/" + tax + "_")) {
					String name = orgMap.get(tax);
					if(!org.getName().contains(name)) {
						org.addName(name);
					}
				}
			}
		}

		return providerModel;
	}


	//break all cyclic pathway inclusions via pathwayComponent property
	public void breakPathwayComponentCycle(final Pathway pathway) {
		//run recursively, though, avoiding infinite loops (KEGG pathways can cause it)
		breakPathwayComponentCycle(new HashSet<>(), pathway, pathway);
	}

	private void breakPathwayComponentCycle(final Set<Pathway> visited,
											final Pathway rootPathway,
											final Pathway currentPathway)
	{
		if(!visited.add(currentPathway))
			return; // already processed

		if(currentPathway.getPathwayComponent().contains(rootPathway)) {
			currentPathway.removePathwayComponent(rootPathway);
		}

		for(Process proc : currentPathway.getPathwayComponent())
			if(proc instanceof Pathway)
				breakPathwayComponentCycle(visited, rootPathway, (Pathway) proc);
	}

	/**
	 * Exports the main model to the 'All' BioPAX archive
	 * in the cpath2 downloads (in production) or tmp (tests)
	 * directory.
	 */
	public void save() {
		try {		
			new SimpleIOHandler(BioPAXLevel.L3).convertToOWL(mainModel, 
				new GZIPOutputStream(new FileOutputStream(
						service.settings().mainModelFile())));
		} catch (Exception e) {
			throw new RuntimeException("Failed updating the main BioPAX archive.", e);
		}
	}

	void save(Model datasourceModel, Metadata datasource) {
		try {		
			new SimpleIOHandler(BioPAXLevel.L3).convertToOWL(datasourceModel, 
				new GZIPOutputStream(new FileOutputStream(
						service.settings().biopaxFileNameFull(datasource.getIdentifier()))));
		} catch (Exception e) {
			throw new RuntimeException("Failed updating the " + 
					datasource.getIdentifier() + " BioPAX archive.", e);
		}
	}
	
	/**
	 * Integrates the source model into the one-datasource target model 
	 * using the reference biopax objects (entity references, etc.) 
	 * from the target or warehouse models.
	 * As the result, the source model will (and the warehouse might) be broken 
	 * (ok to dispose), and the target - complete, self-integral,
	 * contain all the entities that were there before and new from the source
	 * model (where some original utility class objects are replaced with 
	 * canonical warehouse or previously existing target ones). 
	 * 
	 * @param description datasource (metadata) description
	 * @param source input model
	 * @param target model to merge into
	 */
	public void merge(final String description, final Model source, final Model target) {
		
		final String srcModelInfo = "source: " + description;

		cleanupXrefs(source);

		log.info("Searching for canonical or existing EntityReference objects " +
				" to replace equivalent original objects ("+srcModelInfo+")...");
		final Map<EntityReference, EntityReference> replacements = new HashMap<EntityReference, EntityReference>();
		// map EntityReference objects to the canonical ones (in the warehouse) if possible and safe
		for (EntityReference origEr: new HashSet<>(source.getObjects(EntityReference.class)))
		{
			EntityReference replacement = null;
			
			// Find the best replacement ER in the Warehouse:
			if (origEr instanceof ProteinReference) {
				replacement = findProteinReferenceInWarehouse((ProteinReference) origEr);
			} 
			else if (origEr instanceof SmallMoleculeReference) {
				replacement = findSmallMoleculeReferenceInWarehouse((SmallMoleculeReference) origEr);
			} 
				
			if (replacement != null) {
				//save in the map to replace the source bpe later 
				replacements.put(origEr, replacement);
				if(!replacement.getUri().equals(origEr.getUri()))
					replacement.addComment("REPLACED " + origEr.getUri());
			} else {
				//i.e., no matching ER found in the Warehouse (the ER is from unwanted organism or unknown/no id).
				// Remove the PR/Dna*R/Rna*R if entityReferenceOf() is empty (member of a generic ER, or dangling)
				if(origEr instanceof SequenceEntityReference && origEr.getEntityReferenceOf().isEmpty()) {
					//remove unwanted dangling/member ER from the source model
					boolean isSupported = false;
					BioSource org = ((SequenceEntityReference) origEr).getOrganism();
					if(org != null) {
						for (Xref x : org.getXref()) {
							if (supportedTaxonomyIds.contains(x.getId())) {
								isSupported = true;
								break;
							}
						}
					}
					if(!isSupported) {
						for(EntityReference genericEr : new HashSet<>(origEr.getMemberEntityReferenceOf())) {
							genericEr.removeMemberEntityReference(origEr);
						}
						source.remove(origEr);
						log.info("Removed a dangling/member " + origEr.getModelInterface().getSimpleName()
							+ " for which no warehouse ER was found: " + origEr.getUri() + "; organism: "
								+ ((SequenceEntityReference) origEr).getOrganism());
					}
				}
			}
		}
		
		// Explicitly remove old (to be replaced) objects from the source model
		// this is important for the replacement (below) to work, esp. in case 
		// new URI is the same as original normalized URI...
		for(EntityReference old : replacements.keySet()) {
			source.remove(old);
		}
		
		// Replace objects in the source model
		log.info("Replacing objects ("+srcModelInfo+")...");	
		ModelUtils.replace(source, replacements);
		
		log.info("Migrate some properties, such as original entityFeature and xref ("+srcModelInfo+")...");
		copySomeOfPropertyValues(replacements, target);

		filterOutUnwantedOrganismInteractions(source); //do carefully

		// cleaning up dangling objects (including the replaced above ones)
		log.info("Removing dangling objects ("+srcModelInfo+")...");
		ModelUtils.removeObjectsIfDangling(source, UtilityClass.class);

		//This improves our graph queries results and simple format output:
		xrefByMapping(source, srcModelInfo, target);

		log.info("Replacing conflicting URIs, if any (" + srcModelInfo + ")");
		replaceConflictingUris(source, target);
		
		log.info("Merging into the target one-datasource BioPAX model...");
		// merge all the elements and their children from the source to target model
		SimpleMerger simpleMerger = new SimpleMerger(SimpleEditorMap.L3, new Filter<BioPAXElement>() {		
			public boolean filter(BioPAXElement object) {
				return true; //to copy mul.card. prop. from source to target obj. having same URI
			}
		});
		simpleMerger.merge(target, source);
		log.info("Merged '" + srcModelInfo + "'.");

	}

	private void filterOutUnwantedOrganismInteractions(Model source) {
		//remove simple MIs where all participants are not from the organisms we want (as set in the properties file)
		miLoop: for(MolecularInteraction mi : new HashSet<>(source.getObjects(MolecularInteraction.class))) {
			//try to find a reason to keep this MI in the model:
			// - it has a participant having one of allowed taxonomy ID;
			// - it has a complex or process participant (at this time, we won't bother looking further...);
			// - it has a simple sequence or gene participant with no organism specified (unknown - keep the MI)
			for(Entity e : mi.getParticipant()) {
				if(e instanceof Gene) {
					BioSource bs = ((Gene) e).getOrganism();
					if(bs == null) {
						continue miLoop; //keep this MI untouched; go to the next one
					} else {
						for(Xref x : bs.getXref())
							if(supportedTaxonomyIds.contains(x.getId()))
								continue miLoop; //found a supported taxnonomy ID; skip the rest - do next MI...
					}
				}
				else if(e instanceof SimplePhysicalEntity && !(e instanceof SmallMolecule)) {
					SequenceEntityReference er = (SequenceEntityReference)((SimplePhysicalEntity)e).getEntityReference();
					if (er == null || er.getOrganism() == null)
						continue miLoop; //keep this MI
					else {
						for(Xref x : er.getOrganism().getXref())
							if(supportedTaxonomyIds.contains(x.getId()))
								continue miLoop; //found a supported taxnonomy; keep this MI
					}
				} else if(e instanceof SmallMolecule) {
					continue; //next participant
				} else {
					//won't touch a MI with a Complex or Process participant...
					continue miLoop;
				}
			}

			//unless jumped to 'miLoop:' label above, remove this MI and participants
			source.remove(mi);
			log.info("MI is removed (all participants come from unwanted organisms): " + mi.getUri());
			for(Entity e : new HashSet<>(mi.getParticipant())) {
				mi.removeParticipant(e);
				//ok to remove from the model as well, because some may come back after merging
				//if they are still used by other entities
				source.remove(e);
			}
		}
	}

	private void replaceConflictingUris(Model source, Model target) {
		//wrap source.getObjects() in a new set to avoid concurrent modif. excep.
		for(BioPAXElement bpe : new HashSet<>(source.getObjects()))
		{
			String currUri = bpe.getUri();
			if( !(bpe instanceof ProteinReference) && currUri.startsWith("http://identifiers.org/uniprot/")
				|| !(bpe instanceof SmallMoleculeReference) && currUri.startsWith("http://identifiers.org/chebi/"))
			{
				String newUri = Normalizer.uri(xmlBase, null, currUri, bpe.getModelInterface());
				CPathUtils.replaceID(source, bpe, newUri);
				((Level3Element) bpe).addComment("REPLACED " + currUri);
				log.info(String.format("Replaced URI %s of %s with %s due to potential type collision",
					currUri, bpe.getModelInterface().getSimpleName(), newUri));
			}
			else
			{
				BioPAXElement targetBpe = target.getByID(currUri);
				if (targetBpe != null && bpe.getModelInterface() != targetBpe.getModelInterface()) {
					String newUri = Normalizer.uri(xmlBase, null, currUri, bpe.getModelInterface());
					CPathUtils.replaceID(source, bpe, newUri);
					((Level3Element) bpe).addComment("REPLACED " + currUri);
					log.info(String.format("Replaced URI %s of %s with %s due to type collision: " +
						"%s in the target model has the same URI", currUri, bpe.getModelInterface().getSimpleName(),
							newUri, targetBpe.getModelInterface().getSimpleName()));
				}
			}
		}
	}

	/*
     * Replaces not normalized original URIs
     * in the one-datasource (merged) source model
     * with auto-generated new ones (using the xml:base);
     * adds the original URIs to bp:comment property.
     */
	private void replaceOriginalUris(Model source, String metadataId) {
		//wrap source.getObjects() in a new set to avoid concurrent modif. excep.
		for(BioPAXElement bpe : new HashSet<>(source.getObjects())) {
			String currUri = bpe.getUri();
			if( !(currUri.startsWith(xmlBase) || currUri.startsWith("http://identifiers.org/")) ) {
				// Generate a new URI (using Md5hex);
				// we use metadataId part here to avoid merging/messing up Evidence, SequenceSite, Stoichiometry,
				// etc. (usually not equivalent) annotation class things from different data providers...
				String newUri = Normalizer.uri(xmlBase, null, metadataId + currUri, bpe.getModelInterface());
				CPathUtils.replaceID(source, bpe, newUri);
				((Level3Element) bpe).addComment("REPLACED " + currUri);
			}
		}
	}

	/*
	* This procedure auto-generates UniProt and ChEBI xrefs
	* which improve converting of the biopax to simple text formats.
	*
	* So, entities that originally lacked entityReference, generics, complexes,
	* and ones having no uniprot/chebi/hgnc or with ambiguous ids
	* can now in some cases contribute to the results (SIF, GMT, SBGN output formats).
	*
	* This might eventually result in mutually exclusive identifiers,
	* which is ok as long as we do not ever merge Things based on these xrefs,
	* and only index or search in the db.
	*/
	private void xrefByMapping(Model source, String srcModelInfo, Model target) {
		log.info("Using original xrefs or names and id-mapping, add UniProt/ChEBI/HGNC xrefs " +
				" (unless they're too many) to not-merged PE/ERs (" + srcModelInfo + ")");
		for(Entity pe : new HashSet<>(source.getObjects(Entity.class)))
		{
			if(pe instanceof PhysicalEntity) {
				if(pe instanceof SimplePhysicalEntity) {
					// Skip for SPE that got its ER just replaced with an ER from the Warehouse
					EntityReference er = ((SimplePhysicalEntity) pe).getEntityReference();
					if(er != null && warehouseModel.containsID(er.getUri()))
						continue; //skip for just merged, canonical ERs

					if(pe instanceof SmallMolecule) {
						chemXrefByMapping(target, pe, 5);
					} else {//Protein, Dna*, Rna* - SequenceEntity types
						genomicXrefByMapping(target, pe, 5);
					}
				}
				else { // base PE class or Complex
					genomicXrefByMapping(target, pe, 5);
					chemXrefByMapping(target, pe, 5);
				}
			} else if(pe instanceof Gene) {
				genomicXrefByMapping(target, pe, 5);
			}
		}
	}

	private void copySomeOfPropertyValues(Map<EntityReference, EntityReference> replacements, Model model) {
		// post-fix
		for (EntityReference old : replacements.keySet()) {			
			final EntityReference repl = replacements.get(old);	
			
			for (EntityFeature ef : new HashSet<>(old.getEntityFeature()))
			{ // move entity features of the replaced ER to the new canonical one
				// remove the ef from the old ER
				old.removeEntityFeature(ef);
				// - should not belong to any other ER anymore (no entityFeature prop. can contain this ef)

				// If there exist an equivalent, don't add original 'ef', 
				// but just replace with the equiv. one in all PEs of given old ER
				EntityFeature equivEf = null;
				for(EntityFeature f : repl.getEntityFeature()) {
					if(f.isEquivalent(ef)) {
						equivEf = f;
						equivEf.getComment().addAll(ef.getComment());
						break;
					}
				}			
				if(equivEf == null) //add new EF to the canonical ER
					repl.addEntityFeature(ef);
				else { //update PEs' feature and notFeature properties to use the existing equiv. feature
					for(PhysicalEntity pe : new HashSet<>(ef.getFeatureOf())) {
						pe.removeFeature(ef);
						pe.addFeature(equivEf);
					}
					for(PhysicalEntity pe : new HashSet<>(ef.getNotFeatureOf())) {
						pe.removeNotFeature(ef);
						pe.addNotFeature(equivEf);
					}	
				}
			}				

			// Copy/Keep PublicationXrefs and RelationshipXrefs to the original PEs
			// (otherwise we'd lost most of original xrefs...) TODO review; shall we copy only PXs?
			for(Xref x : new HashSet<>(old.getXref())) {
				if(x instanceof UnificationXref) //sub with RX
					x = CPathUtils.findOrCreateRelationshipXref(RelTypeVocab.IDENTITY, x.getDb(), x.getId(), model, false);

				for(SimplePhysicalEntity owner : old.getEntityReferenceOf()) {
					owner.addXref(x);
				}
			}
		}
	}

	/*
	 * id-mapping by xrefs - to primary ChEBI IDs; adds new relationship xrefs to the entity.
	 * This won't improve our full-text index/search and graph queries (where id-mapping is used again anyway),
	 * but may help improve export to SIF and GSEA formats.
	 * This method is called only for those PEs/ERs that were not merged into warehouse canonical ERs,
	 * for reasons such as no match for a ID, or no ID found, ambiguous ID, etc.
	 * This method won't add additional xrefs if a ChEBI one (secondary id or not doesn't matter) is already there.
	 */
	 void chemXrefByMapping(final Model m, Named bpe, final int maxNumXrefsToAdd)
	{
		Assert.isTrue(!(bpe instanceof Process));
		// try/prefer to use ER instead of entity -
		if(bpe instanceof SimplePhysicalEntity) {
			EntityReference er = ((SimplePhysicalEntity) bpe).getEntityReference();
			if(er != null && !er.getXref().isEmpty())
				bpe = er;
		}
		//shortcut
		if(bpe.getXref().isEmpty() && bpe.getName().isEmpty()) {
			if(!isComplexOrGeneric(bpe))
				log.info("non-generic " + bpe.getModelInterface().getSimpleName()
						+ " (" + bpe.getUri() + ") has no xrefs/names.");
			return;
		}

		if(!xrefsContainDb(bpe, "CHEBI")) { //do only if bpe does not have any CHEBI xrefs
			// map other IDs and names to the primary IDs of ERs that can be found in the Warehouse
			Set<String> primaryIds = idMappingByXrefs(bpe, UnificationXref.class, "CHEBI");
			if (primaryIds.isEmpty())
				primaryIds = idMappingByXrefs(bpe, RelationshipXref.class, "CHEBI");
			if (primaryIds.isEmpty())
				primaryIds = mapSmallMoleculeByExactName(bpe);

			// add rel. xrefs if there are not too many (there's risk to make nonsense SIF/GSEA export...)
			if (!primaryIds.isEmpty() && primaryIds.size() <= maxNumXrefsToAdd) {
				addRelXrefs(m, bpe, "CHEBI", primaryIds, RelTypeVocab.ADDITIONAL_INFORMATION, false);
			}
		}
	}

	/*
     * Using the unification and relationship xrefs of a sequence entity or gene,
     * performs id-mapping to the primary uniprot ID, creates relationship xrefs and adds them back to the entity.
     *
     * This step won't much improve full-text index/search and graph queries
     * (where id-mapping is used again anyway), but may help improve export to SIF and GSEA formats.
     * This method is called only for original PEs or their ERs that were not mapped/merged
     * with a warehouse canonical ERs for various known reasons (no match for a ID or no ID, ambiguous ID, etc.)
     *
     * This method won't add additional xrefs if a UniProt/HGNC one is already present despite it'd map
     * to many canonical ERs/IDs (in fact, it'd even map to hundreds (Trembl) IDs, e.g., in cases like 'ND5',
     * and cause our export to the SIF, GSEA formats fail...)
     */
	void genomicXrefByMapping(final Model m, Named bpe, final int maxNumXrefsToAdd)
	{
		Assert.isTrue(!(bpe instanceof SmallMolecule  || bpe instanceof SmallMoleculeReference || bpe instanceof Process),
		"A process or chemical is not allowed here"); //a seq. entity or gene kind is expected here

		// try/prefer to use ER instead of entity -
		if(bpe instanceof SimplePhysicalEntity) {
			EntityReference er = ((SimplePhysicalEntity) bpe).getEntityReference();
			if(er != null && !er.getXref().isEmpty())
				bpe = er;
		}
		//shortcut
		if(bpe.getXref().isEmpty()) {
			if(!isComplexOrGeneric(bpe))
				log.info("non-generic " + bpe.getModelInterface().getSimpleName()
						+ " (" + bpe.getUri() + ") has no xrefs/names.");
			return;
		}

		final String organismRemark = getOrganism(bpe); //get organism taxID/name if possible
		final Collection<String> primaryACs = new HashSet<>();
		if(!xrefsContainDb(bpe, "UNIPROT")) {
			//bpe does not have any uniprot xrefs; try to map other IDs to the primary ACs of the PRs in the Warehouse
			primaryACs.addAll(idMappingByXrefs(bpe, UnificationXref.class, "UNIPROT"));
			if (primaryACs.isEmpty())
				primaryACs.addAll(idMappingByXrefs(bpe, RelationshipXref.class, "UNIPROT"));
            // FYI: if we'd try mapping biopolymers by name, then e.g,, 'HLA DQB1' or 'ND5'
            // would result in hundreds unique uniprot/trembl IDs; so we don't do this!

			// add rel. xrefs if there are not too many (there's risk to make nonsense SIF/GSEA export...)
			if (!primaryACs.isEmpty() && primaryACs.size() <= maxNumXrefsToAdd) {
				addRelXrefs(m, bpe, "UNIPROT", primaryACs, RelTypeVocab.ADDITIONAL_INFORMATION, true);
			}
			else if(primaryACs.size() > maxNumXrefsToAdd) {
				log.debug(bpe.getUri() + ", " + organismRemark + ", ambiguously maps to many UNIPROT ACs: "
						+ primaryACs.size());
				//remove some
				Iterator<String> it = primaryACs.iterator();
				while (it.hasNext() && primaryACs.size() > maxNumXrefsToAdd) {
					it.next();
					it.remove();
				}
				addRelXrefs(m, bpe, "UNIPROT", primaryACs, RelTypeVocab.ADDITIONAL_INFORMATION, true);
			}
		} else { //bpe has got some UniProt Xrefs (ok if secondary/isoform/trembl ID);
			// let's map those to primary accessions, then - to HGNC Symbols, and then remove other ids
			primaryACs.addAll(idMappingByXrefs(bpe, UnificationXref.class, "UNIPROT", "uniprot"));
			if(primaryACs.isEmpty())
				primaryACs.addAll(idMappingByXrefs(bpe, RelationshipXref.class, "UNIPROT", "uniprot"));
			//remove existing uniprot xrefs, add primary ones (unsupported species IDs must disappear)
			if(!primaryACs.isEmpty()) {
				Collection<String> newACs = new HashSet<>(primaryACs);
				for(Xref x : new HashSet<>(bpe.getXref())) { //here was a bug: body never executed due to empty set
					if(!(x instanceof PublicationXref)
                            && CPathUtils.startsWithAnyIgnoreCase(x.getDb(),"uniprot"))
					{
                        if (primaryACs.contains(x.getId())) {
                            newACs.remove(x.getId()); //won't add the same xref again below
							x.addComment("PRIMARY");
                        } else {
                            bpe.removeXref(x); //remove a secondary or unsupported species uniprot xref
                        }
                    }
				}
				addRelXrefs(m, bpe, "UNIPROT", newACs, RelTypeVocab.IDENTITY,true);
			}
		}

		// map primary ACs to HGNC Symbols and generate RXs if there're not too many...
		if (!xrefsContainDb(bpe, "hgnc symbol"))
			mayAddHgncXrefs(m, bpe, primaryACs, maxNumXrefsToAdd);
	}

	// For biopolymers, also map uniprot accessions to HGNC Symbols, and add the xrefs, if possible -
	private void mayAddHgncXrefs(final Model m, final XReferrable bpe,
								 final Collection<String> accessions, final int maxNumXrefsToAdd)
	{
		if(accessions == null || accessions.isEmpty())
			return;

		final Set<String> hgncSymbols = new HashSet<>();
		for (String ac : accessions) {
			ProteinReference canonicalPR =
					(ProteinReference) warehouseModel.getByID("http://identifiers.org/uniprot/" + ac);
			if (canonicalPR != null) {
				//TODO: shall we keep just one-two symbols (which) instead of using 'maxNumXrefsToAdd' param?
				for (Xref x : canonicalPR.getXref())
					if (x.getDb().equalsIgnoreCase("hgnc symbol"))
						hgncSymbols.add(x.getId());
			}
		}
		// add rel. xrefs if there are not too many (there's risk to make nonsense SIF/GSEA export...)
		if (!hgncSymbols.isEmpty() && hgncSymbols.size() <= maxNumXrefsToAdd)
			addRelXrefs(m, bpe, "hgnc symbol", hgncSymbols, RelTypeVocab.ADDITIONAL_INFORMATION, false);
	}

	private static boolean xrefsContainDb(XReferrable xr, String db)
	{
		db = db.toLowerCase();
		for(Xref x : xr.getXref())
		{
			if (!(x instanceof PublicationXref) && x.getDb().toLowerCase().startsWith(db)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Only for not-merged things, finds or creates canonical relationship xrefs using id-mapping;
	 * 
	 * @param model a biopax model where to find/create xrefs
	 * @param bpe a gene, physical entity or entity reference
	 * @param db ref. target database name for new xrefs; normally, 'uniprot', 'chebi', 'hgnc symbol'
	 * @param accessions bio/chem identifiers
	 * @param relType - vocabulary term to use with the Xref
	 * @param isPrimaryIds - if so, adds a comment "PRIMARY" to xrefs
	 * @throws AssertionError when bpe is neither Gene nor PhysicalEntity nor EntityReference
	 */
	private static void addRelXrefs(Model model, XReferrable bpe, String db,
									Collection<String> accessions, RelTypeVocab relType, boolean isPrimaryIds)
	{	
		if(!(bpe instanceof Gene || bpe instanceof PhysicalEntity || bpe instanceof EntityReference))
			throw new AssertionError("addRelXrefs: not a Gene, ER, or PE: " + bpe.getUri());
		
		for(String ac : accessions) {
			RelationshipXref rx = CPathUtils.findOrCreateRelationshipXref(relType, db, ac, model, isPrimaryIds);
			bpe.addXref(rx);
		}
	}

	/*
	 * Finds previously created or generates (searching in the db) 
	 * a new {@link ProteinReference} BioPAX element that is equivalent 
	 * to the original one and has standard URI and properties, 
	 * which allows to simply merge it with other semantically equivalent ones, by ID (URI).
	 */
	private ProteinReference findProteinReferenceInWarehouse(final ProteinReference orig)
	{
		final String standardPrefix = "http://identifiers.org/";
		final String warehouseUniprotUriPrefix = standardPrefix + "uniprot/";
		final String origUri = orig.getUri();
		
		// Try to re-use existing object
		if(origUri.startsWith(warehouseUniprotUriPrefix)) {
			ProteinReference toReturn = (ProteinReference) warehouseModel.getByID(origUri);
			if(toReturn != null)
				return toReturn;
		}
 
		// If nothing's found by URI so far,
		if (origUri.startsWith(standardPrefix)) {
			// try id-mapping to UniProt AC using the ID part of the normalized URI
			String id = origUri.substring(origUri.lastIndexOf('/')+1);
			Set<String> mp = service.map(id, "UNIPROT");
			Set<EntityReference> ers = findEntityRefUsingIdMappingResult(mp, warehouseUniprotUriPrefix);
			if(ers.size()>1) {
				log.debug(origUri + ": by URI, ambiguously maps to " + ers.size() + " warehouse PRs");
				return null;
			} else if (ers.size()==1)
				return (ProteinReference) ers.iterator().next();
		}
				
		// if still nothing came out yet, try id-mapping by `Xrefs:
		Set<EntityReference> ers = findWarehouseEntityRefByXrefsAndIdMapping(orig, "UNIPROT", warehouseUniprotUriPrefix);
		if(ers.size()>1) {
			log.debug(origUri + ": by Xrefs, ambiguously maps to " + ers.size() + " warehouse PRs");
			return null;
		} else if (ers.size()==1)
			return (ProteinReference) ers.iterator().next();

		// mapping/merging proteins by names is too risky, even when unambiguous (quite unlikely); so we won't do.

		//nothing found
		return null;
	}

	/* A tricky internal id-mapping method.
	 * @param element xRefferable BioPAX object; i.e. that can (and hopefully does) have Xrefs
	 * @param xrefClassForMapping only use this Xref sub-class for mapping
	 * @param toDb target ID type; can be either 'UNIPROT' or 'CHEBI' only
	 * @param dbStartsWithIgnoringcase optional list of allowed source xref.db names or prefixes
	 * @param <T> the Xref sub-type
     * @return primary accession numbers of the kind (toDb)
     */
	private <T extends Xref> Set<String> idMappingByXrefs(XReferrable element, Class<T> xrefClassForMapping,
														  String toDb, String... dbStartsWithIgnoringcase)
	{
		//this method is to be called for a Gene, Complex, EntityReference
		// - or a simple PEs that have no ER or its ER has no xrefs.
		Assert.isTrue(PublicationXref.class != xrefClassForMapping,
				"xrefClassForMapping cannot be PublicationXref");
		Assert.isTrue(element instanceof Gene || element instanceof PhysicalEntity
				|| element instanceof EntityReference, "element can be either: Gene, PE or ER...");
		//An EntityReference must be used here instead of its owner - SimplePhysicalEntity - when possible
		Assert.isTrue(!(element instanceof SimplePhysicalEntity)
				|| ((SimplePhysicalEntity) element).getEntityReference()==null
				|| ((SimplePhysicalEntity) element).getEntityReference().getXref().isEmpty(),
				"bad element type");

		Set<String> result = Collections.emptySet();
		final Set<T> filteredXrefs = new ClassFilterSet<>(element.getXref(), xrefClassForMapping);
		if(filteredXrefs.isEmpty()) {
			log.debug("no " + xrefClassForMapping.getSimpleName() +
				" xrefs found for " + element.getModelInterface().getSimpleName() + " (" + element.getUri());
		}
		else
		{
			final Set<String> sourceIds = new HashSet<>();
			for (T x : filteredXrefs) {
				if ( !(x instanceof PublicationXref) && !CPathUtils.startsWithAnyIgnoreCase(x.getDb(), "PANTHER")
					//- hack: skip "PANTHER PATHWAY COMPONENT" IDs which are similar to UniProt accessions,
					// such as "P02814", but not the same thing (avoid messing them up altogether)!
					&&	(dbStartsWithIgnoringcase.length == 0
							|| CPathUtils.startsWithAnyIgnoreCase(x.getDb(), dbStartsWithIgnoringcase))
				){
					sourceIds.add(CPathUtils.fixSourceIdForMapping(x.getDb(), x.getId()));
				}
			}
			// do id-mapping, for all ids at once, and return the result set:
			result = service.map(sourceIds, toDb);
		}

		return result;
	}

	private static boolean isComplexOrGeneric(BioPAXElement e) {
		return e instanceof Complex || ModelUtils.isGeneric(e);
	}

	//Gets the organism taxID or name from a biopax object if it has property 'organism', and that's not null.
	private static String getOrganism(BioPAXElement bpe) {
		PropertyEditor orgEditor = SimpleEditorMap.L3.getEditorForProperty("organism", bpe.getModelInterface());
		if(orgEditor != null) {
			Set<?> values = orgEditor.getValueFromBean(bpe);
			if(!values.isEmpty()) { //there only can be none or one BioSource
				BioSource bs = (BioSource) values.iterator().next();
				if(!bs.getXref().isEmpty())
					return bs.getXref().toString();
				else if(!bs.getName().isEmpty())
					return bs.getName().toString();
				//else do nothing (null will be the return)
			}
		}
		return null;
	}

	/**
	 * Finds previously created or generates (searching in the data warehouse) 
	 * a new {@link SmallMoleculeReference} BioPAX element that is equivalent 
	 * to the original one and has standard URI and properties, 
	 * which allows to simply merge it with other semantically equivalent ones, by ID (URI).
	 * 
	 * @param orig original reference small molecule
	 * @return the replacement standard object or null if not found/matched
	 */
	private SmallMoleculeReference findSmallMoleculeReferenceInWarehouse(final SmallMoleculeReference orig)
	{
		final String standardPrefix = "http://identifiers.org/";
		final String warehouseChebiUriPrefix = standardPrefix + "chebi/";
		final String origUri = orig.getUri();
		
		// Try to re-use existing object
		if(origUri.startsWith(warehouseChebiUriPrefix)) {
			SmallMoleculeReference toReturn = (SmallMoleculeReference) warehouseModel.getByID(origUri);
			if(toReturn != null)
				return toReturn;
		}

		// If nothing's found by URI, try id-mapping of the normalized URI part to chebi ID
		if (origUri.startsWith(standardPrefix)) {
			String id = origUri.substring(origUri.lastIndexOf('/')+1);
			if(origUri.contains("compound"))
				id = "CID:" + id;
			else if(origUri.contains("substance"))
				id = "SID:" + id;
			Set<String> mp = service.map(id, "CHEBI");
			Set<EntityReference> ers = findEntityRefUsingIdMappingResult(mp, warehouseChebiUriPrefix);
			if(ers.size()>1) {
				log.debug(origUri + ": by URI (ID part), ambiguously maps to " + ers.size() + " warehouse SMRs");
			}
			else if (!ers.isEmpty()) //size==1
				return (SmallMoleculeReference) ers.iterator().next();
		}

		// if so far the mapping there was either ambiguous or got nothing,
		// try id-mapping by (already normalized) Xrefs:
		Set<EntityReference> ers = findWarehouseEntityRefByXrefsAndIdMapping(orig, "CHEBI", warehouseChebiUriPrefix);
		if(ers.size()>1) {
			log.debug(origUri + ": by Xrefs, ambiguously maps to " + ers.size() + " warehouse SMRs");
			return null;
		} else if (ers.size()==1)
			return (SmallMoleculeReference) ers.iterator().next();

		// nothing? - keep trying, map by name (e..g, 'ethanol') to ChEBI ID
		Set<String> mp = mapSmallMoleculeByExactName(orig);
		ers = findEntityRefUsingIdMappingResult(mp, warehouseChebiUriPrefix);
		if(ers.size()>1) {
			log.debug(origUri + ": by NAMEs, ambiguously maps to " + ers.size() + " warehouse SMRs");
			return null;
		} else if (ers.size()==1) {
			SmallMoleculeReference smr = (SmallMoleculeReference) ers.iterator().next();
			log.warn(origUri + " is merged by name(s) to one " + smr.getUri());
			return smr;
		}

		//if nothing found
		return null;
	}

	private Set<EntityReference> findWarehouseEntityRefByXrefsAndIdMapping(
			EntityReference orig, String dest, String canonicalUriPrefix)
	{
		//map by unification xrefs that are equivalent or map to the same, the only, primary ID and warehouse ER
		Set<String> mappingSet = idMappingByXrefs(orig, UnificationXref.class, dest);
		Set<EntityReference> mapsTo = findEntityRefUsingIdMappingResult(mappingSet, canonicalUriPrefix);

		if(mapsTo.isEmpty()) {
			//next, try - relationship xrefs
			mappingSet = idMappingByXrefs(orig, RelationshipXref.class, dest);
			mapsTo = findEntityRefUsingIdMappingResult(mappingSet, canonicalUriPrefix);
		}

		return mapsTo; //can be more than one, but then we won't merge the original ER
	}

	private Set<String> mapSmallMoleculeByExactName(Named el) {
		Set<String> mp = new HashSet<>();

		// save all the names in a different Set:
		final Set<String> names = new HashSet<>();
		for(String n : el.getName())
			names.add(n.toLowerCase()); //LC is vital

		if(el instanceof SmallMolecule || el instanceof SmallMoleculeReference) {
			//find a warehouse SMR(s) with exactly the same name (case-insensitive).
			for(SmallMoleculeReference er : warehouseModel.getObjects(SmallMoleculeReference.class))
			{
				for(String s : er.getName()) {
					if(names.contains(s.toLowerCase())) {
						//extract the ChEBI accession from URI, add
						mp.add(CPathUtils.idfromNormalizedUri(er.getUri()));
						break;
					}
				}
			}			
		}

		return mp;
	}

	private Set<EntityReference> findEntityRefUsingIdMappingResult(Set<String> mapsTo, String uriPrefix)
	{
		Set<EntityReference> toReturn = new HashSet<>();
		
		for(String id : mapsTo) {		
			String uri = uriPrefix + id;
			EntityReference er = (EntityReference) warehouseModel.getByID(uri);
			if(er != null)
				toReturn.add(er);
		}
		
		return toReturn;
	}
}