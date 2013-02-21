package cpath.webservice.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author rodche
 *
 */
public final class CPathWebserviceLoggingHandlerInterceptor implements
		HandlerInterceptor {
	
	private static final Log LOG = LogFactory.getLog(CPathWebserviceLoggingHandlerInterceptor.class);

	@Override
	public void afterCompletion(HttpServletRequest request,
			HttpServletResponse response, Object handler, Exception arg3)
			throws Exception {		
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response,
			Object handler, ModelAndView mav) throws Exception {	
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
			Object handler) throws Exception {
		
		LOG.info("URI: " + request.getRequestURI() 
//				+ "; Content-Type: " + request.getHeader("Content-Type")
				+ "; Referer: " + request.getHeader("Referer")
				+ "; X-Forwarded-For: " + request.getHeader("X-Forwarded-For")
				+ "; Via: " + request.getHeader("Via")
				+ "; X-Requested-With: " + request.getHeader("X-Requested-With"));
		
		return true; 
		
		//TODO can conditionally return false in order to disable all cpath2 queries (using a system variable or static property)
	}

}
