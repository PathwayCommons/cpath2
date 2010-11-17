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

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import javax.validation.constraints.NotNull;

import cpath.config.CPathSettings;
import cpath.dao.DataServices;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;


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
    
    @NotNull
	private String dbUser;
	@Value("${user}")
	public void setDbUser(String dbUser) { this.dbUser = dbUser; }
	public String getDbUser() { return dbUser; }

	@NotNull
	private String dbPassword;
	@Value("${password}")
	public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }
	public String getDbPassword() { return dbPassword; }

	@NotNull
	private String dbDriver;
	@Value("${driver}")
	public void setDbDriver(String dbDriver) { this.dbDriver = dbDriver; }
	public String getDbDriver() { return dbDriver; }

	@NotNull
	private String dbConnection;
	@Value("${connection}")
	public void setDbConnection(String dbConnection) { this.dbConnection = dbConnection; }
	public String getDbConnection() { return dbConnection; }

	@NotNull
	private String metaDb;
	@Value("${metadata.db}")
	public void setMetaDb(String db) {this.metaDb = db;}
	public String getMetaDb() {return metaDb;}

	@NotNull
	private String mainDb;
	@Value("${main.db}")
	public void setMainDb(String db) {this.mainDb = db;}
	public String getMainDb() {return mainDb;}

	@NotNull
	private String proteinsDb;
	@Value("${proteins.db}")
	public void setProteinsDb(String db) {this.proteinsDb = db;}
	public String getProteinsDb() {return proteinsDb;}

	@NotNull
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
	

	@PostConstruct public void init() {
		// create and save data sources in the map
		DataSource dataSource = getDataSource(mainDb);
		getDataSourceMap().put(CPathSettings.MAIN_DB_KEY, dataSource);
		dataSource = getDataSource(metaDb);
		getDataSourceMap().put(CPathSettings.METADATA_DB_KEY, dataSource);
		dataSource = getDataSource(proteinsDb);
		getDataSourceMap().put(CPathSettings.PROTEINS_DB_KEY, dataSource);
		dataSource = getDataSource(moleculesDb);
		getDataSourceMap().put(CPathSettings.MOLECULES_DB_KEY, dataSource);
	}

	
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
	public static DataSource getDataSource(
			String dbUser, 
			String dbPassword,
			String dbDriver, 
			String dbUrl) {
/*		
		com.mchange.v2.c3p0.ComboPooledDataSource dataSource = new com.mchange.v2.c3p0.ComboPooledDataSource();
		try {
			dataSource.setDriverClass(dbDriver);
		} catch (PropertyVetoException e) {
			throw new RuntimeException(e);
		}
		dataSource.setJdbcUrl(dbUrl);
		dataSource.setUser(dbUser);
		dataSource.setPassword(dbPassword);
		dataSource.setInitialPoolSize(0);
		dataSource.setMaxPoolSize(30);
		return dataSource;
*/
		
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(dbDriver);
		/*
		 * The following connection parameters are terribly,
		 * unbelievably important 
		 * (if not the most important part in cpath2 at all)!
		 */
		dataSource.setUrl(dbUrl + 
			"?autoReconnect=true&max_allowed_packet=256M");
			//+ "&useUnicode=true&characterEncoding=UTF-8"); // causes more pain...
		dataSource.setUsername(dbUser);
		dataSource.setPassword(dbPassword);		
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
    
    
    public void dropMainFulltextIndex() {
    	dropFulltextIndex(mainDb);
    }

    public void dropMoleculesFulltextIndex() {
    	dropFulltextIndex(moleculesDb);
    }
    
    public void dropProteinsFulltextIndex() {
    	dropFulltextIndex(proteinsDb);
    }
    
    public void dropMetadataFulltextIndex() {
    	dropFulltextIndex(metaDb);
    }

}