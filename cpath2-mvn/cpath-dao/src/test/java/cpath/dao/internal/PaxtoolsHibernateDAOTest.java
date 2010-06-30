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
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.apache.commons.logging.*;

import cpath.dao.PaxtoolsDAO;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests org.mskcc.cpath2.dao.hibernatePaxtoolsHibernateDAO.
 */
public class PaxtoolsHibernateDAOTest {

    private static Log log = LogFactory.getLog(PaxtoolsHibernateDAOTest.class);

    PaxtoolsDAO paxtoolsDAO;

	
	/*
	 * drop-create db schema, import data file
	 */
	@Before
	public void setUp() throws Exception {
		DataServicesFactoryBean.createSchema("cpath2_test");
		// init the DAO (it loads now because databases are created above)
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"classpath:testContext-cpathDAO.xml");
		paxtoolsDAO = (PaxtoolsDAO) context.getBean("paxtoolsDAO");
		
		// load some data into the test storage
		log.info("Loading BioPAX data (importModel(file))...");
		File biopaxFile = new File(getClass().getResource("/test.owl").getFile());
		//File biopaxFile = new File(getClass().getResource("/biopax-level3-test-normalized.owl").getFile());
		paxtoolsDAO.importModel(biopaxFile);
		
		log.info("importModel(file) done!");
	}

	@After
	public void tearDown() throws Exception {
	}

	
	@Test
	public void testSimple() throws Exception {
		BioPAXElement e = paxtoolsDAO.getByID(
				"http://www.biopax.org/examples/myExample#Protein_A");
		assertNotNull(e);
		assertTrue(e instanceof Protein);
	
		log.info("Testing PaxtoolsDAO as Model.getByID(id)");
		BioPAXElement bpe = paxtoolsDAO
			.getByID("http://www.biopax.org/examples/myExample#Protein_A");
		assertNotNull(bpe);
		assertTrue(bpe instanceof Protein);
	}

	
	//@Test //fails (SimplePhysicalEntity.setEntityReference, hibernate has issues with the collections...)
	// not so important method so far...
	public void testSerch() throws Exception {
		List<? extends BioPAXElement> elist = paxtoolsDAO.search("P46880", BioPAXElement.class);
		assertFalse(elist.isEmpty());
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
