package cpath.dao.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.biopax.paxtools.controller.Cloner;
import org.biopax.paxtools.controller.Completer;
import org.biopax.paxtools.controller.ModelUtils;
import org.biopax.paxtools.controller.PathAccessor;
import org.biopax.paxtools.controller.SimpleMerger;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.Pathway;
import org.biopax.paxtools.model.level3.Provenance;
import org.biopax.paxtools.model.level3.XReferrable;
import org.biopax.paxtools.model.level3.Xref;
import org.biopax.paxtools.util.IllegalBioPAXArgumentException;

import cpath.dao.Analysis;
import cpath.dao.PaxtoolsDAO;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.TraverseEntry;
import cpath.service.jaxb.TraverseResponse;
import cpath.warehouse.WarehouseDAO;

public final class PaxtoolsModelDAO extends AbstractPaxtoolsDAO 
implements PaxtoolsDAO, Model, WarehouseDAO {
	private final static Log log = LogFactory.getLog(PaxtoolsModelDAO.class);
	
	private final Model model;
	private final BioPAXLevel level;
	private final SimpleIOHandler simpleIO;
	private final SimpleMerger simpleMerger;
	private final ModelUtils utils;
	
	public PaxtoolsModelDAO(BioPAXLevel level) {
		this.level = level;
		model = level.getDefaultFactory().createModel();
		simpleIO = new SimpleIOHandler(level);
		simpleMerger = new SimpleMerger(simpleIO.getEditorMap());
		utils = new ModelUtils(model);
	}
	
	@Override
	public void add(BioPAXElement aBioPAXElement) {
		model.add(aBioPAXElement);
	}

	@Override
	public <T extends BioPAXElement> T addNew(Class<T> aClass, String id) {
		return model.addNew(aClass, id);
	}

	@Override
	public boolean contains(BioPAXElement aBioPAXElement) {
		return model.contains(aBioPAXElement);
	}

	@Override
	public BioPAXElement getByID(String id) {
		return model.getByID(id);
	}

	@Override
	public boolean containsID(String id) {
		return model.containsID(id);
	}

	@Override
	public Map<String, String> getNameSpacePrefixMap() {
		return model.getNameSpacePrefixMap(); // immutable!
	}

	@Override
	public Set<BioPAXElement> getObjects() {
		return model.getObjects();
	}

	@Override
	public <T extends BioPAXElement> Set<T> getObjects(Class<T> filterBy) {
		return model.getObjects(filterBy);
	}

	@Override
	public void remove(BioPAXElement aBioPAXElement) {
		model.remove(aBioPAXElement);
	}

	@Override
	public void setFactory(BioPAXFactory factory) {
		model.setFactory(factory);
	}

	@Override
	public BioPAXLevel getLevel() {
		return model.getLevel();
	}

	@Override
	public void setAddDependencies(boolean value) {
		model.setAddDependencies(value);
	}

	@Override
	public boolean isAddDependencies() {
		return model.isAddDependencies();
	}

	@Override
	public void replace(BioPAXElement existing, BioPAXElement replacement) {
		model.replace(existing, replacement);
	}

	@Override
	public void repair() {
		model.repair();
	}

	@Override
	public void setXmlBase(String base) {
		model.setXmlBase(base);
	}

	@Override
	public String getXmlBase() {
		return model.getXmlBase();
	}

	@Override
	public void importModel(File biopaxFile) throws FileNotFoundException {
		if (log.isInfoEnabled()) {
			log.info("Creating biopax model using: " + biopaxFile.getAbsolutePath());
		}

		Model m = simpleIO.convertFromOWL(new FileInputStream(biopaxFile));
		model.merge(m);
	}

	@Override
	public SearchResponse search(String query, int page,
			Class<? extends BioPAXElement> filterByType, String[] dsources,
			String[] organisms) {
		
		return new SearchResponse(); // TODO may use Lucene index later
	}

	@Override
	public void exportModel(OutputStream outputStream, String... ids) {
		simpleIO.convertToOWL(model, outputStream, ids);
	}

	@Override
	public Model getValidSubModel(Collection<String> ids) {
		Analysis analysis = new Analysis() {
			@Override
			public Set<BioPAXElement> execute(Model model, Object... args) {
				Set<BioPAXElement> bioPAXElements = new HashSet<BioPAXElement>();
				for(Object id : args) {
					BioPAXElement bpe = getByID(id.toString());
					if(bpe != null)
						bioPAXElements.add(bpe);
				}
				return bioPAXElements;
			}
		};
		
		return runAnalysis(analysis, ids.toArray());
	}

	@Override
	public void initialize(Object element) {
		// nothing
	}

	@Override
	public void merge(Model source) {
		model.merge(source);
	}

	@Override
	public void merge(BioPAXElement bpe) {
		simpleMerger.merge(model, bpe);
	}

	@Override
	public Model runAnalysis(Analysis analysis, Object... args) {
		// perform
		Set<BioPAXElement> result = analysis.execute(this, args);
		
		if(log.isDebugEnabled())
			log.debug("runAnalysis: finished; now detaching the sub-moodel...");
		
		// auto-complete/detach
		if(result != null) {
			if(log.isDebugEnabled())
				log.debug("runAnalysis: running auto-complete...");
			Completer c = new Completer(simpleIO.getEditorMap());
			result = c.complete(result, null); //null - because the (would be) model is never used there anyway
			if(log.isDebugEnabled())
				log.debug("runAnalysis: cloning...");
			Cloner cln = new Cloner(simpleIO.getEditorMap(), level.getDefaultFactory());
			Model submodel = cln.clone(null, result);
			
			if(log.isDebugEnabled())
				log.debug("runAnalysis: returned");
			return submodel; // new (sub-)model
		} 
		
		if(log.isDebugEnabled())
			log.debug("runAnalysis: returned NULL");
		return null;
	}

	@Override
	public SearchResponse getTopPathways() {
		SearchResponse searchResponse = new SearchResponse();
		
		List<SearchHit> searchHits = searchResponse.getSearchHit();
		for(Pathway pathway : model.getObjects(Pathway.class)) {
			if(pathway.getControlledOf().isEmpty() 
					&& pathway.getPathwayComponentOf().isEmpty()) {
				SearchHit hit = new SearchHit();
				hit.setBiopaxClass("Pathway");
				for(Provenance ds : pathway.getDataSource())
					hit.getDataSource().add(ds.getRDFId());
				hit.setName(pathway.getDisplayName());
				if(pathway.getOrganism() != null)
					hit.getOrganism().add(pathway.getOrganism().getRDFId());
				hit.setUri(pathway.getRDFId());
				searchHits.add(hit);
			}
		}

		searchResponse.setMaxHitsPerPage(searchHits.size());
		searchResponse.setPageNo(0);
		searchResponse.setNumHits(searchHits.size());
		searchResponse.setComment("Top Pathways means, they are neither components of " +
				"other pathways nor controlled of any process)");
		
		return searchResponse;
	}

	@Override
	public TraverseResponse traverse(String propertyPath, String... uris) {
		TraverseResponse resp = new TraverseResponse();
		resp.setPropertyPath(propertyPath);
		PathAccessor pathAccessor = new PathAccessor(propertyPath, getLevel());
		for(String uri : uris) {
			BioPAXElement bpe = getByID(uri);
			try {
				Set<?> v = pathAccessor.getValueFromBean(bpe);
				TraverseEntry entry = new TraverseEntry();
				entry.setUri(uri);
				if(!pathAccessor.isUnknown(v)) {
					for(Object o : v) {
						if(o instanceof BioPAXElement) 
							entry.getValue().add(((BioPAXElement) o).getRDFId());
						else
							entry.getValue().add(String.valueOf(o));
					}
				}
				// add (it might have no values, but the path is correct)
				resp.getTraverseEntry().add(entry); 
			} catch (IllegalBioPAXArgumentException e) {
				// log, ignore if the path does not apply
				if(log.isDebugEnabled())
					log.debug("Failed to get values at: " + 
						propertyPath + " from the element: " + uri, e);
			}
		}
		
		return resp;
	}

	@Override
	public void index() {
		// TODO
	}

	@Override
	public <T extends BioPAXElement> T getObject(String urn, Class<T> clazz) {
		BioPAXElement bpe = getValidSubModel(Collections.singleton(urn)).getByID(urn); //success too!
		
		if(clazz.isInstance(bpe)) {
			return (T) bpe;
		} else {
			if(bpe != null) log.error("getObject(" +
				urn + ", " + clazz.getSimpleName() + 
				"): returned object has different type, " 
				+ bpe.getModelInterface());
			return null;
		}
	}

	@Override
	public Set<String> getByXref(Set<? extends Xref> xrefs,
			Class<? extends XReferrable> clazz) {
		return getByXref(this, xrefs, clazz);
	}

}
