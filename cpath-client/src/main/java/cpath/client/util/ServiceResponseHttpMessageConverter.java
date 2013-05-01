package cpath.client.util;

import cpath.service.jaxb.*;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.transform.stream.StreamSource;

/**
 * Converts the REST response into a {@link ServiceResponse} subclass 
 * or throws a PathwayCommons exception.
 *
 * @see cpath.cpath.client.util.CPathException.PathwayCommonsException
 */
public class ServiceResponseHttpMessageConverter 
	implements HttpMessageConverter<ServiceResponse> 
{
    private static final List<MediaType> mediaList;
    private static final Jaxb2Marshaller jaxb;
    
    static {
    	mediaList = new ArrayList<MediaType>();
        mediaList.add(MediaType.APPLICATION_XML);
        
        jaxb = new Jaxb2Marshaller();
        jaxb.setClassesToBeBound(Help.class,
        		SearchResponse.class, SearchHit.class,
        		TraverseResponse.class, TraverseEntry.class,
        		DataResponse.class, ServiceResponse.class); //the latter two classes may be not required in practice
    }

    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return ServiceResponse.class.isAssignableFrom(clazz);
    }

    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    public List<MediaType> getSupportedMediaTypes() {
        return Collections.unmodifiableList(mediaList);
    }

    public ServiceResponse read(Class<? extends ServiceResponse> clazz, HttpInputMessage inputMessage) 
    	throws IOException, HttpMessageNotReadableException
    {
        // as for cpath-api 5.0.0, the response is either 
        // 'clazz' (ServiceResponce's subclass) or ErrorResponse type!
        return (ServiceResponse) jaxb
        	.unmarshal(new StreamSource(inputMessage.getBody()));
    }

    public void write(ServiceResponse model, MediaType contentType, HttpOutputMessage outputMessage) 
    	throws IOException, HttpMessageNotWritableException
    {
        throw new UnsupportedOperationException("Not supported");
    }
}
