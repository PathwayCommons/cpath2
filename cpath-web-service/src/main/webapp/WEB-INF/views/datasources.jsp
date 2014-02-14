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
	<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
	<script src="<c:url value="/resources/scripts/json.min.js"/>"></script>
	<script src="<c:url value="/resources/scripts/datasources.js"/>"></script>
	<link rel="stylesheet" href="<c:url value="/resources/css/cpath2.css"/>" media="screen"/>
	<title>cPath2::Datasources</title>
</head>
<body>

<jsp:include page="header.jsp"/>

<div id="content">

	<h2>Pathway and Interaction Data</h2>
	
	<p> The following curated biological pathway and interaction models 
		were converted to BioPAX (if required), normalized, and used for 
		creating a single larger BioPAX model (database), in which most of  
		original interactions, states and annotations were preserved 
		(not merged unless there're apparent duplicates), while reference molecules, 
		terms and features were substituted <em>where possible</em> with corresponding 
		standard BioPAX utility class objects (see about the warehouse data sources below).
	</p>
	
	<div id="data_sources" class="parameters">
		<h3>Sources:</h3>
		<!-- items are to be added here by a javascript -->
		<dl id="pathway_datasources" class="datasources"></dl>
	</div>	
	<p> <em>accessed N times</em> - is the total number of times 
		the provider's data were successfully served to a user; 
		i.e., <strong>it counts</strong> if a result (before converting 
		from BioPAX to another format, if requested) contains some 
		states, interactions, pathways (sub-model) from the original 
		data source; or it is a search or traverse request, 
		and the data source is mentioned; or a file 
		<a href='<c:url value="/downloads.html"/>'>is downloaded</a>.</p>

	<h2>Warehouse Data</h2>

	<p> The following databases were converted to BioPAX format 
		(to <em>EntityReference</em> and related utility class 
		objects, such as controlled vocabularies, features, etc.) 
		to form the basis for integrating pathway and interaction 
		data sources and also for id-mapping.<br/>
	</p>
		
	<div class="parameters">
		<h3>Sources:</h3>
		<!-- items are to be added here by a javascript -->
		<dl id="warehouse_datasources" class="datasources"></dl>
	</div>	
	<p> - access counts are not shown, for merely all users 
		deal with reference proteins and small 
		molecules, ontology terms, and identifiers on a regular basis; 
		so, <a href='<c:url value="/log"/>'>total number of all requests</a> 
		minus the access errors will be not far from the fair value 
		for each warehouse data source listed above.
	</p>

	<h3>Acknowledgment</h3>
	<p>
		${cpath.name} team much appreciate the contribution of 
		all the data providers, authors, also 
		<a href="http://www.ebi.ac.uk/miriam/">MIRIAM</a> and 
		<a href="http://identifiers.org/">Identifiers.org</a> projects, 
		all the open biological ontologies, and open-source
		projects and standards, which (who) made creating of this 
		integrated BioPAX web service and database feasible. <br/>
		Thank you people and all supporters.
	</p>
	
</div>
<jsp:include page="footer.jsp"/>

</body>
</html>
