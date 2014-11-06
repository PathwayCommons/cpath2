<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page isErrorPage="true" language="java" contentType="text/html; charset=UTF-8"%>

<!DOCTYPE html>
<html>
<head>
<jsp:include page="head.jsp" />
<title>cPath2::Error Page</title>
</head>
<body>
	<jsp:include page="header.jsp" />
	
		<h2>There was an error</h2>
		<h3>Status Code: <c:out value="${requestScope['javax.servlet.error.status_code']}" /></h3>
		<p><c:out value="${requestScope['javax.servlet.error.message']}" /></p>			
		<h3>Details</h3>
        <ul>
            <li>Action: <c:out value="${requestScope['javax.servlet.forward.request_uri']}" />
            <li>Query String: <c:out value="${requestScope['javax.servlet.forward.query_string']}" />
            <li>Exception: <c:out value="${requestScope['javax.servlet.error.exception']}" />
        </ul>
        		
	<jsp:include page="footer.jsp" />
</body>
</html>
