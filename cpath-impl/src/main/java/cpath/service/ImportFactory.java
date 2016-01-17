package cpath.service;

import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Public static factory to 
 * instantiate complex (and not public) objects 
 * that participate in or control the cpath2 
 * import data process.
 * 
 * @author rodche
 *
 */
public final class ImportFactory {
	private static Logger log = LoggerFactory.getLogger(ImportFactory.class);
	
	private ImportFactory() {
		// when called via reflection -
		throw new AssertionError("Non-instantiable static factory class");
	}

	/**
	 * For the given converter class name,
	 * returns an instance of a class which
	 * implements the converter interface.
	 *
	 * @param converterClassName String
	 * @return Converter
	 */
	public static Converter newConverter(String converterClassName) {
		Converter converter = (Converter) newInstance(converterClassName);
		return converter;
	}
		
	/**
	 * For the given cleaner class name,
	 * returns an instance of a class which
	 * implements the cleaner interface.
	 *
	 * @param cleanerClassName canonical java class name for the Cleaner implementation
	 * @return instance of the class
	 */
	public static Cleaner newCleaner(String cleanerClassName) {
		return (Cleaner) newInstance(cleanerClassName);
	}
	
	/*
	 * Reflectively creates a new instance of a cpath2 
	 * supplementary/plugin class (e.g., Cleaner or Converter).
	 * This method can create non-public classes as well.
	 */
	private static Object newInstance(final String className) {
		try {
			Class<?> clazz = Class.forName(className);
			Constructor<?> c = clazz.getDeclaredConstructor();
			c.setAccessible(true);
			return c.newInstance();
		}
		catch (Exception e) {
			log.error(("Failed to instantiate " + className), e) ;
		}
		
		return null;
	}
}
