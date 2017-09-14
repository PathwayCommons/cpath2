<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page isErrorPage="true" language="java" contentType="text/html; charset=UTF-8"%>
<%-- no sessions on this ERROR page --%>
<!DOCTYPE html>
<html>
<head>
	<meta charset="utf-8" />
	<meta http-equiv="X-UA-Compatible" content="IE=edge">
	<meta name="viewport" content="width=device-width, initial-scale=1.0">
	<meta name="author" content="${cpath.name}" />
	<meta name="description" content="${cpath.name} web services, version ${cpath.version},
	powered by cPath2 software, version @project.version@" />
	<link href="<spring:url value='/resources/css/bootstrap.min.css'/>" rel="stylesheet" />
	<%-- <link href="//netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css" rel="stylesheet"> --%>
	<link href="<spring:url value='/resources/css/pc.css'/>" rel="stylesheet" />
	<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js"></script>
	<%-- <script src="<spring:url value='/resources/scripts/jquery.min.js'/>"></script> --%>
	<%-- <script src="http://netdna.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js"></script> --%>
	<script src="<spring:url value='/resources/scripts/bootstrap.min.js'/>"></script>
	<script src="<spring:url value='/resources/scripts/jquery.placeholder.js'/>"></script>
	<script src="<spring:url value='/resources/scripts/pc.js'/>"></script>
	<!--[if lt IE 8]>
	<script src="<spring:url value='/resources/scripts/icon-font-ie7.js'/>"></script>
	<script src="<spring:url value='/resources/scripts/lte-ie7-24.js'/>"></script>
	<![endif]-->
	<%-- HTML5 shim, for IE6-8 support of HTML5 elements --%>
	<!--[if lt IE 9]>
	<script src="<spring:url value='/resources/scripts/html5shiv.js'/>"></script>
	<![endif]-->
	<title>cPath2::Error (${cpath.name})</title>
	<script>
		(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
					(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
				m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
		})(window,document,'script','//www.google-analytics.com/analytics.js','ga');
		ga('create', '${cpath.ga}', 'auto');
		ga('send', 'pageview');
	</script>
</head>
<body>
<header class="header">
	<nav id="header_navbar" class="navbar navbar-default navbar-fixed-top" role="navigation">
		<div class="container">
			<div class="navbar-header">
				<a class="navbar-brand" href="${cpath.url}">
					<img alt="Project Team Logo" src="${cpath.logoUrl}" id="team-logo"/>&nbsp;
					<c:out value="${cpath.name} Web Service v${cpath.version}"/>&nbsp;
					<c:if test="${cpath.adminEnabled}"><strong>(Maintenance mode)</strong></c:if>
				</a>
			</div>
		</div> <!-- container -->
	</nav>
</header>
<div id="content" class="container nav-target">
	<h2>Error <c:out value="${requestScope['javax.servlet.error.status_code']}" /></h2>
	<p><c:out value="${requestScope['javax.servlet.error.message']}" /></p>
	<h3>Details:</h3>
	<ul>
		<li>Action: <c:out value="${requestScope['javax.servlet.forward.request_uri']}" />
		<li>Query String: <c:out value="${requestScope['javax.servlet.forward.query_string']}" />
		<li>Exception: <c:out value="${requestScope['javax.servlet.error.exception']}" />
	</ul>
</div>
<footer class="footer">
	<div id="footer_navbar" class="navbar navbar-default navbar-fixed-bottom">
		<div class="container">
			<p class="navbar-text navbar-left"> <%--project.version is a Maven var. to inject--%>
				<small><a target="_blank" href="https://pathwaycommons.github.io/cpath2/">cPath2</a> v@project.version@.
				 &copy; 2006-2016 <a href="http://baderlab.org/" target="_blank">Bader Lab</a> (UofT),
				<a href="http://www.sanderlab.org" target="_blank">cBio</a> (MSKCC; DFCI, HMS) and Demir Lab (OHSU).
				</small>
			</p>
		</div>
	</div>
</footer>
</body>
</html>
