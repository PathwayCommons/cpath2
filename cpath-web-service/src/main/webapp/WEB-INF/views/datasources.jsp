<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html>
<head>
	<link href="<spring:url value='/resources/css/xeditable.css'/>" rel="stylesheet" />
	<jsp:include page="head.jsp" />
	<script src="<spring:url value='/resources/scripts/xeditable.js'/>"></script>
	<script src="<spring:url value='/resources/scripts/datasources.js'/>"></script>
	<title>cPath2::Datasources</title>
</head>
<body>
<jsp:include page="header.jsp"/>

<div id="pathway_datasources" ng-app="dsApp" id="ng-app" ng-controller="DatasourcesController">

	<h2>Data Sources</h2>

	<div class="row">
		<div class="jumbotron">
			<blockquote>
				<p><em>Warehouse</em> data (canonical molecules, ontologies) are converted
					to BioPAX utility classes, such as <em>EntityReference, ControlledVocabulary,
						EntityFeature</em> sub-classes, and saved as the initial BioPAX model,
					which forms the foundation for integrating pathway data and for id-mapping.</p>
				<p><em>Pathway</em> and binary interaction data (interactions, participants) are normalized
					next and merged into the database. Original reference molecules are replaced
					with the corresponding BioPAX warehouse objects.</p>
			</blockquote>
		</div>

		<h3>Note:</h3>
		<p> Links to the access summary for Warehouse data sources are not provided below; however,
			the <a rel="nofollow" href="<spring:url value='/log/TOTAL/stats'/>">total number of requests</a>
			minus <a rel="nofollow" href="<spring:url value='/resources/ERROR/stats'/>">errors</a> will be fair estimate.
			Access statistics are computed from January 2014, except unique IP addresses, which are computed from November 2014.
		</p>

		<h3>Acknowledgment</h3>
		<p>
			The ${cpath.name} team much appreciates the fundamental contribution of
			all the data providers, authors, <a href="http://identifiers.org/">Identifiers.org</a>,
			all the open biological ontologies, the open-source projects and standards,
			which made creating of this integrated BioPAX web service and database feasible.<br/>
		</p>
	</div>

	<div ng-repeat="ds in datasources" class="row">
		<div class="thumbnail">
			<div class="caption">
				<h3>
					<!--  ng-src="metadata/logo/{{ds.identifier}}" -->
					<img alt="provider's logo" class="datasource-logo" ng-src="{{ds.iconUrl}}">&nbsp;
					<a ng-href='{{ds.urlToHomepage}}'>{{ds.name[1] || ds.name[0]}}</a>
				</h3>
				<p><strong>{{ds.description}}&nbsp;<em>({{ds.type}})</em></strong></p>
				<p ng-hide="ds.notPathwayData"><em>URI: </em><a ng-href="{{ds.uri}}">{{ds.uri}}</a></p>
				<p>
					<em>All names (for data filtering): </em>{{uniqueStrings(ds.name) + ""}}</span>
				</p>
				<p ng-hide="ds.notPathwayData">
					<em>Contains: </em>
					<span ng-show="ds.numPathways > 0"><span class="badge alert-info">{{ds.numPathways}}</span> pathways,</span>
					<span ng-show="ds.numInteractions > 0"><span class="badge alert-info">{{ds.numInteractions}}</span> interactions,</span>
					<span ng-show="ds.numPhysicalEntities > 0"><span class="badge alert-info">{{ds.numPhysicalEntities}}</span> participants</span>
				</p>
				<p ng-hide="ds.notPathwayData">
					<em><a target="_blank" rel="nofollow" class="alert-success" ng-href="<spring:url
       				  value='/log/PROVIDER/{{ds.name[1] || ds.name[0]}}/stats'/>">Access summary</a></em>
				</p>
				<p>
					<em>Publication: </em>{{ds.citation}}
					<a target="_blank" ng-href="http://identifiers.org/pubmed/{{ds.pubmedId}}">(PMID:{{ds.pubmedId}})</a>
				</p>
				<p><em>Availability: </em>{{ds.availability}}</p>
			</div>
		</div>
	</div>

</div> <!-- ng-app wrapper -->

<jsp:include page="footer.jsp"/>

</body>
</html>
