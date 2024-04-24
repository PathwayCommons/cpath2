package cpath.service;

import cpath.service.api.Service;
import cpath.service.api.RelTypeVocab;
import cpath.service.metadata.Datasource;

import org.apache.commons.lang3.StringUtils;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.level3.Process;
import org.biopax.paxtools.normalizer.Resolver;
import org.biopax.paxtools.util.ClassFilterSet;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
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
	private final Service service;
	private final Set<String> supportedTaxonomyIds;
	private final Model warehouseModel;
  private final Model mainModel;


	/**
	 * Constructor.
	 * 
	 * @param service cpath2 service
	 */
	Merger(Service service)
	{
		this.service = service;
		this.supportedTaxonomyIds = service.settings().getOrganismTaxonomyIds();
		this.warehouseModel = service.loadWarehouseModel();
		Assert.notNull(warehouseModel, "No BioPAX Warehouse");
		log.info("Successfully imported Warehouse BioPAX archive.");
		this.mainModel = BioPAXLevel.L3.getDefaultFactory().createModel();
		this.mainModel.setXmlBase(service.settings().getXmlBase());
		log.info("Created a new empty main BioPAX model.");
	}

	/**
	 * Gets the main BioPAX model, where all other 
	 * by-datasource models are to be (or have been already) merged.
	 * @return the main BioPAX model
	 */
	Model getMainModel() {
		return mainModel;
	}

	Model getWarehouseModel() {
		return warehouseModel;
	}
	
	public void merge() {
		SimpleMerger simpleMerger = new SimpleMerger(SimpleEditorMap.L3, object -> true);
		//init the lucene index
		service.initIndex(mainModel, service.settings().indexDir(), false);
		for (Datasource datasource : service.metadata().getDatasources()) {
			if(datasource.getType().isNotPathwayData()) {
				log.info("Skip Warehouse data: {}", datasource);
				continue;
			}
			Model providerModel = merge(datasource);
			log.info("Replacing xml:base of non-generated/normalized URIs in {}", datasource.getIdentifier());
			CPathUtils.rebaseUris(providerModel, null, datasource.getIdentifier()+":");
			log.info("Replacing conflicting URIs in {} before merging into Main...", datasource.getIdentifier());
			replaceConflictingUris(providerModel, mainModel);
			save(providerModel, datasource);
			log.info("Merging '{}' model into the Main BioPAX model...", datasource.getIdentifier());
			simpleMerger.merge(mainModel, providerModel);
		}
		ModelUtils.removeObjectsIfDangling(mainModel, UtilityClass.class);
		save();
		service.setModel(mainModel);
		log.info("All merged!");
	}

	//remove bad unification and relationship xrefs, if any;
	//otherwise, just lowercase the xref.db (but not id, which is case-sensitive)
	private void cleanupXrefs(Model m) {
		for(Xref x : new HashSet<>(m.getObjects(Xref.class))) {
			if(!(x instanceof PublicationXref)) {
				//remove bad xrefs from the model and properties
				if (StringUtils.isBlank(x.getDb()) || StringUtils.isBlank(x.getId())) {
					m.remove(x);
					//also remove from biopax properties
					for (XReferrable owner : new HashSet<>(x.getXrefOf())) {
						owner.removeXref(x);
					}
				} else {
					x.setDb(x.getDb().toLowerCase());
				}
			}
		}
	}

	private Model merge(Datasource datasource) {
		log.info("Merging {}", datasource.getIdentifier());
		//try to load already merged/processed model first, if there exists
		Model providerModel = service.loadBiopaxModelByDatasource(datasource);
		if(providerModel == null) {
			//create a new model to merge several source files into one
			providerModel = BioPAXLevel.L3.getDefaultFactory().createModel();
			//set xml:base for all generated/normalized objects
			providerModel.setXmlBase(service.settings().getXmlBase());
			for (String f : datasource.getFiles()) {
				String fn = CPathUtils.normalizedFile(f);
				if (Files.notExists(Paths.get(fn))) {
					log.warn("Skipped {} - no normalized data found", datasource.getIdentifier());
					continue;
				}
				log.info("Processing: {}", fn);
				// import the BioPAX L3 pathway data into the in-memory paxtools model
				InputStream inputStream = CPathUtils.gzipInputStream(fn);
				if(inputStream == null) {
					log.error("Skipped {} - cannot read", fn);
					continue;
				}
				//merge each input file model with Warehouse model (using id-mapping too) and into providerModel (one-datasource)
				//
				Model oneFileModel =  (new SimpleIOHandler(BioPAXLevel.L3)).convertFromOWL(inputStream);
				merge(fn, oneFileModel, providerModel);
			}

			log.info("Removing dangling utility class elements from {}...", datasource.getIdentifier());
			ModelUtils.removeObjectsIfDangling(providerModel, UtilityClass.class);

			log.info("Normalizing generics in {}...", datasource.getIdentifier());
			ModelUtils.normalizeGenerics(providerModel);

			//for (already normalized) BioSource, also add the name from
			//application.properties (it helps full-text search in case the orig. BioSource had no names but taxon ref...)
			Map<String,String> orgMap = service.settings().getOrganismsAsTaxonomyToNameMap();
			for(BioSource org : providerModel.getObjects(BioSource.class)) {
				for(UnificationXref x : new ClassFilterSet<>(org.getXref(), UnificationXref.class)) {
					String orgName = orgMap.get(x.getId());
					if(orgName != null) {
						org.addName(orgName);
					}
				}
			}

			log.info("Breaking pathway/pathwayComponent cycles in {}...", datasource.getIdentifier());
			for(Pathway pathway : providerModel.getObjects(Pathway.class)) {
				breakPathwayComponentCycle(pathway);
			}

			log.info("Done merging {}", datasource);
		} else {
			log.info("Loaded BioPAX model from {}", service.settings().biopaxFileNameFull(datasource.getIdentifier()));
		}

		return providerModel;
	}

	//break all cyclic pathway inclusions via pathwayComponent property
	private void breakPathwayComponentCycle(final Pathway pathway) {
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
	protected void save() {
		try {
			log.info("Saving the main BioPAX Model to file: {}", service.settings().mainModelFile());
			new SimpleIOHandler(BioPAXLevel.L3).convertToOWL(mainModel, 
				new GZIPOutputStream(new FileOutputStream(service.settings().mainModelFile())));
		} catch (Exception e) {
			throw new RuntimeException("Failed saving the main BioPAX archive.", e);
		}
	}

	private void save(Model model, Datasource ds) {
		try {
			String path = service.settings().biopaxFileNameFull(ds.getIdentifier());
			log.info("Saving model:'{}' to file: {}", ds.getIdentifier(), path);
			new SimpleIOHandler(BioPAXLevel.L3).convertToOWL(model, new GZIPOutputStream(new FileOutputStream(path)));
		} catch (Exception e) {
			throw new RuntimeException("Failed updating the " + ds.getIdentifier() + " BioPAX archive.", e);
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
	 * @param source input model (normally a one-datasource one-file model here)
	 * @param target model to merge into
	 */
	protected void merge(final String description, final Model source, final Model target) {
		final String srcModelInfo = "source: " + description;

		cleanupXrefs(source);

		log.info("Searching for canonical/existing ERs to replace the original ones in {}...", srcModelInfo);
		final Map<EntityReference, EntityReference> replacements = new HashMap<>();
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
			} else { // No matching ER found in the Warehouse (the ER is from unwanted organism or unknown id)
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
						log.info("Removed a dangling/member: {} as no warehouse ER was found: {}; organism: {}",
								origEr.getModelInterface().getSimpleName(), origEr.getUri(),
								((SequenceEntityReference) origEr).getOrganism());
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
		log.info("Replacing objects in {}...", srcModelInfo);
		ModelUtils.replace(source, replacements);
		
		log.info("Migrate properties, such as original entityFeature and xref in {}...", srcModelInfo);
		copySomeOfPropertyValues(replacements, target);

		filterOutUnwantedOrganismInteractions(source); //do carefully

		// cleaning up dangling objects (including the replaced above ones)
		log.info("Removing dangling objects in {}...", srcModelInfo);
		ModelUtils.removeObjectsIfDangling(source, UtilityClass.class);

		//This improves our graph queries results and simple format output:
		xrefByMapping(source, srcModelInfo, target);

		log.info("Replacing conflicting URIs, if any, in {}", srcModelInfo);
		replaceConflictingUris(source, target);
		
		log.info("Merging into the target one-datasource BioPAX model...");
		// merge all the elements and their children from the source to target model
		// and copy mul. cardinality prop. from source to target obj. having same URI
		SimpleMerger simpleMerger = new SimpleMerger(SimpleEditorMap.L3, object -> true);
		simpleMerger.merge(target, source);

		log.info("Merged {}", srcModelInfo);
	}

	private void filterOutUnwantedOrganismInteractions(Model source) {
		//remove simple MIs where all participants are not from the organisms we want (as set in the properties file)
		miLoop: for(MolecularInteraction mi : new HashSet<>(source.getObjects(MolecularInteraction.class))) {
			//Find a reason to keep this MI in the model:
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
								continue miLoop; //found a supported taxonomy ID; skip the rest - do next MI...
					}
				}
				else if(e instanceof SimplePhysicalEntity && !(e instanceof SmallMolecule)) {
					//e.g., protein, dna, rna (not a chemical nor complex)
					SequenceEntityReference er = (SequenceEntityReference)((SimplePhysicalEntity)e).getEntityReference();
					if (er == null || er.getOrganism() == null)
						continue miLoop; //keep this MI
					else {
						for(Xref x : er.getOrganism().getXref())
							if(supportedTaxonomyIds.contains(x.getId()))
								continue miLoop; //found a supported taxonomy; keep this MI
					}
				} else if( !(e instanceof SmallMolecule) ) {
					//keep when MI has a Complex or Process participant...
					continue miLoop;
				}
				//a SmallMolecule does not help us decide; so we continue to the next participant
			}

			//unless jumped to 'miLoop:' label above, remove this MI and participants
			source.remove(mi);
			log.info("MI is removed (all participants come from unwanted organisms): {}", mi.getUri());
			for(Entity e : new HashSet<>(mi.getParticipant())) {
				mi.removeParticipant(e);
				//ok to remove from the model as well, because some may come back after merging
				//if they are still used by other entities
				source.remove(e);
			}
		}
	}

	void replaceConflictingUris(Model source, Model target) {
		//iterate over a new set to avoid concurrent mod. ex. in CPathUtils.changeUri
		for(BioPAXElement bpe : new HashSet<>(source.getObjects())) {
			String currUri = bpe.getUri();
			BioPAXElement existBpe = target.getByID(currUri);
			if(
				(!(bpe instanceof ProteinReference) && (StringUtils.containsIgnoreCase(currUri, "bioregistry.io/uniprot")))
				||
				(!(bpe instanceof SmallMoleculeReference) && (StringUtils.containsIgnoreCase(currUri, "bioregistry.io/chebi")))
				||
				(existBpe != null && bpe.getModelInterface() != existBpe.getModelInterface())
			){
				//Replace URI due to type collision
				CPathUtils.replaceUri(source, bpe,
						target.getXmlBase() + bpe.getModelInterface().getSimpleName() + "_" + UUID.randomUUID());
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
				" (unless they're too many) to not-merged PE/ERs ({})", srcModelInfo);
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
		for (Map.Entry<EntityReference,EntityReference> entry: replacements.entrySet()) {
			final EntityReference old = entry.getKey();
			final EntityReference repl = entry.getValue();
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
		}
	}

	/*
	 * id-mapping by xrefs - to primary ChEBI IDs; adds new relationship xrefs to the entity.
	 * This won't improve our full-text index/search and graph queries (where id-mapping is used again anyway),
	 * but may help improve export to SIF and GSEA formats.
	 * This method is called only for those PEs/ERs that were not merged into warehouse canonical ERs,
	 * for reasons such as no match for an ID, or no ID found, ambiguous ID, etc.
	 * This method won't add additional xrefs if a ChEBI one (secondary id or not doesn't matter) is already there.
	 */
	private void chemXrefByMapping(final Model m, Named bpe, final int maxNumXrefsToAdd)
	{
		Assert.isTrue(!(bpe instanceof Process), "An interaction or pathway is not expected here");
		// try/prefer to use ER instead of entity -
		if(bpe instanceof SimplePhysicalEntity) {
			EntityReference er = ((SimplePhysicalEntity) bpe).getEntityReference();
			if(er != null && !er.getXref().isEmpty())
				bpe = er;
		}

		//shortcut
		if(bpe.getXref().isEmpty() && bpe.getName().isEmpty()) {
			if(!isComplexOrGeneric(bpe))
				log.info("non-generic {} ({}) has no xrefs/names", bpe.getModelInterface().getSimpleName(), bpe.getUri());
			return;
		}

		if(noneXrefDbStartsWith(bpe, "CHEBI")) { //do only if bpe does not have any CHEBI xrefs
			// map other IDs and names to the primary IDs of ERs that can be found in the Warehouse
			Set<String> primaryIds = idMappingByXrefs(bpe, UnificationXref.class, "CHEBI");
			if (primaryIds.isEmpty())
				primaryIds = idMappingByXrefs(bpe, RelationshipXref.class, "CHEBI");
			if (primaryIds.isEmpty())
				primaryIds = mapSmallMoleculeByExactName(bpe);

			// add rel. xrefs if there are not too many (there's risk to make nonsense SIF/GSEA export...)
			if (!primaryIds.isEmpty() && primaryIds.size() <= maxNumXrefsToAdd) {
				addRelXrefs(m, bpe, "CHEBI", primaryIds, RelTypeVocab.ADDITIONAL_INFORMATION);
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
     * with a warehouse canonical ERs for various known reasons (no match for an ID or no ID, ambiguous ID, etc.)
     *
     * This method won't add additional xrefs if a UniProt/HGNC one is already present despite it'd map
     * to many canonical ERs/IDs (in fact, it'd even map to hundreds (Trembl) IDs, e.g., in cases like 'ND5',
     * and cause our export to the SIF, GSEA formats fail...)
     */
	private void genomicXrefByMapping(final Model m, Named bpe, final int maxNumXrefsToAdd)
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
				log.info("non-generic {} ({}) has no xrefs/names", bpe.getModelInterface().getSimpleName(), bpe.getUri());
			return;
		}

		final String organismRemark = getOrganism(bpe); //get organism taxID/name if possible
		final Collection<String> primaryACs = new HashSet<>();
		if(noneXrefDbStartsWith(bpe, "UNIPROT")) {
			//bpe does not have any uniprot xrefs; try to map other IDs to the primary ACs of the PRs in the Warehouse
			primaryACs.addAll(idMappingByXrefs(bpe, UnificationXref.class, "UNIPROT"));
			if (primaryACs.isEmpty()) {
				primaryACs.addAll(idMappingByXrefs(bpe, RelationshipXref.class, "UNIPROT"));
				// FYI: if we'd try mapping biopolymers by name, then, e.g., 'HLA DQB1' or 'ND5'
				// would result in hundreds unique uniprot/trembl IDs; so we don't do this!
			}

			// add rel. xrefs if there are not too many (there's risk to make nonsense SIF/GSEA export...)
			if (!primaryACs.isEmpty() && primaryACs.size() <= maxNumXrefsToAdd) {
				addRelXrefs(m, bpe, "UNIPROT", primaryACs, RelTypeVocab.ADDITIONAL_INFORMATION);
			}
			else if(primaryACs.size() > maxNumXrefsToAdd) {
				log.debug("{}, {}, ambiguously maps to many UNIPROT ACs: {}", bpe.getUri(), organismRemark, primaryACs.size());
				//remove some
				Iterator<String> it = primaryACs.iterator();
				while (it.hasNext() && primaryACs.size() > maxNumXrefsToAdd) {
					it.next();
					it.remove();
				}
				addRelXrefs(m, bpe, "UNIPROT", primaryACs, RelTypeVocab.ADDITIONAL_INFORMATION);
			}
		} else { //bpe has got some UniProt Xrefs (ok if secondary/isoform/trembl ID);
			// let's map those to primary accessions, then - to HGNC Symbols, and then remove other ids
			primaryACs.addAll(idMappingByXrefs(bpe, UnificationXref.class,
					"UNIPROT", "uniprot"));
			if(primaryACs.isEmpty()) {
				primaryACs.addAll(idMappingByXrefs(bpe, RelationshipXref.class,
						"UNIPROT", "uniprot"));
			}
			//remove existing uniprot xrefs, add primary ones (unsupported species IDs must disappear)
			if(!primaryACs.isEmpty()) {
				Collection<String> newACs = new HashSet<>(primaryACs);
				for(Xref x : new HashSet<>(bpe.getXref())) { //here was a bug: body never executed due to empty set
					if(!(x instanceof PublicationXref)
							&& CPathUtils.startsWithAnyIgnoreCase(x.getDb(),"uniprot")) {
						if (primaryACs.contains(x.getId())) {
							newACs.remove(x.getId()); //won't add the same xref again below
						} else {
							bpe.removeXref(x); //remove a secondary or unsupported species uniprot xref
						}
					}
				}
				addRelXrefs(m, bpe, "UNIPROT", newACs, RelTypeVocab.IDENTITY);
			}
		}

		// map primary ACs to HGNC Symbols and generate RXs if not too many...
		if (noneXrefDbStartsWith(bpe, "hgnc.symbol")) {
			mayAddHgncXrefs(m, bpe, primaryACs, maxNumXrefsToAdd);
		}
	}

	// For biopolymers, also map uniprot accessions to HGNC Symbols, and add the xrefs, if possible -
	private void mayAddHgncXrefs(final Model m, final XReferrable bpe,
								 final Collection<String> accessions, final int maxNumXrefsToAdd) {
		if(accessions == null || accessions.isEmpty()) {
			return;
		}
		final Set<String> hgncSymbols = new HashSet<>();
		for (String ac : accessions) {
			ProteinReference canonicalPR = (ProteinReference) warehouseModel.getByID("http://bioregistry.io/uniprot:" + ac);
			if (canonicalPR != null) {
				for (Xref x : canonicalPR.getXref())
					if (x.getDb().equalsIgnoreCase("hgnc.symbol")) {
						hgncSymbols.add(x.getId());
					}
			}
		}
		// add rel. xrefs if there are not too many (there's risk to make nonsense SIF/GSEA export...)
		if (!hgncSymbols.isEmpty() && hgncSymbols.size() <= maxNumXrefsToAdd) {
			addRelXrefs(m, bpe, "hgnc.symbol", hgncSymbols, RelTypeVocab.ADDITIONAL_INFORMATION);
		}
	}

	private static boolean noneXrefDbStartsWith(XReferrable xr, String db) {
		for(Xref x : xr.getXref()) {
			if (!(x instanceof PublicationXref) && StringUtils.startsWithIgnoreCase(x.getDb(), db)) {
				return false;
			}
		}
		return true;
	}

	/*
	 * Only for not-merged things, finds or creates canonical relationship xrefs using id-mapping;
	 * 
	 * @param model a biopax model where to find/create xrefs
	 * @param bpe a gene, physical entity or entity reference
	 * @param db ref. target database name for new xrefs; normally, 'uniprot', 'chebi', 'hgnc symbol'
	 * @param accessions bio/chem identifiers
	 * @param relType - vocabulary term to use with the Xref
	 * @throws AssertionError when bpe is neither Gene nor PhysicalEntity nor EntityReference
	 */
	private static void addRelXrefs(Model model, XReferrable bpe, String db,
									Collection<String> accessions, RelTypeVocab relType)
	{	
		if(!(bpe instanceof Gene || bpe instanceof PhysicalEntity || bpe instanceof EntityReference))
			throw new AssertionError("addRelXrefs: not a Gene, ER, or PE: " + bpe.getUri());
		
		for(String ac : accessions) {
			RelationshipXref rx = CPathUtils.findOrCreateRelationshipXref(relType, db, ac, model);
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
		final String origUri = orig.getUri();
		
		// first, search in the Warehouse for a PR by the uri
		ProteinReference toReturn = (ProteinReference) warehouseModel.getByID(origUri);
		if(toReturn != null) {
			return toReturn;
		}

		// when orig has no xrefs or only publication xrefs,
		if(orig.getXref().stream().noneMatch(x -> !(x instanceof PublicationXref))) {
			// try id-mapping to uniprot AC using the ID part of the normalized URI
			String id = CPathUtils.idFromNormalizedUri(origUri);
			if (id != null) { //indeed normalized PR
				Set<String> mp = service.map(List.of(id), "UNIPROT");
				Set<EntityReference> ers = entRefFromWhByPrimaryId(mp, "UNIPROT");
				if (ers.size() > 1) {
					log.debug("{}: by URI, ambiguously maps to {} warehouse PRs", origUri, ers.size());
					return null;
				} else if (ers.size() == 1)
					return (ProteinReference) ers.iterator().next();
			}
		} else {
			// try id-mapping by xrefs
			// map by unification xrefs that are equivalent or map to the same, the only, primary ID and warehouse ER
			Set<String> primaryIds = idMappingByXrefs(orig, UnificationXref.class, "UNIPROT");
			Set<EntityReference> ers = entRefFromWhByPrimaryId(primaryIds, "UNIPROT");
			if (ers.isEmpty()) {
				//next, try - relationship xrefs
				primaryIds = idMappingByXrefs(orig, RelationshipXref.class, "UNIPROT");
				ers = entRefFromWhByPrimaryId(primaryIds, "UNIPROT");
			}
			if (ers.size() > 1) {
				log.debug("{}: by Xrefs, ambiguously maps to {} warehouse PRs", origUri, ers.size());
				return null;
			} else if (ers.size() == 1) {
				return (ProteinReference) ers.iterator().next();
			}
		}

		// protein names are risky to use for mapping even if unambiguous (unlikely); won't do

		// none found
		return null;
	}

	/* A tricky internal id-mapping method.
	 * @param element XRefferable BioPAX object; i.e. that can (and hopefully does) have Xrefs
	 * @param xrefClassForMapping only use this Xref subclass for mapping
	 * @param toDb target ID type; can be either 'UNIPROT' or 'CHEBI' only
	 * @param dbStartsWithIgnoringcase optional list of allowed source xref.db names or prefixes
	 * @param <T> only either UnificationXref or RelationshipXref
   * @return primary accession numbers of the kind (toDb)
  */
	private <T extends Xref> Set<String> idMappingByXrefs(XReferrable element, Class<T> xrefClassForMapping,
														  String toDb, String... dbStartsWithIgnoringcase) {
		//this method should be called for a Gene, Complex, EntityReference,
		//or for SimplePhysicalEntity that either have no ER or its ER has no xrefs.
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
			log.debug("no {} found for {}: {}",	xrefClassForMapping.getSimpleName(),
					element.getModelInterface().getSimpleName(),  element.getUri());
		} else {
			final Set<String> sourceIds = new HashSet<>();
			for (T x : filteredXrefs) {
				if ( !(x instanceof PublicationXref) && !CPathUtils.startsWithAnyIgnoreCase(x.getDb(), "PANTHER")
					//- hack: skip "PANTHER PATHWAY COMPONENT" IDs which are similar to UniProt accessions,
					// such as "P02814", but not the same thing (avoid messing them up altogether)!
					&&	(dbStartsWithIgnoringcase.length == 0
							|| CPathUtils.startsWithAnyIgnoreCase(x.getDb(), dbStartsWithIgnoringcase))
				){
					sourceIds.add(CPathUtils.fixIdForMapping(x.getDb(), x.getId()));
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
			Set<BioSource> values = orgEditor.getValueFromBean(bpe);
			if(!values.isEmpty()) { //there only can be none or one BioSource
				BioSource bs = values.iterator().next();
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
		final String origUri = orig.getUri();

		//first, search in the Warehouse with the original SMR uri (feel lucky?)
		BioPAXElement el = warehouseModel.getByID(origUri);
		if( el instanceof SmallMoleculeReference) {
			return (SmallMoleculeReference) el; //awesome!
		}

		// when orig has no xrefs or only publication xrefs,
		if(orig.getXref().stream().noneMatch(x -> !(x instanceof PublicationXref))) {
			// try with the id from the (normalized but not a chebi) SMR URI
			// and perform id-mapping to find a canonical SMR in the Warehouse model,
			String id = CPathUtils.idFromNormalizedUri(origUri);
			if (id != null) {
				Set<String> mp = service.map(List.of(id), "CHEBI");
				Set<EntityReference> ers = entRefFromWhByPrimaryId(mp, "CHEBI");
				if (ers.size() > 1) {
					log.debug("{}: by URI (ID part), ambiguously maps to {} warehouse SMRs", origUri, ers.size());
				} else if (!ers.isEmpty()) //size==1
					return (SmallMoleculeReference) ers.iterator().next();
			}
		} else { //otherwise, use xrefs
			// try id-mapping by/from (already normalized) xrefs
			Set<String> primaryIds = idMappingByXrefs(orig, UnificationXref.class, "CHEBI");
			Set<EntityReference> ers = entRefFromWhByPrimaryId(primaryIds, "CHEBI");
			if (ers.isEmpty()) {
				//next, try - relationship xrefs
				primaryIds = idMappingByXrefs(orig, RelationshipXref.class, "CHEBI");
				ers = entRefFromWhByPrimaryId(primaryIds, "CHEBI");
			}
			if (ers.size() > 1) {
				log.debug("{}: by xrefs, ambiguously maps to {} warehouse SMRs", origUri, ers.size());
				return null;
			} else if (ers.size() == 1) {
				return (SmallMoleculeReference) ers.iterator().next();
			}
		}

		// finally, map by exact name (e.g, 'ethanol' to ChEBI ID, etc.)
		Set<String> mp = mapSmallMoleculeByExactName(orig);
		Set<EntityReference> ers = entRefFromWhByPrimaryId(mp, "CHEBI");
		if(ers.size()>1) {
			log.debug("{}: by names, ambiguously maps to {} warehouse SMRs", origUri, ers.size());
			return null;
		} else if (ers.size()==1) {
			SmallMoleculeReference smr = (SmallMoleculeReference) ers.iterator().next();
			log.warn("{} is merged by name(s) to one {}", origUri, smr.getUri());
			return smr;
		}

		// none found
		return null;
	}

	private Set<String> mapSmallMoleculeByExactName(Named el) {
		Set<String> mp = new HashSet<>(1);
		if(el instanceof SmallMolecule || el instanceof SmallMoleculeReference) {
			//find a warehouse SMR(s) with exactly the same name (case-insensitive).
			for(SmallMoleculeReference er : warehouseModel.getObjects(SmallMoleculeReference.class)) {
				for(String erName : er.getName()) {
					if(el.getName().stream().anyMatch(name -> StringUtils.equalsIgnoreCase(name, erName))) {
						//extract the ChEBI AC from the normalized SMR URI
						mp.add(CPathUtils.idFromNormalizedUri(er.getUri()));
						break;
					}
				}
			}
		}
		return mp;
	}

	/*
	 * @param primaryIds - chebi or uniprot primary IDs
	 * @param collection - 'chebi' or 'uniprot'
	 * @return set of matching ERs from the Warehouse model or empty set
	 */
	private Set<EntityReference> entRefFromWhByPrimaryId(Set<String> primaryIds, String collection) {
		Set<EntityReference> toReturn = new HashSet<>();
		for(String id : primaryIds) {
			// Normalizer.uri("", prefix, id, EntityReference.class); //alternative way (in case we generalize for more biopax types)
			String uri = Resolver.getURI(collection, id); //e.g. id can be 'CHEBI:20' or '20' (no banana)
			if(uri != null) {
				EntityReference er = (EntityReference) getWarehouseModel().getByID(uri);
				if (er != null) {
					toReturn.add(er);
				}
			}
		}
		return toReturn;
	}

}