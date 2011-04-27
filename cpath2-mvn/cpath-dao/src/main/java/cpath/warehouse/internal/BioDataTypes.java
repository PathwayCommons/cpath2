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

package cpath.warehouse.internal;

import java.util.*;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.biopax.miriam.MiriamLink;
import org.bridgedb.DataSource;
import org.bridgedb.DataSource.Builder;

import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Metadata.TYPE;

/**
 * This convenience bean makes all the CPathSquared data sources,
 * id types, and legacy (cPath) data_source names accessible via org.bridgedb.DataSource.
 * For example, one can get BIOGRID data source from anywhere (in the same JVM),
 * either as BioDataTypes.BIOGRID, DataSource.getBySystemCode("BIOGRID").
 * or DataSource.getByFullName("BioGRID"), etc.
 * 
 * @author rodche
 *
 */
public final class BioDataTypes {
	private static final Log LOG = LogFactory.getLog(BioDataTypes.class);
	
	private MetadataDAO metadataDAO;

	/** 
	 * Enumeration to use for the 'type' property 
	 * when registering new org.bridgedb.DataSource
	 */
	public enum Type {
	// for pathway/network data provider
		PATHWAY_DATA,	
	// for physical entity's and other identifiers (RDFId, CPATH_ID, InCHI, iRefWeb, etc.)
		IDENTIFIER;
		
		public static Type parse(String value) {
			for(Type v : Type.values()) {
				if(v.name().equalsIgnoreCase(value))
					return v;
			}
			return null;
		}
	}
	
	public BioDataTypes(MetadataDAO metadataDAO) {
		this.metadataDAO = metadataDAO;
	}

	
	/**
	 * Initializes data sources.
	 * 
	 * Note: metadata.getType() becomes DataSource's type, and it will be used 
	 * in the web controller, to separate known to cPathSquare data sources 
	 * from the rest.
	 * 
	 */
	@PostConstruct
	void init() 
	{
		/* The following was commented out, because it's
		 * either wrong or not required (because Miriam does not separate different classes of data source)
		 * 
		// dynamically register MIRIAM data types as ID_TYPE data sources
		for (String name : MiriamLink.getDataTypesName()) {
			// register all synonyms (incl. the name)
			for (String s : MiriamLink.getNames(name)) {
				// warn: if s (name) exists, will override
				register(s, name, Type.IDENTIFIER).urnBase(MiriamLink.getDataTypeURI(name));
				if(LOG.isInfoEnabled()) 
					LOG.info("Register data provider: " + s + " (" + name + ")");
			}
		}
		*/
		
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
		
		/* 
		 * The following would fix possibly overridden 
		 * legacy data sources (query parameters are still required
		 * to be exactly as they were in the cPath web services...)
		 */
		/*
		 * This was commented out, because it's not required anymore;
		 * we will only support those data source names found 
		 * during the pathway data import (as defined in the metadata)
		 * 
		 * 
		// register legacy (cpath) data source names
		register("BIOGRID", "BioGRID", Type.PATHWAY_DATA);
		register("CELL_MAP", "Cancer Cell Map", Type.PATHWAY_DATA); 
		register("HPRD", "HPRD", Type.PATHWAY_DATA);
		register("HUMANCYC", "HumanCyc", Type.PATHWAY_DATA);
		register("IMID", "IMID", Type.PATHWAY_DATA);
		register("INTACT", "IntAct", Type.PATHWAY_DATA);
		register("MINT", "MINT", Type.PATHWAY_DATA);
		register("NCI_NATURE", "NCI / Nature Pathway Interaction Database", Type.PATHWAY_DATA);
		register("REACTOME", "Reactome", Type.PATHWAY_DATA);
		
		// register what is used in cPath WS, parameter input_id_type
		register("UNIPROT", "UniProt", Type.IDENTIFIER);
		register("CPATH_ID", "CPATH_ID", Type.IDENTIFIER); // now - RDFId
		register("ENTREZ_GENE", "Entrez Gene", Type.IDENTIFIER);
		register("GENE_SYMBOL", "Gene Symbol", Type.IDENTIFIER);
		// new
		register("REFSEQ", "RefSeq", Type.IDENTIFIER);
		*/
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
