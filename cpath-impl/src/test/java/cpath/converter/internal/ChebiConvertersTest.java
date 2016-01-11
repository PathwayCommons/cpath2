package cpath.converter.internal;

import cpath.config.CPathSettings;
import cpath.dao.CPathUtils;
import cpath.importer.Converter;
import cpath.importer.ImportFactory;

import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.normalizer.Normalizer;

import org.junit.Test;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;


/**
 * Test ChEBI to BioPAX converter.
 *
 */
public class ChebiConvertersTest {	
	
	@Test
	public void testConvertObo() throws IOException {
		// convert test data
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		CPathUtils.unzip(new ZipInputStream(new FileInputStream(
				getClass().getResource("/chebi.obo.zip").getFile())), bos);
		byte[] data = bos.toByteArray();
		//run the new OBO converter
		bos.reset(); //re-use
		Converter converter = ImportFactory.newConverter("cpath.converter.internal.ChebiOboConverterImpl");
		converter.setXmlBase(CPathSettings.getInstance().getXmlBase());		
		converter.convert(new ByteArrayInputStream(data), bos);
		
		Model model = new SimpleIOHandler().convertFromOWL(new ByteArrayInputStream(bos.toByteArray()));
		assertNotNull(model);
		assertFalse(model.getObjects().isEmpty());
		
		bos.close(); data=null;
		
		// dump owl for review
		String outFilename = getClass().getClassLoader().getResource("").getPath() 
			+ File.separator + "testConvertChebiObo.out.owl";		
		(new SimpleIOHandler(BioPAXLevel.L3)).convertToOWL(model, new FileOutputStream(outFilename));
		
		// get all small molecule references out
		assertEquals(7, model.getObjects(SmallMoleculeReference.class).size());
		assertNotNull(model.getByID("http://identifiers.org/chebi/CHEBI:58342")); //the SMR without InChIKey

		// get lactic acid sm
		String rdfID = "http://identifiers.org/chebi/CHEBI:422";
		assertTrue(model.containsID(rdfID));
		SmallMoleculeReference smallMoleculeReference = (SmallMoleculeReference)model.getByID(rdfID);

		// check some props
		assertEquals("(S)-lactic acid", smallMoleculeReference.getDisplayName());
		assertEquals(13, smallMoleculeReference.getName().size()); //now includes Wikipedia, SMILES(CHEBI) names
		assertEquals("C3H6O3", smallMoleculeReference.getChemicalFormula());
		System.out.println("CHEBI:422 xrefs (from OBO): " + smallMoleculeReference.getXref());
		int relationshipXrefCount = 0;
		int unificationXrefCount = 0;
		int publicationXrefCount = 0;
		for (Xref xref : smallMoleculeReference.getXref()) {
			if (xref instanceof RelationshipXref) ++relationshipXrefCount;
			if (xref instanceof UnificationXref) ++ unificationXrefCount;
			if (xref instanceof PublicationXref) ++ publicationXrefCount;
		}
		assertEquals(1, unificationXrefCount); //no secondary ChEBI IDs there (non-chebi are, if any, relationship xrefs)
		assertEquals(7, relationshipXrefCount); //chebi, inchikey, cas, kegg, hmdb
		assertEquals(9, publicationXrefCount); //there are nine such xrefs
		
		// following checks work in this test only (using in-memory model); with DAO - use getObject...
        assertTrue(model.containsID("http://identifiers.org/chebi/CHEBI:20"));
        EntityReference er20 = (EntityReference) model.getByID("http://identifiers.org/chebi/CHEBI:20");
        assertTrue(model.containsID("http://identifiers.org/chebi/CHEBI:28"));
        EntityReference er28 = (EntityReference) model.getByID("http://identifiers.org/chebi/CHEBI:28");
        assertTrue(model.containsID("http://identifiers.org/chebi/CHEBI:422"));
        EntityReference er422 = (EntityReference) model.getByID("http://identifiers.org/chebi/CHEBI:422");
        
// member are not generated anymore (only rel. xrefs 'is_a' -> multiple_parent_reference rel. type)
//        // 28 has member - 422 has member - 20
//        assertTrue(er20.getMemberEntityReferenceOf().contains(er422));
//        assertEquals(er20, er422.getMemberEntityReference().iterator().next());
//		assertTrue(er422.getMemberEntityReferenceOf().contains(er28));
//        assertEquals(er422, er28.getMemberEntityReference().iterator().next());
		assertTrue(er20.getMemberEntityReferenceOf().isEmpty());
		assertTrue(er422.getMemberEntityReferenceOf().isEmpty());
		assertTrue(model.containsID(Normalizer.uri(model.getXmlBase(), "chebi", "CHEBI_422_multiple_parent_reference", RelationshipXref.class)));

        // check new elements (created by the OBO converter) exist in the model;
        // (particularly, these assertions are important to test within the persistent model (DAO) session)
        assertTrue(model.containsID(Normalizer.uri(model.getXmlBase(), "CHEBI", "CHEBI_20_see-also", RelationshipXref.class)));
        assertTrue(model.containsID(Normalizer.uri(model.getXmlBase(), "CHEBI", "CHEBI_422_see-also", RelationshipXref.class)));		
		
	}
}
