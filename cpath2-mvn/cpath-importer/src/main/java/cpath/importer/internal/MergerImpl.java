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
				log.info("Merging pathway data of " 
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
						if(log.isInfoEnabled())
							log.info("Do '-premerge' first! Skipping: #" 
								+ pwdata.getId() + " - "+ pwdata.toString());
						continue;
					} else if (pwdata.getValid() == false) {
						// has BioPAX errors
						if(log.isInfoEnabled())
							log.info("There are BioPAX errors! Skipping: #" 
								+ pwdata.getId() + " - "+ pwdata.toString());
						continue;
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
		// copy set (to prevent concurrent modification exception)
		Set<UtilityClass> srcElements = 
			new HashSet<UtilityClass>(pathwayModel.getObjects(UtilityClass.class));
		
		// iterate over all the utility-class elements to replace PR/SMR/CVs
		ModelUtils modelUtils = new ModelUtils(pathwayModel);
		for (UtilityClass bpe: srcElements) {	
			UtilityClass replacement = null; // to find in the Warehouse
			
			if (bpe instanceof ProteinReference) {
				replacement = processProteinReference((ProteinReference) bpe);
			} 
			else if (bpe instanceof ControlledVocabulary) { 
				replacement = processControlledVocabulary((ControlledVocabulary) bpe);
			}
			else if (bpe instanceof SmallMoleculeReference) {
				replacement = processSmallMoleculeReference((SmallMoleculeReference)bpe);
			}
			
			if(replacement == null) {
				if (log.isInfoEnabled()) 
					log.info("No match found in the Warehouse: " + bpe.getRDFId() 
						+ " (" + bpe.getModelInterface().getSimpleName() + ") " +
						"will be merged as is.");
			} else {
				// label it by adding a special signature comment,
				replacement.addComment(CPathSettings.CPATH2_GENERATED_COMMENT);
				
				// already emerged from our warehouse?
				UtilityClass existing = (UtilityClass) pathwayModel.getByID(replacement.getRDFId());
				if (existing != null) { // well, nice tricky test comes next ;)
					if(existing.getComment().contains(
							CPathSettings.CPATH2_GENERATED_COMMENT)) {
						// simply re-use it again!
						replacement = existing;
					} else {
						// existing has the same ID as the replacement but it's original (no signature)
						// replace it first
						pathwayModel.replace(existing, replacement);
						modelUtils.removeDependentsIfDangling(existing);
					}
				}
				
				// replace bpe with the new object
				if(bpe != replacement) {
					pathwayModel.replace(bpe, replacement);
					modelUtils.removeDependentsIfDangling(bpe);
					
					if (log.isInfoEnabled()) {
						log.info(bpe.getRDFId() 
							+ " (" + bpe.getModelInterface().getSimpleName() + ") " +
							"is replaced with " + replacement + "(from the warehouse)");	
					}
				}
			}
		}

		// auto-fix object properties and adds lost children
		pathwayModel.repair();
		
		// goodbye dangling utility classes
		// (this should also prevent the hibernate's duplicate PK exceptions...)
		modelUtils.removeObjectsIfDangling(UtilityClass.class);
		
		// cut from external objects, recover inverse properties -
		//pathwayModel = modelUtils.writeRead();

		// finally, merge into the global (persistent) model;
		pcDAO.merge(pathwayModel);
		
		if(log.isInfoEnabled()) {
			log.info("merge(Model pathwayModel) complete, exiting...");
		}
	}

	
	private ProteinReference processProteinReference(ProteinReference bpe) 
	{
		// find specific subclass
		ProteinReference toReturn =
			proteinsDAO.getObject(bpe.getRDFId(), ProteinReference.class);
		
		// if not found by id, - search by UnificationXrefs
		if (toReturn == null) {
			Set<UnificationXref> urefs =
				new ClassFilterSet<UnificationXref>(((XReferrable)bpe).getXref(), UnificationXref.class);
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
					new ClassFilterSet<RelationshipXref>(((XReferrable)bpe).getXref(), RelationshipXref.class);
				prefs = proteinsDAO.getByXref(rrefs, ProteinReference.class);
				if (!prefs.isEmpty()) { 
					if (prefs.size() > 1) {
						log.info("More than one ProteinReference " +
							"that share the same relationship xref weren found:" 
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
				new ClassFilterSet<UnificationXref>(((XReferrable)bpe).getXref(), UnificationXref.class);
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
		
		// this is a pubchem or chebi small molecule reference.
		// get set of unification xrefs for this incoming smr
		// which we will then use to lookup our version of the smr
		// in the warehouse.
		Set<UnificationXref> uxrefs = new ClassFilterSet<UnificationXref>(
				premergeSMR.getXref(), UnificationXref.class);

		// get id of matching smr in our warehouse.  note:
		// all smr in warehouse have at least ChEBI and,
		// possibly, inchi and/or pubchem uxrefs.  Not sure
		// if it is possible that multiple SMRs in our warehouse
		// match the given set of uxrefs.  In any event
		// we return only the first matching id we
		// encounter - see getSmallMoleculeReference() below.
		String chebiUrn = getSmallMoleculeReferenceUrn(uxrefs);
		
		if (chebiUrn != null) { 
			toReturn = moleculesDAO.getObject(chebiUrn, SmallMoleculeReference.class);
		} else {
			log.warn(premergeSMR.getRDFId() + " added 'As Is', " +
			"because nothing's found in Warehouse.");
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
		
		/* the following used to do the same as above two lines...
		// get the complete model from the pre-merge db!
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		premergePaxtoolsDAO.exportModel(outputStream);
		InputStream inputStream = new BufferedInputStream(
				new ByteArrayInputStream(outputStream.toByteArray()));
		SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
		return simpleReader.convertFromOWL(inputStream);
		*/
	}

	
	private <T extends BioPAXElement> T getById(Model model, String urn, Class<T> type) 
	{
		return 
		(model instanceof WarehouseDAO) 
			? ((WarehouseDAO)model).getObject(urn, type) //completely detached
				: (T) model.getByID(urn) ;	
	}

	
	@Deprecated
	private void processBioPAXElementSource(final Metadata metadata, final Model model) {
	
		// some sanity checking
		if (metadata == null) {
			if (log.isInfoEnabled()) {
				log.info("processBioPAXElementSource(), metadata is null!");
				return;
			}
		}

		// get tax id
		String taxId = getTaxID(model);
		if (taxId == null) {
			if (log.isInfoEnabled()) {
				log.info("processBioPAXElementSource(), cannot find taxID!");
				return;
			}
		}
		
		// iterate over all Entity 
		for (Entity entity : (Set<Entity>)model.getObjects(Entity.class)) {
			// we explicitly set props to avoid IdentifierGenerationException
			BioPAXElementSource bes = new BioPAXElementSource();
			bes.setRDFId(entity.getRDFId());
			bes.setTaxId(taxId);
			bes.setProviderId(metadata.getIdentifier());
			metadataDAO.importBioPAXElementSource(bes);
		}
	}
	
	/*
	 * Given a paxtools model for an entire owl file
	 * returns the tax id for the organism.  This method 
	 * assumes that at least one pathway in model is annotated
	 * with a biosource (although all should be).  It also assumes
	 * that all pathways share the the same biosource.  With that in 
	 * mind, it returns the first tax id encountered while iterating 
	 * over each pathways biosource, else returns null.
	 * 
	 * @param model Model
	 * @return String
	 */
	private String getTaxID(final Model model) {

		for (Pathway pathway : model.getObjects(Pathway.class)) {
			BioSource bioSource = pathway.getOrganism();
			if (bioSource != null) {
				for (Xref xref : bioSource.getXref()) {
					if (xref instanceof UnificationXref) {
						if (xref.getDb().contains("taxonomy")) {
							return xref.getId();
						}
					}
				}
			}
		}
		
		return null;
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
	
}