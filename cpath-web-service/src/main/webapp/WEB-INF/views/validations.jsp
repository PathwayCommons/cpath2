<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page language="java" contentType="text/html; charset=UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8" />
<meta name="author" content="${cpath.name}" />
<meta name="description"
	content="cPath2 BioPAX Data Validations per Data Source" />
<meta name="keywords" content="cPath2, BioPAX, Validation" />
<link media="screen" href="<c:url value="/resources/css/cpath2.css"/>"
	 rel="stylesheet" />
<title>cPath2::Validation Results</title>
</head>
<body>
	<jsp:include page="header.jsp" />
	<div id="content">
		<h2>BioPAX Validation Results</h2>

		<dl id="#pathway_datasources">
		<c:forEach var="datasource" items="${providers}">
			<dt> 
<%-- 				change img src to use base64 bytes from ${datasource.icon} --%>
				<img src='<c:url value="/metadata/logo/${datasource.identifier}"/>' /> BioPAX Validation Results for: <c:out value="${datasource.identifier}" />
			</dt>
			<dd><ul>
				<c:forEach var="file" items="${datasource.files}">
					<li><a href='<c:url value="/metadata/validations/${datasource.identifier}/${file.key}.html"/>'>${file.value}</a></li>
				</c:forEach>
				</ul>
			</dd>
		</c:forEach>
		</dl>
	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>
