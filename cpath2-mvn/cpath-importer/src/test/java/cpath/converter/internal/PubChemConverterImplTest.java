package cpath.converter.internal;

// imports
import cpath.converter.Converter;

import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.impl.ModelImpl;
import org.biopax.paxtools.io.simpleIO.SimpleEditorMap;
import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;
import org.biopax.paxtools.model.level3.UnificationXref;

import org.junit.Test;

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;


/**
 * Test PubChem to BioPAX converter.
 *
 */
public class PubChemConverterImplTest {

	/**
	 * Test method for {@link cpath.converter.internal.PubChemConverterImpl#convert(java.io.InputStream, org.biopax.paxtools.model.BioPAXLevel)}.
	 * @throws IOException 
	 */
	@Test
	public void testConvert() throws IOException {

		// setup the converter
		Converter converter = new PubChemConverterImpl();

		// convert test data
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("test_pubchem_data.dat");
		//Model model = BioPAXLevel.L3.getDefaultFactory().createModel();
		// extend Model for the converter the calls 'merge' method to work
		Model model = new ModelImpl(BioPAXLevel.L3.getDefaultFactory()) {
			/* (non-Javadoc)
			 * @see org.biopax.paxtools.impl.ModelImpl#merge(org.biopax.paxtools.model.Model)
			 */
			@Override
			public void merge(Model source) {
				SimpleMerger simpleMerger = new SimpleMerger(new SimpleEditorMap(getLevel()));
				simpleMerger.merge(this, source);
			}
		};
		converter.convert(is, model);
		
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

		assertFalse(model.getObjects(UnificationXref.class).isEmpty());
		
		// dump owl out to stdout for review
		//System.out.println("ChEBI BioPAX: ");
		//(new SimpleExporter(BioPAXLevel.L3)).convertToOWL(model, System.out);
	}
}
