package cpath.converter.internal;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipInputStream;

import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.EntityFeature;
import org.biopax.paxtools.model.level3.ModificationFeature;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.SequenceLocation;
import org.biopax.paxtools.model.level3.SequenceModificationVocabulary;
import org.biopax.paxtools.model.level3.SequenceSite;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.paxtools.normalizer.Normalizer;
import org.junit.Test;

import cpath.dao.CPathUtils;
import cpath.importer.Converter;
import cpath.importer.ImportFactory;

/**
 * @author rodche
 *
 */
public class UniprotConverterImplTest {

	/**
	 * Test method for {@link cpath.converter.internal.UniprotConverterImpl#convert(java.io.InputStream)}.
	 * @throws IOException 
	 */
	@Test
	public void testConvert() throws IOException {
		
		//test the tricky FT pattern first
		assertEquals("AA-(test test)test", "AA-(test test)test (Bysimilarity)".replaceFirst("\\([^()]+?\\)$","").trim());	
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		CPathUtils.unzip(new ZipInputStream(new FileInputStream(
				getClass().getResource("/test_uniprot_data.dat.zip").getFile())), bos);
		bos.close();	
		String outFilename = getClass().getClassLoader().getResource("").getPath() 
				+ File.separator + "testConvertUniprot.out.owl";
		Converter converter = ImportFactory.newConverter("cpath.converter.internal.UniprotConverterImpl");
		converter.setXmlBase(null);
		converter.convert(new ByteArrayInputStream(bos.toByteArray()), new FileOutputStream(outFilename));

		// read Model
		Model model = new SimpleIOHandler().convertFromOWL(new FileInputStream(outFilename));
		Set<ProteinReference> proteinReferences = model.getObjects(ProteinReference.class);
		assertEquals(10, proteinReferences.size());
		assertTrue(proteinReferences.iterator().next().getXref().iterator().hasNext());
				
		//
		ProteinReference pr = (ProteinReference) model.getByID("http://identifiers.org/uniprot/P27797");
		assertEquals(10, pr.getName().size()); //make sure this one is passed (important!)
		assertEquals("CALR_HUMAN", pr.getDisplayName());
		assertEquals("Calreticulin", pr.getStandardName());
//		System.out.println("CALR_HUMAN xrefs: " + pr.getXref().toString());
		assertEquals(13, pr.getXref().size()); //no duplicates
		assertTrue(pr.getXref().toString().contains("CALR_HUMAN"));
		assertEquals("http://identifiers.org/taxonomy/9606", pr.getOrganism().getUri());
		
		// test MOD_RES features are created
		pr = (ProteinReference) model.getByID("http://identifiers.org/uniprot/P62158");
		assertTrue(pr.getName().contains("CALM2"));
		assertTrue(pr.getName().contains("CALM3"));
		assertNotNull(pr);
		
		assertEquals(9, pr.getEntityFeature().size());
		
		//check for a feature object by using URI generated the same way as it's in the converter:
		String mfUri = Normalizer.uri(model.getXmlBase(), null, pr.getDisplayName() + "_1", ModificationFeature.class);
		ModificationFeature mf = (ModificationFeature) model.getByID(mfUri);
		assertNotNull(mf);
		assertTrue(pr.getEntityFeature().contains(mf));
		SequenceLocation sl = mf.getFeatureLocation();
		assertTrue(sl instanceof SequenceSite);
		assertEquals(2, ((SequenceSite)sl).getSequencePosition());
		assertEquals("MOD_RES N-acetylalanine", mf.getModificationType().getTerm().iterator().next());
		
		//this is just to test for a bug in the DR text format parser...
		boolean rel = false;
		for(Xref x : pr.getXref()) {
			if("NCBI GENE".equalsIgnoreCase(x.getDb())) {
				rel = true;
				break;
			}
		}
		assertTrue(rel);
		//test if the correct RefSeq xrefs was created from the DR line despite having something ([bla-bla]) after the '.' at the end
		String uri = Normalizer.uri(model.getXmlBase(), "REFSEQ", "NP_001734_identity", RelationshipXref.class);
		assertNotNull(model.getByID(uri));
		//test if the correct Ensembl ENSG* xrefs was created from the DR line despite having something ([bla-bla]) after the '.' at the end
		uri = Normalizer.uri(model.getXmlBase(), "ENSEMBL", "ENSG00000143933_identity", RelationshipXref.class);
		assertNotNull(model.getByID(uri));
		uri = Normalizer.uri(model.getXmlBase(), "NCBI GENE", "801_identity", RelationshipXref.class);
		assertNotNull(model.getByID(uri));
		
		uri = Normalizer.uri(model.getXmlBase(), "REFSEQ", "NP_619650_identity", RelationshipXref.class);
		assertNotNull(model.getByID(uri));
		//but the parser should not create xrefs from for the last parts in DR like "...; -.", "; Homo sapiens.\n", etc.
		uri = Normalizer.uri(model.getXmlBase(), "REFSEQ", "-_identity", RelationshipXref.class);
		assertNull(model.getByID(uri));	
		uri = Normalizer.uri(model.getXmlBase(), "ENSEMBL", "Homo_sapiens_identity", RelationshipXref.class);
		assertNull(model.getByID(uri));	
		
		//total xrefs generated for P62158
		assertEquals(35, pr.getXref().size());
		
		//test for the following FT entry (two-line) was correctly parsed/converted:
		//FT   MOD_RES      45     45       Phosphothreonine; by CaMK4 (By
		//FT                                similarity).
		EntityFeature f = null;
		EntityFeature g = null;
		for(EntityFeature ef : pr.getEntityFeature()) {
			assertEquals(1, ef.getComment().size());
			if(ef.getComment().iterator().next().contains("Phosphothreonine; by CaMK4"))
				f = ef;
			if(ef.getComment().iterator().next().contains("AA-(test"))
				g = ef;
		}
		assertNotNull(f);
		assertTrue(f instanceof ModificationFeature);
		assertTrue(((ModificationFeature)f).getModificationType() instanceof SequenceModificationVocabulary);
		assertEquals("MOD_RES Phosphothreonine", ((ModificationFeature)f).getModificationType().getTerm().iterator().next());
		assertTrue(((ModificationFeature)f).getFeatureLocation() instanceof SequenceSite);
			
		//another special test for records like this one:
		//FT   MOD_RES       1      1       AA-(test test)test (By
		//FT                                similarity).
		assertNotNull(g);
		assertTrue(g instanceof ModificationFeature);
		assertTrue(((ModificationFeature)g).getModificationType() instanceof SequenceModificationVocabulary);
		assertEquals("MOD_RES AA-(test test)test", ((ModificationFeature)g).getModificationType().getTerm().iterator().next());
		assertTrue(((ModificationFeature)g).getFeatureLocation() instanceof SequenceSite);
	}

}
