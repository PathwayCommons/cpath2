package cpath.cleaner;

import cpath.service.api.Cleaner;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Pathway;
import org.biopax.paxtools.model.level3.PathwayStep;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class PathbankCleanerTest {

	@Test
	public final void testClean() throws IOException {	
		Cleaner cleaner = new PathbankCleaner();
		String uri1 = "http://bioregistry.io/pathbank:SMP0000040"; //was "http://identifiers.org/smpdb/SMP0000040";
		String f1 = getClass().getClassLoader().getResource("").getPath() + File.separator + "PW000146.cleaned.owl";
		cleaner.clean(new FileInputStream(getClass().getResource("/PW000146.owl").getFile()), new FileOutputStream(f1));
		Model m1 = new SimpleIOHandler().convertFromOWL(new FileInputStream(f1));
		Pathway p1 = (Pathway)m1.getByID(uri1);
		String uri2 = "http://bioregistry.io/pathbank:SMP0000057"; //was "http://identifiers.org/smpdb/SMP0000057";
		String f2 = getClass().getClassLoader().getResource("").getPath() + File.separator + "PW000005.cleaned.owl";
		cleaner.clean(new FileInputStream(getClass().getResource("/PW000005.owl").getFile()), new FileOutputStream(f2));
		Model m2 = new SimpleIOHandler().convertFromOWL(new FileInputStream(f2));

		//Using SimpleMerger with Filter makes merging by URI work properly (regardless order of sub-models)
		SimpleMerger merger = new SimpleMerger(SimpleEditorMap.L3, (o)-> o instanceof Pathway);
		Model model = BioPAXLevel.L3.getDefaultFactory().createModel();
		merger.merge(model, m2);
		merger.merge(model, m1);
		Pathway pw = (Pathway) model.getByID(uri1);

		//write the merged model (debug)
		new SimpleIOHandler().convertToOWL(model, new FileOutputStream(
				getClass().getClassLoader().getResource("").getPath()
					+ File.separator + "PW000005-000146.merged.owl"));

		assertAll(
				() -> assertTrue(m1.containsID(uri1)),
				() -> assertEquals("Glycolysis", p1.getDisplayName()),
				() -> assertTrue(model.containsID(uri1)),
				() -> assertTrue(model.containsID(uri2)),
				() -> assertEquals(37, pw.getPathwayComponent().size()),
				() -> assertTrue(pw.getPathwayOrder().isEmpty()), //smpdb/pathbank use pathwayOrder, but it's useless - no nextStep at all!
				() -> assertEquals(2, model.getObjects(Pathway.class).size()),
				() -> assertTrue(model.getObjects(PathwayStep.class).isEmpty())
		);
	}
}
