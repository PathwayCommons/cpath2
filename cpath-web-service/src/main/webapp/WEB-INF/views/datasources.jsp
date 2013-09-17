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
	<!-- additional parameter details -->
	<h2>Pathway and Interaction Data Sources</h2>

	<p>Imported pathway data with corresponding logo and names.
		These names are recommended to use for the 'datasource' filter
		parameter (see about <a href="#search">'/search'</a> command).
		For example, 'NCI_Nature', 'reactome' can be successfully
		used (case insensitive) there. Using URIs instead of names is also
		possible. If there are several items having a name in commom,
		e.g. 'Reactome', that means we imported the provider's data
		from different locations or archives. Not all data are ready to be
		imported into the cPath2 system right away; so one has to unpack,
		add/remove/edit, pack original BioPAX or PSI-MI files. Data location
		URLs are shown for information and copyright purpose only.
		One can find all BioPAX Provenance objects in the system by using
		<a href="search?q=*&type=provenance">search for all Provenance objects</a>.
	</p>

	<div id="data_sources" class="parameters">
		<!-- items are to be added here by a javascript -->
		<dl id="pathway_datasources" class="datasources"></dl>
	</div>
	<br/>

	<h2>Warehouse Data Sources</h2>

	<div class="parameters">
		<!-- items are to be added here by a javascript -->
		<p>In order to consistently normalize and merge all pathways and interactions,
			we created a BioPAX warehouse using the following data:</p>
		<dl id="warehouse_datasources" class="datasources"></dl>
	</div>
	<br/>
</div>
<jsp:include page="footer.jsp"/>

</body>
</html>
