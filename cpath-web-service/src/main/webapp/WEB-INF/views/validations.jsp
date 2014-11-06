<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page language="java" contentType="text/html; charset=UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<!DOCTYPE html>
<html>
<head>
<jsp:include page="head.jsp"/>
<title>cPath2::Validation Results</title>
<meta name="robots" content="noindex,nofollow" />
</head>
<body>
	<jsp:include page="header.jsp" />
	<div class="row">
		<h2>BioPAX Validation Results</h2>

		<dl id="#pathway_datasources">
		<c:forEach var="datasource" items="${providers}">
			<dt> 
				<img class="datasource-logo" src='<c:url value="/metadata/logo/${datasource.identifier}"/>' /> 
				BioPAX Validation Results for: <c:out value="${datasource.identifier}" />
			</dt>
			<dd><ul>
				<c:forEach var="file" items="${datasource.files}">
					<li><a target="_blank" 
						href='<c:url value="/datadir/${datasource.identifier}/${file.key}.validation.xml.gz"/>'>${file.key} validation XML</a></li>
				</c:forEach>
				</ul>
			</dd>
		</c:forEach>
		</dl>
	</div>		
	<jsp:include page="footer.jsp" />
</body>
</html>
