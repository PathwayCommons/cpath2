package cpath.converter.internal;

import java.io.InputStream;

import cpath.importer.Converter;


/**
 * General implementation of Converter interface.
 */
abstract class BaseConverterImpl implements Converter {

	protected InputStream inputStream;
	
	protected String xmlBase;

	@Override
	public void setInputStream(InputStream is) {
		this.inputStream = is;
	}
	
	@Override
	public void setXmlBase(String xmlBase) {
		this.xmlBase = xmlBase;
	}

}
