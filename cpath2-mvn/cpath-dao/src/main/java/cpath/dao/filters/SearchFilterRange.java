package cpath.dao.filters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.biopax.paxtools.model.BioPAXElement;

@Target(ElementType.TYPE)
public @interface SearchFilterRange 
{
	Class<? extends BioPAXElement> value() default BioPAXElement.class;
}
