<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>   
<%@ page language="java" contentType="text/html; charset=UTF-8"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
   <meta http-equiv="content-type" content="text/html;charset=utf-8" />
	<meta name="author" content="BioPAX" />
	<meta name="description" content="BioPAX Validator" />
	<meta name="keywords" content="BioPAX, Validation, Validator, Rule, OWL, Exchange" />
	<link rel="stylesheet" type="text/css" href="styles/style.css" media="screen" />
	<link rel="shortcut icon" href="images/favicon.ico" />
	<script type="text/javascript" src="scripts/rel.js"></script>
	<title>Login Page</title>
</head>
<body>

<h2>Login</h2>
<c:if test="${!empty param.login_error}">
	<h3 style="color: red"> <spring:message code="login.invalid"/></h3>
</c:if>
<form action="<c:url value='/j_spring_security_check'/>" method="post">
	<table>
		<tr>
			<td>Username:</td>
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

</body>
</html>
