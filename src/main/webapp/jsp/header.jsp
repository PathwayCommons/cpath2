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
				 <img alt="Project Team Logo" src="${cpath.logo}" id="team-logo"/>&nbsp;
				 <c:out value="${cpath.organization}"/>&nbsp;
				</a>
			  </div>              
              <div class="collapse navbar-collapse pull-right" id="top-navbar-collapse">
                  <ul class="nav navbar-nav">
					<li class="dropdown">
                  		<a href="#" class="dropdown-toggle" data-toggle="dropdown">
                  		  <b class="caret"></b> MENU <b class="caret"></b>
                  		</a>
                  		<span class="dropdown-arrow"></span>
                  		<ul class="dropdown-menu">
                  		 <li><a href="home" class="smooth-scroll">About</a></li>
                  		 <li><a href="swagger" target="_blank">API</a></li>
                  		 <li><a href="datasources">Datasources</a></li>
                         <li><a href="${cpath.downloads}" target="_blank">Downloads</a></li>
                  		 <li class="divider"></li>
               			 <li><a href="home#search" class="smooth-scroll">/search</a></li>
               			 <li><a href="home#fetch" class="smooth-scroll">/fetch</a></li>
               			 <li><a href="home#traverse" class="smooth-scroll">/traverse</a></li>
               			 <li><a href="home#graph" class="smooth-scroll">/graph</a></li>
               			 <li><a href="home#top_pathways" class="smooth-scroll">/top_pathways</a></li>
                    	 <li><a href="home#parameter_values" class="smooth-scroll">parameter values</a></li>
                    	</ul>
                	</li>
                  </ul>
          	  </div> <%-- collapse --%>
      </div> <%-- container --%>
 	</nav>
</header>
<%-- begin the #content container that will be closed in the footer.jsp --%>
<div id="content" class="container nav-target">
