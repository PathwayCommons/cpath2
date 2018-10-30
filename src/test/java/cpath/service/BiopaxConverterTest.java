package cpath.service;

import static org.junit.Assert.*;

import cpath.service.api.OutputFormat;
import cpath.service.jaxb.DataResponse;
import cpath.service.jaxb.ServiceResponse;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Pathway;
import org.junit.Test;


public class BiopaxConverterTest {

    @Test
    public final void testToJsonLd() {
        Model m = BioPAXLevel.L3.getDefaultFactory().createModel();
        m.setXmlBase("http://pathwaycommons.org/pc2/"); //Jena fails if not set!
        Pathway bpe = m.addNew(Pathway.class, "http://pathwaycommons.org/pc2/Pathway_test-URI");
        bpe.setDisplayName("My test pathway");
        bpe.addComment("Hello JSON-LD!");
        ServiceResponse sr = new BiopaxConverter(null).convert(m, OutputFormat.JSONLD, null);
        assertTrue(sr instanceof DataResponse);
        assertFalse(sr.isEmpty());
    }

}
