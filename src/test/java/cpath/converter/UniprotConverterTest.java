package cpath.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import cpath.service.CPathUtils;
import org.apache.commons.lang3.StringUtils;
import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.ProteinReference;
import org.biopax.paxtools.model.level3.RelationshipXref;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.paxtools.normalizer.Normalizer;
import org.junit.jupiter.api.Test;

import cpath.service.api.Converter;

/**
 * @author rodche
 */
public class UniprotConverterTest {

	/**
	 * Test method for {@link UniprotConverter#convert(java.io.InputStream, java.io.OutputStream)}.
	 * @throws IOException 
	 */
	@Test
	public void convert() throws IOException {
		//test the tricky FT prop first
		assertEquals("AA-(test test)test", "AA-(test test)test (Bysimilarity)"
				.replaceFirst("\\([^()]+?\\)$","").trim());

		Path outFilename = Paths.get(getClass().getClassLoader()
				.getResource("").getPath(),"testConvertUniprot.out.owl");
		Converter converter = CPathUtils.newConverter("cpath.converter.UniprotConverter");
		converter.setXmlBase(null);

		ZipFile zf = new ZipFile(getClass().getResource("/test_uniprot_data.dat.zip").getFile());
		assertTrue(zf.entries().hasMoreElements());
		ZipEntry ze = zf.entries().nextElement();

		converter.convert(zf.getInputStream(ze), Files.newOutputStream(outFilename));

		// read Model
		Model model = new SimpleIOHandler().convertFromOWL(Files.newInputStream(outFilename));
		Set<ProteinReference> proteinReferences = model.getObjects(ProteinReference.class);
		assertEquals(10, proteinReferences.size());
		assertTrue(proteinReferences.iterator().next().getXref().iterator().hasNext());
				
		ProteinReference pr = (ProteinReference) model.getByID("bioregistry.io/uniprot:P27797");
		assertEquals(10, pr.getName().size()); //make sure this one is passed (important!)
		assertEquals("CALR_HUMAN", pr.getDisplayName());
		assertEquals("Calreticulin", pr.getStandardName());
		assertEquals(48, pr.getXref().size()); //no duplicates (UniProt, HGNC, PDB, IPI, EMBL, PIR, DIP, etc., xrefs)
		assertEquals("bioregistry.io/ncbitaxon:9606", pr.getOrganism().getUri());

		pr = (ProteinReference) model.getByID("bioregistry.io/uniprot:P0DP23");
		assertNotNull(pr);
		assertTrue(pr.getName().contains("CALM"));
		assertTrue(pr.getName().contains("CALM1"));
		assertEquals("CALM1_HUMAN", pr.getDisplayName());
		assertTrue(pr.getXref().toString().contains("CALM1_HUMAN")); //it's in a RX with db='uniprot'
		assertTrue(pr.getXref().toString().contains("1J7P")); // has that PDB xref too
		//we don't convert/add features anymore
		assertTrue(pr.getEntityFeature().isEmpty());
//		assertEquals(11, pr.getEntityFeature().size());
//		//check for a feature object by using URI generated the same way as it's in the converter:
//		String mfUri = Normalizer.uri(model.getXmlBase(), null, pr.getDisplayName() + "_1", ModificationFeature.class);
//		ModificationFeature mf = (ModificationFeature) model.getByID(mfUri);
//		assertNotNull(mf);
//		assertTrue(pr.getEntityFeature().contains(mf));
//		SequenceLocation sl = mf.getFeatureLocation();
//		assertTrue(sl instanceof SequenceSite);
//		assertEquals(2, ((SequenceSite)sl).getSequencePosition());
//		assertEquals("N-acetylalanine", mf.getModificationType().getTerm().iterator().next());
//		//test for the following FT entry was correctly parsed/converted:
//		//		FT   MOD_RES         45
//		//		FT                   /note="Phosphothreonine; by CaMK4"
//		//		FT                   /evidence="ECO:0000250|UniProtKB:P0DP29"
//		ModificationFeature f = null;
//		for(EntityFeature ef : pr.getEntityFeature()) {
//			if((ef instanceof ModificationFeature) && ((ModificationFeature)ef).getModificationType().getTerm().contains("Phosphothreonine")) {
//				f = (ModificationFeature) ef;
//			}
//		}
//		assertNotNull(f);
//		assertTrue(f.getModificationType() instanceof SequenceModificationVocabulary);
//		Set<String> terms = f.getModificationType().getTerm();
//		assertTrue(terms.contains("Phosphothreonine"));
//		assertTrue(f.getFeatureLocation() instanceof SequenceSite);
		
		//this is just to test for a bug in the DR text format parser...
		boolean rel = false;
		for(Xref x : pr.getXref()) {
			if("NCBI GENE".equalsIgnoreCase(x.getDb())) {
				rel = true;
				break;
			}
		}
		assertTrue(rel);
		//test if the correct RefSeq xrefs was created from the DR line despite having something ([bla-bla]) after the '.' at the end
		String uri = Normalizer.uri(model.getXmlBase(), "REFSEQ", "NP_001734_identity", RelationshipXref.class);
		assertNotNull(model.getByID(uri));
		//test if the correct Ensembl ENSG* xrefs was created from the DR line despite having something ([bla-bla]) after the '.' at the end
		uri = Normalizer.uri(model.getXmlBase(), "ENSEMBL", "ENSG00000169885_10_identity", RelationshipXref.class);
		assertNotNull(model.getByID(uri));
		uri = Normalizer.uri(model.getXmlBase(), "NCBI GENE", "801_identity", RelationshipXref.class);
		assertNotNull(model.getByID(uri));
		
		uri = Normalizer.uri(model.getXmlBase(), "REFSEQ", "NP_619650_identity", RelationshipXref.class);
		assertNotNull(model.getByID(uri));
		//but the parser should not create xrefs from for the last parts in DR like "...; -.", "; Homo sapiens.\n", etc.
		uri = Normalizer.uri(model.getXmlBase(), "REFSEQ", "-_identity", RelationshipXref.class);
		assertNull(model.getByID(uri));	
		uri = Normalizer.uri(model.getXmlBase(), "ENSEMBL", "Homo_sapiens_identity", RelationshipXref.class);
		assertNull(model.getByID(uri));	
		
		//total xrefs generated for P0DP23 (the special test '[bla-bla]' id should not be there)
		assertFalse(pr.getXref().toString().contains("bla-bla"));
		assertEquals(274, pr.getXref().size()); //uh, so many PDB IDs...

		model.getObjects(Xref.class).stream().filter(x -> StringUtils.contains(x.getUri(),"uniprot:"))
				.forEach(x -> assertEquals("uniprot", x.getDb()));
	}

}
