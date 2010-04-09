package cpath.warehouse.pathway.internal;

// imports
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.pathway.PathwayDataJDBCServices;

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
public final class PathwayDataJDBCServicesImpl implements PathwayDataJDBCServices {

    // log
    private static Log log = LogFactory.getLog(PathwayDataJDBCServicesImpl.class);

	// ref to some db props - set via spring
	private String dbUser;
	public void setDbUser(String dbUser) { this.dbUser = dbUser; }
	public String getDbUser() { return dbUser; }

	private String dbPassword;
	public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }
	public String getDbPassword() { return dbPassword; }

	private String dbDataSource;
	public void setDbDataSource(String dbDataSource) { this.dbDataSource = dbDataSource; }
	public String getDbDataSource() { return dbDataSource; }

	private String dbConnection;
	public void setDbConnection(String dbConnection) { this.dbConnection = dbConnection; }
	public String getDbConnection() { return dbConnection; }

	// ref to jdbc template
	private JdbcTemplate jdbcTemplate;

	/**
	 * Default Constructor.
	 */
	public PathwayDataJDBCServicesImpl() {}

    /**
	 * (non-Javadoc)
	 * @see cpath.warehouse.pathway.PathwayDataJDBCServices#createProviderDatabase(cpath.warehouse.beans.Metadata, java.lang.boolean)
     */
    public boolean createProviderDatabase(final Metadata metadata, final boolean drop) {
		
		boolean toReturn = true;

		// create simplet JdbcTemplate if necessary
		if (jdbcTemplate == null) {
			DriverManagerDataSource dataSource = new DriverManagerDataSource();
			dataSource.setDriverClassName(dbDataSource);
			dataSource.setUrl(dbConnection + "mysql");
			dataSource.setUsername(dbUser);
			dataSource.setPassword(dbPassword);
			jdbcTemplate = new JdbcTemplate(dataSource);
		}

		try {
			// drop if desired
			if (drop) {
				jdbcTemplate.execute("DROP DATABASE IF EXISTS " + metadata.getIdentifier());
			}

			// create
			jdbcTemplate.execute("CREATE DATABASE " + metadata.getIdentifier());
		}
		catch (DataAccessException e) {
			e.printStackTrace();
			toReturn = false;
		}

		// outta here
		return toReturn;
	}
}
