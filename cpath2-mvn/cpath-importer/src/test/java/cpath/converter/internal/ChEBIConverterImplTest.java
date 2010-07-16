package cpath.converter.internal;

// imports
import cpath.converter.Converter;

import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.impl.ModelImpl;
import org.biopax.paxtools.io.simpleIO.SimpleEditorMap;
import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.level3.*;

import org.junit.Test;

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.InputStream;


/**
 * Test Chebi to BioPAX converter.
 *
 */
public class ChEBIConverterImplTest {

	/**
	 * Test method for {@link cpath.converter.internal.ChEBIConverterImpl#convert(java.io.InputStream, org.biopax.paxtools.model.BioPAXLevel)}.
	 * @throws IOException 
	 */
	@Test
	public void testConvert() throws IOException {

		// setup the converter
		Converter converter = new ChEBIConverterImpl();

		// convert test data
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("test_chebi_data.dat");
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
		assertTrue(model.getObjects(SmallMoleculeReference.class).size() == 6);

		// get lactic acid sm
		String rdfID = "urn:miriam:chebi:422";
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
		assertTrue(unificationXrefCount == 1);
		assertTrue(relationshipXrefCount == 12);

		// dump owl out to stdout for review
		System.out.println("ChEBI BioPAX: ");
		(new SimpleExporter(BioPAXLevel.L3)).convertToOWL(model, System.out);
	}
}
