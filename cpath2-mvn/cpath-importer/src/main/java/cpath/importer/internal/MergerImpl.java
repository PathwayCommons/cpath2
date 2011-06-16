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
import org.biopax.paxtools.impl.ModelImpl;
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
		// First, find/collect matching elements from the warehouse
		if(log.isInfoEnabled())
			log.info("merge(pathwayModel): looking for matching utility class elements in the warehouse...");
		final Map<UtilityClass, UtilityClass> replacements = new HashMap<UtilityClass, UtilityClass>();
		for (UtilityClass uce: new HashSet<UtilityClass>(pathwayModel.getObjects(UtilityClass.class))) {
			UtilityClass replacement = getFromWarehouse(uce);
			if(replacement != null) {
				replacements.put(uce, replacement);
			}
		}
		
		// Second, replace utility class object property values if
		// matching element (the replacement) was found in the warehouse
		Visitor visitor = new Visitor() {
			@Override
			public void visit(BioPAXElement domain, Object range, Model model,
					PropertyEditor editor) 
			{
				if (range instanceof UtilityClass && range != null) {
					UtilityClass replacement = replacements.get(range);
					if (replacement != null) {
						String oldId = ((UtilityClass)range).getRDFId();
						String newId = replacement.getRDFId(); // (can be the same, it does not matter)
						editor.setValueToBean(replacement, domain);
						if (editor.isMultipleCardinality()) {
							editor.removeValueFromBean(range, domain);
						}
						// remove the old one
						model.remove((BioPAXElement) range);
						
						if(log.isDebugEnabled())
							log.debug("merge(pathwayModel): replaced - "
								+ oldId + " (" +  range + "; " 
								+ ((UtilityClass) range).getModelInterface().getSimpleName() + ")"
								+ " with " + newId + " (" +  replacement + "); for property: " + editor.getProperty()
								+ " of bean: " + domain.getRDFId() + " (" + domain + "; " 
								+ domain.getModelInterface().getSimpleName() + ")");
					}
				}
			}
		};
		
		if(log.isInfoEnabled())
			log.info("merge(pathwayModel): updating UtilityClass range object properties...");
		EditorMap em = SimpleEditorMap.get(pathwayModel.getLevel());
		Traverser traverser = new Traverser(em, visitor);
		for (BioPAXElement bpe: new HashSet<BioPAXElement>(pathwayModel.getObjects())) {	
			traverser.traverse(bpe, pathwayModel);
		}

		// auto-fix object properties and adds lost children
		// - not required when merging into PaxtoolsDAO persistent model
		// (hibernate will get to all child elements anyway)
		if(pcDAO instanceof ModelImpl)  {
			if(log.isInfoEnabled())
				log.info("merge(pathwayModel): resolving child elements (adding to the model)...");
			pathwayModel.repair();
		}
		
		// find and remove dangling util. elements, -
		// those left after their parent has been replaced
		// (e.g., old ChemicalStructure, Xref, etc..)
		if(log.isInfoEnabled())
			log.info("merge(pathwayModel): cleaning up (to remove dangling utility objects)...");
		ModelUtils mu = new ModelUtils(pathwayModel);
		mu.removeObjectsIfDangling(UtilityClass.class);

		if(log.isInfoEnabled())
			log.info("merge(pathwayModel): persisting...");
		
		// finally, merge into the global (persistent) model;
		pcDAO.merge(pathwayModel);
		
		if(log.isInfoEnabled())
			log.info("merge(pathwayModel): merge is complete, exiting...");
	}

	
	private UtilityClass getFromWarehouse(UtilityClass bpe) {
		UtilityClass replacement = null;
		
		if (bpe instanceof ProteinReference) {
			replacement = processProteinReference((ProteinReference) bpe);
		} 
		else if (bpe instanceof ControlledVocabulary) { 
			replacement = processControlledVocabulary((ControlledVocabulary) bpe);
		}
		else if (bpe instanceof SmallMoleculeReference) {
			replacement = processSmallMoleculeReference((SmallMoleculeReference)bpe);
		}
		
		if(replacement != null)
			replacement.addComment(CPathSettings.CPATH2_GENERATED_COMMENT);
		
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