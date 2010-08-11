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
import cpath.dao.PaxtoolsDAO;
import cpath.dao.DataServices;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Metadata.TYPE;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.WarehouseDAO;

import org.biopax.miriam.MiriamLink;

import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.impl.ModelImpl;
import org.biopax.paxtools.util.ClassFilterSet;
import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.biopax.paxtools.io.simpleIO.SimpleEditorMap;
import org.biopax.paxtools.io.simpleIO.SimpleExporter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.*;

import javax.sql.DataSource;

/**
 * Class responsible for Merging pathway data.
 */
public class MergerImpl implements Merger {

	// log
    private static final Log log = LogFactory.getLog(MergerImpl.class);

	// ref to MetadataDAO
	private MetadataDAO metadataDAO;

	// ref to the main repository
	private Model pcDAO;
    private WarehouseDAO cvRepository;
    private WarehouseDAO moleculesDAO;
    private WarehouseDAO proteinsDAO;

	@Autowired
	private ApplicationContext applicationContext; // gets the parent/existing context

	private final SimpleMerger simpleMerger;

	/**
	 * Constructor.
	 *
	 * @param pcDAO Model;
	 * @param metadataDAO MetadataDAO
	 */
	public MergerImpl(final Model pcDAO, final MetadataDAO metadataDAO) 
	{
		this.pcDAO = pcDAO;
		this.metadataDAO = metadataDAO;
		
		ApplicationContext context = null;
		// molecules
		context = new ClassPathXmlApplicationContext(new String [] {"classpath:applicationContext-whouseMolecules.xml"});
		this.moleculesDAO = (WarehouseDAO)context.getBean("moleculesDAO");
		// proteins
		context = new ClassPathXmlApplicationContext(new String [] {"classpath:applicationContext-whouseProteins.xml"});
		this.proteinsDAO = (WarehouseDAO)context.getBean("proteinsDAO");
		// cvRepository
		context = new ClassPathXmlApplicationContext(new String [] {"classpath:applicationContext-cvRepository.xml"});
		this.cvRepository = (WarehouseDAO)context.getBean("cvFetcher");
		
		simpleMerger = new SimpleMerger(new SimpleEditorMap(BioPAXLevel.L3));
	}

	/**
	 * Constructor.
	 *
	 * This constructor was added to be used in a test context. At least called by
	 * cpath.importer.internal.CPathInMemoryModelMergerTest.testMerger().
	 *
	 * @param pcDAO final "global" Model
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
		this.simpleMerger = 
			new SimpleMerger(new SimpleEditorMap(BioPAXLevel.L3));
	}
	
	/*
	 * (non-Javadoc)
	 * @see cpath.importer.Merger#merge
	 */
	@Override
	public void merge() {

		// iterate over all providers
		for (Metadata metadata : metadataDAO.getAll()) {

			// only process pathway data (we assume only pathway data comes in as biopax)
			if (metadata.getType() != TYPE.BIOPAX && metadata.getType() != TYPE.BIOPAX_L2) continue;

			/*
			 *  create a new in-memory model; 
			 *  this is where all the pathway data (all data sources)
			 *  will be merged before it's saved in the database;
			 *  
			 *  TODO instead, try to save and flush it after each provider... 
			 */
			Model pcModel = new ModelImpl(BioPAXLevel.L3.getDefaultFactory()) {
				@Override
				public void merge(Model source) {
					SimpleMerger simpleMerger = 
						new SimpleMerger(new SimpleEditorMap(BioPAXLevel.L3));
					simpleMerger.merge(this, source);
				}
			};
			
			// in-memory copy of the persisted model for this provider
			Model pathwayModel = getPreMergeModel(metadata);

			// merge
			merge(pcDAO, pathwayModel);

			// finally, merge with the database
			//pcDAO.merge(pcModel);
		}
	}
 
	/**
	 * Merge a new single (provider's pathway) model
	 * into the existing target model.
	 * 
	 * @param target
	 * @param pathwayModel
	 */
	// is package-private - to be available in junit tests
	void merge(Model target, Model pathwayModel) {
		
		Model copyOfPathwayModel = copyModel(pathwayModel); // to prevent concurrent modification exception

		// iterate over all utility elements in the pathway model
		for (UtilityClass bpe : copyOfPathwayModel.getObjects(UtilityClass.class)) {

			/*
			 * skip if the element already exists in pcModel
			 * don't skip small molecule ref.  it may have gotten in from
			 * a previous merge via a shared inchi and is not fully processed
			 * (chebi previously processed now equivalent pubchem is encountered, 
			 * and warehouse version of  pubchem was placed in target via inchi merge
			 */
			if (!(bpe instanceof SmallMoleculeReference) && (target.getByID(bpe.getRDFId()) != null)) {
				continue; // already exists
			}
			
			UtilityClass object = null;

			// merge all protein/SM references & controlled vocabularies
			if (bpe instanceof ProteinReference) {
				object = processProteinReference(pathwayModel, bpe);
			}
			else if (bpe instanceof ControlledVocabulary) { 
				object = processControlledVocabulary(pathwayModel, bpe);
			}
			else if (bpe instanceof SmallMoleculeReference) {
				object = processSmallMoleculeReference(target, pathwayModel, (SmallMoleculeReference)bpe);
			}

			if (object != null) {
				// add (with all members) if not already there
				if(!target.containsID(object.getRDFId())) {
					simpleMerger.merge(target, object);
				}
			}
			else {
				if (log.isInfoEnabled()) {
					log.info(bpe.getRDFId() + " wil be added 'As Is', " +
						"because nothing's found in Warehouse.");
				}
			}
		}

		// merge the rest of elements
		target.merge(pathwayModel);
	}
	
	private UtilityClass processProteinReference(Model pathwayModel, UtilityClass bpe) {

		// find specific subclass
		UtilityClass toReturn =
			proteinsDAO.getObject(bpe.getRDFId(), ProteinReference.class);
		
		// if not found by id, - search by UnificationXrefs
		if (toReturn == null) {
			
			Set<UnificationXref> urefs =
				new ClassFilterSet<UnificationXref>(((XReferrable)bpe).getXref(), UnificationXref.class);
			Collection<String> prefs = proteinsDAO.getByXref(urefs, ProteinReference.class);
			if (!prefs.isEmpty()) { 
				toReturn = proteinsDAO.getObject(prefs.iterator().next(), ProteinReference.class);
			}
			if (prefs.size() > 1) {
				throw new RuntimeException("Several ProteinReference " +
					"that share the same xref found:" + prefs);
			}
		}
		
		if (toReturn != null) {

			/*
			 * if we found a warehouse PR by other rdf, update
			 * pathway model with rdf id of PR from warehouse
			 */
			if (!toReturn.getRDFId().equals(bpe.getRDFId())) {
				// update references to bpe to point to new protein ref
				pathwayModel.updateID(bpe.getRDFId(), toReturn.getRDFId());
			}

			/*
			 *remove bpe protein ref from pathwayModel
			 * so its properties don't clobber properties of 
			 * warehouse protein ref that will be added to target
			 * via merge
			 */
			pathwayModel.remove(bpe);
								
			// remove xrefs of bpe or they will become dangling props
			for(Xref x: ((ProteinReference)bpe).getXref()) {
				pathwayModel.remove(x);
			}
			
			// cannot get rid of biosource -
			// it may be referred to by other entity, like a pathway
		}
		
		// outta here
		return toReturn;
	}
	
	private UtilityClass processControlledVocabulary(Model pathwayModel, UtilityClass bpe) {

		// get the CV subclass (e.g. CellVocabulary)!
		Class<? extends ControlledVocabulary> clazz =
			(Class<? extends ControlledVocabulary>) bpe.getModelInterface();
		UtilityClass toReturn = cvRepository.getObject(bpe.getRDFId(), clazz);
		
		// if not found by id, - search by UnificationXrefs
		if (toReturn == null) {
			
			Set<UnificationXref> urefs = 
				new ClassFilterSet<UnificationXref>(((XReferrable)bpe).getXref(), UnificationXref.class);
			Collection<String> cvs = cvRepository.getByXref(urefs, clazz);
			if (!cvs.isEmpty()) {
				toReturn = cvRepository.getObject(cvs.iterator().next(), clazz);
			}
			if (cvs.size() > 1) {
				throw new RuntimeException("Several ControlledVocabulary "
					+ "that use the same xref found:" + cvs.toString());
			}
		}
		
		if (toReturn != null) {

			/*
			 * if we found a warehouse CV by other rdf, update
			 * pathway model with rdf id of CV from warehouse
			 */
			if (!toReturn.getRDFId().equals(bpe.getRDFId())) {
				// update references to bpe to point to new protein ref
				pathwayModel.updateID(bpe.getRDFId(), toReturn.getRDFId());
			}

			/*
			 *remove bpe cv from pathwayModel
			 * so its properties don't clobber properties of 
			 * warehouse cv ref that will be added to target
			 * via merge
			 */
			pathwayModel.remove(bpe);
								
			// remove xrefs of bpe or they will become dangling props
			for(Xref x: ((ControlledVocabulary)bpe).getXref()) {
				pathwayModel.remove(x);
			}
		}
		
		// outta here
		return toReturn;
	}
	
	private UtilityClass processSmallMoleculeReference(Model target, Model pathwayModel, SmallMoleculeReference premergeSMR) {
		
		UtilityClass toReturn = null;
		
		// this is a (pubchem or chebi...) small molecule reference,
		// get respective 'inchi' one from the warehouse using unification xrefs
		Set<UnificationXref> uxrefs = new ClassFilterSet<UnificationXref>(premergeSMR.getXref(), UnificationXref.class);

		// - to find the 'inchi' SMR in the warehouse
		String inchiUrn = getSmallMoleculeReference(uxrefs);
		
		// did we find anything?
		if (inchiUrn != null) { 
			
			if (target.containsID(inchiUrn)) {
				toReturn = getById(target, inchiUrn, SmallMoleculeReference.class);
			}
			else {
				toReturn = moleculesDAO.getObject(inchiUrn, SmallMoleculeReference.class);
			}
			SmallMoleculeReference inchiSMR = (SmallMoleculeReference)toReturn;

			if (target.getByID(premergeSMR.getRDFId()) != null) {
				/*
				 * remove premergeSMR from pathwayModel
				 * so its properties don't clobber properties of 
				 * premergeSMR that resides in target (placed in 
				 * from a previous merge).  for example, a pubchem
				 * smr that resides in pathwayModel may have gotten into
				 * the target model when its parent inchiSMR was put into
				 * the model.
				 */
				pathwayModel.remove(premergeSMR);
			 }
			 else {
				 /*
				  *  the following is required for the merge to work properly,
				  *  i.e., premergeSMR must be replaced by the object (inchiSMR)
				  *  This ensures that SmallMolecule(s) that refer to this SMR will
				  *  get updated with the inchiSMR rdf id.
				  */
				 pathwayModel.updateID(premergeSMR.getRDFId(), inchiSMR.getRDFId());
				 /*
				  * we now remove inchiSMR from pathwayModel so
				  * its properties (inherited from premergeSMR)
				  * do not clobber properties of inchiSMR in target
				  */
				 pathwayModel.remove(inchiSMR);
			 }
			 
			 /*
			  * now remove dangling property elements (Xrefs, ChemicalStructures)
			  */
			 for (Xref x: premergeSMR.getXref()) {
				 pathwayModel.remove(x);
			 }
			 if (premergeSMR.getStructure() != null) {
				 pathwayModel.remove(premergeSMR.getStructure());
			 }
		}
		else {
			log.warn(premergeSMR.getRDFId() + " added 'As Is', " +
			"because nothing's found in Warehouse.");
		}
		
		// outta here
		return toReturn;
	}
	
	private String getSmallMoleculeReference(Set<UnificationXref> uxrefs) {
		
		String id = null;

		//TODO (instead) try ((PaxtoolsDAO)moleculesDAO).find(..) to find by either 'xref.id' or xref's rdfid...
		
		/* Now (after re-design) that 'inchi' SMRs do not 
		* contain any xrefs but do have other SMRs as member ER,
		* Warehouse should not return more than one SMR!  
		* If it does, let's log a warning (the xrefs
		* in the set are probably about different molecules).
		*/
		// will return one 'inchi' one (does getMemberEntityReferenceOf() lookup internally!)
		Collection<String> smrs = moleculesDAO.getByXref(uxrefs, SmallMoleculeReference.class);
		if(!smrs.isEmpty()) {
			id = smrs.iterator().next();
			if(smrs.size()>1) //is this real?!
				log.warn("Multiple SMRs " + smrs + " found in Warehouse by: " 
					+ uxrefs);
		} 				

		return id;
	}
	
	/**
	 * For the given provider, gets the 
	 * in-memory copy of the persisted 	 * (pre-merge) model.
	 *
	 * @param metadata Metadata
	 * @return Model BioPAX Model created during the Pre Merge stage
	 */
	private Model getPreMergeModel(final Metadata metadata) 
	{
		String metadataIdentifier = metadata.getIdentifier();
		// get the factory bean (not its product, data source bean)
		DataServices dataServices = (DataServices) applicationContext.getBean("&cpath2_meta");
		// create data source
		DataSource pathwayDataSource = dataServices.getDataSource(metadataIdentifier);
		// get the PaxtoolsDAO instance
		PaxtoolsDAO mergePaxtoolsDAO = PremergeImpl.buildPremergeDAO(metadata.getIdentifier(), pathwayDataSource);

		// get the complete model from the pre-merge db!
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		mergePaxtoolsDAO.exportModel(outputStream);
		InputStream inputStream = new BufferedInputStream(
				new ByteArrayInputStream(outputStream.toByteArray()));
		SimpleReader simpleReader = new SimpleReader(BioPAXLevel.L3);
		Model toReturn = simpleReader.convertFromOWL(inputStream);
		
		// outta here
		return toReturn;
	}

	private Model copyModel(final Model modelToCopy) {

		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			(new SimpleExporter(BioPAXLevel.L3)).convertToOWL(modelToCopy, outputStream);
			InputStream inputStream =
				new BufferedInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
			SimpleReader simpleReader = new SimpleReader(BioPAXLevel.L3);
			return simpleReader.convertFromOWL(inputStream);
		}
		catch (IOException e) {
			log.info("copyModel(), error copying pathwayModel.");
		}

		return null;
	}
	
	private <T extends BioPAXElement> T getById(Model model, String urn, Class<T> type) {
	
		return 
		(model instanceof WarehouseDAO) 
			? ((WarehouseDAO)model).getObject(urn, type) //completely detached
				: (T) model.getByID(urn) ;	
	}
	
	/**
	 * Given a SmallMoleculeReference with uxrefs, Find respective 
	 * SmallMoleculeReference in warehouse.  Typically, there is
	 * only one such SMR.  In some cases (bugs?), two or more can be found...
	 *
	 * @param smrWithUXrefs SmallMoleculeReference
	 * @return SmallMoleculeReference
	 * 
	 * @deprecated
	 */
	private SmallMoleculeReference findSmallMoleculeReference(Set<UnificationXref> uxrefs) 
	{
		SmallMoleculeReference toReturn = null;

		//TODO understand why iterate, whereas moleculesDA.getByXref could do for any xrefs?..
		//TODO (instead) try ((PaxtoolsDAO)moleculesDAO).find(..) to find by either 'xref.id' or xref's rdfid...
		
		boolean found = false;
		for (UnificationXref uxref : uxrefs) {
			String urn = MiriamLink.getDataTypeURI(uxref.getDb());
			if ("urn.miriam.chebi".equals(urn) || 
				urn.toLowerCase().startsWith("urn.miriam.pubchem.")
				//urn.miriam.pubchem.substance or urn.miriam.pubchem.compound
			){
				if(log.isInfoEnabled())
					log.info("Looking in Warehouse for a (inchi) SMR " +
						"having its member ER's xref: " + uxref);
				if(found) {
					log.warn("Small molecule reference " + toReturn.getRDFId() + 
						" has been already found by using another xref!");
					continue;
				}
				
				/* Now (after re-design) that 'inchi' SMRs do not 
				 * contain any xrefs but do have other SMRs as member ER,
				 * Warehouse should not return more than one SMR!  
				 * If it does, let's log a warning (the xrefs
				 * in the set are probably about different molecules).
				 */
				// will return one 'inchi' one (does getMemberEntityReferenceOf() lookup internally!)
				Collection<String> smrs = moleculesDAO
					.getByXref(Collections.singleton(uxref), SmallMoleculeReference.class);
				if(!smrs.isEmpty()) {
					String id = smrs.iterator().next();
					toReturn = moleculesDAO.getObject(id, SmallMoleculeReference.class);
					found = true;
					if(smrs.size()>1) //is this real?!
						log.warn("Multiple SMRs " + smrs + " found in Warehouse by: " 
							+ uxref);
				} 				
			}
		}

		return toReturn;
	}
}