package cpath.webservice.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * @author rodche
 * 
 */
public final class CPathWebserviceHandlerInterceptor extends
		HandlerInterceptorAdapter {

	private static final Logger LOG = LoggerFactory
			.getLogger(CPathWebserviceHandlerInterceptor.class);

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
