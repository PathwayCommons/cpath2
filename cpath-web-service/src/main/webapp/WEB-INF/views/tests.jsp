<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html>
<head>
	<jsp:include page="head.jsp"/>
	<title>cPath2::QUnit Tests</title>
  	<link rel="stylesheet" media="screen" href="<c:url value="/resources/css/qunit.css"/>" />	
</head>
<body>

<jsp:include page="header.jsp" />

<h1>cPath2 Tests</h1>
<!-- basic html - just to show the test results -->
 <h2 id="qunit-banner"></h2>
 <div id="qunit-testrunner-toolbar"></div>
 <h2 id="qunit-userAgent"></h2>
 <ol id="qunit-tests"></ol>
 <div id="qunit-fixture">test markup, will be hidden</div>

<jsp:include page="footer.jsp" />

<script src="<c:url value="/resources/scripts/qunit.js"/>"></script>
<script src="<c:url value="/resources/scripts/tests.js"/>"></script>
</body>
</html>
