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
	// ref to provider model - used during testing
	private Model pathwayModel;

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
		this.pathwayModel = null;
		
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
		
		simpleMerger = 
			new SimpleMerger(new SimpleEditorMap(BioPAXLevel.L3));
	}

	/**
	 * Constructor.
	 *
	 * This constructor was added to be used in a test context. At least called by
	 * cpath.importer.internal.CPathInMemoryModelMergerTest.testMerger().
	 *
	 * @param pcDAO Model
	 * @param providerPathwayModel Model
	 * @param metadataDAO MetadataDAO
	 * @param moleculesDAO WarehouseDAO
	 * @param proteinsDAO WarehouseDAO
	 * @param cvRepository WarehouseDAO
	 */
	public MergerImpl(final Model pcDAO, final Model providerPathwayModel, final MetadataDAO metadataDAO,
					  final WarehouseDAO moleculesDAO, final WarehouseDAO proteinsDAO, final WarehouseDAO cvRepository) 
	{
		this.pcDAO = pcDAO;
		this.pathwayModel = providerPathwayModel;
		this.metadataDAO = metadataDAO;
		this.moleculesDAO = moleculesDAO;
		this.proteinsDAO = proteinsDAO;
		this.cvRepository = cvRepository;

		simpleMerger = 
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
			pathwayModel = (pathwayModel == null) ? getPreMergeModel(metadata) : pathwayModel;
			Model copyOfPathwayModel = copyModel(pathwayModel); // to prevent concurrent modification exception
			// (these models go to trash after the merge)

			if (copyOfPathwayModel == null) {
				log.info("merge(), error copy pathwayModel for provider: " + metadata.getIdentifier() + ", continuing.");
				continue;
			}

			// iterate over all utility elements in the pathway model
			for (UtilityClass bpe : copyOfPathwayModel.getObjects(UtilityClass.class)) {

				// skip if the element already exists in pcModel
				if(pcModel.getByID(bpe.getRDFId()) != null)
					continue; // already exists
				
				UtilityClass object = null;

				// merge all protein/SM references & controlled vocabularies
				if (bpe instanceof ProteinReference) 
				{
					// find specific subclass (e.g. CellVocabulary)!
					object = proteinsDAO.getObject(bpe.getRDFId(), ProteinReference.class);
					
					// if not found by id, - search by UnificationXrefs
					if (object==null) {
						XReferrable r = (XReferrable) bpe;
						Set<UnificationXref> urefs =
							new ClassFilterSet<UnificationXref>(r.getXref(), UnificationXref.class);

						Collection<String> prefs = proteinsDAO
							.getByXref(urefs, ProteinReference.class);
						
						if(!prefs.isEmpty()) 
							object = proteinsDAO.getObject(prefs.iterator().next(), ProteinReference.class);
						
						if(prefs.size() > 1)
							throw new RuntimeException("Several ProteinReference " +
								"that share the same xref found:" + prefs);
					}
				} else if (bpe instanceof ControlledVocabulary) 
				{
					// get the CV subclass (e.g. CellVocabulary)!
					Class<? extends ControlledVocabulary> clazz = 
						(Class<? extends ControlledVocabulary>) bpe.getModelInterface(); 
					object = cvRepository.getObject(bpe.getRDFId(), clazz);
					
					// if not found by id, - search by UnificationXrefs
					if (object==null) {
						XReferrable r = (XReferrable) bpe;
						Set<UnificationXref> urefs =
							new ClassFilterSet<UnificationXref>(r.getXref(), UnificationXref.class);

						Collection<String> cvs = cvRepository.getByXref(urefs, clazz);
						
						if (!cvs.isEmpty())
							object = cvRepository.getObject(cvs.iterator().next(), clazz);
							
						if (cvs.size() > 1)
							throw new RuntimeException("Several ControlledVocabulary "
								+ "that use the same xref found:" + cvs.toString());
					}
				} else if (bpe instanceof SmallMoleculeReference) {
					
					// this is a (pubchem or chebi...) small molecule reference,
					// get respective 'inchi' one from the warehouse
					SmallMoleculeReference premergeSMR = (SmallMoleculeReference)bpe;
					// using unification xrefs 
					Set<UnificationXref> uxrefs = new 
						ClassFilterSet<UnificationXref>(premergeSMR.getXref(), UnificationXref.class);
					// - to find the 'inchi' SMR in the warehouse
					String inchiUrn = getSmallMoleculeReference(uxrefs);
					// did we find anything?
					if (inchiUrn != null) { 
						if(pcModel.containsID(inchiUrn)) {
							object = (SmallMoleculeReference) pcModel.getByID(inchiUrn);
						} else {
							object = moleculesDAO.getObject(inchiUrn, SmallMoleculeReference.class);
						}
						SmallMoleculeReference inchiSMR = (SmallMoleculeReference) object;
						
						/*
						 *  the following is required for the merge to work properly,
						 *  i.e., premergeSMR must be replaced by the object (inchiSMR)
						 */
						pathwayModel.updateID(premergeSMR.getRDFId(), inchiSMR.getRDFId());
						
						/* because of the preceding line,
						 * premergeSMR will be skipped during the merge,
						 * but its members (mainly xrefs) will be added to 
						 * the target 'inchi' one;
						 * still, lets copy some of the properties manually,
						 * otherwise, SimpleMerger would ignore all data properties 
						 * in premergeSMR and create dangling ones (e.g., ChemicalStructure)
						 */
						// warehouse inchiSMR does not have any chem. formula (but has structure!)
						if(inchiSMR.getChemicalFormula()==null)
							inchiSMR.setChemicalFormula(premergeSMR.getChemicalFormula());
						// premergeSMR may have different ChemicalStructure (not InChi based :))
						if(premergeSMR.getStructure() != null) { 
							// TODO what to do with original structure, if any?
							// remove it
							pathwayModel.remove(premergeSMR.getStructure());
							/*
							inchiSMR.addComment("original structure:" + 
								premergeSMR.getStructure().getStructureFormat() + 
									"; " + premergeSMR.getStructure().getStructureData());
							*/
						}
						// copy some of data properties
						if(premergeSMR.getChemicalFormula() != null)
							inchiSMR.addComment("original formula: " +
									premergeSMR.getChemicalFormula());
						for(String comm : premergeSMR.getComment())
							inchiSMR.addComment(comm);
						for(String name : premergeSMR.getName())
							inchiSMR.addName(name);
						
					} else {
						log.warn(bpe.getRDFId() + " added 'As Is', " +
							"because nothing's found in Warehouse.");
					}
				}

				if (object != null) {
					// add (with all members) if not already there
					if(!pcModel.containsID(object.getRDFId()))
						simpleMerger.merge(pcModel, object);
				} else {
					if(log.isInfoEnabled())
						log.info(bpe.getRDFId() + " wil be added 'As Is', " +
							"because nothing's found in Warehouse.");
				}
			}

			// merge the rest of elements
			pcModel.merge(pathwayModel);

			// trash pathwayModel, we will get new one next iteration
			pathwayModel = null;
			copyOfPathwayModel = null;

			// finally, merge with the database
			pcDAO.merge(pcModel);
		}
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
	
	
	private String getSmallMoleculeReference(Set<UnificationXref> uxrefs) 
	{
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
}