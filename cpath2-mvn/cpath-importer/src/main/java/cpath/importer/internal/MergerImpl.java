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
import cpath.dao.CPathWarehouse;
import cpath.dao.DataServices;
import cpath.dao.PaxtoolsDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.MetadataDAO;

import org.biopax.paxtools.io.simpleIO.SimpleEditorMap;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.biopax.paxtools.model.*;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.util.ClassFilterSet;
import org.biopax.miriam.MiriamLink;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

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
	private PaxtoolsDAO pcDAO;

    private CPathWarehouse cvRepository;
    private CPathWarehouse moleculesDAO;
    private CPathWarehouse proteinsDAO;
	
	@Autowired
	private ApplicationContext applicationContext; // gets the parent/existing context


	/**
	 *
	 * Constructor.
	 *
	 * @param pcDAO PaxtoolsDAO;
	 * @param metadataDAO MetadataDAO
	 * @param cPathWarehouse CPathWarehouse
	 */
	public MergerImpl(final PaxtoolsDAO pcDAO,
					  final MetadataDAO metadataDAO) 
	{
		// init members
		this.pcDAO = pcDAO;
		this.metadataDAO = metadataDAO;
		ApplicationContext context = null;
		// molecules
		context = new ClassPathXmlApplicationContext(new String [] {"classpath:applicationContext-whouseMolecules.xml"});
		this.moleculesDAO = (CPathWarehouse)context.getBean("moleculesDAO");
		// proteins
		context = new ClassPathXmlApplicationContext(new String [] {"classpath:applicationContext-whouseProteins.xml"});
		this.proteinsDAO = (CPathWarehouse)context.getBean("proteinsDAO");
		// cvRepository
		context = new ClassPathXmlApplicationContext(new String [] {"classpath:applicationContext-cvRepository.xml"});
		this.cvRepository = (CPathWarehouse)context.getBean("cvFetcher");
	}

    /*
	 * (non-Javadoc)
	 * @see cpath.importer.Merger#merge
	 */
	@Override
	public void merge() {
		// create pc model
		Model pcModel= BioPAXLevel.L3.getDefaultFactory().createModel();

		// iterate over all providers
		for (Metadata metadata : metadataDAO.getAll()) {

			// get the persisted model for this provider
			Model pathwayModel = getModel(metadata);

			// iterate over all elements in pathway model
			for (BioPAXElement bpe : pathwayModel.getObjects()) {

				// check if element already exists in pc
				boolean elementExistsInPC = (pcModel.getByID(bpe.getRDFId()) != null);

				// merge all protein/SM references & controlled vocabularies
				if (bpe instanceof ProteinReference && !elementExistsInPC) 
				{
					// find specific subclass (e.g. CellVocabulary)!
					ProteinReference object = proteinsDAO.getObject(bpe.getRDFId(), ProteinReference.class);
					
					// if not found by id, - search by UnificationXrefs
					if (object==null) {
						XReferrable r = (XReferrable) bpe;
						Set<UnificationXref> urefs =
							new ClassFilterSet<UnificationXref>(r.getXref(), UnificationXref.class);
						
						Collection<ProteinReference> prefs = proteinsDAO
							.getObjects(urefs, ProteinReference.class);
						
						if(!prefs.isEmpty()) 
							object = prefs.iterator().next();
						
						if(prefs.size()==1)
							throw new RuntimeException("Several ProteinReference " +
								"that share the same xref found:" + prefs.toString());
					}
					
					if (object != null) {
						pcModel.add(object);
					} else {
						utilityClassNotFoundInWarehouse(pcModel, bpe);
					}
				} else if (bpe instanceof ControlledVocabulary && !elementExistsInPC) 
				{
					// get the CV subclass (e.g. CellVocabulary)!
					Class<? extends ControlledVocabulary> clazz = 
						(Class<? extends ControlledVocabulary>) bpe.getModelInterface(); 
					ControlledVocabulary object = cvRepository.getObject(bpe.getRDFId(), clazz);
					
					// if not found by id, - search by UnificationXrefs
					if (object==null) {
						XReferrable r = (XReferrable) bpe;
						Set<UnificationXref> urefs =
							new ClassFilterSet<UnificationXref>(r.getXref(), UnificationXref.class);

						Collection<? extends ControlledVocabulary> cvs = cvRepository.getObjects(urefs, clazz);
						
						if (!cvs.isEmpty())
							object = cvs.iterator().next();
							
						if (cvs.size() > 1)
							throw new RuntimeException("Several ControlledVocabulary "
								+ "that use the same xref found:" + cvs.toString());
					}
					
					if (object != null) {
						pcModel.add(object);
					} else {
						utilityClassNotFoundInWarehouse(pcModel, bpe);
					}
				} else if (bpe instanceof SmallMoleculeReference) {

					// this is a pubchem or chebi small molecule reference
					// get respective inchi small molecule reference from warehouse
					Set<SmallMoleculeReference> inchiSmallMoleculeReferences = 
						getSmallMoleculeReference((SmallMoleculeReference)bpe);

					// did we find inchi SMR in warehouse - should only be one
					if (inchiSmallMoleculeReferences.size() == 1) {
						SmallMoleculeReference inchiSMR = inchiSmallMoleculeReferences.iterator().next();
						// if inchi smr is not already in pc, merge it
						if (pcModel.getByID(inchiSMR.getRDFId()) == null) {
							mergeSmallMolecules(pcModel, pathwayModel, (SmallMoleculeReference)bpe, inchiSMR);
						}
					}
					else {
						// we have a problem
						utilityClassNotFoundInWarehouse(pcModel, bpe);
					}
				}
			}

			// local merge (id-based)
			pcModel.merge(pathwayModel);
		}
		
		// final merge
		pcDAO.merge(pcModel);
	}

	/**
	 * For the given provider, gets the persisted model.
	 *
	 * @param metadata Metadata
	 * @return Model
	 */
	private Model getModel(final Metadata metadata) {

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
	 * SmallMoleculeReferences in warehouse.  Typically, the set returned
	 * should only contain one smr.  In some cases, two can be returned,
	 * for example, given an inchi smr, find the pubchem and chebi smrs
	 * in the warehouse.
	 *
	 * @param smrWithUXrefs SmallMoleculeReference
	 * @return Set<SmallMoleculeReference>
	 */
	private Set<SmallMoleculeReference> getSmallMoleculeReference(SmallMoleculeReference smrWithUXrefs) {

		Set<SmallMoleculeReference> toReturn = new HashSet<SmallMoleculeReference>();

		// the set of unification xrefs for this incoming smr
		Set<UnificationXref> uxrefs = 
			new ClassFilterSet<UnificationXref>(smrWithUXrefs.getXref(), UnificationXref.class);

		// iterate over all smr uxrefs and look for chebio or pubchem
		for (UnificationXref uxref : uxrefs) {
			try {
				String urn = MiriamLink.getDataTypeURI(uxref.getDb());
				if (("urn.miriam.chebi").equals(urn) || ("urn.miriam.pubchem").equals(urn)) {
					// this uxref is to chebi or pubchem.  we search warehouse on each uxref
					// because the warehouse may find more than one smr if a set of uxrefs
					// are passed simulataneously.  for example, if we were to pass the set
					// of uxrefs contains in the inchi smr, we could get back both 
					// a pubchem and a chebi smr
					Collection<SmallMoleculeReference> smrs =
						moleculesDAO.getObjects(Collections.singleton(uxref), SmallMoleculeReference.class);
					if (!smrs.isEmpty()) {
						toReturn.addAll(smrs);
					}
				}
			}
			catch (IllegalArgumentException e) {
				log.error("Unknown or misspelled database name! Won't fix this now... " + e);
			}
		}

		// outta here
		return toReturn;
	}

	/**
	 * Given an inchi small molecule reference, find pubchem and chebi small molecule
	 * references and merge into pc.
	 * 
	 *
	 * @param pcModel Model
	 * @param pathwayModel Model
	 * @param incomingSMR SmallMoleculeReference
	 * @param inchiSMR SmallMoleculeReference
	 */
	private void mergeSmallMolecules(Model pcModel, Model pathwayModel,
									 SmallMoleculeReference incomingSMR, SmallMoleculeReference inchiSMR) {

		// using inchi smr, get pubchem and chebi smr's
		Set<SmallMoleculeReference> smrs = getSmallMoleculeReference(inchiSMR);

		// sanity check
		if (smrs.size() == 0) {
			// we have a problem
			utilityClassNotFoundInWarehouse(pcModel, inchiSMR);
			return;
		}
		
		// merge pubchem and chebi info into inchi smr
		SmallMoleculeMerger smallMoleculeMerger =
			new SmallMoleculeMerger(new SimpleEditorMap(BioPAXLevel.L3));
		smallMoleculeMerger.merge(inchiSMR, smrs);

		// add inchi smr into pc
		pcModel.add(inchiSMR);

		// the follow steps are required for simple merger to work properly
		pathwayModel.remove(incomingSMR);
		pathwayModel.add(inchiSMR);
		
		/* [igor] commented out because the above is much simpler and more natural way to go.
		// 1) change rdf id of incoming smr to inchi smr id
		String incomingRDFId = incomingSMR.getRDFId();
		String inchiRDFId = inchiSMR.getRDFId();
		incomingSMR.setRDFId(inchiRDFId); // [igor] doesn't make sense, because the following pathwayModelMap.remove(incomingRDFId) removes the object anyway!
		// 2) update id map of incoming model and replace id of incoming smr with inchi smr id
		Map<String, BioPAXElement> pathwayModelMap = pathwayModel.getIdMap();
		pathwayModelMap.remove(incomingRDFId); // [igor] this removes the object from the model!
		pathwayModelMap.put(inchiRDFId, inchiSMR); // adds the object to the model, bypassing the natural 'add' method's checks, why?..
		*/
	}

	/**
	 * Handler called when desired UtilityClass is not found in warehouse.
	 *
	 * @param pcModel Model
	 * @param bpe BioPAXElement
	 */
	private void utilityClassNotFoundInWarehouse(Model pcModel, BioPAXElement bpe) {

		// TODO log error, add the bpe as is?
		pcModel.add(bpe);
		log.warn(bpe.getRDFId() + " added 'As Is', because nothing's found in Warehouse.");
	}
}