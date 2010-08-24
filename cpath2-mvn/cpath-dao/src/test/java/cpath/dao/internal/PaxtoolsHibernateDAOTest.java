// $Id$
//------------------------------------------------------------------------------
/** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center.
 **
 ** This library is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** Memorial Sloan-Kettering Cancer Center
 ** has no obligations to provide maintenance, support,
 ** updates, enhancements or modifications.  In no event shall
 ** Memorial Sloan-Kettering Cancer Center
 ** be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** Memorial Sloan-Kettering Cancer Center
 ** has been advised of the possibility of such damage.  See
 ** the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this library; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 **/
package cpath.dao.internal;

import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.junit.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.apache.commons.logging.*;

import cpath.dao.PaxtoolsDAO;
import cpath.warehouse.WarehouseDAO;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests org.mskcc.cpath2.dao.hibernatePaxtoolsHibernateDAO.
 */
public class PaxtoolsHibernateDAOTest {

    static Log log = LogFactory.getLog(PaxtoolsHibernateDAOTest.class);
    static PaxtoolsDAO paxtoolsDAO;
    static SimpleExporter exporter;

	
    static {
    	DataServicesFactoryBean.createSchema("cpath2_test");
		// init the DAO (it loads now because databases are created above)
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"classpath:testContext-cpathDAO.xml");
		paxtoolsDAO = (PaxtoolsDAO) context.getBean("paxtoolsDAO");
		
		// load some data into the test storage
		log.info("Loading BioPAX data (importModel(file))...");
		File biopaxFile = new File(PaxtoolsHibernateDAOTest.class.getResource("/test.owl").getFile());
		try {
			paxtoolsDAO.importModel(biopaxFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		exporter = new SimpleExporter(BioPAXLevel.L3);
    }
    
	
    @Test
	public void testImportingAnotherFileAndTestInitialization() throws IOException {
    	/* first, trying to import another "pathway" 
    	 * and ensure this does not fail
    	 * (I suspected a "duplicate entry for the key (rdfid)" exception)
    	 */
		File biopaxFile = new File(PaxtoolsHibernateDAOTest.class.getResource(
				"/test2.owl").getFile());
		paxtoolsDAO.importModel(biopaxFile);
		// a few smoke checks
		assertTrue(paxtoolsDAO.containsID("urn:miriam:uniprot:P46880"));
		assertTrue(paxtoolsDAO.containsID("http://www.biopax.org/examples/myExample2#Protein_A"));
		assertTrue(paxtoolsDAO.containsID("http://www.biopax.org/examples/myExample#Protein_A"));
		assertTrue(paxtoolsDAO.containsID("http://www.biopax.org/examples/myExample#Protein_B"));
		assertTrue(paxtoolsDAO.containsID("urn:pathwaycommons:UnificationXref:Taxonomy_562"));
		
		BioPAXElement bpe = ((WarehouseDAO) paxtoolsDAO).getObject(
				"urn:pathwaycommons:UnificationXref:Taxonomy_562", UnificationXref.class);
		assertTrue(bpe instanceof UnificationXref);
		
		BioPAXElement e = paxtoolsDAO
				.getByID("http://www.biopax.org/examples/myExample2#Protein_A");
		assertTrue(e instanceof Protein);
		
		
		e = paxtoolsDAO // try to initialize
		.getByIdInitialized("http://www.biopax.org/examples/myExample2#Protein_A");
		Protein p = (Protein) e;
				
		assertTrue(p.getEntityReference() != null);
		assertEquals("urn:miriam:uniprot:P46880", p.getEntityReference().getRDFId());
		
		// this would fail (lazy collections)
		//assertEquals(4, p.getEntityReference().getEntityReferenceOf().size());
			
		// but when -
		e = paxtoolsDAO // try to initialize
		.getByIdInitialized("urn:miriam:uniprot:P46880");
		assertTrue(e instanceof ProteinReference);
		ProteinReference pr = (ProteinReference) e;
		assertNotNull(pr.getOrganism());
		// however, with using getByIdInitialized, next line would fail -
		// pr.getEntityReferenceOf().size();
		// assertEquals(4, pr.getEntityReferenceOf().size());
		
		// different approach works!
		pr = (ProteinReference) paxtoolsDAO.getByID("urn:miriam:uniprot:P46880");
		//pr.getEntityReferenceOf().size() would fail here, but...
		// initialize(bpe) can be called at any time (it's bidirectional, though not recursive)
		paxtoolsDAO.initialize(pr);
		// should pass now :)
		assertEquals(4, pr.getEntityReferenceOf().size());
		assertEquals(2, pr.getName().size());
		//pr.getOrganism().getXref().size(); // would fail, hehe... but
		BioSource bs = pr.getOrganism();
		assertNotNull(bs);
		paxtoolsDAO.initialize(bs);
		assertTrue(bs.getXref().size() > 0);
	}
    
    
	@Test
	public void testSimple() throws Exception {
		log.info("Testing PaxtoolsDAO as Model.getByID(id)");
		BioPAXElement bpe = paxtoolsDAO
			.getByID("http://www.biopax.org/examples/myExample#Protein_A");
		assertTrue(bpe instanceof Protein);
		
		bpe = ((WarehouseDAO)paxtoolsDAO).getObject(
				"urn:pathwaycommons:UnificationXref:UniProt_P46880", UnificationXref.class);
		assertTrue(bpe instanceof UnificationXref);
	}

	
	@Test // protein reference's xref's getXrefOf() is not empty
	public void testGetObjectXReferableAndXrefOf() throws Exception {
		ProteinReference pr = ((WarehouseDAO)paxtoolsDAO).getObject(
				"urn:miriam:uniprot:P46880", ProteinReference.class);
		assertTrue(pr instanceof ProteinReference);
		assertFalse(pr.getXref().isEmpty());
		Xref x = pr.getXref().iterator().next();		
		Set<XReferrable> xrOfs = x.getXrefOf();
		assertEquals(1, xrOfs.size());
		System.out.println(x.getRDFId() + " is xrefOf " + 
				x.getXrefOf().iterator().next().toString()
		);
	}
	
	
	
	@Test // getXrefOf() returns empty set, but it's not a bug!
	public void testGetObjectXrefAndXrefOf() throws Exception {
		/* 
		 * getByID would return an object with lazy collections, 
		 * which is usable only within the session/transaction,
		 * which is closed after the call :) So we use getObject instead - 
		 */
		//BioPAXElement bpe = paxtoolsDAO.getByID("urn:pathwaycommons:UnificationXref:UniProt_P46880");
		
		BioPAXElement bpe = ((WarehouseDAO)paxtoolsDAO).getObject(
				"urn:pathwaycommons:UnificationXref:UniProt_P46880", UnificationXref.class);
		assertTrue(bpe instanceof UnificationXref);
		
		// if the element can be exported like this, it's fully initialized...
		StringWriter writer = new StringWriter();
		exporter.writeObject(writer, bpe);
		System.out.println("Export single Xref (incomplete BioPAX):");
		System.out.println(writer.toString());
		
		// check if it has xrefOf values...
		Set<XReferrable> xrOfs = ((UnificationXref) bpe).getXrefOf();
		assertTrue(xrOfs.isEmpty()); // EMPTY when the xref is returned by getObject!
	}
	
	
	@Test
	public void testGetObject() throws Exception {		
		// get a protein
		log.info("Testing PaxtoolsDAO as Model.getByID(id)");
		BioPAXElement bpe =  ((WarehouseDAO)paxtoolsDAO).getObject(
				"http://www.biopax.org/examples/myExample#Protein_A", Protein.class);
		
		assertTrue(bpe instanceof Protein);
		assertEquals("glucokinase A", ((Protein)bpe).getDisplayName());
		assertNotNull(((Protein)bpe).getEntityReference());
		assertEquals(1, ((Protein)bpe).getEntityReference().getXref().size());
		
		// if the element can be exported like this, it's fully initialized...
		StringWriter writer = new StringWriter();
		exporter.writeObject(writer, bpe);
		System.out.println("Export single protein (incomplete BioPAX):");
		System.out.println(writer.toString());
	}
	
	
	@Test
	public void testGetValidSubModel() throws Exception {	
		Model m =  paxtoolsDAO.getValidSubModel(
			Collections.singleton("http://www.biopax.org/examples/myExample#Protein_A"));
		System.out.println("Clone the protein and export model:");
		assertTrue(m.containsID("http://www.biopax.org/examples/myExample#Protein_A"));
		assertTrue(m.containsID("urn:miriam:uniprot:P46880"));
		assertTrue(m.containsID("urn:pathwaycommons:UnificationXref:UniProt_P46880"));
		
		OutputStream out = new FileOutputStream(
				getClass().getClassLoader().getResource("").getPath() 
					+ File.separator + "testGetValidSubModel.out.owl");
		exporter.convertToOWL(m, out);
	}
	
	
	@Test
	public void testSerchForObjects() throws Exception {
		List<? extends BioPAXElement> elist = paxtoolsDAO.search("P46880", BioPAXElement.class);
		assertFalse(elist.isEmpty());
		System.out.println(elist.toString());
	}

	@Test
	public void testSerchForIDs() throws Exception {
		List<String> elist = paxtoolsDAO.find("P46880", UnificationXref.class);
		assertFalse(elist.isEmpty());
		assertTrue(elist.size()==1);
		System.out.println(elist.toString());
	}
	
	@Test
	public void testCount() throws Exception {
		Integer n = paxtoolsDAO.count("P46880", UnificationXref.class);
		assertEquals(1, n.intValue());
		
		n = paxtoolsDAO.count("P46880", BioPAXElement.class);
		assertEquals(2, n.intValue());
		
		n = paxtoolsDAO.count(null, BioPAXElement.class);
		assertEquals(9, n.intValue());
		
		n = paxtoolsDAO.count(null, UnificationXref.class);
		assertEquals(3, n.intValue());
	}

	
	@Test
	public void testFind() throws Exception {
		List<String> list = paxtoolsDAO.find("P46880", BioPAXElement.class);
		assertFalse(list.isEmpty());
		assertTrue(list.contains("urn:pathwaycommons:UnificationXref:UniProt_P46880"));
		System.out.println("find by 'P46880' returned: " + list.toString());
		
		list = paxtoolsDAO.find("P46880", ProteinReference.class);
		assertTrue(list.size()==1);
		assertTrue(list.contains("urn:miriam:uniprot:P46880"));
	}
	
	
	//@Test // takes forever...
	public void testIndex() {
		paxtoolsDAO.createIndex();
	}

}
