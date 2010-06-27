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
 * Test PubChem to BioPAX converter.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:testContext-dao.xml"})
@TransactionConfiguration(transactionManager="proteinsTransactionManager")
public class PubChemConverterImplTest {

	//@Autowired
	//PaxtoolsDAO proteinsDAO;

	/**
	 * Test method for {@link cpath.converter.internal.PubChemConverterImpl#convert(java.io.InputStream, org.biopax.paxtools.model.BioPAXLevel)}.
	 * @throws IOException 
	 */
	@Test
	@Transactional
	//@Rollback(false)
	public void testConvert() throws IOException {

		// setup the converter
		Converter converter = new PubChemConverterImpl();

		// convert test data
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("test_pubchem_data.dat");
		Model model = converter.convert(is, BioPAXLevel.L3);
		
		// get all small molecule references out
		assertTrue(model.getObjects(SmallMoleculeReference.class).size() == 4);

		// get Cyclohexyl acetate
		String rdfID = "urn:miriam:pubchem:14441";
		SmallMoleculeReference smallMoleculeReference = (SmallMoleculeReference)model.getByID(rdfID);
		assertTrue(smallMoleculeReference != null);

		// check comments
		Set<String> comments = smallMoleculeReference.getComment();
		assert(comments.size() == 2);
		for (String comment : comments) {
			assertTrue(comment.equals("CAS: 622-45-7") || comment.equals("Deposited Compound"));
		}

		// dump owl out to stdout for review
		//System.out.println("ChEBI BioPAX: ");
		//(new SimpleExporter(BioPAXLevel.L3)).convertToOWL(model, System.out);
	}
}
