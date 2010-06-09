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

package cpath.webservice.args;

import org.bridgedb.DataSource;

import cpath.warehouse.internal.BioDataTypes;



/**
 * Limits values for the cPath webservice's parameters -
 * 'input_id_type' and 'output_id_type'  
 * (added for backward compatibility).
 * 
 * All the required data sources (as org.bridgedb.DataSource) -
 * networks/pathway data providers, ID types, etc., - 
 * are defined and initialized in the Warehouse's bean BioDataTypes.
 * This enumeration defines a sub-set of those.
 * 
 * @author rodche
 *
 */
public enum CPathIdType {
	UNIPROT(BioDataTypes.UNIPROT),
	CPATH_ID(BioDataTypes.CPATH_ID),
	ENTREZ_GENE(BioDataTypes.ENTREZ_GENE),
	GENE_SYMBOL(BioDataTypes.GENE_SYMBOL),
	;
	
	public final DataSource dataSource;
	
	private CPathIdType(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	public static CPathIdType parse(String value) {
		for(CPathIdType v : CPathIdType.values()) {
			if(value.equalsIgnoreCase(v.dataSource.getSystemCode())
				|| value.equalsIgnoreCase(v.dataSource.getFullName())
			) return v;
		}
		return null;
	}
	
}
