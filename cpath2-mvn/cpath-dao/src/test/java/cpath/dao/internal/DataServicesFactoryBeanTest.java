package cpath.dao.internal;


import org.junit.Test;

public class DataServicesFactoryBeanTest {

	@Test
	public void testCreateSchemaString() {	
		DataServicesFactoryBean.createSchema("cpath2_test");
//		DataServicesFactoryBean.rebuildIndex("cpath2_test"); //deprecated, not used anymore
	}

}
