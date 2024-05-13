package cpath.service;

import static org.junit.jupiter.api.Assertions.*;

import cpath.service.api.OutputFormat;
import cpath.service.jaxb.DataResponse;
import cpath.service.jaxb.ServiceResponse;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Pathway;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;


public class BiopaxConverterTest {

    @Test
    public final void testToJsonld() {
        Model m = BioPAXLevel.L3.getDefaultFactory().createModel();
        m.setXmlBase("http://pathwaycommons.org/pc2/"); //Jena fails if not set!
        Pathway bpe = m.addNew(Pathway.class, "http://pathwaycommons.org/pc2/Pathway_test-URI");
        bpe.setDisplayName("My test pathway");
        bpe.addComment("Hello JSON-LD!");
        ServiceResponse sr = new BiopaxConverter(null).convert(m, OutputFormat.JSONLD, null);
        assertTrue(sr instanceof DataResponse);
        assertFalse(sr.isEmpty());
    }

    @Test
    public final void testDemo1ToJsonld() throws IOException {
        Model m = new SimpleIOHandler().convertFromOWL(getClass().getResourceAsStream("/demo-pathway.owl"));
        ServiceResponse sr = new BiopaxConverter(null).convert(m, OutputFormat.JSONLD, null);

        assertTrue(sr instanceof DataResponse && !sr.isEmpty());

        String resf = ((DataResponse)sr).getData().toString(); //must be a temp. file name
        String res = Files.readString(Paths.get(resf));

        Assertions.assertAll(
            () -> Assertions.assertThrows(IllegalArgumentException.class, () -> URI.create("http://")), //bad URI
            () -> Assertions.assertTrue(res.contains("@id\" : \"http://bioregistry.io/chebi:20")),
            () -> Assertions.assertTrue(res.contains("@id\" : \"http://bioregistry.io/mi:0361")),//as long as it has 'http://' (valid abs. uri w/o schema would fail here due Jena bug)
            () -> Assertions.assertTrue(res.contains("@id\" : \"chebi:20")), //CURIE of a standard/normalized SMR's UnificationXref
            () -> Assertions.assertTrue(res.contains("\"@id\" : \"http://www.biopax.org/release/biopax-level3.owl#displayName\"")),
            () -> Assertions.assertTrue(res.contains("\"displayName\" : \"(+)-camphene\"")), //chebi:20
            () -> Assertions.assertTrue(res.contains("\"id\" : \"CHEBI:20\"")) //with jena v3 (e.g. 3.2.0 or 3.17.0)
        );
    }

}
