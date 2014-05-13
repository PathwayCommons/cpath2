package cpath.converter.internal;

import cpath.config.CPathSettings;
import cpath.dao.CPathUtils;
import cpath.dao.PaxtoolsDAO;
import cpath.importer.Converter;
import cpath.importer.ImportFactory;

import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.level3.*;
import org.biopax.validator.utils.Normalizer;

import org.junit.Test;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;


/**
 * Test Chebi to BioPAX converter.
 *
 * TODO add tests for the Chebi OBO converter!
 */
public class ChebiConvertersTest {

	@Test
	public void testConvertToInMemoryModel() throws IOException {
		// convert test data
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		CPathUtils.unzip(new ZipInputStream(new FileInputStream(
				getClass().getResource("/test_chebi_data.dat.zip").getFile())), bos);
		byte[] data = bos.toByteArray();
		
		//run the SDF converter
		bos.reset(); //re-use
		Converter converter = ImportFactory.newConverter("cpath.converter.internal.ChebiSdfConverterImpl");
		converter.setXmlBase(CPathSettings.getInstance().getXmlBase());		
		converter.convert(new ByteArrayInputStream(data), bos);
		//make model
		Model model = new SimpleIOHandler().convertFromOWL(new ByteArrayInputStream(bos.toByteArray()));
		assertNotNull(model);
		assertFalse(model.getObjects().isEmpty());
		
		//run the OBO Analysis
		bos.reset();
		CPathUtils.unzip(new ZipInputStream(new FileInputStream(
				getClass().getResource("/chebi.obo.zip").getFile())), bos);
		data = bos.toByteArray();
		assertTrue(data.length>0);
		ChebiOntologyAnalysis oboAnalysis = new ChebiOntologyAnalysis();
		oboAnalysis.setInputStream(new ByteArrayInputStream(data));
		oboAnalysis.execute(model);
		bos.close(); data=null;
		
		checkResultModel(model, "testConvertChebiToInMemoryModel.out.owl");
	}
	
	
	private void checkResultModel(Model model, String outf) {
		
		// dump owl for review
		String outFilename = getClass().getClassLoader().getResource("").getPath() 
			+ File.separator + outf;
		
		try {
			if(model instanceof PaxtoolsDAO)
				((PaxtoolsDAO)model).exportModel(new FileOutputStream(outFilename));
			else 
				(new SimpleIOHandler(BioPAXLevel.L3)).convertToOWL(model, 
						new FileOutputStream(outFilename));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		// get all small molecule references out
		assertEquals(6, model.getObjects(SmallMoleculeReference.class).size());

		// get lactic acid sm
		String rdfID = "http://identifiers.org/chebi/CHEBI:422";
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
		assertEquals(2, relationshipXrefCount);
		
		// following checks work in this test only (using in-memory model); with DAO - use getObject...
        assertTrue(model.containsID("http://identifiers.org/chebi/CHEBI:20"));
        EntityReference er20 = (EntityReference) model.getByID("http://identifiers.org/chebi/CHEBI:20");
        assertTrue(model.containsID("http://identifiers.org/chebi/CHEBI:28"));
        EntityReference er28 = (EntityReference) model.getByID("http://identifiers.org/chebi/CHEBI:28");
        assertTrue(model.containsID("http://identifiers.org/chebi/CHEBI:422"));
        EntityReference er422 = (EntityReference) model.getByID("http://identifiers.org/chebi/CHEBI:422");
        
        // 28 has member - 422 has member - 20
        assertTrue(er20.getMemberEntityReferenceOf().contains(er422));
        assertEquals(er20, er422.getMemberEntityReference().iterator().next());
        
		assertTrue(er422.getMemberEntityReferenceOf().contains(er28));
        assertEquals(er422, er28.getMemberEntityReference().iterator().next());

        // check new elements (created by the OBO converter) exist in the model;
        // (particularly, these assertions are important to test within the persistent model (DAO) session)
        assertTrue(model.containsID(Normalizer.uri(model.getXmlBase(), "CHEBI", "CHEBI:20has_part", RelationshipXref.class)));
        assertTrue(model.containsID(Normalizer.uri(model.getXmlBase(), "CHEBI", "CHEBI:422is_conjugate_acid_of", RelationshipXref.class)));
	}
	
}
