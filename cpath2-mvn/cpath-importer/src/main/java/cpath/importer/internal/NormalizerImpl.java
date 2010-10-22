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
import org.biopax.paxtools.controller.AbstractTraverser;
import org.biopax.paxtools.controller.ObjectPropertyEditor;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.converter.OneTwoThree;
import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.biopax.paxtools.model.BioPAXElement;
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
	
	private SimpleReader biopaxReader;
	
	
	/**
	 * Constructor
	 */
	public NormalizerImpl() {
		this.biopaxReader = new SimpleReader(); //may be to use 'biopaxReader' bean that uses (new BioPAXFactoryForPersistence(), BioPAXLevel.L3);
		biopaxReader.mergeDuplicates(true);
	}

	
	/* (non-Javadoc)
	 * @see cpath.importer.Normalizer#normalize(String)
	 */
	public String normalize(String biopaxOwlData) {
		
		if(biopaxOwlData == null || biopaxOwlData.length() == 0) 
			throw new IllegalArgumentException("no data.");
		
		// if required, upgrade to L3
		biopaxOwlData = convertToLevel3(biopaxOwlData);
		
		// fix BioPAX L3 pre-release property name 'taxonXref' (BioSource)
		biopaxOwlData = biopaxOwlData.replaceAll("taxonXref","xref");
		
		// build the model
		Model model = biopaxReader.convertFromOWL(new ByteArrayInputStream(biopaxOwlData.getBytes()));
		if(model == null || model.getLevel() != BioPAXLevel.L3) {
			throw new IllegalArgumentException("Data is not BioPAX L3!");
		}
		
		// clean/normalize xrefs first (they are used next)!
		normalizeXrefs(model);
		
		// fix displayName where possible
		fixDisplayName(model);
		
		// copy
		Set<? extends UtilityClass> objects = 
			new HashSet<UtilityClass>(model.getObjects(UtilityClass.class));
		// process the rest of utility classes (selectively though)
		for(UtilityClass bpe : objects) 
		{
			if(bpe instanceof ControlledVocabulary || bpe instanceof BioSource) 
			{
				UnificationXref uref = getFirstUnificationXref((XReferrable) bpe);
				if (uref != null) 
					normalizeID(model, bpe, uref.getDb(), uref.getId());
				else 
					log.error("Cannot normalize ControlledVocabulary: " +
					"no unification xrefs found in " + bpe.getRDFId());
			} else if(bpe instanceof EntityReference) {
				UnificationXref uref = getFirstUnificationXrefOfEr((EntityReference) bpe);
				if (uref != null) 
					normalizeID(model, bpe, uref.getDb(), uref.getId());
				else 
					log.error("Cannot normalize EntityReference: " +
					"no unification xrefs found in " + bpe.getRDFId());
			} else if(bpe instanceof Provenance) {
				Provenance pro = (Provenance) bpe;
				String name = pro.getStandardName();
				if(name == null) 
					name = pro.getDisplayName();
				if (name != null) 
					normalizeID(model, pro, name, null);
				else 
					log.error("Cannot normalize Provenance: " +
					"no standard names found in " + bpe.getRDFId());
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
				log.error("Unknown or misspelled database name! Won't fix for now... " + e);
			}
			
			// build new standard rdfid
			String rdfid =  BIOPAX_URI_PREFIX + ref.getModelInterface().getSimpleName() 
				+ ":" + URLEncoder.encode(name + "_" + ref.getId());
			if(ref.getIdVersion() != null && !"".equals(ref.getIdVersion().trim()))
				rdfid += "_" + ref.getIdVersion();
			// replace xref or update ID
			updateID(model, ref, rdfid);
		}
	}	
	
	
	/**
	 * Sets Miriam standard URI (if possible) for a utility object 
	 * (but not for *Xref!); also removes duplicates...
	 * 
	 * @param model the BioPAX model
	 * @param bpe element to normalize
	 * @param db official database name or synonym (that of bpe's unification xref)
	 * @param id identifier (if null, new ID will be that of the Miriam Data Type; this is mainly for Provenance)
	 */
	private void normalizeID(Model model, UtilityClass bpe, String db, String id) 
	{	
		if(bpe instanceof Xref) {
			log.error("normalizeID called for Xref (hey, this is a bug!)");
			return;
		}
		
		// get the standard ID
		String urn = null;
		try {
			// make a new ID for the element
			if(id != null)
				urn = MiriamLink.getURI(db, id);
			else 
				urn = MiriamLink.getDataTypeURI(db);
		} catch (Exception e) {
			log.error("Cannot get a Miriam standard ID for " + bpe 
				+ " (" + bpe.getModelInterface().getSimpleName()
				+ ") " + ", using " + db + ":" + id 
				+ ". " + e);
			return;
		}
		
		// update element and model (if required)
		try {
			updateID(model, bpe, urn);
		} catch (Exception e) {
			log.error("Failed to replace ID of " + bpe 
				+ " (" + bpe.getModelInterface().getSimpleName()
				+ ") with '" + urn + "'. " + e);
			return;
		}
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


	/**
	 * Updates ID and/or removes duplicate
	 * 
	 * @param model
	 * @param ref
	 * @param rdfid
	 * @return
	 */
	private UtilityClass updateID(final Model model, final UtilityClass u, final String rdfid) 
	{	
		final UtilityClass v = (UtilityClass) model.getByID(rdfid);
		if(v != null) {
			if(log.isInfoEnabled())
				log.info("Removing duplicate, updating links" +
					" (object properties) using existing " 
					 + rdfid + " element instead of " + u.getRDFId());
			//assert(v.isEquivalent(u)); // TODO (whew) strictly speaking...
			if(!v.isEquivalent(u)) {
				log.warn(u + " (" + u.getRDFId() + ", " + u.getModelInterface().getSimpleName()
					+ ") is to be replaced with but NOT semantically equivalent to " + 
					v + " (" + v.getRDFId() + ", " + v.getModelInterface().getSimpleName()
					+ "). Ignoring...");
			}
			
			AbstractTraverser traverser = new AbstractTraverser(biopaxReader.getEditorMap()) {
				@Override
				protected void visit(Object range, BioPAXElement domain, Model model,
						PropertyEditor editor) {
					if(editor instanceof ObjectPropertyEditor && u.equals(range)) {
						// replace value
						editor.removeValueFromBean(u, domain);
						editor.setValueToBean(v, domain);
						if(log.isDebugEnabled()) {
							log.debug("Replaced " + u.getRDFId() + 
								" with " + v.getRDFId() +
								"; " + editor.toString() + 
								"; (domain) bean: " + domain);
						}
					}
				}
			};
			
			// look inside every object -
			for(BioPAXElement element : model.getObjects()) {
				traverser.traverse(element, model);
			}
			// remove now dangling object
			model.remove(u);
			
			// smoke test...
			if(u instanceof Xref)
				assert(((Xref)u).getXrefOf().isEmpty());
			else if(u instanceof EntityReference)
				assert(((EntityReference)u).getEntityReferenceOf().isEmpty());
			
			return v;
		} else {
			model.updateID(u.getRDFId(), rdfid);
			return u;
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

	
	/**
	 * Converts biopax l2 string to biopax l3 if it's required
	 *
	 * @param biopaxData String
	 * @return
	 */
	private String convertToLevel3(final String biopaxData) {
		String toReturn = "";
		
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			InputStream is = new ByteArrayInputStream(biopaxData.getBytes());
			SimpleReader reader = new SimpleReader();
			reader.mergeDuplicates(true);
			Model model = reader.convertFromOWL(is);
			if (model.getLevel() != BioPAXLevel.L3) {
				if (log.isInfoEnabled())
					log.info("Converting to BioPAX Level3...");
				model = (new OneTwoThree()).filter(model);
				if (model != null) {
					SimpleExporter exporter = new SimpleExporter(model.getLevel());
					exporter.convertToOWL(model, os);
					toReturn = os.toString();
				}
			} else {
				toReturn = biopaxData;
			}
		} catch(Exception e) {
			throw new RuntimeException(
					"Failed to reading data or convert to L3!", e);
		}

		// outta here
		return toReturn;
	}
}
