<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<header class="header">
	<nav id="header_navbar" class="navbar navbar-default navbar-fixed-top" role="navigation">
        <div class="container">
              <div class="navbar-header">
				<button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#top-navbar-collapse">
				  <span class="sr-only">Toggle navigation</span>
				  <span class="icon-bar"></span>
				  <span class="icon-bar"></span>
				  <span class="icon-bar"></span>
				</button>
				<a class="navbar-brand" href="${cpath.url}">
				 <img alt="Project Team Logo" src="${cpath.logoUrl}" id="team-logo"/>&nbsp;
				 <c:out value="${cpath.name} Web Service v${cpath.version}"/>
				</a>
			  </div>              
              <div class="collapse navbar-collapse pull-right" id="top-navbar-collapse">
                  <ul class="nav navbar-nav">
					<li><a href="<spring:url value='/home'/>">About</a></li>
					<li><a href="<spring:url value='/datasources'/>">Providers</a></li>
					<li><a href="${cpath.url}/archives">Downloads</a></li>
                  </ul>
          	  </div> <!-- collapse -->
      </div> <!-- container -->
 	</nav>
</header>
<!-- begin the #content container that will be closed in the footer.jsp-->
<div id="content" class="container nav-target">
