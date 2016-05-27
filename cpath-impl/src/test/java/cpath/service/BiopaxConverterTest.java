package cpath.service;

import static org.junit.Assert.*;

import cpath.service.jaxb.DataResponse;
import cpath.service.jaxb.ServiceResponse;
//import org.apache.commons.io.IOUtils;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Pathway;
import org.junit.Test;

//import java.io.File;
//import java.io.FileInputStream;
import java.io.IOException;
//import java.nio.file.Path;

/**
 * Created by rodche on 2016-04-26.
 */

public class BiopaxConverterTest {

    @Test
    public final void testToJsonLd() throws IOException {
        Model m = BioPAXLevel.L3.getDefaultFactory().createModel();
        m.setXmlBase("http://pathwaycommons.org/pc2/"); //Jena fails if not set!
        Pathway bpe = m.addNew(Pathway.class, "http://pathwaycommons.org/pc2/Pathway_test-URI");
        bpe.setDisplayName("My test pathway");
        bpe.addComment("Hello JSON-LD!");
        ServiceResponse sr = new BiopaxConverter(null).convert(m, OutputFormat.JSONLD);
        assertTrue(sr instanceof DataResponse);
        assertFalse(sr.isEmpty());
        DataResponse dr = (DataResponse) sr;
//        System.out.println(dr.getData());
//        File f = ((Path) dr.getData()).toFile();
//        IOUtils.copyLarge(new FileInputStream(f), System.out); //works!
    }

}
