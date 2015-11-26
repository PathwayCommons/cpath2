package cpath.admin;

import cpath.service.Analysis;
import org.apache.commons.lang.mutable.MutableInt;
import org.biopax.paxtools.controller.PathAccessor;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;

import java.io.PrintStream;
import java.util.*;

/**
 * Prints out а summary of the main merged BioPAX model.
 *
 * This class is not an essential part of this system; 
 * it's to be optionally called using the cpath-cli.sh script,
 * i.e., via {@link Admin} '-run-analysis' console command.
 * 
 * @author rodche
 */
public final class IdsSummary implements Analysis<Model> {

	//a java option that, if defined (any value),
	//enables more details printed to the idsSummary.out.txt:
	public static final String JAVA_OPTION_VERBOSE = "cpath.idsSummary.verbose";
	public static final String JAVA_OPTION_OUTPUT = "cpath.idsSummary.output";

	public void execute(Model model) {

		boolean verbose = System.getProperty(JAVA_OPTION_VERBOSE)!=null;
		PrintStream out;
		try {
			out = new PrintStream(System.getProperty(JAVA_OPTION_OUTPUT));
		} catch (Exception e) {
			out = System.out;
		}

		//Analyse SERs (Protein-, Dna* and Rna* references) - HGNC usage, coverage,..
		//Calc. the no. non-generic ERs having >1 different HGNC symbols and IDs, or none, etc.
		Set<SequenceEntityReference> haveMultipleHgnc = new HashSet<SequenceEntityReference>();
		Map<Provenance,MutableInt> numErs = new HashMap<Provenance,MutableInt>();
		Map<Provenance,MutableInt> numProblematicErs = new HashMap<Provenance,MutableInt>();
		PathAccessor pa = new PathAccessor("EntityReference/entityReferenceOf/dataSource", model.getLevel());
		Set<String> problemErs = new TreeSet<String>();
		for(EntityReference ser : model.getObjects(EntityReference.class)) {
			//skip if it's a SMR or generic
			if(ser instanceof SmallMoleculeReference || !ser.getMemberEntityReference().isEmpty())
				continue;

			Set<String> hgncSymbols = new HashSet<String>();
			Set<String> hgncIds = new HashSet<String>();

			if(ser.getUri().startsWith("http://identifiers.org/hgnc")) {
				String s = ser.getUri().substring(ser.getUri().lastIndexOf("/")+1);
				if(s.startsWith("HGNC:"))
					hgncIds.add(s);
				else
					hgncSymbols.add(s);
			}

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
				haveMultipleHgnc.add((SequenceEntityReference) ser);

			//increment "no hgnc" and "total" counts by data source
			for(Object provenance : pa.getValueFromBean(ser)) {
				if (hgncSymbols.isEmpty() && hgncIds.isEmpty()) {
					if (verbose) {
						problemErs.add(String.format("%s\t%s\t%s",
								((Provenance) provenance).getDisplayName(), ser.getDisplayName(), ser.getUri()));
					}

					MutableInt n = numProblematicErs.get(provenance);
					if (n == null)
						numProblematicErs.put((Provenance) provenance, new MutableInt(1));
					else
						n.increment();
				}

				MutableInt tot = numErs.get(provenance);
				if (tot == null)
					numErs.put((Provenance) provenance, new MutableInt(1));
				else
					tot.increment();
			}
		}
		//print results
		if(verbose) {
			out.println("SequenceEntityReferences (not generics) without any HGNC Symbol:");
			for(String line : problemErs) out.println(line);
		}
		out.println("The number of SERs (not generic) " +
				"that have more than one HGNC Symbols: " + haveMultipleHgnc.size());
		out.println("\nNumber of SequenceEntityReferences (not generics) without any HGNC ID, by data source:");
		int totalPrs = 0;
		int numPrsNoHgnc = 0;
		for(Provenance ds : numProblematicErs.keySet()) {
			int n = numProblematicErs.get(ds).intValue();
			numPrsNoHgnc += n;
			int t = numErs.get(ds).intValue();
			totalPrs += t;
			out.println(String.format("%s\t\t%d\t(%3.1f%%)", ds.getUri(), n, ((float)n)/t*100));
		}
		out.println(String.format("Total\t\t%d\t(%3.1f%%)", numPrsNoHgnc, ((float)numPrsNoHgnc)/totalPrs*100));

		//Analyse PRs - UniProt ID coverage,..
		numErs = new HashMap<Provenance,MutableInt>();
		numProblematicErs = new HashMap<Provenance,MutableInt>();
		pa = new PathAccessor("EntityReference/entityReferenceOf:Protein/dataSource", model.getLevel());
		problemErs = new TreeSet<String>();
		for(ProteinReference pr : model.getObjects(ProteinReference.class)) {
			//skip a generic one
			if(!pr.getMemberEntityReference().isEmpty())
				continue;

			for(Object provenance : pa.getValueFromBean(pr)) {
				if(!pr.getUri().startsWith("http://identifiers.org/uniprot")
						&& !pr.getXref().toString().toLowerCase().contains("uniprot")) {

					if (verbose) {
						problemErs.add(String.format("%s\t%s\t%s",
								((Provenance) provenance).getDisplayName(), pr.getDisplayName(), pr.getUri()));
					}

					MutableInt n = numProblematicErs.get(provenance);
					if (n == null)
						numProblematicErs.put((Provenance) provenance, new MutableInt(1));
					else
						n.increment();
				}

				//increment total PRs per datasource
				MutableInt tot = numErs.get(provenance);
				if(tot == null)
					numErs.put((Provenance)provenance, new MutableInt(1));
				else
					tot.increment();
			}
		}

		//print results
		if(verbose) {
			out.println("\nProteinReferences (not generics) without any UniProt AC:");
			for(String line : problemErs) out.println(line);
		}
		out.println("\nNumber of ProteinReferences (not generics) without any UniProt AC, by data source:");
		int totalErs = 0;
		int problematicErs = 0;
		for(Provenance ds : numProblematicErs.keySet()) {
			int n = numProblematicErs.get(ds).intValue();
			problematicErs += n;
			int t = numErs.get(ds).intValue();
			totalErs += t;
			out.println(String.format("%s\t\t%d\t(%3.1f%%)", ds.getUri(), n, ((float)n)/t*100));
		}
		out.println(String.format("Total\t\t%d\t(%3.1f%%)", numProblematicErs, ((float)problematicErs)/totalErs*100));


		//Analyse SMRs - ChEBI usage, coverage,..
		numErs = new HashMap<Provenance,MutableInt>();
		numProblematicErs = new HashMap<Provenance,MutableInt>();
		pa = new PathAccessor("EntityReference/entityReferenceOf:SmallMolecule/dataSource", model.getLevel());
		problemErs = new TreeSet<String>();
		for(SmallMoleculeReference smr : model.getObjects(SmallMoleculeReference.class)) {
			//skip a generic SMR
			if(!smr.getMemberEntityReference().isEmpty())
				continue;

			for(Object provenance : pa.getValueFromBean(smr)) {
				if(!smr.getUri().startsWith("http://identifiers.org/chebi/CHEBI:")
					&& !smr.getXref().toString().contains("CHEBI:")) {

					if (verbose) {
						problemErs.add(String.format("%s\t%s\t%s",
							((Provenance) provenance).getDisplayName(), smr.getDisplayName(), smr.getUri()));
					}

					MutableInt n = numProblematicErs.get(provenance);
					if (n == null)
						numProblematicErs.put((Provenance) provenance, new MutableInt(1));
					else
						n.increment();
				}

				//increment total SMRs per datasource
				MutableInt tot = numErs.get(provenance);
				if(tot == null)
					numErs.put((Provenance)provenance, new MutableInt(1));
				else
					tot.increment();
			}
		}

		//print results
		if(verbose) {
			out.println("\nSmallMoleculeReferences (not generics) without any ChEBI ID:");
			for(String line : problemErs) out.println(line);
		}
		out.println("\nNumber of SmallMoleculeReferences (not generics) without any ChEBI ID, by data source:");
		int totalSmrs = 0;
		int numSmrsNoChebi = 0;
		for(Provenance ds : numProblematicErs.keySet()) {
			int n = numProblematicErs.get(ds).intValue();
			numSmrsNoChebi += n;
			int t = numErs.get(ds).intValue();
			totalSmrs += t;
			out.println(String.format("%s\t\t%d\t(%3.1f%%)", ds.getUri(), n, ((float)n)/t*100));
		}
		out.println(String.format("Total\t\t%d\t(%3.1f%%)", numSmrsNoChebi, ((float)numSmrsNoChebi)/totalSmrs*100));
	}
}
