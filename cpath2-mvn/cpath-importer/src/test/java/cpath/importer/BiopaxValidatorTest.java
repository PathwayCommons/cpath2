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

package cpath.importer;

import static org.junit.Assert.*;

import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Catalysis;
import org.biopax.paxtools.model.level3.ControlType;
import org.biopax.paxtools.model.level3.Level3Factory;
import org.biopax.paxtools.model.level3.TemplateReactionRegulation;
import org.biopax.validator.Behavior;
import org.biopax.validator.Rule;
import org.biopax.validator.result.Validation;
import org.biopax.validator.rules.ControlTypeRule;
import org.biopax.validator.utils.BiopaxValidatorException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author rodche
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"classpath:cpath/importer/validator-test-context.xml", 
		"classpath:validator-context.xml"
	})
public class BiopaxValidatorTest {

	@Autowired
	BiopaxValidator validator;
	
	Level3Factory level3 = (Level3Factory) BioPAXLevel.L3.getDefaultFactory();
	
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * Test method for {@link cpath.importer.BiopaxValidator#validate(org.biopax.paxtools.model.Model)}.
	 */
	@Test
	public final void testValidate() {
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
		
		// write the example XML
		Model m = level3.createModel();
		ca.setControlType(ControlType.INHIBITION); // set wrong
		tr.setControlType(ControlType.ACTIVATION_ALLOSTERIC); // set bad
		m.add(ca);
		m.add(tr);
		
		
		rule.setBehavior(Behavior.ERROR);
		Validation result = validator.validate(m);

	}

}
