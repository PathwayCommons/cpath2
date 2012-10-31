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

import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.validator.result.*;
import org.biopax.validator.*;
import org.biopax.validator.rules.ControlTypeRule;
import org.biopax.validator.utils.BiopaxValidatorException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * Additional integration tests for the BioPAX Validator 
 * (it's also a CV repository) configuration used by cPath2 importer. 
 * 
 * By default, this test class is now disabled/ignored to speed 
 * up builds, and because the validator was tested separately, 
 * and also due to some tests depend on the validator version 
 * (and default settings) and the actual validation.properties 
 * file (in the current CPATH2_HOME dir)! 
 * 
 * So, if required, comment out the @Ignore annotation to run tests!

 * @author rodche
 *
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"classpath:applicationContext-biopaxValidation.xml",
		"classpath:applicationContext-cvRepository.xml"
	})
public class BiopaxValidatorTest {

	@Autowired
	Validator validator;
	
	@Autowired
	ApplicationContext context;
	
	BioPAXFactory level3 = BioPAXLevel.L3.getDefaultFactory();

	/**
	 * Test method for {@link cpath.validator.BiopaxValidator#validate(org.biopax.paxtools.model.Model)}.
	 */
	@Test
	public final void testCheckRule() {
		Rule rule = new ControlTypeRule();	
		Catalysis ca = level3.create(Catalysis.class, "catalysis1");
		rule.check(ca, false); // controlType==null, no error expected
		ca.setControlType(ControlType.ACTIVATION);
		rule.check(ca, false); // no error expected
		ca.setControlType(ControlType.INHIBITION);
		ca.addComment("error: illegal controlType");
		try {
			rule.check(ca, false); 
			fail("must throw BiopaxValidatorException");
		} catch(BiopaxValidatorException e) {
		}
		
		TemplateReactionRegulation tr = level3
			.create(TemplateReactionRegulation.class, "regulation1");
		tr.setControlType(ControlType.INHIBITION);
		rule.check(tr, false); // no error...
		
		tr.setControlType(ControlType.ACTIVATION_ALLOSTERIC);
		tr.addComment("error: illegal controlType");
		try {
			rule.check(tr, false); 
			fail("must throw BiopaxValidatorException");
		} catch(BiopaxValidatorException e) {
		}
	}

	@Test //controlType
	public final void testValidateModel() {		
		Catalysis ca = level3.create(Catalysis.class, "catalysis1"); 
		ca.setControlType(ControlType.INHIBITION);
		ca.addComment("error: illegal controlType");	
		TemplateReactionRegulation tr = level3
			.create(TemplateReactionRegulation.class, "regulation1");
		tr.setControlType(ControlType.ACTIVATION_ALLOSTERIC);
		tr.addComment("error: illegal controlType");
		Model m = level3.createModel();
		m.add(ca);
		m.add(tr);
		
		Validation result = new Validation("test");
		result.setModel(m);
		validator.validate(result);
		
		assertNotNull(result);
		ErrorType error = result.findErrorType("range.violated", Behavior.WARNING);
		assertNotNull(error);
		assertEquals(2, error.getErrorCase().size());
		for(ErrorCaseType errorCase : error.getErrorCase()) {
			assertNotNull(errorCase.getObject());
			System.out.println(errorCase);
		}

	}
	
    /*
     * Special case - check synonyms are there
     */
    @Test
    public void testXrefRuleEntezGene() {
    	Rule rule = null;
		for(Rule r : validator.getRules()) {
			if(r.getName().equals("xrefRule")) {
				rule = r;
				break;
			}
		}
		rule.setBehavior(Behavior.WARNING); 
        
        UnificationXref x = level3.create(UnificationXref.class, "1");
        x.setDb("EntrezGene");
        x.setId("0000000");
        rule.check(x, false);
    }
 
    
    @Test
	public void testProteinModificationFeatureCvRule() {
    	CvRule rule = (CvRule) context.getBean("proteinModificationFeatureCvRule");
    	//System.out.print("proteinModificationFeatureCvRule valid terms are: " 
    			//+ rule.getValidTerms().toString());
    	assertTrue(rule.getValidTerms().contains("(2S,3R)-3-hydroxyaspartic acid".toLowerCase()));
    	
    	SequenceModificationVocabulary cv = level3
    		.create(SequenceModificationVocabulary.class, "MOD_00036");
    	cv.addTerm("(2S,3R)-3-hydroxyaspartic acid");
    	ModificationFeature mf = level3.create(ModificationFeature.class, "MF_MOD_00036");
    	mf.setModificationType(cv);
   		rule.check(mf, false); // should not fail
	}
}
