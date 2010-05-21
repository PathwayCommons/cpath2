package cpath.converter.internal;

// imports
import cpath.converter.Converter;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.RelationshipXref;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;


/**
 * Test Chebi to BioPAX converter.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:applicationContext-whouseProteins.xml"})
@TransactionConfiguration(transactionManager="proteinsTransactionManager")
public class ChEBIConverterImplTest {

	//@Autowired
	//PaxtoolsDAO proteinsDAO;

	/**
	 * Test method for {@link cpath.converter.internal.ChEBIConverterImpl#convert(java.io.InputStream, org.biopax.paxtools.model.BioPAXLevel)}.
	 * @throws IOException 
	 */
	@Test
	@Transactional
	//@Rollback(false)
	public void testConvert() throws IOException {

		// setup the converter
		Converter converter = new ChEBIConverterImpl();

		// convert test data
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("test_chebi_data.dat");
		Model model = converter.convert(is, BioPAXLevel.L3);
		
		// get all small molecule references out
		assertTrue(model.getObjects(SmallMoleculeReference.class).size() == 3);

		// get lactic acid sm
		String rdfID = "urn:miriam:chebi:422";
		SmallMoleculeReference smallMoleculeReference = (SmallMoleculeReference)model.getByID(rdfID);

		// check some props
		assertTrue(smallMoleculeReference.getDisplayName().equals("(S)-lactic acid"));
		assertTrue(smallMoleculeReference.getName().size() == 10);
		assertTrue(smallMoleculeReference.getChemicalFormula().equals("C[C@H](O)C(O)=O"));
		//assertTrue(smallMoleculeReference.getMolecularWeight() == 90.07794);
		int relationshipXrefCount = 0;
		int unificationXrefCount = 0;
		for (Xref xref : smallMoleculeReference.getXref()) {
			if (xref instanceof RelationshipXref) ++relationshipXrefCount;
			if (xref instanceof UnificationXref) ++ unificationXrefCount;
		}
		assertTrue(unificationXrefCount == 3);
		assertTrue(relationshipXrefCount == 12);

		// dump owl out to stdout for review
		//System.out.println("ChEBI BioPAX: ");
		//(new SimpleExporter(BioPAXLevel.L3)).convertToOWL(model, System.out);
	}
}
