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

import javax.annotation.Resource;

import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.PublicationXref;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.Xref;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


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
	 * Test method for {@link cpath.importer.internal.IdNormalizer#filter(org.biopax.paxtools.model.Model)}.
	 */
	@Test
	public final void testNormalize() {
		Model model = simpleReader.getFactory().createModel();
    	Xref ref = simpleReader.getFactory().reflectivelyCreate(UnificationXref.class);
    	ref.setDb("uniprotkb"); // normalizer should convert this to 'uniprot'
    	ref.setRDFId("http://www.pathwaycommons.org/import#Xref1");
    	ref.setId("P68250");
    	ProteinReference pr = simpleReader.getFactory().reflectivelyCreate(ProteinReference.class);
    	pr.setRDFId("http://www.pathwaycommons.org/import#ProteinReference1");
    	pr.setDisplayName("ProteinReference1");
    	pr.addXref(ref);
    	model.add(ref);
    	ref = simpleReader.getFactory().reflectivelyCreate(RelationshipXref.class);
    	ref.setDb("refseq");
    	ref.setRDFId("Xref2");
    	ref.setId("NP_001734");
    	ref.setIdVersion("1");
		model.add(ref);
		pr.addXref(ref);
		model.add(pr);
		
		
	   	ref = simpleReader.getFactory().reflectivelyCreate(UnificationXref.class);
    	ref.setDb("uniprotkb"); // will be converted to 'uniprot'
    	ref.setRDFId("Xref3");
    	/* The following ID is the secondary accession of P68250, 
    	 * but Normalizer won't complain (it's Validator's and - later - Merger's job)!
    	 * However, it it were P68250 here again, the normalize(model) would throw exception
    	 * (because ProteinReference1 becomes ProteinReference2, both get RDFId= urn:miriam:uniprot:P68250!)
    	 */
    	ref.setId("Q0VCL1"); 
    	pr = simpleReader.getFactory().reflectivelyCreate(ProteinReference.class);
    	pr.setRDFId("ProteinReference2");
    	pr.setDisplayName("ProteinReference2");
    	pr.addXref(ref);
		model.add(ref);
    	ref = simpleReader.getFactory().reflectivelyCreate(RelationshipXref.class);
    	ref.setDb("refseq");
    	ref.setRDFId("Xref4");
    	ref.setId("NP_001734"); //normalizer must get rid of one of these two refseq xrefs!
    	//ref.setIdVersion("1"); // shall we consider versions comparing xrefs?
		model.add(ref);
		pr.addXref(ref);
		model.add(pr);
		
		// normalizer won't merge diff. types of xref with the same db:id
	   	ref = simpleReader.getFactory().reflectivelyCreate(PublicationXref.class);
    	ref.setDb("pubmed"); 
    	ref.setRDFId("http://biopax.org/Xref5");
    	ref.setId("2549346"); // the same id
    	pr.addXref(ref);
    	model.add(ref);
	   	ref = simpleReader.getFactory().reflectivelyCreate(RelationshipXref.class);
    	ref.setDb("pubmed"); 
    	ref.setRDFId("http://www.pathwaycommons.org/import#Xref6");
    	ref.setId("2549346"); // the same id
    	pr.addXref(ref);
		model.add(ref);

		// add biosource
	   	ref = simpleReader.getFactory().reflectivelyCreate(UnificationXref.class);
    	ref.setDb("taxonomy"); 
    	ref.setRDFId("Xref7");
    	ref.setId("10090"); // the same id
		BioSource bioSource = simpleReader.getFactory().reflectivelyCreate(BioSource.class);
		bioSource.setRDFId("BioSource_Mouse_Tissue");
		bioSource.addXref((UnificationXref)ref);
		model.add(ref);
		model.add(bioSource);
		
		model.getNameSpacePrefixMap().put("pc", "http://www.pathwaycommons.org/import#");
		model.getNameSpacePrefixMap().put("", "http://biopax.org/");
		
		// normalize!		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			simpleExporter.convertToOWL(model, out);
		} catch (IOException e1) {
			fail(e1.toString());
		}
		
		IdNormalizer idNormalizer = new IdNormalizer();
		String xml = idNormalizer.normalize(out.toString());
		
		System.out.println(xml);
		
		// check
		model = simpleReader.convertFromOWL(new ByteArrayInputStream(xml.getBytes()));
		
		// check Xref
		BioPAXElement bpe = model.getByID("http://biopax.org/UnificationXref#UniProt_P68250");
		assertTrue(bpe instanceof UnificationXref);
		
		// check PR
		bpe = model.getByID("urn:miriam:uniprot:Q0VCL1");
		assertTrue(bpe instanceof ProteinReference);
		
		//check when two xrefs have the same db:id
		bpe = model.getByID("http://biopax.org/RelationshipXref#RefSeq_NP_001734");
		assertEquals(2, ((Xref)bpe).getXrefOf().size());
		
		
    	//TODO test when uniprot's is not the first xref
    	//TODO test illegal 'id', 'db', etc.
    	//TODO add to test CV (and use a MI term)
		
		//test BioSource
		bpe = model.getByID("urn:miriam:taxonomy:10090");
		assertTrue(bpe instanceof BioSource);
		bpe = model.getByID("http://biopax.org/UnificationXref#Taxonomy_10090");
		assertTrue(bpe instanceof UnificationXref);
		
		
		//TODO test Provenance
	}

	
	// TODO fix/clean the input file - it has biopax errors!
	//@Test
	public final void testNormalizeTestFile() throws IOException {
		IdNormalizer idNormalizer = new IdNormalizer();
		Model m = simpleReader.convertFromOWL(getClass()
				.getResourceAsStream(File.separator + "biopax-level3-test.owl"));
		String xml = idNormalizer.normalize(m);
		System.out.println(xml);	
		// check the result
		Model model = simpleReader.convertFromOWL(new ByteArrayInputStream(xml.getBytes()));
	}
}
