<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="content-type" content="text/html;charset=utf-8" />
<meta name="author" content='<fmt:message key="provider.name"/>' />
<meta name="description" content="cPath2 Error Page (version ${project.version})" />
<meta name="keywords"content="<fmt:message key="provider.name"/>, cPath2, cPathSquared, service, error" />
<title>cPath2::Error</title>
<link rel="stylesheet" href="<c:url value="/resources/css/cpath2.css"/>" type="text/css" media="screen" />
</head>
<body>
	<jsp:include page="header.jsp" />
	<div id="content">
		<h2>Internal Error</h2>
		<h3>
			<c:out value="${exception}" />
		</h3>
		<h4>
			<c:out value="${exception.message}" />
		</h4>
		<ul>
			<c:forEach var="trace" items="${exception.stackTrace}">
				<li>${trace}</li>
			</c:forEach>
		</ul>
	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>
