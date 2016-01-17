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


import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.biopax.paxtools.model.level3.*;
import org.biopax.psidev.ontology_manager.OntologyTermI;
import org.biopax.psidev.ontology_manager.impl.OntologyTermImpl;
import org.junit.Test;

import cpath.service.OntologyManagerCvRepository;

/**
 * This tests are for CVs only (not using DAO);
 * other tests are in the cpath-importer module.
 * 
 * @author rodche
 *
 */
public class WarehouseCVsTest {

	static OntologyManagerCvRepository cvRepository;	
	
	static {
		final Properties cfg = new Properties();
		cfg.put("MI", "classpath:mi.obo");
		cfg.put("MOD", "classpath:mod.obo");
		cfg.put("GO", "classpath:go.obo");
		cvRepository = new OntologyManagerCvRepository(cfg);
	}
	
	@Test
	public void ontologyLoading() {
		Collection<String> ontologyIDs = cvRepository.getOntologyIDs();
		assertTrue(ontologyIDs.contains("GO"));
		assertEquals("gene ontology", cvRepository.getOntology("GO").getName().toLowerCase());
		assertTrue(ontologyIDs.contains("MI"));
		assertTrue(ontologyIDs.contains("MOD"));
	}

	/**
	 * Test method for
	 * {@link OntologyManagerCvRepository#ontologyTermsToUris(java.util.Collection)}
	 */
	@Test
	public final void testOntologyTermsToUris() {
		OntologyTermI term = new OntologyTermImpl("GO", "GO:0005654", "nucleoplasm");
		Set<OntologyTermI> terms = new HashSet<OntologyTermI>();
		terms.add(term);
		Set<String> urns = cvRepository.ontologyTermsToUris(terms);
		assertFalse(urns.isEmpty());
		assertTrue(urns.size() == 1);
		String urn = urns.iterator().next();
		assertEquals("http://identifiers.org/go/GO:0005654", urn);
	}

	/**
	 * Test method for
	 * {@link OntologyManagerCvRepository#ontologyTermsToUris(java.util.Collection)}
	 */
	@Test
	public final void testSearchForTermByAccession() {
		OntologyTermI term = cvRepository.findTermByAccession("GO:0005654");
		assertNotNull(term);
		assertEquals("nucleoplasm", term.getPreferredName());
	}

	@Test
	public final void testSearchMODForTermByAccession() {
		OntologyTermI term = cvRepository.findTermByAccession("MOD:00046");
		assertNotNull(term);
		assertEquals("MOD", term.getOntologyId());
	}
	
	@Test
	public final void testGetDirectChildren() {
		Set<String> dc = cvRepository.getDirectChildren("urn:miriam:obo.go:GO%3A0005654");
		assertFalse(dc.isEmpty());
		assertTrue(dc.contains("http://identifiers.org/go/GO:0044451"));
	}

	@Test
	public final void testGetDirectAllChildren() {
		Set<String> dc = cvRepository.getAllChildren("http://identifiers.org/obo.go/GO:0005654");
		assertFalse(dc.isEmpty());
		assertTrue(dc.contains("http://identifiers.org/go/GO:0044451"));
		assertTrue(dc.contains("http://identifiers.org/go/GO:0071821"));
		assertTrue(dc.contains("http://identifiers.org/go/GO:0070847"));
	}

	@Test
	public final void testGetDirectParents() {
		Set<String> dc = cvRepository.getDirectParents("urn:miriam:obo.go:GO%3A0005654");
		assertFalse(dc.isEmpty());
		assertTrue(dc.contains("http://identifiers.org/go/GO:0031981"));
	}

	@Test
	public final void testGetAllParents() {
		Set<String> dc = cvRepository.getAllParents("http://identifiers.org/obo.go/GO:0005654");
		assertFalse(dc.isEmpty());
		assertTrue(dc.contains("http://identifiers.org/go/GO:0031981"));
		assertTrue(dc.contains("http://identifiers.org/go/GO:0044428"));
		assertTrue(dc.contains("http://identifiers.org/go/GO:0044422"));
	}


	@Test // using correct ID(s)
	public final void testGetObject() {
		CellularLocationVocabulary cv = cvRepository.getControlledVocabulary(
				"urn:miriam:obo.go:GO%3A0005737",CellularLocationVocabulary.class);
		assertNotNull(cv);
		cv = null;
		cv = cvRepository.getControlledVocabulary( //using now deprecated URL
				"http://identifiers.org/obo.go/GO:0005737",CellularLocationVocabulary.class);
		assertNotNull(cv);
		//same
		cv = cvRepository.getControlledVocabulary(
				"http://identifiers.org/go/GO:0005737",CellularLocationVocabulary.class);
		assertNotNull(cv);
	}
	
	@Test // using bad ID (with 'X' in it)
	public final void testGetObject2() {
		CellularLocationVocabulary cv = cvRepository.getControlledVocabulary(
				"urn:miriam:obo.go:GO%3A0005737X",CellularLocationVocabulary.class);
		assertNull(cv);
	}

	@Test 
	public final void testEscapeChars() {
		ControlledVocabulary cv = cvRepository.getControlledVocabulary(
				"http://identifiers.org/obo.psi-mod/MOD:00048",SequenceModificationVocabulary.class);
		assertNotNull(cv);
		assertTrue(cv.getTerm().contains("O4'-phospho-L-tyrosine")); // apostrophe
	}
	
}
