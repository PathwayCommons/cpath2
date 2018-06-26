package cpath.service;

import static org.junit.Assert.*;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.validator.api.CvRule;
import org.biopax.validator.api.Rule;
import org.biopax.validator.api.Validator;
import org.biopax.validator.api.beans.Validation;
import org.biopax.validator.impl.IdentifierImpl;
import org.biopax.paxtools.normalizer.Normalizer;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;


@Ignore //TODO: enable and run tests after updating biopax-validator version

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {BiopaxConfiguration.class})
@ActiveProfiles({"premerge"})
public class BiopaxValidatorTest {

	@Autowired
	ApplicationContext context;

	final BioPAXFactory level3 = BioPAXLevel.L3.getDefaultFactory();

	/*
	 * This tests that the BioPAX Validator framework
	 * is properly configured and usable in the current context.
	 */
	@Test //controlType
	public void testValidateModel() {
		Catalysis ca = level3.create(Catalysis.class, "catalysis1");
		ca.setControlType(ControlType.INHIBITION);
		ca.addComment("error: illegal controlType");
		TemplateReactionRegulation tr = level3.create(TemplateReactionRegulation.class, "regulation1");
		tr.setControlType(ControlType.ACTIVATION_ALLOSTERIC);
		tr.addComment("error: illegal controlType");
		Model m = level3.createModel();
		m.add(ca);
		m.add(tr);

		Validation v = new Validation(new IdentifierImpl());//, "", true, null, 0, null);// do auto-fix
		v.setModel(m);
		Validator validator = context.getBean(Validator.class);
		validator.validate(v);
		validator.getResults().remove(v);
		System.out.println(v.getError());
		assertEquals(2, v.countErrors(null, null, "range.violated", null, false, false));
	}

	/*
	 * Checks DB names and synonyms were loaded there -
	 */
	@Test
	public void testXrefRuleEntezGene() {
		Rule rule = (Rule) context.getBean("xrefRule");
		UnificationXref x = level3.create(UnificationXref.class, "1");
		x.setDb("EntrezGene"); //but official preferred name is: "NCBI Gene"
		x.setId("0000000");
		Validation v = new Validation(new IdentifierImpl());
		rule.check(v, x);
		assertTrue(v.getError().isEmpty()); //no error
	}

	@Test
	public void testProteinModificationFeatureCvRule() {
		CvRule rule = (CvRule) context.getBean("proteinModificationFeatureCvRule");
		//System.out.print("proteinModificationFeatureCvRule valid terms are: " + rule.getValidTerms().toString());
		assertTrue(rule.getValidTerms().contains("(2S,3R)-3-hydroxyaspartic acid".toLowerCase()));
		SequenceModificationVocabulary cv = level3.create(SequenceModificationVocabulary.class, "MOD_00036");
		cv.addTerm("(2S,3R)-3-hydroxyaspartic acid");
		ModificationFeature mf = level3.create(ModificationFeature.class, "MF_MOD_00036");
		mf.setModificationType(cv);
		Validation v = new Validation(new IdentifierImpl(), "", true, null, 0, null); // auto-fix=true - fixex "no xref" error
		rule.check(v, mf);

		assertEquals(0, v.countErrors(mf.getUri(), null, "illegal.cv.term", null, false, false));
		assertEquals(1, v.countErrors(mf.getUri(), null, "no.xref.cv.terms", null, false, false)); //- one but fixed though -
		assertEquals(0, v.countErrors(null, null, null, null, false, true)); //- no unfixed errors
	}


	@Test
	public void testNormalizeTestFile() {
		SimpleIOHandler simpleReader = new SimpleIOHandler();
		simpleReader.mergeDuplicates(true);

		Normalizer normalizer = new Normalizer();
		String base = "test/";
		normalizer.setXmlBase(base);

		Model m = simpleReader.convertFromOWL(getClass().getResourceAsStream("/biopax-level3-test.owl"));
		normalizer.normalize(m);

		/*
		 * Normalizer, if used alone (without Validator), does not turn DB or ID values to upper case
		 * when generating a new xref URI anymore... (that was actually a bad idea);
		 * "c00022", by the way, is illegal identifier (- C00022 is a valid KEGG id),
		 * which wouldn't pass the Premerger (import pipeline) stage without critical errors...
		 * BioPAX Normalizer alone cannot fix such IDs, because there are non-trivial cases,
		 * where we cannot simply convert the first symbol to upper case...;
		 * More importantly, bio identifiers are normally case sensitive.
		 */
		assertTrue(m.containsID(Normalizer.uri(base, "kegg compound", "c00002", UnificationXref.class)));
		assertTrue(m.containsID(Normalizer.uri(base, "kegg compound", "C00002", UnificationXref.class)));
		m = null;

		// However, using the validator (with autofix=true) and then - normalizer (as it's done in Premerger) together
		// will, in fact, fix and merge these two xrefs
		m = simpleReader.convertFromOWL(getClass().getResourceAsStream("/biopax-level3-test.owl"));
		Validation v = new Validation(new IdentifierImpl(), null, true, null, 0, null);
		v.setModel(m);
		m.setXmlBase(base);
		Validator validator = context.getBean(Validator.class);
		validator.validate(v);
		validator.getResults().remove(v);
		m = (Model) v.getModel();
		normalizer.normalize(m);

		assertFalse(m.containsID(Normalizer.uri(base, "KEGG COMPOUND", "c00002", UnificationXref.class)));
		assertTrue(m.containsID(Normalizer.uri(base, "KEGG COMPOUND", "C00002", UnificationXref.class)));
	}
}
