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

package cpath.warehouse.internal;

//import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.miriam.MiriamLink;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.level3.ControlledVocabulary;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.XReferrable;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.validator.utils.BiopaxOntologyManager;
import org.biopax.validator.utils.Normalizer;

import psidev.ontology_manager.Ontology;
import psidev.ontology_manager.OntologyTermI;

import cpath.config.CPathSettings;
import cpath.warehouse.CvRepository;
import cpath.warehouse.WarehouseDAO;



/**
 * This is to access OBO Cvs:
 * 
 * @author rodch
 *
 */
public class OntologyManagerCvRepository extends BiopaxOntologyManager 
	implements CvRepository, WarehouseDAO 
{
	private static final Log log = LogFactory.getLog(OntologyManagerCvRepository.class);
	private static final String URN_OBO_PREFIX = "urn:miriam:obo.";
	private static BioPAXFactory biopaxFactory = BioPAXLevel.L3.getDefaultFactory();
	
	/**
	 * Constructor
	 * 
	 * @param ontologies ontology config XML resource (for OntologyManager)
	 * @param miriam
	 * @param ontTmpDir
	 * @throws Exception
	 */
	public OntologyManagerCvRepository(Properties ontologies, 
			String ontTmpDir, boolean isReuseAndStoreOBOLocally) 
	{
		super(ontologies, ontTmpDir, isReuseAndStoreOBOLocally);
		
		//Normalize (for safety :)) ontology names using IDs
		for(String id : getOntologyIDs()) {
			String officialName = MiriamLink.getName(id); 
			Ontology o = getOntology(id);
			o.setName(officialName);
		}
	}
	

	/* (non-Javadoc)
	 * @see cpath.warehouse.CvRepository#getByDbAndId(java.lang.String, java.lang.String, java.lang.Class)
	 */
	@Override
	public <T extends ControlledVocabulary> T getControlledVocabulary(
			String db, String id, Class<T> cvClass) 
	{
		OntologyTermI term = null;
		
		Ontology ontology = getOntology(db);
		if(ontology == null) // it may be urn -
			ontology = getOntologyByUrn(db);
		
		if (ontology != null) {
			term = ontology.getTermForAccession(id);
		} else { // still null? well, no problem -
			/*
			 * surprisingly or by design, "accession" is a unique key (through
			 * all ontologies) in the ontology manager
			 */
			term = findTermByAccession(id);
		}
		
		if(term != null) {
			return getControlledVocabulary(term, cvClass);
		} 
		
		return null;
	}


	/* (non-Javadoc)
	 * @see cpath.warehouse.CvRepository#getById(java.lang.String, java.lang.Class)
	 */
	@Override
	public <T extends ControlledVocabulary> T getControlledVocabulary(String urn,
			Class<T> cvClass) 
	{
		OntologyTermI term = getTermByUrn(urn);
		T cv = getControlledVocabulary(term, cvClass);
		if(cv != null)
			cv.addComment(CPathSettings.CPATH2_GENERATED_COMMENT);
		return cv;
	}
	
	
	public Set<String> getDirectChildren(String urn) {
		OntologyTermI term = getTermByUrn(urn);
		Ontology ontology = getOntology(term.getOntologyId());
		Collection<OntologyTermI> terms = ontology.getDirectChildren(term);
		return ontologyTermsToUrns(terms);
	}

	
	public Set<String> getDirectParents(String urn) {
		OntologyTermI term = getTermByUrn(urn);
		Ontology ontology = getOntology(term.getOntologyId());
		Collection<OntologyTermI> terms = ontology.getDirectParents(term);
		return ontologyTermsToUrns(terms);
	}

	
	public Set<String> getAllChildren(String urn) {
		OntologyTermI term = getTermByUrn(urn);
		Ontology ontology = getOntology(term.getOntologyId());
		Collection<OntologyTermI> terms = ontology.getAllChildren(term);
		return ontologyTermsToUrns(terms);
	}

	
	public Set<String> getAllParents(String urn) {
		OntologyTermI term = getTermByUrn(urn);
		Ontology ontology = getOntology(term.getOntologyId());
		Collection<OntologyTermI> terms = ontology.getAllParents(term);
		return ontologyTermsToUrns(terms);
	}
	
	
	public boolean isChild(String parentUrn, String urn) {
		return getAllParents(urn).contains(parentUrn)
			|| getAllChildren(parentUrn).contains(urn);
	}
	
	
	/* ==========================================================================*
	 *        Internal Methods (package-private - for easy testing)              *
	 * ==========================================================================*/
	
	
	<T extends ControlledVocabulary> T getControlledVocabulary(OntologyTermI term,
			Class<T> cvClass) 
	{
		if(term == null)
			return null;
		
		String urn = ontologyTermToUrn(term);
		T cv = biopaxFactory.create(cvClass, urn);
		cv.addTerm(term.getPreferredName());
		
		String ontId = term.getOntologyId(); // like "GO" 
		String db = getOntology(ontId).getName(); // names were fixed in the constructor!
		String rdfid = Normalizer.generateURIForXref(db, term.getTermAccession(), null, UnificationXref.class);
		UnificationXref uref = biopaxFactory.create(UnificationXref.class, rdfid);
		uref.setDb(db); 
		uref.setId(term.getTermAccession());
		cv.addXref(uref);
		
		return cv;
	}
	

	OntologyTermI getTermByUrn(String urn) {
		if(urn.startsWith(URN_OBO_PREFIX)) {
			int l = URN_OBO_PREFIX.length();
			int indexOfTheColon = urn.indexOf(':', l);
			String acc = urn.substring(indexOfTheColon+1);
			acc=URLDecoder.decode(acc);
			String dtUrn = urn.substring(0, indexOfTheColon);				
			OntologyTermI term = findTermByAccession(acc); // acc. is globally unique in OntologyManager!..
			return term;
		} else {
			log.error("Cannot Decode not a Controlled Vocabulary's URI : " + urn);
			return null;
		}
	}
	
	
	/*
	 * Gets Ontology by (Miriam's) datatype URI
	 */
	Ontology getOntologyByUrn(String dtUrn) {
		for (String id : getOntologyIDs()) {
			Ontology ont = getOntology(id);
			String urn = MiriamLink.getDataTypeURI(id);
			if(dtUrn.equalsIgnoreCase(urn)) {
				return ont;
			}
		}
		return null;
	}
	
	
	Set<String> ontologyTermsToUrns(Collection<OntologyTermI> terms) {
		Set<String> urns = new HashSet<String>();
		for(OntologyTermI term : terms) {
			urns.add(ontologyTermToUrn(term));
		}
		return urns;
	}
	
	
	String ontologyTermToUrn(OntologyTermI term) {
		String urn = null;
		if (term != null) {
			String ontologyID = term.getOntologyId();
			String accession = term.getTermAccession();
			urn = MiriamLink.getURI(ontologyID, accession);
		}
		return urn;
	}


	/* (non-Javadoc)
	 * @see cpath.dao.WarehouseDAO#getObject(java.lang.String, java.lang.Class)
	 */
	// TODO validate if the ontology term (form URN) can be used by this CV class?
	@Override
	public <T extends BioPAXElement> T getObject(String urn, Class<T> clazz) {
		return (T) getControlledVocabulary(urn,(Class<ControlledVocabulary>) clazz);
	}

	
	@Override
	public Set<String> getByXref(Set<? extends Xref> xrefs, Class<? extends XReferrable> clazz)
	{
		Set<String> toReturn = new HashSet<String>();
		
		for (Xref xref : xrefs) {
			ControlledVocabulary cv = getControlledVocabulary(xref.getDb(), xref.getId(),
					(Class<ControlledVocabulary>) clazz);
			
			// TODO validate if the ontology terms (form xrefs) can be used by this CV class?
			
			if (cv != null) {
				toReturn.add(cv.getRDFId());
			}
		}

		return toReturn;
	}


	/* (non-Javadoc)
	 * @see cpath.dao.WarehouseDAO#getObject(java.lang.String)
	 */
	@Override
	public BioPAXElement getObject(String urn) {
		throw new UnsupportedOperationException(
			"Use getObject(String urn, Class<T> cvSubclass) instead.");
	}
	
}
