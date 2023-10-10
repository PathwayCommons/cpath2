package cpath.web.args.binding;

import org.biopax.paxtools.query.algorithm.LimitType;

import java.beans.PropertyEditorSupport;


/**
 * @author ozgun
 *
 */
public class GraphQueryLimitEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String arg0)
	{
		setValue(LimitType.typeOf(arg0));
	}
}