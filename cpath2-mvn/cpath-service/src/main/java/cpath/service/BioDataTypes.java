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
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.util.ClassFilterSet;
import org.bridgedb.DataSource;
import org.bridgedb.DataSource.Builder;
import org.bridgedb.bio.Organism;

import cpath.dao.PaxtoolsDAO;
import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Metadata.TYPE;

/**
 * This convenience bean makes all the CPathSquared datasources
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
	
	private MetadataDAO metadataDAO;
	private PaxtoolsDAO mainDAO;
	
	//available in mainDAO organisms:
	private static final Set<Organism> organisms = new HashSet<Organism>(); 

	/** 
	 * Enumeration to use for the 'type' property 
	 * when registering a new org.bridgedb.DataSource
	 */
	public enum Type {
	// for pathway/network data provider
		PATHWAY_DATA,	
	// for physical entity's and other identifiers (RDFId, CPATH_ID, InCHI, iRefWeb, etc.)
		IDENTIFIER,
		;
		
		public static Type parse(String value) {
			for(Type v : Type.values()) {
				if(v.name().equalsIgnoreCase(value))
					return v;
			}
			return null;
		}
	}
	
	public BioDataTypes(MetadataDAO metadataDAO, PaxtoolsDAO mainDAO) {
		this.metadataDAO = metadataDAO;
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
	void init() 
	{	
		// dynamically register all the Pathway Data providers -
		for(Metadata metadata : metadataDAO.getAll()) {
			// skip data sources of protein and molecule references (warehouse resources)
			if(metadata.getType() == TYPE.BIOPAX 
					|| metadata.getType() == TYPE.PSI_MI) 
			{
				// warn: if s (name) exists, will override
				register(metadata.getIdentifier(), metadata.getName(), Type.PATHWAY_DATA)
					.mainUrl(metadata.getURLToData());
				if(LOG.isInfoEnabled()) 
					LOG.info("Register data provider: " + metadata.getIdentifier());
			}
		}
		
		// dynamically register all available organisms -
		for(BioSource bioSource : mainDAO.getObjects(BioSource.class)) {
			// warn: if s (name) exists, will override
			String taxon = getTaxonId(bioSource);
			Organism o = Organism.fromCode(taxon);
			if(o != null) {
				organisms.add(o);
				if(LOG.isInfoEnabled())  {
					String nameInMainDAO = getOrganismName(bioSource);
					LOG.info("Register organism: " + o.latinName()
						+ " matched by " + taxon + " and " + nameInMainDAO);
				}
			} else {
				if(LOG.isWarnEnabled())  {
					LOG.warn("Cannot create Organism from " + taxon);
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
				ClassFilterSet<UnificationXref>(bioSource.getXref(), 
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
	
	
	public static Set<Organism> getOrganisms() {
		return organisms;
	}
	
	/**
	 * Checks whether our system contains the organism 
	 * specified by taxonomy id or name.
	 * @param key
	 */
	public static boolean containsOrganism(String key) {
		Organism o = Organism.fromCode(key);
		if(o == null) {
			o = Organism.fromShortName(key);
			if(o == null) {
				o = Organism.fromLatinName(key);
			}
		}
		
		return o != null && organisms.contains(o);
	}
}
