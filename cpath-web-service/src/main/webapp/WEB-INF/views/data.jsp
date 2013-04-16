<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page language="java" contentType="text/html; charset=UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<!DOCTYPE html>

<html>
<head>
<meta charset="utf-8" />
<meta name="author" content="${cpath.name}" />
<meta name="description" content="cPath2 Input Data" />
<meta name="keywords" content="cpath2, admin, data, files" />
<link media="screen" href="<c:url value="/resources/css/cpath2.css"/>"  rel="stylesheet" />
<title>cPath2::Data</title>
</head>
<body>

	<jsp:include page="header.jsp" />
	<div id="content">
		<h2>The Data Directory Content</h2>
		<h3>Description:</h3>
			<p>
				Data uploaded to the CPath2 server get organized as follows:
				<br /><br /> <code>&lt;IDENTIFIER&gt;.&lt;VERSION&gt;.&lt;[EXT]&gt;</code><br /><br />
				where:
				<em>IDENTIFIER</em> - Metadata Identifier;
				<em>VERSION</em> - Metadata Version;
				<em>EXT</em> (optional).
			</p>
		<h3>FILES:</h3>
		<dl>
			<c:forEach var="f" items="${files}">
				<dt>
				<a href='<c:url value="/admin/homedir/data/${f.key}"/>'>${f.key}</a>
				</dt><dd>(${f.value})</dd>
			</c:forEach>
		</dl>
	</div>
	<jsp:include page="footer.jsp" />
</body>
</html>
