package cpath.dao.internal;

import static org.junit.Assert.*;

import javax.sql.DataSource;

import org.junit.Test;

public class DataServicesFactoryBeanTest {

	@Test
	public void testCreateSchemaString() {
		
		DataServicesFactoryBean.createSchema("cpath2_test");
		
		DataSource dataSource = DataServicesFactoryBean
			.getDataSource("cbio", "cbio", "com.mysql.jdbc.Driver", 
					"jdbc:mysql://localhost/cpath2_test");
		
		assertNotNull(dataSource);
		
		DataServicesFactoryBean.rebuildIndex("cpath2_test");
	}

}
