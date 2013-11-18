/**
 * 
 */
package cpath.log;


import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cpath.log.LogType;
import cpath.log.LogUtils;
import cpath.log.jpa.Geoloc;
import cpath.log.jpa.LogEntitiesRepository;
import cpath.log.jpa.LogEntity;
import cpath.log.jpa.LogEvent;
import cpath.service.Cmd;
import cpath.service.OutputFormat;
import cpath.service.Status;

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
		Geoloc loc = LogUtils.lookup("66.249.74.168");
		assertNotNull(loc);
		assertEquals("US", loc.getCountry());
//		assertNull(loc.getRegion());
	}

	
	@Test
	@DirtiesContext //other tests might added records too; do cleanup
	public final void testSave() {
		final String ipAddr = "66.249.74.168";
		//explicitly create and save a new log record
		LogEntity logEntity = new LogEntity(LogUtils.today(), 
				LogEvent.from(Status.INTERNAL_ERROR), Geoloc.fromIpAddress(ipAddr)); //"US"
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
		LogEntity logEntity = LogUtils.count(repository, LogUtils.today(), LogEvent.total(), ipAddr);
		assertEquals(1L, logEntity.getCount().longValue());
		logEntity = LogUtils.count(repository, LogUtils.today(), LogEvent.total(), ipAddr);
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
		LogUtils.count(repository, today, LogEvent.from(Status.INTERNAL_ERROR), ipAddr);
		LogUtils.count(repository, today, LogEvent.total(), ipAddr);
		LogUtils.count(repository, today, LogEvent.from(Status.NO_RESULTS_FOUND), ipAddr);
		LogUtils.count(repository, today, LogEvent.total(), ipAddr);
		LogUtils.count(repository, today, new LogEvent(LogType.PROVIDER, "Reactome"), ipAddr);
		LogUtils.count(repository, today, LogEvent.total(), ipAddr);
		LogUtils.count(repository, today, new LogEvent(LogType.PROVIDER, "HumanCyc"), ipAddr);
		LogUtils.count(repository, today, LogEvent.total(), ipAddr);
		LogUtils.count(repository, today, LogEvent.from(Cmd.SEARCH, null), ipAddr);
		// Yesterday
		String yesterDay = LogUtils.addIsoDate(today, -1);
		LogUtils.count(repository, yesterDay, LogEvent.from(Status.INTERNAL_ERROR), ipAddr);
		LogUtils.count(repository, yesterDay, LogEvent.total(), ipAddr);
		LogUtils.count(repository, yesterDay, LogEvent.from(Status.NO_RESULTS_FOUND), ipAddr);
		LogUtils.count(repository, yesterDay, LogEvent.total(), ipAddr);
		LogUtils.count(repository, yesterDay, new LogEvent(LogType.PROVIDER, "Reactome"), ipAddr);
		LogUtils.count(repository, yesterDay, LogEvent.total(), ipAddr);
		LogUtils.count(repository, yesterDay, new LogEvent(LogType.PROVIDER, "HumanCyc"), ipAddr);
		LogUtils.count(repository, yesterDay, LogEvent.total(), ipAddr);
		LogUtils.count(repository, yesterDay, LogEvent.from(Cmd.SEARCH, null), ipAddr);
		
		assertEquals(12, repository.count());
		
		//timeline per type (incl. for TOTAL)
		Map<String, List<Object[]>>	res = repository.downloadsTimeline();
		assertNotNull(res);
		assertEquals(4, res.size());
		
		List<Object[]> tl = res.get(LogType.TOTAL.description);
		assertNotNull(tl);
		assertEquals(2, tl.size());
		// check the first item is [today, 4] - 
		// because the time line is sorted in reverse order
		assertEquals(4L, tl.get(0)[1]);
		assertEquals(today, tl.get(0)[0]);
		
		// for one category only
		res = repository.downloadsTimeline(LogType.PROVIDER, null);
		//two entries: for all types, for PROVIDER type
		assertEquals(3, res.size());
		tl = res.get(LogType.TOTAL.description);
		assertNotNull(tl);
		assertEquals(4L, tl.get(0)[1]); //total
		tl = res.get("Reactome");
		assertNotNull(tl);
		assertEquals(1L, tl.get(0)[1]); //PROVIDER type, today counts
		assertEquals(today, tl.get(0)[0]);
		tl = res.get("HumanCyc");
		assertNotNull(tl);
		assertEquals(1L, tl.get(1)[1]); //PROVIDER type, yesterday counts
		assertEquals(yesterDay, tl.get(1)[0]);
				
		// for error 500 only
		res = repository.downloadsTimeline(LogType.ERROR, "INTERNAL_ERROR");
		//two map entries: for all downloads, for error 500
		assertEquals(2, res.size());
		tl = res.get(LogType.TOTAL.description);
		assertNotNull(tl);
		assertEquals(4L, tl.get(0)[1]); //total everything (not only in ERROR cat.)
		tl = res.get("INTERNAL_ERROR");
		assertNotNull(tl);
		assertEquals(1L, tl.get(0)[1]); //PROVIDER type, today counts
		assertEquals(today, tl.get(0)[0]);
	}
	
	@DirtiesContext //other tests might added records too; do cleanup
	@Test
	public final void testTimeline2() {	
		final String ipAddr = "66.249.74.168"; //some IP (perhaps it's Google's)
		Geoloc loc = Geoloc.fromIpAddress(ipAddr);
	
		// add some logs (for two days, several categories):
		// Today
		Set<LogEvent> events = new HashSet<LogEvent>(
			Arrays.asList(
					LogEvent.from(OutputFormat.BIOPAX),
					new LogEvent(LogType.PROVIDER, "Reactome"),
					new LogEvent(LogType.PROVIDER, "HumanCyc"),
					LogEvent.from(Cmd.GET, null)
			)
		);
		
		//save/count all + total (once)
		LogUtils.log(repository, events, loc);

		assertEquals(5, repository.count());
		
		//timeline per type (incl. TOTAL)
		Map<String, List<Object[]>>	res = repository.downloadsTimeline();
		assertNotNull(res);
		assertEquals(4, res.size());
		
		List<Object[]> tl = res.get(LogType.TOTAL.description);
		assertNotNull(tl);
		assertEquals(1, tl.size());
		assertEquals(1L, tl.get(0)[1]);
		assertEquals(LogUtils.today(), tl.get(0)[0]);
		
		// for one category only
		res = repository.downloadsTimeline(LogType.PROVIDER, null);
		//two entries: for all types, for PROVIDER type
		assertEquals(3, res.size());
		tl = res.get(LogType.TOTAL.description);
		assertNotNull(tl);
		assertEquals(1L, tl.get(0)[1]); //total
		tl = res.get("Reactome");
		assertNotNull(tl);
		assertEquals(1L, tl.get(0)[1]); //PROVIDER type, today counts
		tl = res.get("HumanCyc");
		assertNotNull(tl);
		assertEquals(1L, tl.get(0)[1]); //PROVIDER type, yesterday counts
	}
}
