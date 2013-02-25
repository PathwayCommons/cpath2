<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>   
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<meta http-equiv="content-type" content="text/html;charset=utf-8" />
	<meta name="author" content='<fmt:message key="cpath2.provider"/>'/>
	<title>Login</title>
	<link rel="icon" type="image/gif" href="/resources/images/pc2-144x144.png" />
	<link media="screen" href="/resources/css/basic.css" type="text/css" rel="stylesheet"/>
</head>
<body>
<div id="header">
	<h1>Login</h1>
</div>
<div id="content">
<c:if test="${!empty param.login_error}">
	<h3 style="color: red"> <spring:message code="login.invalid"/></h3>
</c:if>
<form action="<c:url value='/j_spring_security_check'/>" method="post">
	<table>
		<tr>
			<td>User name:</td>
			<td><input type="text" name="j_username"/></td>
		</tr>
		<tr>
			<td>Password:</td>
			<td><input type="password" name="j_password"/></td>
		</tr>
		<tr>
			<td colspan='2'><input name="submit" type="submit" value="Login"/></td>
		</tr>
	</table>
</form>
</div>    
<div id="footer">
<p>
<a href="<c:url value='/' />">Return to Home Page</a> or 
<a href="<c:url value='/j_spring_security_logout' />">Logout</a>
</p>
</div>
</body>
</html>
