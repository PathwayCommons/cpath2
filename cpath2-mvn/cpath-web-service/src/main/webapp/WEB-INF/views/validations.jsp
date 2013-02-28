<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="content-type" content="text/html;charset=utf-8" />
<meta name="author" content="Pathway Commons" />
<meta name="description"
	content="cPath2 BioPAX Data Validations per Data Source" />
<meta name="keywords" content="cPath2, BioPAX, Validation" />
<link media="screen" href="resources/css/cpath2.css" type="text/css" rel="stylesheet" />
<title>cPath2::Validation Results</title>
</head>
<body>
	<div id="header">
		<h1>
			<fmt:message key="cpath2.provider" /> version 
			<fmt:message key="cpath2.data.version" /><br/>
			- Input Data Validation Results
		</h1>
	</div>
	<div id="content">
		<div>
			<img src='<c:url value="/logo/${identifier}"/>' /><br />
		</div>
		<h2>
			BioPAX Validation Results for:
			<c:out value="${identifier}" />
		</h2>

		<ul>
			<c:forEach var="result" items="${results}">
				<li><a href='<c:url value="/validation/${result.key}.html"/>'>${result.value}</a></li>
			</c:forEach>
		</ul>
	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>
