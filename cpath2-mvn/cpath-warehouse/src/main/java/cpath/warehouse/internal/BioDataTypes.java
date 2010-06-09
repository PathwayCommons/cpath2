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

package cpath.warehouse.internal;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.miriam.MiriamLink;
import org.bridgedb.DataSource;

import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Metadata.TYPE;

/**
 * This convenience bean makes all the CPathSquared data sources (from Metadata),
 * id types, and legacy (cPath) data_source names accessible via Bridgedb DataSource.
 * For example, one can get BIOGRID data source from anywhere (in the same JVM),
 * either as BioDataTypes.BIOGRID, DataSource.getBySystemCode("BIOGRID").
 * or DataSource.getByFullName("BioGRID"), etc.
 * 
 * @author rodche
 *
 */
public class BioDataTypes {
	private static final Log LOG = LogFactory.getLog(BioDataTypes.class);
	
	private MetadataDAO metadataDAO;

	public static final String NETWORK_TYPE = "network";
	// manually register legacy (cpath) data source names
	public static final DataSource BIOGRID = DataSource.register("BIOGRID", "BioGRID").type(NETWORK_TYPE).asDataSource();
	public static final DataSource CELL_MAP = DataSource.register("CELL_MAP", "Cancer Cell Map").type(NETWORK_TYPE).asDataSource(); 
	public static final DataSource HPRD = DataSource.register("HPRD", "HPRD").type("").type(NETWORK_TYPE).asDataSource();
	public static final DataSource HUMANCYC = DataSource.register("HUMANCYC", "HumanCyc").type(NETWORK_TYPE).asDataSource();
	public static final DataSource IMID = DataSource.register("IMID", "IMID").type(NETWORK_TYPE).asDataSource();
	public static final DataSource INTACT = DataSource.register("INTACT", "IntAct").type(NETWORK_TYPE).asDataSource();
	public static final DataSource MINT = DataSource.register("MINT", "MINT").type(NETWORK_TYPE).asDataSource();
	public static final DataSource NCI_NATURE = DataSource.register("NCI_NATURE", "NCI / Nature Pathway Interaction Database").type(NETWORK_TYPE).asDataSource();
	public static final DataSource REACTOME = DataSource.register("REACTOME", "Reactome").type(NETWORK_TYPE).asDataSource();
	
	public static final String ID_TYPE = "id";
	// manually register what is used in cPath WS, parameter input_id_type
	public static final DataSource UNIPROT = DataSource.register("UNIPROT", "UniProt").type(ID_TYPE).asDataSource();
	public static final DataSource CPATH_ID = DataSource.register("CPATH_ID", "CPATH_ID").type(ID_TYPE).asDataSource(); // now - RDFId
	public static final DataSource ENTREZ_GENE = DataSource.register("ENTREZ_GENE", "Entrez Gene").type(ID_TYPE).asDataSource();
	public static final DataSource GENE_SYMBOL =  DataSource.register("GENE_SYMBOL", "Gene Symbol").type(ID_TYPE).asDataSource();

	
	public BioDataTypes(MetadataDAO metadataDAO) {
		this.metadataDAO = metadataDAO;
	}

	
	/**
	 * Initializes (Bridgedb) data sources.
	 * 
	 * Note: metadata.getType() becomes DataSource's type, and it will be used, 
	 * e.g., in web controllers, to separate "true" cPathSquare data sources 
	 * from the rest.
	 * 
	 */
	@PostConstruct
	void init() {
		
		// register MIRIAM data types as "id" data sources
		for (String name : MiriamLink.getDataTypesName()) {
			// register all synonyms (incl. the name)
			for (String s : MiriamLink.getNames(name)) {
				// warn: if s (name) exists, will override
				DataSource ds = DataSource.register(s, name).urnBase(
						MiriamLink.getDataTypeURI(name)).type(ID_TYPE).asDataSource();
				if(LOG.isInfoEnabled()) 
					LOG.info("Register data provider: " + ds);
			}
		}
		
		//Register all the pathway data providers (as stored in Warehouse).
		for(Metadata metadata : metadataDAO.getAll()) {
			// won't register here data sources of proteins and molecules (warehouse resources)
			if(metadata.getType() == TYPE.BIOPAX || metadata.getType() == TYPE.PSI_MI
				|| metadata.getType() == TYPE.BIOPAX_L2) 
			{
				// warn: if s (name) exists, will override
				DataSource ds = DataSource.register(metadata.getIdentifier(), metadata.getName())
					.type(NETWORK_TYPE).mainUrl(metadata.getURLToPathwayData()).asDataSource();
				if(LOG.isInfoEnabled()) 
					LOG.info("Register data provider: " + ds);
			}
		}
		
		/* 
		 * fix if the above possibly overrides the legacy data sources defined 
		 * earlier as constants (this is for parameter values that are required
		 * to be exactly as they were in the cPath web service)
		 */
		DataSource.register("BIOGRID", "BioGRID").type(NETWORK_TYPE).asDataSource();
		DataSource.register("CELL_MAP", "Cancer Cell Map").type(NETWORK_TYPE).asDataSource(); 
		DataSource.register("HPRD", "HPRD").type("").type(NETWORK_TYPE).asDataSource();
		DataSource.register("HUMANCYC", "HumanCyc").type(NETWORK_TYPE).asDataSource();
		DataSource.register("IMID", "IMID").type(NETWORK_TYPE).asDataSource();
		DataSource.register("INTACT", "IntAct").type(NETWORK_TYPE).asDataSource();
		DataSource.register("MINT", "MINT").type(NETWORK_TYPE).asDataSource();
		DataSource.register("NCI_NATURE", "NCI / Nature Pathway Interaction Database").type(NETWORK_TYPE).asDataSource();
		DataSource.register("REACTOME", "Reactome").type(NETWORK_TYPE).asDataSource();
		
		DataSource.register("UNIPROT", "UniProt").type(ID_TYPE).asDataSource();
		DataSource.register("CPATH_ID", "CPATH_ID").type(ID_TYPE).asDataSource(); // now - RDFId
		DataSource.register("ENTREZ_GENE", "Entrez Gene").type(ID_TYPE).asDataSource();
		DataSource.register("GENE_SYMBOL", "Gene Symbol").type(ID_TYPE).asDataSource();
	}
}
