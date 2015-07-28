<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags"%>

</div><!--closing tag for the common #content .container, opened in the header.jsp-->

<footer class="footer">
	<div id="footer_navbar" class="navbar navbar-default navbar-fixed-bottom">
          <div class="container">	
			<security:authorize access="hasAnyRole('ROLE_ADMIN','ROLE_USER')">
			  <p class="navbar-text navbar-left">Logged as &quot;
			    <strong>
			      <security:authentication property="principal.username"/>&quot;
				  <security:authorize access="hasRole('ROLE_ADMIN')"> (Administrator) </security:authorize>
				</strong>
				<a href="<c:url value='/logout' />">Log Out</a>
			  </p>
			</security:authorize>
			  <p class="navbar-text navbar-left">
				Powered by <a target="_blank" <%-- class="navbar-link"  --%>
				href="https://github.com/PathwayCommons/cpath2">cPath2</a> v${project.version}. 
				@Copyright 2009-2014 <a href="http://baderlab.org/" target="_blank">University of
				Toronto</a> and <a href="http://www.cbio.mskcc.org" target="_blank">cBio MSKCC</a>
			  </p>				
		 </div>
	</div>	
</footer>
