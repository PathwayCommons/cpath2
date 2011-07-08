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

import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.Xref;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cpath.dao.PaxtoolsDAO;
import cpath.service.jaxb.SearchHitType;
import cpath.service.jaxb.SearchResponseType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:testContext-pcDAO.xml")
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class PaxtoolsHibernateDAOMoreTest {

    @javax.annotation.Resource(name="pcDAO")
	PaxtoolsDAO paxtoolsDAO;
    static SimpleIOHandler io = new SimpleIOHandler(BioPAXLevel.L3);

    @Test
	public void testImportExportRead() throws IOException {
    	// import (not so good) pathway data
		Resource input = (new DefaultResourceLoader()).getResource("classpath:biopax-level3-test.owl");
		paxtoolsDAO.importModel(input.getFile());
		assertTrue(paxtoolsDAO.containsID("http://www.biopax.org/examples/myExample#Stoichiometry_58"));
		assertEquals(55, paxtoolsDAO.getObjects().size()); 
		// there was a bug in paxtools (due to Stoichiometry.hashCode() override)!
		
		// export from the DAO to OWL
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		paxtoolsDAO.exportModel(outputStream);
		String exported = outputStream.toString();
		//System.out.println("\n\n*********\n\n" + exported);
		// read it back
		io.mergeDuplicates(true);
		Model model = io.convertFromOWL(new ByteArrayInputStream(exported.getBytes("UTF-8")));
		assertNotNull(model);
		assertTrue(model.containsID("http://www.biopax.org/examples/myExample#Stoichiometry_58"));
		assertEquals(55, model.getObjects().size());
	}
    
    @Test
	public void testSearch() throws IOException {
    	// import (not so good) pathway data
		Resource input = (new DefaultResourceLoader()).getResource("classpath:xrefs.owl");
		paxtoolsDAO.importModel(input.getFile());
		
		assertEquals(3, paxtoolsDAO.getObjects(Xref.class).size());
		DataServicesFactoryBean.rebuildIndex("cpath2_testpc");
		
		SearchResponseType resp = paxtoolsDAO.findElements("P46880", 0, UnificationXref.class);
		assertFalse(resp.getSearchHit().isEmpty());
		assertEquals(1, resp.getSearchHit().size());
	}
}
