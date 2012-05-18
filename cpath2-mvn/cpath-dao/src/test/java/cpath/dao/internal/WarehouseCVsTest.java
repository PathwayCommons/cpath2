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

package cpath.dao.internal;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.biopax.paxtools.model.level3.*;
import org.junit.Ignore;
import org.junit.Test;

import cpath.dao.internal.OntologyManagerCvRepository;

import psidev.ontology_manager.OntologyTermI;
import psidev.ontology_manager.impl.OntologyTermImpl;

/**
 * This tests are for CVs only (not using DAO);
 * other tests are in the cpath-importer module.
 * 
 * @author rodche
 *
 */
//@Ignore
public class WarehouseCVsTest {

	static OntologyManagerCvRepository warehouse; // implements cpath.dao.WarehouseDAO	
	
	static {
		final Properties cfg = new Properties();
		cfg.put("SO", "http://song.cvs.sourceforge.net/viewvc/song/ontology/so.obo"); //?revision=1.283
		cfg.put("MI", "http://psidev.cvs.sourceforge.net/viewvc/psidev/psi/mi/rel25/data/psi-mi25.obo?revision=1.58");
		cfg.put("MOD", "http://psidev.cvs.sourceforge.net/viewvc/psidev/psi/mod/data/PSI-MOD.obo?revision=1.23");
//		cfg.put("GO", "http://obo.cvs.sourceforge.net/*checkout*/obo/obo/ontology/genomic-proteomic/gene_ontology_edit.obo");
		cfg.put("GO", "http://www.geneontology.org/ontology/gene_ontology_edit.obo");
		warehouse = new OntologyManagerCvRepository(cfg, null, true);
	}
	
	@Test
	public void ontologyLoading() {
		Collection<String> ontologyIDs = warehouse.getOntologyIDs();
		assertTrue(ontologyIDs.contains("GO"));
		assertEquals("gene ontology", warehouse.getOntology("GO").getName().toLowerCase());
		assertTrue(ontologyIDs.contains("SO"));
		assertTrue(ontologyIDs.contains("MI"));
		assertTrue(ontologyIDs.contains("MOD"));
	}

	/**
	 * Test method for
	 * {@link cpath.dao.internal.OntologyManagerCvRepository#ontologyTermsToUrns(java.util.Collection)}
	 */
	@Test
	public final void testOntologyTermsToUrns() {
		OntologyTermI term = new OntologyTermImpl("GO", "GO:0005654",
				"nucleoplasm");
		Set<OntologyTermI> terms = new HashSet<OntologyTermI>();
		terms.add(term);
		Set<String> urns = warehouse.ontologyTermsToUrns(terms);
		assertFalse(urns.isEmpty());
		assertTrue(urns.size() == 1);
		String urn = urns.iterator().next();
		assertEquals("urn:miriam:obo.go:GO%3A0005654", urn);
	}

	/**
	 * Test method for
	 * {@link cpath.dao.internal.OntologyManagerCvRepository#ontologyTermsToUrns(java.util.Collection)}
	 */
	@Test
	public final void testSearchForTermByAccession() {
		OntologyTermI term = warehouse.findTermByAccession("GO:0005654");
		assertNotNull(term);
		assertEquals("nucleoplasm", term.getPreferredName());
	}

	@Test
	public final void testSearchMODForTermByAccession() {
		OntologyTermI term = warehouse.findTermByAccession("MOD:00046");
		assertNotNull(term);
		assertEquals("MOD", term.getOntologyId());
	}
	
	@Test
	public final void testGetDirectChildren() {
		Set<String> dc = warehouse.getDirectChildren("urn:miriam:obo.go:GO%3A0005654");
		assertFalse(dc.isEmpty());
		assertTrue(dc.contains("urn:miriam:obo.go:GO%3A0044451"));
		System.out.println("DirectChildren:\n" + dc.toString() + " " +
			warehouse.createBiopaxObject("urn:miriam:obo.go:GO%3A0044451", ControlledVocabulary.class));
	}

	@Test
	public final void testGetDirectAllChildren() {
		Set<String> dc = warehouse.getAllChildren("urn:miriam:obo.go:GO%3A0005654");
		assertFalse(dc.isEmpty());
		assertTrue(dc.contains("urn:miriam:obo.go:GO%3A0044451"));
		assertTrue(dc.contains("urn:miriam:obo.go:GO%3A0042555"));
		assertTrue(dc.contains("urn:miriam:obo.go:GO%3A0070847"));
		//System.out.println("AllChildren:\n" +dc.toString() + "; e.g., " +
		//		warehouse.getObject("urn:miriam:obo.go:GO%3A0042555", ControlledVocabulary.class));
	}

	@Test
	public final void testGetDirectParents() {
		Set<String> dc = warehouse.getDirectParents("urn:miriam:obo.go:GO%3A0005654");
		assertFalse(dc.isEmpty());
		assertTrue(dc.contains("urn:miriam:obo.go:GO%3A0031981"));
		System.out.println("DirectParents:\n" +dc.toString() + "; e.g., " +
			warehouse.createBiopaxObject("urn:miriam:obo.go:GO%3A0031981", ControlledVocabulary.class));
	}

	@Test
	public final void testGetAllParents() {
		Set<String> dc = warehouse.getAllParents("urn:miriam:obo.go:GO%3A0005654");
		assertFalse(dc.isEmpty());
		assertTrue(dc.contains("urn:miriam:obo.go:GO%3A0031981"));
		assertTrue(dc.contains("urn:miriam:obo.go:GO%3A0044428"));
		assertTrue(dc.contains("urn:miriam:obo.go:GO%3A0044422"));
		//System.out.println("AllParents:\n" +dc.toString());
	}


	@Test // using correct ID
	public final void testGetObject() {
		CellularLocationVocabulary cv = warehouse.createBiopaxObject(
				"urn:miriam:obo.go:GO%3A0005737",CellularLocationVocabulary.class);
		assertNotNull(cv);
	}
	
	@Test // using bad ID
	public final void testGetObject2() {
		CellularLocationVocabulary cv = warehouse.createBiopaxObject(
				"urn:miriam:obo.go:GO%3A0005737X",CellularLocationVocabulary.class);
		assertNull(cv);
	}

	@Test 
	public final void testEscapeChars() {
		ControlledVocabulary cv = warehouse.createBiopaxObject(
				"urn:miriam:obo.psi-mod:MOD%3A00048",SequenceModificationVocabulary.class);
		assertNotNull(cv);
		
//		System.out.println("MOD%253A00048 term:" + cv.getTerm().toString());
		
		assertTrue(cv.getTerm().contains("O4'-phospho-L-tyrosine")); // apostrophe
	}
	
}
