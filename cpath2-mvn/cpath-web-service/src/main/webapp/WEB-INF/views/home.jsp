<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
   <meta http-equiv="content-type" content="text/html;charset=utf-8" />
	<meta name="author" content="cPath2" />
	<meta name="description" content="cPath2 info" />
	<meta name="keywords" content="cPath2, PathwayCommons, webservice, documentation" />
	<title>cPath2 Welcome Page</title>
</head>
<body>

<h2>Welcome to cPath2 (cPath Squared)!</h2>
<div style="width: 90%">
cPath2 is...
</div>

<ul>
<li><a href='<c:out value="help/commands"/>'>commands</a></li>
<li><a href='<c:out value="help/datasources"/>'>data sources</a></li>
<li><a href='<c:out value="help/types"/>'>search types</a></li>
<li>converting to other <a href='<c:out value="help/formats"/>'>formats</a></li>
</ul>

</body>
</html>
