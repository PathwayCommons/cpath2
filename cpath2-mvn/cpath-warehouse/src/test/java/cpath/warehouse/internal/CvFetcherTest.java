package cpath.warehouse.internal;
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

import java.util.*;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cpath.warehouse.internal.OntologyManagerCvRepository;

import psidev.ontology_manager.impl.OntologyTermImpl;
import psidev.ontology_manager.impl.OntologyImpl;
import psidev.ontology_manager.Ontology;
import psidev.ontology_manager.OntologyTermI;

/**
 * @author rodche
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"classpath:applicationContext-cvRepository.xml"})
public class CvFetcherTest {

	@Autowired
	private OntologyManagerCvRepository cvFetcher;
	
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	
    @Test
    public void ontologyLoading() {
        Collection<String> ontologyIDs = cvFetcher.getOntologyIDs();
        Assert.assertTrue( ontologyIDs.contains( "GO" ) );
        Assert.assertTrue( ontologyIDs.contains( "SO" ) );
        Assert.assertTrue( ontologyIDs.contains( "MI" ) );

        Ontology oa2 = cvFetcher.getOntology( "GO" );
        Assert.assertNotNull( oa2 );
        Assert.assertTrue( oa2 instanceof OntologyImpl);
        
        oa2 = cvFetcher.getOntology( "SO" );
        Assert.assertNotNull( oa2 );
        Assert.assertTrue( oa2 instanceof OntologyImpl);
        
        oa2 = cvFetcher.getOntology( "MI" );
        Assert.assertNotNull( oa2 );
        Assert.assertTrue( oa2 instanceof OntologyImpl);
    }
	
	
	/**
	 * Test method for {@link cpath.warehouse.internal.OntologyManagerCvRepository#ontologyTermsToUrns(java.util.Collection)}.
	 */
	@Test
	public final void testOntologyTermsToUrns() {
		OntologyTermI term = new OntologyTermImpl("GO", "GO:0005654", "nucleoplasm");
		Set<OntologyTermI> terms = new HashSet<OntologyTermI>();
		terms.add(term);
		Set<String> urns = cvFetcher.ontologyTermsToUrns(terms);
		assertFalse(urns.isEmpty());
		assertTrue(urns.size()==1);
		String urn = urns.iterator().next();
		assertEquals("urn:miriam:obo.go:GO%3A0005654", urn);
	}

	
	/**
	 * Test method for {@link cpath.warehouse.internal.OntologyManagerCvRepository#ontologyTermsToUrns(java.util.Collection)}.
	 */
	@Test
	public final void testSearchForTermByAccession() {
		OntologyTermI term = cvFetcher.findTermByAccession("GO:0005654");
		assertNotNull(term);
		assertEquals("nucleoplasm", term.getPreferredName());
	}
	
	@Test
	public final void testGetDirectChildren() {
		
	}
	
	@Test
	public final void testGetDirectAllChildren() {
		
	}
	
	@Test
	public final void testGetDirectParents() {
		
	}
	
	@Test
	public final void testGetAllParents() {
		
	}
}
