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

import java.io.IOException;
import java.util.*;

import org.junit.*;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cpath.config.CPathSettings;
import cpath.dao.CPathUtils;
import cpath.dao.MetadataDAO;
import cpath.warehouse.beans.Mapping;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Metadata.METADATA_TYPE;
import cpath.warehouse.beans.PathwayData;

import static org.junit.Assert.*;

/**
 * @author rodche
 *
 */
public class MetadataHibernateDAOTest {
	
	
	@Test
	public void testImportPathwayData() throws IOException {

		CPathUtils.createDatabase(CPathSettings.TEST_DB);
		
		ClassPathXmlApplicationContext context = 
				new ClassPathXmlApplicationContext("classpath:testContext-dao.xml");
        MetadataDAO dao = (MetadataDAO) context.getBean("metadataDAO");
        
        // mock metadata and pathway data
        Metadata md = new Metadata("testpw", "test", "test", "", "",
        		new byte[]{}, METADATA_TYPE.BIOPAX, null, null);        
        byte[] testData = "<rdf>          </rdf>".getBytes(); 
        md.init(false);
        PathwayData pathwayData = new PathwayData(md, "test0");
        pathwayData.setPathwayData(testData);
        pathwayData.setPremergeData(testData);
        md.addPathwayData(pathwayData);
        //add the second pd (for the tests at the end of this method)
        PathwayData pd = new PathwayData(md, "test1");
        pd.setPathwayData("aaaaaaaaaa".getBytes());
        md.addPathwayData(pd);
        
        // test if internal pack/unpach, if any, works well
        assertTrue(Arrays.equals(testData, pathwayData.getPathwayData()));
        assertTrue(Arrays.equals(testData, pathwayData.getPremergeData()));
        // persist
        dao.saveMetadata(md); //this md is still detached
        
        // test get pathwaydata directly (by PK)
        pathwayData = dao.getPathwayData(1);
        assertNotNull(pathwayData);
        assertEquals("test0", pathwayData.getFilename());
        assertNull(pathwayData.getPathwayData()); // ok: it's transient field (not to be saved)
        byte[] read = pathwayData.getPremergeData();
        assertNotNull(read);
        // check whether DB save/read changed data
        assertTrue(Arrays.equals(testData, read));    
        
        //get initialized persistent detached metadata
        md = dao.getMetadataByIdentifier(md.getIdentifier());
        assertNotNull(md);
        assertEquals("testpw", md.getIdentifier());
        assertEquals(2, md.getPathwayData().size());       
        pathwayData = md.getPathwayData().get(1); // the second entry
        // add validation result());          
        testData = "<rdf></rdf>".getBytes();
        pathwayData.setValidationResults("<?xml version=\"1.0\"?>".getBytes());        
        // update
        dao.saveMetadata(md);
         
        //read the latest state
        md = dao.getMetadata(1);
        assertNotNull(md);
        List<PathwayData>  lpd = md.getPathwayData();
        assertFalse(lpd.isEmpty());
        pathwayData = lpd.get(1); // the second entry
        assertNotNull(pathwayData);
        assertNotNull(pathwayData.getValidationResults());
        assertNull(pathwayData.getPremergeData()); // ok, - for the 2nd we did not set premergeData
        assertTrue(pathwayData.getValidationResults().length > 0);  
        assertTrue(Arrays.equals("<?xml version=\"1.0\"?>".getBytes(), pathwayData.getValidationResults()));         
        
        //delete all member pathwaydata
        md.getPathwayData().clear();
        //this should remove orphan (all) pathwaydata entities
        // (as specified by the ORM annotation: cascade, deleteOrphans
        dao.saveMetadata(md);
        md=null;
        md = dao.getMetadata(1);
        assertTrue(md.getPathwayData().isEmpty()); 
        
        context.close();
	}
	
	
	@Test
	public void testImportIdMapping() {		
		
		CPathUtils.createDatabase(CPathSettings.TEST_DB);
		
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:testContext-dao.xml");
        MetadataDAO dao = (MetadataDAO) context.getBean("metadataDAO"); 

        Map<String,String> idMap = new TreeMap<String, String>();
        Mapping map = new Mapping(Mapping.Type.UNIPROT, "test", idMap);
        idMap.put("ZHX1", "P12345");
        idMap.put("ZHX1-C8orf76", "Q12345");  
        //capitalization is important in 99% of identifier types (we should not ignore it)
        // we should be able to save it and not get 'duplicate key' exception here
        idMap.put("ZHX1-C8ORF76", "Q12345"); 
        dao.saveMapping(map);
        
        //check it's saved
        assertEquals(1, dao.mapIdentifier("ZHX1-C8orf76", Mapping.Type.UNIPROT, null).size());
        assertEquals(1, dao.mapIdentifier("ZHX1-C8ORF76", Mapping.Type.UNIPROT, null).size());
        
        // repeat (should successfully update)
        idMap = new TreeMap<String, String>();
        idMap.put("FooBar", "CHEBI:12345");  
        map = new Mapping(Mapping.Type.CHEBI, "test2", idMap);
        //add new Mapping entity
        dao.saveMapping(map);
        assertTrue(dao.mapIdentifier("FooBar", Mapping.Type.UNIPROT, null).isEmpty());
        Set<String> mapsTo = dao.mapIdentifier("FooBar", Mapping.Type.CHEBI, null);
        assertEquals(1, mapsTo.size());
        assertEquals("CHEBI:12345", mapsTo.iterator().next());
        
        context.close();
	}
}
