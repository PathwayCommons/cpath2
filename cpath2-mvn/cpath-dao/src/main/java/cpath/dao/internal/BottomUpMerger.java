package cpath.dao.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.controller.AbstractTraverser;
import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.UtilityClass;

import java.util.*;


public class BottomUpMerger extends AbstractTraverser
{
	private static final Log log = LogFactory.getLog(BottomUpMerger.class);

	/**
	 * @param map a class containing the editors for the model elements to modify.
	 */
	public BottomUpMerger(EditorMap map) {
		super(map);
	}


	/**
	 * Merges the <em>source</em> model into <em>target</em> model.
	 *
	 * @param target model into which merging process will be done
	 * @param source model that is going to be merged into the <em>target</em>
	 */
	public void merge(Model target, Model source)
	{
		// reset visited elements list
		getCurrentParentsList().clear();

		Set<BioPAXElement> sourceElements = source.getObjects();
		for (BioPAXElement bpe : sourceElements) {
			
			/* skip utility classes - 
			 * they will be merged via parent elements anyway;
			 * ortherwise, it's "dangling" element
			 */
			if(bpe instanceof UtilityClass) {
				continue;
			}
			
			String rdfid = bpe.getRDFId();
			if (target.getByID(rdfid) == null) {
				// add the element after all its children added
				if(log.isDebugEnabled())
					log.debug("merging new " + 
						bpe.getModelInterface().getSimpleName()
						+ " " + rdfid);
				traverse(bpe, target);
				if(!target.containsID(rdfid)) {
					// chances are, it's just been added
					target.add(bpe);
				} else {
					if(log.isDebugEnabled())
						log.debug("Found existing " + 
								bpe.getModelInterface().getSimpleName()
								+ "element: " + rdfid);
				}
			} else {
				if(log.isDebugEnabled())
					log.debug("skip existing " + 
							bpe.getModelInterface().getSimpleName()
							+ " " + rdfid);
			}
		}
	}


	@Override
	protected void visit(Object value, BioPAXElement bpe, Model target,
			PropertyEditor editor) {
		if(value instanceof BioPAXElement )
			//of course,  && editor instanceof ObjectPropertyEditor
		{
			if(log.isDebugEnabled())
				log.debug("Traverser's path: " + 
					getCurrentParentsList().toString() +
					"; " + editor.getDomain().getSimpleName() +
					"." + editor.getProperty() + "=" + value 
					+ " (" + editor.getRange().getSimpleName()
					+")");
			
			BioPAXElement e = (BioPAXElement) value;
			String rdfid = e.getRDFId();
			/* if the element with the same RDFId 
			 * exists in the target model, use it
			 */
			BioPAXElement t = target.getByID(rdfid);
			if(t != null) {
				editor.setPropertyToBean(bpe, t);
			} else {
				//do not add to the target model before its children
				traverse(e, target);
				if(!target.containsID(rdfid)) { // - double-check
					target.add(e);
				}
			}
		}
	}

}