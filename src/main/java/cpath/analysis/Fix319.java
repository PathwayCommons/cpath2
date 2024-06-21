package cpath.analysis;

import cpath.service.api.Analysis;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.SimplePhysicalEntity;
import org.biopax.paxtools.model.level3.UtilityClass;

import java.util.HashSet;

/**
 * This is to fix issue #319 in the PC v14 data (in June 2024 it was beta)
 * and similar potential LD issues due to invalid biopax URIs...
 * Also remove all dangling SimplePhysicalEntity (i.e. not Complex) individuals, if any
 * (these ain't useful for anything and are likely there due to mistakes or duplicate original data, e.g. in NetPath...)
 */
public class Fix319 implements Analysis<Model> {
  public void execute(Model model) {
    //remove dangling SPEs (such non-participant/components molecules are not useful for pathway analyses...)
    ModelUtils.removeObjectsIfDangling(model, SimplePhysicalEntity.class);

    //now, remove dangling xrefs, CV et al. utility type individuals
    ModelUtils.removeObjectsIfDangling(model, UtilityClass.class);

    //replace bad URI part "intact_complex" with "intact.complex" (also replaces "pc14:intact_complex")
    for (BioPAXElement e : new HashSet<>(model.getObjects())) {
      if (StringUtils.contains(e.getUri(), "intact_complex")) {
        String r = RegExUtils.replaceFirst(e.getUri(), "intact_complex", "intact.complex");
        ModelUtils.updateUri(model, e, r);
      }
    }

    //fix bad invalid URIs (there were some URIs with a space,
    // e.g. "netpath:S 312" causing trouble when converting to JSONLD, etc.)
    ModelUtils.fixInvalidUris(model);
  }
}
