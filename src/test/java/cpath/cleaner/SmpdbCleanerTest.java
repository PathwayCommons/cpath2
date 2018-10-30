package cpath.cleaner;

import cpath.service.api.Cleaner;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Pathway;
import org.biopax.paxtools.model.level3.PathwayStep;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;

public class SmpdbCleanerTest {

	@Test
	public final void testClean() throws IOException {	
		Cleaner cleaner = new SmpdbCleaner();
		final String uri40 = "http://identifiers.org/smpdb/SMP00040";

		String f40 = getClass().getClassLoader().getResource("").getPath() + File.separator
				+ "testCleanPW000146.owl";
		cleaner.clean(new FileInputStream(getClass().getResource("/PW000146.owl").getFile()),
				new FileOutputStream(f40));
		Model m40 = new SimpleIOHandler().convertFromOWL(new FileInputStream(f40));
		assertTrue(m40.containsID(uri40));
		Pathway p40 = (Pathway)m40.getByID(uri40);
		assertEquals("Glycolysis", p40.getDisplayName());

		final String uri57 = "http://identifiers.org/smpdb/SMP00057";
		String f57 = getClass().getClassLoader().getResource("").getPath() + File.separator
				+ "testCleanPW000005.owl";
		cleaner.clean(new FileInputStream(getClass().getResource("/PW000005.owl").getFile()),
				new FileOutputStream(f57));
		Model m57 = new SimpleIOHandler().convertFromOWL(new FileInputStream(f57));

		//Using SimpleMerger with Filter makes merging by URI work properly (regardless order of sub-models)-
		SimpleMerger merger = new SimpleMerger(SimpleEditorMap.L3, (o)-> o instanceof Pathway);
		Model model = BioPAXLevel.L3.getDefaultFactory().createModel();
		merger.merge(model, m57);
		merger.merge(model, m40);
		assertTrue(model.containsID(uri40));
		assertTrue(model.containsID(uri57));
		new SimpleIOHandler().convertToOWL(model, new FileOutputStream(
				getClass().getClassLoader().getResource("").getPath() 
					+ File.separator + "testCleanSmpdbMergeOK.owl"));
		
		Pathway pw = (Pathway) model.getByID(uri40);
		assertEquals(37, pw.getPathwayComponent().size());
		assertTrue(pw.getPathwayOrder().isEmpty());
		assertEquals(2, model.getObjects(Pathway.class).size());
		assertTrue(model.getObjects(PathwayStep.class).isEmpty());
	}

}
