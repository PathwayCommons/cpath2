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
				return object instanceof EntityReference || object instanceof Pathway;
				//though, normally, pathways in different source models have different URIs and never merge/clash...
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

			//merge equiv. PEs within a data source (e.g., stateless vcam1 P19320 MI participants in hprd, intact, biogrid)
			log.info("Merging all equivalent physical entity groups (" + metadata.getIdentifier() + ")...");
			ModelUtils.mergeEquivalentPhysicalEntities(providerModel);

			//export to the biopax archive in the batch downloads dir.
			save(providerModel, metadata);
			
			//merge into the main model
			log.info("Merging the integrated '" + metadata.getIdentifier() +
					"' model into the main all-providers BioPAX model...");
			
			simpleMerger.merge(mainModel, providerModel);
		}
		
		//extra cleanup
		cleanupXrefs(mainModel);

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
				}
			}
		}
	}

	private Model merge(Metadata metadata) {
		log.info("Begin merging warehouse data with pathway data from  " + metadata);
		
		Model targetModel = BioPAXLevel.L3.getDefaultFactory().createModel();
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
			
		cleanupXrefs(targetModel);
		ModelUtils.removeObjectsIfDangling(targetModel, UtilityClass.class);
		
		log.info("Done merging " + metadata);
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
		
		//First, convert all Xref.db values to lower case
		//...the Normalized must have already done so; anyway...
		for(Xref x : source.getObjects(Xref.class))
			if(x.getDb()!=null) x.setDb(x.getDb().toLowerCase());

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

		//This improves our graph queries results and simple format output:
		addMoreXrefsByMapping(source, srcModelInfo, target);

		// Replace all not normalized so far URIs in the source model
		// with auto-generated new short ones (also add a bp:comment about original URIs)
		log.info("Assigning new URIs (xml:base=" + xmlBase + 
				"*) to all not normalized BioPAX elements (" + 
				srcModelInfo + ", xml:base=" + source.getXmlBase() + ")...");
		replaceOriginalUris(source, description, target);
		
		log.info("Merging into the target one-datasource BioPAX model...");
		// merge all the elements and their children from the source to target model
		SimpleMerger simpleMerger = new SimpleMerger(SimpleEditorMap.L3, new Filter<BioPAXElement>() {		
			public boolean filter(BioPAXElement object) {
				return object instanceof EntityReference
						|| object instanceof Pathway
							|| object instanceof SimplePhysicalEntity;
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
	* The following can improve our export to simple text formats:
	* physical entities that lack entity reference, are generic, or
	* have no uniprot/chebi/hgnc, or have ambiguous IDs
	* can in some cases contribute to the results.
	*
	* This might eventually result in mutually exclusive identifiers,
	* which is not a big deal as long as we do not merge things based on these new xrefs,
	* but just index/search/query the db (this especially helps when no entity references defined
	* for a molecule or when id-mapping is ambiguous).
	*/
	private void addMoreXrefsByMapping(Model source, String srcModelInfo, Model target) {
		log.info("Using original xrefs or names and id-mapping, add UniProt/ChEBI/HGNC xrefs " +
				" (unless they're too many) to not-merged PE/ERs (" + srcModelInfo + ")");
		for(Entity pe : new HashSet<Entity>(source.getObjects(Entity.class)))
		{
			if(pe instanceof PhysicalEntity) {
				if(pe instanceof SimplePhysicalEntity) {
					// Skip for SPE that got its ER just replaced with an ER from the Warehouse
					EntityReference er = ((SimplePhysicalEntity) pe).getEntityReference();
					if(er != null && warehouseModel.containsID(er.getUri()))
						continue; //skip for just merged, canonical ERs

					if(pe instanceof SmallMolecule) {
						mayAddUniprotHgncOrChebiXrefs(target, pe, "CHEBI", 6);
					} else {//Protein, Dna*, Rna* type
						mayAddUniprotHgncOrChebiXrefs(target, pe, "UNIPROT", 12);
					}
				} else { // top PE class, i.e., pe.getModelInterface()==PhysicalEntity.class
					mayAddUniprotHgncOrChebiXrefs(target, pe, "UNIPROT", 12);
					mayAddUniprotHgncOrChebiXrefs(target, pe, "CHEBI", 6);
				}
			} else if(pe instanceof Gene || pe instanceof Complex) {
				mayAddUniprotHgncOrChebiXrefs(target, pe, "UNIPROT", 12);
			}
		}
	}


	/* 
	 * Replace all not PC2 URIs in the source model
	 * with new auto-generated ones using the PC2 xml:base
	 * (also add a bp:comment about previous URIs)
	 */
	private void replaceOriginalUris(Model source, String description, Model target) {
		//wrap source.getObjects() in a new set to avoid concurrent modif. excep.
		for(BioPAXElement bpe : new HashSet<BioPAXElement>(source.getObjects())) {
			String currUri = bpe.getUri();

			// skip for some previously normalized or generated objects
			if(	currUri.startsWith(xmlBase)
				|| (bpe instanceof PublicationXref && currUri.startsWith("http://identifiers.org/pubmed"))
				|| (currUri.startsWith("http://identifiers.org/") &&
					(	bpe instanceof Process
						|| bpe instanceof ProteinReference
						|| bpe instanceof SmallMoleculeReference
						//or BioSource if both tissue and cellType are not defined -
						|| (bpe instanceof BioSource && ((BioSource)bpe).getTissue()==null && ((BioSource)bpe).getCellType()==null)
					)
				)
			){continue;}

			// Generate new consistent URI for not generated not previously normalized objects:
			String newUri = Normalizer.uri(xmlBase, null, description + currUri, bpe.getModelInterface());
			
			// Replace URI
			CPathUtils.replaceID(source, bpe, newUri);
			// save original URI in comments
			((Level3Element) bpe).addComment("REPLACED " + currUri);
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

	/*
	 * Using the unification and relationship xrefs of a physical entity or gene,
	 * performs id-mapping to the primary canonical ID (can only be uniprot or chebi),
	 * creates relationship xrefs and adds them back to the entity.
	 *
	 * This step won't much improve full-text index/search and graph queries
	 * (where id-mapping is used again anyway), but may help improve export to SIF and GSEA formats.
	 * This method is called only for original PEs or their ERs that were not mapped/merged
	 * with a warehouse canonical ERs for various known reasons (no match for a ID or no ID, ambiguous ID, etc.)
	 *
	 * This method won't add additional xrefs if a UniProt/ChEBI/HGNC one is already present despite it'd map
	 * to many canonical ERs/IDs (in fact, it'd even map to hundreds (Trembl) IDs, e.g., in cases like 'ND5',
	 * and cause our export to the SIF, GSEA formats fail...)
	 */
	private void mayAddUniprotHgncOrChebiXrefs(final Model m, Named bpe, final String db, final int maxNumXrefsToAdd)
	{
		if(!(bpe instanceof Gene || bpe instanceof PhysicalEntity) || !("UNIPROT".equals(db) || "CHEBI".equals(db)))
		{
			throw new AssertionError("Either biopax type: " + bpe.getModelInterface().getSimpleName()
					+ " or db: " + db + " is not allowed here.");
		}

		// try/prefer to use ER instead of entity -
		if(bpe instanceof SimplePhysicalEntity) {
			EntityReference er = ((SimplePhysicalEntity) bpe).getEntityReference();
			if(er != null && !er.getXref().isEmpty())
				bpe = er;
		}

		//shortcut
		if(bpe.getXref().isEmpty() && bpe.getName().isEmpty()) {
			if(bpe instanceof Complex
				|| (bpe instanceof PhysicalEntity && !((PhysicalEntity)bpe).getMemberPhysicalEntity().isEmpty())
				|| (bpe instanceof EntityReference && !((EntityReference)bpe).getMemberEntityReference().isEmpty())
			) //if complex or generic PE or ER -
				log.debug("mayAddUniprotHgncOrChebiXrefs(): GENERIC " + bpe.getModelInterface().getSimpleName()
					+ " (" + bpe.getUri() + ") has neither xrefs nor any names.");
			else
				log.warn("mayAddUniprotHgncOrChebiXrefs(): NOT generic " + bpe.getModelInterface().getSimpleName()
						+ " (" + bpe.getUri() + ") has no xrefs/names!");

			return;
		}

		String organismRemark = " (organism=";
		if(bpe instanceof SequenceEntityReference)
			organismRemark += String.valueOf(((SequenceEntityReference)bpe).getOrganism()) + ")";
		else if(bpe instanceof Gene)
			organismRemark += String.valueOf(((Gene)bpe).getOrganism()) + ")";
		else
			organismRemark = "";

		if(!xrefsContainDb(bpe, db)) { //bpe does not have any uniprot or chebi xrefs
			// map other IDs and names to the primary IDs of ERs that can be found in the Warehouse
			Set<String> primaryIds = idMappingByXrefs(bpe, db, UnificationXref.class);
			if (primaryIds.isEmpty())
				primaryIds.addAll(idMappingByXrefs(bpe, db, RelationshipXref.class));
			// if none found, try (map) by names; but -
			// e.g, 'HLA DQB1' (HPRD_05054) protein gets >200 uniprot xrefs if mapped by names...
			if (primaryIds.isEmpty()
			//		&& (bpe instanceof SmallMolecule || bpe instanceof SmallMoleculeReference)
			) { primaryIds.addAll(mapByExactName(bpe)); }

			// add rel. xrefs if there are not too many (there's risk to make nonsense SIF/GSEA export...)
			else if(!primaryIds.isEmpty() && primaryIds.size() <= maxNumXrefsToAdd) {
				addRelXrefs(m, bpe, db, primaryIds, RelTypeVocab.ADDITIONAL_INFORMATION);
				// for biopolymers, also map uniprot IDs to HGNC Symbols (and corresp. xrefs) if possible
				if(db.equals("UNIPROT") && !xrefsContainDb(bpe, "hgnc"))
					mayAddHgncXrefs(m, bpe, primaryIds, maxNumXrefsToAdd);
			} else {
				log.info("mayAddUniprotHgncOrChebiXrefs(), " + bpe.getUri()  + organismRemark +
						", using xrefs/names gets too many (" + primaryIds.size() + ") primary " + db +
					" IDs by id-mapping; so, won't add any more rel. xrefs here...");
			}
		}
		else if (db.equals("UNIPROT") && !xrefsContainDb(bpe, "hgnc")) {
			//but it has some uniprot xrefs (can be secondary or isoform IDs);
			// let's try to map those to HGNC Symbols
			final Collection<String> uniprotIds = new HashSet<String>();
			for(Xref x : bpe.getXref()) {
				if(!(x instanceof PublicationXref)) {
					if (x.getDb() != null && x.getId() != null && x.getDb().toLowerCase().startsWith("uniprot")) {
						//map the uniprot ID (x.getId()) to primary accessions, collect them
						String id = CPathUtils.fixSourceIdForMapping(x.getDb(), x.getId());
						uniprotIds.add(id);
					}
				}
			}
			//perform id-mapping to primary uniprot accessions
			if(uniprotIds.isEmpty())
				log.warn("idMappingByXrefs, no uniprot accessions collected from " + bpe.getUri() + organismRemark);
			else {
				final Collection<String> primaryACs = service.map(uniprotIds, "UNIPROT");
				// map primary ACs to HGNC Symbols and generate RXs if there're not too many...
				mayAddHgncXrefs(m, bpe, primaryACs, maxNumXrefsToAdd);
			}
		}
	}

	// For biopolymers, also map uniprot accessions to HGNC Symbols, and add the xrefs, if possible -
	private void mayAddHgncXrefs(final Model m, final XReferrable bpe,
								 final Collection<String> accessions, final int maxNumXrefsToAdd)
	{
		final Set<String> hgncSymbols = new HashSet<String>();
		for (String ac : accessions) {
			ProteinReference canonicalPR =
					(ProteinReference) warehouseModel.getByID("http://identifiers.org/uniprot/" + ac);
			if (canonicalPR != null)
				for (Xref x : canonicalPR.getXref())
					if (x.getDb().equalsIgnoreCase("hgnc symbol"))
						hgncSymbols.add(x.getId());
		}
		// add rel. xrefs if there are not too many (there's risk to make nonsense SIF/GSEA export...)
		if (!hgncSymbols.isEmpty() && hgncSymbols.size() <= maxNumXrefsToAdd)
			addRelXrefs(m, bpe, "hgnc symbol", hgncSymbols, RelTypeVocab.ADDITIONAL_INFORMATION);
	}

	private boolean xrefsContainDb(XReferrable xr, String db)
	{
		db = db.toLowerCase();
		for(Xref x : xr.getXref())
		{
			if (!(x instanceof PublicationXref) && x.getDb()!=null && x.getId()!=null
					&& x.getDb().toLowerCase().startsWith(db)) {
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
	 * @throws AssertionError when bpe is neither Gene nor PhysicalEntity nor EntityReference
	 */
	private void addRelXrefs(Model model, XReferrable bpe, String db, Collection<String> accessions, RelTypeVocab relType)
	{	
		if(!(bpe instanceof Gene || bpe instanceof PhysicalEntity || bpe instanceof EntityReference))
			throw new AssertionError("addRelXrefs: not a Gene, ER, or PE: " + bpe.getUri());
		
		ac: for(String ac : accessions) {
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

	private Set<String> idMappingByXrefs(XReferrable orig, String mapTo, Class<? extends Xref> xrefType)
	{
		//this method is to be called for a Gene, Complex, EntityReference
		// - or a simple PEs that have no ER or its ER has no xrefs.
		Assert.isTrue(
			(orig instanceof Gene || orig instanceof PhysicalEntity || orig instanceof EntityReference)
			&&
			(!(orig instanceof SimplePhysicalEntity) || ((SimplePhysicalEntity)orig).getEntityReference()==null
				|| ((SimplePhysicalEntity)orig).getEntityReference().getXref().isEmpty())
		);

		final Set<String> sourceIds = new HashSet<String>();
		for (Xref x : orig.getXref()) {
			if(x instanceof PublicationXref)
				continue;
			if(x.getDb() == null || x.getDb().isEmpty() || x.getId() == null || x.getId().isEmpty()) {
				log.debug("Ignored malformed " + xrefType.getSimpleName() + ";" + x.getUri());
				continue;
			}
			if (xrefType.isInstance(x)) {
				String id = CPathUtils.fixSourceIdForMapping(x.getDb(), x.getId());
				sourceIds.add(id);
			}
		}

		if(sourceIds.isEmpty()) {
			final String org = (orig instanceof SequenceEntityReference)
				? "organism: " + String.valueOf(((SequenceEntityReference)orig).getOrganism()) : "";
			final boolean isGeneric = orig instanceof Complex
				|| (orig instanceof PhysicalEntity && !((PhysicalEntity)orig).getMemberPhysicalEntity().isEmpty())
				|| (orig instanceof EntityReference && !((EntityReference)orig).getMemberEntityReference().isEmpty());

			final String msg = "idMappingByXrefs, no " + xrefType.getSimpleName() +
					" xref IDs for mapping: " + ((isGeneric)?"GENERIC ":"") + orig.getModelInterface().getSimpleName() +
					" (" + orig.getUri() + "); " + org;

			if(isGeneric) //usually, no IDs is no surprise for generic/complex entity case...
				log.debug(msg);
			else //warning can help improve input data (provider should have had some gene/chem xrefs there)
				log.info(msg);

			return Collections.emptySet();
		}

		// do id-mapping, for all ids at once, and return the result set
		return service.map(sourceIds, mapTo);
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
		Set<String> mp = mapByExactName(orig);
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
		Set<String> mappingSet = idMappingByXrefs(orig, dest, UnificationXref.class);
		Set<EntityReference> mapsTo = findEntityRefUsingIdMappingResult(mappingSet, canonicalUriPrefix);

		if(mapsTo.isEmpty()) {
			//next, try - relationship xrefs
			mappingSet = idMappingByXrefs(orig, dest, RelationshipXref.class);
			mapsTo = findEntityRefUsingIdMappingResult(mappingSet, canonicalUriPrefix);
		}

		return mapsTo; //can be more than one, but then we won't merge the original ER
	}

	private Set<String> mapByExactName(Named el) {
		Set<String> mp = new TreeSet<String>();

		// save all the names, turning them to lower case, in a different Set:
		final Set<String> names = new HashSet<String>();
		for(String n : el.getName())
			names.add(n.toLowerCase());

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
		} else if(el instanceof Gene
				|| el instanceof SequenceEntityReference
				|| el instanceof SimplePhysicalEntity //except SmallMolecule (it would satisfy the above 'if')
				|| el.getModelInterface().equals(PhysicalEntity.class)) {
			//consider a bio-polymer, map by names to warehouse sequence ERs (currently, only PRs) to collect uniprot IDs
			for(SequenceEntityReference er : warehouseModel.getObjects(SequenceEntityReference.class))
			{
				for(String s : er.getName()) {
					if(names.contains(s.toLowerCase())) {
						//extract the UniProt accession from URI, add
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