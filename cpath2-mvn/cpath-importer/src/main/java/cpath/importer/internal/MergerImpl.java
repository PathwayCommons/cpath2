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

import cpath.config.CPathSettings;
import cpath.dao.Analysis;
import cpath.dao.PaxtoolsDAO;
import cpath.importer.Merger;
import cpath.warehouse.beans.*;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.WarehouseDAO;

import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.util.ClassFilterSet;
import org.biopax.paxtools.controller.*;
import org.biopax.paxtools.io.*;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.*;

/**
 * Class responsible for Merging pathway data into the main DB/Model.
 */
final class MergerImpl implements Merger, Analysis {

    private static final Log log = LogFactory.getLog(MergerImpl.class);

	// where to merge pathway data
    private PaxtoolsDAO mainDAO; //also implements Model interface!
    
    // cpath2 repositories
	private MetadataDAO metadataDAO;
    private WarehouseDAO cvRepository;
    private WarehouseDAO moleculesDAO;
    private WarehouseDAO proteinsDAO;
    
    // configuration/flags
	private String pathwayDataIdentifier;
	private String pathwayDataVersion;
	private boolean force;
	
	private SimpleMerger simpleMerger;

	/**
	 * Constructor.
	 */
	MergerImpl(PaxtoolsDAO dest) 
	{	
		assert dest instanceof Model : 
			"PaxtoolsDAO must also implement org.biopax.paxtools.Model!";
		
		this.mainDAO = dest;
		
		// metadata
		ApplicationContext whApplicationContext = 
			new ClassPathXmlApplicationContext("classpath:applicationContext-whouseDAO.xml");
		this.metadataDAO = (MetadataDAO)whApplicationContext.getBean("metadataDAO");
		
		// molecules
		ApplicationContext context = new ClassPathXmlApplicationContext(new String [] {"classpath:applicationContext-whouseMolecules.xml"});
		this.moleculesDAO = (WarehouseDAO)context.getBean("moleculesDAO");
		// proteins
		context = new ClassPathXmlApplicationContext(new String [] {"classpath:applicationContext-whouseProteins.xml"});
		this.proteinsDAO = (WarehouseDAO)context.getBean("proteinsDAO");
		// cvRepository - disable for now (replacing CVs may be not required...)
//		context = new ClassPathXmlApplicationContext(new String [] {"classpath:applicationContext-cvRepository.xml"});
//		this.cvRepository = (WarehouseDAO)context.getBean("cvFetcher");
		
		this.simpleMerger = new SimpleMerger(SimpleEditorMap.L3);
	}

	/**
	 * Test Constructor (package-private).
	 *
	 * This constructor was added to be used in a test context. At least called by
	 * cpath.importer.internal.CPathInMemoryModelMergerTest.testMerger().
	 *
	 * @param pcDAO final "global" Model (e.g., {@link PaxtoolsHibernateDAO} may be used here)
	 * @param metadataDAO MetadataDAO
	 * @param moleculesDAO WarehouseDAO
	 * @param proteinsDAO WarehouseDAO
	 * @param cvRepository WarehouseDAO
	 */
	MergerImpl(final PaxtoolsDAO dest, final MetadataDAO metadataDAO, final WarehouseDAO moleculesDAO,
					  final WarehouseDAO proteinsDAO, final WarehouseDAO cvRepository) 
	{
		assert dest instanceof Model : 
			"PaxtoolsDAO must also implement org.biopax.paxtools.Model!";
	
		this.mainDAO = dest;
		this.metadataDAO = metadataDAO;
		this.moleculesDAO = moleculesDAO;
		this.proteinsDAO = proteinsDAO;
		this.cvRepository = cvRepository;
		this.simpleMerger = new SimpleMerger(SimpleEditorMap.L3);
	}
	
	
	/**
	 * 'args' is the list of BioPAX/Paxtools Models.
	 * 
	 * @throws ClassCastException
	 * 
	 */
	@Override
	public Set<BioPAXElement> execute(Model model, Object... args) {
		
		for(Object arg : args) {
			Model pathwayModel = (Model) arg;
			mergePathwayModel((PaxtoolsDAO)model, pathwayModel);
		}

		return null; // to ignore
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

			InputStream inputStream = new ByteArrayInputStream(pwdata.getPremergeData());
			Model pathwayModel = simpleReader.convertFromOWL(inputStream);
			mainDAO.runAnalysis(this, pathwayModel);
		}

		if (log.isInfoEnabled()) {
			log.info("merge() complete, exiting...");
		}
	}

		
	/* 
	 * Merges a new pathway model into persistent main model: 
	 * inserts new objects and updates object properties
	 * (and should not break inverse properties).
	 * It re-uses already merged or new Warehouse UtilityClass objects 
	 * (e.g., EntityReference) to replace equivalent ones in the pathway data.
	 * 
	 * Note: active transaction must exist around this method if the main model is a 
	 * persistent model implementation (PaxtoolsHibernateDAO).
	 */
	private void mergePathwayModel(PaxtoolsDAO mainModel, Model pathwayModel) 
	{	
		//we suppose, the pathwayModel is self-integral, 
		//i.e, - no external refs and implicit children
		//(this is almost for sure true if it's just came from a string/file)
		
		// find matching utility class elements in the warehouse or main db
		if(log.isInfoEnabled())
			log.info("mergePathwayModel: looking for equivalent utility class elements, " +
					"either in the main Model or Warehouse, to re-use or merge/use them, " +
					"respectively...");
		//new "replacements" Model
		Model generatedModel = BioPAXLevel.L3.getDefaultFactory().createModel(); 
		final Map<BioPAXElement, BioPAXElement> replacements = new HashMap<BioPAXElement, BioPAXElement>();
		
		// match some utility class objects to ones from the warehouse or previously imported
		for (UtilityClass bpe: new HashSet<UtilityClass>(pathwayModel.getObjects(UtilityClass.class))) 
		{
			UtilityClass replacement = null;
			// Find the best replacement either in the warehouse or target model;
			if (bpe instanceof ProteinReference) {
				replacement = findOrCreate((ProteinReference)bpe, 
						ProteinReference.class, (Model)mainModel, proteinsDAO, generatedModel);
			} else if (bpe instanceof SmallMoleculeReference) {
				replacement = findOrCreate((SmallMoleculeReference)bpe, 
						SmallMoleculeReference.class, (Model)mainModel, moleculesDAO, generatedModel);
			}
// replace CVs? I doubt now (cannot help if the validator/normalizer haven't done already)...
//			else if (bpe instanceof ControlledVocabulary) {
//				replacement = processControlledVocabulary((ControlledVocabulary) bpe, target);
//			}
				
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
					
						// clear the AA sequence?
//						if(replacement instanceof ProteinReference) {
//							if(toReturn != null)
//								toReturn.setSequence(null);
//						}
					
						// in-memory merge to reuse same child xrefs, etc.
						simpleMerger.merge(generatedModel, replacement);
					} 
					
					// associate, continue
					replacements.put(bpe, generatedModel.getByID(id));
				}
			}
		}
				
		// fix entityFeature/entityFeatureOf for sure, and may be other properties...
		if(log.isInfoEnabled())
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
		if(log.isInfoEnabled())
			log.info("mergePathwayModel: replacing utility objects with matching ones...");	
		ModelUtils.replace(pathwayModel, replacements);
		
		if(log.isInfoEnabled())
			log.info("mergePathwayModel: removing replaced/dangling objects...");	
		ModelUtils.removeObjectsIfDangling(pathwayModel, UtilityClass.class);
		//force re-using of matching by id Xrefs, CVs, etc.. from the generated model
		simpleMerger.merge(generatedModel, pathwayModel); 
	
		// create completely detached in-memory model (fixes dangling properties...)
		if(log.isInfoEnabled())
			log.info("mergePathwayModel: updating in-memory model...");
		pathwayModel = ModelUtils.writeRead(generatedModel);
			
		if(log.isInfoEnabled())
			log.info("mergePathwayModel: persisting...");
		// merge to the main model (save/update object relationships)
		mainModel.merge(pathwayModel);
		
		if(log.isInfoEnabled())
			log.info("mergePathwayModel: merge is complete, exiting...");
	}

	
	private <T extends UtilityClass & XReferrable> T findOrCreate(T orig, Class<T> type, 
			Model mainModel, WarehouseDAO whDAO, Model subsModel) 
	{	
		String id = orig.getRDFId();
		
		//First, try to re-use previously matched (in the current merge run) object
		T toReturn = getById(id, type, subsModel, whDAO, mainModel);

		// if not found by id, search by UnificationXrefs -
		if (toReturn == null) { // no match - try with xrefs
			Set<UnificationXref> urefs = new ClassFilterSet<Xref, UnificationXref>(
					orig.getXref(), UnificationXref.class);
			Collection<String> ids = whDAO.findByXref(urefs, type);
			id = null;
			if (!ids.isEmpty()) {
				if (ids.size() == 1) {
					id = ids.iterator().next();
					// again, try to find the existing (already merged) one first
					toReturn = getById(id, type, subsModel, whDAO, mainModel);
				} else {
					log.warn("Several " + type.getSimpleName() + ": " + ids
							+ ", were found in the warehouse by unification xrefs: "
							+ urefs + " of the original object: "
							+ orig.getRDFId());
				}
			}
		}
		
		return toReturn;
	}
	
	
	private <T extends UtilityClass> T getById(final String id, final Class<T> type, 
			final Model tmp, final WarehouseDAO wh, final Model main) 
	{
		assert id != null;
		
		// get from the in-memory model
		T t = type.cast(tmp.getByID(id));
		if (t == null) {
			// second, try - in the main model
			t = type.cast(main.getByID(id));
			if (t == null)
				// third, create new if available in the warehouse
				t = wh.createBiopaxObject(id, type);
			else {
				log.debug(id);
			}
		}
		
		return t;
	}

	
	@Deprecated // why to replace CVs? (won't help if the validator/normalizer haven't already)
	private ControlledVocabulary processControlledVocabulary(ControlledVocabulary bpe, Model target) 
	{
		// get the CV subclass (e.g. CellVocabulary)!
		Class<? extends ControlledVocabulary> clazz =
			(Class<? extends ControlledVocabulary>) bpe.getModelInterface();
		
		ControlledVocabulary toReturn = cvRepository.createBiopaxObject(bpe.getRDFId(), clazz);
		
		// if not found by id, - search by UnificationXrefs
		if (toReturn == null) {
			Set<UnificationXref> urefs = 
				new ClassFilterSet<Xref,UnificationXref>(((XReferrable)bpe).getXref(), UnificationXref.class);
			Collection<String> cvUrns = cvRepository.findByXref(urefs, clazz);
			if (!cvUrns.isEmpty()) {
				if (cvUrns.size() == 1) {
					toReturn = cvRepository.createBiopaxObject(cvUrns.iterator().next(), clazz);
				} else {
					log.warn("Several ControlledVocabulary: " + cvUrns.toString()
						+ "were found in the warehouse by unification xrefs: "
						+ urefs + " of the original CV: " + bpe.getRDFId());
				}
			}
		}
		
		return toReturn;
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
	 * For the given provider, gets the completely detached
	 * in-memory copy of the persisted (pre-merge) model.
	 *
	 * @param metadata Metadata
	 * @return BioPAX Model created during the Premerge stage
	 * 
	 */
	@Deprecated //probably won't use a separate "premerge" DBs in the future
	private Model getPreMergeModel(final Metadata metadata) 
	{
		String dbname = CPathSettings.CPATH_DB_PREFIX + metadata.getIdentifier()
			+ "_" + metadata.getVersion();
		
		// get the PaxtoolsDAO (Model) instance
		PaxtoolsDAO premergePaxtoolsDAO = ImportFactory.buildPaxtoolsHibernateDAO(dbname);
		
		// "detach" the model by export to/import from owl/xml
		return ModelUtils.writeRead((Model) premergePaxtoolsDAO);
	}


	/**
	 * Set/select the pathway data (by metadata 
	 * identifier and version) to merge. Default is
	 * - both are null, which means merge all premerged data.
	 * 
	 * @param identifier
	 * @param version
	 * 
	 * @throws IllegalArgumentException
	 */
	void setPathwayData(String identifier, String version) {
		this.pathwayDataIdentifier = identifier;
		if(this.pathwayDataIdentifier != null)
			this.pathwayDataVersion = version;
		else { //null id - version must be null as well
			assert version == null : "'identifier' is null, whereas 'version' is not: " + version;
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