package cpath.dao.internal;

import static org.biopax.paxtools.impl.BioPAXElementImpl.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.util.Version;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.BioSource;
import org.biopax.paxtools.model.level3.Named;
import org.biopax.paxtools.model.level3.Provenance;
import org.biopax.paxtools.model.level3.UnificationXref;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.paxtools.util.ClassFilterSet;
import org.hibernate.transform.ResultTransformer;

import cpath.dao.PaxtoolsDAO;
import cpath.service.jaxb.SearchHit;


/**
 * Creates {@link SearchHit} objects from {@link BioPAXElement}
 * and its cPath2 Licene Documents.
 * 
 * Custom {@link ResultTransformer} implementation 
 * for full-text search results and projections used 
 * in {@link PaxtoolsDAO#search(String, int, Class, String[], String[])}
 * and {@link PaxtoolsDAO#getTopPathways()}.
 * 
 * We suppose, 'organism', 'dataSource' (URIs), and 'keyword' 
 * search fields values were stored in the Lucene index.
 */
final class SearchHitsTransformer implements ResultTransformer {
	private final Highlighter highlighter;
	private static final Analyzer ANALYZER =
			new StandardAnalyzer(Version.LUCENE_31);
	
	public SearchHitsTransformer(Highlighter highlighter) {
		this.highlighter = highlighter;
	}
	
	@Override //the annotation must be present, for safety, because I changed return Object to SearchHit (it's ok in Java1.6)
	public SearchHit transformTuple(Object[] tuple, String[] aliases) {
		BioPAXElement bpe = (BioPAXElement) tuple[0];
		Document doc = (Document) tuple[1];
		
		SearchHit hit = new SearchHit();
		
        hit.setUri(bpe.getRDFId());
        hit.setBiopaxClass(bpe.getModelInterface().getSimpleName());
		
		// add standard and display names if any -
		if (bpe instanceof Named) {
			Named named = (Named) bpe;
			String std = named.getStandardName();
			if (std != null)
				hit.setName(std);
			else
				hit.setName(named.getDisplayName());
			
			// a hack for BioSource (store more info)
			if(bpe instanceof BioSource) {
				for(String name : named.getName())
					hit.getOrganism().add(name);
				String txid = getTaxonId((BioSource)named);
				if(txid != null)
					hit.getOrganism().add(txid);
			}
			
			// a hack for Provenance: save/return other names 
			// (to be used as filter by data source values) in the dataSource element 
			if(bpe instanceof Provenance) {
				for(String name : named.getName())
					hit.getDataSource().add(name);
			}	
		}
		
		
		// extract only organism URIs (no names, etc..)
		if(doc.getField(FIELD_ORGANISM) != null) {
			Set<String> uniqueVals = new TreeSet<String>();
			for(String o : doc.getValues(FIELD_ORGANISM)) {
				if(o.startsWith("urn:") || o.startsWith("http:")) {
					uniqueVals.add(o);
				}
			}
			hit.getOrganism().addAll(uniqueVals);
		}
		
		// extract values form the index
		if(doc.getField(FIELD_DATASOURCE) != null) {
			Set<String> uniqueVals = new TreeSet<String>();
			for(String d : doc.getValues(FIELD_DATASOURCE)) {
				if(d.startsWith("urn:") && d.startsWith("http:")) { 
					uniqueVals.add(d);
				}
			}
			hit.getDataSource().addAll(uniqueVals);
		}	
		
		// extract only pathway URIs
		if(doc.getField(FIELD_PATHWAY) != null) {
			Set<String> uniqueVals = new TreeSet<String>();
			for(String d : doc.getValues(FIELD_PATHWAY)) {
				try {
					if(URI.create(d).isAbsolute()) 
						uniqueVals.add(d);
				} catch(IllegalArgumentException e) {/*skip*/}
			}
			hit.getPathway().addAll(uniqueVals);
		}
		
		// use the highlighter (get matching fragments)
		if (highlighter != null) {
			final List<String> frags = new ArrayList<String>();
			try {
				if (doc.getField(FIELD_KEYWORD) != null) {
					final String text = StringUtils.join(doc.getValues(FIELD_KEYWORD), " ");					
					for(String fr : highlighter.getBestFragments(ANALYZER,
							FIELD_KEYWORD, text, 5)) {
						frags.add(fr);
					}
				}

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
			if(!frags.isEmpty())
				hit.setExcerpt(frags.toString());
		}
		
		return hit;
	}
	
	// when no projection used
	@Override
	public List<SearchHit> transformList(List collection) {
		throw new UnsupportedOperationException("Must use projection with this results transformer!");
	}

	
	private static String getTaxonId(BioSource bioSource) {
		String id = null;
		if(!bioSource.getXref().isEmpty()) {
			Set<UnificationXref> uxs = new 
				ClassFilterSet<Xref,UnificationXref>(bioSource.getXref(), 
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
	
}
