/**
 * 
 */
package cpath.log.jpa;


import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cpath.log.LogType;
import cpath.log.LogUtils;

import static org.junit.Assert.*;

/**
 * @author rodche
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:testContext-log.xml"})
public class LogRepositoriesTest {
	
	@Autowired
	private LogEntitiesRepository repository;
	
	
	@Test
	public final void testCountryLookup() {
		Geoloc loc = LogUtils.countryLookup("66.249.74.168");
		assertNotNull(loc);
		assertEquals("US", loc.getCountry());
		assertNull(loc.getRegion());
	}

	
	@Test
	@DirtiesContext //other tests might added records too; do cleanup
	public final void testSave() {
		final String ipAddr = "66.249.74.168";
		//explicitly create and save a new log record
		LogEntity logEntity = new LogEntity(LogType.ERROR, "500", 
				LogUtils.today(), Geoloc.fromIpAddress(ipAddr)); //"US"
		assertNull(logEntity.getId());
		//save and check that new log entrie's initial count it set to 0
		logEntity = repository.save(logEntity);
		assertEquals(0L, logEntity.getCount().longValue());	
		assertNotNull(logEntity.getId());
		assertEquals(1, repository.count());
	}

	
	/**
	 * Test method for {@link org.springframework.data.repository.CrudRepository#save(S)}.
	 */
	@DirtiesContext //other tests might added records too; do cleanup
	@Test
	public final void testCount() {	
		final String ipAddr = "66.249.74.168"; //some IP (perhaps it's Google's)
	
		// count twice
		LogEntity logEntity = LogUtils.count(repository, LogUtils.today(), LogType.ERROR, null, ipAddr);
		assertEquals(1L, logEntity.getCount().longValue());
		logEntity = LogUtils.count(repository, LogUtils.today(), LogType.ERROR, null, ipAddr);
		assertEquals(2L, logEntity.getCount().longValue());
		
		// test that there is only one record yet
		assertEquals(1, repository.count());
	}
	
	
	@DirtiesContext //other tests might added records too; do cleanup
	@Test
	public final void testTimeline() {	
		final String ipAddr = "66.249.74.168"; //some IP (perhaps it's Google's)
	
		// add some logs (for two days, several categories):
		// Today
		String today = LogUtils.today();
		LogUtils.count(repository, today, LogType.ERROR, "500", ipAddr);
		LogUtils.count(repository, today, LogType.TOTAL, null, ipAddr);
		LogUtils.count(repository, today, LogType.ERROR, "460", ipAddr);
		LogUtils.count(repository, today, LogType.TOTAL, null, ipAddr);
		LogUtils.count(repository, today, LogType.PROVIDER, "Reactome", ipAddr);
		LogUtils.count(repository, today, LogType.TOTAL, null, ipAddr);
		LogUtils.count(repository, today, LogType.PROVIDER, "HumanCyc", ipAddr);
		LogUtils.count(repository, today, LogType.TOTAL, null, ipAddr);
		LogUtils.count(repository, today, LogType.COMMAND, "search", ipAddr);
		// Yesterday
		String yesterDay = LogUtils.addIsoDate(today, -1);
		LogUtils.count(repository, yesterDay, LogType.ERROR, "500", ipAddr);
		LogUtils.count(repository, yesterDay, LogType.TOTAL, null, ipAddr);
		LogUtils.count(repository, yesterDay, LogType.ERROR, "460", ipAddr);
		LogUtils.count(repository, yesterDay, LogType.TOTAL, null, ipAddr);
		LogUtils.count(repository, yesterDay, LogType.PROVIDER, "Reactome", ipAddr);
		LogUtils.count(repository, yesterDay, LogType.TOTAL, null, ipAddr);
		LogUtils.count(repository, yesterDay, LogType.PROVIDER, "HumanCyc", ipAddr);
		LogUtils.count(repository, yesterDay, LogType.TOTAL, null, ipAddr);
		LogUtils.count(repository, yesterDay, LogType.COMMAND, "search", ipAddr);
		
		assertEquals(12, repository.count());
		
		Map<String, List<Object[]>>	res = repository.downloadsTimeline();
		assertNotNull(res);
		assertEquals(4, res.size());
		
		List<Object[]> tl = res.get(LogType.TOTAL.description);
		assertEquals(2, tl.size());
		// check the first item is [today, 4] - 
		// because the time line is sorted in reverse order
		assertEquals(4L, tl.get(0)[1]);
		assertEquals(today, tl.get(0)[0]);

	}
}
