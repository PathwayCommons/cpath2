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

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.validator.Rule;
import org.biopax.validator.result.Validation;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Validates a BioPAX (Paxtools) Model
 * 
 * 
 * @author rodch
 *
 */
public class BiopaxValidator {
	private static final Log log = LogFactory.getLog(BiopaxValidator.class);
	
	
    @Autowired
	private Set<Rule<?>> rules;
    

    public BiopaxValidator() {
		// TODO Auto-generated constructor stub
	}
    
	
	public Validation validate(Model model) {
			
		if (model != null) {
			if (log.isDebugEnabled()) {
				log.debug("validating model: " + model + " that has "
						+ model.getObjects().size() + " objects");
			}
			
			for (Rule rule : rules) {
				// rules can check the model or specific elements
				if (rule.canCheck(model)) {
					rule.check(model);
				} else {
					for (BioPAXElement el : model.getObjects()) {
						if (rule.canCheck(el)) {
							rule.check(el);
						}
					}
				}
			}
			
		} else {
			log.warn("Model is null");
		}
		
		return null;
		
	}
}
