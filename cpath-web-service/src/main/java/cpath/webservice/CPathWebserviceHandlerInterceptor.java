package cpath.webservice;

import java.net.URLDecoder;
//import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import cpath.service.CPathService;
//import cpath.service.Status;

/**
 * @author rodche
 */
@Component
public final class CPathWebserviceHandlerInterceptor extends
		HandlerInterceptorAdapter {

	private static final Logger LOG = LoggerFactory.getLogger(CPathWebserviceHandlerInterceptor.class);
	
	@Autowired
	private CPathService service;

	@Override
	public void postHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception 
	{	
		String requestUri = request.getRequestURI();
		if(requestUri.contains(";jsessionid"))
			requestUri = requestUri.substring(0, requestUri.indexOf(";jsessionid"));
		String ip = BasicController.clientIpAddress(request);

		if(response.getStatus() == HttpServletResponse.SC_OK
				|| response.getStatus() == HttpServletResponse.SC_NOT_MODIFIED) {
			// log accessing some of static resources (defined in the spring xml/conf.)
			if( requestUri.contains("/downloads/")
				|| requestUri.contains("/datadir/")
				|| requestUri.contains("/validations/"))
			{
				//extract file name from the URI
				String file = requestUri.substring(requestUri.lastIndexOf("/") + 1);
				if(!file.isEmpty()) {
					file = URLDecoder.decode(file, "UTF-8");
					//update counts for: file, format, provider, command (event types)
					if(!file.isEmpty())
						service.log(file, ip);
				}
			}
		}
		
		super.postHandle(request, response, handler, modelAndView);
	}
}
