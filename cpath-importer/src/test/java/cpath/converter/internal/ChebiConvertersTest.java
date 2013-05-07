package cpath.converter.internal;

// imports
import cpath.config.CPathSettings;
import cpath.dao.Analysis;
import cpath.dao.CPathUtils;
import cpath.dao.PaxtoolsDAO;
import cpath.importer.Converter;
import cpath.importer.internal.ImportFactory;

import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.level3.*;
import org.biopax.validator.utils.Normalizer;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Test Chebi to BioPAX converter.
 *
 */
public class ChebiConvertersTest {

	@Test
	public void testConvertToInMemoryModel() throws IOException {
		// convert test data
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("test_chebi_data.dat");

		// init and run the SDF converter first
		Converter converter = ImportFactory.newConverter("cpath.converter.internal.ChebiSdfConverterImpl");
		converter.setInputStream(is);
		converter.setXmlBase(CPathSettings.xmlBase());
		Model model = converter.convert();
		
		// second, run the OBO converter
		is = this.getClass().getClassLoader().getResourceAsStream("chebi.obo");
		converter = ImportFactory.newConverter("cpath.converter.internal.ChebiOboConverterImpl");
		assertTrue(converter instanceof Analysis);
		converter.setInputStream(is);
//not needed here:		converter.setXmlBase(CPathSettings.xmlBase());
		((Analysis)converter).execute(model);
		
		checkResultModel(model, "testConvertChebiToInMemoryModel.out.owl");
	}
	
		
	@Test
	public void testConvertToPersistentModel() throws IOException {
		// get the DAO (implements PaxtoolsDAO and Model interfaces)
		CPathUtils.createDatabase(CPathSettings.TEST_DB);
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] {"classpath:testContext-dao.xml"});
		final Model model = (Model) context.getBean("paxtoolsDAO");	
		
		// convert test SDF data
		final InputStream is1 = this.getClass().getClassLoader().getResourceAsStream("test_chebi_data.dat");
		// init and run the SDF converter first
		final Converter sdfConverter = ImportFactory.newConverter("cpath.converter.internal.ChebiSdfConverterImpl");
		sdfConverter.setInputStream(is1);
		sdfConverter.setXmlBase(model.getXmlBase());
		Model m = sdfConverter.convert(); //conv. builds a new model
		model.merge(m);
				
		// second, run the OBO converter
		final InputStream is2 = this.getClass().getClassLoader().getResourceAsStream("chebi.obo");
		final Converter oboConverter = ImportFactory.newConverter("cpath.converter.internal.ChebiOboConverterImpl");
		if(oboConverter instanceof Analysis) {
			oboConverter.setXmlBase(model.getXmlBase());
			oboConverter.setInputStream(is2);
			((PaxtoolsDAO)model).run((Analysis) oboConverter);
		} else {
			fail("Chebi Obo Converter does not implement Analysis; cannot modify the Warehouse model.");
		}
		
		// do various checks (transactionally)
		((PaxtoolsDAO)model).run(new Analysis() {			
			@Override
			public void execute(Model model) {
				checkResultModel((Model)model, "testConvertChebiToPersistentModel.out.owl");
			}
		});
		
		context.close();
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
		assertEquals(0, relationshipXrefCount); //modified sdf converter skips for most ext. refs (at least for this test data).
		
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
