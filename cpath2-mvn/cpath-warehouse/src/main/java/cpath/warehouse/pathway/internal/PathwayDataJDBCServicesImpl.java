package cpath.warehouse.pathway.internal;

// imports
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.pathway.PathwayDataJDBCServices;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Properties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Class which provides services to create provider database to persist pathway data.
 */
public final class PathwayDataJDBCServicesImpl implements PathwayDataJDBCServices {

    // log
    private static Log log = LogFactory.getLog(PathwayDataJDBCServices.class);

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
		Statement statement = null;
		Connection connection = null;

		// create connection string - tack on provider id
		//String dbConnectionStr = dbConnection + "//localhost/mysql";
		String dbConnectionStr = dbConnection + "mysql";

		log.info("createProviderDatabase(), user: " + dbUser);
		log.info("createProviderDatabase(), password: " + dbPassword);
		log.info("createProviderDatabase(), dbDataSource: " + dbDataSource);
		log.info("createProviderDatabase(), dbConnection (url): " + dbConnectionStr);
		
		try {
			// setup a connection
			log.info("createProviderDatabase(), creating a connection.");
			Class.forName(dbDataSource).newInstance();
			connection = DriverManager.getConnection(dbConnectionStr, dbUser, dbPassword);

			// drop database if desired and if it exists
			if (drop) {
				log.info("createProviderDatabase(), executing drop statement for provider: " + metadata.getIdentifier());
				statement = connection.createStatement();
				statement.executeUpdate("DROP DATABASE IF EXISTS " + metadata.getIdentifier());
			}
			
			// create new database
			log.info("createProviderDatabase(), executing create statement for provider: " + metadata.getIdentifier());
			statement = connection.createStatement();
			statement.executeUpdate("CREATE DATABASE " + metadata.getIdentifier());
		}
		catch (Exception e) {
			// note, we can catch mysql SQLException code 1007, database already exists,
			// but if that occurs, it means our drop flag was incorrect,
			// so we shouldn't silently fail, so all exceptions return a false value
			e.printStackTrace();
			toReturn = false;
		}
		finally {
			if (statement != null) {
				try {
					statement.close();
				}
				catch (SQLException e) {
					// ignore
				}
			}
			if (connection != null) {
				try {
					connection.close();
				}
				catch (SQLException e) {
					// ignore
				}
			}
		}

		// outta here
		return toReturn;
	}
}
