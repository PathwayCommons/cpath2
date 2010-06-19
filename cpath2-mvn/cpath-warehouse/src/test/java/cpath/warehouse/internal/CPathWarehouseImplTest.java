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

import static org.junit.Assert.*;

import org.biopax.paxtools.model.level3.ProteinReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cpath.dao.DataServices;
import cpath.warehouse.CPathWarehouse;

/**
 * @author rodche
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"classpath:testContext-whouseMolecules.xml",
		"classpath:testContext-whouseProteins.xml",
		"classpath:testContext-whouseDAO.xml",
		"classpath:applicationContext-cpathWarehouse.xml",
		"classpath:applicationContext-cvRepository.xml"})
public class CPathWarehouseImplTest {

	@Autowired
	CPathWarehouse warehouse;
	
	@Autowired
	ApplicationContext context;
	
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		System.out.println("Preparing...");
		DataServices dataServices = (DataServices) context.getBean("&cpath2_meta_test");
		dataServices.createDatabasesAndTables(false);
	}

	/**
	 * Test method for {@link cpath.warehouse.internal.CPathWarehouseImpl#getObject(java.lang.String, java.lang.Class)}.
	 */
	@Test
	public final void testCreateUtilityClass() {
		ProteinReference pr = warehouse.getObject("urn:miriam:uniprot:P62158", ProteinReference.class);
		//assertNotNull(pr);
	}

	/**
	 * Test method for {@link cpath.warehouse.internal.CPathWarehouseImpl#getAllChildrenOfCv(java.lang.String)}.
	 */
	//@Test
	public final void testGetAllChildrenOfCv() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link cpath.warehouse.internal.CPathWarehouseImpl#getDirectChildrenOfCv(java.lang.String)}.
	 */
	//@Test
	public final void testGetDirectChildrenOfCv() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link cpath.warehouse.internal.CPathWarehouseImpl#getParentsOfCv(java.lang.String)}.
	 */
	//@Test
	public final void testGetParentsOfCv() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link cpath.warehouse.internal.CPathWarehouseImpl#getDirectParentsOfCv(java.lang.String)}.
	 */
	//@Test
	public final void testGetDirectParentsOfCv() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link cpath.warehouse.internal.CPathWarehouseImpl#getPrimaryURI(java.lang.String)}.
	 */
	//@Test
	public final void testGetPrimaryURI() {
		fail("Not yet implemented"); // TODO
	}
}
