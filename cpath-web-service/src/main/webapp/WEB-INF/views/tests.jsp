<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html>
<head>
   <meta charset="utf-8" />
	<meta name="author" content="${cpath.name}" />
	<meta name="description" content="cPath2 help" />
	<meta name="keywords" content="cPath2, webservice, help, documentation" />
	<title>cPath2::QUnit Tests</title>
	<script  src="<c:url value="/resources/scripts/jquery-1.9.1.min.js"/>"></script>
	<script  src="<c:url value="/resources/scripts/json.min.js"/>"></script>
	<link media="screen" href="<c:url value="/resources/css/cpath2.css"/>"  rel="stylesheet" />
  	<link rel="stylesheet" href="<c:url value="/resources/css/qunit-1.10.0.css"/>"  media="screen" />
	<script  src="<c:url value="/resources/scripts/qunit-1.10.0.js"/>"></script>
	<script  src="<c:url value="/resources/scripts/tests.js"/>"></script>
</head>
<body>
<jsp:include page="header.jsp" />
<h1>QUnit Tests</h1>
<!-- basic html - just to show the test results -->
 <h2 id="qunit-banner"></h2>
 <div id="qunit-testrunner-toolbar"></div>
 <h2 id="qunit-userAgent"></h2>
 <ol id="qunit-tests"></ol>
 <div id="qunit-fixture">test markup, will be hidden</div>
<jsp:include page="footer.jsp" />
</body>
</html>
