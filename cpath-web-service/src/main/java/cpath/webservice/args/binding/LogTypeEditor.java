package cpath.webservice.args.binding;

import java.beans.PropertyEditorSupport;

import cpath.log.LogType;


/**
 * @author rodche
 *
 */
public class LogTypeEditor extends PropertyEditorSupport {
	
	@Override
	public void setAsText(String arg0) {
		Object value = null;
		value = LogType.valueOf(arg0.trim().toUpperCase());
		setValue(value);
	}
}
