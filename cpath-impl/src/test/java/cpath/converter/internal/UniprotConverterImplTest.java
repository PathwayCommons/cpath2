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
import org.biopax.paxtools.model.level3.ModificationFeature;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.SequenceLocation;
import org.biopax.paxtools.model.level3.SequenceSite;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.validator.utils.Normalizer;
//import org.junit.Ignore;
import org.junit.Test;

import cpath.dao.CPathUtils;
import cpath.importer.Converter;
import cpath.importer.ImportFactory;

/**
 * @author rodche
 *
 */
//@Ignore
public class UniprotConverterImplTest {

	/**
	 * Test method for {@link cpath.converter.internal.UniprotConverterImpl#convert(java.io.InputStream)}.
	 * @throws IOException 
	 */
	@Test
	public void testConvert() throws IOException {
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
		
		// test MOD_RES features are created
		ProteinReference pr = (ProteinReference) model.getByID("http://identifiers.org/uniprot/P62158");
		assertTrue(pr.getName().contains("CALM2"));
		assertTrue(pr.getName().contains("CALM3"));
		assertNotNull(pr);
		assertEquals(8, pr.getEntityFeature().size());
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
		String uri = Normalizer.uri(model.getXmlBase(), "REFSEQ", "NP_001734mapped-identity", RelationshipXref.class);
		assertNotNull(model.getByID(uri));
		//test if the correct Ensembl ENSG* xrefs was created from the DR line despite having something ([bla-bla]) after the '.' at the end
		uri = Normalizer.uri(model.getXmlBase(), "ENSEMBL", "ENSG00000143933mapped-identity", RelationshipXref.class);
		assertNotNull(model.getByID(uri));
		uri = Normalizer.uri(model.getXmlBase(), "NCBI GENE", "801mapped-identity", RelationshipXref.class);
		assertNotNull(model.getByID(uri));
		
		uri = Normalizer.uri(model.getXmlBase(), "REFSEQ", "NP_619650mapped-identity", RelationshipXref.class);
		assertNotNull(model.getByID(uri));
		//but the parser should not create xrefs from for the last parts in DR like "...; -.", "; Homo sapiens.\n", etc.
		uri = Normalizer.uri(model.getXmlBase(), "REFSEQ", "-mapped-identity", RelationshipXref.class);
		assertNull(model.getByID(uri));	
		uri = Normalizer.uri(model.getXmlBase(), "ENSEMBL", "Homo sapiensmapped-identity", RelationshipXref.class);
		assertNull(model.getByID(uri));	
		
		//total xrefs generated for P62158
		assertEquals(32, pr.getXref().size());
		
		assertEquals(8, pr.getEntityFeature().size());
	}

}
