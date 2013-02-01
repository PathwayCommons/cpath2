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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.BioPAXLevel;
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

import cpath.importer.Converter;
import cpath.importer.internal.ImportFactory;

/**
 * @author rodche
 *
 */
//@Ignore
public class UniprotConverterImplTest {

	/**
	 * Test method for {@link cpath.converter.internal.UniprotConverterImpl#convert(java.io.InputStream, Object...)}.
	 * @throws IOException 
	 */
	@Test
	public void testConvert() throws IOException {
		InputStream is = getClass().getResourceAsStream("/test_uniprot_data.dat.gz");
		GZIPInputStream zis = new GZIPInputStream(new BufferedInputStream(is));
		
		Model model = BioPAXLevel.L3.getDefaultFactory().createModel();
		
		Converter converter = ImportFactory.newConverter("cpath.converter.internal.UniprotConverterImpl");
		converter.setModel(model);
		converter.convert(zis);
		
		Set<ProteinReference> proteinReferences = model.getObjects(ProteinReference.class);
		assertEquals(10, proteinReferences.size());
		assertTrue(proteinReferences.iterator().next().getXref().iterator().hasNext());

		//get by URI and check the sequence (CALL6_HUMAN)
		ProteinReference pr = (ProteinReference) model.getByID("http://identifiers.org/uniprot/Q8TD86");
		assertNotNull(pr);
		final String expected = "MGLQQEISLQPWCHHPAESCQTTTDMTERLSAEQIKEYKGVFEMFDEEGNGEVKTGE" +
				"LEWLMSLLGINPTKSELASMAKDVDRDNKGFFNCDGFLALMGVYHEKAQNQESELRAAFRVFDKEGKGYIDWN" +
				"TLKYVLMNAGEPLNEVEAEQMMKEADKDGDRTIDYEEFVAMMTGESFKLIQ";
		assertEquals(expected, pr.getSequence());
		assertTrue(pr.getComment().contains("SEQUENCE   181 AA;  20690 MW;  F29C088AFC42BB13 CRC64;"));
		
		// test MOD_RES features are created
		pr = (ProteinReference) model.getByID("http://identifiers.org/uniprot/P62158");
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
		
		//TODO add more checks that the conversion went ok..
		
		//this is just to test for a bug in the DR text format parser...
		boolean rel = false;
		for(Xref x : pr.getXref()) {
			if("Entrez Gene".equals(x.getDb())) {
				rel = true;
				break;
			}
		}
		assertTrue(rel);
		
		String uri = Normalizer.uri(model.getXmlBase(), "RefSeq", "NP_619650", RelationshipXref.class);
		assertNotNull(model.getByID(uri));
		//but the parser should not create xrefs from for the last parts in DR like "...; -.", "; Homo sapiens.\n", etc.
		uri = Normalizer.uri(model.getXmlBase(), "RefSeq", "-", RelationshipXref.class);
		assertNull(model.getByID(uri));	
		uri = Normalizer.uri(model.getXmlBase(), "Ensembl", "Homo sapiens", RelationshipXref.class);
		assertNull(model.getByID(uri));	
		
		// dump owl for review
		String outFilename = getClass().getClassLoader().getResource("").getPath() 
			+ File.separator + "testConvertUniprot.out.owl";
		(new SimpleIOHandler(BioPAXLevel.L3)).convertToOWL(model, 
				new FileOutputStream(outFilename));
	}

}
