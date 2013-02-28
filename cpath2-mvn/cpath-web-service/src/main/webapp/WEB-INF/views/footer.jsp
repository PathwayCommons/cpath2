<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags"%>

	<div id="footer">
		<ul>
			<li>
				Go back to <a href="<c:url value='/' />">Home Page</a> / <a href="#header">Top of this page</a> 
			</li>
			<li>
				<security:authorize ifAnyGranted="ROLE_ADMIN">
					You've got <strong>Admin privileges</strong>.
				</security:authorize> 
				<security:authorize ifAnyGranted="ROLE_USER">
					User: <strong><security:authentication property="principal.username"/></strong>. 
					<a href="<c:url value='/j_spring_security_logout' />">Sign Out</a> 
				</security:authorize>
			</li>
			<li>
				<fmt:message key="cpath2.provider"/> version <fmt:message key="cpath2.data.version"/> server. 
				<strong>${maintenanceMode}</strong>
			</li>
			<li>Powered by <a
				href="http://code.google.com/p/pathway-commons/">cPath2</a> version ${project.version}. 
<!-- 				@Copyright 2009-2013 <a href="http://baderlab.org/">University of -->
<!-- 				Toronto</a> and <a href="http://www.cbio.mskcc.org">Memorial Sloan-Kettering Cancer Center</a> -->
			</li>
		</ul>
	</div>
