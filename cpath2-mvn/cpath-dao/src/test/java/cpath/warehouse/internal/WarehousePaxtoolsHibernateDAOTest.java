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
package cpath.warehouse.internal;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.level3.*;
import org.junit.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.apache.commons.logging.*;

import cpath.dao.PaxtoolsDAO;
import cpath.dao.internal.DataServicesFactoryBean;
import cpath.dao.internal.PaxtoolsHibernateDAOTest;
import cpath.warehouse.WarehouseDAO;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests use the same (cpath2_testpc) db and data 
 * using another PaxtoolsDAO implementation for the Warehouse
 * (which has additional methods and slightly different full-text search implementation).
 * 
 * @see PaxtoolsHibernateDAOTest
 */
public class WarehousePaxtoolsHibernateDAOTest {

    static Log log = LogFactory.getLog(WarehousePaxtoolsHibernateDAOTest.class);
    static WarehouseDAO whpcDAO;
    static SimpleIOHandler exporter;

	/* test methods will use the same data (read-only, 
	 * with one exception: testImportingAnotherFileAndTestInitialization
	 * imports the same data again...)
	 */
    static {
    	DataServicesFactoryBean.createSchema("cpath2_testpc");
		// init the DAO (it loads now because databases are created above)
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"classpath:testContext-whpcDAO.xml");
		whpcDAO = (WarehouseDAO) context.getBean("whpcDAO");
		
		// load some data into the test storage
		log.info("Loading BioPAX data (importModel(file))...");
		try {
	    	/* import two files to ensure it does not fail
	    	 * (suspecting a "duplicate entry for the key (rdfid)" exception) */
			((PaxtoolsDAO)whpcDAO).importModel(new File(WarehousePaxtoolsHibernateDAOTest.class.getResource("/test.owl").getFile()));
			((PaxtoolsDAO)whpcDAO).importModel(new File(WarehousePaxtoolsHibernateDAOTest.class.getResource("/test2.owl").getFile()));
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Failed to read test data!", e);
		}
		
		exporter = new SimpleIOHandler(BioPAXLevel.L3);
    }
    
    public void testIsWhDAOInstance() {
    	assertTrue(whpcDAO instanceof PaxtoolsDAO);
    	assertTrue(whpcDAO instanceof WarehouseDAO);
    }
    
	
    @Test
	public void testImportingAnotherFile() throws IOException {
		assertTrue(((PaxtoolsDAO)whpcDAO).containsID("urn:miriam:uniprot:P46880"));
		assertTrue(((PaxtoolsDAO)whpcDAO).containsID("http://www.biopax.org/examples/myExample2#Protein_A"));
		assertTrue(((PaxtoolsDAO)whpcDAO).containsID("http://www.biopax.org/examples/myExample#Protein_A"));
		assertTrue(((PaxtoolsDAO)whpcDAO).containsID("http://www.biopax.org/examples/myExample#Protein_B"));
		assertTrue(((PaxtoolsDAO)whpcDAO).containsID("urn:biopax:UnificationXref:Taxonomy_562"));
		
		BioPAXElement bpe = whpcDAO.getObject("urn:biopax:UnificationXref:Taxonomy_562", UnificationXref.class);
		assertTrue(bpe instanceof UnificationXref);
		
		BioPAXElement e = whpcDAO
				.getObject("http://www.biopax.org/examples/myExample2#Protein_A");
		assertTrue(e instanceof Protein);
		
		e = whpcDAO 
		.getObject("http://www.biopax.org/examples/myExample2#Protein_A");
		Protein p = (Protein) e;
				
		assertTrue(p.getEntityReference() != null);
		assertEquals("urn:miriam:uniprot:P46880", p.getEntityReference().getRDFId());
		
		e = whpcDAO.getObject("urn:miriam:uniprot:P46880");
		assertTrue(e instanceof ProteinReference);
		ProteinReference pr = (ProteinReference) e;
		assertNotNull(pr.getOrganism());
		
		// WarehouseDAO.getObject cannot get inverse props for the object itself!
		assertTrue(pr.getEntityReferenceOf().isEmpty()); 
		assertEquals(2, pr.getName().size());

		BioSource bs = pr.getOrganism();
		assertNotNull(bs);
		assertTrue(bs.getXref().size() > 0);
	}
    
    
	@Test
	public void testSimple() throws Exception {
		log.info("Testing PaxtoolsDAO as Model.getByID(id)");
		BioPAXElement bpe = whpcDAO.getObject("http://www.biopax.org/examples/myExample#Protein_A");
		assertTrue(bpe instanceof Protein);
		
		bpe = ((WarehouseDAO)whpcDAO)
			.getObject("urn:biopax:UnificationXref:UniProt_P46880", UnificationXref.class);
		assertTrue(bpe instanceof UnificationXref);
	}

	
	@Test // protein reference's xref's getXrefOf() is not empty
	public void testGetObjectXReferableAndXrefOf() throws Exception {
		ProteinReference pr = whpcDAO.getObject(
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
		BioPAXElement bpe = whpcDAO
			.getObject("urn:biopax:UnificationXref:UniProt_P46880", UnificationXref.class);
		assertTrue(bpe instanceof UnificationXref);
		
		// if the element can be exported like this, it's fully initialized...
		StringWriter writer = new StringWriter();
		exporter.writeObject(writer, bpe);
		//System.out.println("Export single Xref (incomplete BioPAX):");
		//System.out.println(writer.toString());
		
		// check if it has xrefOf values...
		Set<XReferrable> xrOfs = ((UnificationXref) bpe).getXrefOf();
		assertTrue(xrOfs.isEmpty()); // EMPTY when the xref is returned by getObject!
	}
	
	
	@Test
	public void testGetObject() throws Exception {		
		// get a protein
		log.info("Testing WarehouseDAO.getObject(id, clazz)");
		BioPAXElement bpe =  whpcDAO.getObject(
				"http://www.biopax.org/examples/myExample#Protein_A", Protein.class);
		
		assertTrue(bpe instanceof Protein);
		assertEquals("glucokinase A", ((Protein)bpe).getDisplayName());
		assertNotNull(((Protein)bpe).getEntityReference());
		assertEquals(1, ((Protein)bpe).getEntityReference().getXref().size());
		
		// if the element can be exported like this, it's fully initialized...
		StringWriter writer = new StringWriter();
		exporter.writeObject(writer, bpe);
		//System.out.println("Export single protein (incomplete BioPAX):");
		//System.out.println(writer.toString());
	}

}
