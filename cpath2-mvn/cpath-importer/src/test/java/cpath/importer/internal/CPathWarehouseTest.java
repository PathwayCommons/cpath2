package cpath.importer.internal;
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


import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import javax.imageio.ImageIO;

import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.validator.utils.Normalizer;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cpath.config.CPathSettings;
import cpath.dao.PaxtoolsDAO;
import cpath.dao.internal.DataServicesFactoryBean;
import cpath.importer.Fetcher;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.warehouse.*;
import cpath.warehouse.beans.*;

/**
 * Test the WarehouseDAO implementation,
 * except for CVs (tested in cpath-warehouse module),
 * using DAO and some test data 
 * 
 * @author rodche
 *
 */
public class CPathWarehouseTest {

	static WarehouseDAO warehouse;
	static BioPAXFactory factory;
	static MetadataDAO metadataDAO;
	
	static final String XML_BASE = CPathSettings.xmlBase();
	
	static {
		System.out.println("Preparing...");
		// init the test database
		DataServicesFactoryBean.createSchema("cpath2_test");
		
		// load beans
		ApplicationContext context = new ClassPathXmlApplicationContext(
			new String[]{"classpath:testContext-whDAO.xml"});
		warehouse = (WarehouseDAO) context.getBean("warehouseDAO");
		metadataDAO = (MetadataDAO) context.getBean("metadataDAO");
		
		// load test data
		Fetcher fetcher = new FetcherImpl();
        Collection<Metadata> metadata;
		try {
			metadata = fetcher.readMetadata("classpath:metadata.conf");
			for (Metadata mdata : metadata) {
				metadataDAO.importMetadata(mdata);
				fetcher.fetchData(mdata);
				if (mdata.getType().isNotPathwayData()) {
					PremergeImpl.storeWarehouseData(mdata, (Model) warehouse);
				} 
			}
			
			// id-mapping init
			ImportFactory.newPremerge(metadataDAO, (PaxtoolsDAO) warehouse, null, null).updateMappingData();
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		factory = BioPAXLevel.L3.getDefaultFactory();
		
		((PaxtoolsDAO)warehouse).index();
	}

		
	@Test
	public void testWarehouseData() throws IOException {
		assertFalse(((Model)warehouse).getObjects(ProteinReference.class).isEmpty());
		assertTrue(((Model)warehouse).containsID("http://identifiers.org/uniprot/P62158"));
		assertFalse(((Model)warehouse).getObjects(SmallMoleculeReference.class).isEmpty());
		assertTrue(((Model)warehouse).containsID("http://identifiers.org/chebi/CHEBI:20"));
	}
		
	
	@Test
	public void testGetProteinReference() {
		ProteinReference pr = warehouse
			.createBiopaxObject("http://identifiers.org/uniprot/P62158", ProteinReference.class);
		assertNotNull(pr);
		assertFalse(pr.getName().isEmpty());
		assertNotNull(pr.getOrganism());
		assertEquals("Homo sapiens", pr.getOrganism().getStandardName());
		assertFalse(pr.getXref().isEmpty());
		// more checks?..
		
		// generate an xref to search Warehouse with -
		// (it pretends to come from a pathway during merge...)
		Xref x = factory.create(UnificationXref.class, Normalizer.uri(XML_BASE, "UNIPROT", "A2A2M3", UnificationXref.class));
		x.setDbVersion("uniprot");
		x.setId("A2A2M3"); // not a primary accession ;)
		x.setDb("uniprot"); // db must be set for getByXref to work [since 19-May-2011]!
		
// method was removed
//		Set<String> prIds =  warehouse.findByXref(Collections.singleton(x), ProteinReference.class);
//		assertFalse(prIds.isEmpty());
//		assertEquals(1, prIds.size());
//		assertEquals("http://identifiers.org/uniprot/Q8TD86", prIds.iterator().next());		
//		// same but using uniprot.isoform, which must be internally converted to canonical to match anything -
//		x = factory.create(UnificationXref.class, Normalizer.uri(PROP_XML_BASE, "UniProt Isoform", "Q8TD86-1", UnificationXref.class));
//		x.setId("Q8TD86-1");
//		x.setDb("UniProt Isoform");
//		prIds =  warehouse.findByXref(Collections.singleton(x), ProteinReference.class);
//		assertFalse(prIds.isEmpty());
//		assertEquals(1, prIds.size());
//		assertEquals("http://identifiers.org/uniprot/Q8TD86", prIds.iterator().next());
		
		// id-mapping
		String ac = metadataDAO.getIdMapping("uniprot", "A2A2M3", GeneMapping.class).getAccession();
		assertEquals("Q8TD86", ac);
		assertTrue(((Model)warehouse).containsID("http://identifiers.org/uniprot/" + ac));
		
		assertNull(metadataDAO.getIdMapping("uniprot", "Q8TD86-1", GeneMapping.class));
		assertNull(metadataDAO.getIdMapping(null, "Q8TD86-1", GeneMapping.class));
		
		assertNotNull(metadataDAO.getIdMapping("uniprot isoform", "Q8TD86-1", GeneMapping.class)); //infers Q8TD86!
		ac = metadataDAO.getIdMapping("uniprot isoform", "Q8TD86-1", GeneMapping.class).getAccession();
		assertEquals("Q8TD86", ac);		
	}

	@Test
	public void testSearchForProteinReference() {
		// search with a secondary (RefSeq) accession number
		SearchResponse resp =  ((PaxtoolsDAO)warehouse).search("NP_619650", 0, RelationshipXref.class, null, null);
		Collection<SearchHit> prs = resp.getSearchHit();
		assertFalse(prs.isEmpty());
		Collection<String> prIds = new HashSet<String>();
		for(SearchHit e : prs) {
			prIds.add(e.getUri());
		}
		
		String uri = Normalizer.uri(XML_BASE, "REFSEQ", "NP_619650", RelationshipXref.class);
				
		assertTrue(prIds.contains(uri));
		
		// get that xref
		Xref x = warehouse.createBiopaxObject(uri, RelationshipXref.class);
		assertNotNull(x);
		assertTrue(x.getXrefOf().isEmpty()); // when elements are detached using getObject, they do not remember its owners!
		// if you get the owner (entity reference) by id, then this xref.xrefOf will contain the owner.
		
// method was removed
//		// search/map for the corresponding entity reference
//		prIds =  warehouse.findByXref(Collections.singleton(x), ProteinReference.class);
//		assertFalse(prIds.isEmpty());
//		assertEquals(1, prIds.size());
//		assertEquals("http://identifiers.org/uniprot/Q8TD86", prIds.iterator().next());
//		
//		// same but using uniprot.isoform, which must be internally converted to canonical to match anything -
//		x = factory.create(UnificationXref.class, Normalizer.uri(PROP_XML_BASE, "RefSeq", "NP_619650.2", UnificationXref.class));
//		x.setId("NP_619650.2");
//		x.setDb("RefSeq");
//		prIds =  warehouse.findByXref(Collections.singleton(x), ProteinReference.class);
//		assertFalse(prIds.isEmpty());
//		assertEquals(1, prIds.size());
//		assertEquals("http://identifiers.org/uniprot/Q8TD86", prIds.iterator().next());
		
		// alternatively -
		String ac = metadataDAO.getIdMapping("refseq", "NP_619650", GeneMapping.class).getAccession(); 
		assertNull(metadataDAO.getIdMapping(null, "NP_619650.1", GeneMapping.class));
		assertNotNull(metadataDAO.getIdMapping("refseq", "NP_619650.1", GeneMapping.class)); //used 'suggest' method internally to infer NP_619650!
		assertEquals("Q8TD86", ac);
		assertTrue(((Model)warehouse).containsID("http://identifiers.org/uniprot/" + ac));
	}

    @Test
	// just another test (not very useful...)
	public void testSubModel() {
		Model m =((PaxtoolsDAO)warehouse).getValidSubModel(
				Arrays.asList(
					Normalizer.uri(XML_BASE, "REFSEQ", "NP_619650", UnificationXref.class),
					"http://identifiers.org/uniprot/Q8TD86",
					Normalizer.uri(XML_BASE, "UNIPROT", "Q8TD86", UnificationXref.class),
					Normalizer.uri(XML_BASE, "UNIPROT", "A2A2M3", UnificationXref.class),
					Normalizer.uri(XML_BASE, "UNIPROT", "Q6Q2C4", UnificationXref.class),
					Normalizer.uri(XML_BASE, "ENTREZ GENE", "163688", UnificationXref.class)));
		// The following item should come from Q8TD86 ProteinReference
        // See TEST_UNIPROT*.gz file for the original reference
		assertTrue(m.containsID("http://identifiers.org/taxonomy/9606")); // added by auto-complete

		//(new SimpleIOHandler(BioPAXLevel.L3)).convertToOWL(m, System.out);
	}

    
	@Test
	public void testMetadataIcon() throws IOException {
		Metadata ds = metadataDAO.getMetadataByIdentifier("TEST_BIOPAX");
		assertNotNull(ds);
		
		byte[] icon = ds.getIcon();
		assertNotNull(icon);
		assertTrue(icon.length >0);
		
		BufferedImage bImageFromConvert = ImageIO.read(new ByteArrayInputStream(icon));
		ImageIO.write(bImageFromConvert, "gif", 
			new File(getClass().getClassLoader().getResource("").getPath() 
				+ File.separator + "out.gif"));
	}
}
