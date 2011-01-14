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
import java.util.Set;

import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.PublicationXref;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.validator.utils.Normalizer;
import org.junit.Test;


/**
 * @author rodch
 */
public class NormalizerTest {
	/**
	 * Test method for {@link cpath.importer.internal.NormalizerImpl#filter(org.biopax.paxtools.model.Model)}.
	 */
	
	SimpleReader simpleReader;
	SimpleExporter simpleExporter;
	
	public NormalizerTest() {
		simpleExporter = new SimpleExporter(BioPAXLevel.L3);
		simpleReader = new SimpleReader();
		simpleReader.mergeDuplicates(true);
	}

	@Test
	public final void testNormalize() {
		Model model = BioPAXLevel.L3.getDefaultFactory().createModel();
    	Xref ref = model.addNew(UnificationXref.class,
    			"http://www.pathwaycommons.org/import#Xref1");
    	ref.setDb("uniprotkb"); // normalizer should convert this to 'uniprot'
    	ref.setId("P68250");
    	ProteinReference pr = model.addNew(ProteinReference.class,
    			"http://www.pathwaycommons.org/import#ProteinReference1");
    	pr.setDisplayName("ProteinReference1");
    	pr.addXref(ref);
    	ref = model.addNew(RelationshipXref.class, "Xref2");
    	ref.setDb("refseq");
    	ref.setId("NP_001734");
    	ref.setIdVersion("1");  // this xref won't be removed by norm. (version matters in xrefs comparing!)
		pr.addXref(ref);
	   	ref = model.addNew(UnificationXref.class, "Xref3");
    	ref.setDb("uniprotkb"); // will be converted to 'uniprot'
    	/* The following ID is the secondary accession of P68250, 
    	 * but Normalizer won't complain (it's Validator's and - later - Merger's job)!
    	 * However, it it were P68250 here again, the normalize(model) would throw exception
    	 * (because ProteinReference1 becomes ProteinReference2, both get RDFId= urn:miriam:uniprot:P68250!)
    	 */
    	ref.setId("Q0VCL1"); 
    	Xref uniprotX = ref;
    	
    	pr = model.addNew(ProteinReference.class, "ProteinReference2");
    	pr.setDisplayName("ProteinReference2");
    	pr.addXref(uniprotX);
    	ref = model.addNew(RelationshipXref.class, "Xref4");
    	ref.setDb("refseq");
    	ref.setId("NP_001734");
		pr.addXref(ref);
		
		// this ER is duplicate (same uniprot xref as ProteinReference2's) and must be removed by normalizer
    	pr = model.addNew(ProteinReference.class, "ProteinReference3");
    	pr.setDisplayName("ProteinReference3");
    	pr.addXref(uniprotX);
    	ref = model.addNew(RelationshipXref.class, "Xref5");
    	ref.setDb("refseq");
    	ref.setId("NP_001734");
		pr.addXref(ref);
		
		// normalizer won't merge diff. types of xref with the same db:id
	   	ref = model.addNew(PublicationXref.class, "http://biopax.org/Xref5");
    	ref.setDb("pubmed");
    	ref.setId("2549346"); // the same id
    	pr.addXref(ref);
	   	ref = model.addNew(RelationshipXref.class,
	   			"http://www.pathwaycommons.org/import#Xref6");
    	ref.setDb("pubmed"); 
    	ref.setId("2549346"); // the same id
    	pr.addXref(ref);

		// add biosource
	   	ref = model.addNew(UnificationXref.class, "Xref7");
    	ref.setDb("taxonomy"); 
    	ref.setId("10090"); // the same id
		BioSource bioSource = model.addNew(BioSource.class, "BioSource_Mouse_Tissue");
		bioSource.addXref((UnificationXref)ref);
		
		model.getNameSpacePrefixMap().put("", "http://www.pathwaycommons.org/import#");
		model.getNameSpacePrefixMap().put("biopax", "http://biopax.org/");
		
		// normalize!		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			simpleExporter.convertToOWL(model, out);
		} catch (IOException e1) {
			fail(e1.toString());
		}
		
		Normalizer normalizer = new Normalizer();
		String xml = normalizer.normalize(out.toString());
		
		//System.out.println(xml);
		
		// check
		model = simpleReader.convertFromOWL(new ByteArrayInputStream(xml.getBytes()));
		
		// check Xref
		BioPAXElement bpe = model.getByID(Normalizer.BIOPAX_URI_PREFIX + "UnificationXref:UNIPROT_P68250");
		assertTrue(bpe instanceof UnificationXref);
		
		// check PR
		bpe = model.getByID("urn:miriam:uniprot:Q0VCL1");
		assertTrue(bpe instanceof ProteinReference);
		
		//check xref's ID gets normalized
		bpe = model.getByID(Normalizer.BIOPAX_URI_PREFIX + "RelationshipXref:REFSEQ_NP_001734");
		assertEquals(1, ((Xref)bpe).getXrefOf().size());
		// almost the same xref (was different idVersion)
		bpe = model.getByID(Normalizer.BIOPAX_URI_PREFIX + "RelationshipXref:REFSEQ_NP_001734_1");
		assertEquals(1, ((Xref)bpe).getXrefOf().size());
		
    	//TODO test when uniprot's is not the first xref
    	//TODO test illegal 'id', 'db', etc.
    	//TODO add to test CV (and use a MI term)
		
		//test BioSource
		bpe = model.getByID("urn:miriam:taxonomy:10090");
		assertTrue(bpe instanceof BioSource);
		bpe = model.getByID(Normalizer.BIOPAX_URI_PREFIX + "UnificationXref:TAXONOMY_10090");
		assertTrue(bpe instanceof UnificationXref);
		
		
		// test that one of ProteinReference (2nd or 3rd) is removed
		assertEquals(2, model.getObjects(ProteinReference.class).size());
		
		//TODO test Provenance
	}

	
	@Test
	public final void testNormalizeTestFile() throws IOException {
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
		assertFalse(m.containsID(Normalizer.BIOPAX_URI_PREFIX + "UnificationXref:KEGG+COMPOUND_c00022"));
		assertTrue(m.containsID(Normalizer.BIOPAX_URI_PREFIX + "UnificationXref:KEGG+COMPOUND_C00022"));	
	}
}
