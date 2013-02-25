<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
   <meta http-equiv="content-type" content="text/html;charset=utf-8" />
	<meta name="author" content='<fmt:message key="cpath2.provider"/>'/>
	<meta name="description" content="cPath2::Demo (version ${project.version})" />
	<meta name="keywords" content="<fmt:message key="cpath2.provider"/>, cPath2, cPathSquared webservice, help, demo, documentation" />
	<title>cPath2::Exception</title>
	<!-- JQuery plugins -->
	<script type="text/javascript" src="<c:url value="/resources/plugins/jquery-1.6.1.js" />"></script>
	<script type="text/javascript" src="<c:url value="/resources/plugins/jquery-ui-1.8.11.custom.min.js" />"></script>
	<link rel="stylesheet" href="<c:url value="/resources/plugins/jquery-ui-1.8.11.custom.css" />" type="text/css"/>
	<link rel="stylesheet" href="<c:url value="/resources/plugins/jquery.ui.override.css" />" type="text/css"/>
	<!-- other -->
	<script type="text/javascript" src="<c:url value="/resources/scripts/json.min.js" />"></script>
	<script type="text/javascript" src="<c:url value="/resources/scripts/help.js" />"></script>
	<link rel="stylesheet" href="<c:url value="/resources/css/andreas08.css" />" type="text/css" media="screen,projection" />
</head>
<body>
<div id="header">
	<h1>Error Page</h1>
</div>
<div id="content">
	<h2><c:out value="${exception}" /></h2>	
	<p>
		<c:out value="${exception.message}" />
	</p>
	<ul>
		<c:forEach var="trace" items="${exception.stackTrace}">
			<li>${trace}</li>
		</c:forEach>
	</ul>		
</div>    
<div id="footer">
</div>
</body>
</html>
