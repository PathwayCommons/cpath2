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
	<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
	<script src="<c:url value="/resources/scripts/json.min.js"/>"></script>
	<script src="<c:url value="/resources/scripts/stats.js"/>"></script>
	<title>cPath2: Stats</title>
</head>
<body>
	<jsp:include page="header.jsp" />
	<div id="content">
		<h2>Service and Data Usage</h2>
		<h3>Simple counts of user requests using log files.</h3>
		<form method="post" action="javascript:getAccessCounts();">
			<input type="submit" alt="Update now" name="Update" value="Update"/>
		</form>
		<br/>
		<dl id="accessCounts" title="From log files:">
		</dl>
	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>
