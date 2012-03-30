package cpath.cleaner.internal;

import cpath.importer.Cleaner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.SequenceModificationVocabulary;
import org.biopax.paxtools.model.level3.UnificationXref;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;

public class PhosphoSitePlusCleanerImpl implements Cleaner {
    private static final Log log = LogFactory.getLog(PhosphoSitePlusCleanerImpl.class);

    @Override
    public String clean(String pathwayData) {
		InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(pathwayData.getBytes()));
		SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
		Model model = simpleReader.convertFromOWL(inputStream);

        // something to do with the way they create the OWL file.
        if(model.getXmlBase() == null) {
            String xmlBase = "http://www.phosphosite.org/phosphosite.owl#";
            log.debug("Xml base was null. Setting it to '" + xmlBase + "'");
            model.setXmlBase(xmlBase);
        }

        // Old terms -> replaced terms
        HashMap<String, String> termMap = new HashMap<String, String>();
        termMap.put("MI:0176", "MOD:00046");
        termMap.put("MI:0177", "MOD:00047");
        termMap.put("MI:0178", "MOD:00048");

        log.debug("Fixing isoform specific UniProt and a few old PSI-MI Ids...");
        for (UnificationXref xref : model.getObjects(UnificationXref.class)) {
            // Get rid of isoform specific notation: Q9EQS9-3 --> Q9EQS9
            if(xref.getDb().startsWith("UniProt") && xref.getId().contains("-")) {
                String isoformId = xref.getId();
                xref.setId(isoformId.split("-")[0]);
                log.trace("Converting " + isoformId + " to " + xref.getId());
            // Replace some of the "nice-to-have" terms
            } else if(xref.getDb().startsWith("PSI-MI") && xref.getId().startsWith("MI")) {
                String newId = termMap.get(xref.getId());
                if(newId != null)
                    xref.setId(newId);
            }
        }

        log.debug("Fixing phosphorylation terms -- adding an 'L' for MOD compatibility...");
        // Fix phosphorylation terms
        for (SequenceModificationVocabulary vocabulary : model.getObjects(SequenceModificationVocabulary.class)) {
            HashSet<String> oldTerms = new HashSet<String>(vocabulary.getTerm());
            for (String term : oldTerms) {
                String pTerm = term.replace("phospho-", "phospho-L-");
                vocabulary.removeComment(term);
                vocabulary.addTerm(pTerm);
            }
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            simpleReader.convertToOWL(model, outputStream);
        }
        catch (Exception e) {
            throw new RuntimeException("clean(), Exception thrown while cleaning pathway data!", e);
        }

        return outputStream.toString();
    }
}
