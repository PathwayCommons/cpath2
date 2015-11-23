package cpath.admin;

import cpath.service.Analysis;
import org.apache.commons.lang.mutable.MutableInt;
import org.biopax.paxtools.controller.PathAccessor;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Prints out а summary of the main merged BioPAX model.
 *
 * This class is not an essential part of this system; 
 * it's to be optionally called using the cpath-cli.sh script,
 * i.e., via {@link Admin} '-run-analysis' console command.
 * 
 * @author rodche
 *
 */
public final class IdsSummary implements Analysis<Model> {

	public void execute(Model model) {
		//Calc. the no. non-generic ERs having >1 different HGNC symbols or different HGNC IDs...
		Set<SequenceEntityReference> haveMultipleHgnc = new HashSet<SequenceEntityReference>();
		for(SequenceEntityReference ser : model.getObjects(SequenceEntityReference.class)) {
			//skip generic ERs
			if(!ser.getMemberEntityReference().isEmpty())
				continue;

			Set<String> hgncSymbols = new HashSet<String>();
			Set<String> hgncIds = new HashSet<String>();
			for(Xref x : ser.getXref()) {
				if(x instanceof PublicationXref || x.getDb()==null || x.getId()==null)
					continue; //skip

				if(x.getDb().toLowerCase().startsWith("hgnc") && !x.getId().toLowerCase().startsWith("hgnc:")) {
					hgncSymbols.add(x.getId().toLowerCase());
				}
				else if(x.getDb().toLowerCase().startsWith("hgnc") && x.getId().toLowerCase().startsWith("hgnc:")) {
					hgncIds.add(x.getId().toLowerCase());
				}
			}

			if(hgncIds.size()>1 || hgncSymbols.size()>1)
				haveMultipleHgnc.add(ser);
		}
		//print
		System.out.println("\n" + haveMultipleHgnc.size() +
				" of non-generic SequenceEntityReferences have more than one different HGNC Symbol/ID xrefs.\n");

		//analyse ChEBI usage, coverage,..
		Map<Provenance,MutableInt> numSmrs = new HashMap<Provenance,MutableInt>();
		Map<Provenance,MutableInt> numSmrsWithoutChebiId = new HashMap<Provenance,MutableInt>();
		PathAccessor pa = new PathAccessor("EntityReference/entityReferenceOf:SmallMolecule/dataSource", model.getLevel());
		for(SmallMoleculeReference smr : model.getObjects(SmallMoleculeReference.class)) {
			for(Object element : pa.getValueFromBean(smr)) {
				if(!smr.getXref().toString().contains("CHEBI:")) {
					MutableInt n = numSmrsWithoutChebiId.get(element);
					if (n == null)
						numSmrsWithoutChebiId.put((Provenance) element, new MutableInt(1));
					else
						n.increment();
				}
				//increment total SMRs per datasource
				MutableInt tot = numSmrs.get(element);
				if(tot == null)
					numSmrs.put((Provenance)element, new MutableInt(1));
				else
					tot.increment();
			}
		}
		System.out.println("\nSmallMoleculeReferences without any ChEBI ID, by data source:");
		int totalSmrs = 0;
		int numSmrsNoChebi = 0;
		for(Provenance bs : numSmrsWithoutChebiId.keySet()) {
			int n = numSmrsWithoutChebiId.get(bs).intValue();
			numSmrsNoChebi += n;
			int t = numSmrs.get(bs).intValue();
			totalSmrs += t;
			System.out.println(String.format("\n %s\t%d\t(%3.1f%%)", bs.getUri(), n, n/t*100));
		}
		System.out.println(String.format("\n Total\t%d\t(%3.1f%%)", numSmrsNoChebi, numSmrsNoChebi/totalSmrs*100));
	}
}
