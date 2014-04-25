<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags"%>

</div><!--closing tag for the common #content .container, opened in the header.jsp-->

<footer class="footer">
	<div id="footer_navbar" class="navbar navbar-default navbar-fixed-bottom">
          <div class="container">	
			<security:authorize ifAnyGranted="ROLE_USER">
			  <p class="navbar-text navbar-left">Logged as &quot;
			    <strong>
			      <security:authentication property="principal.username"/>&quot;
				  <security:authorize ifAnyGranted="ROLE_ADMIN"> (Administrator) </security:authorize>
				</strong>
				<a href="<c:url value='/j_spring_security_logout' />">Log Out</a>
			  </p>
			</security:authorize>
			  <p class="navbar-text navbar-left">
				Powered by <a target="_blank" <%-- class="navbar-link"  --%>
				href="http://code.google.com/p/pathway-commons/">cPath2</a> v${project.version}. 
				@Copyright 2009-2014 <a href="http://baderlab.org/" target="_blank">University of
				Toronto</a> and <a href="http://www.cbio.mskcc.org" target="_blank">cBio MSKCC</a>
			  </p>				
		 </div>
	</div>	
</footer>

<!-- <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js"></script> -->
<script src="<c:url value="/resources/scripts/jquery.min.js"/>"></script>
<!-- <script src="//netdna.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js"></script> -->
<script src="<c:url value="/resources/scripts/bootstrap.min.js"/>"></script>
<script src="<c:url value="/resources/scripts/bootstrap-switch.min.js"/>"></script>
<script src="<c:url value="/resources/scripts/bootstrap-select.min.js"/>"></script>
<script src="<c:url value="/resources/scripts/jquery.placeholder.js"/>"></script>

<!--[if lt IE 8]>
      <script src='<c:url value="/resources/scripts/icon-font-ie7.js"/>'></script>
      <script src='<c:url value="/resources/scripts/lte-ie7-24.js"/>'></script>
<![endif]-->

<!-- <script src="http://ajax.googleapis.com/ajax/libs/angularjs/1.2.15/angular.min.js"></script> -->
<script src="<c:url value="/resources/scripts/angular.js"/>"></script>
<!-- <script src="http://ajax.googleapis.com/ajax/libs/angularjs/1.2.15/angular-route.min.js"></script> -->
<script src="<c:url value="/resources/scripts/angular-route.js"/>"></script>

<script src="<c:url value="/resources/scripts/pc.js"/>"></script>
