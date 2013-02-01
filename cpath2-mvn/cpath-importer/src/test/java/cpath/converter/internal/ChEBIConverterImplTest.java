package cpath.converter.internal;

// imports
import cpath.importer.Converter;
import cpath.importer.internal.ImportFactory;

import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.level3.*;
import org.biopax.validator.utils.Normalizer;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Test Chebi to BioPAX converter.
 *
 */
//@Ignore
public class ChEBIConverterImplTest {

	/**
	 * Test method for {@link cpath.converter.internal.ChEBIConverterImpl#convert(java.io.InputStream, Object...)}.
	 * @throws IOException 
	 */
	@Test
	public void testConvert() throws IOException {
	
		// convert test data
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("test_chebi_data.dat");
		Model model = BioPAXLevel.L3.getDefaultFactory().createModel();
		model.setXmlBase("test/"); // some xml:base

		// setup the converter using a special constructor to set mock chebi.obo data
		Converter converter = ImportFactory.newConverter("cpath.converter.internal.ChEBIConverterImpl");
		converter.setModel(model);
		converter.convert(is, "classpath:chebi.obo");
		
		// dump owl for review
		String outFilename = getClass().getClassLoader().getResource("").getPath() 
			+ File.separator + "testConvertChebi.out.owl";
		(new SimpleIOHandler(BioPAXLevel.L3)).convertToOWL(model, 
				new FileOutputStream(outFilename));

		// get all small molecule references out
		assertEquals(3, model.getObjects(SmallMoleculeReference.class).size());

		// get lactic acid sm
		String rdfID = "http://identifiers.org/obo.chebi/CHEBI:422";
		assertTrue(model.containsID(rdfID));
		SmallMoleculeReference smallMoleculeReference = (SmallMoleculeReference)model.getByID(rdfID);

		// check some props
		assertTrue(smallMoleculeReference.getDisplayName().equals("(S)-lactic acid"));
		assertTrue(smallMoleculeReference.getName().size() == 10);
		assertTrue(smallMoleculeReference.getChemicalFormula().equals("C3H6O3"));
		int relationshipXrefCount = 0;
		int unificationXrefCount = 0;
		for (Xref xref : smallMoleculeReference.getXref()) {
			if (xref instanceof RelationshipXref) ++relationshipXrefCount;
			if (xref instanceof UnificationXref) ++ unificationXrefCount;
		}
		assertEquals(3, unificationXrefCount);
		assertEquals(12, relationshipXrefCount);
		
		// following checks work in this test only (using in-memory model); with DAO - use getObject...
        assertTrue(model.containsID("http://identifiers.org/obo.chebi/CHEBI:20"));
        EntityReference er20 = (EntityReference) model.getByID("http://identifiers.org/obo.chebi/CHEBI:20");
        assertTrue(model.containsID("http://identifiers.org/obo.chebi/CHEBI:28"));
        EntityReference er28 = (EntityReference) model.getByID("http://identifiers.org/obo.chebi/CHEBI:28");
        assertTrue(model.containsID("http://identifiers.org/obo.chebi/CHEBI:422"));
        EntityReference er422 = (EntityReference) model.getByID("http://identifiers.org/obo.chebi/CHEBI:422");
        
        // 28 has member - 422 has member - 20
        assertTrue(er20.getMemberEntityReferenceOf().contains(er422));
        assertEquals(er20, er422.getMemberEntityReference().iterator().next());
        
		assertTrue(er422.getMemberEntityReferenceOf().contains(er28));
        assertEquals(er422, er28.getMemberEntityReference().iterator().next());

        assertTrue(model.containsID(Normalizer.uri("test/", "CHEBI", "CHEBI:20has_part", RelationshipXref.class))); //has_part
        assertTrue(model.containsID(Normalizer.uri("test/", "CHEBI", "CHEBI:422is_conjugate_acid_of", RelationshipXref.class))); //
	}
}
