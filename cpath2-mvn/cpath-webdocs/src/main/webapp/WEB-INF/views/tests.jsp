<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
   <meta http-equiv="content-type" content="text/html;charset=utf-8" />
	<meta name="author" content="Pathway Commons" />
	<meta name="description" content="cPath2 help" />
	<meta name="keywords" content="cPath2, webservice, help, documentation" />
	<title>cPath2 Tests</title>
	<script type="text/javascript" src="<c:url value="/resources/jquery-1.5.1.min.js" />"></script>
	<script type="text/javascript" src="<c:url value="/resources/json.min.js" />"></script>
  	<link rel="stylesheet" href="<c:url value="/resources/qunit.css" />" type="text/css" media="screen" />
	<script type="text/javascript" src="<c:url value="/resources/qunit.js" />"></script>
	<script type="text/javascript" src="<c:url value="/resources/tests.js" />"></script>
</head>
<body>

<h2><fmt:message key="cpath2.welcome"/> <fmt:message key="cpath2.provider"/> QUnit Tests!</h2>

<!-- basic html - just to show the test results -->
 <h2 id="qunit-banner"></h2>
 <div id="qunit-testrunner-toolbar"></div>
 <h2 id="qunit-userAgent"></h2>
 <ol id="qunit-tests"></ol>
 <div id="qunit-fixture">test markup, will be hidden</div>

</body>
</html>
