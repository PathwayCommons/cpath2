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
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.junit.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import org.apache.commons.logging.*;

import cpath.dao.PaxtoolsDAO;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;

public class PaxtoolsHibernateDAOMoreTest {

    static Log log = LogFactory.getLog(PaxtoolsHibernateDAOMoreTest.class);
    static PaxtoolsDAO paxtoolsDAO;
    static SimpleExporter exporter = new SimpleExporter(BioPAXLevel.L3);
    static SimpleReader reader = new SimpleReader();
    

	/* test methods will use the same data (read-only, 
	 * with one exception: testImportingAnotherFileAndTestInitialization
	 * imports the same data again...)
	 */
    @Before
    public void setUp() {
    	DataServicesFactoryBean.createSchema("cpath2_testpc");
		// init the DAO (it loads now because databases are created above)
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"classpath:testContext-pcDAO.xml");
		paxtoolsDAO = (PaxtoolsDAO) context.getBean("pcDAO");
    }
    
	
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
		
		// read it back
		reader.mergeDuplicates(true);
		Model model = reader.convertFromOWL(new ByteArrayInputStream(exported
				.getBytes("UTF-8")));
		assertNotNull(model);
		assertTrue(model
			.containsID("http://www.biopax.org/examples/myExample#Stoichiometry_58"));
		assertEquals(55, model.getObjects().size());
	}
    
    
	/* it should normally work.., but sometimes "hangs"
	* the entire maven build process if the lucene index directory
	* already exists (and, seems, when the index has been touched
	* by another app./process, e.g., by cpath-admin '-create-index' command)
	* In such an event, simply deleting the index dir makes this test pass.
	* So, let's keep it disabled for now...
	*/
	//@Test
	public void testIndex() {
		paxtoolsDAO.createIndex();
	}
}
