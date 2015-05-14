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

import cpath.jpa.LogEvent;
import cpath.service.CPathService;
//import cpath.service.Status;

/**
 * @author rodche
 */
@Component
public final class CPathWebserviceHandlerInterceptor extends
		HandlerInterceptorAdapter {

	private static final Logger LOG = LoggerFactory
			.getLogger(CPathWebserviceHandlerInterceptor.class);
	
	@Autowired
	private CPathService service;

	@Override
	public void postHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception 
	{	
		String requestUri = request.getRequestURI();
		String ip = BasicController.clientIpAddress(request);
		
		if(!requestUri.startsWith("/favicon.ico"))
			LOG.info(String.format("%d %s '%s'", response.getStatus(), ip, requestUri));
		
		if(response.getStatus() == HttpServletResponse.SC_OK
				|| response.getStatus() == HttpServletResponse.SC_NOT_MODIFIED) {
			// log accessing some of static resources (defined in the spring xml/conf.)
			if( requestUri.contains("/downloads/") || requestUri.contains("/datadir/") 
					|| requestUri.contains("/validations/")) 
			{
				//extract file name from the URI
				String file = requestUri.substring(requestUri.lastIndexOf("/")+1);
				if(!file.isEmpty()) {
					file = URLDecoder.decode(file);
					int idx = file.lastIndexOf(";jsession");
					if(idx>0)
						file = file.substring(0, idx);

					//update counts for: file, format, provider, command (event types)
					if(!file.isEmpty())
						service.log(LogEvent.fromDownloads(file), ip);
				}
			}
		} else if(response.getStatus()>=400) { 
			//if it's an error and not other stuff...
			//- must have been already counted/logged
//			Status cpathStatus = Status.fromCode(response.getStatus());
//			if(cpathStatus==null) 
//				cpathStatus=Status.BAD_REQUEST;
//			service.log(Collections.singleton(LogEvent.from(cpathStatus)), ip);
		}
		
		super.postHandle(request, response, handler, modelAndView);
	}
}
