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

package cpath.normalizer;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.annotation.Resource;

import org.biopax.miriam.MiriamLink;
import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cpath.normalizer.internal.IdNormalizer;


/**
 * uses idNormalizer-test-context.xml
 * 
 * @author rodch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"classpath:applicationContext-paxtools.xml"})
public class IdNormalizerTest {
	
	@Resource
	SimpleReader simpleReader;
	@Resource
	SimpleExporter simpleExporter;
	
	/**
	 * Test method for {@link cpath.normalizer.internal.IdNormalizer#filter(org.biopax.paxtools.model.Model)}.
	 */
	@Test
	public final void testNormalize() {
		Model model = simpleReader.getFactory().createModel();
    	UnificationXref ref = simpleReader.getFactory().reflectivelyCreate(UnificationXref.class);
    	ref.setDb("uniprotkb");
    	ref.setRDFId("http://www.pathwaycommons.org/import#UnificationXref1");
    	ref.setId("P62158");
    	ProteinReference pr = simpleReader.getFactory().reflectivelyCreate(ProteinReference.class);
    	pr.setRDFId("http://www.pathwaycommons.org/import#ProteinReference1");
    	pr.setDisplayName("ProteinReference1");
    	pr.addXref(ref);
		model.add(pr);
		model.add(ref);
		model.getNameSpacePrefixMap().put("", "http://www.pathwaycommons.org/import#");
		
		// normalize		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			simpleExporter.convertToOWL(model, out);
		} catch (IOException e1) {
			fail(e1.toString());
		}
		
		IdNormalizer idNormalizer = new IdNormalizer(new MiriamLink());
		String xml = idNormalizer.normalize(out.toString());
		System.out.println(xml);
		
		// check the result
		model = simpleReader.convertFromOWL(new ByteArrayInputStream(xml.getBytes()));
		
		BioPAXElement bpe = model.getByID("urn:miriam:uniprot:P62158");
		assertTrue(bpe instanceof ProteinReference);
		
		for(BioPAXElement e : model.getObjects()) {
			System.out.println(e);
		}

    	//TODO test when uniprot's is not the first xref
    	//TODO test illegal 'id', 'db', etc.
    	//TODO add to test CV (and use a MI term)
		//TODO test BioSource
		//TODO test Provenance
	}

}
