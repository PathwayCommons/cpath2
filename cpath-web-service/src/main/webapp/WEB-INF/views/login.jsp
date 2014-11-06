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
		<c:if test="${not empty param.login_error}">
			<h3 style="color: red">
				Your login attempt was not successful, try again. 
				Reason: <c:out value="${SPRING_SECURITY_LAST_EXCEPTION.message}"/><br />
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
	<jsp:include page="footer.jsp" />
</body>
</html>
