/**
 ** Copyright (c) 2009 Memorial Sloan-Kettering Cancer Center (MSKCC)
 ** and University of Toronto (UofT).
 **
 ** This is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** both UofT and MSKCC have no obligations to provide maintenance, 
 ** support, updates, enhancements or modifications.  In no event shall
 ** UofT or MSKCC be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** UofT or MSKCC have been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this software; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA;
 ** or find it at http://www.fsf.org/ or http://www.gnu.org.
 **/

package cpath.normalizer.internal;

import java.io.*;
import java.util.*;

import org.biopax.miriam.MiriamLink;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.ControlledVocabulary;
import org.biopax.paxtools.model.level3.EntityReference;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.XReferrable;
import org.biopax.paxtools.util.ClassFilterSet;

import cpath.normalizer.Normalizer;

/**
 * @author rodch
 *
 */
public class IdNormalizer implements Normalizer {
	
	
	private MiriamLink miriam;
	private BioPAXIOHandler biopaxReader;
	
	/**
	 * Constructor
	 */
	public IdNormalizer(MiriamLink miriam) {
		this.miriam = miriam;
		this.biopaxReader = new SimpleReader(); //biopaxReader; // BUG - with factoryForPersistence, it allows duplicate RDFId!
	}
	

	/** 
	 * This will modify the original model, so that
	 * controlled vocabulary (CV) and entity reference (ER) 
	 * will have got standard unique resource identifiers (defined by Miriam). 
	 * For example, the official URI for UniProt is "urn:miriam:uniprot", 
	 * and a protein can be referred as "urn:miriam:uniprot:P62158". 
	 * So, if an ER or CV does not have such RDFId, it will be "normalized": 
	 * one of unification xrefs will be used to create the new, standard, URN. 
	 * It is not guaranteed, however, that the best URI id generated 
	 * (if required, converting to the best one may be done separately)
	 * 
	 * @param model a BioPAX Paxtools Model 
	 * 
	 * @see org.biopax.paxtools.controller.ModelFilter#filter(org.biopax.paxtools.model.Model)
	 */
	public String normalize(String owl) {
		
		// build the model
		Model model = biopaxReader.convertFromOWL(new ByteArrayInputStream(owl.getBytes()));
		if(model == null || model.getLevel() != BioPAXLevel.L3) {
			throw new IllegalArgumentException(model.getLevel() + " is not supported!");
		}
		
		for(XReferrable bpe : model.getObjects(XReferrable.class)) {
			List<UnificationXref> urefs = getUnificationXrefsSorted(bpe);
			UnificationXref uref = null;
			
			if(bpe instanceof ControlledVocabulary) {
				uref = getFirstUnificationXrefOfCv(urefs);
			} else if(bpe instanceof EntityReference) {
				uref = getFirstUnificationXrefOfEr(urefs);
			} else {
				//TODO can we also normalize Provenance, Score, and Evidence?..
				continue;
			}
			
			if(uref != null) {
				String urn = miriam.getURI(uref.getDb(), uref.getId());
				model.updateID(bpe.getRDFId(), urn);
			} else {
				throw new IllegalArgumentException(
					"Cannot find a unification xrefs of CV : " + bpe);
			}
		}
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			(new SimpleExporter(model.getLevel())).convertToOWL(model, out);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		return out.toString();
	}

	
	private List<UnificationXref> getUnificationXrefsSorted(XReferrable referrable) {
		
		List<UnificationXref> urefs = new ArrayList<UnificationXref>(
			new ClassFilterSet<UnificationXref>(referrable.getXref(), UnificationXref.class)
		);	
		
		Comparator<UnificationXref> comparator = new Comparator<UnificationXref>() {
			@Override
			public int compare(UnificationXref o1, UnificationXref o2) {
				String s1 = o1.getDb() + o1.getId();
				String s2 = o2.getDb() + o2.getId();
				return s1.compareTo(s2);
			}
		};
		
		Collections.sort(urefs, comparator);
		
		return urefs;
	}

	
	/*
	 * Gets the first one, the set is not empty, or null.
	 */
	private UnificationXref getFirstUnificationXrefOfCv(List<UnificationXref> urefs) {
		return (urefs.isEmpty()) ? null : urefs.get(0);
	}

	/*
	 * The first uniprot or enterz gene xref, if exists, will be returned;
	 * otherwise, the first one of any kind is the answer.
	 */
	private UnificationXref getFirstUnificationXrefOfEr(List<UnificationXref> urefs) {
		UnificationXref ret = null;

		for(UnificationXref uref : urefs) {
			if(uref.getDb().toLowerCase().startsWith("uniprot") 
				|| uref.getDb().toLowerCase().startsWith("entrez")) {
				return uref;
			}
		}
		
		// otherwise, take the first one
		return (urefs.isEmpty()) ? null : urefs.get(0);
	}

}
