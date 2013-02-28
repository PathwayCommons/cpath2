<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="content-type" content="text/html;charset=utf-8" />
<meta name="author" content='<fmt:message key="provider.name"/>' />
<meta name="description"
	content="cPath2 Web Admin (version ${project.version})" />
<meta name="keywords"
	content="<fmt:message key="provider.name"/>, cPath2, cPathSquared, admin, configuration" />
<link media="screen" href="<c:url value="/resources/css/cpath2.css"/>" type="text/css" rel="stylesheet" />
<script type="text/javascript" src="<c:url value="/resources/scripts/json.min.js"/>"></script>
<title>cPath2::Admin</title>
</head>
<body>
	<jsp:include page="header.jsp" />
	<div id="content">
	<h2>Manage the cPath2 System</h2>
		
	<div id="nav">
		<ol>
			<li><a href="admin/homedir.html">View Home Dir</a></li>
			<li><a href="admin/data.html">View Data Dir</a></li>
			<li><a href="admin/tmp.html">View Tmp Dir</a></li>
		</ol>
	</div>	

	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>
