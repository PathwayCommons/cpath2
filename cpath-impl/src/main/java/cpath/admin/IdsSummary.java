package cpath.admin;

import cpath.service.Analysis;
import org.biopax.paxtools.model.Model;

import java.util.HashSet;
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
		//TODO implement (analyse HGNC, ChEBI usage, coverage,..)
	}

}
