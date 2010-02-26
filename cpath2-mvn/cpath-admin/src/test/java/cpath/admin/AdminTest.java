package cpath.admin;

// imports
import cpath.fetcher.CPathFetcher;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.metadata.MetadataDAO;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:admin-test-context.xml")
public class AdminTest {

    private static Log log = LogFactory.getLog(AdminTest.class);

    @Autowired
    private CPathFetcher cpathFetcher;

    @Autowired
    private MetadataDAO metadataDAO;

	Admin admin;

	@Before
	public void setUp() throws Exception {
        //String[] args = { "-fetch-metadata", "http://cbio.mskcc.org/~grossb/DataProviderPage.html"};
        //admin = new Admin(args, cpathFetcher, metadataDAO);
    }

	@Test
	@Transactional(propagation=Propagation.SUPPORTS)
	public void testRun() throws Exception {

        //admin.run();
		fetchMetadataTest();
    }

	private void fetchMetadataTest() throws Exception {

		String url = "http://cbio.mskcc.org/~grossb/DataProviderPage.html";

        // grab the data
        Collection<Metadata> metadata = cpathFetcher.getProviderMetadata(url);
		assertTrue(metadata.size() == 1);
        
        // process metadata
        for (Metadata mdata : metadata) {
            metadataDAO.importMetadata(mdata);
        }

		Metadata reactomeMetadata = metadataDAO.getByIdentifier("REACTOME");
		assertTrue(reactomeMetadata != null);
		assertTrue(reactomeMetadata.getIdentifier().equals("REACTOME"));
	}
}