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


import java.util.Arrays;
import java.util.Collection;

import org.junit.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cpath.dao.internal.DataServicesFactoryBean;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Metadata.METADATA_TYPE;
import cpath.warehouse.beans.PathwayData;

import static org.junit.Assert.*;

/**
 * @author rodche
 *
 */
public class MetadataHibernateDAOTest {

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		DataServicesFactoryBean.createSchema("test_cpath2main");
	}

	@Test
	public void testImportPathwayData() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"classpath:testContext-whDAO.xml");
        MetadataDAO dao = (MetadataDAO) context.getBean("metadataDAO");
        		
        // mock metadata and pathway data
        Metadata md = new Metadata("testpw", "test", "test", "", "",
        		new byte[]{}, METADATA_TYPE.BIOPAX, null, null);        
        byte[] testData = "<rdf>          </rdf>".getBytes();       
        PathwayData pathwayData = new PathwayData(md, "test.gz",  testData);
        md.addPathwayData(pathwayData);
        
        // test if internal pack/unpach, if any, works well
        assertTrue(Arrays.equals(testData, pathwayData.getPathwayData()));
        // persist
        dao.saveMetadata(md); //this md is still detached
        
        //read back the initialized persistent detached object
        md = dao.getMetadataByIdentifier(md.getIdentifier());
        assertNotNull(md);
        assertEquals("testpw", md.getIdentifier());
      
        // get pathwaydata directly (by PK)
        pathwayData = dao.getPathwayData(1);
        assertNotNull(pathwayData);
        assertNull(pathwayData.getValidationResults());
        // check whether DB save/read changed data
        assertTrue(Arrays.equals(testData, pathwayData.getPathwayData()));
        
        // add premerge data and validation result
        pathwayData.setPremergeData("<rdf></rdf>".getBytes());
        pathwayData.setValidationResults("<?xml version=\"1.0\"?>".getBytes());        

        // update
        dao.savePathwayData(pathwayData);
                
        md = dao.getMetadata(1);
        assertNotNull(md);
        Collection<PathwayData>  pd = md.getPathwayData();
        assertFalse(pd.isEmpty());
        pd = dao.getPathwayDataByIdentifier(md.getIdentifier());
        assertFalse(pd.isEmpty());
        pathwayData = pd.iterator().next();      
        assertNotNull(pathwayData);
        assertNotNull(pathwayData.getValidationResults());
        assertNotNull(pathwayData.getPremergeData());
        assertTrue(pathwayData.getValidationResults().length > 0);  
        assertTrue(pathwayData.getPremergeData().length > 0);  
        
        // check      
        pd = dao.getPathwayDataByIdentifier(md.getIdentifier());
        assertFalse(pd.isEmpty());
        pathwayData = pd.iterator().next();      
        assertNotNull(pathwayData);
        assertNotNull(pathwayData.getValidationResults());
        assertTrue(pathwayData.getValidationResults().length > 0);       

	}
	
}
