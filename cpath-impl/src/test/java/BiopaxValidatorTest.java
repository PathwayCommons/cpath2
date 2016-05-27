/**
 ** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** both UofT and MSKCC have no obligations to provide maintenance, 
 ** support, updates, enhancements or modifications.  In no event shall
 ** UofT or MSKCC be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** UofT or MSKCC have been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this software; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA;
 ** or find it at http://www.fsf.org/ or http://www.gnu.org.
 **/


import static org.junit.Assert.*;

import java.io.IOException;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Catalysis;
import org.biopax.paxtools.model.level3.ControlType;
import org.biopax.paxtools.model.level3.ModificationFeature;
import org.biopax.paxtools.model.level3.SequenceModificationVocabulary;
import org.biopax.paxtools.model.level3.TemplateReactionRegulation;
import org.biopax.paxtools.model.level3.UnificationXref;
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
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@Ignore //disabled to build faster - TODO enable tests after switching to a new validator version

/**
 * Extra integration tests for the BioPAX Validator 
 * (- is a CV repository) and Normalizer used by cPath2 importer. 
 * 
 * @author rodche
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"classpath:META-INF/spring/applicationContext-validator.xml"
	})
public class BiopaxValidatorTest {

	@Autowired
	Validator validator;
	
	@Autowired
	ApplicationContext context;
	
	BioPAXFactory level3 = BioPAXLevel.L3.getDefaultFactory();

	
	/*
	 * This tests that the BioPAX Validator framework
	 * is properly configured and usable in the
	 * current context.
	 * 
	 */
	@Test //controlType
	public final void testValidateModel() {		
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
	public final void testNormalizeTestFile() throws IOException {
		SimpleIOHandler simpleReader = new SimpleIOHandler();
		simpleReader.mergeDuplicates(true);
		
		Normalizer normalizer = new Normalizer();
		String base = "test/";
		normalizer.setXmlBase(base);
		
		Model m = simpleReader.convertFromOWL(getClass().getResourceAsStream("/biopax-level3-test.owl"));
		normalizer.normalize(m);

		/* Following assertions were changed since using the biopax-validator v3 (2012/11/14).
		 * Normalizer there does not turn DB or ID values to upper case when generating a new xref URI anymore..." (that was actually a bad idea)
		 * "c00022", by the way, is technically an illegal KEGG identifier (must be C00022), - 
		 * and it won't pass our Premerger (import pipeline) stage without the critical error being reported.
		 * Unfortunately, the normalizer alone cannot always fix such issues (it just generates consistent xref URIs), 
		 * because there are less trivial cases than where one could simply convert the first symbol to upper case...;
		 * and, more important, xref.id value capitalization does usually matter...
		 */
		assertTrue(m.containsID(Normalizer.uri(base, "KEGG COMPOUND", "c00022", UnificationXref.class)));
		assertTrue(m.containsID(Normalizer.uri(base, "KEGG COMPOUND", "C00022", UnificationXref.class)));
		m = null;
		
		// However, using the validator (with autofix=true) and then - normalizer (as it's done in Premerger) together
		// will, in fact, fix and merge these two xrefs
		m = simpleReader.convertFromOWL(getClass().getResourceAsStream("/biopax-level3-test.owl"));
		Validation v = new Validation(new IdentifierImpl(), null, true, null, 0, null);
		v.setModel(m);
		validator.validate(v);
		validator.getResults().remove(v);
		m = (Model)v.getModel();
		normalizer.normalize(m);

		assertFalse(m.containsID(Normalizer.uri(base, "KEGG COMPOUND", "c00022", UnificationXref.class)));
		assertTrue(m.containsID(Normalizer.uri(base, "KEGG COMPOUND", "C00022", UnificationXref.class)));
	}
}
