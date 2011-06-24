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

// imports
import cpath.importer.Merger;
import cpath.config.CPathSettings;
import cpath.dao.PaxtoolsDAO;
import cpath.dao.internal.PaxtoolsHibernateDAO;
import cpath.warehouse.beans.*;
import cpath.warehouse.beans.Metadata.TYPE;
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
 * Class responsible for Merging pathway data.
 */
public class MergerImpl implements Merger {

    private static final Log log = LogFactory.getLog(MergerImpl.class);

	// cpath2 repositories
	private MetadataDAO metadataDAO;
    private Model pcDAO;
    private WarehouseDAO cvRepository;
    private WarehouseDAO moleculesDAO;
    private WarehouseDAO proteinsDAO;
	private String identifier;
	private String version;
	private boolean useDb;
	private boolean force;
	private SimpleMerger simpleMerger;

	/**
	 * Constructor.
	 *
	 * @param pcDAO Model target, where to merge data
	 */
	public MergerImpl(final Model pcDAO) 
	{
		this.pcDAO = pcDAO;
		
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
		// cvRepository
		context = new ClassPathXmlApplicationContext(new String [] {"classpath:applicationContext-cvRepository.xml"});
		this.cvRepository = (WarehouseDAO)context.getBean("cvFetcher");
		
		this.useDb = false;
		this.force = false;
		
		this.simpleMerger = new SimpleMerger(SimpleEditorMap.get(pcDAO.getLevel()));
	}

	/**
	 * Constructor.
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
	public MergerImpl(final Model pcDAO, final MetadataDAO metadataDAO, final WarehouseDAO moleculesDAO,
					  final WarehouseDAO proteinsDAO, final WarehouseDAO cvRepository) 
	{
		this.pcDAO = pcDAO;
		this.metadataDAO = metadataDAO;
		this.moleculesDAO = moleculesDAO;
		this.proteinsDAO = proteinsDAO;
		this.cvRepository = cvRepository;
		this.simpleMerger = new SimpleMerger(SimpleEditorMap.get(pcDAO.getLevel()));
	}
	
	/*
	 * (non-Javadoc)
	 * @see cpath.importer.Merger#merge
	 */
	@Override
	public void merge() {
		SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
		// iterate over all providers
		for (Metadata metadata : metadataDAO.getAll()) 
		{
			// use filter
			if(identifier != null) {
				if(!metadata.getIdentifier().equals(identifier))
					continue;
				if(version != null)
					if(!metadata.getVersion().equals(version))
						continue;
			}
			
			// only process pathway data
			if (metadata.getType() != TYPE.BIOPAX 
				&& metadata.getType() != TYPE.PSI_MI) 
				continue;

			if(log.isInfoEnabled()) 
				log.info("merge(): reading (normalized) pathway data - " 
					+ metadata + ", " + metadata.getName());
			
			if(useDb) {
				// in-memory copy of the persisted model for this provider/version
				Model pathwayModel = getPreMergeModel(metadata);
				merge(pathwayModel);	
			} 
			else {
				// build models and merge from pathwayData.premergeData
				Collection<PathwayData> data;
				if(version != null) 
					data = metadataDAO.getPathwayDataByIdentifierAndVersion(
						metadata.getIdentifier(), metadata.getVersion());
				else
					data = metadataDAO.getPathwayDataByIdentifier(metadata.getIdentifier());
			
				for(PathwayData pwdata : data) {
					if(pwdata.getValid() == null 
						|| pwdata.getPremergeData() == null 
						|| pwdata.getPremergeData().length() == 0) 
					{
						// must run pre-merge first!
						log.info("Do '-premerge' first! Skipping: " 
							+ pwdata.getId() + " - "+ pwdata.toString());
						continue;
					} else if (pwdata.getValid() == false) {
						// has BioPAX errors
						log.info("There were critical BioPAX errors in " 
							+ pwdata.getId() + " - "+ pwdata.toString());
						if(!isForce()) {
							log.info("Skipping for " + pwdata.getId());
							continue;
						} else {
							log.info("FORCE merging (ignoring all validation issues) for " + pwdata.getId());
						}
					}
				
					InputStream inputStream;
					try {
						inputStream = new ByteArrayInputStream(
							pwdata.getPremergeData().getBytes("UTF-8"));
					} catch (UnsupportedEncodingException e) {
						throw new RuntimeException(e);
					}
					Model pathwayModel = simpleReader.convertFromOWL(inputStream);
					merge(pathwayModel);
				}
			}
		}
		
		if(log.isInfoEnabled()) {
			log.info("merge() complete, exiting...");
		}
	}

	
	/*
	 * (non-Javadoc)
	 * @see cpath.importer.Merger#merge(org.biopax.paxtools.model.Model)
	 */
	@Override
	public void merge(Model pathwayModel) 
	{	
		// build a merged in-memory model
		final Model target = pathwayModel.getLevel().getDefaultFactory().createModel();
		ModelUtils mu = new ModelUtils(target);
		
		// find/collect matching elements from the warehouse
		if(log.isInfoEnabled())
			log.info("merge(pathwayModel): looking for matching utility class elements in the warehouse...");
	
		// a map to relate utility class objects to ones from the warehouse
		final Map<UtilityClass, UtilityClass> replacements = new HashMap<UtilityClass, UtilityClass>();
		for (UtilityClass u: new HashSet<UtilityClass>(pathwayModel.getObjects(UtilityClass.class))) 
		{
			//find the replacement in the warehouse or target model
			// (it also adds the replacement elements to the target model)
			UtilityClass replacement = getFromWarehouse(u, target);
			if(replacement != null) {
				replacements.put(u, replacement);
			}
			
			// remove entity features (cause hibernate exceptions; can be recovered after all)
			if (u instanceof EntityReference) {
				EntityReference er = (EntityReference) u;
				// loop, to ensure entityFeatureOf is also cleared (cannot simple set null or empty set!)
				for (EntityFeature ef : new HashSet<EntityFeature>(er.getEntityFeature())) {
					er.removeEntityFeature(ef);
					assert ef.getEntityFeatureOf() == null;
				}
			}
		}
		
		if(log.isInfoEnabled())
			log.info("merge(pathwayModel): updating object property values in the pathwayModel...");
		
		// replace utility class object property values if replacements were found
		ModelUtils pathwayModelUtils = new ModelUtils(pathwayModel);
		pathwayModelUtils.replace(replacements);
		pathwayModelUtils.removeObjectsIfDangling(UtilityClass.class);
		
		// merge into target (in-memory)
		if(log.isInfoEnabled())
			log.info("merge(pathwayModel): merging the updated pathwayModel " +
				"into the (in-memory) target model, which already contains " +
				"the replacement (warehouse) objects...");
		target.merge(pathwayModel);
		pathwayModel = null; //trash
		
// not required anymore
//		// find and remove dangling util. elements, if any, after their parents 
//		// have been replaced (e.g., old ChemicalStructure, Xref, etc..)
//		if(log.isInfoEnabled())
//			log.info("merge(pathwayModel): cleaning up (to remove dangling utility objects)...");
//		mu.removeObjectsIfDangling(UtilityClass.class);
		
//		//sanity checks
//		if(log.isInfoEnabled())
//			log.info("merge(pathwayModel): checking...");
//		for(BioPAXElement e : replacements.keySet()) {
//			assert !target.contains(e) 
//				: "old element "+ e +"is still in the model!";
//			BioPAXElement r = replacements.get(e);
//			assert target.containsID(r.getRDFId()) 
//				: "replacement element ID "+ r.getRDFId() +" is not in the model!";
//			assert target.contains(r) 
//				: "replacement element "+ r +" is not in the model!";
//			if(e instanceof EntityReference)
//				assert ((EntityReference) e).getEntityFeature().isEmpty()
//					: e.getRDFId() + " - entityFeature is still not empty!";
//		}
//		for(EntityFeature ef : target.getObjects(EntityFeature.class)) {
//			assert ef.getEntityFeatureOf() == null
//			: ef.getRDFId() + " - entityFeatureOf still links to "
//				+ ef.getEntityFeatureOf().getRDFId();
//		}
		
		
		if(log.isInfoEnabled())
			log.info("merge(pathwayModel): persisting...");
		// finally, merge into the global (persistent) model;
		pcDAO.merge(target);
		
		if(log.isInfoEnabled())
			log.info("merge(pathwayModel): merge is complete, exiting...");
	}

	
	private UtilityClass getFromWarehouse(UtilityClass bpe, Model cache) {
		UtilityClass replacement = null;

		if (bpe instanceof ProteinReference) {
			replacement = processProteinReference((ProteinReference) bpe);
		} else if (bpe instanceof ControlledVocabulary) {
			replacement = processControlledVocabulary((ControlledVocabulary) bpe);
		} else if (bpe instanceof SmallMoleculeReference) {
			replacement = processSmallMoleculeReference((SmallMoleculeReference) bpe);
		}

		if (replacement != null) {
			String id = replacement.getRDFId();
			// searching the cache, because the warehouse DAO builds a new java object every time
			// (for the same query, we actually want the same instance); multiple objects with the same ID
			// will cause exceptions during the merge to the DB)
			if (!cache.containsID(id)) {
//				cache.add(replacement);
				simpleMerger.merge(cache, replacement);
			} else {
				if(log.isDebugEnabled())
					log.debug("getFromWarehouse(bpe, cache): cache hit: " + id);
				// get from the cache model
				replacement = (UtilityClass) cache.getByID(id);
			}
		}

		return replacement;
	}

	private ProteinReference processProteinReference(ProteinReference bpe) 
	{
		// find specific subclass
		ProteinReference toReturn =
			proteinsDAO.getObject(bpe.getRDFId(), ProteinReference.class);
		
		// if not found by id, - search by UnificationXrefs
		if (toReturn == null) {
			Set<UnificationXref> urefs =
				new ClassFilterSet<Xref,UnificationXref>(((XReferrable)bpe).getXref(), UnificationXref.class);
			Collection<String> prefs = proteinsDAO.getByXref(urefs, ProteinReference.class);
			if (!prefs.isEmpty()) { 
				if (prefs.size() > 1) {
					throw new RuntimeException("Several ProteinReference " +
						"that share the same xref found:" + prefs);	
				}
				toReturn = proteinsDAO.getObject(prefs.iterator().next(), ProteinReference.class);
			} else {
				// use relationship xrefs (refseq, entrez gene,..)
				Set<RelationshipXref> rrefs =
					new ClassFilterSet<Xref,RelationshipXref>(((XReferrable)bpe).getXref(), RelationshipXref.class);
				prefs = proteinsDAO.getByXref(rrefs, ProteinReference.class);
				if (!prefs.isEmpty()) { 
					if (prefs.size() > 1) {
						log.warn("More than one warehouse ProteinReferences " +
							"that share the same relationship xref were found:" 
							+ prefs + ". Skipping (TODO: choose one).");	
					} else {					
						toReturn = proteinsDAO.getObject(prefs.iterator().next(), ProteinReference.class);
						log.warn("ProteinReference: " + bpe +  " will be replaced "
							+ "with the one found in the warehouse by RelationshipXref"
							+ " (not by unification xref): " + prefs);
					}
				}
			}
		}
		
		// a quick fix, saving/query time optimization
		if(toReturn != null)
			toReturn.setSequence(null);
		
		return toReturn;
	}

	
	private ControlledVocabulary processControlledVocabulary(ControlledVocabulary bpe) 
	{
		// get the CV subclass (e.g. CellVocabulary)!
		Class<? extends ControlledVocabulary> clazz =
			(Class<? extends ControlledVocabulary>) bpe.getModelInterface();
		
		ControlledVocabulary toReturn = cvRepository.getObject(bpe.getRDFId(), clazz);
		
		// if not found by id, - search by UnificationXrefs
		if (toReturn == null) {
			
			Set<UnificationXref> urefs = 
				new ClassFilterSet<Xref,UnificationXref>(((XReferrable)bpe).getXref(), UnificationXref.class);
			Collection<String> cvUrns = cvRepository.getByXref(urefs, clazz);
			if (!cvUrns.isEmpty()) {
				toReturn = cvRepository.getObject(cvUrns.iterator().next(), clazz);
			}
			if (cvUrns.size() > 1) {
				throw new RuntimeException("Several ControlledVocabulary "
					+ "that use the same xref found:" + cvUrns.toString());
			}
		}
		
		return toReturn;
	}

	
	private UtilityClass processSmallMoleculeReference(SmallMoleculeReference premergeSMR) 
	{	
		SmallMoleculeReference toReturn = null;

		// try by ID first (should work if properly normalized)
		toReturn = moleculesDAO.getObject(premergeSMR.getRDFId(), SmallMoleculeReference.class);
		if(toReturn != null) {
			return toReturn;
		}
		
		// If not found by id, we search by UnificationXrefs
		
		// This is a pubchem or chebi small molecule reference.
		// Let's get the set of its unification xrefs,
		// which we will then use to lookup our version of the smr
		// in the warehouse.
		Set<UnificationXref> uxrefs = new ClassFilterSet<Xref,UnificationXref>(
				premergeSMR.getXref(), UnificationXref.class);

		// Get id of matching smr in our warehouse.  Note:
		// all smr in warehouse have at least ChEBI and,
		// possibly, inchi and/or pubchem uxrefs.  Not sure
		// if it is possible that multiple SMRs in our warehouse
		// match the given set of uxrefs.  In any event
		// we return only the first matching id we
		// encounter - see getSmallMoleculeReference() below.
		String chebiUrn = getSmallMoleculeReferenceUrn(uxrefs);
		
		if (chebiUrn != null) { 
			toReturn = moleculesDAO.getObject(chebiUrn, SmallMoleculeReference.class);
		}
		
		return toReturn;
	}

	/**
	 * Given a set of unification xrefs (could be ChEBI, PubChem or combination of both),
	 * return an ID to a matching (equivalent) SMR in our warehouse.
	 * 
	 * @param uxrefs
	 * @return
	 */
	private String getSmallMoleculeReferenceUrn(Set<UnificationXref> uxrefs) 
	{	
		String id = null;
		// will return 'chebi' SMRs
		Collection<String> smrs = moleculesDAO.getByXref(uxrefs, SmallMoleculeReference.class);
		if (!smrs.isEmpty()) {
			id = smrs.iterator().next();
			if(smrs.size()>1) {
				// we believe it is hardly possible...
				if(log.isWarnEnabled())
					log.warn("Multiple SMRs " + smrs + 
						" found in Warehouse by: " + uxrefs);
			}
		} 
		
		// TODO someday.., try moleculesDAO.find(..) to search in 'xref.id'
		
		return id;
	}
	
	/**
	 * For the given provider, gets the completely detached
	 * in-memory copy of the persisted (pre-merge) model.
	 *
	 * @param metadata Metadata
	 * @return BioPAX Model created during the Pre Merge stage
	 * 
	 */
	private Model getPreMergeModel(final Metadata metadata) 
	{
		String dbname = CPathSettings.CPATH_DB_PREFIX + metadata.getIdentifier()
			+ "_" + metadata.getVersion();
		
		// get the PaxtoolsDAO (Model) instance
		PaxtoolsDAO premergePaxtoolsDAO = PremergeImpl.buildPremergeDAO(dbname);
		
		// "detach" the model by export to/import from owl/xml
		ModelUtils modelUtils = new ModelUtils(premergePaxtoolsDAO);
		return modelUtils.writeRead();
	}

	
	private <T extends BioPAXElement> T getById(Model model, String urn, Class<T> type) 
	{
		return 
		(model instanceof WarehouseDAO) 
			? ((WarehouseDAO)model).getObject(urn, type) //completely detached
				: (T) model.getByID(urn) ;	
	}

	
	public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}

	public boolean isUseDb() {
		return useDb;
	}
	public void setUseDb(boolean useDb) {
		this.useDb = useDb;
	}

	public boolean isForce() {
		return force;
	}
	public void setForce(boolean forceInvalid) {
		this.force = forceInvalid;
	}
}