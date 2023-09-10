package cpath.converter;

import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.humanmetabolism.converter.SbmlToBiopaxConverter;

import javax.xml.stream.XMLStreamException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by igor on 24/11/16.
 */
public class ReconxConverter extends BaseConverter {

    public void convert(InputStream is, OutputStream os) {
        Model bpModel;
        SbmlToBiopaxConverter sbmlToBiopaxConverter = new SbmlToBiopaxConverter();
        try {
            sbmlToBiopaxConverter.setMakePathway(false);
            //do not produce root/top pathway
            bpModel = sbmlToBiopaxConverter.convert(is);
        } catch (XMLStreamException e) {
            throw new RuntimeException("Failed to convert Recon2 SBML to BioPAX.", e);
        }

        new SimpleIOHandler(BioPAXLevel.L3).convertToOWL(bpModel, os);
    }
}
