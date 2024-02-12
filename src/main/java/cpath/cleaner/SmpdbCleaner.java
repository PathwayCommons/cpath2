package cpath.cleaner;

import cpath.service.api.Cleaner;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.level3.Process;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * Implementation of Cleaner interface for the SMPDB BioPAX L3 pathway data
 * and Pathbank (similar to SMPDB).
 */
final class SmpdbCleaner implements Cleaner {

  public void clean(InputStream data, OutputStream cleanedData) {
    // create bp model from dataFile
    SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
    Model model = simpleReader.convertFromOWL(data);

// As we managed to get only human data archive from SMPDB there is no need for filtering by organism anymore -
    if (!model.containsID(model.getXmlBase() + "Reference/TAXONOMY_9606")
      && !model.containsID(model.getXmlBase() + "Reference/Taxonomy_9606")
      && !model.getObjects(BioSource.class).isEmpty())
      throw new RuntimeException("Highly likely non-human datafile (skip).");

    // Normalize Pathway URIs KEGG stable id, where possible
    Set<Pathway> pathways = new HashSet<>(model.getObjects(Pathway.class));
    for (Pathway pw : pathways) {
      //since Apr-2018, top/main pathway URIs are there already normalized (true for Pathbank 2019 as well)

      for (PathwayStep step : new HashSet<>(pw.getPathwayOrder())) {
        if (step.getNextStep().isEmpty() && step.getNextStepOf().isEmpty()) {
          for (Process process : step.getStepProcess())
            if (process instanceof Interaction && !Interaction.class.equals(process.getModelInterface()))
              pw.addPathwayComponent(process);
          pw.removePathwayOrder(step);
        }
      }

      //remove all Interaction.class (base) objects
      for (Interaction it : new HashSet<>(model.getObjects(Interaction.class))) {
        if (Interaction.class.equals(it.getModelInterface()))
          model.remove(it);
      }

      //remove sub-pathways
      for (Pathway pathway : new HashSet<>(model.getObjects(Pathway.class))) {
        if (pathway.getName().contains("SubPathway")) {
          model.remove(pathway);
          for (Pathway pp : new HashSet<>(pathway.getPathwayComponentOf()))
            pp.removePathwayComponent(pathway);
        }
      }

    }

    for (Named o : model.getObjects(Named.class)) {
      //move bogus dummy names to comments
      for (String name : new HashSet<>(o.getName())) {
        if (name.startsWith("SubPathway")) {
          o.removeName(name);
          o.addComment(name);
        }
      }
    }

//		ModelUtils.replace(model, replacements);
    ModelUtils.removeObjectsIfDangling(model, UtilityClass.class);

    // convert model back to OutputStream for return
    simpleReader.convertToOWL(model, cleanedData);
  }
}
