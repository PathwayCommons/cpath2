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
import cpath.dao.MetadataDAO;
import cpath.jpa.MappingsRepository;
import cpath.warehouse.beans.*;

import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.Complex;
import org.biopax.paxtools.model.level3.Entity;
import org.biopax.paxtools.model.level3.EntityFeature;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.Gene;
import org.biopax.paxtools.model.level3.Level3Element;
import org.biopax.paxtools.model.level3.Named;
import org.biopax.paxtools.model.level3.PhysicalEntity;
import org.biopax.paxtools.model.level3.ProteinReference;
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
import org.biopax.paxtools.controller.ObjectPropertyEditor;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.controller.Traverser;
import org.biopax.paxtools.controller.Visitor;
import org.biopax.paxtools.io.*;
import org.biopax.validator.utils.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
    private final SimpleMerger simpleMerger;
    
    private Model warehouseModel;
    private Model mainModel;   
    
    // cpath2 metadata repositories
	private final MetadataDAO metadataDAO;
	private final MappingsRepository mappingsRepository;
    
    // configuration/flags
	private final String provider;
	private final boolean force;	
	private final String xmlBase;
	

	/**
	 * Constructor (package-private).
	 *
	 * This constructor was added to be used in a test context. At least called by
	 * cpath.importer.internal.CPathInMemoryModelMergerTest.testMerger().
	 * 
	 * @param metadataDAO MetadataDAO
	 * @param mappingsRepository
	 * @param provider merge pathway data from this provider only
	 * @param force whether to forcibly merge BioPAX data the validation reported critical about or skip.
	 * @throws AssertionError when dest is not instanceof {@link Model};
	 */
	public Merger(final MetadataDAO metadataDAO, 
			MappingsRepository mappingsRepository, String provider, boolean force) 
	{
		this.metadataDAO = metadataDAO;
		this.mappingsRepository = mappingsRepository;
		this.xmlBase = CPathSettings.getInstance().getXmlBase();
		this.provider = provider;
		this.force = force;		
		this.simpleMerger = new SimpleMerger(SimpleEditorMap.L3);
		
		try {
			this.warehouseModel = CPathUtils.loadWarehouseBiopaxModel();
			log.info("Successfully imported the Warehouse BioPAX model from archive...");
		} catch (IOException e) {
			throw new RuntimeException("Failed to import the Warehouse model!", e);
		}	
		
		try {	
			this.mainModel = CPathUtils.loadMainBiopaxModel();
			log.info("Continue merging into previously used main model...");
		} catch (IOException e) {
			log.info("Could not import Main model; so, created a new empty one.", e);			
			this.mainModel = BioPAXLevel.L3.getDefaultFactory().createModel();
			this.mainModel.setXmlBase(xmlBase);
		}		
		
	}

	
	public void merge() {
		SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);

		// build models and merge from dataFile.premergeData
		Collection<Metadata> providersMetadata = new ArrayList<Metadata>();
		
		if (provider != null) {			
			providersMetadata.add(metadataDAO.getMetadataByIdentifier(provider));
		}
		else {
			providersMetadata = metadataDAO.getAllMetadata();
		}

		for (Metadata metadata : providersMetadata) {
			
			if(metadata.isNotPathwayData()) {
				log.info("Skip for warehouse data: " + metadata);
				continue;
			}
			
			log.info("Start merging " + metadata);
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
				
				Model pathwayModel = simpleReader.convertFromOWL(inputStream);

				// merge the input biopax model
				merge(pwdata.toString(), pathwayModel);
			}
			
			log.info("Done merging " + metadata);
		}
	
		//create or replace the main BioPAX archive
		save();
		
		log.info("Complete.");
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
			throw new RuntimeException("Failed updating the main BioPAX archive!", e);
		}
	}
	
	
	/**
	 * Integrates the source model into the target using the 
	 * reference biopax objects (entity references, etc.) 
	 * in the target or warehouse models.
	 * As the result, the source model will (and the warehouse might) be broken 
	 * (ok to dispose), and the target - complete, self-integral,
	 * contain all the entities that were there before and new from the source
	 * model (where some original utility class objects are replaced with 
	 * canonical warehouse or previously existing target ones). 
	 * 
	 * @param description
	 * @param source
	 */
	void merge(String description, final Model source) {	
		
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
			((Level3Element) bpe).addComment("REPLACED " + currUri);
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
						addCanonicalRelXrefs(source, (SmallMolecule) pe, "CHEBI");
					} else {
						// for Protein, Dna, DnaRegion, Rna*...
						addCanonicalRelXrefs(source, (PhysicalEntity) pe, "UNIPROT");
					}						
				} else if(pe instanceof Complex) {
					continue; // skip complexes
				} else {
					// do for base PEs
					addCanonicalRelXrefs(source, (PhysicalEntity) pe, "UNIPROT");
					addCanonicalRelXrefs(source, (PhysicalEntity) pe, "CHEBI");
				}
			} else if(pe instanceof Gene) {
				addCanonicalRelXrefs(source, pe, "UNIPROT");
			}
		}
			
		log.info("Searching for canonical or existing utility class objects " +
				" to replace equivalent original objects ("+srcModelInfo+")...");
		final Map<UtilityClass, UtilityClass> replacements = new HashMap<UtilityClass, UtilityClass>();		
		// match some UtilityClass objects to existing ones (previously imported, warehouse)
		for (UtilityClass bpe: new HashSet<UtilityClass>(source.getObjects(UtilityClass.class))) 
		{
			UtilityClass replacement = null;
			
			// Find the best replacement ER in the Warehouse:
			if (bpe instanceof ProteinReference) {
				replacement = findOrCreateProteinReference((ProteinReference)bpe);
			} 
			else if (bpe instanceof SmallMoleculeReference) {
				replacement = findOrCreateSmallMoleculeReference((SmallMoleculeReference)bpe);
			} 
			else { //e.g., BioSource, CV, or Provenance - simply match by URI (no id-mapping or additional checks)
				replacement = getByUri(bpe.getRDFId());
			}
				
			if (replacement != null) {
				UtilityClass r = (UtilityClass) mainModel.getByID(replacement.getRDFId());
				if(r != null) // re-use previously merged one
					replacement = r;
				else //merge (including all children)
					simpleMerger.merge(mainModel, replacement);
				//save in the map to replace the source bpe later 
				replacements.put(bpe, replacement);
				replacement.addComment("REPLACED " + bpe.getRDFId());
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
				}
			}				
			// move PublicationXrefs and RelationshipXrefs (otherwise we lost some original xrefs)
			if(old instanceof XReferrable) {
				for(Xref x : new HashSet<Xref>(((XReferrable) old).getXref())) {
					if(!(x instanceof UnificationXref)) {
						((XReferrable) old).removeXref(x);						
						XReferrable repl = ((XReferrable) replacements.get(old));					
						if(mainModel.containsID(x.getRDFId())) {
							Xref mergedX = (Xref) mainModel.getByID(x.getRDFId());						
							repl.removeXref(x);
							repl.addXref(mergedX);						
						} else {
							repl.addXref(x);
						}
						
					}
				}
			}
		}	
										
		// fix dangling inverse properties (some objects there
		// in the source model might still refer to the removed ones)
//		fixInverseProperties(removed);	
		
		log.info("Merging into the main BioPAX model...");
		// merge to the target model
		mainModel.merge(source);
		
		log.info("Merge is done ("+srcModelInfo+").");			
	}

		
	private UtilityClass getByUri(String uri) {
		return (UtilityClass) warehouseModel.getByID(uri);
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
	 * @param src source model (where the following element belongs)
	 * @param bpe a {@link Gene} or {@link PhysicalEntity}
	 * @param db map identifiers to (e.g., CHEBI, UNIPROT)
	 * @throws AssertionError when bpe is neither Gene nor PhysicalEntity
	 */
	private void addCanonicalRelXrefs(Model src, Named bpe, String db) 
	{
		if(!(bpe instanceof Gene || bpe instanceof PhysicalEntity))
			throw new AssertionError("Not Gene or PE: " + bpe);
			
		// map and generate/add xrefs
		Set<String> mappingSet = idMappingByXrefs(bpe, db, UnificationXref.class);
		addRelXref(src, bpe, db, mappingSet);
		
		mappingSet = idMappingByXrefs(bpe, db, RelationshipXref.class);
		addRelXref(src, bpe, db, mappingSet);
		
		//map by display and standard names
		mappingSet = mappingsRepository.map(null, bpe.getDisplayName(), db);
		addRelXref(src, bpe, db, mappingSet);
	}


	/**
	 * Finds or creates relationship xrefs
	 * using id-mapping results;
	 * adds them to the object and model.
	 * 
	 * @param source of the  following element
	 * @param bpe a gene, physical entity or entity reference
	 * @param db database name for all (primary/canonical) xrefs; 'uniprot' or 'chebi'
	 * @param mappingSet
	 * @throws AssertionError when bpe is neither Gene nor PhysicalEntity nor EntityReference
	 */
	private void addRelXref(Model source, XReferrable bpe, String db, Set<String> mappingSet) 
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
	 * @return the replacement object or null if none can found
	 */
	private ProteinReference findOrCreateProteinReference(final ProteinReference orig) 
	{				
		ProteinReference toReturn = null;	
		
		final String standardPrefix = "http://identifiers.org/";
		final String canonicalPrefix = standardPrefix + "uniprot/";
		
		String uri = orig.getRDFId();
		
		// 1) try to match an existing PR by URI
		if(uri.startsWith(canonicalPrefix)) {
			toReturn = (ProteinReference) getByUri(uri);
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
				toReturn = (ProteinReference) getByIdsFromModel(orig.getRDFId(), "URI", mp, canonicalPrefix);
			}
		}
				
		// if yet nothing's found, 
		// 3) try using (already normalized) all Unification Xrefs and id-mapping (to uniprot ac). 
		if (toReturn == null) {
			Set<String> mappingSet = idMappingByXrefs(orig, "UNIPROT", UnificationXref.class);
			if(!mappingSet.isEmpty()) {
				toReturn = (ProteinReference) getByIdsFromModel(
						orig.getRDFId(), "Unif. Xrefs", mappingSet, canonicalPrefix);		
			}
		}	
		
		// if nothing's found in the warehouse by URI and unif. xrefs, - 
		// 4) try relationship xrefs and id-mapping 
		if (toReturn == null) {
			Set<String> mappingSet = idMappingByXrefs(orig,"UNIPROT", RelationshipXref.class);
			if(!mappingSet.isEmpty()) {
				toReturn = (ProteinReference) getByIdsFromModel(
						orig.getRDFId(), "Rel. Xrefs", mappingSet, canonicalPrefix);
			}	
		}
				
		//TODO map by names? (too RISKY, might replace wrong PR, for proteins/genes often share names!)
//		if (toReturn == null) {
//			Set<String> mp = mapByName(orig, "UNIPROT");		
//			if(!mp.isEmpty()) {
//				toReturn = (ProteinReference) getByIdsFromModel(
//						orig.getRDFId(), "names", mp, canonicalPrefix);
//			}
//		}
		
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
	private SmallMoleculeReference findOrCreateSmallMoleculeReference(final SmallMoleculeReference orig) 
	{				
		SmallMoleculeReference toReturn = null;	
		
		final String standardPrefix = "http://identifiers.org/";
		final String canonicalPrefix = standardPrefix + "chebi/";
		
		String uri = orig.getRDFId();
		
		// 1) try to re-use existing object
		if(uri.startsWith(canonicalPrefix)) {
			toReturn = (SmallMoleculeReference) getByUri(uri);
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
				toReturn = (SmallMoleculeReference) getByIdsFromModel(
						orig.getRDFId(), "URI", mp, canonicalPrefix);
			}
		}
				
		// if yet nothing's found, 
		// 3) try using (already normalized) all Unification Xrefs and id-mapping (to primary chebi). 
		if (toReturn == null) {
			Set<String> mappingSet = idMappingByXrefs(orig, "CHEBI", UnificationXref.class);
			if(!mappingSet.isEmpty()) {
				toReturn = (SmallMoleculeReference) getByIdsFromModel(
						orig.getRDFId(), "Unif. Xrefs", mappingSet, canonicalPrefix);
			}	
		}	
		
		// if nothing's found in the db by URI or unif. xrefs, - 
		// 4) try using relationship xrefs and id-mapping. 
		if (toReturn == null) {
			Set<String> mappingSet = idMappingByXrefs(orig, "CHEBI", RelationshipXref.class);
			if(!mappingSet.isEmpty()) {
				//not needed if we replace the ER
//				addRelXref(orig, "CHEBI", mappingSet);
				toReturn = (SmallMoleculeReference) getByIdsFromModel(
						orig.getRDFId(), "Rel. Xrefs", mappingSet, canonicalPrefix);
			}	
		}
		
		//5) map by name (e..g, 'ethanol') to ChEBI ID
		if (toReturn == null) {		
			Set<String> mp = mapByName(orig, "CHEBI");
			if(!mp.isEmpty()) {
				toReturn = (SmallMoleculeReference) getByIdsFromModel(
						orig.getRDFId(), "names", mp, canonicalPrefix);
			}
		}
		
		return toReturn;
	}


	@SuppressWarnings("unchecked")
	private Set<String> mapByName(EntityReference orig, String toDb) {
		Set<String> mp = new TreeSet<String>();

		String name = orig.getStandardName();
		if(name != null)
			mp.addAll(mappingsRepository.map(null, name.toLowerCase(), toDb));

		if(mp.isEmpty()) {
			name = orig.getDisplayName();
			if(name != null)
				mp.addAll(mappingsRepository.map(null, name.toLowerCase(), toDb));
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


	private EntityReference getByIdsFromModel(String origUri, String mapBy, Set<String> mapsTo, String uriPrefix) 
	{
		String uri = uriPrefix + mapsTo.iterator().next();
		EntityReference toReturn = (EntityReference) getByUri(uri);
		if(toReturn!= null && mapsTo.size() > 1)
			log.warn("Merger picked the FIRST " + toReturn.getModelInterface().getSimpleName() 
				+ " with " + uri + " of " + mapsTo + " (id-mapping by " 
					+ mapBy + ") to REPLACE " + origUri);
		
		return toReturn;
	}
		
}