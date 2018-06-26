package cpath.service.web.args.binding;

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
		LimitType value = null;
		value = LimitType.valueOf(arg0.trim().toUpperCase());
		setValue(value);
	}
}