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

package cpath.importer.internal;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.miriam.MiriamLink;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.*;
import org.biopax.paxtools.util.ClassFilterSet;

import cpath.config.CPathSettings;
import cpath.importer.Normalizer;

/**
 * BioPAX Normalizer.
 * 
 * @author rodch
 *
 */
public class NormalizerImpl implements Normalizer {
	private static final Log log = LogFactory.getLog(NormalizerImpl.class);
	
	/* 
	 * URI namespace prefix for the utility class IDs 
	 * generated during data convertion, normalization, merge...
	 */
	public static final String BIOPAX_URI_PREFIX = CPathSettings.CPATH_URI_PREFIX;
	
	private BioPAXIOHandler biopaxReader;
	

	/**
	 * Constructor
	 */
	public NormalizerImpl() {
		this.biopaxReader = new SimpleReader(); //may be to use 'biopaxReader' bean that uses (new BioPAXFactoryForPersistence(), BioPAXLevel.L3);
	}

	
	/* (non-Javadoc)
	 * @see cpath.importer.Normalizer#normalize(String)
	 */
	public String normalize(String biopaxOwlData) {
		
		if(biopaxOwlData == null || biopaxOwlData.length() == 0) 
			throw new IllegalArgumentException("no data.");
		
		// fix BioPAX L3 pre-release property name 'taxonXref' (BioSource)
		biopaxOwlData = biopaxOwlData.replaceAll("taxonXref","xref");
		
		// build the model
		Model model = biopaxReader.convertFromOWL(new ByteArrayInputStream(biopaxOwlData.getBytes()));
		if(model == null || model.getLevel() != BioPAXLevel.L3) {
			throw new IllegalArgumentException("Data is not BioPAX L3!");
		}
		
		// clean/normalize xrefs first!
		normalizeXrefs(model);
		
		// fix displayName where possible
		fixDisplayName(model);
		
		// copy
		Set<? extends UtilityClass> objects = 
			new HashSet<UtilityClass>(model.getObjects(UtilityClass.class));
		// process the rest of utility classes (selectively though)
		for(UtilityClass bpe : objects) {
			UnificationXref uref = null;
			if(bpe instanceof ControlledVocabulary 
					|| bpe instanceof BioSource) {
				uref = getFirstUnificationXref((XReferrable) bpe);
				// continue after the last 'else'
			} else if(bpe instanceof EntityReference) {
				uref = getFirstUnificationXrefOfEr((EntityReference) bpe);
				// continue after the last 'else'
			} else if(bpe instanceof Provenance) {
				/*
				 * TODO do we want normalizing Provenance?..
				 */
				Provenance pro = (Provenance) bpe;
				String name = pro.getStandardName();
				if(name == null) name = pro.getDisplayName();
				try {
					String urn = MiriamLink.getDataTypeURI(name);
					model.updateID(pro.getRDFId(), urn);
				} catch (Exception e) {
					log.warn("Cannot normalize Provenance " + 
							pro + " (name:" + name + ")");
				}
				// done.
				continue;
			} else {
				// skip other utility classes
				continue;
			}
			
			if (uref != null) {
				try {
					String urn = MiriamLink.getURI(uref.getDb(), uref.getId());
					model.updateID(bpe.getRDFId(), urn);
				} catch (Exception e) {
					log.error("Cannot normalize Xref " + uref + " - " + e);
				}
			} else {
				log.error("Cannot find a unification xrefs of : " 
					+ bpe.getRDFId());
			}
		}
		
		
		/* 
		 * We could also "fix" organism property, where it's null,
		 * a swell (e.g., using the value from the pathway);
		 * also - check those values in protein references actually
		 * correspond to what can be found in the UniProt by using
		 * unification xrefs's 'id'... But this, fortunately, 
		 * happens in the CPathMerger (a ProteinReference 
		 * comes from the Warehouse with organism property already set!)
		 */
		
		// return as BioPAX OWL
		String owl = convertToOWL(model);
		return owl;
	}
	
	
	private void fixDisplayName(Model model) {
		if (log.isInfoEnabled())
			log.info("Trying to auto-fix 'null' displayName...");
		// where it's null, set to the shortest name if possible
		for (Named e : model.getObjects(Named.class)) {
			if (e.getDisplayName() == null) {
				if (e.getStandardName() != null) {
					e.setDisplayName(e.getStandardName());
					if (log.isInfoEnabled())
						log.info(e + " displayName auto-fix: "
								+ e.getDisplayName());
				} else if (!e.getName().isEmpty()) {
					String dsp = e.getName().iterator().next();
					for (String name : e.getName()) {
						if (name.length() < dsp.length())
							dsp = name;
					}
					e.setDisplayName(dsp);
					if (log.isInfoEnabled())
						log.info(e + " displayName auto-fix: " + dsp);
				}
			}
		}
		// if required, set PE name to (already fixed) ER's name...
		for(EntityReference er : model.getObjects(EntityReference.class)) {
			for(SimplePhysicalEntity spe : er.getEntityReferenceOf()) {
				if(spe.getDisplayName() == null || spe.getDisplayName().trim().length() == 0) {
					spe.setDisplayName(er.getDisplayName());
				}
			}
		}
	}


	private String convertToOWL(Model model) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			(new SimpleExporter(model.getLevel())).convertToOWL(model, out);
		} catch (IOException e) {
			throw new RuntimeException("Conversion to OWL failed.", e);
		}
		return out.toString();
	}

	/* (non-Javadoc)
	 * @see cpath.importer.Normalizer#normalizeXrefs(org.biopax.paxtools.model.Model)
	 */
	public void normalizeXrefs(Model model) {
		// normalize xrefs first: set db name as in Miriam and rdfid as db_id
		
		// make a copy (to safely remove duplicates)
		Set<? extends Xref> xrefs = new HashSet<Xref>(model.getObjects(Xref.class));
		for(Xref ref : xrefs) {
			// get database official urn
			String name = ref.getDb();
			name = fixKnownMisspell(name);
			try {
				String urn = MiriamLink.getDataTypeURI(name);
				// update name to the primary one
				name = MiriamLink.getName(urn);
				ref.setDb(name);
			} catch (IllegalArgumentException e) {
				log.error("Unknown or misspelled datanase name! Won't fix this now... " + e);
			}
			
			Xref x = null;
			// build new standard rdfid
			String rdfid =  BIOPAX_URI_PREFIX + ref.getModelInterface().getSimpleName() 
				+ ":" + URLEncoder.encode(name + "_" + ref.getId());
			Xref existingXref = (Xref) model.getByID(rdfid);
			if(existingXref != null) {
				if(log.isInfoEnabled())
					log.info("Removing duplicate and re-using xref " 
						 + rdfid + " instead " + ref.getRDFId());
				// replace the xref
				for(XReferrable bpe : new HashSet<XReferrable>(ref.getXrefOf())) {
					bpe.removeXref(ref);
					bpe.addXref(existingXref);
				}
				assert(ref.getXrefOf().isEmpty());
				model.remove(ref);
				// use existing
				x = existingXref;
			} else {
				model.updateID(ref.getRDFId(), rdfid);
				x = ref;
			}
			
			// warn if two elements reference the same unif. xref!
			if(x instanceof UnificationXref && x.getXrefOf().size()>1) {
				if(log.isWarnEnabled()) {
					// report max. three such cases
					Collection<String> list = new HashSet<String>();
					int i = 0;
					for(XReferrable e : x.getXrefOf()) {
						list.add(x.getRDFId());
						if(i == 3) break;
					}
					if(list.size() > 1) // yes, it can be the case! (different objects have the same rdfid...)
						log.warn("UnificationXref " + x + 
						" is used by several elements, e.g.,: " + list.toString() + 
						". It may be a semantic error, or these elements " +
						"are the same and should be merged!");
				}
			}
		}
	}


	/*
	 * Quick fix...
	 * 
	 * TODO generalize (e.g. using Validator's db synonyms...)
	 * 
	 * @param name
	 * @return
	 */
	private String fixKnownMisspell(String name) {
		String fixed = name.trim();
		
		if("psimi".equalsIgnoreCase(fixed) 
			|| "psi-mi".equalsIgnoreCase(fixed)
			|| "psi_mi".equalsIgnoreCase(fixed)
			|| "psi mi".equalsIgnoreCase(fixed)) 
		{
			fixed = "MI";
		}
		

		return fixed;
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
	private UnificationXref getFirstUnificationXref(XReferrable xr) {
		List<UnificationXref> urefs = getUnificationXrefsSorted(xr);
		return (urefs.isEmpty()) ? null : urefs.get(0);
	}

	
	/*
	 * The first uniprot or enterz gene xref, if exists, will be returned;
	 * otherwise, the first one of any kind is the answer.
	 */
	private UnificationXref getFirstUnificationXrefOfEr(EntityReference er) {
		List<UnificationXref> urefs = getUnificationXrefsSorted(er);
		for(UnificationXref uref : urefs) {
			if(uref.getDb().toLowerCase().startsWith("uniprot") 
				|| uref.getDb().toLowerCase().startsWith("entrez")) {
				return uref;
			}
		}
		// otherwise, take the first one
		return (urefs.isEmpty()) ? null : urefs.get(0);
	}


	/* (non-Javadoc)
	 * @see cpath.importer.Normalizer#normalize(org.biopax.paxtools.model.Model)
	 */
	public String normalize(Model model) {
		String owl = convertToOWL(model);
		return normalize(owl);
	}

}
