/**
 * 
 */
package cpath.dao;


import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cpath.config.CPathSettings;
import cpath.dao.LogUtils;
import cpath.jpa.Geoloc;
import cpath.jpa.LogEntitiesRepository;
import cpath.jpa.LogEntity;
import cpath.jpa.LogEvent;
import cpath.jpa.LogType;
import cpath.jpa.Mapping;
import cpath.jpa.MappingsRepository;
import cpath.service.Cmd;
import cpath.service.GraphType;
import cpath.service.OutputFormat;
import cpath.service.Status;

import static org.junit.Assert.*;

/**
 * @author rodche
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:META-INF/spring/applicationContext-jpa.xml"})
@ActiveProfiles("dev")
public class JpaRepositoriesTest {
	
	@Autowired
	private LogEntitiesRepository logEntitiesRepository;
	
	@Autowired
	private MappingsRepository mappingsRepository;
	
	
	@Test
	public final void testCountryLookup() {
		Geoloc loc = LogUtils.lookup("66.249.74.168");
		assertNotNull(loc);
		assertEquals("US", loc.getCountry());
		assertEquals("CA", loc.getRegion());
		
		// localhost (and also for any LAN IPs, or not IPv4 ones...)
		loc = LogUtils.lookup("127.0.0.1");
		assertNull(loc);
		loc = Geoloc.fromIpAddress("foo");
		assertNull(loc);
		
		//Basel, Switzerland - no reion
		loc = LogUtils.lookup("5.148.173.100");
		assertNotNull(loc);
		assertEquals("CH", loc.getCountry());
		assertNull(loc.getRegion());
		assertNull(loc.getCity());
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
		logEntity = logEntitiesRepository.save(logEntity);
		assertEquals(0L, logEntity.getCount().longValue());	
		assertNotNull(logEntity.getId());
		assertEquals(1, logEntitiesRepository.count());
	}

	
	/**
	 * Test method for {@link org.springframework.data.repository.CrudRepository#save(S)}.
	 */
	@DirtiesContext //other tests might added records too; do cleanup
	@Test
	public final void testCount() {	
		final String ipAddr = "66.249.74.168"; //some IP (perhaps it's Google's)
	
		// count twice
		LogEntity logEntity = LogUtils.count(logEntitiesRepository, LogUtils.today(), LogEvent.TOTAL, ipAddr);
		assertEquals(1L, logEntity.getCount().longValue());
		logEntity = LogUtils.count(logEntitiesRepository, LogUtils.today(), LogEvent.TOTAL, ipAddr);
		assertEquals(2L, logEntity.getCount().longValue());
		
		// test that there is only one record yet
		assertEquals(1, logEntitiesRepository.count());
	}
	
	
	@DirtiesContext //other tests might added records too; do cleanup
	@Test
	public final void testTimeline() {	
		final String ipAddr = "66.249.74.168"; //some IP (perhaps it's Google's)
	
		// add some logs (for two days, several categories):
		// Today
		String today = LogUtils.today();
		LogUtils.count(logEntitiesRepository, today, LogEvent.from(Status.INTERNAL_ERROR), ipAddr);
		LogUtils.count(logEntitiesRepository, today, LogEvent.TOTAL, ipAddr);
		LogUtils.count(logEntitiesRepository, today, LogEvent.from(Status.NO_RESULTS_FOUND), ipAddr);
		LogUtils.count(logEntitiesRepository, today, LogEvent.TOTAL, ipAddr);
		LogUtils.count(logEntitiesRepository, today, new LogEvent(LogType.PROVIDER, "Reactome"), ipAddr);
		LogUtils.count(logEntitiesRepository, today, LogEvent.TOTAL, ipAddr);
		LogUtils.count(logEntitiesRepository, today, new LogEvent(LogType.PROVIDER, "HumanCyc"), ipAddr);
		LogUtils.count(logEntitiesRepository, today, LogEvent.TOTAL, ipAddr);
		LogUtils.count(logEntitiesRepository, today, LogEvent.from(Cmd.SEARCH), ipAddr);
		// Yesterday
		String yesterDay = LogUtils.addIsoDate(today, -1);
		LogUtils.count(logEntitiesRepository, yesterDay, LogEvent.from(Status.INTERNAL_ERROR), ipAddr);
		LogUtils.count(logEntitiesRepository, yesterDay, LogEvent.TOTAL, ipAddr);
		LogUtils.count(logEntitiesRepository, yesterDay, LogEvent.from(Status.NO_RESULTS_FOUND), ipAddr);
		LogUtils.count(logEntitiesRepository, yesterDay, LogEvent.TOTAL, ipAddr);
		LogUtils.count(logEntitiesRepository, yesterDay, new LogEvent(LogType.PROVIDER, "Reactome"), ipAddr);
		LogUtils.count(logEntitiesRepository, yesterDay, LogEvent.TOTAL, ipAddr);
		LogUtils.count(logEntitiesRepository, yesterDay, new LogEvent(LogType.PROVIDER, "HumanCyc"), ipAddr);
		LogUtils.count(logEntitiesRepository, yesterDay, LogEvent.TOTAL, ipAddr);
		LogUtils.count(logEntitiesRepository, yesterDay, LogEvent.from(Cmd.SEARCH), ipAddr);
		
		assertEquals(12, logEntitiesRepository.count());
		
		//timeline per type
		Map<String, List<Object[]>>	res = logEntitiesRepository.downloadsTimeline(LogType.TOTAL, null);
		assertNotNull(res);
		assertEquals(1, res.size());
		
		List<Object[]> tl = res.get(LogType.TOTAL.description);
		assertNotNull(tl);
		assertEquals(2, tl.size());
		// check the first item is [today, 4] - 
		// because the time line is sorted in reverse order
		assertEquals(4L, tl.get(0)[1]);
		assertEquals(today, tl.get(0)[0]);
		
		// for one category only
		res = logEntitiesRepository.downloadsTimeline(LogType.PROVIDER, null);
		//two entries (reactome and humancyc)
		assertEquals(2, res.size());
		tl = res.get(LogType.TOTAL.description);
		assertNull(tl); //global TOTAL not included
		tl = res.get("Reactome");
		assertNotNull(tl);
		assertEquals(1L, tl.get(0)[1]); //PROVIDER type, today counts
		assertEquals(today, tl.get(0)[0]);
		tl = res.get("HumanCyc");
		assertNotNull(tl);
		assertEquals(1L, tl.get(1)[1]); //PROVIDER type, yesterday counts
		assertEquals(yesterDay, tl.get(1)[0]);
				
		// for error 500 only
		res = logEntitiesRepository.downloadsTimeline(LogType.ERROR, "INTERNAL_ERROR");
		//two map entries: for all downloads, for error 500
		assertEquals(1, res.size());
		tl = res.get(LogType.TOTAL.description);
		assertNull(tl); //global TOTAL not included
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
					LogEvent.from(GraphType.NEIGHBORHOOD)
			)
		);
		
		//save/count all + total (once)
		LogUtils.log(logEntitiesRepository, events, loc);

		assertEquals(5, logEntitiesRepository.count());
		
		//timeline per type (incl. TOTAL)
		Map<String, List<Object[]>>	res = logEntitiesRepository.downloadsTimeline(LogType.TOTAL, null);
		assertNotNull(res);
		assertEquals(1, res.size());
		
		List<Object[]> tl = res.get(LogType.TOTAL.description);
		assertNotNull(tl);
		assertEquals(1, tl.size());
		assertEquals(1L, tl.get(0)[1]);
		assertEquals(LogUtils.today(), tl.get(0)[0]);
		
		// for one category only
		res = logEntitiesRepository.downloadsTimeline(LogType.PROVIDER, null);
		//two entries: reactome, humancyc
		assertEquals(2, res.size());
		tl = res.get(LogType.TOTAL.description);
		assertNull(tl); //global TOTAL not included
		tl = res.get("Reactome");
		assertNotNull(tl);
		assertEquals(1L, tl.get(0)[1]); //PROVIDER type, today counts
		tl = res.get("HumanCyc");
		assertNotNull(tl);
		assertEquals(1L, tl.get(0)[1]); //PROVIDER type, yesterday counts
	}
	
	@Test
	public final void testLogEventFromDownloads() {
		CPathSettings cpath = CPathSettings.getInstance();
		
		String file = cpath.exportArchivePrefix() + "Reactome.BIOPAX.owl.gz";
		Set<LogEvent> events = LogEvent.fromDownloads(file);
		assertEquals(4, events.size());
		
		//'All' 
		file = cpath.exportArchivePrefix() + "All.BIOPAX.owl.gz";
		events = LogEvent.fromDownloads(file);
		assertEquals(3, events.size());
		
		file = cpath.exportArchivePrefix() + "Reactome.GSEA.gmt.gz";
		events = LogEvent.fromDownloads(file);
		assertEquals(4, events.size());
		
		//illegal format - still logged as OTHER
		file = cpath.exportArchivePrefix() + "Reactome.foo.gmt.gz";
		events = LogEvent.fromDownloads(file);
		assertEquals(4, events.size());
		
		//other (metadata etc.)
		file = "blacklist.txt";
		events = LogEvent.fromDownloads(file);
		assertEquals(3, events.size());//counted for: file, command (DOWNLOAD), format (OTHER)
		
		//when a provider's name does not start from a capital letter, LogType.PROVIDER event won't be there
		file = cpath.exportArchivePrefix() + "reactome.foo.gmt.gz";
		events = LogEvent.fromDownloads(file);
		assertEquals(3, events.size());
	}
	
	
	@Test
	public void testIdMapping() {		
        //capitalization is important in 99% of identifier types (we should not ignore it)
        // we should be able to save it and not get 'duplicate key' exception here
		mappingsRepository.save(new Mapping("GeneCards", "ZHX1", "UNIPROT", "P12345"));
		mappingsRepository.save(new Mapping("GeneCards", "ZHX1-C8orf76", "UNIPROT", "Q12345"));
		mappingsRepository.save(new Mapping("GeneCards", "ZHX1-C8ORF76", "UNIPROT", "Q12345"));
        
        //check it's saved
        assertEquals(1, mappingsRepository.map(null, "ZHX1-C8orf76", "UNIPROT").size());
        assertEquals(1, mappingsRepository.map(null, "ZHX1-C8ORF76", "UNIPROT").size());
        assertEquals(1, mappingsRepository.map("GeneCards", "ZHX1-C8ORF76", "UNIPROT").size());
        
        // repeat (should successfully update)- add a Mapping
        mappingsRepository.save(new Mapping("TEST", "FooBar", "CHEBI", "CHEBI:12345"));
        assertTrue(mappingsRepository.map(null, "FooBar", "UNIPROT").isEmpty());
        assertTrue(mappingsRepository.map("TEST", "FooBar", "UNIPROT").isEmpty());
        Set<String> mapsTo = mappingsRepository.map(null, "FooBar", "CHEBI");
        assertEquals(1, mapsTo.size());
        assertEquals("CHEBI:12345", mapsTo.iterator().next());
	}
}
