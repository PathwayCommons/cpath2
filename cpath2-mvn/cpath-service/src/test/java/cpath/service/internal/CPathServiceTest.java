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
package cpath.service.internal;

// imports
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.apache.commons.logging.*;

import cpath.dao.PaxtoolsDAO;
import cpath.dao.internal.DataServicesFactoryBean;
import cpath.service.CPathService;
import cpath.service.OutputFormat;
import cpath.service.jaxb.DataResponse;
import cpath.service.jaxb.ErrorResponse;
import cpath.service.jaxb.ServiceResponse;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Metadata.TYPE;

import java.io.IOException;

import static org.junit.Assert.*;


public class CPathServiceTest {

    private static Log log = LogFactory.getLog(CPathServiceTest.class);

    static ApplicationContext context;
    static final SimpleIOHandler io = new SimpleIOHandler(BioPAXLevel.L3);
	
    static {
    	DataServicesFactoryBean.createSchema("cpath2_testpc");
		context = new ClassPathXmlApplicationContext(
				new String[] {"classpath:testContext-pcDAO.xml", 
					"classpath:testContext-whDAO.xml"});
		PaxtoolsDAO dao = (PaxtoolsDAO) context.getBean("pcDAO");
		MetadataDAO mdao = (MetadataDAO) context.getBean("metadataDAO");
		log.info("Loading BioPAX data (importModel(file))...");
		Model m = io.convertFromOWL(CPathServiceTest.class
				.getResourceAsStream("/test.owl"));
		Metadata md = new Metadata("test", "Reactome", "00", 
				"Foo", "", "", new byte[]{}, TYPE.BIOPAX, "", "");
		mdao.importMetadata(md);
		md.setProvenanceFor(m); // normally, this happens in PreMerge
		dao.merge(m);
		dao.index();
    }
	
	
	@Test
	public void testFetchBiopaxModel() throws Exception {
		PaxtoolsDAO dao = (PaxtoolsDAO) context.getBean("pcDAO");
		CPathServiceImpl service = new CPathServiceImpl(dao, null, null, null);		
		Model m = service.fetchBiopaxModel("http://www.biopax.org/examples/myExample#Protein_A");
		assertNotNull(m);
		BioPAXElement e = m.getByID("http://www.biopax.org/examples/myExample#Protein_A");
		assertTrue(e instanceof Protein);
		
	}

	
	@Test
	public void testFetchBiopax() throws Exception {
		PaxtoolsDAO dao = (PaxtoolsDAO) context.getBean("pcDAO");
		CPathService service = new CPathServiceImpl(dao, null, new OutputFormatConverterImpl(null), null);
		ServiceResponse res = service.fetch(OutputFormat.BIOPAX, "http://identifiers.org/uniprot/P46880");
		assertNotNull(res);
		assertFalse(res instanceof ErrorResponse);
		assertTrue(res instanceof DataResponse);
		assertFalse(res.isEmpty());
		assertTrue(((DataResponse)res).getData().toString().length()>0);
	}

	
	// TODO add 'find' tests that use different strings (matching different biopax fields)
	
	@Test
	public void testFetchAsSIF() throws Exception {
		PaxtoolsDAO dao = (PaxtoolsDAO) context.getBean("pcDAO");
		CPathService service = new CPathServiceImpl(dao, null, new OutputFormatConverterImpl(null), null);
		ServiceResponse res = service.fetch(
				OutputFormat.BINARY_SIF,
				"http://www.biopax.org/examples/myExample#biochemReaction1");
		assertNotNull(res);
		assertFalse(res instanceof ErrorResponse);
		assertFalse(res.isEmpty());
		assertTrue(res instanceof DataResponse);
		String data = (String) ((DataResponse)res).getData();		
		assertNotNull(data);

		System.out.println(data);
		assertTrue(data.contains("REACTS_WITH"));
		assertFalse(data.contains("Protein_A"));
		assertTrue(data.contains("http://identifiers.org/uniprot/P46880"));
	}
	
	
	@Test
	public void testDataAndBioSources() throws IOException {
		PaxtoolsDAO dao = (PaxtoolsDAO) context.getBean("pcDAO");
		MetadataDAO mdao = (MetadataDAO) context.getBean("metadataDAO");
		CPathService service = new CPathServiceImpl(dao, mdao, null, null);
		
		assertFalse(service.bioSources().isEmpty());
		assertFalse(service.dataSources().isEmpty());
		
		assertEquals(1, service.bioSources().getSearchHit().size());
		assertEquals(1, service.dataSources().getSearchHit().size());
		
	}
}
