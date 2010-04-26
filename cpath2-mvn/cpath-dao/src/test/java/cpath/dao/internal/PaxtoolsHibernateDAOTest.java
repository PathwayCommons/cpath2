// $Id$
//------------------------------------------------------------------------------
/** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center.
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

// imports
import cpath.dao.PaxtoolsDAO;

import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.BiochemicalReaction;
import org.biopax.paxtools.model.level3.ChemicalStructure;
import org.biopax.paxtools.model.level3.Pathway;
import org.biopax.paxtools.model.level3.SmallMolecule;
import org.biopax.paxtools.model.level3.SmallMoleculeReference;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.paxtools.proxy.BioPAXElementProxy;
import org.biopax.paxtools.proxy.level3.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import org.apache.commons.logging.*;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Tests org.mskcc.cpath2.dao.hibernatePaxtoolsHibernateDAO.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:applicationContext-cpathDAO.xml"})
@TransactionConfiguration(transactionManager="mainTransactionManager")
public class PaxtoolsHibernateDAOTest {

    private static Log log = LogFactory.getLog(PaxtoolsHibernateDAOTest.class);

    @Autowired
    PaxtoolsDAO paxtoolsDAO;

	//
	// used to test getByID
	//
	private List<String> PATHWAY_TEST_VALUES = new ArrayList<String>();
	private List<String> PROTEIN_A_TEST_VALUES = new ArrayList<String>();
	private List<String> PROTEIN_B_TEST_VALUES = new ArrayList<String>();
	private List<List<String>> PROTEIN_TEST_VALUES = new ArrayList<List<String>>();
	private String[] UNIFICATION_TEST_VALUES = { "Gene Ontology", "GO:0005737" };
	private String GET_BY_QUERY_TEST_VALUE = "ATP";
	private Class GET_BY_QUERY_RETURN_TEST_CLASS = ChemicalStructureProxy.class;
	private List<Class> GET_BY_QUERY_RETURN_CLASSES = new ArrayList<Class>();

	@Before
	public void setUp() throws Exception {
		PATHWAY_TEST_VALUES.add("glycolysis");
		PATHWAY_TEST_VALUES.add("Glycolysis Pathway");
		PATHWAY_TEST_VALUES.add("Embden-Meyerhof pathway");
		PATHWAY_TEST_VALUES.add("glucose degradation");
		
		PROTEIN_A_TEST_VALUES.add("glucokinase");
		PROTEIN_A_TEST_VALUES.add("GLK");
		PROTEIN_A_TEST_VALUES.add("GLK_ECOLI");
		
		PROTEIN_B_TEST_VALUES.add("GPI");
		PROTEIN_B_TEST_VALUES.add("phosphoglucose isomerase");
		PROTEIN_B_TEST_VALUES.add("phosphohexose isomerase");
		PROTEIN_B_TEST_VALUES.add("PGI");
		PROTEIN_B_TEST_VALUES.add("PHI");
		PROTEIN_B_TEST_VALUES.add("glucose-6-phosphate isomerase");
		
		PROTEIN_TEST_VALUES.add(PROTEIN_A_TEST_VALUES);
		PROTEIN_TEST_VALUES.add(PROTEIN_B_TEST_VALUES);
		
		// model interfaces
		GET_BY_QUERY_RETURN_CLASSES.add(SmallMolecule.class);
		GET_BY_QUERY_RETURN_CLASSES.add(ChemicalStructure.class);
		GET_BY_QUERY_RETURN_CLASSES.add(BiochemicalReaction.class);
		GET_BY_QUERY_RETURN_CLASSES.add(SmallMoleculeReference.class);
		
	}

	@Test
	@Transactional
	//@Rollback(false) // once used - e.g., to dump/re-use the data in the cpath-web (import.sql) :)
	public void testRun() throws Exception {
		// verify a call to importModel
		log.info("Testing call to paxtoolsDAO.importModel()...");
		String cpathHome = System.getProperty("CPATH2_HOME");
		String separator = System.getProperty("file.separator");
		File biopaxFile = new File(getClass()
				.getResource("/biopax-level3-test.owl").getFile());
		paxtoolsDAO.importModel(biopaxFile, true);
		log.info("paxtoolsDAO.importModel() succeeded!");
		
		log.info("Testing call to paxtoolsDAO.getByID()...");
		BioPAXElement bpe = (BioPAXElement) paxtoolsDAO
			.getByID("http://www.biopax.org/examples/myExample#Pathway50", false, true); // get detached!
		
		assertTrue(bpe != null && bpe instanceof Pathway);
		
		Set<String> pathwayNames = ((Pathway)bpe).getName();
		
		assertTrue(pathwayNames.size() == PATHWAY_TEST_VALUES.size());
		
		for (String name : pathwayNames) {
			assertTrue(PATHWAY_TEST_VALUES.contains(name));
		}
		log.info("paxtoolsDAO.getByID() succeeded!");

		// verify a call to getObjects(Class<T> filterBy)
		log.info("Testing call to paxtoolsDAO.getObjects()...");
		Set<ProteinProxy> proteins = paxtoolsDAO.getObjects(ProteinProxy.class, false, false);
		
		assertTrue(proteins != null && proteins.size() == PROTEIN_TEST_VALUES.size());
		
		int lc = 0;
		for (ProteinProxy protein : proteins) {
			Set<String> names = protein.getName();
			List<String> proteinTestValues = PROTEIN_TEST_VALUES.get(lc++);
			
			assertTrue(names.size() == proteinTestValues.size());
			
			for (String name : names) {
				assertTrue(proteinTestValues.contains(name));
			}
		}
		log.info("paxtoolsDAO.getObjects() succeeded!");

		// verify a call to getByQueryString - filter by BioPAXElementProxy
		log.info("Testing first call to paxtoolsDAO.getByQueryString()...");
		List<Level3ElementProxy> returnClasses = paxtoolsDAO
			.search(GET_BY_QUERY_TEST_VALUE, Level3ElementProxy.class, false);
		Set<Class<? extends BioPAXElement>> uniqueClasses = new HashSet<Class<? extends BioPAXElement>>();
		for (BioPAXElement returnClass : returnClasses) {
			uniqueClasses.add(returnClass.getModelInterface());
			System.out.println(returnClass.toString() + " is " + returnClass.getModelInterface().getSimpleName());
		}
		
		assertEquals(GET_BY_QUERY_RETURN_CLASSES.size(), uniqueClasses.size());
		
		for (Class<? extends BioPAXElement> returnClass : uniqueClasses) {
			assertTrue(GET_BY_QUERY_RETURN_CLASSES.contains(returnClass));
		}
		log.info("paxtoolsDAO.getByQueryString() first call succeeded!");
		
		log.info("Testing second call to paxtoolsDAO.getByQueryString()...");
		// verify a call to getByQueryString - filter by GET_BY_QUERY_RETURN_TEST_CLASS
		returnClasses = paxtoolsDAO.search(GET_BY_QUERY_TEST_VALUE, GET_BY_QUERY_RETURN_TEST_CLASS, false);
		
		assertEquals(returnClasses.size(), 1);
		
		for (BioPAXElement returnClass : returnClasses) {
			assertTrue(returnClass.getClass() == GET_BY_QUERY_RETURN_TEST_CLASS);
		}
		log.info("paxtoolsDAO.getByQueryString() second call succeeded!");
		
		
		// verify object property is set
		SmallMoleculeReference smr = (SmallMoleculeReference) paxtoolsDAO
			.getByID("http://www.biopax.org/examples/myExample#SmallMoleculeReference_10", false, false);
		Set<Xref> xs = smr.getXref();
		assertFalse(xs.isEmpty());
		assertTrue(xs.size()==1);
		Xref x = xs.iterator().next();
		assertEquals("C00008", x.getId());
	}
}
