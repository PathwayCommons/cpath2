package cpath.converter;

import cpath.service.api.Converter;


/**
 * General implementation of Converter interface.
 */
abstract class BaseConverter implements Converter {
	
	protected String xmlBase;
	
	@Override
	public void setXmlBase(String xmlBase) {
		this.xmlBase = xmlBase;
	}

}
