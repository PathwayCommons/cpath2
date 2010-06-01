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
import org.bridgedb.DataSource;

import cpath.warehouse.MetadataDAO;
import cpath.warehouse.beans.Metadata;
import cpath.warehouse.beans.Metadata.TYPE;

/**
 * This convenience bean makes all the CPathSquared data sources (from Metadata) 
 * as well as legacy (CPath data_source) ones accessible as/via Bridgedb DataSource.
 * 
 * @author rodche
 *
 */
public class BioDataTypes {
	private static final Log LOG = LogFactory.getLog(BioDataTypes.class);
	
	public static final String NETWORK_TYPE = "network";
	
	private MetadataDAO metadataDAO;

	// manually register legacy (cpath) data source names
	public final DataSource BIOGRID = DataSource.register("BIOGRID", "BioGRID").type(NETWORK_TYPE).asDataSource();
	public final DataSource CELL_MAP = DataSource.register("CELL_MAP", "Cancer Cell Map").type(NETWORK_TYPE).asDataSource(); 
	public final DataSource HPRD = DataSource.register("HPRD", "HPRD").type("").type(NETWORK_TYPE).asDataSource();
	public final DataSource HUMANCYC = DataSource.register("HUMANCYC", "HumanCyc").type(NETWORK_TYPE).asDataSource();
	public final DataSource IMID = DataSource.register("IMID", "IMID").type(NETWORK_TYPE).asDataSource();
	public final DataSource INTACT = DataSource.register("INTACT", "IntAct").type(NETWORK_TYPE).asDataSource();
	public final DataSource MINT = DataSource.register("MINT", "MINT").type(NETWORK_TYPE).asDataSource();
	public final DataSource NCI_NATURE = DataSource.register("NCI_NATURE", "NCI / Nature Pathway Interaction Database").type(NETWORK_TYPE).asDataSource();
	public final DataSource REACTOME = DataSource.register("REACTOME", "Reactome").type(NETWORK_TYPE).asDataSource();
	
	/* Remark:
	 * for example, we can get BIOGRID data source from anywhere,
	 * either as BioDataTypes.BIOGRID, DataSource.getBySystemCode("BIOGRID").
	 * or DataSource.getByFullName("BioGRID");
	 */
	
	public BioDataTypes(MetadataDAO metadataDAO) {
		this.metadataDAO = metadataDAO;
	}
	
	
	/**
	 * Register all the pathway data providers (as stored in Warehouse).
	 * 
	 * Note: metadata.getType() becomes DataSource's type, and it will be used, 
	 * e.g., in web controllers, to separate "true" cPathSquare data sources 
	 * (imported and merged through Admin tool) from all others 
	 * (legacy, synonyms, ID types, etc..)
	 * 
	 */
	@PostConstruct
	void init() {
		for(Metadata metadata : metadataDAO.getAll()) {
			if(metadata.getType() == TYPE.BIOPAX || metadata.getType() == TYPE.PSI_MI) {
				DataSource ds = DataSource.register(metadata.getIdentifier(), metadata.getName())
					.type(metadata.getType().name()).mainUrl(metadata.getURLToPathwayData()).asDataSource();
				if(LOG.isInfoEnabled()) 
					LOG.info("Register data provider: " + ds);
			}
		}
	}
}
