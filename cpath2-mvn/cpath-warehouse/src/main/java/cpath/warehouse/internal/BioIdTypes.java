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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.miriam.MiriamLink;
import org.bridgedb.DataSource;



/**
 * This convenience bean makes some ID types, e.g., 
 * old CPath web service's input_id_type, available as/via Bridgedb DataSource.
 * 
 * @author rodche
 */
public class BioIdTypes {
	private static final Log LOG = LogFactory.getLog(BioIdTypes.class);
	
	// manually register what is used in cPath WS, parameter input_id_type
	public final DataSource UNIPROT = DataSource.getByFullName("UNIPROT");
	public final DataSource CPATH_ID = DataSource.getByFullName("CPATH_ID"); // RDFId
	public final DataSource ENTREZ_GENE = DataSource.getByFullName("ENTREZ_GENE");
	public final DataSource GENE_SYMBOL =  DataSource.getByFullName("GENE_SYMBOL");

	static {
		// register MIRIAM data types
		for (String name : MiriamLink.getDataTypesName()) {
			// register all synonyms (incl. the name)
			for (String s : MiriamLink.getNames(name)) {
				DataSource ds = DataSource.register(s, name).urnBase(
						MiriamLink.getDataTypeURI(name)).asDataSource();
				if(LOG.isInfoEnabled()) 
					LOG.info("Register data provider: " + ds);
			}
		}
	}
}
