package cpath.webservice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import cpath.config.CPathSettings;

/**
 * @author rodche
 *
 */
@Component
public final class CPathMaintenanceHandlerInterceptor extends HandlerInterceptorAdapter
{
	private static final Logger LOG = LoggerFactory.getLogger(CPathMaintenanceHandlerInterceptor.class);

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
			Object handler) throws Exception {
		
		String requestUri = request.getRequestURI();
		
		//disable user web service queries in admin mode is enabled
		if(CPathSettings.getInstance().isAdminEnabled() 				
			&& !(  requestUri.contains("/resources") 
				|| requestUri.contains("/help")
				|| requestUri.contains("/error")
				|| requestUri.contains("/home")
				|| requestUri.contains("/datasources")
				|| requestUri.contains("/metadata/")
				|| requestUri.contains("/favicon")
				|| requestUri.contains("/logback")
				|| requestUri.contains("/formats")
				|| requestUri.contains("/downloads")
				|| requestUri.contains("/robots")
				)
		)
		{
			try {
				response.sendError(503, CPathSettings.getInstance().property(CPathSettings.PROVIDER_NAME)
						+ " service maintenance.");
			} catch (Exception e) {
				LOG.error("preHandle: request URI: " + requestUri + "; response.sendError failed" + e);
			}

			return false;
		}
		else
			return true; 
	}

}
