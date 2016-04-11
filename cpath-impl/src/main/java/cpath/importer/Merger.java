/**
 ** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT). Pathway Commons.
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
 ** we have no obligations to provide maintenance,
 ** support, updates, enhancements or modifications.  In no event shall
 ** we be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** we have been advised of the possibility of such damage.
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this software; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA;
 ** or find it at http://www.fsf.org/ or http://www.gnu.org.
 **/

package cpath.importer;

import cpath.config.CPathSettings;
import cpath.dao.CPathUtils;
import cpath.importer.PreMerger.RelTypeVocab;
import cpath.jpa.Content;
import cpath.jpa.Metadata;
import cpath.service.CPathService;

import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.level3.Process;
import org.biopax.paxtools.normalizer.Normalizer;
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
import java.util.zip.GZIPInputStream;
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
		this.xmlBase = CPathSettings.getInstance().getXmlBase();
		this.supportedTaxonomyIds = CPathSettings.getInstance().getOrganismTaxonomyIds();
		
		this.warehouseModel = CPathUtils.loadWarehouseBiopaxModel();
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
		Collection<Metadata> providersMetadata = new ArrayList<Metadata>();
		
		for(Metadata metadata : service.metadata().findAll())
			providersMetadata.add(metadata);

		for (Metadata metadata : providersMetadata) {			
			if(metadata.isNotPathwayData()) {
				log.info("Skip for warehouse data: " + metadata);
				continue;
			}

			// merge all (normalized BioPAX) data files of the same provider into one-provider model:
			Model providerModel = merge(metadata);

			//export to the biopax archive in the batch downloads dir.
			save(providerModel, metadata);
			
			//merge into the main model
			log.info("Merging the integrated '" + metadata.getIdentifier() +
					"' model into the main all-providers BioPAX model...");
			
			simpleMerger.merge(mainModel, providerModel);
		}

		ModelUtils.removeObjectsIfDangling(mainModel, UtilityClass.class);

		//create or replace the main BioPAX archive
		log.info("Saving or updating the Main BioPAX file...");
		save();
		
		log.info("Complete.");
	}

	//remove bad unif. and rel. xrefs
	private void cleanupXrefs(Model m) {
		for(Xref x : new HashSet<Xref>(m.getObjects(Xref.class))) {
			if(!(x instanceof PublicationXref)) {
				//remove bad xrefs from the model and properties
				if (x.getDb() == null || x.getDb().isEmpty() || x.getId() == null || x.getId().isEmpty()) {
					m.remove(x);
					//remove from properties
					for (XReferrable owner : new HashSet<XReferrable>(x.getXrefOf())) {
						owner.removeXref(x);
					}
				} else {
					x.setDb(x.getDb().toLowerCase());
				}
			}
		}
	}

	private Model merge(Metadata metadata) {
		Model targetModel = CPathUtils.loadBiopaxModelByDatasource(metadata);
		if(targetModel == null) {
			targetModel = BioPAXLevel.L3.getDefaultFactory().createModel();
			targetModel.setXmlBase(xmlBase);

			for (Content pwdata : metadata.getContent()) {
				final String description = pwdata.toString();
				if (!new File(pwdata.normalizedFile()).exists()) {
					log.warn("Skipped " + description + " - haven't fully gone through the premerge stage yet");
					continue;
				}

				log.info("Merging: " + description);

				// import the BioPAX L3 pathway data into the in-memory paxtools model
				InputStream inputStream;
				try {
					inputStream = new GZIPInputStream(new FileInputStream(pwdata.normalizedFile()));
				} catch (IOException e) {
					log.error("Skipped " + description + " - " +
							"failed to read from " + pwdata.normalizedFile());
					continue;
				}

				Model inputModel = (new SimpleIOHandler(BioPAXLevel.L3)).convertFromOWL(inputStream);
				merge(description, inputModel, targetModel);
			}

			ModelUtils.removeObjectsIfDangling(targetModel, UtilityClass.class);

			//merge equiv. PEs within a data source (e.g., stateless vcam1 P19320 MI participants in hprd, intact, biogrid)
			log.info("Merging all equivalent physical entity groups (" + metadata.getIdentifier() + ")...");
			ModelUtils.mergeEquivalentPhysicalEntities(targetModel);

			// Replace not normalized so far URIs with generated ours; add a bp:comment about original URIs
			log.info("Replacing some original URIs with " + xmlBase + "* as needed...");
			replaceOriginalUris(targetModel, metadata.getIdentifier());

			log.info("Done merging " + metadata);
		} else {
			log.info("Loaded previously created " + metadata.getIdentifier() + " BioPAX model.");
		}

		return targetModel;
	}

	/**
	 * Exports the main model to the 'All' BioPAX archive
	 * in the cpath2 downloads (in production) or tmp (tests)
	 * directory.
	 */
	void save() {
		try {		
			new SimpleIOHandler(BioPAXLevel.L3).convertToOWL(mainModel, 
				new GZIPOutputStream(new FileOutputStream(
						CPathSettings.getInstance().mainModelFile())));
		} catch (Exception e) {
			throw new RuntimeException("Failed updating the main BioPAX archive.", e);
		}
	}

	void save(Model datasourceModel, Metadata datasource) {
		try {		
			new SimpleIOHandler(BioPAXLevel.L3).convertToOWL(datasourceModel, 
				new GZIPOutputStream(new FileOutputStream(
						CPathSettings.getInstance().biopaxExportFileName(datasource.getIdentifier()))));
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
	void merge(final String description, final Model source, final Model target) {	
		
		final String srcModelInfo = "source: " + description;

		//extra cleanup
		cleanupXrefs(mainModel);

		log.info("Searching for canonical or existing EntityReference objects " +
				" to replace equivalent original objects ("+srcModelInfo+")...");
		final Map<EntityReference, EntityReference> replacements = new HashMap<EntityReference, EntityReference>();
		// map EntityReference objects to the canonical ones (in the warehouse) if possible and safe
		for (EntityReference origEr: new HashSet<EntityReference>(source.getObjects(EntityReference.class)))
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
						for(EntityReference genericEr : new HashSet<EntityReference>(origEr.getMemberEntityReferenceOf())) {
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
		copySomeOfPropertyValues(replacements);

		filterOutUnwantedOrganismInteractions(source); //do carefully

		// cleaning up dangling objects (including the replaced above ones)
		log.info("Removing dangling objects ("+srcModelInfo+")...");
		ModelUtils.removeObjectsIfDangling(source, UtilityClass.class);
		
		/* 
		 * The following can improve graph queries and full-text search, 
		 * for generic and poorly defined physical entities (lacking entity reference)
		 * can eventually match a query.
		 * 
		 * Using existing xrefs and id-mapping, add primary uniprot/chebi RelationshipXref 
		 * to all simple PEs (SM, Protein, Dna, Rna,..) and Gene, if possible (skip Complexes).
		 * This might eventually result in mutually exclusive identifiers, 
		 * but we'll keep those xrefs and just log a warning for future (data) fix;
		 * - not a big deal as long as we do not merge data based on these new xrefs,
		 * but just index/search/query (this especially helps 
		 * when no entity references defined for a molecule).
		 */	
		log.info("Adding canonical UniProt/ChEBI RelationshipXrefs to physical"
			+ " entities by using existing xrefs and id-mapping (" + srcModelInfo + ")");
		for(Entity pe : new HashSet<Entity>(source.getObjects(Entity.class))) 
		{
			if(pe instanceof PhysicalEntity) {
				if(pe instanceof SimplePhysicalEntity) {
					// skip for SPE that got its ER just replaced (from Warehouse)
					EntityReference er = ((SimplePhysicalEntity) pe).getEntityReference();
					if(er != null && warehouseModel.containsID(er.getUri()))
						continue; //skip for just merged, canonical ERs

					if(pe instanceof SmallMolecule) {
						if(er == null)
							addCanonicalRelXrefs(target, pe, "CHEBI");
						else
							addCanonicalRelXrefs(target, er, "CHEBI");
					} else {//Protein, Dna*, Rna* type
						if(er == null)
							addCanonicalRelXrefs(target, pe, "UNIPROT");
						else 
							addCanonicalRelXrefs(target, er, "UNIPROT");
					}						
				} else if(pe instanceof Complex) {
					continue; // skip
				} else { // top PE class, i.e., pe.getModelInterface()==PhysicalEntity.class
					addCanonicalRelXrefs(target, pe, "UNIPROT");
					addCanonicalRelXrefs(target, pe, "CHEBI");
				}
			} else if(pe instanceof Gene) {
				addCanonicalRelXrefs(target, pe, "UNIPROT");
			}
		}
		
		log.info("Merging into the target one-datasource BioPAX model...");
		// merge all the elements and their children from the source to target model
		SimpleMerger simpleMerger = new SimpleMerger(SimpleEditorMap.L3, new Filter<BioPAXElement>() {		
			public boolean filter(BioPAXElement object) {
				return true; //to copy mul.card. prop. from source to target obj. having same URI
			}
		});
		simpleMerger.merge(target, source);
		log.info("Merged '" + srcModelInfo + "' model.");

	}

	private void filterOutUnwantedOrganismInteractions(Model source) {
		//remove simple MIs where all participants are not from the organisms we want (as set in the properties file)
		miLoop: for(MolecularInteraction mi : new HashSet<MolecularInteraction>(source.getObjects(MolecularInteraction.class))) {
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
			for(Entity e : new HashSet<Entity>(mi.getParticipant())) {
				mi.removeParticipant(e);
				//ok to remove from the model as well, because some may come back after merging
				//if they are still used by other entities
				source.remove(e);
			}
		}
	}


	/* 
	 * Replaces conflicting URIs in the source model
	 * by auto-generated ones (using the xml:base)
	 * and add the original URIs to bp:comment property.
	 */
	private void replaceOriginalUris(Model source, String description) {
		//wrap source.getObjects() in a new set to avoid concurrent modif. excep.
		for(BioPAXElement bpe : new HashSet<BioPAXElement>(source.getObjects())) {
			String currUri = bpe.getUri();
			BioPAXElement target = mainModel.getByID(currUri);
			// skip for some new URIs, or for already normalized or generated objects
			if( target != null && bpe.getModelInterface() != target.getModelInterface())
			{
				log.info(String.format("replaceOriginalUris: main model has %s having uri=%s, " +
					"which also belongs to %s in the source (%s).",
						target.getModelInterface(), currUri, bpe.getModelInterface(), description));
				// Generate new consistent URI for not generated not previously normalized objects:
				String newUri = Normalizer.uri(xmlBase, null, description + currUri, bpe.getModelInterface());
				// Replace URI
				CPathUtils.replaceID(source, bpe, newUri);
				// save original URI in comments
				((Level3Element) bpe).addComment("REPLACED " + currUri);
			}
			else if(target == null && currUri.contains("+")) {
				((Level3Element) bpe).addComment("REPLACED " + currUri);
				CPathUtils.replaceID(source, bpe, currUri.replaceAll("\\+","_"));
			}
		}
	}

	private void copySomeOfPropertyValues(Map<EntityReference, EntityReference> replacements) {
		// post-fix
		for (EntityReference old : replacements.keySet()) {			
			final EntityReference repl = replacements.get(old);	
			
			for (EntityFeature ef : new HashSet<EntityFeature>(old.getEntityFeature())) 
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
					for(PhysicalEntity pe : new HashSet<PhysicalEntity>(ef.getFeatureOf())) {
						pe.removeFeature(ef);
						pe.addFeature(equivEf);
					}
					for(PhysicalEntity pe : new HashSet<PhysicalEntity>(ef.getNotFeatureOf())) {
						pe.removeNotFeature(ef);
						pe.addNotFeature(equivEf);
					}	
				}
			}				

			// Copy/Keep PublicationXrefs and RelationshipXrefs to the original PEs
			// (otherwise we'd lost most of original xrefs...)
			for(Xref x : new HashSet<Xref>(old.getXref())) {
				if(x instanceof UnificationXref)
					continue;

				for(SimplePhysicalEntity owner : old.getEntityReferenceOf()) {
					owner.addXref(x);
				}
			}
		}
	}

	/**
	 * Performs id-mapping from the  
	 * unification and relationship xrefs 
	 * of a physical entity or gene to the primary/canonical
	 * id (only uniprot or chebi), creates new relationship xrefs,
	 * and adds them back to the entity.
	 * 
	 * @param m where to add new xrefs (and who's xml:base to apply for new URIs)
	 * @param bpe a {@link Gene} or {@link PhysicalEntity} or {@link EntityReference}
	 * @param db map identifiers to either CHEBI or UNIPROT IDs
	 * @throws AssertionError when bpe is neither Gene nor PhysicalEntity nor EntityReference; or - db is unsupported.
	 */
	private void addCanonicalRelXrefs(Model m, Named bpe, String db)
	{
		if(!(bpe instanceof Gene || bpe instanceof PhysicalEntity || bpe instanceof EntityReference)
				|| !("UNIPROT".equals(db) || "CHEBI".equals(db))) {
			throw new AssertionError("Either biopax type: " + bpe.getModelInterface().getSimpleName()
					+ " or db: " + db + " is not allowed here.");
		}

		Set<String> uniprots = new HashSet<String>();

		// map and generate/add xrefs
		Set<String> mappingSet = idMappingByXrefs(bpe, db, UnificationXref.class, true);
		if(db.equalsIgnoreCase("uniprot"))
			uniprots.addAll(mappingSet);
		addCanonicalRelXrefs(m, bpe, db, mappingSet, RelTypeVocab.IDENTITY);
		
		mappingSet = idMappingByXrefs(bpe, db, RelationshipXref.class, true);
		addCanonicalRelXrefs(m, bpe, db, mappingSet, RelTypeVocab.ADDITIONAL_INFORMATION);
		if(db.equalsIgnoreCase("uniprot"))
			uniprots.addAll(mappingSet);

		//map by display and standard names
		if((bpe instanceof  SmallMolecule || bpe instanceof SmallMoleculeReference)
				&& bpe.getDisplayName() != null) {
			mappingSet = service.map(null, bpe.getDisplayName(), db);
			addCanonicalRelXrefs(m, bpe, db, mappingSet, RelTypeVocab.ADDITIONAL_INFORMATION);
		}

		if(!uniprots.isEmpty()) {
			//add HGNC Symbol xrefs as well if possible
			Set<String> hgncSymbols = new HashSet<String>();
			for(String ac : uniprots) {
				ProteinReference canonicalPR = (ProteinReference) warehouseModel
						.getByID("http://identifiers.org/uniprot/" + ac);
				if(canonicalPR != null) {
					for(Xref x : canonicalPR.getXref()) {
						if(x.getDb().equalsIgnoreCase("hgnc symbol"))
							hgncSymbols.add(x.getId());
					}
				}
			}
			addCanonicalRelXrefs(m, bpe, "hgnc symbol", hgncSymbols, RelTypeVocab.ADDITIONAL_INFORMATION);
		}
	}

	/**
	 * Finds or creates relationship xrefs
	 * using id-mapping results;
	 * adds them to the object and model.
	 * 
	 * @param model a biopax model where to find/create xrefs
	 * @param bpe a gene, physical entity or entity reference only
	 * @param db database name for all (primary/canonical) xrefs; 'uniprot' or 'chebi'
	 * @param mappingSet
	 * @param relType - term to use with the Xref
	 * @throws AssertionError when bpe is neither Gene nor PhysicalEntity nor EntityReference
	 */
	private void addCanonicalRelXrefs(Model model, XReferrable bpe, String db, Set<String> mappingSet, RelTypeVocab relType)
	{	
		if(!(bpe instanceof Gene || bpe instanceof PhysicalEntity || bpe instanceof EntityReference))
			throw new AssertionError("addRelXrefs: not a Gene, ER, or PE: " + bpe.getUri());
		
		ac: for(String ac : mappingSet) {
			// find or create
			RelationshipXref rx = PreMerger.findOrCreateRelationshipXref(relType, db, ac, model);
			
			//check if an equivalent rel. xref is already present (skip it then)
			for(Xref x : bpe.getXref()) {
				if(x instanceof RelationshipXref && x.isEquivalent(rx))
					continue ac; //break and go to next ac
			}
			
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
		final String canonicalPrefix = standardPrefix + "uniprot/";
		final String origUri = orig.getUri();
		
		// Try to re-use existing object
		if(origUri.startsWith(canonicalPrefix)) {
			ProteinReference toReturn = (ProteinReference) warehouseModel.getByID(origUri);
			if(toReturn != null)
				return toReturn;
		}
 
		// If nothing's found by URI so far,
		if (origUri.startsWith(standardPrefix)) {
			// try id-mapping to UniProt AC using the ID part of the normalized URI
			String id = origUri.substring(origUri.lastIndexOf('/')+1);	
			String db = null;				
			//a hack/shortcut for normalized PRs
			if(origUri.toLowerCase().contains("uniprot.isoform")) {
				db = "uniprot isoform";
			} else if(origUri.toLowerCase().contains("uniprot")) {
				db = "uniprot";
			} else if(origUri.toLowerCase().contains("refseq")) {
				db = "refseq";
			} else if(origUri.toLowerCase().contains("kegg") && id.contains(":")) {
				db = "ncbi gene"; //KEGG actually uses NCBI Gene (aka Entrez Gene)
			}
			if(db == null) 
				db = dbById(id, orig.getXref());	

			Set<String> mp = service.map(db, id, "UNIPROT");
			Set<EntityReference> ers = findEntityRefUsingIdMappingResult(mp, canonicalPrefix);
			if(ers.size()>1) {
				log.warn(origUri + " maps to multiple warehouse ERs: " + ers);
				return null;
			} else if (ers.size()==1)
				return (ProteinReference) ers.iterator().next();
		}
				
		// if still nothing came out yet, try id-mapping by (already normalized) Xrefs:
		Set<EntityReference> ers = mapByXrefs(orig, "UNIPROT", canonicalPrefix);
		if(ers.size()>1) {
			log.debug(origUri + ": by Xrefs, ambiguously maps to " + ers.size() + " warehouse PRs");
			return null;
		} else if (ers.size()==1)
			return (ProteinReference) ers.iterator().next();

		// mapping/merging proteins by names is too risky, even when unambiguous (quite unlikely); so we won't do.

		//nothing found
		return null;
	}

	private Set<String> idMappingByXrefs(final XReferrable orig,
			String mapTo, final Class<? extends Xref> xrefType, boolean isUnion) 
	{
		return (isUnion) ? idMappingByXrefsUnion(orig, mapTo, xrefType)
				: idMappingByXrefsIntersection(orig, mapTo, xrefType);
	}

	private Set<String> idMappingByXrefsIntersection(final XReferrable orig,
			String mapTo, final Class<? extends Xref> xrefType) 
	{
		Set<String> xSet = new HashSet<String>();
		
		for (Xref x : orig.getXref()) {			
			if(x.getDb() == null || x.getDb().isEmpty() || x.getId() == null || x.getId().isEmpty()) {
				log.warn("Ignored bad " + xrefType.getSimpleName()
					+ " (" + x.getUri() + "), db: " + x.getDb() + ", id: " + x.getId());
				continue;
			}
						
			if (xrefType.isInstance(x)) {
				Set<String> mp = service.map(x.getDb(), x.getId(), mapTo);
				if(mp==null || mp.isEmpty()) 
					continue; //ignore xrefs that don't map to any primary IDs
				
				// mp is not empty
				if(mp.size() > 1) 
					log.debug("Ambiguous xref.id: " + x.getId() + " maps to: " + mp);
				
				if(!xSet.isEmpty()) {//and mp is not empty too -
					mp.retainAll(xSet);
					if(mp.isEmpty()) {
						//quit w/o trying other xrefs due to apparently empty intersection
						xSet.clear(); //refs issue #224
						break;
					}
				} 
				
				xSet = mp; //xSet now contains not empty intersection
			}
		}

		if(xSet.isEmpty())
			return Collections.EMPTY_SET;
		else if(xSet.size()>1)
			return new TreeSet<String>(xSet);
		else {//size == 1
			if(log.isDebugEnabled())
				log.debug(orig.getUri() + ", using its " + xrefType.getSimpleName()
					+ "s, distinctly maps to: " + xSet.iterator().next());
			return xSet;
		}
	}

	private Set<String> idMappingByXrefsUnion(final XReferrable orig,
			String mapTo, final Class<? extends Xref> xrefType) 
	{
		final Set<String> mappedTo = new TreeSet<String>();
		
		for (Xref x : orig.getXref()) {			
			if(x.getDb() == null || x.getDb().isEmpty()
					|| x.getId() == null || x.getId().isEmpty()) {
				log.warn("Ignored bad " + xrefType.getSimpleName() + ", " + x.getUri());
				continue;
			}
						
			if (xrefType.isInstance(x)) {
				Set<String> mp = service.map(x.getDb(), x.getId(), mapTo);
				mappedTo.addAll(mp);
				if(mp.size() > 1) //one xref maps to several primary ACs
					log.debug("Ambiguous xref.id: " + x.getId() + " maps to: " + mp);
			}
		}

		return mappedTo;
	}

	/*
	 * Finds a {@link UnificationXref} by id 
	 * and returns its db value or null.
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
	 * @param orig original reference small molecule
	 * @return the replacement standard object or null if not found/matched
	 */
	private SmallMoleculeReference findSmallMoleculeReferenceInWarehouse(final SmallMoleculeReference orig)
	{
		final String standardPrefix = "http://identifiers.org/";
		final String canonicalPrefix = standardPrefix + "chebi/";
		final String origUri = orig.getUri();
		
		// Try to re-use existing object
		if(origUri.startsWith(canonicalPrefix)) {
			SmallMoleculeReference toReturn = (SmallMoleculeReference) warehouseModel.getByID(origUri);
			if(toReturn != null)
				return toReturn;
		}

		// If nothing's found by URI, try id-mapping of the normalized URI part to chebi ID
		if (origUri.startsWith(standardPrefix)) {
			String id = origUri.substring(origUri.lastIndexOf('/')+1);
			String db = dbById(id, orig.getXref()); //find by id			
			Set<String> mp = service.map(db, id, "CHEBI");
			Set<EntityReference> ers = findEntityRefUsingIdMappingResult(mp, canonicalPrefix);
			if(ers.size()>1)
				log.warn(origUri + ", using canonical URI (ID part), maps to " + ers.size() + " canonical ChEBI SMRs");
			else if (!ers.isEmpty()) //size==1
				return (SmallMoleculeReference) ers.iterator().next();
		}

		// if so far the mapping there was either ambiguous or got nothing,
		// try id-mapping by (already normalized) Xrefs:
		Set<EntityReference> ers = mapByXrefs(orig, "CHEBI", canonicalPrefix);
		if(ers.size()>1) {
			log.debug(origUri + ": by Xrefs, ambiguously maps to " + ers.size() + " warehouse SMRs");
			return null;
		} else if (ers.size()==1)
			return (SmallMoleculeReference) ers.iterator().next();

		// nothing? - keep trying, map by name (e..g, 'ethanol') to ChEBI ID
		Set<String> mp = mapSmallMoleculeByExactName(orig);
		ers = findEntityRefUsingIdMappingResult(mp, canonicalPrefix);
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

	private Set<EntityReference> mapByXrefs(EntityReference orig, String dest, String canonicalUriPrefix) {

		//map by unification xrefs
		Set<String> mappingSet = idMappingByXrefs(orig, dest, UnificationXref.class, false);
		Set<EntityReference> mapsTo = findEntityRefUsingIdMappingResult(mappingSet, canonicalUriPrefix);

		if(mapsTo.isEmpty()) {
			//next, try - relationship xrefs
			mappingSet = idMappingByXrefs(orig, dest, RelationshipXref.class, false);
			mapsTo = findEntityRefUsingIdMappingResult(mappingSet, canonicalUriPrefix);
		}

		return mapsTo; //can be more than one, but then we won't merge the original ER
	}

	private Set<String> mapSmallMoleculeByExactName(Named el) {
		Set<String> mp = new HashSet<String>();

		// save all the names in a different Set:
		final Set<String> names = new HashSet<String>();
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
		Set<EntityReference> toReturn = new HashSet<EntityReference>();
		
		for(String id : mapsTo) {		
			String uri = uriPrefix + id;
			EntityReference er = (EntityReference) warehouseModel.getByID(uri);
			if(er != null)
				toReturn.add(er);
		}
		
		return toReturn;
	}
}