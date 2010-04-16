package cpath.warehouse;

// imports
import cpath.warehouse.beans.Metadata;

/**
 * Class which provides services to create provider database to persist pathway data.
 */
public interface PathwayDataJDBCServices {

	/**
	 * Gets db user.
	 * 
	 * @return String
	 */
	String getDbUser();

	/**
	 * Set db user property.
	 *
	 * @param dbUser String
	 */
	void setDbUser(String dbUser);

	/**
	 * Gets db password.
	 * 
	 * @return String
	 */
	String getDbPassword();

	/**
	 * Set db password property
	 *
	 * @param dbPassword String
	 */
	void setDbPassword(String dbPassword);

	/**
	 * Gets db data source.
	 * 
	 * @return String
	 */
	String getDbDataSource();

	/**
	 * Set db data source property
	 *
	 * @param dbDataSource String
	 */
	void setDbDataSource(String dbDataSource);

	/**
	 * Gets db connection.
	 * 
	 * @return String
	 */
	String getDbConnection();

	/**
	 * Set db connection property.
	 *
	 * @param dbConnection String
	 */
	void setDbConnection(String dbConnection);

    /**
     * Creates database to persist given provider data.
     *
     * @param metadata Metadata
	 * @param drop boolean
	 * @return boolean (true - success, false - failure)
     */
    boolean createProviderDatabase(final Metadata metadata, final boolean drop);
}
