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

import java.io.*;

import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.junit.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import static org.junit.Assert.*;

/**
 * @author rodche
 *
 */
public class SimpleExporterTest {

	final ResourceLoader resourceLoader = new DefaultResourceLoader();
	final SimpleReader reader = new SimpleReader();
	
	@Test
	public void testReadWriteRead() throws IOException {
		
		// read medel from owl
		Model model = reader.convertFromOWL(
				resourceLoader.getResource("classpath:biopax-level3-test.owl")
					.getInputStream());
		assertNotNull(model);
		assertTrue(model.containsID("http://www.biopax.org/examples/myExample#Stoichiometry_58"));
		assertEquals(50, model.getObjects(BioPAXElement.class).size());
		
		// write (unchanged)
		OutputStream out = new ByteArrayOutputStream();
		(new SimpleExporter(BioPAXLevel.L3)).convertToOWL(model, out);
		
		// read again
		model = null;
		String bytes = out.toString();
		reader.mergeDuplicates(true);
		model = reader.convertFromOWL(new ByteArrayInputStream(bytes.getBytes()));
		assertNotNull(model);
		assertTrue(model.containsID("http://www.biopax.org/examples/myExample#Stoichiometry_58"));
		assertEquals(50, model.getObjects().size());
	}
	
}
