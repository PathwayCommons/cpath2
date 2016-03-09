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
	powered by cPath2 software, version ${project.version}" />
	<link href="<spring:url value='/resources/css/bootstrap.min.css" rel="stylesheet'/>" />
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
	<title>cPath2::Error Page</title>
</head>
<body>
<header class="header">
	<nav id="header_navbar" class="navbar navbar-default navbar-fixed-top" role="navigation">
		<div class="container">
			<div class="navbar-header">
				<%--<button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#top-navbar-collapse">--%>
					<%--<span class="sr-only">Toggle navigation</span>--%>
					<%--<span class="icon-bar"></span>--%>
					<%--<span class="icon-bar"></span>--%>
					<%--<span class="icon-bar"></span>--%>
				<%--</button>--%>
				<a class="navbar-brand" href="${cpath.url}">
					<img alt="Project Team Logo" src="${cpath.logoUrl}" id="team-logo"/>&nbsp;
					<c:out value="${cpath.name} Web Service v${cpath.version}"/>&nbsp;
					<c:if test="${cpath.adminEnabled}"><strong>(Maintenance mode)</strong></c:if>
				</a>
			</div>
			<%--<div class="collapse navbar-collapse pull-right" id="top-navbar-collapse">--%>
				<%--<ul class="nav navbar-nav">--%>
					<%--<li class="dropdown">--%>
						<%--<a href="#" class="dropdown-toggle" data-toggle="dropdown">Web Service<b class="caret"></b></a>--%>
						<%--<span class="dropdown-arrow"></span>--%>
						<%--<ul class="dropdown-menu">--%>
							<%--<spring:url value="/home" var="home" />--%>
							<%--<li><a href="${home}" class="smooth-scroll">About</a></li>--%>
							<%--<li class="divider"></li>--%>
							<%--<li><a href="${home}#search" class="smooth-scroll">Search</a></li>--%>
							<%--<li><a href="${home}#get" class="smooth-scroll">Get</a></li>--%>
							<%--<li><a href="${home}#traverse" class="smooth-scroll">Traverse</a></li>--%>
							<%--<li><a href="${home}#graph" class="smooth-scroll">Graph</a></li>--%>
							<%--<li><a href="${home}#top_pathways" class="smooth-scroll">Top pathways</a></li>--%>
							<%--<li class="divider"></li>--%>
							<%--<li><a href="${home}#parameter_values" class="smooth-scroll">Values</a></li>--%>
						<%--</ul>--%>
					<%--</li>--%>
					<%--<li><a href="<spring:url value='/datasources'/>">Providers</a></li>--%>
					<%--<li><a href="<spring:url value='/downloads'/>">Downloads</a></li>--%>
				<%--</ul>--%>
			<%--</div> <!-- collapse -->--%>
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
			<p class="navbar-text navbar-left">
				Powered by <a target="_blank" <%-- class="navbar-link"  --%>
							  href="https://pathwaycommons.github.io/cpath2/">cPath2</a> v${project.version}.
				<%-- project.version is a Maven var. --%>
				@Copyright 2009-2016 <a href="http://baderlab.org/" target="_blank">University of
				Toronto</a> and <a href="http://www.cbio.mskcc.org" target="_blank">cBio MSKCC</a>
			</p>
		</div>
	</div>
</footer>
</body>
</html>
