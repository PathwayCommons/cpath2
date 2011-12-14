/**
 ** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** both UofT and MSKCC have no obligations to provide maintenance, 
 ** support, updates, enhancements or modifications.  In no event shall
 ** UofT or MSKCC be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** UofT or MSKCC have been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this software; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA;
 ** or find it at http://www.fsf.org/ or http://www.gnu.org.
 **/
package cpath.dao.internal;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import cpath.config.CPathSettings;
import cpath.dao.DataServices;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.impl.BioPAXElementImpl;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean;



/**
 * This is a fantastic (crazy) factory that 
 * helps create any cPath database schema, 
 * and it is also a dynamic data source factory!
 * 
 * Note: it is MySQL-specific.
 * 
 * @author rodche
 */
public class DataServicesFactoryBean implements DataServices, BeanNameAware, FactoryBean<DataSource> 
{
    private static Log log = LogFactory.getLog(DataServicesFactoryBean.class);

	// fields are set by Spring from cpath.properties
    
	private String dbUser;
	@Value("${user}")
	public void setDbUser(String dbUser) { this.dbUser = dbUser; }
	public String getDbUser() { return dbUser; }

	private String dbPassword;
	@Value("${password}")
	public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }
	public String getDbPassword() { return dbPassword; }

	private String dbDriver;
	@Value("${driver}")
	public void setDbDriver(String dbDriver) { this.dbDriver = dbDriver; }
	public String getDbDriver() { return dbDriver; }

	private String dbConnection;
	@Value("${connection}")
	public void setDbConnection(String dbConnection) { this.dbConnection = dbConnection; }
	public String getDbConnection() { return dbConnection; }

	private String metaDb;
	@Value("${metadata.db}")
	public void setMetaDb(String db) {this.metaDb = db;}
	public String getMetaDb() {return metaDb;}


	private String mainDb;
	@Value("${main.db}")
	public void setMainDb(String db) {this.mainDb = db;}
	public String getMainDb() {return mainDb;}


	private String proteinsDb;
	@Value("${proteins.db}")
	public void setProteinsDb(String db) {this.proteinsDb = db;}
	public String getProteinsDb() {return proteinsDb;}

	private String moleculesDb;
	@Value("${molecules.db}")
	public void setMoleculesDb(String db) {this.moleculesDb = db;}
	public String getMoleculesDb() {return moleculesDb;}

	
	private JdbcTemplate jdbcTemplate;
	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

    private String beanName;
    
    /**
     * This sort of map allows for a DataSource dynamically 
     * created at runtime (e.g., by #{@link cpath.importer.internal.PremergeImpl}
     * persistPathway method) to be associated with a key (e.g.,"myId") 
     * and then used by any internal spring context within the same thread 
     * loaded from a xml configuration that contains the following -
     * <bean id="myId" class="cpath.dao.internal.DataServicesFactoryBean"/>
     */	
    private static ThreadLocal<Map<String, DataSource>> beansByName =
        new ThreadLocal<Map<String, DataSource>>() {
            @Override
            protected Map<String, DataSource> initialValue() {
                return new HashMap<String, DataSource>(1);
            }
        };
    
    public static Map<String, DataSource> getDataSourceMap() {
    	return beansByName.get();
    }
        

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    
    @Override
    public DataSource getObject() {
    	return getDataSource(beanName);
    }

    
    @Override
    public Class<?> getObjectType() {
        return getObject().getClass();
    }

    
    @Override
    public boolean isSingleton() {
        return true;
    }
	
	
	/**
	 * Default Constructor.
	 */
	public DataServicesFactoryBean() {}
	
    public boolean createDatabase(final String db, final boolean drop) {
		boolean toReturn = true;

		// create simple JdbcTemplate if necessary
		if (jdbcTemplate == null) {
			DataSource dataSource = getDataSource("");
			jdbcTemplate = new JdbcTemplate(dataSource);
		}

		try {
			// drop if desired
			if (drop)
				jdbcTemplate.execute("DROP DATABASE IF EXISTS " + db);

			// create
			jdbcTemplate.execute("CREATE DATABASE " + db);
		}
		catch (DataAccessException e) {
			log.error(e);
			toReturn = false;
		}

		return toReturn;
	}


	/**
	 * - instance factory-method 
	 * that returns the existing data source 
	 * or creates a new one using instance
	 * variables: driver, connection, user, password, and
	 * the database name.
	 * 
	 * @param databaseName 
	 */
	public DataSource getDataSource(String databaseName) {
    	DataSource ds = getDataSourceMap().get(databaseName);
    	if(ds == null) { // create new one
    		ds = getDataSource(dbUser, dbPassword, dbDriver, 
    				dbConnection + databaseName);
    	}
        return ds; 
	}
	
	
	/* *** static methods *** */
	
	/**
	 * Static factory method that creates any DataSource.
	 * 
	 * @param dbUser
	 * @param dbPassword
	 * @param dbDriver
	 * @param dbUrl
	 * @return
	 */
	private final static DataSource getDataSource(
			String dbUser, 
			String dbPassword,
			String dbDriver, 
			String dbUrl) 
	{
		
		// Springs DriverManagerDataSource is not intended for production use, but this method is not critical anymore...
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(dbDriver);
		// The following connection parameters are terribly important 
		// (if not the most important part in cpath2 at all)!
		dataSource.setUrl(dbUrl + "?autoReconnect=true&max_allowed_packet=256M");
		//&useServerPrepStmts=true&useCursorFetch=true
		dataSource.setUsername(dbUser);
		dataSource.setPassword(dbPassword);
		
//		
//		ComboPooledDataSource dataSource = new ComboPooledDataSource();
//		try {
//			dataSource.setDriverClass(dbDriver);
//			dataSource.setJdbcUrl(dbUrl + "?autoReconnect=true&max_allowed_packet=256M");
//			dataSource.setUser(dbUser);
//			dataSource.setPassword(dbPassword);
//			dataSource.setMaxPoolSize(10);
//			dataSource.setMaxStatements(50);
//			dataSource.setMaxIdleTime(1800);
//		} catch (PropertyVetoException e) {
//			throw new RuntimeCacheException("getDataSource: failed to set connection properties!", e);
//		}
		
		return dataSource;
	}	
	
	
	/**
	 * Fantastic way to create a database schema!
	 * 
	 * This implicitly calls 
	 * {@link #createDatabase(String, String, String, String, String)} 
	 * method.
	 * 
	 * @param dbName - db name to initialize
	 */
	public static void createSchema(String dbName) {
		// drop existing index dir.
		dropFulltextIndex(dbName);
		
		// get the data source factory bean (aware of the driver, user, and password)
		ApplicationContext ctx = 
			new ClassPathXmlApplicationContext("classpath:internalContext-dsFactory.xml");
		DataServices dataServices = (DataServices) ctx.getBean("&dsBean");
		dataServices.createDatabase(dbName, true);
		DataSource premergeDataSource = dataServices.getDataSource(dbName);
		getDataSourceMap().put(CPathSettings.CREATE_DB_KEY, premergeDataSource);
		// set property for the index dir
		System.setProperty("cpath2.db.name", dbName);
		// load the context (depends on the above key) that auto-creates tables
		ctx = new ClassPathXmlApplicationContext(
				"classpath:internalContext-createSchema.xml");
	}
	
    
    /**
     * Deletes a not empty file directory
     * 
     * @param path
     * @return
     */
    static public boolean deleteDirectory(File path) {
        if( path.exists() ) {
          File[] files = path.listFiles();
          for(int i=0; i<files.length; i++) {
             if(files[i].isDirectory()) {
               deleteDirectory(files[i]);
             }
             else {
               files[i].delete();
             }
          }
        }
        return( path.delete() );
    }
    
    
    private static void dropFulltextIndex(String dbName) {
		// drop existing index dir.
		File dir = new File(CPathSettings.getHomeDir() + File.separator + dbName);
		if(log.isInfoEnabled())
			log.info("Removing full-text index directory : " 
				+ dir.getAbsolutePath());
		deleteDirectory(dir);
    }
 
    
    /**
     * Creates full-text index for the "main" DB
     * (actual connection parameters are set 
     * from system/environment properties)
     * 
     */
    public static void rebuildMainIndex() {
    	ApplicationContext ctx = 
			new ClassPathXmlApplicationContext("classpath:internalContext-dsFactory.xml");
		DataServices ds = (DataServices) ctx.getBean("&dsBean");
    	ds.createIndex(ds.getMainDb());
    }
    
    /**
     * Creates full-text index for the "proteins" DB
     * (actual connection parameters are set 
     * from system/environment properties)
     * 
     */
    public static void rebuildProteinsIndex() {
    	ApplicationContext ctx = 
			new ClassPathXmlApplicationContext("classpath:internalContext-dsFactory.xml");
		DataServices ds = (DataServices) ctx.getBean("&dsBean");
    	ds.createIndex(ds.getProteinsDb());
    }
    
    /**
     * Creates full-text index for the "molecules" DB
     * (actual connection parameters are set 
     * from system/environment properties)
     * 
     */
    public static void rebuildMoleculesIndex() {
    	ApplicationContext ctx = 
			new ClassPathXmlApplicationContext("classpath:internalContext-dsFactory.xml");
		DataServices ds = (DataServices) ctx.getBean("&dsBean");
    	ds.createIndex(ds.getMoleculesDb());
    }
    
    /**
     * Creates full-text index for the database name
     * (other connection parameters are set from 
     * system properties)
     * 
     * @param db
     */
    public static void rebuildIndex(String db) {
    	ApplicationContext ctx = 
			new ClassPathXmlApplicationContext("classpath:internalContext-dsFactory.xml");
		DataServices ds = (DataServices) ctx.getBean("&dsBean");
    	ds.createIndex(db);
    }

    
    class MySessionFactoryBean extends AnnotationSessionFactoryBean {
    	public MySessionFactoryBean(Properties properties) {
        	setHibernateProperties(properties);
        	setPackagesToScan(new String[]{"org.biopax.paxtools.impl.level3",
    			"cpath.warehouse.beans"});
        	setAnnotatedClasses(new Class[]{BioPAXElementImpl.class});
    	}
    	
    	@Override
    	public SessionFactory getObject() {
    		try {
				afterPropertiesSet();
				return buildSessionFactory();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
    	}
	}
    
    
    /**
     * Awesome mass-parallel indexing method.
     * 
     * Note: this high-speed method automatically re-builds the index  
     * based only on the Hibernate/Search annotations; so
     * all other/extra index fields and information (e.g., manually generated 
     * using Lucene/Search API) will be lost (has to be re-created again)!
     * 
     */
    public void createIndex(String db) {
    	// delete existing index dir
    	dropFulltextIndex(db);
    	
    	Properties properties = new Properties();
    	// pooling (c3p0) is disabled (it leads to a deadlock during the mass-indexing...)
    	properties.put("hibernate.connection.release_mode", "after_transaction");
    	properties.put("hibernate.dialect", "org.hibernate.dialect.MySQL5Dialect");
    	properties.put("hibernate.connection.url", dbConnection+db);
    	properties.put("hibernate.connection.username", dbUser);
    	properties.put("hibernate.connection.password", dbPassword);
    	properties.put("hibernate.connection.driver_class", dbDriver);
    	properties.put("hibernate.search.default.indexBase", CPathSettings.getHomeDir()+File.separator+db);
    	properties.put("hibernate.search.default.directory_provider", "filesystem");
    	properties.put("hibernate.search.indexing_strategy", "manual");
    	properties.put("hibernate.cache.use_second_level_cache", "false");
    	
		MySessionFactoryBean sfb = new MySessionFactoryBean(properties);
    	SessionFactory sessionFactory = sfb.getObject();
    	
		if(log.isInfoEnabled())
			log.info("Begin indexing...");
		
		// - often gets stuck or crashes...
		Session ses = sessionFactory.openSession();
		FullTextSession fullTextSession = Search.getFullTextSession(ses);
		Transaction tx = fullTextSession.beginTransaction();
		try {
			fullTextSession.createIndexer()
				.purgeAllOnStart(true)
				.batchSizeToLoadObjects( 5 )
				.threadsForSubsequentFetching( 1 )
				.threadsToLoadObjects( 1 )
//				.limitIndexedObjectsTo(10000)
//				.optimizeOnFinish(true)
				.startAndWait();
		} catch (Exception e) {
			throw new RuntimeException("Index re-build is interrupted.", e);
		}	
		tx.commit();
		fullTextSession.close();
		sessionFactory.close();
		sfb.destroy();
		
		if(log.isInfoEnabled())
			log.info("End indexing.");
	}
    
}