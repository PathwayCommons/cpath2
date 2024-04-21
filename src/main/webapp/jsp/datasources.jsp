<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html>
<head>
	<jsp:include page="head.jsp" />
	<script src="<spring:url value='/scripts/datasources.js'/>"></script>
	<title>${cpath.name} datasources</title>
</head>
<body>
<jsp:include page="header.jsp"/>

<div id="pathway_datasources" ng-app="dsApp" id="ng-app" ng-controller="DatasourcesController">

  <h2>Acknowledgment</h2>
	<div class="row">
	  <div class="jumbotron">
        <p>We much appreciate the fundamental contribution of all the data providers, authors,
        open biological ontologies, identifier registries, open-source projects and standards,
        which made creating of this web service possible.<br/></p>
        <blockquote>
		<ul>
			<li>WAREHOUSE data (canonical molecules, ontologies) are converted
			to BioPAX utility classes, such as <em>EntityReference, ControlledVocabulary,
			EntityFeature</em> sub-classes, and saved as the initial BioPAX model,
			which forms the foundation for integrating pathway data and for id-mapping.
			</li>
			<li>PATHWAY and binary interaction data (interactions, participants) are normalized
			and merged into the database. Original reference molecules are replaced
			with the corresponding BioPAX warehouse objects.</li>
			<li>MAPPING data are used to improve the ID-mapping, data merging, and make the graph queries easier to use.</li>
		</ul>
		</blockquote>
	  </div>
    </div>

  <h2>Data Sources</h2>
	<div ng-repeat="ds in datasources" class="row">
		<div class="thumbnail">
			<div class="caption">
				<h3>
					<img alt="provider's logo" class="datasource-logo" ng-src="{{ds.iconUrl}}">&nbsp;
					<a ng-href='{{ds.homepageUrl}}'>{{ds.name[1] || ds.name[0]}}</a>
				</h3>
				<p><strong>{{ds.description}}</code></strong></p>
				<p><var>Type: </var><code>{{ds.type}}</code></p>
				<p ng-hide="ds.type === 'WAREHOUSE' || ds.type === 'MAPPING'"><var>URI: </var><code>${cpath.xmlBase}{{ds.identifier}}</code></p>
				<p><var>Names: </var><code>{{uniqueStrings(ds.name) + ""}}</code></p>
				<p ng-hide="ds.type === 'WAREHOUSE' || ds.type === 'MAPPING'">
					<var>Contains: </var>
					<span ng-show="ds.numPathways > 0"><span class="badge alert-info">{{ds.numPathways}}</span> pathways,</span>
					<span ng-show="ds.numInteractions > 0"><span class="badge alert-info">{{ds.numInteractions}}</span> interactions,</span>
					<span ng-show="ds.numPhysicalEntities > 0"><span class="badge alert-info">{{ds.numPhysicalEntities}}</span> participants</span>
				</p>
				<p>
					<var>Publication: </var>{{ds.citation}}
					<a ng-href="https://bioregistry.io/pubmed:{{ds.pubmedId}}">(PMID:{{ds.pubmedId}})</a>
				</p>
				<p><var>Availability: </var><code>{{ds.availability}}</code></p>
			</div>
		</div>
	</div>

</div> <!-- ng-app wrapper -->

<jsp:include page="footer.jsp"/>

</body>
</html>
