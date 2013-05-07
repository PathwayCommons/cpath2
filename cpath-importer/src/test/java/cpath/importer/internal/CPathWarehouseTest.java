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
import java.util.Collection;
import java.util.HashSet;

import javax.imageio.ImageIO;

import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.validator.utils.Normalizer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cpath.config.CPathSettings;
import cpath.dao.CPathUtils;
import cpath.dao.MetadataDAO;
import cpath.dao.PaxtoolsDAO;
import cpath.importer.Premerger;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.warehouse.beans.*;

/**
 * Test the WarehouseDAO implementation,
 * except for CVs (which is tested in cpath-dao module),
 * using DAO and some test data 
 * 
 * @author rodche
 *
 */
public class CPathWarehouseTest {

	static final String XML_BASE = CPathSettings.xmlBase();
	
	@BeforeClass
	public static void init() {
		System.out.println("Preparing...");

		CPathUtils.createDatabase(CPathSettings.TEST_DB); //drop/make an empty db
		CPathUtils.deleteDirectory(new File(CPathSettings.homeDir() + 
				File.separator + CPathSettings.TEST_DB));
		
		// load beans
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
			new String[]{"classpath:testContext-dao.xml"});
		PaxtoolsDAO warehouse = (PaxtoolsDAO) context.getBean("paxtoolsDAO");
		MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");
		
		//fetch and save test metadata and files
		metadataDAO.importMetadata("classpath:metadata.conf");	
			
		// build the test warehouse and id-mapping tables
		Premerger premerger = new PremergeImpl(metadataDAO, warehouse, null, null);
		premerger.buildWarehouse();
		premerger.updateIdMapping(false);

		// re-index all
		warehouse.index();
		
		context.close();
	}

		
	
	@Test
	public void test() {
		//reload app. context and DAO
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				new String[]{"classpath:testContext-dao.xml"});
		PaxtoolsDAO warehouse = (PaxtoolsDAO) context.getBean("paxtoolsDAO");
		MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");
		
		assertFalse(((Model)warehouse).getObjects(ProteinReference.class).isEmpty());
		assertTrue(((Model)warehouse).containsID("http://identifiers.org/uniprot/P62158"));
		assertFalse(((Model)warehouse).getObjects(SmallMoleculeReference.class).isEmpty());
		assertTrue(((Model)warehouse).containsID("http://identifiers.org/chebi/CHEBI:20"));				
							
		ProteinReference pr = (ProteinReference) ((Model)warehouse).getByID("http://identifiers.org/uniprot/P62158");
		warehouse.initialize(pr);
		warehouse.initialize(pr.getName());
		warehouse.initialize(pr.getXref());
		assertNotNull(pr);
		assertFalse(pr.getName().isEmpty());
		assertNotNull(pr.getOrganism());
		assertEquals("Homo sapiens", pr.getOrganism().getStandardName());
		assertFalse(pr.getXref().isEmpty());
		// more checks?..
		
		// id-mapping
		String ac = metadataDAO.mapIdentifier("A2A2M3", Mapping.Type.UNIPROT, "uniprot").iterator().next(); 
		assertEquals("Q8TD86", ac);
		assertTrue(((Model)warehouse).containsID("http://identifiers.org/uniprot/" + ac));
		
		assertTrue(metadataDAO.mapIdentifier("Q8TD86-1", Mapping.Type.UNIPROT, null).isEmpty());
		assertTrue(metadataDAO.mapIdentifier("Q8TD86-1", Mapping.Type.UNIPROT, "uniprot").isEmpty());
		
		//infers Q8TD86
		assertFalse(metadataDAO.mapIdentifier("Q8TD86-1", Mapping.Type.UNIPROT, "uniprot isoform").isEmpty());
		assertEquals("Q8TD86", metadataDAO.mapIdentifier("Q8TD86-1", Mapping.Type.UNIPROT, "uniprot isoform").iterator().next());			
		
		
		// Test full-text search works
		
		// search with a secondary (RefSeq) accession number
		SearchResponse resp =  warehouse.search("NP_619650", 0, RelationshipXref.class, null, null);
		Collection<SearchHit> prs = resp.getSearchHit();
		assertFalse(prs.isEmpty());
		Collection<String> prIds = new HashSet<String>();
		for(SearchHit e : prs) {
			prIds.add(e.getUri());
		}
		
		String uri = Normalizer.uri(XML_BASE, "REFSEQ", "NP_619650", RelationshipXref.class);
				
		assertTrue(prIds.contains(uri));
		
		// get that xref
		Xref x = (RelationshipXref) ((Model)warehouse).getByID(uri);
		assertNotNull(x);
		warehouse.initialize(x);
		warehouse.initialize(x.getXrefOf());
		assertFalse(x.getXrefOf().isEmpty()); 
		
		// alternatively -
		ac = metadataDAO.mapIdentifier("NP_619650", Mapping.Type.UNIPROT, "refseq").iterator().next(); 
		assertTrue(metadataDAO.mapIdentifier("NP_619650.1", Mapping.Type.UNIPROT, null).isEmpty());
		assertFalse(metadataDAO.mapIdentifier("NP_619650.1", Mapping.Type.UNIPROT, "refseq").isEmpty()); //used 'suggest' method internally to infer NP_619650
		assertEquals("Q8TD86", ac);
		assertTrue(((Model)warehouse).containsID("http://identifiers.org/uniprot/" + ac));
			
		context.close();
	}

	
	@Test
	public void testMetadataIcon() throws IOException {
		//reload app. context and DAO
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				new String[]{"classpath:testContext-dao.xml"});
		MetadataDAO metadataDAO = (MetadataDAO) context.getBean("metadataDAO");
		
		Metadata ds = metadataDAO.getMetadataByIdentifier("TEST_BIOPAX");
		assertNotNull(ds);
		
		byte[] icon = ds.getIcon();
		assertNotNull(icon);
		assertTrue(icon.length >0);
		
		BufferedImage bImageFromConvert = ImageIO.read(new ByteArrayInputStream(icon));
		ImageIO.write(bImageFromConvert, "gif", 
			new File(getClass().getClassLoader().getResource("").getPath() 
				+ File.separator + "out.gif"));
		
		context.close();
	}

}
