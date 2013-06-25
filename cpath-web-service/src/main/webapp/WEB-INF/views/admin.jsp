<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<!DOCTYPE html>

<html>
<head>
<meta charset="utf-8" />
<meta name="author" content="${cpath.name}" />
<meta name="description"
	content="cPath2 Web Admin (version ${project.version})" />
<meta name="keywords" content="${cpath.name}, cPath2, cPathSquared, admin, configuration" />
<link media="screen" href="<c:url value="/resources/css/cpath2.css"/>"  rel="stylesheet" />
<script  src="<c:url value="/resources/scripts/json.min.js"/>"></script>
<!-- <script src="//ajax.googleapis.com/ajax/libs/jquery/1.10.1/jquery.min.js"></script> -->
<!-- <script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.10.3/jquery-ui.min.js"></script> -->
<!-- <link rel="stylesheet" href="http://code.jquery.com/ui/1.10.3/themes/smoothness/jquery-ui.css" /> -->

<title>cPath2::Admin</title>
</head>
<body>
	<jsp:include page="header.jsp" />
	<div id="content">
	<h2>Manage the cPath2 System</h2>
	
	<form method="POST">
		<input type="hidden" name="toggle" value="toggle"/>
<%-- 		<label for="bt_toggle">Maintenance Mode is <c:choose><c:when test="${cpath.adminEnabled}">Enabled</c:when><c:otherwise>Disabled</c:otherwise></c:choose></label>	 --%>
		<input id="bt_toggle" type="submit" value="Toggle Maintenance Mode" />
	</form>
	<br/>
	<div id="nav">
		<ol>
			<li><a href="admin/homedir.html">View Home Dir. Content</a></li>
			<li><a href="admin/data.html">View Data Dir. Content</a></li>
			<li><a href="admin/tmp.html">View Tmp Dir. Content</a></li>
		</ol>
	</div>	

	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>
