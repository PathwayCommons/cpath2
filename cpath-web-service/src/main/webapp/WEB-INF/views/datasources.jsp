<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!DOCTYPE html>
<html>
<head>
	<meta charset="utf-8"/>
	<meta name="author" content="${cpath.name}"/>
	<meta name="description" content="cPath2 Data Providers"/>
	<meta name="keywords" content="${cpath.name}, cPath2, cPathSquared, webservice, datasources, data providers"/>
	<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.10.1/jquery.min.js"></script>
	<script src="<c:url value="/resources/scripts/json.min.js"/>"></script>
	<script src="<c:url value="/resources/scripts/datasources.js"/>"></script>
	<link rel="stylesheet" href="<c:url value="/resources/css/cpath2.css"/>" media="screen"/>
	<title>cPath2::Datasources</title>
</head>
<body>

<jsp:include page="header.jsp"/>

<div id="content">

	<h2>Pathway and Interaction Data</h2>
	<div id="data_sources" class="parameters">
		<p>The summary of sources normalized and merged 
		into one big BioPAX model:</p>
		<!-- items are to be added here by a javascript -->
		<dl id="pathway_datasources" class="datasources"></dl>
	</div>
	<br/>

	<h2>Warehouse Data</h2>
	<div class="parameters">
		<p>In order to integrate curated pathways and interactions 
		from various sources, the following files were first 
		converted to a BioPAX model of canonical biomolecules 
		and to unambiguous id-mapping table:</p>
		<!-- items are to be added here by a javascript -->
		<dl id="warehouse_datasources" class="datasources"></dl>
	</div>
	<br/>
	
	<p>To find all the BioPAX Provenance objects in the system 
	use <a href="search?q=*&type=provenance">this query</a>; hits's names 
	or URIs can be 'datasource' filter values in other cpath2 queries. 
	</p>
	
</div>
<jsp:include page="footer.jsp"/>

</body>
</html>
