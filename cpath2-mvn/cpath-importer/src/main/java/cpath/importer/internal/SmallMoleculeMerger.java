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

package cpath.importer.internal;

// imports
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;

import java.util.Set;

/**
 * Utility class to merge a set of SmallMolecule properties into a single SmallMolecule reference.
 * A majority of this code is taken from org.biopax.paxtools.controller.SimpleMerger.
 */
public class SmallMoleculeMerger {

	private final EditorMap map;

	/**
	 * Constructor.
	 *
	 * @param map EditorMap
	 */
	public SmallMoleculeMerger(EditorMap map) {
		this.map = map;
	}

	/**
	 * This method merges properties contained in the set of smrs into
	 * the inchiSMR.  inchiSMR is modified and return via reference
	 *
	 * @param inchiSMR SmallMoleculeReference
	 * @param smrs Set<SmallMoleculeReference>
	 */
	public void merge(SmallMoleculeReference inchiSMR, Set<SmallMoleculeReference> smrs) {

		// create model and place inchiSMR inside it
		Model inchiModel= BioPAXLevel.L3.getDefaultFactory().createModel();
		inchiModel.add(inchiSMR);

		// currently we do not worry about which
		// SmallMoleculeReference in smrs gets merged last
		for (SmallMoleculeReference smr : smrs) {
			Model smrModel= BioPAXLevel.L3.getDefaultFactory().createModel();
			smrModel.add(smr);
			merge(inchiModel, smrModel);
		}
	}

    /**
	 * Merge method (taken from SimpleMerger).
	 *
	 * @param target Model
	 * @param source Model
	 */
	private void merge(Model target, Model source) {

		Set<BioPAXElement> sourceElements = source.getObjects();
		for (BioPAXElement bpe : sourceElements) {
			BioPAXElement paxElement = target.getByID(bpe.getRDFId());
			if (paxElement == null) {
				target.add(bpe);
			}
		}
		for (BioPAXElement bpe : sourceElements) {
			updateObjectFields(bpe, target);
		}
	}

	/**
	 * Updates each value of <em>existing</em> element, using the value(s) of <em>update</em>.
	 *
	 * Note: This method is taken from SimpleMerge with minor mods - all property editors are used.
	 *
	 * @param update BioPAX element of which values are ued for update
	 */
	private void updateObjectFields(BioPAXElement update, Model target) {

		Set<PropertyEditor> editors =
				map.getEditorsOf(update);
		for (PropertyEditor editor : editors) {
			if (editor.isMultipleCardinality()) {
				Set<BioPAXElement> values = (Set<BioPAXElement>) editor.getValueFromBean(update);
				for (BioPAXElement value : values) {
					migrateToTarget(update, target, editor, value);
				}
			}
			else {
				BioPAXElement value = (BioPAXElement) editor.getValueFromBean(update);
				migrateToTarget(update, target, editor, value);
			}
		}
	}
	
	private void migrateToTarget(BioPAXElement update, Model target, 
			PropertyEditor editor, BioPAXElement value) {

		if (value!=null && !target.contains(value)) {
			BioPAXElement newValue = target.getByID(value.getRDFId());
			editor.removePropertyFromBean(value,update);
			editor.setPropertyToBean(update, newValue);
		}
	}
}