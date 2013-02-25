<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
   <meta http-equiv="content-type" content="text/html;charset=utf-8" />
	<meta name="author" content='<fmt:message key="cpath2.provider"/>'/>
	<meta name="description" content="cPath2 Web Admin (version ${project.version})" />
	<meta name="keywords" content="<fmt:message key="cpath2.provider"/>, cPath2, cPathSquared, admin, configuration" />
	<title>cPath2 Admin</title>
	<link rel="icon" type="image/gif" href="/resources/images/pc2-144x144.png" />
	<link rel="shortcut icon" href="/resources/images/pc2-144x144.png" />
	<link media="screen" href="/resources/css/basic.css" type="text/css" rel="stylesheet"/>
	<script type="text/javascript" src="<c:url value="/resources/scripts/json.min.js" />"></script>
	
</head>
<body>
	<div id="header">
		<h1>cPath2 Web Admin ${project.version}</h1>
		<h2><fmt:message key="cpath2.provider"/></h2>
	</div>
  
    <div id="content">
	  <!-- Name and description of this cpath2 instance is taken from the $CPATH2_HOME/cpath.properties -->
      
      <p><fmt:message key="cpath2.description"/></p>

	</div>

	<div id="footer">	
	</div>
	
</body>
</html>
