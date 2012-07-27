<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page language="java" contentType="text/html; charset=UTF-8"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
   <meta http-equiv="content-type" content="text/html;charset=utf-8" />
	<meta name="author" content="Pathway Commons" />
	<meta name="description" content="cPath2 BioPAX Data Validations per Data Source" />
	<meta name="keywords" content="cPath2, BioPAX, Validation" />
	<title>Validation Results</title>
</head>
<body>

<h2>Individual BioPAX File Validations (<c:out value="${identifier}"/>)</h2>

<ul>
  <c:forEach var="result" items="${results}">
	<li><a href='<c:url value="/validation/file/${result.key}.html"/>'>${result.value}</a></li>
  </c:forEach>
</ul>

</body>
</html>
