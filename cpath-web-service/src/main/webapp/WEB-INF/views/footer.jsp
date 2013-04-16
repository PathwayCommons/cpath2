<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags"%>

	<footer>
		<ul>
			<li>
				Go back to <a href="<c:url value='/' />">Home Page</a> / <a href="#">Top of this page</a> 
			</li>
			<li>
				<security:authorize ifAnyGranted="ROLE_USER">
					Logged as: <strong><security:authentication property="principal.username"/></strong>
						<security:authorize ifAnyGranted="ROLE_ADMIN">
							<strong>and have Admin rights</strong>. 
						</security:authorize>  
					<a href="<c:url value='/j_spring_security_logout' />">Sign Out</a> 
				</security:authorize>
			</li>
			<li>
				${cpath.name} version ${cpath.version} server.
			</li>
			<li>
			  <div class="marquee">
				Powered by <a rel="external"
				href="http://code.google.com/p/pathway-commons/">cPath2</a>, version ${project.version}. 
				@Copyright 2009-2013 <a href="http://baderlab.org/" rel="external">University of
				Toronto</a> and <a href="http://www.cbio.mskcc.org" rel="external">Memorial Sloan-Kettering Cancer Center</a>
			  </div>
				
			</li>
		</ul>
	</footer>
