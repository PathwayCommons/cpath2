<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<jsp:include page="head.jsp" />
<title>cPath2::Login</title>
</head>
<body>
	<jsp:include page="header.jsp" />
	<h2>Login</h2>
		<c:if test="${not empty param.error}">
			<h3 style="color: red">
				Invalid username / password. 
			</h3>
			<c:if test="${not empty SPRING_SECURITY_LAST_EXCEPTION.message}">
				<p><c:out value="${SPRING_SECURITY_LAST_EXCEPTION.message}"/></p>
			</c:if>
		</c:if>
		
		<form action="<spring:url value='/login'/>" method="post">
			<dl>
					<dt>User:</dt>
					<dd><input type="text" name="username" id="username"/></dd>
					<dt>Password:</dt>
					<dd><input type="password" name="password" id="password"/></dd>
					<dt><input name="submit" type="submit"/></dt>
			</dl>
		</form>
	<jsp:include page="footer.jsp" />
</body>
</html>
