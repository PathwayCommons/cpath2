package cpath.dao.internal;


import org.junit.Test;

public class DataServicesFactoryBeanTest {

	@Test
	public void testCreateSchemaString() {	
		DataServicesFactoryBean.createSchema("test_cpath2ware");
//		DataServicesFactoryBean.rebuildIndex("test_cpath2ware"); //deprecated, not used anymore
	}

}
