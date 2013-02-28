<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="content-type" content="text/html;charset=utf-8" />
<meta name="author" content='<fmt:message key="cpath2.provider"/>' />
<meta name="description" content="cPath2 Error Page (version ${project.version})" />
<meta name="keywords"content="<fmt:message key="cpath2.provider"/>, cPath2, cPathSquared, service, error" />
<title>cPath2::Error</title>
<link rel="stylesheet" href="resources/css/cpath2.css" type="text/css" media="screen" />
</head>
<body>
	<div id="header">
		<h1>
			<fmt:message key="cpath2.provider" /> version 
			<fmt:message key="cpath2.data.version" /><br/> 
			- Error Page
		</h1>
	</div>
	<div id="content">
		<h2>
			<c:out value="${exception}" />
		</h2>
		<p>
			<c:out value="${exception.message}" />
		</p>
		<ul>
			<c:forEach var="trace" items="${exception.stackTrace}">
				<li>${trace}</li>
			</c:forEach>
		</ul>
	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>
