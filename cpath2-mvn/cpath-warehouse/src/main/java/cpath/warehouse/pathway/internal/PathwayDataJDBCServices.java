package cpath.warehouse.pathway.internal;

// imports
import cpath.warehouse.beans.Metadata;

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
public final class PathwayDataJDBCServices {

    // log
    private static Log log = LogFactory.getLog(PathwayDataJDBCServices.class);

	// ref to warehouse properties
	private Properties dbProperties;

	/**
	 * Constructor.
	 */
	public PathwayDataJDBCServices() throws Exception {

		// load up properties
		this.dbProperties = new Properties();
		this.dbProperties.load(this.getClass().getResourceAsStream("/warehouse.properties"));

		if (dbProperties == null) {
			throw new Exception("Cannot find warehouse.properties file.");
		}
		else {
			log.info("user: " + dbProperties.getProperty("db.user"));
			log.info("user: " + dbProperties.getProperty("db.password"));
			log.info("datasource: " + dbProperties.getProperty("db.datasource"));
			log.info("connection (url): " + dbProperties.getProperty("db.connection"));
		}
	}

    /**
     * Persists the given biopax model to a unique provider db.
     *
     * @param metadata Metadata
	 * @param drop boolean
	 * @return boolean (true - success, false - failure)
     */
    public boolean createProviderDatabase(final Metadata metadata, final boolean drop) {

		boolean toReturn = true;
		Statement statement = null;
		Connection connection = null;
		
		try {
			// setup a connection
			log.info("createProviderDatabase(), creating a connection.");
			Class.forName(dbProperties.getProperty("db.datasource")).newInstance();
			connection = DriverManager.getConnection(dbProperties.getProperty("db.connection"),
													 dbProperties.getProperty("db.user"),
													 dbProperties.getProperty("db.password"));

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
