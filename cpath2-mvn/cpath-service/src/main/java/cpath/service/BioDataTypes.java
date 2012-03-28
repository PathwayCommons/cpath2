/**
 ** Copyright (c) 2010 Memorial Sloan-Kettering Cancer Center (MSKCC)
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

package cpath.service;

import java.util.*;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.miriam.MiriamLink;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.Provenance;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.paxtools.util.ClassFilterSet;
import org.bridgedb.DataSource;
import org.bridgedb.DataSource.Builder;

import cpath.dao.PaxtoolsDAO;

/**
 * This convenience bean makes all the ServiceResponse datasources
 * id types, and organisms accessible via org.bridgedb.DataSource.
 * For example, one can get BIOGRID data source from anywhere (in the same JVM),
 * either as BioDataTypes.BIOGRID, DataSource.getBySystemCode("BIOGRID").
 * or DataSource.getByFullName("BioGRID"), etc...
 * 
 * @author rodche
 *
 */
public final class BioDataTypes {
	private static final Log LOG = LogFactory.getLog(BioDataTypes.class);
	
	private PaxtoolsDAO mainDAO;

	/** 
	 * Enumeration to use for the 'type' property 
	 * when registering a new org.bridgedb.DataSource
	 */
	public enum Type {
	// for pathway/network data provider
		DATASOURCE,	
	// for physical entity's and other identifiers (RDFId, CPATH_ID, InCHI, iRefWeb, etc.)
		IDENTIFIER,
	// for organisms (because BridgeDb Organism class that we've tried does not have any attribute for taxonomy!)
		ORGANISM,
		;
		
		public static Type parse(String value) {
			for(Type v : Type.values()) {
				if(v.name().equalsIgnoreCase(value))
					return v;
			}
			return null;
		}
	}
	
	public BioDataTypes(PaxtoolsDAO mainDAO) {
		this.mainDAO = mainDAO;
	}

	
	/**
	 * Initializes data sources and organisms.
	 * 
	 * Note: metadata.getType() becomes DataSource's type, and it will be used 
	 * in the web controller, to separate known to cPathSquare data sources 
	 * from the rest.
	 * 
	 */
	@PostConstruct
	public void init() 
	{	
		boolean useObsoleteResources = MiriamLink.useObsoleteResources;
		MiriamLink.useObsoleteResources = false; //set to ignore obsolete URLs
		for(Provenance prov : ((Model)mainDAO).getObjects(Provenance.class)) {
			mainDAO.initialize(prov);
			// it must have a standard name, uri (was normalized in the data import pipeline)!
			String urn = prov.getRDFId();
			String url = null;
			try {
				url = MiriamLink.getDataResources(urn)[0]; //any... (usually one)
				//TODO use Arrays.toString(MiriamLink.getDataResources(prov.getRDFId())) instead?
			} catch (IllegalArgumentException e) {
				LOG.warn("Provenance object " + prov + 
					" (" + urn + ") " + "is not normalized! (OK for junit test data)");
			}
			
			register(urn, prov.getStandardName(), Type.DATASOURCE)
				.organism(prov) // *a hack* - just stores the Provenance object (not organism!)
				.mainUrl(url)
				.urnBase(urn)
				; // it has been normalized (in the data import pipeline)!
			if(LOG.isInfoEnabled())  {
				LOG.info("Registered a new datasource: " + prov.getStandardName());
			}	
		}
		MiriamLink.useObsoleteResources = useObsoleteResources; //restore
		
		
		// dynamically register organisms (available BioSource)
		for(BioSource bioSource : ((Model)mainDAO).getObjects(BioSource.class)) {
			mainDAO.initialize(bioSource);
			String taxon = getTaxonId(bioSource);
			if(taxon != null) {
				String name = getOrganismName(bioSource);
				register(taxon, name, Type.ORGANISM)
					.urnBase("urn:miriam:taxonomy")
					.organism(bioSource)
					.mainUrl("urn:miriam:taxonomy:"+taxon);
				if(LOG.isInfoEnabled())  {
					LOG.info("Registered a new organism: " + name
						+ ", taxon:" + taxon);
				}
			} else {
				if(LOG.isWarnEnabled())  {
					LOG.warn("Cannot create Organism from " + bioSource);
				}
			}	
		}
	}
	
	
	private String getOrganismName(BioSource bioSource) {
		String name = bioSource.getStandardName();
		if( name == null) {
			name = bioSource.getDisplayName();
			if(name == null && !bioSource.getName().isEmpty()) {
				name = bioSource.getName().iterator().next();
			}
		} 
		return name;
	}
	
	private String getTaxonId(BioSource bioSource) {
		String id = null;
		if(!bioSource.getXref().isEmpty()) {
			Set<UnificationXref> uxs = new 
				ClassFilterSet<Xref,UnificationXref>(bioSource.getXref(), 
						UnificationXref.class);
			for(UnificationXref ux : uxs) {
				if("taxonomy".equalsIgnoreCase(ux.getDb())) {
					id = ux.getId();
					break;
				}
			}
		}
		return id;
	}
	
	
	/**
	 * Registers a new org.bridgedb.DataType 
	 * 
	 * (currently, being used internally, but it may help future plugins or admin tools)
	 * 
	 * @param id - data source identifier (short name)
	 * @param fullName
	 * @param type
	 * @return
	 */
	public static Builder register(String id, String fullName, Type type) {
		return DataSource.register(id, fullName).type(type.name());
	}
	
	/**
	 * Gets the set of data sources of the specified types.
	 * 
	 * @param types
	 * @return
	 */
	public static Set<DataSource> getDataSources(Type... types) {
		Set<DataSource> allDatasources = DataSource.getDataSources();
		List<Type> reqiredTypes = Arrays.asList(types);
		if(reqiredTypes.isEmpty()) {
			return allDatasources;
		} else {
			Set<DataSource> toReturn = new HashSet<DataSource>();
			for(DataSource ds : allDatasources) {
				Type type = Type.parse(ds.getType());
				if(type != null && reqiredTypes.contains(type))
					toReturn.add(ds);
			}
			return toReturn;
		}
	}
	
	
	/**
	 * Gets the set of datasource "keys" (codes)
	 * of the specified types.
	 * 
	 * @param types
	 * @return
	 */
	public static Set<String> getDataSourceKeys(Type... types) {
		Set<String> dss = new HashSet<String>();
		
		for(DataSource ds : getDataSources(types)) {
			dss.add(ds.getSystemCode().toUpperCase());
		}
		
		return dss;
	}

}
