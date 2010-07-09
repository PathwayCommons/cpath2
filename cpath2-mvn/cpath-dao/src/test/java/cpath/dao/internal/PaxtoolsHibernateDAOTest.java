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

// imports
import org.biopax.paxtools.controller.Completer;
import org.biopax.paxtools.io.simpleIO.SimpleEditorMap;
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
import java.io.StringWriter;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests org.mskcc.cpath2.dao.hibernatePaxtoolsHibernateDAO.
 */
public class PaxtoolsHibernateDAOTest {

    private static Log log = LogFactory.getLog(PaxtoolsHibernateDAOTest.class);

    static PaxtoolsDAO paxtoolsDAO;

	
    static {
    	DataServicesFactoryBean.createSchema("cpath2_test");
		// init the DAO (it loads now because databases are created above)
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"classpath:testContext-cpathDAO.xml");
		paxtoolsDAO = (PaxtoolsDAO) context.getBean("paxtoolsDAO");
		
		// load some data into the test storage
		log.info("Loading BioPAX data (importModel(file))...");
		File biopaxFile = new File(PaxtoolsHibernateDAOTest.class.getResource("/test.owl").getFile());
		
		//File biopaxFile = new File(getClass().getResource("/biopax-level3-test-normalized.owl").getFile());
		try {
			paxtoolsDAO.importModel(biopaxFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
    }
    
	
	@Test
	public void testSimple() throws Exception {
		log.info("Testing PaxtoolsDAO as Model.getByID(id)");
		BioPAXElement bpe = paxtoolsDAO
			.getByID("http://www.biopax.org/examples/myExample#Protein_A");
		assertTrue(bpe instanceof Protein);
	}

	
	//@Test // fails (lazy collections...) even though getObject uses Hibernate.initialize
	public void testAdvanced() throws Exception {
		// check inverse property (xrefOf) is set
		/* 
		 * getByID would return an object with lazy collections, 
		 * which is usable only within the session/transaction,
		 * which is closed after the call :)
		 * so we gonna use getObject instead - 
		 */
		//BioPAXElement bpe = paxtoolsDAO.getByID("urn:pathwaycommons:UnificationXref:UniProt_P46880");
		
		BioPAXElement bpe = ((WarehouseDAO)paxtoolsDAO).getObject(
				"urn:pathwaycommons:UnificationXref:UniProt_P46880", UnificationXref.class);
		assertTrue(bpe instanceof UnificationXref);
		Set<XReferrable> xrOfs = ((UnificationXref) bpe).getXrefOf();
		assertTrue(xrOfs.size()==1);
		ProteinReference pr = (ProteinReference) xrOfs.iterator().next();
		System.out.println(pr.toString());
	}
	
	//@Test // fails (lazy collections...)
	public void testAdvanced2() throws Exception {		
		// get a protein
		log.info("Testing PaxtoolsDAO as Model.getByID(id)");
		BioPAXElement bpe =  ((WarehouseDAO)paxtoolsDAO).getObject(
				"http://www.biopax.org/examples/myExample#Protein_A", Protein.class);
		
		// if the element can be exported like this, it's fully initialized...
		SimpleExporter exporter = new SimpleExporter(BioPAXLevel.L3);
		StringWriter writer = new StringWriter();
		exporter.writeObject(writer, bpe);
		System.out.println("Export single protein (partial BioPAX OWL):");
		System.out.println(writer.toString());
		
		
		// auto-complete - make model
		Model m = BioPAXLevel.L3.getDefaultFactory().createModel();
		Completer completer = new Completer(new SimpleEditorMap(BioPAXLevel.L3));
		Set<BioPAXElement> elements = completer.complete(Collections
				.singletonList(bpe), paxtoolsDAO);
		for (BioPAXElement el: elements) {
			m.add(el);
		}
		System.out.println("Auto-complete the protein and export model:");
		exporter.convertToOWL(m, System.out);
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
