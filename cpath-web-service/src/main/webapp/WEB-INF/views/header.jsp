<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags"%>
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
				 <c:out value="${cpath.name} Web Service v${cpath.version}"/>&nbsp;
				 <c:if test="${cpath.adminEnabled}"><strong>(Maintenance mode)</strong></c:if>
				</a>
			  </div>              
              <div class="collapse navbar-collapse pull-right" id="top-navbar-collapse">
                  <ul class="nav navbar-nav">
 		  			<security:authorize ifAnyGranted="ROLE_ADMIN">
					  <li><a href="<c:url value="/admin"/>">Admin</a></li>
					</security:authorize>
					<li class="dropdown">
                  		<a href="#" class="dropdown-toggle" data-toggle="dropdown">Web App<b class="caret"></b></a>
                  		<span class="dropdown-arrow"></span>
                  		<ul class="dropdown-menu">
                  		 <li><a href="<c:url value="/view"/>">About</a></li>
                  		 <li><a href="<c:url value="/view/#/pw"/>">Pathways</a></li>	
                    	 <li class="divider"></li>
                    	 <li><a href="<c:url value="/metadata/validations"/>">Validations</a></li>
                    	 <li><a href="<c:url value="/log/TOTAL/stats"/>">Log</a></li>
                    	</ul>
                	</li>									
					<li class="dropdown">
                  		<a href="#" class="dropdown-toggle" data-toggle="dropdown">Web Service<b class="caret"></b></a>
                  		<span class="dropdown-arrow"></span>
                  		<ul class="dropdown-menu">
                  		 <li><a href="/home" class="smooth-scroll">About</a></li>
                  		 <li class="divider"></li>
               			 <li><a href="/home#search" class="smooth-scroll">Search</a></li>
               			 <li><a href="/home#get" class="smooth-scroll">Get</a></li>
               			 <li><a href="/home#traverse" class="smooth-scroll">Traverse</a></li>
               			 <li><a href="/home#graph" class="smooth-scroll">Graph</a></li>
               			 <li><a href="/home#top_pathways" class="smooth-scroll">Top pathways</a></li>
                    	 <li class="divider"></li>
                    	 <li><a href="/home#parameter_values" class="smooth-scroll">Values</a></li>
                    	</ul>
                	</li>
					<li><a href="<c:url value="/datasources"/>">Providers</a></li>
					<li><a href="<c:url value="/downloads"/>">Downloads</a></li>
					<li><a href="#" class="top-scroll">Top</a></li>
                  </ul>
          	  </div> <!-- collapse -->
      </div> <!-- container -->
 	</nav>
</header>
<!-- begin the #content container that will be closed in the footer.jsp-->
<div id="content" class="container">
