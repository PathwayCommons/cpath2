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


package cpath.dao.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import javax.validation.constraints.NotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import cpath.dao.CPathService;
import cpath.dao.PaxtoolsDAO;

/**
 * Service tier class - to uniformly access 
 * BioPAX model (DAO) from console and web service 
 * applications.
 * 
 * @author rodche
 *
 * TODO It's not finished at all; - add/implement methods, debug!
 */
@Service
@Repository
public class CPathServiceImpl implements CPathService {
	private static final Log log = LogFactory.getLog(CPathServiceImpl.class);
	
	@NotNull
	private PaxtoolsDAO dao;
	
	private final SimpleReader reader;
	private final SimpleExporter exporter;
	private final SimpleMerger merger;
	
	public CPathServiceImpl(PaxtoolsDAO paxtoolsDAO) {
		this.dao = paxtoolsDAO;
		this.reader = new SimpleReader(BioPAXLevel.L3);
		this.exporter = new SimpleExporter(BioPAXLevel.L3);
		this.merger = new SimpleMerger(reader.getEditorMap());
	}


	@Override
	public Map<ResultMapKey, Object> list(String queryStr, 
			Class<? extends BioPAXElement> biopaxClass, boolean countOnly) {
		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();

		try {
			if (countOnly) {
				Integer count =  dao.count(null, biopaxClass);
				map.put(ResultMapKey.COUNT, count);
			} else {
				Collection<String> data = dao.find(queryStr, biopaxClass);
				map.put(ResultMapKey.DATA, data);
				map.put(ResultMapKey.COUNT, data.size()); // becomes Integer
			}
		} catch (Exception e) {
			map.put(ResultMapKey.ERROR, e.toString());
		}
		
		return map;
	}

	
	@Override
	public String toOWL(Model model) {
		if(model == null) return null;
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			exporter.convertToOWL(model, out);
		} catch (IOException e) {
			log.error(e);
		}
		return out.toString();
	}

	
	@Override
	public Map<ResultMapKey, Object> element(String id, OutputFormat format) {
		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();
		try {
			switch (format) {
			case BINARY_SIF: // TODO
				
				break;
			case GSEA: // TODO
				break;
			case PC_GENE_SET: // TODO
				break;
			case SBML: // TODO
				break;
			case XML: 
				// also return as BioPAX... 
				// TODO map to the legacy cpath xml schema format on the client's side
			case BIOPAX: // is default
			default:
				map = asBiopax(id);
			}
		} catch (Exception e) {
			map.put(ResultMapKey.ERROR, e.toString());
		}
		return map;
	}	
	
	
	/*
	 * Gets the element (first-level object props are initialized) and 
	 * the valid sub-model of it; and returns the map result.
	 */
	Map<ResultMapKey, Object> asBiopax(String id) {
		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();
		BioPAXElement element = dao.getByID(id);
		if (element != null) {
			dao.initialize(element);
			map.put(ResultMapKey.ELEMENT, element);
			Model m = dao.getValidSubModel(Collections.singleton(id));
			map.put(ResultMapKey.MODEL, m);
			map.put(ResultMapKey.DATA, toOWL(m));
			map.put(ResultMapKey.COUNT, 1);
		} 
		
		return map;
	}
}
