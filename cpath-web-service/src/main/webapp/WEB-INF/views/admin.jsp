<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<!DOCTYPE html>
<html>
<head>
<jsp:include page="head.jsp" />
<script type="text/javascript" src='<c:url value="/resources/scripts/admin.js"/>'></script>
<title>cPath2::Admin</title>
</head>
<body data-spy="scroll" data-target=".navbar">
	<jsp:include page="header.jsp" />

	<h2>Instance Properties</h2>
	
	<div class="row">
	<div class="col-xs-12">
	<form id="form_cpath_properties" method="POST" class="form-inline" role="form">
	  <div class="checkbox">
	  <label>
    	<c:choose>
    	  <c:when test="${cpath.adminEnabled}">
    	  <input type="checkbox" name="admin" value="on" checked="checked" data-toggle="switch" data-on-color="danger" data-off-color="default"/></c:when>
      	  <c:otherwise><input type="checkbox" name="admin" value="on" data-toggle="switch" data-on-color="danger" data-off-color="default"/></c:otherwise>
      	</c:choose>
      	Maintenance Mode 
	  </label>
	  </div>	  
	  <div class="checkbox">
	  <label>
    	<c:choose>
    	  <c:when test="${cpath.proxyModelEnabled}">
    	  <input type="checkbox" name="proxy" value="on" checked="checked" data-toggle="switch" data-on-color="primary" data-off-color="default"/></c:when>
      	  <c:otherwise><input type="checkbox" name="proxy" value="on" data-toggle="switch" data-on-color="primary" data-off-color="default"/></c:otherwise>
      	</c:choose>
      	In-memory BioPAX Model 
	  </label>
	  </div>	  
	  <div class="checkbox">
	  <label>
    	<c:choose>
    	  <c:when test="${cpath.debugEnabled}">
    	  <input type="checkbox" name="debug" value="on" checked="checked" data-toggle="switch" data-on-color="warning" data-off-color="default"/></c:when>
      	  <c:otherwise><input type="checkbox" name="debug" value="on" data-toggle="switch" data-on-color="warning" data-off-color="default"/></c:otherwise>
      	</c:choose>
      	Debug Mode 
	  </label>
	  </div>  	
	  
	 <button class="btn btn-warning" type="submit">Refresh </button>
	</form>
	</div>
	</div>
	
	<hr/>
	
	<h2>Other</h2>
	<div id="admin_links" class="row dropdown">	
		<div class="col-xs-12">
	    <a href="" class="dropdown-toggle" data-toggle="dropdown">Links<b class="caret"></b></a>
        <span class="dropdown-arrow"></span>
		<ul class="dropdown-menu">
			<li><a href="admin/homedir.html">View Home Directory (except hidden,tmp,cache)</a></li>
			<li><a href="metadata/validations">Get BioPAX Validation Reports</a></li>
			<li><a href="tests.html">Run QUnit Tests</a></li>
		</ul>
		</div>
	</div>
	
	<jsp:include page="footer.jsp" />

</body>
</html>
