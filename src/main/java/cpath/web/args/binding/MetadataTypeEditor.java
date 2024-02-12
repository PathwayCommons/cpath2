package cpath.web.args.binding;

import java.beans.PropertyEditorSupport;

import cpath.service.metadata.Datasource.METADATA_TYPE;


public class MetadataTypeEditor extends PropertyEditorSupport {
	
	@Override
	public void setAsText(String arg0) {
		METADATA_TYPE value = METADATA_TYPE.valueOf(arg0.trim().toUpperCase());
		setValue(value);
	}
	
}
