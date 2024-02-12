package cpath.converter;

import cpath.service.CPathUtils;
import cpath.service.api.Converter;

import org.apache.commons.lang3.StringUtils;
import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.level3.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * Test ChEBI to BioPAX converter.
 */
public class ChebiConvertersTest {

	@Test
	public void convertObo() throws IOException {
		// convert test data
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		Converter converter = CPathUtils.newConverter("cpath.converter.ChebiOboConverter");
		converter.setXmlBase("");

		ZipFile zf = new ZipFile(getClass().getResource("/chebi.obo.zip").getFile());
		assertTrue(zf.entries().hasMoreElements());
		ZipEntry ze = zf.entries().nextElement();

		converter.convert(zf.getInputStream(ze), bos);

		Model model = new SimpleIOHandler().convertFromOWL(new ByteArrayInputStream(bos.toByteArray()));
		assertNotNull(model);
		assertFalse(model.getObjects().isEmpty());

		// dump owl for review
		Path outFilename = Paths
				.get(getClass().getClassLoader().getResource("").getPath(),"testConvertChebiObo.out.owl");
		(new SimpleIOHandler(BioPAXLevel.L3)).convertToOWL(model, Files.newOutputStream(outFilename));

		// get all small molecule references out
		assertEquals(7, model.getObjects(SmallMoleculeReference.class).size());
		assertNotNull(model.getByID("bioregistry.io/chebi:58342")); //the SMR without InChIKey

		// get lactic acid sm
		String rdfID = "bioregistry.io/chebi:422";
		assertTrue(model.containsID(rdfID));
		SmallMoleculeReference smallMoleculeReference = (SmallMoleculeReference) model.getByID(rdfID);

		// check some props
		assertEquals("(S)-lactic acid", smallMoleculeReference.getDisplayName());
		assertEquals(13, smallMoleculeReference.getName().size()); //now includes Wikipedia, SMILES(CHEBI) names
		assertEquals("C3H6O3", smallMoleculeReference.getChemicalFormula());
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
		assertEquals(0, publicationXrefCount); //there are no such xrefs anymore

		// following checks work in this test only (using in-memory model); with DAO - use getObject...
		assertTrue(model.containsID("bioregistry.io/chebi:20"));
		EntityReference er20 = (EntityReference) model.getByID("bioregistry.io/chebi:20");
		assertTrue(model.containsID("bioregistry.io/chebi:28"));
//		EntityReference er28 = (EntityReference) model.getByID("bioregistry.io/chebi:28");
		assertTrue(model.containsID("bioregistry.io/chebi:422"));
		EntityReference er422 = (EntityReference) model.getByID("bioregistry.io/chebi:422");

		assertTrue(er20.getMemberEntityReferenceOf().isEmpty());
		assertTrue(er422.getMemberEntityReferenceOf().isEmpty());
		assertTrue(model.containsID("RX_chebi_CHEBI_422_multiple_parent_reference"));

		// check new elements (created by the OBO converter) exist in the model;
		// (particularly, these assertions are important to test within the persistent model (DAO) session)
		assertTrue(model.containsID("RX_chebi_CHEBI_20_see-also"));
		assertTrue(model.containsID("RX_chebi_CHEBI_422_see-also"));


		//after refactoring, make sure there are no CHEBI:* xref.id anymore (only unprefixed)
		model.getObjects(Xref.class).stream().filter(x -> StringUtils.contains(x.getUri(),"chebi"))
				.forEach(x -> assertTrue(StringUtils.containsIgnoreCase(x.getId(),"CHEBI:")));

		model.getObjects(Xref.class).stream().filter(x -> StringUtils.contains(x.getUri(),"chebi"))
				.forEach(x -> assertEquals("chebi", x.getDb()));
	}
}
