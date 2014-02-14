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
<link href="<c:url value="/resources/css/bootstrap.min.css"/>" rel="stylesheet" />
<link href="<c:url value="/resources/css/bootstrap-select.min.css"/>" rel="stylesheet" />
<link href="<c:url value="/resources/css/cpath2.css"/>" rel="stylesheet" media="screen"/>
<link href="<c:url value="/resources/jquery-fileupload/css/jquery.fileupload-ui.css"/>" rel='stylesheet' media='screen'>
<script	type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
<script type="text/javascript" src="https://www.google.com/jsapi"></script>
<script type="text/javascript" src="<c:url value="/resources/scripts/json.min.js"/>"></script>
<script type="text/javascript" src="<c:url value="/resources/scripts/bootstrap.min.js"/>"></script>
<script type="text/javascript" src="<c:url value="/resources/scripts/bootstrap-select.min.js"/>"></script>
<script type="text/javascript" src="<c:url value="/resources/scripts/admin.js"/>"></script>
<script type="text/javascript"  src="<c:url value="/resources/jquery-fileupload/js/jquery.iframe-transport.js"/>"></script>
<script type="text/javascript"  src="<c:url value="/resources/jquery-fileupload/js/jquery.fileupload.js"/>"></script>
<script type="text/javascript"  src="<c:url value="/resources/jquery-fileupload/js/jquery.fileupload-ui.js"/>"></script>

<title>cPath2::Admin</title>
</head>
<body>
	<jsp:include page="header.jsp" />
	<div id="content">
	<h2>Manage the cPath2 System</h2>
	
	<section id="other">
	<form method="POST">
		<input type="hidden" name="toggle" value="toggle"/>
		<input id="bt_toggle" type="submit" value="Toggle Maintenance Mode" />
	</form>
	<br/>
	<div id="nav">
		<ol>
			<li><a href="admin/homedir.html">Home Directory</a>(hidden, tmp, cache files are not listed)</li>
			<li><a href="tests.html">Tests (QUnit)</a></li>
		</ol>
	</div>	
	</section>
	
	<section id="metadata">
		<h2>Metadata</h2>
		<div class="row-fluid">
			<!-- to be filled by Javascript -->
			<dl id="datasources" class="datasources"></dl>
		</div>
		<br/>
		<!-- TODO add buttons -->
	</section>

	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>
