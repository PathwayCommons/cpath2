package cpath.dao.internal;

import static org.biopax.paxtools.impl.BioPAXElementImpl.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.util.Version;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.level3.Named;
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
		}
		
		// extract only organism URNs (no names, etc..)
		if(doc.getField(FIELD_ORGANISM) != null) {
			List<String> l = hit.getOrganism();
			for(String o : doc.getValues(FIELD_ORGANISM)) {
				if(o.startsWith("urn") && !l.contains(o)) {
					l.add(o);
				}
			}
		}
		
		// extract only dataSource URNs
		if(doc.getField(FIELD_DATASOURCE) != null) {
			List<String> l = hit.getDataSource();
			for(String d : doc.getValues(FIELD_DATASOURCE)) {
				if(d.startsWith("urn") && !l.contains(d)) {
					l.add(d);
				}
			}
		}	
		
		// extract only pathway URIs
		if(doc.getField(FIELD_PATHWAY) != null) {
			List<String> l = hit.getPathway();
			for(String d : doc.getValues(FIELD_PATHWAY)) {
				try {
					if(URI.create(d).isAbsolute()) //skip if throws
						l.add(d);
				} catch(IllegalArgumentException e) {}
			}
		}
		
		// use the highlighter (get matching fragments)
		if (highlighter != null) {
			final List<String> frags = new ArrayList<String>();
			try {
				if (doc.getField(FIELD_KEYWORD) != null) {
					final String text = StringUtils.join(doc.getValues(FIELD_KEYWORD));
					//a trick: name and comment field values were not actually stored in the index doc
					String s = highlighter.getBestFragment(ANALYZER, FIELD_NAME, text);
					if(s != null) 
						frags.add(s);
					s = highlighter.getBestFragment(ANALYZER, FIELD_COMMENT, text);
					if(s != null) 
						frags.add(s);
					
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
}
