package cpath.converter.internal;

import cpath.importer.Converter;


/**
 * General implementation of Converter interface.
 */
abstract class BaseConverterImpl implements Converter {
	
	protected String xmlBase;
	
	@Override
	public void setXmlBase(String xmlBase) {
		this.xmlBase = xmlBase;
	}

}
