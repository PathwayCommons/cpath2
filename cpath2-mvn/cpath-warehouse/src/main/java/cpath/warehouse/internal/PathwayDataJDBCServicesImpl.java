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
package cpath.warehouse.internal;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import cpath.warehouse.PathwayDataJDBCServices;
import cpath.warehouse.beans.Metadata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Repository;

/**
 * Class which provides services to create provider database to persist pathway data.
 */
@Repository
public class PathwayDataJDBCServicesImpl implements PathwayDataJDBCServices {
    // log
    private static Log log = LogFactory.getLog(PathwayDataJDBCServicesImpl.class);

	// ref to some db props - set via spring
	private String dbUser;
	public void setDbUser(String dbUser) { this.dbUser = dbUser; }
	public String getDbUser() { return dbUser; }

	private String dbPassword;
	public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }
	public String getDbPassword() { return dbPassword; }

	private String dbDriver;
	public void setDbDriver(String dbDriver) { this.dbDriver = dbDriver; }
	public String getDbDriver() { return dbDriver; }

	private String dbConnection;
	public void setDbConnection(String dbConnection) { this.dbConnection = dbConnection; }
	public String getDbConnection() { return dbConnection; }

	// ref to jdbc template
	private JdbcTemplate jdbcTemplate;

	/**
	 * Default Constructor.
	 */
	public PathwayDataJDBCServicesImpl() {}
	

	// this method is automatically called by Spring after configuration 
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
	
	
    /*
	 * (non-Javadoc)
	 * @see cpath.warehouse.pathway.PathwayDataJDBCServices#createProviderDatabase(cpath.warehouse.beans.Metadata, java.lang.boolean)
     */
    public boolean createProviderDatabase(final Metadata metadata, 
    		final boolean drop) {
		
		boolean toReturn = true;

		// create simple JdbcTemplate if necessary
		if (jdbcTemplate == null) {
			DataSource dataSource = getDataSource("mysql");
			jdbcTemplate = new JdbcTemplate(dataSource);
		}

		try {
			// drop if desired
			if (drop) {
				jdbcTemplate.execute("DROP DATABASE IF EXISTS " + metadata.getIdentifier());
			}

			// create
			jdbcTemplate.execute("CREATE DATABASE " + metadata.getIdentifier());
			
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
	 * Factory-method
	 * 
	 * (non-Javadoc)
	 * @see cpath.warehouse.PathwayDataJDBCServices#getDataSource(java.lang.String)
	 */
	public DataSource getDataSource(String databaseName) {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(dbDriver);
		dataSource.setUrl(dbConnection + databaseName);
		dataSource.setUsername(dbUser);
		dataSource.setPassword(dbPassword);
		return dataSource;
	}
	
}
