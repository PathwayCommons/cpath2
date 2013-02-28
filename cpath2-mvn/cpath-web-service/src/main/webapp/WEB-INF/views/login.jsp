<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="content-type" content="text/html;charset=utf-8" />
<meta name="author" content='<fmt:message key="cpath2.provider"/>' />
<title>cPath2::Login</title>
<link media="screen" href="resources/css/cpath2.css" type="text/css" rel="stylesheet" />
</head>
<body>
	<div id="header">
		<h1>
			<fmt:message key="cpath2.provider" /> version 
			<fmt:message key="cpath2.data.version" /><br/>
			- Login
		</h1>
	</div>
	<div id="content">
		<c:if test="${!empty param.login_error}">
			<h3 style="color: red">
				<spring:message code="login.invalid" />
			</h3>
		</c:if>
		<form action="<c:url value='/j_spring_security_check'/>" method="post">
			<table>
				<tr>
					<td>User name:</td>
					<td><input type="text" name="j_username" /></td>
				</tr>
				<tr>
					<td>Password:</td>
					<td><input type="password" name="j_password" /></td>
				</tr>
				<tr>
					<td colspan='2'><input name="submit" type="submit"
						value="Login" /></td>
				</tr>
			</table>
		</form>
	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>
