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
import cpath.dao.IdMapping;
import cpath.dao.PaxtoolsDAO;
import cpath.importer.Merger;
import cpath.warehouse.beans.*;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.WarehouseDAO;

import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.controller.*;
import org.biopax.paxtools.io.*;
import org.biopax.validator.utils.Normalizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.*;

/**
 * This class is responsible for semantic Merging 
 * of the normalized original provider's pathway data 
 * into the main database/model.
 */
final class MergerImpl implements Merger, Analysis {

    private static final Log log = LogFactory.getLog(MergerImpl.class);

	// where to merge pathway data
    private PaxtoolsDAO mainDAO; //also implements Model interface!
    
    // cpath2 repositories
	private MetadataDAO metadataDAO;
    private WarehouseDAO warehouseDAO;
    
    // configuration/flags
	private String pathwayDataIdentifier;
	private String pathwayDataVersion;
	private boolean force;
	
	private SimpleMerger simpleMerger;
	
//TODO try local in-memory id-mapping tables (optimization?)
//	private final Map<String,String> geneIdMap;
//	private final Map<String,String> chemIdMap;


	/**
	 * Test Constructor (package-private).
	 *
	 * This constructor was added to be used in a test context. At least called by
	 * cpath.importer.internal.CPathInMemoryModelMergerTest.testMerger().
	 * @param metadataDAO MetadataDAO
	 * @param warehouseDAO WarehouseDAO
	 * @param pcDAO final "global" Model (e.g., {@link PaxtoolsHibernateDAO} may be used here)
	 */
	MergerImpl(final PaxtoolsDAO dest, final MetadataDAO metadataDAO, final WarehouseDAO warehouseDAO) 
	{
		assert dest instanceof Model : 
			"PaxtoolsDAO must also implement org.biopax.paxtools.Model!";
	
		this.mainDAO = dest;
		this.metadataDAO = metadataDAO;
		this.warehouseDAO = warehouseDAO;
		this.simpleMerger = new SimpleMerger(SimpleEditorMap.L3);
// future optimization/try		
//		this.geneIdMap = metadataDAO.getIdMap(GeneMapping.class);
//		this.chemIdMap = metadataDAO.getIdMap(ChemMapping.class);
	}
	
	
	/**
	 * {@inheritDoc}
	 * 
	 * @param args - BioPAX Paxtools Models.
	 * 
	 * @throws ClassCastException
	 */
	@Override
	public Set<BioPAXElement> execute(Model model, Object... args) {
		
		for(Object arg : args) {
			Model pathwayModel = (Model) arg;
			mergePathwayModel((PaxtoolsDAO)model, pathwayModel);
		}

		return null; // ignore
	}
	
	
	@Override
	public void merge() {
		SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);

		// build models and merge from pathwayData.premergeData
		Collection<PathwayData> data;
		if (pathwayDataIdentifier != null && pathwayDataVersion != null)
			data = metadataDAO.getPathwayDataByIdentifierAndVersion(pathwayDataIdentifier, pathwayDataVersion);
		else if (pathwayDataIdentifier != null)
			data = metadataDAO.getPathwayDataByIdentifier(pathwayDataIdentifier);
		else
			data = metadataDAO.getAllPathwayData();

		for (PathwayData pwdata : data) {
			log.info("merge(): now merging " + pwdata.toString());

			if (pwdata.getValid() == null || pwdata.getPremergeData() == null
					|| pwdata.getPremergeData().length == 0) {
				// must run pre-merge first!
				log.warn("Do '-premerge' first! Skipping " + pwdata.toString());
				continue;
			} else if (pwdata.getValid() == false) {
				// has BioPAX errors
				log.warn("There were critical BioPAX errors in " + " - "
						+ pwdata.toString());
				if (!isForce()) {
					log.warn("Skipping " + pwdata);
					continue;
				} else {
					log.warn("FORCE merging (ignoring all "
							+ "validation issues) for " + pwdata);
				}
			}

			// import the BioPAX L3 pathway data into the in-memory paxtools model
			InputStream inputStream = new ByteArrayInputStream(pwdata.getPremergeData());
			Model pathwayModel = simpleReader.convertFromOWL(inputStream);
			
			// the following can improve graph queries and full-text relevance -
			
			/* using xrefs abd id-mapping, add primary uniprot/chebi RelationshipXref 
			 * to all SimplePE (SM, Protein, Dna,..) and Gene if possible;
			 * skip if pe.entityReference.xref if not empty (then id-mapping is done
			 * later on, in  )
			 */ 		
			for(PhysicalEntity pe : new HashSet<PhysicalEntity>(pathwayModel.getObjects(PhysicalEntity.class))) {
				/* well, let's generate canonical primary uniprot/chebi relationship xrefs
				 * from all available rel. and unif. xrefs using our id-mapping;
				 * even if it leads to mutually exclusive identifiers (which would be a data issue), 
				 * we keep those (not a big deal, for we are not merging based on this, but search/query), 
				 * and just log a warning for future fix)
				 */ 
				if(pe instanceof SimplePhysicalEntity) {
					EntityReference er = ((SimplePhysicalEntity) pe).getEntityReference();
					if(er != null && !er.getXref().isEmpty())
						continue;
					
					//TODO
			
				} else if(pe instanceof Gene) {
					//TODO
				}
			}
			
			// merge/persist the new biopax; run within a transaction:
			mainDAO.runAnalysis(this, pathwayModel);
		}

		log.info("merge() complete, exiting...");
	}

	
//	private static boolean isGeneric(BioPAXElement bpe) {
//		boolean ret = false;
//		
//		if(bpe instanceof PhysicalEntity) {
//			if(!((PhysicalEntity) bpe).getMemberPhysicalEntity().isEmpty())
//				ret = true;
//			else if(bpe instanceof SimplePhysicalEntity) {
//				ret = isGeneric(((SimplePhysicalEntity) bpe).getEntityReference());
//			} else {			
//				//TODO decide/do what generic Complex or Gene means...
//			}
//			
//		} else if(bpe instanceof EntityReference) {
//			if(!((EntityReference) bpe).getMemberEntityReference().isEmpty())
//				ret = true;
//		}
//		
//		return ret;
//	}
	
		
	/** 
	 * Merges a new pathway model into persistent main model: 
	 * inserts new objects and updates object properties
	 * (and should not break inverse properties).
	 * It re-uses already merged or new Warehouse UtilityClass objects 
	 * (e.g., EntityReference) to replace equivalent ones in the pathway data.
	 * 
	 * Note: active transaction must exist around this method if the main model is a 
	 * persistent model implementation (PaxtoolsHibernateDAO).
	 *  
	 * @param mainModel target model (in production, - persistent model)
	 * @param pathwayModel normalized self-integral in-memory biopax model to be merged
	 */
	private void mergePathwayModel(PaxtoolsDAO mainModel, Model pathwayModel) 
	{	
		//we suppose, the pathwayModel is self-integral, 
		//i.e, - no external refs and implicit children
		//(this is almost for sure true if it's just came from a string/file)
		
		// find matching utility class elements in the warehouse or main db
		log.info("mergePathwayModel: looking to re-use/merge with " +
			"semantically equivalent utility class elements " +
			"in the target Model or Warehouse");
		//new "replacements" Model
		Model generatedModel = BioPAXLevel.L3.getDefaultFactory().createModel(); 
		final Map<BioPAXElement, BioPAXElement> replacements = new HashMap<BioPAXElement, BioPAXElement>();
		
		// match some utility class objects to ones from the warehouse or previously imported
		for (UtilityClass bpe: new HashSet<UtilityClass>(pathwayModel.getObjects(UtilityClass.class))) 
		{
			UtilityClass replacement = null;
			// Find the best replacement either in the warehouse or target model;
			if (bpe instanceof ProteinReference) {
				replacement = findOrCreateProteinReference((ProteinReference)bpe, (Model)mainModel, generatedModel);
			} else if (bpe instanceof SmallMoleculeReference) {
				replacement = findOrCreateSmallMoleculeReference((SmallMoleculeReference)bpe, (Model)mainModel, generatedModel);
			}
				
			if (replacement != null) {	
				final String id = replacement.getRDFId();
				if(((Model)mainModel).containsID(id)) {
					// just put the existing object to the replacements map and continue;
					// skip in-memory merging, - preserves existing inverse BioPAX properties!
					replacements.put(bpe, replacement);
				} else {
					if(!generatedModel.containsID(id)) {//- just from Warehouse? -
						// Do some fixes and merge into the in-memory model;
						// e.g., remove thousands of special ChEBI relationship xrefs
						if(replacement instanceof SmallMoleculeReference)
							removeRelXrefs((EntityReference) replacement);
					
						// clear the AA sequence (save space and time; not really very useful...)
						if(replacement instanceof ProteinReference)
							((ProteinReference) replacement).setSequence(null);
					
						// in-memory merge to reuse same child xrefs, etc.
						simpleMerger.merge(generatedModel, replacement);
					} 
					
					// associate, continue
					replacements.put(bpe, generatedModel.getByID(id));
				}
			}
		}
				
		// fix entityFeature/entityFeatureOf for sure, and may be other properties...
		log.info("mergePathwayModel: migrating some properties (features)...");
		for (BioPAXElement old : replacements.keySet()) {
			if (old instanceof EntityReference) {
				for (EntityFeature ef : new HashSet<EntityFeature>(
						((EntityReference) old).getEntityFeature())) {
					
					// the following updates the existing (already merged) object
					if(ef.getEntityFeatureOf() == old) //it may not
						((EntityReference) old).removeEntityFeature(ef);
					else
						log.warn(old.getRDFId() + " contains entityFeature (f) " + ef.getRDFId()
							+ ", but f.entityFeatureOf is another entity refernece " +
							"(there is probably an erorr in both the BioPAX data and validator/normalizer)!");
					
					EntityReference replacement = ((EntityReference) replacements.get(old));
					replacement.addEntityFeature(ef); // this fixes entityFeatureOf (-single cardinality) as well!
				}
			}
		}
		
		// do replace (object refs) in the original pathwayModel
		log.info("mergePathwayModel: replacing utility objects with matching ones...");	
		ModelUtils.replace(pathwayModel, replacements);
		
		log.info("mergePathwayModel: removing replaced/dangling objects...");	
		ModelUtils.removeObjectsIfDangling(pathwayModel, UtilityClass.class);
		//force re-using of matching by id Xrefs, CVs, etc.. from the generated model
		simpleMerger.merge(generatedModel, pathwayModel); 
	
		// create completely detached in-memory model (fixes dangling properties...)
		log.info("mergePathwayModel: updating in-memory model...");
		pathwayModel = ModelUtils.writeRead(generatedModel);
			
		log.info("mergePathwayModel: persisting...");
		// merge to the main model (save/update object relationships)
		mainModel.merge(pathwayModel);
		
		log.info("mergePathwayModel: merge is complete, exiting...");
	}


	/**
	 * Finds previously created or generates (searching in the data warehouse) 
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
		// because we did validate/normalize all the data in Premerge stage and 
		// can expect a quick result in most cases...
		// warehouse ERs have such URIs only
		if(uri.startsWith(canonicalPrefix)) {
			toReturn = getById(uri, ProteinReference.class, generatedModel, warehouseDAO, mainModel);
			if(toReturn != null)
				return toReturn;
		}
 
		// otherwise - try more - with id-mapping
		
		/* getting here also means biopax normalization was
		 * not quite successful, due to lack of uniprot unif. xref, 
		 * having geneId/ensemble (relationship xrefs),
		 * or using a non standard gene/protein db names.
		 */
		
		// if nothing's found in the warehouse by original or normalized URI, 
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
// can later optimize by using an in-memory map instead of DAO -
//			id = IdMappingFactory.suggest(db, id); //can improve mapping is several known cases		
//			id = geneIdMap.get(id);	
			IdMapping mp = metadataDAO.getIdMapping(db, id, GeneMapping.class); //IdMappingFactory.suggest is called inside.
			if(mp != null) {
				id = mp.getAccession();
				toReturn = getById(canonicalPrefix + id, ProteinReference.class, generatedModel, warehouseDAO, mainModel);
				//keep specific isoform/version id (NP_123456.2, P04150-3,..) after merging/replacing original PRs
				addRelXrefToParents(orig, mainModel, generatedModel);
			}
		}
				
		// if yet nothing's found, 
		// 3) try using (already normalized) all Unification Xrefs and id-mapping (to uniprot ac). 
		if (toReturn == null) {
			Set<IdMapping> mappingSet = idMappingByXrefs(orig, UnificationXref.class, GeneMapping.class);
			if(!mappingSet.isEmpty()) {
				// use only the first result (a warning logged already)
				toReturn = getById(canonicalPrefix + mappingSet.iterator().next().getAccession(), 
					ProteinReference.class, generatedModel, warehouseDAO, mainModel);
				//keep specific isoform/version id (NP_123456.2, P04150-3,..) after merging/replacing original PRs
				addRelXrefToParents(orig, mainModel, generatedModel); //call only once mapping succeeded				
			}
		}	
		
		// if nothing's found in the warehouse by URI and unif. xrefs, - 
		// 4) try relationship xrefs and id-mapping 
		if (toReturn == null) {
			Set<IdMapping> mappingSet = idMappingByXrefs(orig, RelationshipXref.class, GeneMapping.class);
			if(!mappingSet.isEmpty()) {
				// use only the first result (a warning logged already)
				toReturn = getById(canonicalPrefix + mappingSet.iterator().next().getAccession(), 
						ProteinReference.class, generatedModel, warehouseDAO, mainModel);
				addRelXrefToParents(orig, mainModel, generatedModel); //call only once mapping succeeded
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
	 * @param xrefType
	 * @param mappClass
	 * @return
	 */
	private Set<IdMapping> idMappingByXrefs(final EntityReference orig,
			final Class<? extends Xref> xrefType, Class<? extends IdMapping> mappClass) 
	{
		final Set<IdMapping> mappings = new HashSet<IdMapping>();
		
		for (Xref x : orig.getXref()) {
			if (xrefType.isInstance(x)) {
				IdMapping mp = metadataDAO.getIdMapping(x.getDb(), x.getId(), mappClass);
				if (mp != null) {
					mappings.add(mp);
				}
			}
		}
		
		if(mappings.size() > 1)
			log.error("getPrimaryId, not unique id-mapping of " + 
				orig.getRDFId() + ": " + mappings);

		return mappings;
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
	 * Copy xrefs of original entity reference to
	 * all its parent entities (mol. states), converting 
	 * unification xrefs to relationship xrefs.
	 * 
	 * @param original
	 * @param generatedModel 
	 * @param mainModel 
	 */
	private void addRelXrefToParents(EntityReference original, Model mainModel, Model generatedModel) {
		for(Xref x : original.getXref()) {
			if(x instanceof UnificationXref) {
				// create/re-use RelationshipXref (same db/id as x's)
				String rxUri = Normalizer.uri(mainModel.getXmlBase(), x.getDb(), x.getId(), RelationshipXref.class);
				RelationshipXref rx = getById(rxUri, RelationshipXref.class, generatedModel, null, mainModel);
				if(rx == null) {
					rx = generatedModel.addNew(RelationshipXref.class, rxUri);
					rx.setDb(x.getDb());
					rx.setId(x.getId());
					if(!x.getComment().isEmpty())
						rx.getComment().addAll(x.getComment());
				}				
				x = rx; //replace for the next operation
			}
			
			//add the xref (not unification xref) to every parent
			for(SimplePhysicalEntity parent : original.getEntityReferenceOf())
				parent.addXref(x);
		}
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
		// because we did validate/normalize all the data in Premerge stage and 
		// can expect a quick result in most cases...
		// warehouse ERs have such URIs only
		if(uri.startsWith(canonicalPrefix)) {
			toReturn = getById(uri, SmallMoleculeReference.class, generatedModel, warehouseDAO, mainModel);
			if(toReturn != null)
				return toReturn;
		}
 
		// otherwise - try more - with id-mapping
		
		/* getting here also means biopax normalization was 
		 * not quite successful, due to lack of chebi unif. xrefs, 
		 * having a pubchem/kegg (relationship) xref instead,
		 * or using a non standard chemical db name.
		 */
		
		// if nothing's found in the warehouse by original or normalized URI, 
		// 2) try id-mapping (to uniprot ac). 
		if (uri.startsWith(standardPrefix)) {
			String id = uri.substring(uri.lastIndexOf('/')+1);	
			String db = dbById(id, orig.getXref()); //find by id
// can later optimize by using an in-memory map instead of DAO -
//			id = IdMappingFactory.suggest(db, id);
//			id = chemIdMap.get(id);	
			IdMapping mp = metadataDAO.getIdMapping(db, id, ChemMapping.class);
			if(mp != null) {
				id = mp.getAccession();
				toReturn = getById(canonicalPrefix + id, SmallMoleculeReference.class, generatedModel, warehouseDAO, mainModel);
				//keep specific isoform/version id (NP_123456.2, P04150-3,..) after merging/replacing original PRs				
				addRelXrefToParents(orig, mainModel, generatedModel);
			}
		}
				
		// if yet nothing's found, 
		// 3) try using (already normalized) all Unification Xrefs and id-mapping (to primary chebi). 
		if (toReturn == null) {
			Set<IdMapping> mappingSet = idMappingByXrefs(orig, UnificationXref.class, ChemMapping.class);
			if(!mappingSet.isEmpty()) {
				// use only the first result (a warning logged already)
				toReturn = getById(canonicalPrefix + mappingSet.iterator().next().getAccession(), 
					SmallMoleculeReference.class, generatedModel, warehouseDAO, mainModel);
				addRelXrefToParents(orig, mainModel, generatedModel); //call only once mapping succeeded
			}	
		}	
		
		// if nothing's found in the warehouse by URI or unif. xrefs, - 
		// 4) try using relationship xrefs and id-mapping. 
		if (toReturn == null) {
			// TODO not sure about mapping/merging SMRs by rel. xrefs, as, currently for mol., we might have ambiguous xrefs in the warehouse...
		}
		
		return toReturn;
	}
	
	
	/**
	 * Finds or generates a biopax object by first looking in
	 * the in-memory (tmp) model, next - main (usually target/DAO),
	 * finally - in the warehouse. The last two model can be null 
	 * (to skip looking there). 
	 *
	 * 
	 * @param id
	 * @param type
	 * @param tmp
	 * @param wh
	 * @param main
	 * @return object or null
	 */
	private <T extends UtilityClass> T getById(final String id, final Class<T> type, 
			final Model tmp, final WarehouseDAO wh, final Model main) 
	{
		assert id != null;
		
		// get from the in-memory model
		T t = type.cast(tmp.getByID(id));
		if (t == null && main != null) {
			// second, try - in the main model
			t = type.cast(main.getByID(id));
			if (t == null && wh != null )
				// third, create new if available in the warehouse
				t = wh.createBiopaxObject(id, type);
			else {
				log.debug(id);
			}
		}
		
		return t;
	}

	
	/*
	 * Removes those (thousands!) relationship xrefs
	 * generated by the samll mol. data converter
	 * with special idVersion="entry_name"
	 * (most likely, we will never need them in the main DB)
	 */
	private void removeRelXrefs(EntityReference er) {
		for(Xref x : new HashSet<Xref>(er.getXref())) {
			if(x instanceof RelationshipXref 
				&& "entry_name".equalsIgnoreCase(x.getIdVersion())) 
			{
				er.removeXref(x);
			}
		}
		
		for(EntityReference member : er.getMemberEntityReference()) {
			removeRelXrefs(member);
		}
	}


	/**
	 * Set/select the pathway data (by metadata 
	 * identifier and version) to merge. Default is
	 * - both are null, which means merge all premerged data.
	 * 
	 * @param identifier
	 * @param version
	 */
	void setPathwayData(String identifier, String version) {
		if(identifier != null && !identifier.isEmpty()) {
			this.pathwayDataIdentifier = identifier;
			this.pathwayDataVersion = (version == null || version.isEmpty()) 
					? null : version;		
		} else { //null id - version must be null as well
			this.pathwayDataIdentifier = null;
			this.pathwayDataVersion = null;
		}
	}

	/**
	 * Whether to try merging pathway data,
	 * despite the cpath2 BioPAX validator
	 * reported errors during the premerge stage.
	 * The default is false;
	 * 
	 * @return
	 */
	boolean isForce() {
		return force;
	}
	void setForce(boolean forceInvalid) {
		this.force = forceInvalid;
	}
	
}