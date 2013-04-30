/**
 * 
 */
package cpath.dao;

import static cpath.config.CPathSettings.PROP_DB_CONNECTION;
import static cpath.config.CPathSettings.PROP_DB_DRIVER;
import static cpath.config.CPathSettings.PROP_DB_PASSW;
import static cpath.config.CPathSettings.PROP_DB_USER;
import static cpath.config.CPathSettings.property;

import java.io.File;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * @author rodche
 *
 */
public final class CPathUtils {

	
	/**
	 * Creates an new empty MySQL database.
	 * If the database exists, it will be destroyed,
	 * and new empty (no tables) one will be created.
	 * 
	 * @param db
	 */
	public static void createDatabase(final String db) {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(property(PROP_DB_DRIVER));
		dataSource.setUrl(property(PROP_DB_CONNECTION));
		dataSource.setUsername(property(PROP_DB_USER));
		dataSource.setPassword(property(PROP_DB_PASSW));
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute("DROP DATABASE IF EXISTS " + db);
		jdbcTemplate.execute("CREATE DATABASE " + db);
	}
	
	
    /**
     * Deletes a not empty file directory
     * 
     * @param path
     * @return
     */
    public static boolean deleteDirectory(File path) {
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
}
