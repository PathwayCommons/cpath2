package cpath.common.internal;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Set;

import org.junit.Test;

public class UniprotClientTest {

	@Test
	public void testGetTaxon() {
		assertEquals(Integer.valueOf(9606), UniprotClient.getTaxon("P62158"));
	}

	@Test
	public void testDoMapping() throws IOException {
		Set<String> ids = UniprotClient.doMapping(
				UniprotClient.FROM_UNIPROT, UniprotClient.REFSEQ, "P62158");
		assertFalse(ids.isEmpty());
		assertTrue(ids.contains("NP_001734.1"));
	}

	@Test
	public void testDoMapping2() throws IOException {
		Set<String> ids = UniprotClient.doMapping(
				UniprotClient.FROM_UNIPROT, UniprotClient.ENREZ_GENE, "P62158");
		assertFalse(ids.isEmpty());
		System.out.println(ids.toString());
		assertTrue(ids.contains("808"));
	}
	
	
}
