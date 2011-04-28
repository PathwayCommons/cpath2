<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
   <meta http-equiv="content-type" content="text/html;charset=utf-8" />
	<meta name="author" content="cPath2" />
	<meta name="description" content="cPath2 help" />
	<meta name="keywords" content="cPath2, webservice, help, documentation" />
	<title>cPath2 Help Page</title>
</head>
<body>

<c:if test="${title != null}">
<h2>Information for ${title}:</h2>
</c:if>
<!-- "list" is the (MVC) model attribute (List<String>) -->
<c:if test="${list != null}">
 <ul style="list-style: inside;">
  <c:forEach var="itm" items="${list}">
  	<c:url var="itmUrl" value="${itm}"/>
	<li><a href='<c:out value="${itm}"/>'>${itm}</a></li>
  </c:forEach>
 </ul>
</c:if>

</body>
</html>
