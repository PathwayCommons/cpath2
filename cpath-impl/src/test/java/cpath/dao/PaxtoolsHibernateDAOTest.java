
// $Id$
//------------------------------------------------------------------------------
/** Copyright (c) 2009-2011 Memorial Sloan-Kettering Cancer Center
 ** and University of Toronto.
 **
 ** This library is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** Memorial Sloan-Kettering Cancer Center
 ** has no obligations to provide maintenance, support,
 ** updates, enhancements or modifications.  In no event shall
 ** Memorial Sloan-Kettering Cancer Center
 ** be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** Memorial Sloan-Kettering Cancer Center
 ** has been advised of the possibility of such damage.  See
 ** the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this library; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 **/
package cpath.dao;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.validator.api.beans.Validation;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cpath.dao.MetadataDAO;
import cpath.dao.PaxtoolsDAO;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.warehouse.beans.Mapping;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.PathwayData;
import cpath.warehouse.beans.Metadata.METADATA_TYPE;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests org.mskcc.cpath2.dao.hibernatePaxtoolsHibernateDAO.
 */
@Ignore
public class PaxtoolsHibernateDAOTest {

    static Logger log = LoggerFactory.getLogger(PaxtoolsHibernateDAOTest.class);
    static SimpleIOHandler exporter = new SimpleIOHandler(BioPAXLevel.L3);
    static PaxtoolsDAO dao;
    static MetadataDAO meta;
    static ClassPathXmlApplicationContext ctx;
	

    @BeforeClass
	public static void init() throws FileNotFoundException {
    	ctx = new ClassPathXmlApplicationContext("classpath:testContext-1.xml");
    	dao = (PaxtoolsDAO) ctx.getBean("paxtoolsDAO");
    	// load some data into the test storage
		log.info("Loading test1 data...");
		dao.importModel(new File(PaxtoolsHibernateDAOTest.class.getResource("/test.owl").getFile()));
    	//import almost the same file to ensure it does not fail due to "duplicate entry for the key" ex.
		log.info("Loading test2 data...");
		dao.importModel(new File(PaxtoolsHibernateDAOTest.class.getResource("/test2.owl").getFile()));
    	meta = (MetadataDAO) ctx.getBean("metadataDAO");
	}
	
    @AfterClass
    public static void end() {
    	if(ctx!=null && ctx.isActive())
    		ctx.close();
    }
       
    
    @Test
	public void testInitialization() throws IOException {
		assertTrue(((Model)dao).containsID("http://identifiers.org/uniprot/P46880"));
		assertTrue(((Model)dao).containsID("http://www.biopax.org/examples/myExample2#Protein_A"));
		assertTrue(((Model)dao).containsID("http://www.biopax.org/examples/myExample#Protein_A"));
		assertTrue(((Model)dao).containsID("http://www.biopax.org/examples/myExample#Protein_B"));
		assertTrue(((Model)dao).containsID("UnificationXref:Taxonomy_562"));
		
		BioPAXElement bpe = ((Model)dao).getByID("UnificationXref:Taxonomy_562");
		assertTrue(bpe instanceof UnificationXref);
		
		BioPAXElement e = ((Model)dao)
				.getByID("http://www.biopax.org/examples/myExample2#Protein_A");
		assertTrue(e instanceof Protein);
		
		e = ((Model)dao) // try to initialize
				.getByID("http://www.biopax.org/examples/myExample2#Protein_A");
		dao.initialize(bpe);
		Protein p = (Protein) e;
				
		assertTrue(p.getEntityReference() != null);
		assertEquals("http://identifiers.org/uniprot/P46880", p.getEntityReference().getRDFId());
		
		// this would fail (lazy collections)
		//assertEquals(4, p.getEntityReference().getEntityReferenceOf().size());
			
		// but when -
		e = ((Model)dao) // try to initialize
				.getByID("http://identifiers.org/uniprot/P46880");
		dao.initialize(e);
		assertTrue(e instanceof ProteinReference);
		ProteinReference pr = (ProteinReference) e;
		assertNotNull(pr.getOrganism());
		// however, with using getByIdInitialized, next line would fail -
		// pr.getEntityReferenceOf().size();
		// assertEquals(4, pr.getEntityReferenceOf().size());
		
		// different approach works!
		pr = (ProteinReference) ((Model)dao).getByID("http://identifiers.org/uniprot/P46880");
		//pr.getEntityReferenceOf().size() would fail here, but...
		// initialize(bpe) can be called at any time (it's bidirectional, though not recursive)
		dao.initialize(pr);
		// should pass now :)
		assertEquals(4, pr.getEntityReferenceOf().size());
		assertEquals(2, pr.getName().size());
		//pr.getOrganism().getXref().size(); // would fail, hehe... but
		BioSource bs = pr.getOrganism();
		assertNotNull(bs);
		dao.initialize(bs);
		assertTrue(bs.getXref().size() > 0);
	}

	
	@Test // protein reference's xref's getXrefOf() is not empty
	public void testGetXReferableAndXrefOf() throws Exception {
		ProteinReference pr = (ProteinReference) ((Model)dao)
			.getByID("http://identifiers.org/uniprot/P46880");
		dao.initialize(pr);
		assertTrue(pr instanceof ProteinReference);
		assertFalse(pr.getXref().isEmpty());
		Xref x = pr.getXref().iterator().next();		
		dao.initialize(x);
		assertEquals(1, x.getXrefOf().size());
		System.out.println(x.getRDFId() + " is xrefOf " + 
				x.getXrefOf().iterator().next().toString()
		);
	}
	
	
	@Test // getXrefOf() returns empty set, but it's not a bug
	public void testGetXrefAndXrefOf() throws Exception {
		/* 
		 * getByID normally returns an object with lazy collections, 
		 * which is usable only within the session/transaction,
		 * which is closed after the call :) So 'initialize' is required - 
		 */
		BioPAXElement bpe = ((Model)dao).getByID("UnificationXref:UniProt_P46880");
		dao.initialize(bpe);
		assertTrue(bpe instanceof UnificationXref);
		
		OutputStream out = new ByteArrayOutputStream();
		dao.exportModel(out, bpe.getRDFId());
		
		// check if it has xrefOf values...
		Set<XReferrable> xrOfs = ((UnificationXref) bpe).getXrefOf();
		assertFalse(xrOfs.isEmpty());
	}
	
	
	@Test
	public void testGetByID() throws Exception {		
		// get a protein
		log.info("Testing PaxtoolsDAO as Model.getByID(id)");
		BioPAXElement bpe =  ((Model)dao).getByID(
				"http://www.biopax.org/examples/myExample#Protein_A");
		dao.initialize(bpe);
		assertTrue(bpe instanceof Protein);
		assertEquals("glucokinase A", ((Protein)bpe).getDisplayName());
		EntityReference er = ((Protein)bpe).getEntityReference();
		assertNotNull(er);
		dao.initialize(er);
		assertEquals(1, er.getXref().size());
		
		OutputStream out = new ByteArrayOutputStream();
		dao.exportModel(out, bpe.getRDFId());
	}
	
	
	@Test
	public void testSearch() throws Exception {
//		CPathUtils.cleanupIndexDir(CPathSettings.property(CPathSettings.TEST_DB));
		dao.index();
		
		SearchResponse resp = dao.search("xrefid:P46880", 0, UnificationXref.class, null, null);
		List<SearchHit> elist = resp.getSearchHit();
		assertEquals(1, elist.size());
		
		resp = dao.search("P46880", 0, UnificationXref.class, null, null);
		elist = resp.getSearchHit();
		assertEquals(1, elist.size());
		
		resp = dao.search("P46880", 0, BioPAXElement.class, null, null);
		List<SearchHit> list = resp.getSearchHit();
		assertFalse(list.isEmpty());
		Set<String> m = new HashSet<String>();
		for(SearchHit e : list) {
			m.add(e.getUri());
		}
		assertTrue(m.contains("UnificationXref:UniProt_P46880"));
		System.out.println("search by 'P46880' returned: " + list.toString());
		
		// PR must match if one of its xref.id matches the query -
		resp = dao.search("P46880", 0, ProteinReference.class, null, null);
		list = resp.getSearchHit();
		System.out.println("search by 'P46880', " +
			"filter by ProteinReference.class, returned: " + list.toString());
		assertEquals(1, list.size());
		
		resp = dao.search("glucokinase", 0, ProteinReference.class, null, null);
		list = resp.getSearchHit();
		assertEquals(1, list.size());
		assertTrue(list.get(0).getUri().equals("http://identifiers.org/uniprot/P46880"));
		
		
		
//    	/* This precious piece of code used to be a separate test method, which
//		 * once has helped understand a VERY important problem about Hibernate/Search Mass Indexer:
//		 * if failed/hang if a class has an ORM annotated/mapped 'id' property 
//		 * (as, e.g., Xref had @Field @Column getId() method) 
//		 * despite that there was also a primary key field defined, e.g., 
//		 * as "@Id @DocumentId public String getRDFId()..."! It was resolved by making getId() @Transient 
//		 * and creating another pair of @Field annotated getter/setter, getIdx()/setIdx(String).
//		 * So, "id" field/property name is better to avoid and use a more specific name instead!
//		 */
//		paxtoolsDAO.importModel(
//			(new DefaultResourceLoader()).getResource("classpath:xrefs.owl")
//				.getFile());
//		DataServicesFactoryBean.rebuildIndex("test_cpath2main");
//		
//		resp = paxtoolsDAO.findElements("P46880", 0, UnificationXref.class);
//		assertFalse(resp.getSearchHit().isEmpty());
//		assertEquals(1, resp.getSearchHit().size());
//		resp = paxtoolsDAO.findElements("9847135", 0, PublicationXref.class);
//		assertFalse(resp.getSearchHit().isEmpty());
//		assertEquals(1, resp.getSearchHit().size());		
	}	

	
	@Test
	public void testAddVeryLongURI() {
		char[] c = new char[333];
		Arrays.fill(c, 'a');	
		final String s = new String(c);
		assertEquals(333, s.length());
		// add a new object with URI longer than 256 bytes
		final String id1 = s;
		((Model)dao).addNew(Gene.class, id1);
		//check index works as we want it (case sensitive, 256 bytes long...)
		final String id2 = s.toUpperCase();
		((Model)dao).addNew(Gene.class, id2); 
		//SO we have a PROBLEM:  JDBCExceptionReporter  - Duplicate entry ... for key 1
		// Mysql PK index is case insensitive and only 64-chars long!
	}
	
	
	@Test
	public void testProvenanceCommentsNotMerged() {
		assertFalse(((Model)dao).getObjects(Provenance.class).isEmpty());
		Provenance pro = (Provenance) ((Model)dao).getByID("Provenance:aMAZE");
		assertNotNull(pro);
		dao.initialize(pro);
		//there were two Provenance objects with the same URI in the two imported test files;
		//hereby we prove that comments (as well as other data type props) are NOT merged automatically!
		assertEquals(1, pro.getComment().size());
		// in fact, the last imported object overwrites the first one:
		assertEquals("test2", pro.getComment().iterator().next());
	}    
 
	
	@Test
	public void testImportPathwayData() throws IOException {
        // mock metadata and pathway data
        Metadata md = new Metadata("TEST", "test", "test", "", "",
        		new byte[]{}, METADATA_TYPE.BIOPAX, null, null);        
        byte[] testData = "<rdf>          </rdf>".getBytes(); 
        
        //cleanup previous tests data if any
        md.cleanupOutputDir();
        
        PathwayData pathwayData = new PathwayData(md, "test0");
        pathwayData.setData(testData);
        pathwayData.setNormalizedData(testData);
        md.getPathwayData().add(pathwayData);
        //add the second pd (for the tests at the end of this method)
        final PathwayData pd = new PathwayData(md, "test1");
        pd.setData("aaaaaaaaaa".getBytes());
        md.getPathwayData().add(pd);
        
        // test if internal pack/unpach, if any, works well
        assertTrue(Arrays.equals(testData, pathwayData.getData()));
        assertTrue(Arrays.equals(testData, pathwayData.getNormalizedData()));
        
        // persist
        meta.saveMetadata(md);
        
        // test pathwaydata content is not accidentally erased
        Iterator<PathwayData> it = md.getPathwayData().iterator();
        pathwayData = it.next();
        //we want test0 for following assertions
        if("test1".equals(pathwayData.getFilename()))
        	pathwayData = it.next();
        assertEquals("test0",pathwayData.getFilename());    
        assertNotNull(pathwayData.getData()); // data is still there
        byte[] read = pathwayData.getNormalizedData();
        assertNotNull(read);
        assertTrue(Arrays.equals(testData, read)); 
        
        //even if we update from the db, data must not be empty
        md = meta.getMetadataByIdentifier(md.getIdentifier());
        assertNotNull(md);
        assertEquals("TEST", md.getIdentifier());
        assertEquals(2, md.getPathwayData().size()); 
        it = md.getPathwayData().iterator();
        pathwayData = it.next();
        //we want test0 for following assertions
        if("test1".equals(pathwayData.getFilename()))
        	pathwayData =it.next();
        assertEquals("test0",pathwayData.getFilename());
        //data (persisted to file system) survives re-assigning of 'md' variable
        assertNotNull(pathwayData.getData()); 
        //preperge data persisted in the file system
        read = pathwayData.getNormalizedData();        
        assertNotNull(read);
        assertTrue(Arrays.equals(testData, read));        

        // add validation result());  
        for(PathwayData o : md.getPathwayData())
        	o.setValidationReport(new Validation(null));        
        // update
        meta.saveMetadata(md);
         
        //read the latest state
        md = meta.getMetadataByIdentifier("TEST");
        assertNotNull(md);
        Set<PathwayData>  lpd = md.getPathwayData();
        assertFalse(lpd.isEmpty());
        pathwayData = lpd.iterator().next();
        assertNotNull(pathwayData);
        assertNotNull(pathwayData.getValidationReport()); //reads from file if needed
        assertTrue(pathwayData.getValidationReport().length > 0);    
        
        //cleanup
        md = meta.init(md);
        assertTrue(md.getPathwayData().isEmpty()); 
        md = meta.getMetadataByIdentifier("TEST");
        assertTrue(md.getPathwayData().isEmpty());         
	}

	
	@Test
	public void testImportIdMapping() {		
        Map<String,String> idMap = new TreeMap<String, String>();
        Mapping map = new Mapping(Mapping.Type.UNIPROT, "test", idMap);
        idMap.put("ZHX1", "P12345");
        idMap.put("ZHX1-C8orf76", "Q12345");  
        //capitalization is important in 99% of identifier types (we should not ignore it)
        // we should be able to save it and not get 'duplicate key' exception here
        idMap.put("ZHX1-C8ORF76", "Q12345"); 
        meta.saveMapping(map);
        
        //check it's saved
        assertEquals(1, meta.mapIdentifier("ZHX1-C8orf76", Mapping.Type.UNIPROT, null).size());
        assertEquals(1, meta.mapIdentifier("ZHX1-C8ORF76", Mapping.Type.UNIPROT, null).size());
        
        // repeat (should successfully update)
        idMap = new TreeMap<String, String>();
        idMap.put("FooBar", "CHEBI:12345");  
        map = new Mapping(Mapping.Type.CHEBI, "test2", idMap);
        //add new Mapping entity
        meta.saveMapping(map);
        assertTrue(meta.mapIdentifier("FooBar", Mapping.Type.UNIPROT, null).isEmpty());
        Set<String> mapsTo = meta.mapIdentifier("FooBar", Mapping.Type.CHEBI, null);
        assertEquals(1, mapsTo.size());
        assertEquals("CHEBI:12345", mapsTo.iterator().next());
	}
}
