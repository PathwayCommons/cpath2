<%@page language="java" contentType="text/html; charset=UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html>
<head>
<jsp:include page="head.jsp" />
<title>cPath2::Data Dir</title>
<meta name="robots" content="noindex,nofollow" />
</head>
<spring:url value="/datadir" var="dataUrl" />
<body>
	<jsp:include page="header.jsp" />
		<h2>Internal Data Directory</h2>
		<h3>Files:</h3>
		<dl>
			<c:forEach var="f" items="${files}">
				<dt>
				<a rel="nofollow" href="${dataUrl}/${f.key}">${f.key}</a>
				</dt><dd>(${f.value})</dd>
			</c:forEach>
		</dl>
	<jsp:include page="footer.jsp" />
</body>
</html>
