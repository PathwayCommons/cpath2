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
final class MergerImpl implements Merger {

    private static final Log log = LogFactory.getLog(MergerImpl.class);

	// cpath2 repositories
	private MetadataDAO metadataDAO;
    private PaxtoolsDAO pcDAO;
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
	MergerImpl(final PaxtoolsDAO pcDAO) 
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
		
		this.simpleMerger = new SimpleMerger(SimpleEditorMap.get(((Model)pcDAO).getLevel()));
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
	MergerImpl(final PaxtoolsDAO pcDAO, final MetadataDAO metadataDAO, final WarehouseDAO moleculesDAO,
					  final WarehouseDAO proteinsDAO, final WarehouseDAO cvRepository) 
	{
		this.pcDAO = pcDAO;
		this.metadataDAO = metadataDAO;
		this.moleculesDAO = moleculesDAO;
		this.proteinsDAO = proteinsDAO;
		this.cvRepository = cvRepository;
		this.simpleMerger = new SimpleMerger(SimpleEditorMap.L3);
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
					log.info("merge(): now merging " 
							+ pwdata.getId() + " - "+ pwdata.toString());
					
					if(pwdata.getValid() == null 
						|| pwdata.getPremergeData() == null 
						|| pwdata.getPremergeData().length() == 0) 
					{
						// must run pre-merge first!
						log.warn("Do '-premerge' first! Skipping " 
							+ pwdata.getId() + " - "+ pwdata.toString());
						continue;
					} else if (pwdata.getValid() == false) {
						// has BioPAX errors
						log.warn("There were critical BioPAX errors in " 
							+ pwdata.getId() + " - "+ pwdata.toString());
						if(!isForce()) {
							log.warn("Skipping " + pwdata.getId());
							continue;
						} else {
							log.warn("FORCE merging (ignoring all validation issues) for " + pwdata.getId());
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
		final ModelUtils pathwayModelUtils = new ModelUtils(pathwayModel);
		
		/* create a new in-memory model, where matching warehouse objects 
		 * will be placed to then merge with pathway model objects
		 */
		final Model target = pathwayModel.getLevel().getDefaultFactory().createModel();
		final ModelUtils targetModelUtils = new ModelUtils(target);
		
		// find/collect matching elements from the warehouse
		if(log.isInfoEnabled())
			log.info("merge(pathwayModel): looking for matching utility class elements in the warehouse...");
	
		// match some utility class objects to ones from the warehouse (to replace later)
		// also, generate new URIs and replacement copies for all Entities
		final Map<BioPAXElement, BioPAXElement> replacements = new HashMap<BioPAXElement, BioPAXElement>();
		final ShallowCopy copier = new ShallowCopy();
		for (BioPAXElement b: new HashSet<BioPAXElement>(pathwayModel.getObjects())) 
		{
			BioPAXElement replacement = null;

			if (b instanceof UtilityClass) {
				// Find the best replacement either in the warehouse or target model;
				// it also adds the replacement elements to the target model
				final UtilityClass u = (UtilityClass) b;
				replacement = getFromWarehouse(u, target);
				if (replacement != null) {
					// shelve to replace later
					replacements.put(u, replacement);
					
					if (u instanceof EntityReference) { 
						// merge entity features
						EntityReference er = (EntityReference) u;
						// loop, to ensure entityFeatureOf is also cleared (cannot set null or empty set!)
						for (EntityFeature ef : new HashSet<EntityFeature>(
								er.getEntityFeature())) {
							er.removeEntityFeature(ef);
							((EntityReference) replacement).addEntityFeature(ef);
						}
					}
					
				}

			}
			// extra quick fix for too long URIs...
			if(replacement == null && b.getRDFId().length() > 256) { 
				// set to replace objects (with cpath2-generated URI)
				// having too long URIs 
				String newId = ((Model)pcDAO).getXmlBase() +
					b.getModelInterface().getSimpleName() + 
					"/" + System.currentTimeMillis();
				Level3Element e = (Level3Element) copier.copy(b, newId);
				String rem = "Original URI was too long: " + b.getRDFId();
				e.addComment(rem);
				replacements.put(b, e);
				log.warn("Using new URI=" + newId + "; " + rem);
			}
		}
		
		if(log.isInfoEnabled())
			log.info("merge(pathwayModel): updating object property values in the pathwayModel...");
		
		// replace utility class object property values if replacements were found
		/*
		 * Why we need 'replacements' map and 'replace' method? 
		 * It's mainly because, for some objects, the replacement 
		 * equivalent one will have a different URI. Had they always
		 * the same URI, we could just use target.merge(pathwayModel)
		 * and nothing more...  
		 */
		pathwayModelUtils.replace(replacements);
		pathwayModelUtils.removeObjectsIfDangling(UtilityClass.class);
		
		// merge into target (in-memory)
		if(log.isInfoEnabled())
			log.info("merge(pathwayModel): merging the updated pathwayModel " +
				"into the (in-memory) target model, which already contains " +
				"the replacement (warehouse) objects...");
		target.merge(pathwayModel);
		targetModelUtils.removeObjectsIfDangling(UtilityClass.class);
		
	
		// TODO validate/normalize "target" model once more? 
		// TODO recover entity features and add uni.xrefs to CVs? (not a trivial task: think of different pathwayData and how to update 'entityFeatureOf' of previously saved features...)
		//...
		
		
		//sanity/integrity quick check (enabled by java -ea flag)
		assert assertNoDanglingInverseProps(target);
		
		if(log.isInfoEnabled())
			log.info("merge(pathwayModel): persisting...");
		// finally, merge into the global (persistent) model;
		pcDAO.merge(target);
		
		if(log.isInfoEnabled())
			log.info("merge(pathwayModel): merge is complete, exiting...");
	}

	
	//advanced assertions
	private static boolean assertNoDanglingInverseProps(Model m) {
		final class Print {
			String print(Collection<? extends BioPAXElement> objs) {
				StringBuilder sb = new StringBuilder();
				for(BioPAXElement b : objs) {
					sb.append(b.getRDFId()).append(' ')
					.append(b.getClass().getSimpleName())
					.append('@').append(b.hashCode()).append(", ");
				}
				return sb.toString();
			}
		};
		final Print p = new Print();
		
		for(Xref x : m.getObjects(Xref.class)) {
			for(XReferrable r : x.getXrefOf()) {
				assert m.contains(r) :  
					r.getRDFId() +
					" is not in the target model anymore!" +
					" but it has Xref " + x + ", which belongs to\n" +
					p.print(x.getXrefOf());
			}
		}
		
		return true;
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
		//First, try to get a copy of previously imported object 
		//(this will later allow to update some of properties w/o breaking old refs)
		ProteinReference toReturn = ((WarehouseDAO)pcDAO).getObject(bpe.getRDFId(), ProteinReference.class);
		
		if(toReturn == null) // build a new object using the warehouse
			toReturn = proteinsDAO.getObject(bpe.getRDFId(), ProteinReference.class);
		
		// if not found by id, - search by UnificationXrefs
		if (toReturn == null) {
			Set<UnificationXref> urefs =
				new ClassFilterSet<Xref,UnificationXref>(((XReferrable)bpe).getXref(), UnificationXref.class);
			Collection<String> prefs = proteinsDAO.getByXref(urefs, ProteinReference.class);
			if (!prefs.isEmpty()) { 
				if (prefs.size() == 1) {
					String id = prefs.iterator().next();
					toReturn = ((WarehouseDAO)pcDAO).getObject(id, ProteinReference.class);
					if(toReturn == null)
						toReturn = proteinsDAO.getObject(id, ProteinReference.class);
				} else {
					log.warn("Several ProteinReference: " + prefs 
						+ " were found in the warehouse by unification xrefs: " + urefs 
						+ " of the original ProteinReference: " + bpe.getRDFId());	
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
				if (cvUrns.size() == 1) {
					toReturn = cvRepository.getObject(cvUrns.iterator().next(), clazz);
				} else {
					log.warn("Several ControlledVocabulary: " + cvUrns.toString()
						+ "were found in the warehouse by unification xrefs: "
						+ urefs + " of the original CV: " + bpe.getRDFId());
				}
			}
		}
		
		return toReturn;
	}

	
	private UtilityClass processSmallMoleculeReference(SmallMoleculeReference premergeSMR) 
	{	
		//First, try to get a copy of previously imported object 
		//(this will later allow to update some of properties w/o breaking old refs)
		SmallMoleculeReference toReturn = ((WarehouseDAO)pcDAO).getObject(premergeSMR.getRDFId(), SmallMoleculeReference.class);
				
		if(toReturn == null) // build a new object using the warehouse
			toReturn = moleculesDAO.getObject(premergeSMR.getRDFId(), SmallMoleculeReference.class);
		
		if(toReturn == null) {
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
			// encounter or nothing (won't replace SMR) 
			// - see getSmallMoleculeReference() below.
			String chebiUrn = null;
			Collection<String> smrs = moleculesDAO.getByXref(uxrefs, SmallMoleculeReference.class);
			// TODO someday.., try moleculesDAO.find(..) to search in 'xref.id'
			if (!smrs.isEmpty()) {
				if (smrs.size() == 1) {
					chebiUrn = smrs.iterator().next();
				} else {
					log.warn("Several SMRs: " + smrs 
						+ " were found in the warehouse by unification xrefs: " 
						+ uxrefs + " of the ofiginal SMR: " + premergeSMR.getRDFId());	
				}
			} 
		
			if (chebiUrn != null) { 
				toReturn = ((WarehouseDAO)pcDAO).getObject(chebiUrn, SmallMoleculeReference.class);
				if(toReturn == null)
					toReturn = moleculesDAO.getObject(chebiUrn, SmallMoleculeReference.class);
			}
		}
		
		// a fix, to remove thousands rel.xrefs (later, if required, 
		// one may query the (chebi) warehouse rather the main db 
		// to get those xrefs back...)
		// (TODO - think to remove all member SMRs as well?)
		if(toReturn != null) {
			removeRelXrefs(toReturn);
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
	 * @return BioPAX Model created during the Pre Merge stage
	 * 
	 */
	private Model getPreMergeModel(final Metadata metadata) 
	{
		String dbname = CPathSettings.CPATH_DB_PREFIX + metadata.getIdentifier()
			+ "_" + metadata.getVersion();
		
		// get the PaxtoolsDAO (Model) instance
		PaxtoolsDAO premergePaxtoolsDAO = ImportFactory.buildPremergeDAO(dbname);
		
		// "detach" the model by export to/import from owl/xml
		ModelUtils modelUtils = new ModelUtils((Model) premergePaxtoolsDAO);
		return modelUtils.writeRead();
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