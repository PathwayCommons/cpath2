package cpath.cleaner.internal;

import static org.junit.Assert.*;

import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.junit.Test;

public class BaseCleanerImplTest {

	@Test
	public final void testReplaceID() {
		BaseCleanerImpl cleaner = new BaseCleanerImpl() {
			@Override
			public String clean(String pathwayData) {
				return null;
			}};
		
		Model m = BioPAXLevel.L3.getDefaultFactory().createModel();
		UnificationXref xref = m.addNew(UnificationXref.class, "one");
		cleaner.replaceID(m, xref, "two");
		
		assertTrue(xref.getRDFId().equals("two"));
		assertTrue(m.containsID("two"));
	}

}
