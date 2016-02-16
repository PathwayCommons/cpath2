<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html>
<head>
<jsp:include page="head.jsp" />
<script type="text/javascript" src="<spring:url value='/scripts/admin.js'/>"></script>
<title>cPath2::Admin</title>
<meta name="robots" content="noindex,nofollow" />
</head>
<body>
	<jsp:include page="header.jsp" />	
	<div class="row">
	 <h2>Instance Properties</h2>
	 <form id="form_cpath_properties" method="POST" role="form" class="form-inline">
	  <div class="col-sm-6 col-md-3">
	  	<h4>Maintenance Mode</h4>
	  	<p>
    	<c:choose>
    	  <c:when test="${cpath.adminEnabled}">
    	  <input type="checkbox" name="admin" value="on" checked="checked" data-toggle="switch" data-on-color="danger" data-off-color="default"/></c:when>
      	  <c:otherwise><input class="form-control" type="checkbox" name="admin" value="on" data-toggle="switch" data-on-color="danger" data-off-color="default"/></c:otherwise>
      	</c:choose>
      	</p>
	  </div>
	  <div class="col-sm-6 col-md-3">
	    <h4>Debug Mode</h4>
	    <p>
    	<c:choose>
    	  <c:when test="${cpath.debugEnabled}">
    	  <input type="checkbox" name="debug" value="on" checked="checked" data-toggle="switch" data-on-color="warning" data-off-color="default"/></c:when>
      	  <c:otherwise><input type="checkbox" name="debug" value="on" data-toggle="switch" data-on-color="warning" data-off-color="default"/></c:otherwise>
      	</c:choose>
      	</p>
      </div>
	  <div class="col-sm-3">
	    <h4>Log Start</h4>
    	<p><input type="date" size="10" name="logStartDate" value="${cpath.logStart}" alt="yyyy-MM-dd, e.g., 2015-01-01"/></p>
	  </div>
	  <div class="col-sm-3">
	    <h4>Log End</h4>
	    <p><input type="date" size="10" name="logEndDate" value="${cpath.logEnd}" alt="yyyy-MM-dd, e.g., 2015-12-31"/></p>
	  </div>	    
	  <button class="btn btn-warning" type="submit">Apply</button>
	 </form>
	 <br/>
	</div>
	<hr/>
	<div class="row">
	  <h2>Other</h2>
	  <div id="admin_links" class="dropdown">	
	    <a href class="dropdown-toggle" data-toggle="dropdown">Links<b class="caret"></b></a>
        <span class="dropdown-arrow"></span>
		<ul class="dropdown-menu">
			<li><a href="admin/homedir">View Home Directory (except hidden, tmp, cache)</a></li>
			<li><a href="validations">Get BioPAX Validation Reports</a></li>
			<li><a href="tests">Run QUnit Tests</a></li>
		</ul>
	  </div>
	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>
