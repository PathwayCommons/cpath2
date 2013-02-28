<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
   <meta http-equiv="content-type" content="text/html;charset=utf-8" />
	<meta name="author" content="Pathway Commons" />
	<meta name="description" content="cPath2 help" />
	<meta name="keywords" content="cPath2, webservice, help, documentation" />
	<title>cPath2::QUnit Tests</title>
	<script type="text/javascript" src="<c:url value="/resources/scripts/jquery-1.5.1.min.js"/>"></script>
	<script type="text/javascript" src="<c:url value="/resources/scripts/json.min.js"/>"></script>
	<link media="screen" href="<c:url value="/resources/css/cpath2.css"/>" type="text/css" rel="stylesheet" />
  	<link rel="stylesheet" href="<c:url value="/resources/css/qunit.css"/>" type="text/css" media="screen" />
	<script type="text/javascript" src="<c:url value="/resources/scripts/qunit.js"/>"></script>
	<script type="text/javascript" src="<c:url value="/resources/scripts/tests.js"/>"></script>
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
