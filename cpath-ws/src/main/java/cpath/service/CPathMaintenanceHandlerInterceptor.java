package cpath.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cpath.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;


@Component
public final class CPathMaintenanceHandlerInterceptor extends HandlerInterceptorAdapter
{
	private static final Logger LOG = LoggerFactory.getLogger(CPathMaintenanceHandlerInterceptor.class);

	@Autowired
	private Settings settings;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    {
		String requestUri = request.getRequestURI();
		
		//disable user web service queries in admin mode is enabled
		if(settings.isAdminEnabled()
			&& !(  requestUri.contains("/resources") 
				|| requestUri.contains("/help")
				|| requestUri.contains("/error")
				|| requestUri.contains("/home")
				|| requestUri.contains("/datasources")
				|| requestUri.contains("/metadata/")
				|| requestUri.contains("/favicon")
				|| requestUri.contains("/logback")
				|| requestUri.contains("/formats")
				|| requestUri.contains("/robots")
				)
		)
		{
			try {
				response.sendError(503, settings.getProviderName() + " service maintenance.");
			} catch (Exception e) {
				LOG.error("preHandle: request URI: " + requestUri + "; response.sendError failed" + e);
			}

			return false;
		}
		else
			return true; 
	}

}
