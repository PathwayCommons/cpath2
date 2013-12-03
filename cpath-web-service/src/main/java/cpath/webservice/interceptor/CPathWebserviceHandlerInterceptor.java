package cpath.webservice.interceptor;

import java.io.File;
import java.net.URLDecoder;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import cpath.log.LogUtils;
import cpath.log.jpa.Geoloc;
import cpath.log.jpa.LogEntitiesRepository;
import cpath.log.jpa.LogEvent;
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
		Assert.notNull(logEntitiesRepository, "logEntitiesRepository is null");
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
			LOG.debug("DOWNLOAD " + ip
					+ "\t" + request.getMethod() 
					+ "\t" + request.getRequestURI()
					+ "\t" + request.getQueryString()
					);
			
			//extract file name from the URI
			String file = requestUri.substring(requestUri.lastIndexOf(File.separator)+1);
			file = URLDecoder.decode(file);
			
			//update counts for: file, format, provider, command (event types)
			Set<LogEvent> events = LogEvent.fromDownloads(file);
			LogUtils.log(logEntitiesRepository, events, Geoloc.fromIpAddress(ip));
			
		}

		return true;
	}

}
