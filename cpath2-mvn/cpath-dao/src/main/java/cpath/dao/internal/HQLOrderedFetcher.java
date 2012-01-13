package cpath.dao.internal;

import org.biopax.paxtools.controller.OrderedFetcher;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.model.BioPAXElement;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 */
public class HQLOrderedFetcher extends OrderedFetcher {
    private PaxtoolsHibernateDAO dao;

    public HQLOrderedFetcher(PaxtoolsHibernateDAO dao,boolean fetchAttributes)
    {
        super(fetchAttributes);
        this.dao = dao;
    }

    @Override
    public Set<BioPAXElement> fetch(Set<? extends BioPAXElement> elements)
    {
        return super.fetch(elements);
    }

    @Override
    protected Set getValuesFromBeans(Set<? extends BioPAXElement> elements, PropertyEditor editor)
    {
        return dao.getByProperty(editor,elements);
    }
}
