package cpath.importer.internal;

import java.lang.reflect.Constructor;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.model.Model;
import org.biopax.validator.Validator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cpath.config.CPathSettings;
import cpath.dao.DataServices;
import cpath.dao.PaxtoolsDAO;
import cpath.dao.internal.DataServicesFactoryBean;
import cpath.importer.Cleaner;
import cpath.importer.Converter;
import cpath.importer.Fetcher;
import cpath.importer.Merger;
import cpath.importer.Premerge;
import cpath.warehouse.MetadataDAO;

/**
 * @author rodche
 *
 */
public final class ImportFactory {
	private static Log log = LogFactory.getLog(ImportFactory.class);
	
	private ImportFactory() {
		// when called via reflection -
		throw new AssertionError("Non-instantiable static factory class!");
	}
	
	public static Fetcher newFetcher() {
		return new FetcherImpl();
	}
	
	public static Merger newMerger(Model model) {
		return new MergerImpl(model);
	}
	
	public static Premerge newPremerge(MetadataDAO metaDAO, Validator validator) {
		return new PremergeImpl(metaDAO, validator);
	}
	
	/**
	 * Creates new PaxtoolsDAO instance to work with existing "premerge"
	 * database. This is used both during the "pre-merge" (here) and "merge".
	 */
	public static PaxtoolsDAO buildPremergeDAO(String premergeDbName) {
		/* 
		 * set system properties and data source 
		 * (replaces existing one in the same thread),
		 * load another specific application context
		 */
		String home = CPathSettings.getHomeDir();
		if (home==null) {
			throw new RuntimeException(
				"Please set " + CPathSettings.HOME_VARIABLE_NAME + " environment variable " +
            	" (point to a directory where cpath.properties, etc. files are placed)");
		}
		
		// get the data source factory bean (aware of the driver, user, and password)
		ApplicationContext context = 
			new ClassPathXmlApplicationContext("classpath:internalContext-dsFactory.xml");
		DataServices dataServices = (DataServices) context.getBean("&dsBean");
		DataSource premergeDataSource = dataServices.getDataSource(premergeDbName);
		// "premergeDataSource" map key matches the dataSource bean name in the internalContext-premerge.xml
		DataServicesFactoryBean.getDataSourceMap().put("premergeDataSource", premergeDataSource);
		// get the premerge DAO
		context = new ClassPathXmlApplicationContext("classpath:internalContext-premerge.xml");	
		return (PaxtoolsDAO)context.getBean("premergePaxtoolsDAO");
	}
	
	/**
	 * For the given converter class name,
	 * returns an instance of a class which
	 * implements the converter interface.
	 *
	 * @param converterClassName String
	 * @return Converter
	 */
	public static Converter newConverter(final String converterClassName) {
		return (Converter) newInstance(converterClassName);
	}
		
	/**
	 * For the given cleaner class name,
	 * returns an instance of a class which
	 * implements the cleaner interface.
	 *
	 * @param converterClassName String
	 * @return Converter
	 */
	public static Cleaner newCleaner(final String cleanerClassName) {
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
			log.fatal(("Failed to instantiate " + className), e) ;
		}
		
		return null;
	}
}
