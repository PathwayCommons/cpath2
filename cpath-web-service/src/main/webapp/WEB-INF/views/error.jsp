<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page isErrorPage="true" language="java" contentType="text/html; charset=UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8" />
<meta name="author" content="${cpath.name}" />
<meta name="description"
	content="cPath2 Error Page (version ${project.version})" />
<meta name="keywords"
	content="${cpath.name}, cPath2, cPathSquared, service, error" />
<title>cPath2::Error Page</title>
<link rel="stylesheet" href="<c:url value="/resources/css/cpath2.css"/>"
	media="screen" />
</head>
<body>
	<jsp:include page="header.jsp" />
	<div id="content">	
		<h1>There was an error</h1>
		<h2>Status Code: <c:out value="${requestScope['javax.servlet.error.status_code']}" /></h2>
		<p><c:out value="${requestScope['javax.servlet.error.message']}" /></p>			
		<h3>Details</h3>
        <ul>
            <li>Action: <c:out value="${requestScope['javax.servlet.forward.request_uri']}" />
            <li>Query String: <c:out value="${requestScope['javax.servlet.forward.query_string']}" />
            <li>Exception: <c:out value="${requestScope['javax.servlet.error.exception']}" />
<%--             <li>Message: <c:out value="${requestScope['javax.servlet.error.message']}" /> --%>
<%--             <li>User agent: <c:out value="${header['user-agent']}" /> --%>
        </ul>		
	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>
