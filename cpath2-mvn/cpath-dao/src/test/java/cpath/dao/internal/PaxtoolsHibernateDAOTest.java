
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
package cpath.dao.internal;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.junit.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.apache.commons.logging.*;

import cpath.dao.PaxtoolsDAO;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.warehouse.WarehouseDAO;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests org.mskcc.cpath2.dao.hibernatePaxtoolsHibernateDAO.
 */
public class PaxtoolsHibernateDAOTest {

    static Log log = LogFactory.getLog(PaxtoolsHibernateDAOTest.class);
    static PaxtoolsDAO dao;
    static SimpleIOHandler exporter = new SimpleIOHandler(BioPAXLevel.L3);

	/* test methods will use the same data (read-only, 
	 * with one exception: testImportingAnotherFileAndTestInitialization
	 * imports the same data again...)
	 */
    static {
		// init the DAO (it loads now because databases are created above)
    	DataServicesFactoryBean.createSchema("test_cpath2main");
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"classpath:testContext-pcDAO.xml");
		dao = (PaxtoolsDAO) context.getBean("pcDAO");
		
		// load some data into the test storage
		log.info("Loading BioPAX data (importModel(file))...");
		try {
	    	/* import two files to ensure it does not fail
	    	 * (suspecting a "duplicate entry for the key (rdfid)" exception) */
			dao.importModel(new File(PaxtoolsHibernateDAOTest.class.getResource("/test.owl").getFile()));
			dao.importModel(new File(PaxtoolsHibernateDAOTest.class.getResource("/test2.owl").getFile()));
		} catch (FileNotFoundException e) {
			fail("Test file not found!");
		}
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
    
    
	@Test
	public void testSimple() throws Exception {
		log.info("Testing PaxtoolsDAO as Model.getByID(id)");
		BioPAXElement bpe = ((Model)dao)
			.getByID("http://www.biopax.org/examples/myExample#Protein_A");
		assertTrue(bpe instanceof Protein);
		
		bpe = ((Model)dao).getByID("UnificationXref:UniProt_P46880");
		assertTrue(bpe instanceof UnificationXref);
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
	
	
	@Test // getXrefOf() returns empty set, but it's not a bug!
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
		//System.out.println("Export single Xref (incomplete BioPAX):");
		//System.out.println(out.toString());
		
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
		//System.out.println("Export single protein (incomplete BioPAX):");
		//System.out.println(out.toString());
	}
	
	
	@Test
	public void testGetValidSubModel() throws Exception {	
		Model m =  dao.getValidSubModel(
	        Collections.singleton("http://www.biopax.org/examples/myExample#Protein_A"));
		System.out.println("Clone the protein and export model:");
		assertTrue(m.containsID("http://www.biopax.org/examples/myExample#Protein_A"));
		assertTrue(m.containsID("http://identifiers.org/uniprot/P46880"));
		assertTrue(m.containsID("UnificationXref:UniProt_P46880"));
		
		OutputStream out = new FileOutputStream(
				getClass().getClassLoader().getResource("").getPath() 
					+ File.separator + "testGetValidSubModel.out.owl");
		exporter.convertToOWL(m, out);
	}

	
	@Test
	public void testSearch() throws Exception {
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
//		 * once has helped to catch/understand a VERY important problem about Hibernate/Search Mass Indexer:
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
	
	
	public void testIsWhDAOInstance() {
		assertTrue(dao instanceof PaxtoolsDAO);
	    assertTrue(dao instanceof WarehouseDAO);
	}
	    
		
	@Test
	public void testWarehouseDAO() throws IOException {
		assertTrue(((Model)dao).containsID("http://identifiers.org/uniprot/P46880"));
		assertTrue(((Model)dao).containsID("http://www.biopax.org/examples/myExample2#Protein_A"));
		assertTrue(((Model)dao).containsID("http://www.biopax.org/examples/myExample#Protein_A"));
		assertTrue(((Model)dao).containsID("http://www.biopax.org/examples/myExample#Protein_B"));
		assertTrue(((Model)dao).containsID("UnificationXref:Taxonomy_562"));
			
		BioPAXElement bpe = ((WarehouseDAO) dao).createBiopaxObject("UnificationXref:Taxonomy_562", UnificationXref.class);
		assertTrue(bpe instanceof UnificationXref);
		BioPAXElement e = ((WarehouseDAO) dao).createBiopaxObject("http://www.biopax.org/examples/myExample2#Protein_A", BioPAXElement.class);
		assertTrue(e instanceof Protein);
		
		Protein p = ((WarehouseDAO) dao).createBiopaxObject("http://www.biopax.org/examples/myExample2#Protein_A", Protein.class);
		assertTrue(p.getEntityReference() != null);
		assertEquals("http://identifiers.org/uniprot/P46880", p.getEntityReference().getRDFId());
			
		e = ((WarehouseDAO) dao).createBiopaxObject("http://identifiers.org/uniprot/P46880", ProteinReference.class);
		assertTrue(e instanceof ProteinReference);
		ProteinReference pr = (ProteinReference) e;
		assertNotNull(pr.getOrganism());
			
		// WarehouseDAO.getObject cannot get inverse props for the object itself!
		assertTrue(pr.getEntityReferenceOf().isEmpty()); 
		assertEquals(2, pr.getName().size());

		BioSource bs = pr.getOrganism();
		assertNotNull(bs);
		assertTrue(bs.getXref().size() > 0);
	}
	    
	@Test
	public void testSimpleWh() throws Exception {
		log.info("Testing PaxtoolsDAO as Model.getByID(id)");
		BioPAXElement bpe = ((WarehouseDAO) dao)
				.createBiopaxObject("http://www.biopax.org/examples/myExample#Protein_A", BioPAXElement.class);
		assertTrue(bpe instanceof Protein);

		bpe = ((WarehouseDAO) dao).createBiopaxObject(
				"UnificationXref:UniProt_P46880",
				UnificationXref.class);
		assertTrue(bpe instanceof UnificationXref);
	}

	@Test
	// protein reference's xref's getXrefOf() is not empty
	public void testWarehouseXReferrableXrefOf() throws Exception {
		ProteinReference pr = ((WarehouseDAO) dao)
			.createBiopaxObject("http://identifiers.org/uniprot/P46880",
				ProteinReference.class);
		assertTrue(pr instanceof ProteinReference);
		assertFalse(pr.getXref().isEmpty());
		Xref x = pr.getXref().iterator().next();
		Set<XReferrable> xrOfs = x.getXrefOf();
		assertEquals(1, xrOfs.size());
		System.out.println(x.getRDFId() + " is xrefOf "
				+ x.getXrefOf().iterator().next().toString());
	}

	@Test
	// getXrefOf() returns empty set, but it's not a bug!
	public void testWarehouseXrefAndXrefOf() throws Exception {
		/*
		 * getByID would return an object with lazy collections, which is usable
		 * only within the session/transaction, which is closed after the call
		 * :) So we use getObject instead -
		 */
		BioPAXElement bpe = ((WarehouseDAO) dao).createBiopaxObject(
				"UnificationXref:UniProt_P46880",
				UnificationXref.class);
		assertTrue(bpe instanceof UnificationXref);

		// if the element can be exported like this, it's fully initialized...
		StringWriter writer = new StringWriter();
		exporter.writeObject(writer, bpe);
		// System.out.println("Export single Xref (incomplete BioPAX):");
		// System.out.println(writer.toString());

		// check if it has xrefOf values...
		Set<XReferrable> xrOfs = ((UnificationXref) bpe).getXrefOf();
		assertTrue(xrOfs.isEmpty()); // EMPTY when the xref is returned by getObject!
	}

	@Test
	public void testGetObject() throws Exception {
		// get a protein
		log.info("Testing WarehouseDAO.getObject(id, clazz)");
		BioPAXElement bpe = ((WarehouseDAO) dao).createBiopaxObject(
				"http://www.biopax.org/examples/myExample#Protein_A",
				Protein.class);

		assertTrue(bpe instanceof Protein);
		assertEquals("glucokinase A", ((Protein) bpe).getDisplayName());
		assertNotNull(((Protein) bpe).getEntityReference());
		assertEquals(1, ((Protein) bpe).getEntityReference().getXref().size());

		// if the element can be exported like this, it's fully initialized...
		StringWriter writer = new StringWriter();
		exporter.writeObject(writer, bpe);
		// System.out.println("Export single protein (incomplete BioPAX):");
		// System.out.println(writer.toString());
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
		assertEquals(1, ((Model)dao).getObjects(Provenance.class).size());
		Provenance pro = (Provenance) ((Model)dao).getByID("Provenance:aMAZE");
		assertNotNull(pro);
		dao.initialize(pro);
		//there were two Provenance objects with the same URI in the two imported test files;
		//hereby we prove that comments (as well as other data type props) are NOT merged automatically!
		assertEquals(1, pro.getComment().size());
		// in fact, the last imported object overwrites the first one:
		assertEquals("test2", pro.getComment().iterator().next());
	}
	
}
