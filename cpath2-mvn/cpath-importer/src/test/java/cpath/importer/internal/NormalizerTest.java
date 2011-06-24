package cpath.importer.internal;
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

import java.io.*;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.*;
import org.biopax.validator.utils.Normalizer;
import org.junit.Ignore;
import org.junit.Test;


/**
 * Additional tests
 * for {@link cpath.importer.internal.NormalizerImpl#filter(org.biopax.paxtools.model.Model)}.
 * 
 * @author rodch
 */
//@Ignore
public class NormalizerTest {

	@Test
	public final void testNormalizeTestFile() throws IOException {
		SimpleIOHandler simpleReader = new SimpleIOHandler();
		simpleReader.mergeDuplicates(true);
		
		Normalizer normalizer = new Normalizer();
		Model m = simpleReader.convertFromOWL(getClass()
				.getResourceAsStream("/biopax-level3-test.owl"));
		normalizer.normalize(m);
/*
		Set<UnificationXref> xrefs = m.getObjects(UnificationXref.class);
		for(UnificationXref x : xrefs) {
			System.out.println(x.getRDFId() + " [" + x + "]");
		}
*/
		assertFalse(m.containsID("urn:biopax:UnificationXref:KEGG+COMPOUND_c00022"));
		assertTrue(m.containsID("urn:biopax:UnificationXref:KEGG+COMPOUND_C00022"));	
	}
}
