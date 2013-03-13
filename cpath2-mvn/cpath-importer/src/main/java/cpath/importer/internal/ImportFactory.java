package cpath.importer.internal;

import java.lang.reflect.Constructor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.validator.api.Validator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cpath.dao.PaxtoolsDAO;
import cpath.importer.Cleaner;
import cpath.importer.Converter;
import cpath.importer.Fetcher;
import cpath.importer.Merger;
import cpath.importer.Premerger;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.WarehouseDAO;

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
	 * identifier.
	 * 
	 * @param target main DB
	 * @param provider metadata identifier for the pathway data provider
	 * @param force when 'true', it will try to merge despite the cpath2 BioPAX validator 
	 * (in 'premerge') reported errors; otherwise ('false') - skip merging such data.
	 * @return
	 */
	public static Merger newMerger(
			final PaxtoolsDAO target, final String provider, final boolean force) 
	{
		
		ApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:applicationContext-Metadata.xml");
		MetadataDAO mdao = (MetadataDAO)ctx.getBean("metadataDAO");
		
		ctx = new ClassPathXmlApplicationContext("classpath:applicationContext-Warehouse.xml");
		WarehouseDAO wdao = (WarehouseDAO)ctx.getBean("warehouseDAO");
		
		MergerImpl merger = new MergerImpl(target, mdao, wdao);
		merger.setPathwayData(provider);
		merger.setForce(force);
		
		return merger;
	}
	
	
	/**
	 * Creates and configures a new cpath2 merger,
	 * which can merge all pre-merged pathway data
	 * (all providers available)
	 * 
	 * @see ImportFactory#newMerger(PaxtoolsDAO, String, boolean)
	 */
	public static Merger newMerger(PaxtoolsDAO target, boolean force) {
		return newMerger(target, null, force);
	}
	
	
	/**
	 * Creates and configures a new cpath2 "premerger"
	 * to read/clean/convert/validate the pathway data 
	 * with given metadata identifier.
	 * 
	 * @param metadataDAO
	 * @param warehouseDAO
	 * @param biopaxValidator
	 * @param metadataIdentifier pathway data provider identifier (from the metadata conf.)
	 * @return
	 */
	public static Premerger newPremerger(final MetadataDAO metadataDAO, PaxtoolsDAO warehouseDAO, 
			final Validator biopaxValidator, final String metadataIdentifier) {
		
		PremergeImpl premerge = new PremergeImpl(metadataDAO, warehouseDAO, biopaxValidator);		
		premerge.setIdentifier(metadataIdentifier);
		
		return premerge;
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
