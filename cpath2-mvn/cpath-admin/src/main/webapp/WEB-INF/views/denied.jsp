<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<meta http-equiv="content-type" content="text/html;charset=utf-8" />
	<meta name="author" content='<fmt:message key="cpath2.provider"/>'/>
	<title>Access Denied</title>
	<link rel="icon" type="image/gif" href="/resources/images/pc2-144x144.png" />
	<link media="screen" href="/resources/css/basic.css" type="text/css" rel="stylesheet"/>
</head>
<body>
<div id="header">
	<h1>Access Denied</h1>
</div>
<div id="content">
<p>Sorry, Access Denied</p>		
</div>    
<div id="footer">
<p>
<a href="<c:url value='/' />">Return to Home Page</a> or 
<a href="<c:url value='/j_spring_security_logout' />">Logout</a>
</p>
</div>
</body>
</html>