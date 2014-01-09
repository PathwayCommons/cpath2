<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8" />
<meta name="author" content="${cpath.name}" />
<title>cPath2::Access Denied</title>
<link media="screen" href="<c:url value="/resources/css/cpath2.css"/>"  rel="stylesheet" />
</head>
<body>
	<jsp:include page="header.jsp" />
	<div id="content">
		<h2>Access Denied</h2>
	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>