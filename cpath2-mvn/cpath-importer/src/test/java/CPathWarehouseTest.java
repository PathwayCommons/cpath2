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


import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collection;

import org.biopax.paxtools.model.level3.ProteinReference;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cpath.dao.PaxtoolsDAO;
import cpath.dao.internal.DataServicesFactoryBean;
import cpath.fetcher.*;
import cpath.warehouse.*;
import cpath.warehouse.beans.*;
import cpath.warehouse.beans.Metadata.TYPE;
import cpath.warehouse.internal.CPathWarehouseImpl;

/**
 * Test the CPathWarehouse implementation,
 * except for CVs (tested in cpath-warehouse module),
 * using DAO and some test data 
 * 
 * @author rodche
 *
 */
public class CPathWarehouseTest {

	CPathWarehouse warehouse;
	
	public CPathWarehouseTest() throws IOException {
		System.out.println("Preparing...");
		// init the test database
		DataServicesFactoryBean.createSchema("cpath2_test");
		
		// load beans
		ApplicationContext context = new ClassPathXmlApplicationContext(
			new String[]{
				"classpath:testContext-allDAO.xml", 
				"classpath:applicationContext-cpathFetcher.xml"});
		PaxtoolsDAO moleculesDAO = (PaxtoolsDAO) context.getBean("moleculesDAO");
		MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");
		PaxtoolsDAO proteinsDAO = (PaxtoolsDAO) context.getBean("proteinsDAO");
		PathwayDataDAO pathwayDataDAO = (PathwayDataDAO) context.getBean("pathwayDataDAO");

		// create Warehouse instance
		warehouse = new CPathWarehouseImpl(moleculesDAO, null, proteinsDAO);	
		
		// load test data
		WarehouseDataService warehouseDataService = 
			(WarehouseDataService) context.getBean("warehouseDataService");
		ProviderMetadataService metadataService = 
			(ProviderMetadataService) context.getBean("providerMetadataService");
		ProviderPathwayDataService pathwayDataService = 
			(ProviderPathwayDataService) context.getBean("providerPathwayDataService");
        Collection<Metadata> metadata = metadataService.getProviderMetadata("classpath:metadata.html");
        for (Metadata mdata : metadata) {
            metadataDAO.importMetadata(mdata);
            if(mdata.getType() == TYPE.PROTEIN) {
            	warehouseDataService.storeWarehouseData(mdata, proteinsDAO);
            } else if(mdata.getType() == TYPE.SMALL_MOLECULE) {
            	warehouseDataService.storeWarehouseData(mdata, moleculesDAO);
            } else {
            	Collection<PathwayData> pathwayData =
					pathwayDataService.getProviderPathwayData(mdata);
				for (PathwayData pwData : pathwayData) {
					pathwayDataDAO.importPathwayData(pwData);
				}
            }
        }
        
        //warehouse.createIndex();
	}

	/**
	 * Test method for 
	 * {@link cpath.warehouse.internal.CPathWarehouseImpl#getObject(java.lang.String, java.lang.Class)}.
	 */
	@Test
	public final void testCreateUtilityClass() {
		ProteinReference pr = warehouse
			.getObject("urn:miriam:uniprot:P62158", ProteinReference.class);
		//assertNotNull(pr);
	}


	/**
	 * Test method for {@link cpath.warehouse.internal.CPathWarehouseImpl#getPrimaryURI(java.lang.String)}.
	 */
	//@Test
	public final void testGetPrimaryURI() {
		fail("Not yet implemented"); // TODO
	}
}
