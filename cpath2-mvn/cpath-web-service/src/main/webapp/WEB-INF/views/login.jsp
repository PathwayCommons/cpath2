<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="content-type" content="text/html;charset=utf-8" />
<meta name="author" content='<fmt:message key="provider.name"/>' />
<title>cPath2::Login</title>
<link media="screen" href="<c:url value="/resources/css/cpath2.css"/>" type="text/css" rel="stylesheet" />
</head>
<body>
	<jsp:include page="header.jsp" />
	<div id="content">
	<h2>Login</h2>
		<c:if test="${!empty param.login_error}">
			<h3 style="color: red">
				<spring:message code="login.invalid" />
			</h3>
		</c:if>
		<form action="<c:url value='/j_spring_security_check'/>" method="post">
			<dl>
					<dt>User name:</dt>
					<dd><input type="text" name="j_username" /></dd>
					<dt>Password:</dt>
					<dd><input type="password" name="j_password" /></dd>
					<dt><input name="submit" type="submit" value="Login" /></dt>
			</dl>
		</form>
	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>
