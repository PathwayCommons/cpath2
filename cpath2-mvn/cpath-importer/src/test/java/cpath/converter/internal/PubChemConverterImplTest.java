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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;


/**
 * Test PubChem to BioPAX converter.
 *
 */
public class PubChemConverterImplTest {

	/**
	 * Test method for {@link cpath.converter.internal.PubChemConverterImpl#convert(java.io.InputStream)}.
	 * @throws IOException 
	 */
	@Test
	public void testConvert() throws IOException {

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

		// setup the converter
		Converter converter = new PubChemConverterImpl(model);
		converter.convert(is);
		
		// dump owl for review
		String outFilename = getClass().getClassLoader().getResource("").getPath() 
			+ File.separator + "testConvertPubchem.out.owl";
		(new SimpleExporter(BioPAXLevel.L3)).convertToOWL(model, 
				new FileOutputStream(outFilename));
		
		// small molecule references without smiles or inchi are skipped!
		assertTrue(model.containsID("urn:miriam:pubchem.substance:14438"));
		assertTrue(model.containsID("urn:miriam:pubchem.substance:14439"));
		
		assertTrue(model.containsID("urn:pathwaycommons:CRPUJAZIXJMDBK-DTWKUNHWBS"));
		assertTrue(model.containsID("urn:pathwaycommons:ChemicalStructure:CRPUJAZIXJMDBK-DTWKUNHWBS"));
		
		// get Cyclohexyl acetate
		String rdfID = "urn:miriam:pubchem.substance:14438";
		SmallMoleculeReference smallMoleculeReference = (SmallMoleculeReference)model.getByID(rdfID);
		assertNotNull(smallMoleculeReference);

		// check comments
		Set<String> comments = smallMoleculeReference.getComment();
		assertEquals(2, comments.size());
		for (String comment : comments) {
			assertTrue(comment.equals("CAS: 105-86-2") || comment.equals("Deposited Compound"));
		}

		assertFalse(model.getObjects(UnificationXref.class).isEmpty());
	}
}
