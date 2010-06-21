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
import cpath.dao.DataServices;
import cpath.dao.PaxtoolsDAO;
import cpath.dao.internal.DataServicesFactoryBean;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.CPathWarehouse;
import cpath.warehouse.MetadataDAO;

import org.biopax.paxtools.io.simpleIO.SimpleEditorMap;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.ControlledVocabulary;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.UtilityClass;
import org.biopax.paxtools.model.level3.XReferrable;
import org.biopax.paxtools.util.ClassFilterSet;
import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.miriam.MiriamLink;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import javax.sql.DataSource;

/**
 * Class responsible for Merging pathway data.
 */
public final class MergerImpl implements Merger {

	// log
    private static final Log log = LogFactory.getLog(MergerImpl.class);

	// ref to MetadataDAO
	private MetadataDAO metadataDAO;

	// ref to pc repository
	private PaxtoolsDAO pcDAO;

	// ref to the warehouse
	private CPathWarehouse cpathWarehouse;
	
	@Autowired
	private ApplicationContext applicationContext;


	/**
	 *
	 * Constructor.
	 *
	 * @param pcDAO PaxtoolsDAO;
     * @param metadataDAO MetadataDAO
	 * @param cPathWarehouse CPathWarehouse
	 */
	public MergerImpl(final PaxtoolsDAO pcDAO,
					  final MetadataDAO metadataDAO,
					  final CPathWarehouse cpathWarehouse) 
	{
		// init members
		this.pcDAO = pcDAO;
		this.metadataDAO = metadataDAO;
		this.cpathWarehouse = cpathWarehouse;
	}

    /*
	 * (non-Javadoc)
	 * @see cpath.importer.Merger#merge
	 */
	@Override
	public void merge() {
		
		SimpleMerger simpleMerger = new SimpleMerger(new SimpleEditorMap(BioPAXLevel.L3));

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
				if ((bpe instanceof ProteinReference ||bpe instanceof ControlledVocabulary)
					&& !elementExistsInPC) {

					// find specific subclass (e.g. CellVocabulary)!
					Class<? extends UtilityClass> clazz = 
						(Class<? extends UtilityClass>) bpe.getModelInterface(); 
					UtilityClass object = cpathWarehouse.getObject(bpe.getRDFId(), clazz);
					
					// if not found by id, - search by UnificationXrefs
					if (object==null) {
						XReferrable r = (XReferrable)bpe; // because PR, SMR, and CV are XReferrable
						Set<UnificationXref> urefs =
							new ClassFilterSet<UnificationXref>(r.getXref(), UnificationXref.class);
						object = cpathWarehouse.getObject(urefs, clazz);
					}
					
					// TODO if not found, shall we search by RelationshipXrefs?.. ;)
					//...
					
					if (object != null) {
						pcModel.add(object);
					}
					else {
						utilityClassNotFoundInWarehouse(pcModel, bpe);
					}
				}
				else if (bpe instanceof SmallMoleculeReference) {

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

			// local merge
			simpleMerger.merge(pcModel, pathwayModel);
		}
		
		// final merge
		simpleMerger.merge(pcDAO, pcModel);
	}

	/**
	 * For the given provider, gets the persisted model.
	 *
	 * @param metadata Metadata
	 * @return Model
	 */
	private Model getModel(final Metadata metadata) {

		// get the factory bean (not its product, data source bean)
		DataServices dataServices = (DataServices) applicationContext.getBean("&cpath2_meta");
		// create data source
		DataSource pathwayDataSource = dataServices.getDataSource(metadata.getIdentifier());

		// set custom datasource (replaces old one); name matters!
		DataServicesFactoryBean.getDataSourceMap().put("premergeDataSource", pathwayDataSource);
		
		// exactly the same context configuration is now used in "premerge" and "merge"!
		ApplicationContext context = 
			new ClassPathXmlApplicationContext("classpath:internalContext-premerge.xml");
		// get a ref to PaxtoolsDAO
		PaxtoolsDAO mergePaxtoolsDAO = (PaxtoolsDAO)context.getBean("premergePaxtoolsDAO");

		// create a model, grab all elements from db and add to model
		Model toReturn = BioPAXLevel.L3.getDefaultFactory().createModel();
		
		Set<BioPAXElement> allElements = mergePaxtoolsDAO.getElements(BioPAXElement.class, true);
		for (BioPAXElement bpe : allElements) {
			toReturn.add(bpe);
		}

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
					Set<UnificationXref> uxrefsToSearch = new HashSet<UnificationXref>();
					uxrefsToSearch.add(uxref);
					SmallMoleculeReference smr =
						cpathWarehouse.getObject(uxrefsToSearch, SmallMoleculeReference.class);
					if (smr != null) {
						toReturn.add(smr);
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
		// 1) change rdf id of incoming smr to inchi smr id
		String incomingRDFId = incomingSMR.getRDFId();
		String inchiRDFId = inchiSMR.getRDFId();
		incomingSMR.setRDFId(inchiRDFId);
		// 2) update id map of incoming model and replace id of incoming smr with inchi smr id
		Map<String, BioPAXElement> pathwayModelMap = pathwayModel.getIdMap();
		pathwayModelMap.remove(incomingRDFId);
		pathwayModelMap.put(inchiRDFId, inchiSMR);
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