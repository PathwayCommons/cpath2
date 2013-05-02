package cpath.webservice.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * @author rodche
 * 
 */
public final class CPathWebserviceHandlerInterceptor extends
		HandlerInterceptorAdapter {

	private static final Log LOG = LogFactory
			.getLog(CPathWebserviceHandlerInterceptor.class);

	@Override
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler) throws Exception {

		String requestUri = request.getRequestURI();

		// do not log about accessing css, images, scripts...
		if (!(requestUri.contains("/resources/") 
		))
			LOG.info("URI: "
					+ requestUri
					+ "; Parameters: " + request.getQueryString()
					+ "; Referer: " + request.getHeader("Referer")
					+ "; User-Agent: " + request.getHeader("User-Agent")
					);

		return true;
	}
	
}
