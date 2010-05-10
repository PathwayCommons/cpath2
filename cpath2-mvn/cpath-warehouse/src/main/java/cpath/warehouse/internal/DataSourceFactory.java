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

// imports
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Map;
import java.util.HashMap;

import javax.sql.DataSource;

public class DataSourceFactory implements BeanNameAware, FactoryBean<DataSource> {

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
        return getDataSourceMap().get(beanName);
    }

    @Override
    public Class<?> getObjectType() {
        return getDataSourceMap().get(beanName).getClass();
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
	 * @param dbConnection
	 * @param dbName the name of the database to use
	 * @return the data source
	 */
	public static DataSource createDataSource(String dbUser, String dbPassword,
			String dbDriver, String dbConnection, String dbName) {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(dbDriver);
		dataSource.setUrl(dbConnection + dbName);
		dataSource.setUsername(dbUser);
		dataSource.setPassword(dbPassword);
		return dataSource;
	}
}