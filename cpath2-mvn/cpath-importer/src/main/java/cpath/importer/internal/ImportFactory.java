package cpath.importer.internal;

import java.lang.reflect.Constructor;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.validator.api.Validator;
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
 * Public static factory to 
 * instantiate complex (and not public) objects 
 * that participate in or control the cpath2 
 * import data process.
 * 
 * @author rodche
 *
 */
public final class ImportFactory {
	private static Log log = LogFactory.getLog(ImportFactory.class);
	
	private ImportFactory() {
		// when called via reflection -
		throw new AssertionError("Non-instantiable static factory class!");
	}
	
	
	/**
	 * Creates a new Fetcher instance with default options.
	 * 
	 * @return
	 */
	public static Fetcher newFetcher() {
		return new FetcherImpl();
	}
	
	/**
	 * Creates a new Fetcher instance.
	 * 
	 * @see FetcherImpl#isReUseFetchedDataFiles()
	 * 
	 * @param reUseFetchedDataFiles
	 * @return
	 */
	public static Fetcher newFetcher(boolean reUseFetchedDataFiles) {
		FetcherImpl fetcher =  new FetcherImpl();
		fetcher.setReUseFetchedDataFiles(reUseFetchedDataFiles);
		return fetcher;
	}
	
	/**
	 * Creates and configures a new cpath2 merger
	 * for merging a pathway data with given metadata 
	 * identifier and version.
	 * 
	 * @param target main DB
	 * @param provider metadata identifier for the pathway data provider
	 * @param version metadata version
	 * @param force when 'true', it will try to merge despite the cpath2 BioPAX validator 
	 * (in 'premerge') reported errors; otherwise ('false') - skip merging such data.
	 * @return
	 */
	public static Merger newMerger(final PaxtoolsDAO target, final String provider, 
			final String version, final boolean force) {
		
		MergerImpl merger = new MergerImpl(target);
		merger.setPathwayData(provider, version);
		merger.setForce(force);
		
		return merger;
	}
	
	
	/**
	 * Creates and configures a new cpath2 merger,
	 * which can merge all pre-merged pathway data
	 * (all providers, versions available)
	 * 
	 * @see ImportFactory#newMerger(PaxtoolsDAO, String, String, boolean)
	 */
	public static Merger newMerger(PaxtoolsDAO target, boolean force) {
		return newMerger(target, null, null, force);
	}
	
	
	/**
	 * Creates and configures a new cpath2 "premerger"
	 * to read/clean/convert/validate the pathway data 
	 * with given metadata identifier and version.
	 * 
	 * @param metadataDAO
	 * @param warehouseDAO
	 * @param biopaxValidator
	 * @param createPremergeDatabases optionally (when 'true'), it persists the result 
	 * data in separate auto-generated "premerge" cpath2 Dbs instead (when 'false') in the pathwayData table.
	 * @param metadataIdentifier pathway data provider identifier (from the metadata conf.)
	 * @return
	 */
	public static Premerge newPremerge(final MetadataDAO metadataDAO, PaxtoolsDAO warehouseDAO, 
			final Validator biopaxValidator, final boolean createPremergeDatabases, final String metadataIdentifier) {
		
		PremergeImpl premerge = new PremergeImpl(metadataDAO, warehouseDAO, biopaxValidator);		
		premerge.setCreateDb(createPremergeDatabases);
		premerge.setIdentifier(metadataIdentifier);
		
		return premerge;
	}
	
	/**
	 * Creates new PaxtoolsDAO instance to work with an existing cpath2
	 * database. This is used both during the "premerge" and "merge" stages,
	 * in in export data utilities.
	 * 
	 * @param cPath2DbName
	 */
	public static PaxtoolsDAO buildPaxtoolsHibernateDAO(String cPath2DbName) {
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
		DataSource cPath2DataSource = dataServices.getDataSource(cPath2DbName);
		// "cPath2DataSource" map key matches the dataSource bean name in the internalContext-cpathDAO.xml
		DataServicesFactoryBean.getDataSourceMap().put("cPath2DataSource", cPath2DataSource);
		// get the premerge DAO
		context = new ClassPathXmlApplicationContext("classpath:internalContext-cpathDAO.xml");	
		return (PaxtoolsDAO)context.getBean("paxtoolsHibernateDAO");
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
	 * @param converterClassName String
	 * @return Converter
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
