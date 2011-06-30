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

package cpath.warehouse.internal;


import org.junit.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cpath.dao.internal.DataServicesFactoryBean;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.PathwayData;

import static org.junit.Assert.*;

/**
 * @author rodche
 *
 */
//@Ignore
public class MetadataHibernateDAOTest {

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		DataServicesFactoryBean.createSchema("cpath2_test");
	}

	@Test
	public void testImportPathwayData() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"classpath:testContext-whDAO.xml");
        MetadataDAO dao = (MetadataDAO) context.getBean("metadataDAO");
		
		// mock a PathwayData
        PathwayData pathwayData = new PathwayData("testpw", "2010.04", "testpw", "testpw", "<rdf> </rdf>");
        
        // import it
        dao.importPathwayData(pathwayData);
        
        // get
        pathwayData = null;
        pathwayData = dao.getPathwayDataByIdentifierAndVersionAndFilenameAndDigest("testpw", "2010.04", "testpw", "testpw");
        assertNotNull(pathwayData);
        assertTrue(pathwayData.getValidationResults().length() == 0);
        
        // add premerge data and validation result
        pathwayData.setPremergeData("<rdf></rdf>");
        pathwayData.setValidationResults("<?xml version=\"1.0\"?>");
        
        // update
        dao.importPathwayData(pathwayData);
        
        // check
        pathwayData = null;
        pathwayData = dao.getPathwayDataByIdentifierAndVersionAndFilenameAndDigest("testpw", "2010.04", "testpw", "testpw");
        assertNotNull(pathwayData);
        assertTrue(pathwayData.getValidationResults().length() > 0);
	}
	
}
