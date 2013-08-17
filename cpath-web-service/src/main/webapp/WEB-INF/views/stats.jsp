<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page language="java" contentType="text/html; charset=UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<!DOCTYPE html>

<html>
<head>
<meta charset="utf-8" />
<meta name="author" content="${cpath.name}" />
<meta name="description" content="cPath2 simple access log summary" />
<meta name="keywords" content="cPath2, BioPAX, Validation" />
<link media="screen" href="<c:url value="/resources/css/cpath2.css"/>"  rel="stylesheet" />
<title>Stats</title>
</head>
<body>
	<jsp:include page="header.jsp" />
	<div id="content">
		<h2>Stats</h2>
		<h3>A simple summary of user requests extracted from the log files.</h3>
		<form method="post">
			<input type="submit" alt="Update now" name="Update" value="Update"/>
		</form>
		<br/>
		<dl title="From log files:">
			<c:forEach var="entry" items="${counts}">
				<dt>${entry.key}</dt><dd>${entry.value}</dd>
			</c:forEach>
		</dl>
	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>
