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
import cpath.dao.DataServices;
import cpath.dao.internal.PaxtoolsHibernateDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Metadata.TYPE;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.WarehouseDAO;

import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.util.ClassFilterSet;
import org.biopax.paxtools.controller.AbstractTraverser;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.io.simpleIO.*;

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

    private ApplicationContext whApplicationContext;
    
	// ref to MetadataDAO
	private MetadataDAO metadataDAO;

	// cpath2 repositories
	private Model pcDAO;
    private WarehouseDAO cvRepository;
    private WarehouseDAO moleculesDAO;
    private WarehouseDAO proteinsDAO;

	private final SimpleMerger simpleMerger;

	/**
	 * Constructor.
	 *
	 * @param pcDAO Model target, where to merge data
	 */
	public MergerImpl(final Model pcDAO) 
	{
		this.pcDAO = pcDAO;
		
		// metadata
		whApplicationContext = new ClassPathXmlApplicationContext("classpath:applicationContext-whouseDAO.xml");
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
		
		simpleMerger = new SimpleMerger(new SimpleEditorMap(BioPAXLevel.L3));
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
		for (Metadata metadata : metadataDAO.getAll()) 
		{
			// only process pathway data (we assume only pathway data comes in as biopax)
			if (metadata.getType() != TYPE.BIOPAX) continue;

			// in-memory copy of the persisted model for this provider
			Model pathwayModel = getPreMergeModel(metadata);
			
			// merge
			if(log.isInfoEnabled()) 
				log.info("Merging pathway data: " + metadata.getIdentifier() 
						+ ", " + metadata.getName());
			merge(pathwayModel);
		}
	}

	
	/*
	 * (non-Javadoc)
	 * @see cpath.importer.Merger#merge(org.biopax.paxtools.model.Model)
	 */
	@Override
	public void merge(Model pathwayModel) {

		//create a new temporary in-memory model 
		Model tmpModel = BioPAXLevel.L3.getDefaultFactory().createModel();
		
		// copy the elements set (to prevent concurrent modification exception)
		Set<UtilityClass> srcElements = 
			new HashSet<UtilityClass>(pathwayModel.getObjects(UtilityClass.class));
		
		// iterate over all the utility elements in the pathway model;
		// prepare all the PR/SMR/CVs for merging
		for (UtilityClass bpe: srcElements) {	
			if(!(bpe instanceof SmallMoleculeReference) // replace with 'inchi'
					&& tmpModel.containsID(bpe.getRDFId()) // skip prev. added
				) continue;
			
			// find in the warehouse
			UtilityClass object = null;
			if (bpe instanceof ProteinReference) {
				object = processProteinReference(pathwayModel, bpe);
			}
			else if (bpe instanceof ControlledVocabulary) { 
				object = processControlledVocabulary(pathwayModel, bpe);
			}
			else if (bpe instanceof SmallMoleculeReference) {
				object = processSmallMoleculeReference(tmpModel, pathwayModel, (SmallMoleculeReference)bpe);
			}
			
			if (object != null) {
				/* remove the element from the pathwayModel,
				 * so its properties don't clobber properties of 
				 * the "standard" object that will be merged to 
				 * the target model... wait! - 
				 * this is not required: 
				 * SimpleMerger updates only object properties
				 * */
				 //pathwayModel.remove(bpe);
				
				// hack - for object properties to use the target model's object!
				bpe.setRDFId(object.getRDFId());
								
				// add (with children)
				simpleMerger.merge(tmpModel, object);
				
			} else {
				if (log.isInfoEnabled()) {
					log.info(bpe.getRDFId() + " wil be added 'As Is', " +
						"because nothing's found in Warehouse.");
				}
			}
		}

		// in-memory merge the rest of BioPAX elements
		simpleMerger.merge(tmpModel, pathwayModel);
		
		// goodbye dangling utility classes
		// (this should also prevent the hibernate's duplicate PK exceptions...)
		removeDangling(tmpModel);
		
		// finally, merge into the global model;
		pcDAO.merge(tmpModel);
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
		
		// outta here
		return toReturn;
	}

	
	private UtilityClass processSmallMoleculeReference(Model target, 
			Model pathwayModel, SmallMoleculeReference premergeSMR) 
	{	
		UtilityClass toReturn = null;
		
		// this is a pubchem or chebi small molecule reference.
		// get set of unification xrefs for this incoming smr
		// which we will then use to lookup our version of the smr
		// in the warehouse.
		Set<UnificationXref> uxrefs = new ClassFilterSet<UnificationXref>(premergeSMR.getXref(), UnificationXref.class);

		// get id of matching smr in our warehouse.  note:
		// all smr in warehouse have at least ChEBI and
		// possible inchi, and/or pubchem uxrefs.  not sure
		// if it is possible that multiple SMRs in our warehouse
		// match the given set of uxrefs.  In any event
		// we return only the first matching id we
		// encounter - see getSmallMoleculeReference() below.
		String chebiUrn = getSmallMoleculeReference(uxrefs);

		if (chebiUrn != null) { 
			SmallMoleculeReference smr = moleculesDAO
				.getObject(chebiUrn, SmallMoleculeReference.class);	
			
			// special treat for the old chem. structure
			
			if (premergeSMR.getStructure() != null) {
				ChemicalStructure preStr = premergeSMR.getStructure();
				ChemicalStructure chebiStr =  smr.getStructure();
				// hack for the simplemerger
				//pathwayModel.remove(preStr);
				preStr.setRDFId(chebiStr.getRDFId()); 
			}
			
			toReturn = smr;
		} else {
			log.warn(premergeSMR.getRDFId() + " added 'As Is', " +
			"because nothing's found in Warehouse.");
		}
		
		// outta here
		return toReturn;
	}

	/**
	 * Given a set of unification xrefs (could be ChEBI, PubChem or combination of both),
	 * return an ID to a SMR in our warehouse that matches.
	 * 
	 * @param uxrefs Set<UnificationXref>
	 * @return id String
	 */
	private String getSmallMoleculeReference(Set<UnificationXref> uxrefs) {
		
		String id = null;

		//TODO (instead) try ((PaxtoolsDAO)moleculesDAO).find(..) to find by either 'xref.id' or xref's rdfid...
		
		// will return 'chebi' smr.
		Collection<String> smrs = moleculesDAO.getByXref(uxrefs, SmallMoleculeReference.class);
		if (!smrs.isEmpty()) {
			id = smrs.iterator().next();
			if(smrs.size()>1) {
				// is this real?!
				log.warn("Multiple SMRs " + smrs + " found in Warehouse by: " + uxrefs);
			}
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
		String dbname = CPathSettings.CPATH_DB_PREFIX + metadata.getIdentifier();
		// get the PaxtoolsDAO instance
		PaxtoolsDAO premergePaxtoolsDAO = PremergeImpl.buildPremergeDAO(dbname);

		// get the complete model from the pre-merge db!
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		premergePaxtoolsDAO.exportModel(outputStream);
		InputStream inputStream = new BufferedInputStream(
				new ByteArrayInputStream(outputStream.toByteArray()));
		SimpleReader simpleReader = new SimpleReader(BioPAXLevel.L3);
		Model toReturn = simpleReader.convertFromOWL(inputStream);
		
		// outta here
		return toReturn;
	}

	
	private <T extends BioPAXElement> T getById(Model model, String urn, Class<T> type) 
	{
		return 
		(model instanceof WarehouseDAO) 
			? ((WarehouseDAO)model).getObject(urn, type) //completely detached
				: (T) model.getByID(urn) ;	
	}

	
	/*
	 * recursively removes dangling utility class elements
	 */
	private void removeDangling(Model model) 
	{
		final Collection<UtilityClass> dangling = 
			new HashSet<UtilityClass>(model.getObjects(UtilityClass.class));
		
		AbstractTraverser checker = new AbstractTraverser(new SimpleEditorMap(BioPAXLevel.L3)) 
		{	
			@Override
			protected void visit(Object value, BioPAXElement parent, Model model,
					PropertyEditor editor) {
				if(value instanceof UtilityClass)
					dangling.remove(value); // found, i.e., it is used by another element.
			}
		};
		
		for(BioPAXElement e : model.getObjects()) {
			checker.traverse(e, model);
		}
		
		// get rid of dangling objects
		if(!dangling.isEmpty()) {
			if(log.isInfoEnabled()) 
				log.info(dangling.size() + " BioPAX utility objects " +
						"became dangling during the merge, and they "
						+ " will be deleted...");
			if(log.isDebugEnabled())
				log.debug("to remove (dangling after merge) :" + dangling);

			for(BioPAXElement thing : dangling) {
				model.remove(thing);
				assert !model.contains(thing);
				assert !model.containsID(thing.getRDFId());
			}
			// some may become dangling now, so check again...
			removeDangling(model);
		}
	}
	
}