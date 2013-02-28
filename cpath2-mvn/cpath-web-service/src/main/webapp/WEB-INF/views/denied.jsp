<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="content-type" content="text/html;charset=utf-8" />
<meta name="author" content='<fmt:message key="provider.name"/>' />
<title>cPath2::Access Denied</title>
<link media="screen" href="<c:url value="/resources/css/cpath2.css"/>" type="text/css" rel="stylesheet" />
</head>
<body>
	<jsp:include page="header.jsp" />
	<div id="content">
		<h2>Access Denied</h2>
	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>