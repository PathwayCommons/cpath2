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

package cpath.webservice.validation;

import org.bridgedb.DataSource;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import cpath.warehouse.internal.BioDataTypes;
import cpath.webservice.jaxb.ErrorType;
import cpath.webservice.validation.protocol.ProtocolStatusCode;

/**
 * Checks if DataSource is not of BioDataTypes.NETWORK_TYPE.
 * 
 * @author rodche
 *
 */
public class CPathDataSourceValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return clazz.equals(DataSource.class);
	}

	@Override
	public void validate(Object target, Errors errors) {
		DataSource ds = (DataSource) target;
		//if(!CPathDataSource.contains(ds)) { // no good.., too limiting (for old cPath only)
		if(!ds.getType().equals(BioDataTypes.NETWORK_TYPE)) {
			ErrorType e = ProtocolStatusCode.INVALID_ARGUMENT.getError();
			e.setErrorDetails("Value for 'data_source' does not match any " +
				"pathway data provider's name (who's data were merged into " +
				"our system).");
			errors.reject(e.getErrorCode().toString(), ProtocolStatusCode.marshal(e));
		}
	}

}
