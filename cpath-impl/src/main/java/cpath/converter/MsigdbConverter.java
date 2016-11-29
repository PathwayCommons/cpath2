package cpath.converter;

import edu.mit.broad.vdb.msigdb.converter.MsigdbToBiopaxConverter;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.Model;

import java.io.InputStream;
import java.io.OutputStream;

public class MsigdbConverter extends BaseConverter {

    public void convert(InputStream is, OutputStream os) {
        MsigdbToBiopaxConverter converter = new MsigdbToBiopaxConverter();
        Model model;
        try {
            model = converter.convert("msigdb", is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert MsigDb (Transfac) XML to BioPAX", e);
        }

        new SimpleIOHandler().convertToOWL(model, os);
    }
}
