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


package cpath.service.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.controller.Completer;
import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.io.simpleIO.SimpleExporter;
import org.biopax.paxtools.io.simpleIO.SimpleReader;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cpath.dao.PaxtoolsDAO;
import cpath.dao.internal.PaxtoolsHibernateDAO;
import cpath.service.CPathService;
import cpath.warehouse.CPathWarehouse;
import cpath.webservice.args.OutputFormat;

@Service
@Repository
public class CPathServiceImpl implements CPathService {
	private static final Log log = LogFactory.getLog(CPathServiceImpl.class);
	
	@NotNull
	private PaxtoolsDAO mainhouse;
	@NotNull
    private CPathWarehouse warehouse;
	
	private final SimpleReader reader; // to allow BioPAX OWL in queries
	private final SimpleExporter exporter;
	private final SimpleMerger merger;
	
	public CPathServiceImpl(PaxtoolsDAO paxtoolsDAO, CPathWarehouse cPathWarehouse) {
		this.reader = new SimpleReader(BioPAXLevel.L3);
		this.exporter = new SimpleExporter(BioPAXLevel.L3);
		this.merger = new SimpleMerger(reader.getEditorMap());
		this.mainhouse = paxtoolsDAO;
		this.warehouse = cPathWarehouse;
	}
	
	
    @PostConstruct
    void init() {
    	/* attempts to re-build the lucene index (TODO is this required?)
    	 * result in mysql driver issue: too many connections..?
    	 */
    	// mainhouse.createIndex();
    	// warehouse.createIndex();
    }


	@Override
	@Transactional
	public Map<ResultMapKey, Object> list(
			Class<? extends BioPAXElement> biopaxClass, boolean countOnly) {
		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();

		try {
			if (countOnly) {
				Integer count = (Integer) ((PaxtoolsHibernateDAO) mainhouse)
					.session().createQuery("select count(*) from "
						+ biopaxClass.getCanonicalName()).uniqueResult();
				map.put(ResultMapKey.COUNT, count.intValue());
			} else {
				Set<String> data = new HashSet<String>();
				Set<? extends BioPAXElement> results = mainhouse.getElements(
						biopaxClass, false);
				for (BioPAXElement e : results) {
					data.add(e.getRDFId());
				}
				map.put(ResultMapKey.DATA, data);
				map.put(ResultMapKey.COUNT, Integer.valueOf(data.size()));
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
		Map<ResultMapKey, Object> map = null;

		try {

			switch (format) {
			case BIOPAX:
				map = asBiopax(id);
			case BINARY_SIF:
				//TODO
				break;

			default:
			}

		} catch (Exception e) {
			map.put(ResultMapKey.ERROR, e.toString());
		}
		return map;
	}
	

	@Override
	@Transactional
	public Map<ResultMapKey, Object> list(String queryStr,
			Class<? extends BioPAXElement> biopaxClass, boolean countOnly) {
		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();
		try {
			if (countOnly) {
				// TODO count
				map.put(ResultMapKey.COUNT, Integer.valueOf(0));
				map.put(ResultMapKey.MISC, "test stub (0)");
			} else {
				List<BioPAXElement> results = (List<BioPAXElement>) mainhouse
						.search(queryStr, biopaxClass);
				Set<String> idSet = new HashSet<String>(results.size());
				for (BioPAXElement e : results) {
					idSet.add(e.getRDFId());
				}
				map.put(ResultMapKey.DATA, idSet);
				map.put(ResultMapKey.COUNT, Integer.valueOf(idSet.size()));
			}
		} catch (Exception e) {
			map.put(ResultMapKey.ERROR, e.toString());
		}

		return map;
	}	

	
	
	/*=== Private Methods ===*/
	
	@Transactional
	Map<ResultMapKey, Object> asBiopax(String id) {
		Map<ResultMapKey, Object> map = new HashMap<ResultMapKey, Object>();
		
		Model m = mainhouse.getLevel().getDefaultFactory().createModel();
		// find element by id
		BioPAXElement element = mainhouse.getByID(id);
		if (element != null) {
			// auto-complete!
			// warn: completer instance has internal set of elements that is not cleared!
			Completer completer = new Completer(reader.getEditorMap());
			Set<BioPAXElement> elements = completer.complete(Collections
					.singletonList(element), mainhouse);
			for (BioPAXElement bpe : elements) {
				m.add(bpe);
			}
			map.put(ResultMapKey.MODEL, m);
			map.put(ResultMapKey.DATA, toOWL(m));
		}
		
		return map;
	}
}
