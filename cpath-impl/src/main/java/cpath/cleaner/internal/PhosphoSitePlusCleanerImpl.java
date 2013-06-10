package cpath.cleaner.internal;

import cpath.importer.Cleaner;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.SequenceModificationVocabulary;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;

public class PhosphoSitePlusCleanerImpl implements Cleaner {
    private static final Logger log = LoggerFactory.getLogger(PhosphoSitePlusCleanerImpl.class);

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

            // Now let's put it in a write-read cycle to make this effective
            ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
            simpleReader.convertToOWL(model,tmpStream);
            model = simpleReader.convertFromOWL(new ByteArrayInputStream(tmpStream.toByteArray()));
        }

        // Old terms -> replaced terms
        HashMap<String, String> termMap = new HashMap<String, String>();
        termMap.put("MI:0176", "MOD:00046");
        termMap.put("MI:0177", "MOD:00047");
        termMap.put("MI:0178", "MOD:00048");

        log.debug("Fixing isoform specific UniProt and a few old PSI-MI Ids...");
        for (UnificationXref xref : model.getObjects(UnificationXref.class)) {
        	if(xref.getDb()==null || xref.getId()==null)
        		continue;
        	
        	
// no need to do this in the cleaner or at all (normalizer/merger now takes care)
//            // Get rid of isoform specific notation: Q9EQS9-3 --> Q9EQS9
//            if(xref.getDb().startsWith("UniProt") && xref.getId().contains("-")) {
//                String isoformId = xref.getId();
//                xref.setId(isoformId.split("-")[0]);
//                log.trace("Converting " + isoformId + " to " + xref.getId());
//            // Replace some of the "nice-to-have" terms
//            } else 
            if(xref.getDb().toUpperCase().startsWith("PSI-MI") && xref.getId().startsWith("MI")) {
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

// no need to fix provenance anymore (it's to be replaced in Premerger anyway)
//        // And this is to make the Provenance compatible with Miriam
//        String standardName = "PhosphoSitePlus";
//        for (Provenance provenance : model.getObjects(Provenance.class)) {
//            String displayName = provenance.getDisplayName();
//            if(displayName != null && displayName.startsWith("Phosphosite")) {
//                log.trace("Replacing Provenance displayName " + displayName + " with " + standardName);
//                // http://www.ebi.ac.uk/miriam/main/collections/MIR:00000105
//                provenance.setDisplayName(standardName);
//            }
//        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            simpleReader.convertToOWL(model, outputStream);
        }
        catch (Exception e) {
            throw new RuntimeException("clean(), Exception thrown while cleaning pathway data", e);
        }

        return outputStream.toString();
    }
}
