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
import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.apache.commons.logging.*;

import cpath.dao.CPathService;
import cpath.dao.PaxtoolsDAO;
import cpath.dao.CPathService.OutputFormat;
import cpath.dao.CPathService.ResultMapKey;
import cpath.warehouse.WarehouseDAO;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests org.mskcc.cpath2.dao.hibernatePaxtoolsHibernateDAO.
 */
public class CPathServiceTest {

    private static Log log = LogFactory.getLog(CPathServiceTest.class);

    static CPathService service;
    static SimpleExporter exporter;

	
    static {
    	DataServicesFactoryBean.createSchema("cpath2_test");
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"classpath:testContext-cpathDAO.xml");
		Object dao = context.getBean("paxtoolsDAO");
		log.info("Loading BioPAX data (importModel(file))...");
		File biopaxFile = new File(CPathServiceTest.class.getResource("/test.owl").getFile());		
		//File biopaxFile = new File(getClass().getResource("/biopax-level3-test-normalized.owl").getFile());
		try {
			((PaxtoolsDAO)dao).importModel(biopaxFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		service = new CPathServiceImpl((PaxtoolsDAO)dao, (WarehouseDAO)dao);
		exporter = new SimpleExporter(BioPAXLevel.L3);
    }
	
	
	@Test
	public void testServiceElement() throws Exception {
		Map<ResultMapKey, Object> map = service.element(
				"http://www.biopax.org/examples/myExample#Protein_A",
				OutputFormat.BIOPAX);
		assertNotNull(map);
		
		assertNotNull(map.get(ResultMapKey.DATA));
		assertNotNull(map.get(ResultMapKey.ELEMENT));
		assertNotNull(map.get(ResultMapKey.MODEL));
		
		Model m = (Model) map.get(ResultMapKey.MODEL);
		assertNotNull(m);
		BioPAXElement e = m.getByID("http://www.biopax.org/examples/myExample#Protein_A");
		assertTrue(e instanceof Protein);
		
		e = null;
		e = (BioPAXElement) map.get(ResultMapKey.ELEMENT);
		assertTrue(e instanceof Protein);
	}

	
	@Test
	public void testServiceElement2() throws Exception {
		Map<ResultMapKey, Object> map = service.element(
				"urn:miriam:uniprot:P46880", OutputFormat.BIOPAX);
		assertNotNull(map);
		
		assertEquals(1, map.get(ResultMapKey.COUNT));
		
		BioPAXElement e = (BioPAXElement) map.get(ResultMapKey.ELEMENT);
		assertTrue(e instanceof ProteinReference);
		
		System.out.println(map.get(ResultMapKey.DATA));
	}
}
