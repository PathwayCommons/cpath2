package cpath.webservice.interceptor;

import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import cpath.log.jpa.LogEntitiesRepository;
import cpath.webservice.BasicController;

/**
 * @author rodche
 * 
 */
public final class CPathWebserviceHandlerInterceptor extends
		HandlerInterceptorAdapter {

	private static final Logger LOG = LoggerFactory
			.getLogger(CPathWebserviceHandlerInterceptor.class);
	
	private LogEntitiesRepository logEntitiesRepository;
	@Autowired
	public void setLogEntitiesRepository(
			LogEntitiesRepository logEntitiesRepository) {
		this.logEntitiesRepository = logEntitiesRepository;
	}

	@Override
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler) throws Exception {

		String requestUri = request.getRequestURI();

		// log accessing some of static resources (defined in the spring xml/conf.)
		if( requestUri.contains("/downloads/") ) 
		{
			String ip = BasicController.clientIpAddress(request);
			
			LOG.info("DOWNLOAD " + ip
					+ "\t" + request.getMethod() 
					+ "\t" + request.getRequestURI()
					+ "\t" + request.getQueryString()
					);
			
			//filename
//			file = file.substring(uri.lastIndexOf('/')+1);
//			file = URLDecoder.decode(file);
			
			//TODO log the event to the DB (repository)
		}

		return true;
	}
}
