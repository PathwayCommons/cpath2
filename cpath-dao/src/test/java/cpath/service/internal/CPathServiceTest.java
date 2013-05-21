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

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.apache.commons.logging.*;

import cpath.config.CPathSettings;
import cpath.dao.CPathUtils;
import cpath.dao.MetadataDAO;
import cpath.dao.PaxtoolsDAO;

import cpath.service.CPathService;
import cpath.service.ErrorResponse;
import cpath.service.OutputFormat;
import cpath.service.jaxb.DataResponse;
import cpath.service.jaxb.ServiceResponse;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Metadata.METADATA_TYPE;

import java.io.File;

import static org.junit.Assert.*;


public class CPathServiceTest {

    private static Log log = LogFactory.getLog(CPathServiceTest.class);

    static ClassPathXmlApplicationContext context;
    static final SimpleIOHandler io = new SimpleIOHandler(BioPAXLevel.L3);
    static final File idxDir = new File(CPathSettings.homeDir() + File.separator + CPathSettings.TEST_DB);
	
    @BeforeClass
    public static void init() {

    	log.info("Creating test DB " + CPathSettings.TEST_DB);
    	CPathUtils.createDatabase(CPathSettings.TEST_DB);
		
		context = new ClassPathXmlApplicationContext("classpath:testContext-dao.xml");
		
		PaxtoolsDAO dao = (PaxtoolsDAO) context.getBean("paxtoolsDAO"); //actually, will be used as "main" db
		MetadataDAO mdao = (MetadataDAO) context.getBean("metadataDAO");
		
		log.info("Loading test warehouse and model data...");
		Model m = io.convertFromOWL(CPathServiceTest.class.getResourceAsStream("/test3.owl"));
		Metadata md = new Metadata("test", "Reactome", "Foo", "", "", new byte[]{}, METADATA_TYPE.BIOPAX, "", "");
		
		mdao.saveMetadata(md);
		
		md.setProvenanceFor(m); // normally, this happens in PreMerge
		
		dao.merge(m);
		
		dao.index();
		
		log.info("Test init done.");
    }

	
	@Test
	public void testFetchBiopax() throws Exception {
		PaxtoolsDAO dao = (PaxtoolsDAO) context.getBean("paxtoolsDAO");
		CPathService service = new CPathServiceImpl(dao, null);
		ServiceResponse res = service.fetch(OutputFormat.BIOPAX, "http://identifiers.org/uniprot/P46880");
		assertNotNull(res);
		assertFalse(res instanceof ErrorResponse);
		assertTrue(res instanceof DataResponse);
		assertFalse(res.isEmpty());
		assertTrue(((DataResponse)res).getData().toString().length()>0);
	}
	
	
	@Test
	public void testFetchAsSIF() throws Exception {
		PaxtoolsDAO dao = (PaxtoolsDAO) context.getBean("paxtoolsDAO");
		CPathService service = new CPathServiceImpl(dao, null);
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

}
