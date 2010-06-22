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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

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
 * Class that provides services to create 
 * pathway data provider databases; 
 * also is used as a general data source factory.
 * 
 * Note: it is MySQL-specific. TODO: add 'masterDb' field...
 */
public class DataServicesFactoryBean implements DataServices, BeanNameAware, FactoryBean<DataSource> {
    // log
    private static Log log = LogFactory.getLog(DataServicesFactoryBean.class);

	// ref to some db props - set via spring
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
	
	private JdbcTemplate jdbcTemplate;

    private String beanName;
    
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
    	DataSource ds = getDataSourceMap().get(beanName);
    	if(ds == null) {
    		// create new one
    		ds = getDataSource(beanName);
    		getDataSourceMap().put(beanName, ds);
    	}
        return ds; 
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
	 * Static factory method that creates any DataSource.
	 * 
	 * @param dbUser
	 * @param dbPassword
	 * @param dbDriver
	 * @param dbUrl
	 * @return the data source
	 * 
	 * @deprecated not used anymore...
	 */
	public static DataSource getDataSource(String dbUser, String dbPassword,
			String dbDriver, String dbUrl) {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(dbDriver);
		dataSource.setUrl(dbUrl);
		dataSource.setUsername(dbUser);
		dataSource.setPassword(dbPassword);
		return dataSource;
	}
	
	
	/**
	 * Default Constructor.
	 */
	public DataServicesFactoryBean() {}
	

	@PostConstruct public void init() {
		if (dbConnection == null) {
			throw new IllegalArgumentException("The database connection string is required");
		}
		if (dbDriver == null) {
			throw new IllegalArgumentException("The database driver class name is required");
		}
		if (dbUser == null) {
			throw new IllegalArgumentException("The path to the test data set is required");
		}
		if (dbPassword == null) {
			throw new IllegalArgumentException("The path to the test data set is required");
		}
	}
    
    
    public boolean createDatabase(final String db, final boolean drop) 
    {
		boolean toReturn = true;

		// create simple JdbcTemplate if necessary
		if (jdbcTemplate == null) {
			DataSource dataSource = getDataSource("mysql"); // works for MySQL
			jdbcTemplate = new JdbcTemplate(dataSource);
		}

		try {
			// drop if desired
			if (drop) {
				jdbcTemplate.execute("DROP DATABASE IF EXISTS " + db);
			}

			// create
			jdbcTemplate.execute("CREATE DATABASE " + db);
			
			// save 
		}
		catch (DataAccessException e) {
			e.printStackTrace();
			toReturn = false;
		}

		// outta here
		return toReturn;
	}


	/**
	 * Factory-method that get a new data source using instance
	 * variables: driver, connection, user, password, and
	 * parameter database name.
	 * 
	 * (non-Javadoc)
	 * @see cpath.dao.DataServices#getDataSource(java.lang.String)
	 */
	public DataSource getDataSource(String databaseName) {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(dbDriver);
		dataSource.setUrl(dbConnection + databaseName);
		dataSource.setUsername(dbUser);
		dataSource.setPassword(dbPassword);
		return dataSource;
	}
	
	
	/**** static methods *****/
	
	public static void createTestSchema() {
		ApplicationContext ctx = 
			new ClassPathXmlApplicationContext("classpath:internalContext-creationTest.xml");
		// note: createTestDatabases is called during the context init!
	}
	
	public static void createTestDatabases(String user,
			String passwd, String conn, String driver) {
		DataServicesFactoryBean bean = new DataServicesFactoryBean();
		bean.setDbUser(user);
		bean.setDbPassword(passwd);
		bean.setDbConnection(conn);
		bean.setDbDriver(driver);
		bean.createDatabase("cpath2_meta_test", true);
		bean.createDatabase("cpath2_main_test", true);
		bean.createDatabase("cpath2_molecules_test", true);
		bean.createDatabase("cpath2_proteins_test", true);
	}	
	
	public static void createSchema() {
		new ClassPathXmlApplicationContext("classpath:internalContext-creation.xml");
		// note: createDatabases is called during the context init!
	}
	
	public static void createDatabases(String user,
			String passwd, String conn, String driver) {
		DataServicesFactoryBean bean = new DataServicesFactoryBean();
		bean.setDbUser(user);
		bean.setDbPassword(passwd);
		bean.setDbConnection(conn);
		bean.setDbDriver(driver);
		bean.createDatabase("cpath2_meta", true);
		bean.createDatabase("cpath2_main", true);
		bean.createDatabase("cpath2_molecules", true);
		bean.createDatabase("cpath2_proteins", true);
	}
}
