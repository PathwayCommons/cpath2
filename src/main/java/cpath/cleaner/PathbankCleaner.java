package cpath.cleaner;

import cpath.service.CPathUtils;
import cpath.service.api.Cleaner;
import org.apache.commons.lang3.StringUtils;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.model.level3.Process;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * Implementation of Cleaner interface for the SMPDB/Pathbank BioPAX L3 pathway data.
 */
final class PathbankCleaner implements Cleaner {

  private static final Logger log = LoggerFactory.getLogger(PathbankCleaner.class);

  public void clean(InputStream data, OutputStream cleanedData) {
    // create bp model from dataFile
    SimpleIOHandler simpleReader = new SimpleIOHandler(BioPAXLevel.L3);
    Model model = simpleReader.convertFromOWL(data);

    // As we managed to get only human data archive from SMPDB there is no need for filtering by organism anymore -
    if (!model.containsID(model.getXmlBase() + "Reference/TAXONOMY_9606")
      && !model.containsID(model.getXmlBase() + "Reference/Taxonomy_9606")
      && !model.getObjects(BioSource.class).isEmpty())
    {
      throw new RuntimeException("Highly likely non-human datafile (skip).");
    }

    //since Apr-2018, top pathway URIs are "normalized" like: http://identifiers.org/smpdb/...
    //let's fix pathway uris base - use bioregistry.io/pathbank: instead
    CPathUtils.rebaseUris(model, "http://identifiers.org/smpdb/", "http://bioregistry.io/pathbank:");

    //remove pathways that have "SubPathway" name;
    //though all these could be merged to become more informative pathways (once all the datafiles get merged),
    //they add too much/unordered nesting/complexity to our model; not very helpful for the graph queries, SIF/GMT...
    //due to the way pathwayOrder and pathwayComponent are used...
    for (Pathway sp : new HashSet<>(model.getObjects(Pathway.class))) {
      if (sp.getName().contains("SubPathway")) {
        for (Pathway p : new HashSet<>(sp.getPathwayComponentOf())) {
          p.removePathwayComponent(sp);
        }
        model.remove(sp);
      }
    }

    //remove all Interaction.class (base) objects
    for (Interaction it : new HashSet<>(model.getObjects(Interaction.class))) {
      if (Interaction.class.equals(it.getModelInterface())) {
        model.remove(it);
      }
    }

    //smpdb/pathbank use pathwayOrder, but it seems useless/nonsense, - no nextStep, participants are also added as pathwayComponent...
    //move reaction/control from PathwayStep having no nextStep to pathwayComponent property of the parent pw.
    for (PathwayStep step : new HashSet<>(model.getObjects(PathwayStep.class))) {
      Pathway p = step.getPathwayOrderOf();
      if (step.getNextStep().isEmpty() && step.getNextStepOf().isEmpty()) { //seems always TRUE (pathbank 2024/02)!
        for (Process process : step.getStepProcess()) {
          if (process instanceof Interaction && !Interaction.class.equals(process.getModelInterface())) {
            p.addPathwayComponent(process);
          }
        }
        step.getPathwayOrderOf().removePathwayOrder(step);
        model.remove(step);
      } else {
        log.debug("keep pw step {} of pw {}", step.getUri(), p.getUri());
      }
    }

    //delete dummy names if any
    for (Named o : model.getObjects(Named.class)) {
      for (String name : new HashSet<>(o.getName())) {
        if (StringUtils.startsWithIgnoreCase(name, "SubPathway")) {
          o.removeName(name);
        }
      }
    }

    //remove dangling, e.g., pathway steps, cv, xrefs, etc.
    ModelUtils.removeObjectsIfDangling(model, UtilityClass.class);

    // convert model back to OutputStream for return
    simpleReader.convertToOWL(model, cleanedData);
  }
}
