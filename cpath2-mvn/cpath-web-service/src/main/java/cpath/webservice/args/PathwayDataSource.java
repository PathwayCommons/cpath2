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

package cpath.webservice.args;

import org.bridgedb.DataSource;

import cpath.warehouse.internal.BioDataTypes.Type;


/**
 * This defines a specific set values for the webservice 
 * parameter 'data_source' (- mainly for backward compatibility).
 * 
 * All the required data sources (as org.bridgedb.DataSource) -
 * networks/pathway data providers, - 
 * are defined in the bioDataTypes bean.
 *
 * @author rodche
 */
public class PathwayDataSource {
	
	private final DataSource dataSource;
	
	public PathwayDataSource(DataSource dataSource) {
		if(dataSource.getType().equals(Type.PATHWAY_DATA.name()))
		{
			this.dataSource = dataSource;
		} else {
			throw new IllegalArgumentException("Not " 
					+ Type.PATHWAY_DATA + "  datasource type!");
		}
	}	
	
	public DataSource asDataSource() {
		return dataSource;
	}
}
