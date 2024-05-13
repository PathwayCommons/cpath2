package cpath.service;


import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import cpath.service.metadata.Datasource;
import cpath.service.metadata.Metadata;
import org.biopax.paxtools.io.*;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;

import cpath.service.metadata.Datasource.METADATA_TYPE;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class CPathUtilsTest {

	static Model model;
	static SimpleIOHandler exporter;

	static {
		exporter = new SimpleIOHandler(BioPAXLevel.L3);
		// extend Model for the converter calling 'merge' method to work
		model = BioPAXLevel.L3.getDefaultFactory().createModel();
		model.setXmlBase("test:");
	}

	@Test
	public void copyWithGzip() throws IOException {
		String outFilename = getClass().getClassLoader().getResource("").getPath()
				+ File.separator + "testCopyWithGzip.gz";
		byte[] testData = "<rdf>          </rdf>".getBytes();
		ByteArrayInputStream is = new ByteArrayInputStream(testData);
		OutputStream gzip = new GZIPOutputStream(new FileOutputStream(outFilename));
		CPathUtils.copy(is, gzip);
		is.close();
		gzip.close();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		CPathUtils.copy(new GZIPInputStream(new FileInputStream(outFilename)), os);
		byte[] read = os.toByteArray();
        assertNotNull(read);
        assertTrue(Arrays.equals(testData, read));
	}

	@Test
	public void copy() throws IOException {
		Path f = Paths.get(getClass().getClassLoader()
				.getResource("").getPath(),"testCopy.txt");
		byte[] testData = "<rdf>          </rdf>".getBytes();
		ByteArrayInputStream is = new ByteArrayInputStream(testData);
		CPathUtils.copy(is, Files.newOutputStream(f));
		is.close();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		CPathUtils.copy(Files.newInputStream(f), os);
        byte[] read = os.toByteArray();
        assertArrayEquals(testData, read);
	}

	@Test
	public void readWriteMetadata() {
		String url = "classpath:metadata.json";
		Metadata metadata = CPathUtils.readMetadata(url);
		List<Datasource> datasources = metadata.getDatasources();
		final Datasource datasource = datasources.stream()
				.filter(m -> m.getIdentifier().equals("TESTUNIPROT"))
				.findFirst().orElse(null);
		assertAll(
				() -> assertEquals(3, datasources.size()),
				() -> assertNotNull(datasource),
				() -> assertEquals(METADATA_TYPE.WAREHOUSE, datasource.getType())
		);

		Datasource ds = metadata.getDatasources().get(0);
		ds.setNumPathways(-1);
		ds.setNumInteractions(-1);
		ds.setNumPhysicalEntities(-1);
		CPathUtils.saveMetadata(metadata, url); //will replace classpath: with file:/target/
		metadata = CPathUtils.readMetadata("file:target/metadata.json");
		assertEquals(-1, metadata.getDatasources().get(0).getNumPathways());
	}

	@Test
	public void chebiOboXrefLinesPattern() {
		Pattern p = Pattern.compile(".+?:(.+?)\\s+\"(.+?)\"");
		Matcher m = p.matcher("NIST Chemistry WebBook:22325-47-9 \"CAS Registry Number\"");
		boolean matched = m.find();
		assertAll(
				() -> assertTrue(matched),
				() -> assertEquals("22325-47-9", m.group(1)), //ID
				() -> assertEquals("CAS Registry Number", m.group(2)) //DB
		);
	}

	@Test
	public void testFixSourceIdForMapping() {
		assertAll(
			() -> assertEquals("Q8TD86", CPathUtils.fixIdForMapping("uniprot", "Q8TD86-1")),
			() -> assertEquals("Q8TD86", CPathUtils.fixIdForMapping("uniprot knowledgebase", "Q8TD86-1")), //non-standard since recently, unlike 'uniprot protein'
			() -> assertEquals("Q8TD86", CPathUtils.fixIdForMapping("uniprot isoform", "Q8TD86-1")),
			() -> assertEquals("NP_619650", CPathUtils.fixIdForMapping("refseq", "NP_619650.1")),
			() -> assertEquals("CHEBI:28", CPathUtils.fixIdForMapping("ChEBI", "28")),
			() -> assertEquals("CHEBI:28", CPathUtils.fixIdForMapping("chEbi", "chebi:28"))
		);
	}

	@Test
	public void generateFileNames() {
		assertAll(() -> assertEquals("foo.normalized.gz", CPathUtils.normalizedFile("foo.some.gz")),
				() -> assertEquals("/foo/bar.normalized.gz", CPathUtils.normalizedFile("/foo/bar.some.gz")),
				() -> assertEquals("./foo/bar.normalized.gz", CPathUtils.normalizedFile("./foo/bar.some.gz")),
				() -> assertEquals("foo.cleaned.gz", CPathUtils.cleanedFile("foo.some.gz")),
				() -> assertEquals("/foo/bar.cleaned.gz", CPathUtils.cleanedFile("/foo/bar.some.gz")),
				() -> assertEquals("./foo/bar.cleaned.gz", CPathUtils.cleanedFile("./foo/bar.some.gz")),
				() -> assertEquals("foo.converted.gz", CPathUtils.convertedFile("foo.some.gz")),
				() -> assertEquals("/foo/bar.converted.gz", CPathUtils.convertedFile("/foo/bar.some.gz")),
				() -> assertEquals("./foo/bar.converted.gz", CPathUtils.convertedFile("./foo/bar.some.gz")),
				() -> assertEquals("foo.issues.gz", CPathUtils.validationFile("foo.normalized.gz")),
				() -> assertEquals("/foo/bar.issues.gz", CPathUtils.validationFile("/foo/bar.normalized.gz")),
				() -> assertEquals("./foo/bar.issues.gz", CPathUtils.validationFile("./foo/bar.normalized.gz"))
		);
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			bioregistry.io/chebi:1, CHEBI:1
			http://bioregistry.io/uniprot:A, A
			https://bioregistry.io/uniprot:A, A
			http://identifiers.org/chebi/CHEBI:1, CHEBI:1
			identifiers.org/chebi/CHEBI:1, CHEBI:1
			identifiers.org/uniprot/A, A
			identifiers.org/pubchem:1, CID:1
			bioregistry.io/pubchem.substance:1, SID:1
			bioregistry.io/pubchem.compound:1, CID:1
			a.foo/bar,
			"""
	) //the last (expected value) above is: null
	void idFromNormalizedUri(String uri, String expected) {
			assertEquals(expected, CPathUtils.idFromNormalizedUri(uri));
	}

	@ParameterizedTest
	@CsvSource(textBlock = """
			bioregistry.io/chebi:1,,,bioregistry.io/chebi:1
			bioregistry.io/chebi:1, foo, bar, bioregistry.io/chebi:1
			identifiers.org/chebi:1,foo,bar,identifiers.org/chebi:1
			bioregistry.io/chebi:1,,bar,bioregistry.io/chebi:1
			bioregistry.io/chebi:1,foo,,bioregistry.io/chebi:1
			http://bioregistry.io/chebi:1, foo, foo, http://bioregistry.io/chebi:1
			uniprot:1, foo, bar:, uniprot:1
			uniprot:1, , , uniprot:1
			uniprot:1, , bar:, uniprot:1
			a.foo/bar,,,bar
			https://a.foo/bar1,https://a.foo/,xyz:,xyz:bar1
			http://a.foo#bar1,http://a.foo#,xyz:,xyz:bar1
			ftp://a.foo#bar1, foo, xyz:, ftp://a.foo#bar1
			ftp://a.foo#bar1, , xyz:, xyz:bar1
			foo#123, foo#, bar:, bar:123
			foo#123, whatever/, bar:, foo#123
			http://smpdb.ca/pathways/#a1,,foo:,foo:a1
			foo:123, foo:, bar:, bar:123
			foo:123,, bar:, foo:123
			http://smpdb.ca/pathways/#A,,foo:,foo:A
			http://smpdb.ca/pathways/#A,,foo:,foo:A
			http://smpdb.ca/pathways/#A,,foo:,foo:A
			http://smpdb.ca/pathways/#A,http://smpdb.ca/pathways/#,foo:,foo:A
			http://identifiers.org/smpdb/A,http://identifiers.org/smpdb/,foo:,foo:A
			ctdbase:complex_3048803,,ctd:,ctdbase:complex_3048803
			"""
	)
	void rebaseUri(String uri, String obase, String nbase, String expected) {
		assertEquals(expected, CPathUtils.rebaseUri(uri, obase, nbase));
	}
}