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

package cpath.validator;

import static org.junit.Assert.*;

import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Catalysis;
import org.biopax.paxtools.model.level3.ControlType;
import org.biopax.paxtools.model.level3.Level3Factory;
import org.biopax.paxtools.model.level3.TemplateReactionRegulation;
import org.biopax.validator.Behavior;
import org.biopax.validator.Rule;
import org.biopax.validator.result.ErrorCaseType;
import org.biopax.validator.result.ErrorType;
import org.biopax.validator.result.Validation;
import org.biopax.validator.rules.ControlTypeRule;
import org.biopax.validator.utils.BiopaxValidatorException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cpath.validator.BiopaxValidator;

/**
 * @author rodche
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"classpath:cpathValidator-test-context.xml", 
		"classpath:applicationContext-biopaxValidation.xml"
	})
public class BiopaxValidatorTest {

	@Autowired
	BiopaxValidator validator;
	
	Level3Factory level3 = (Level3Factory) BioPAXLevel.L3.getDefaultFactory();


	/**
	 * Test method for {@link cpath.validator.BiopaxValidator#validate(org.biopax.paxtools.model.Model)}.
	 */
	@Test
	public final void testCheckRule() {
		Rule rule = new ControlTypeRule();	
		Catalysis ca = level3.createCatalysis();
		ca.setRDFId("catalysis1");
		rule.check(ca); // controlType==null, no error expected
		ca.setControlType(ControlType.ACTIVATION);
		rule.check(ca); // no error expected
		ca.setControlType(ControlType.INHIBITION);
		ca.addComment("error: illegal controlType");
		try {
			rule.check(ca); 
			fail("must throw BiopaxValidatorException");
		} catch(BiopaxValidatorException e) {
		}
		
		TemplateReactionRegulation tr = level3.createTemplateReactionRegulation();
		tr.setRDFId("regulation1");
		tr.setControlType(ControlType.INHIBITION);
		rule.check(tr); // no error...
		
		tr.setControlType(ControlType.ACTIVATION_ALLOSTERIC);
		tr.addComment("error: illegal controlType");
		try {
			rule.check(tr); 
			fail("must throw BiopaxValidatorException");
		} catch(BiopaxValidatorException e) {
		}
	}

	
	@Test
	public final void testValidateModel() {
		//make sure the rule's behavior, we're interested in this test, is not 'IGNORED' 
		// (otherwise this test should fail at the last line)
		
		System.out.println("testValidateModel");
		
		Rule rule = null;
		for(Rule r : validator.getRules()) {
			if(r.getName().equals("controlTypeRule")) {
				rule = r;
				break;
			}
		}
		rule.setBehavior(Behavior.ERROR); 
		
		Catalysis ca = level3.createCatalysis();
		ca.setControlType(ControlType.INHIBITION);
		ca.addComment("error: illegal controlType");	
		ca.setRDFId("catalysis1");
		TemplateReactionRegulation tr = level3.createTemplateReactionRegulation();
		tr.setRDFId("regulation1");
		tr.setControlType(ControlType.ACTIVATION_ALLOSTERIC);
		tr.addComment("error: illegal controlType");
		Model m = level3.createModel();
		m.add(ca);
		m.add(tr);
		
		Validation result = validator.validate(m);
		assertNotNull(result);
		assertFalse(result.getError().isEmpty());
		assertTrue(result.getError().size()==1);
		ErrorType error = result.getError().iterator().next();
		assertTrue(error.getErrorCase().size()==2);
		assertEquals("range.violated",error.getCode());
		System.out.println(error);
		for(ErrorCaseType errorCase : error.getErrorCase()) {
			assertNotNull(errorCase.getObject());
			System.out.println(errorCase);
		}

	}
}
