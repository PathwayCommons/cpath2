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

import org.biopax.miriam.MiriamLink;
import org.junit.Test;

/**
 * @author rodch
 *
 */
public class MiriamTest {

	/**
	 * Test method for {@link cpath.common.internal.MiriamAdapter#getURI(java.lang.String, java.lang.String)}.
	 */
	@Test
	public final void testGetURI() {
		String urn = MiriamLink.getURI("uniprot", "P62158");
		assertEquals("urn:miriam:uniprot:P62158", urn);
	}
	
	@Test
	public final void testGetURI_CV() {
		String urn = MiriamLink.getURI("GO", "GO:0005654");
		assertEquals("urn:miriam:obo.go:GO%3A0005654", urn);
	}
	
	@Test
	public final void testGetURI_wrong() {
		try {
			String urn = MiriamLink.getURI("uniprotkb", "A62158");
			fail("must throw the IllegalArgumentException!");
		} catch (IllegalArgumentException e) {
		}
	}
	

	/**
	 * Test method for {@link cpath.common.internal.MiriamAdapter#getDataTypeURN(java.lang.String)}.
	 */
	@Test
	public final void testGetDataTypeURN_byId() {
		String urn = MiriamLink.getDataTypeURI("urn:miriam:uniprot");
		assertEquals("urn:miriam:uniprot", urn);
	}
	
	@Test
	public final void testGetDataTypeURN_byName() {
		String urn = MiriamLink.getDataTypeURI("uniprotkb");
		assertEquals("urn:miriam:uniprot", urn);
	}

}
