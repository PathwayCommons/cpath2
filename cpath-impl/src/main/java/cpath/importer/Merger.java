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

package cpath.importer;

import cpath.config.CPathSettings;
import cpath.dao.CPathUtils;
import cpath.importer.PreMerger.RelTypeVocab;
import cpath.jpa.Content;
import cpath.jpa.Metadata;
import cpath.service.CPathService;

import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.Complex;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.EntityFeature;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.Gene;
import org.biopax.paxtools.model.level3.Level3Element;
import org.biopax.paxtools.model.level3.Named;
import org.biopax.paxtools.model.level3.Pathway;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.PublicationXref;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;
import org.biopax.paxtools.model.level3.SmallMolecule;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.UtilityClass;
import org.biopax.paxtools.model.level3.XReferrable;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.paxtools.util.Filter;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.io.*;
import org.biopax.validator.utils.Normalizer;
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
 */
public final class Merger {

    private static final Logger log = LoggerFactory.getLogger(Merger.class);
	
    // configuration/flags
	private final boolean force;	
	private final String xmlBase;

    private Model warehouseModel;
    private Model mainModel;      
	private CPathService service;

	/**
	 * Constructor.
	 * 
	 * @param service
	 * @param force whether to forcibly merge BioPAX data the validation reported critical about or skip.
	 * @throws AssertionError when dest is not instanceof {@link Model};
	 */
	public Merger(CPathService service, boolean force) 
	{
		this.service = service;
		this.xmlBase = CPathSettings.getInstance().getXmlBase();
		this.force = force;		
		
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
	 * @return
	 */
	public Model getMainModel() {
		return mainModel;
	}
	
	public void merge() {
		//using a SimpleMerger with Filter (ER) here (to merge ent. features, xrefs, etc.)
		SimpleMerger simpleMerger = new SimpleMerger(SimpleEditorMap.L3, new Filter<BioPAXElement>() {		
			public boolean filter(BioPAXElement object) {
				return object instanceof EntityReference;
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
			
			Model datasourceResultModel = merge(metadata);
			
			//export to the biopax archive in the batch downloads dir.
			save(datasourceResultModel, metadata);
			
			//merge into the main model
			log.info("Merging the updated and enriched '" + metadata.getIdentifier() + 
					"' model into the main all-data BioPAX model...");
			
			simpleMerger.merge(mainModel, datasourceResultModel);
		}
		
		//extra cleanup
		cleanupXrefs(mainModel);
		
		//create or replace the main BioPAX archive
		log.info("Saving or updating the Main BioPAX file...");
		save();
		
		log.info("Complete.");
	}

	//remove bad unif. and rel. xrefs
	private void cleanupXrefs(Model m) {
		for(Xref x : new HashSet<Xref>(m.getObjects(Xref.class))) {
			if(x instanceof PublicationXref)
				continue;
			if(x.getDb()==null || x.getDb().isEmpty() 
				|| x.getId()==null || x.getId().isEmpty())
					m.remove(x);
		}
	}

	private Model merge(Metadata metadata) {
		log.info("Begin merging warehouse data with pathway data from  " + metadata);
		
		Model targetModel = BioPAXLevel.L3.getDefaultFactory().createModel();
		targetModel.setXmlBase(xmlBase);
		
		for (Content pwdata : metadata.getContent()) {
			final String description = pwdata.toString();
			if (pwdata.getValid() == null) {
				log.warn("Skipped " + description + " - haven't gone through the premerge yet");
				continue;
			} else if (pwdata.getValid() == false) {
				// has BioPAX errors
				log.warn("There were critical BioPAX errors in - " + description);
				if (!force) {
					log.warn("Skipped " + description + " (due to BioPAX errors)");
					continue;
				} else {
					log.warn("FORCE merging " + description + " (ignoring BioPAX errors)");
				}
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
			merge(pwdata.toString(), inputModel, targetModel);
		}
			
		cleanupXrefs(targetModel);
		
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
	 * @param source
	 * @param target
	 */
	void merge(final String description, final Model source, final Model target) {	
		
		final String srcModelInfo = "source: " + description;
		
		log.info("Converting all Xref.db values to upper case (" + srcModelInfo + "; "
				+ "helps when merging xrefs...");
		for(Xref x : source.getObjects(Xref.class))
			if(x.getDb()!=null) x.setDb(x.getDb().toUpperCase());
			
		log.info("Searching for canonical or existing EntityReference objects " +
				" to replace equivalent original objects ("+srcModelInfo+")...");
		final Map<EntityReference, EntityReference> replacements = new HashMap<EntityReference, EntityReference>();		
		// match some UtilityClass objects to existing ones (previously imported, warehouse)
		for (EntityReference bpe: new HashSet<EntityReference>(source.getObjects(EntityReference.class))) 
		{
			EntityReference replacement = null;
			
			// Find the best replacement ER in the Warehouse:
			if (bpe instanceof ProteinReference) {
				replacement = findOrCreateProteinReference((ProteinReference)bpe);
			} 
			else if (bpe instanceof SmallMoleculeReference) {
				replacement = findOrCreateSmallMoleculeReference((SmallMoleculeReference)bpe);
			} 
				
			if (replacement != null) {
				EntityReference r = (EntityReference) target.getByID(replacement.getRDFId());
				if(r != null) // re-use previously merged one
					replacement = r;
				
				//save in the map to replace the source bpe later 
				replacements.put(bpe, replacement);
				
				if(!replacement.getRDFId().equals(bpe.getRDFId()))
					replacement.addComment("REPLACED " + bpe.getRDFId());
			}
		}
		
		//explicitly remove old (to be replaced) objects from the source model
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
					if(er != null && warehouseModel.containsID(er.getRDFId()))
						continue;
					
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
		
		/* 
		 * Replace all not normalized so far URIs in the source model 
		 * with auto-generated new short ones (also add a bp:comment about original URIs)
		 */
		log.info("Assigning new URIs (xml:base=" + xmlBase + 
				"*) to all not normalized BioPAX elements (" + 
				srcModelInfo + ", xml:base=" + source.getXmlBase() + ")...");
		replaceTheRestOfUris(source, description, target);
		
		log.info("Merging into the target one-datasource BioPAX model...");
		// merge all the elements and their children from the source to target model
		SimpleMerger simpleMerger = new SimpleMerger(SimpleEditorMap.L3, new Filter<BioPAXElement>() {		
			public boolean filter(BioPAXElement object) {
				return object instanceof EntityReference
						|| object instanceof Pathway;
			}
		});
		simpleMerger.merge(target, source);
		
		log.info("Merge ("+srcModelInfo+") is done.");
	}

	/* 
	 * Replace all not normalized so far URIs in the source model 
	 * with auto-generated new short ones (also add a bp:comment about original URIs)
	 */
	private void replaceTheRestOfUris(Model source, String description, Model target) {
		//wrap source.getObjects() in a new set to avoid concurrent modif. excep.
		for(BioPAXElement bpe : new HashSet<BioPAXElement>(source.getObjects())) {
			String currUri = bpe.getRDFId();
			
			// skip for previously normalized objects (we believe they are right and ready to merge)
			if(currUri.startsWith(xmlBase) || currUri.startsWith("http://identifiers.org/")) 
				continue; 
			
			// Generate new consistent URI for not generated not previously normalized objects:
			String newRDFId = Normalizer.uri(xmlBase, null, currUri, bpe.getModelInterface());
			
			// Avoid same URI - non-equivalent objects case/clash 
			// (some providers might mistakenly use the same URI 
			// for several non-equivalent biopax entities in different files):
			if(target.containsID(newRDFId)) {
				if(!bpe.isEquivalent(target.getByID(newRDFId))) {
					newRDFId = Normalizer.uri(xmlBase, null, description+currUri, bpe.getModelInterface());
					log.warn(description + " has a " + bpe.getModelInterface().getSimpleName() + 
						", uri: " + currUri + ", which also was the original URI of a " + 
						"not equivalent, previously merged object; fixed.");
				}
			}
			
			// Replace URI
			CPathUtils.replaceID(source, bpe, newRDFId);
			
			// save original URI in comments
			((Level3Element) bpe).addComment("REPLACED " + currUri);
		}
	}

	private void copySomeOfPropertyValues(
			Map<EntityReference, EntityReference> replacements) {
		// post-fix
		for (EntityReference old : replacements.keySet()) {			
			final EntityReference repl = replacements.get(old);	
			
			for (EntityFeature ef : new HashSet<EntityFeature>(old.getEntityFeature())) 
			{ // move entity features of the replaced ER to the new canonical one
				// remove the ef from the old ER
				old.removeEntityFeature(ef);
				// now, this ef should not belong to any other ER (no entityFeature can contain this ef, for all ERs)
				//TODO ideally, we'd check for all the ERs in both the original and target models, and none should contain this ef.
											
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
			// move new PublicationXrefs and RelationshipXrefs (otherwise we lost some of original xrefs...)
			for(Xref x : new HashSet<Xref>(old.getXref())) {
				if(!(x instanceof UnificationXref)) {
					((XReferrable) old).removeXref(x);				
					Xref equivX = null;
					for(Xref y : repl.getXref()) {
						if(y.isEquivalent(x)) {
							equivX = y;
							break;
						}
					}
					//if the repl. ER has neither same-id xrefs nor equivalent ones, add x:
					if(!repl.getXref().contains(x) && equivX == null)
							repl.addXref(x);
				}
			}
		}
	}


	/**
	 * Performs id-mapping from the  
	 * unification and relationship xrefs 
	 * of a physical entity or gene to the primary/canonical
	 * id (uniprot or chebi), creates new relationship xrefs,
	 * and adds them back to the entity.
	 * 
	 * @param m where to add new xrefs (and who's xml:base to apply for new URIs)
	 * @param bpe a {@link Gene} or {@link PhysicalEntity}
	 * @param db map identifiers to (e.g., CHEBI, UNIPROT)
	 * @throws AssertionError when bpe is neither Gene nor PhysicalEntity
	 */
	private void addCanonicalRelXrefs(Model m, Named bpe, String db) 
	{
		if(!(bpe instanceof Gene || bpe instanceof PhysicalEntity || bpe instanceof EntityReference))
			throw new AssertionError("Not Gene or PE: " + bpe);
			
		// map and generate/add xrefs
		Set<String> mappingSet = idMappingByXrefs(bpe, db, UnificationXref.class, true);
		addCanonicalRelXrefs(m, bpe, db, mappingSet, RelTypeVocab.IDENTITY);
		
		mappingSet = idMappingByXrefs(bpe, db, RelationshipXref.class, true);
		addCanonicalRelXrefs(m, bpe, db, mappingSet, RelTypeVocab.ADDITIONAL_INFORMATION);
		
		//map by display and standard names
		if(bpe.getDisplayName()!=null && !bpe.getDisplayName().isEmpty()) {
			mappingSet = service.map(null, bpe.getDisplayName(), db);
			addCanonicalRelXrefs(m, bpe, db, mappingSet, RelTypeVocab.ADDITIONAL_INFORMATION);
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
			throw new AssertionError("Not Gene or PE: " + bpe);
		
		ac: for(String ac : mappingSet) {
			// find or create
			RelationshipXref rx = PreMerger
				.findOrCreateRelationshipXref(relType, db, ac, model);
			
			//check if an equivalent rel. xref is already present (skip it then)
			for(Xref x : bpe.getXref()) {
				if(x instanceof RelationshipXref && x.isEquivalent(rx))
					continue ac; //break and go to next ac
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
	 * @return the replacement object or null if none can found
	 */
	private ProteinReference findOrCreateProteinReference(final ProteinReference orig) 
	{				
		ProteinReference toReturn = null;	
		
		final String standardPrefix = "http://identifiers.org/";
		final String canonicalPrefix = standardPrefix + "uniprot/";
		final String origUri = orig.getRDFId();
		
		// Try to re-use existing object
		if(origUri.startsWith(canonicalPrefix)) {
			toReturn = (ProteinReference) warehouseModel.getByID(origUri);
		}
 
		// If nothing's found by URI right away, 
		// try id-mapping (of the normalized URI part to chebi primary accession) 
		if (toReturn == null && origUri.startsWith(standardPrefix)) {
			String id = origUri.substring(origUri.lastIndexOf('/')+1);	
			String db = null;				
			//a hack/shortcut for normalized PRs
			if(orig instanceof ProteinReference) {
				if(origUri.toLowerCase().contains("uniprot.isoform")) {
					db = "uniprot isoform";
				} else if(origUri.toLowerCase().contains("uniprot")) {
					db = "uniprot";
				} else if(origUri.toLowerCase().contains("refseq")) {
					db = "refseq";	
				} else if(origUri.toLowerCase().contains("kegg") && id.contains(":")) {
					db = "NCBI Gene"; //KEGG actually uses NCBI Gene (aka Entrez Gene)
				}
			}			
			if(db == null) 
				db = dbById(id, orig.getXref());	

			Set<String> mp = service.map(db, id, "UNIPROT");
			Set<EntityReference> ers = findEntityRefUsingIdMappingResult("URI", mp, canonicalPrefix);
			if(ers.size()>1)
				log.warn(origUri + " maps to multiple warehouse ERs: " + ers);
			else if (!ers.isEmpty())
				toReturn = (ProteinReference) ers.iterator().next();
		}
				
		// if yet ambiguous mapping or nothing, 
		// try using (already normalized) Xrefs and id-mapping  
		if (toReturn == null)
			toReturn = (ProteinReference) mapByXrefs(orig, "UNIPROT", canonicalPrefix);
		
		// nothing/ambiguous?..
		//TODO map by names? (too RISKY, might replace wrong PR, for proteins/genes often share names!)
	
		return toReturn;
	}

	/**
	 * Using specified class xrefs of given object, 
	 * finds primary identifiers (can be many).
	 * 
	 * 
	 * @param orig
	 * @param mapTo
	 * @param xrefType
	 * @param isUnion tells to return either union or intersection of each Xref's id-mapping result
	 * @return
	 */
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
					+ " (" + x.getRDFId() + "), db: " + x.getDb() + ", id: " + x.getId());
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
					if(mp.isEmpty())
						break; //quit w/o trying other xrefs due to apparently empty intersection
				} 
				
				xSet = mp; //xSet now contains not empty intersection
				assert !xSet.isEmpty() : "Impossible: xSet is empty.";
			}
		}

		if(xSet.isEmpty())
			return Collections.EMPTY_SET;
		else if(xSet.size()>1)
			return new TreeSet<String>(xSet);
		else {
			log.info(orig.getRDFId() + ", using its " + xrefType.getSimpleName() 
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
				log.warn("Ignored bad " + xrefType.getSimpleName()
					+ " (" + x.getRDFId() + "), db: " + x.getDb() + ", id: " + x.getId());
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
	private SmallMoleculeReference findOrCreateSmallMoleculeReference(final SmallMoleculeReference orig) 
	{				
		SmallMoleculeReference toReturn = null;	
		
		final String standardPrefix = "http://identifiers.org/";
		final String canonicalPrefix = standardPrefix + "chebi/";
		final String origUri = orig.getRDFId();
		
		// Try to re-use existing object
		if(origUri.startsWith(canonicalPrefix)) {
			toReturn = (SmallMoleculeReference) warehouseModel.getByID(origUri);
		}
 
		// If nothing's found by URI right away, 
		// try id-mapping (of the normalized URI part to chebi primary accession) 
		if (toReturn == null && origUri.startsWith(standardPrefix)) {
			String id = origUri.substring(origUri.lastIndexOf('/')+1);	
			String db = dbById(id, orig.getXref()); //find by id			
			Set<String> mp = service.map(db, id, "CHEBI");
			Set<EntityReference> ers = findEntityRefUsingIdMappingResult("URI", mp, canonicalPrefix);
			if(ers.size()>1)
				log.warn(origUri + " URI maps to multiple canonical ChEBI SMRs: " + ers);
			else if (!ers.isEmpty())
				toReturn = (SmallMoleculeReference) ers.iterator().next();
		}
				
		// if yet ambiguous mapping or nothing, 
		// try using (already normalized) Xrefs and id-mapping  
		if (toReturn == null)
			toReturn = (SmallMoleculeReference) mapByXrefs(orig, "CHEBI", canonicalPrefix);
		
		// nothing/ambiguous? - keep trying, map by name (e..g, 'ethanol') to ChEBI ID
		if (toReturn == null) {		
			Set<String> mp = mapByName(orig, "CHEBI");
			Set<EntityReference> ers = findEntityRefUsingIdMappingResult("names", mp, canonicalPrefix);
			if(ers.size()>1)
				log.warn(origUri + ", its names match multiple canonical ChEBI SMRs: " + ers);
			else if (!ers.isEmpty())
				toReturn = (SmallMoleculeReference) ers.iterator().next();
		}
		
		return toReturn;
	}


	private EntityReference mapByXrefs(EntityReference orig,
			String dest, String canonicalUriPrefix) {

		EntityReference toReturn = null; 
		
		Set<String> mappingSet = idMappingByXrefs(orig, dest, UnificationXref.class, false);
		
		Set<EntityReference> mapsTo = findEntityRefUsingIdMappingResult("UnificationXrefs", mappingSet, canonicalUriPrefix);
		if(mapsTo.size()>1) {
			log.warn(orig.getRDFId() + ", UnificationXrefs map to multiple: " + mapsTo);
		} else if(mapsTo.isEmpty()) {
			mappingSet = idMappingByXrefs(orig, dest, RelationshipXref.class, false);
			mapsTo = findEntityRefUsingIdMappingResult("RelationshipXrefs", mappingSet, canonicalUriPrefix);
			if(mapsTo.size()>1)
				log.warn(orig.getRDFId() + ", RelationshipXrefs map to multiple: " + mapsTo);
		}
		
		if(mapsTo.size()==1) 
			toReturn = mapsTo.iterator().next();
		
		return toReturn; //can be null
	}


	@SuppressWarnings("unchecked")
	private Set<String> mapByName(EntityReference orig, String toDb) {
		Set<String> mp = new TreeSet<String>();

		String name = orig.getStandardName();
		if(name != null)
			mp.addAll(service.map(null, name.toLowerCase(), toDb));

		if(mp.isEmpty()) {
			name = orig.getDisplayName();
			if(name != null)
				mp.addAll(service.map(null, name.toLowerCase(), toDb));
		}
		
		if(mp.isEmpty() && //and only for PRs and SMRs -
				(orig instanceof ProteinReference || orig instanceof SmallMoleculeReference)) 
		{
			//To find a warehouse SMR(s) with exactly the same name (case-insensitive).			
			//first, collect all orig names, lowercase
			Set<String> origNames = new HashSet<String>();
			for(String n : orig.getName()) 
				origNames.add(n.toLowerCase());
			
			//search for warehouse ERs of the same as orig's class
			for(EntityReference er : warehouseModel
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


	private Set<EntityReference> findEntityRefUsingIdMappingResult(String mapBy, Set<String> mapsTo, String uriPrefix) 
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