<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="xmlBaseLength" value="${fn:length(cpath.xmlBase)}" />

<!DOCTYPE html>
<html>
<head>
<link href="/resources/css/jquery.dataTables.css" rel="stylesheet">
<jsp:include page="head.jsp" />
<script src="/resources/scripts/pw.js"></script>
<script>
	(function(i, s, o, g, r, a, m) {
		i['GoogleAnalyticsObject'] = r;
		i[r] = i[r] || function() {
			(i[r].q = i[r].q || []).push(arguments)
		}, i[r].l = 1 * new Date();
		a = s.createElement(o), m = s.getElementsByTagName(o)[0];
		a.async = 1;
		a.src = g;
		m.parentNode.insertBefore(a, m);
	})(window, document, 'script', '//www.google-analytics.com/analytics.js', 'ga');
	ga('create', 'UA-43341809-1', 'auto');
	ga('send', 'pageview');
	window.ga = ga;
</script>
<title>cPath2::Site</title>
</head>

<body>
	<jsp:include page="header.jsp" />

  <div id="ng-app" ng-app="pcApp" ng-controller="PcController">
	
<!-- 	  Current action/view: <em ng-bind="renderAction">Unknown</em>&nbsp; -->
<!-- 	  Sub-path: <em>{{ renderPath[ 1 ] }}</em>. -->
	
	  <!--
        When the route changes, we're going to be setting up the
        renderPath - an array of values that help define how the
        page is going to be rendered. We can use these values to
        conditionally show / load parts of the page.
      -->
	  <div ng-switch on="renderPath[ 0 ]">			
					
		<!-- Home Content (explains other dynamic views/paths) -->
        <div ng-switch-when="home">
       		<h3>Currently available views:</h3> 
       		<ul>
       			<li><a href="#/pw">#/pw (top pathways)</a></li>
       			<li>find pathways by keyword(s), e.g., <a href="#/pw/brca2">#/pw/brca2</a></li>
       		</ul>
        </div>
		
		<div ng-switch-when="pw">
		
			<h2>Biological Pathways</h2>
		
			<div class="row" id="find-row">
				<form id="find-form" class="form-inline col-sm-9">
					<input type="text" id="keyword-text" value="" class="input-large"
						placeholder="Enter a keyword (e.g. MDM2)">
					<button class="btn btn-large btn-primary" 
						id="find-button"> Find Pathways &raquo;</button>
				</form>
			</div>
			<hr>
			<div class="row">
				<span ng-show="response.numHits > 0">Total hits: {{response.numHits}} </span>
				<span ng-show="response.numHits > response.maxHitsPerPage">(top {{response.maxHitsPerPage}} are shown)</span>
				<span ng-show="status != 200" class="text-warning">{{errMsg}}</span>
				<table class="table table-striped table-bordered">
				<thead><tr><th>#</th><th>Pathway Name</th><th>Provider ID</th><th>Open/Get</th></tr></thead>
				<tbody>
				<!-- link rows to PCViz using pathway URIs -->
				<tr ng-repeat="hit in response.searchHit">
					<td>{{$index}}</td>
					<td>{{hit.name}}</td><td><code>{{hit.dataSource[0].substring(${xmlBaseLength})}}</code></td>
					<td>
						<ul class="list-inline">
						<li><a target="_blank" href ng-href="http://www.pathwaycommons.org/pcviz/#pathway/{{encode(hit.uri)}}">PCViz (view)</a></li>
						<li><a target="_blank" href ng-href="{{hit.uri}}">by URI</a></li>
						<li><a target="_blank" href ng-href="${cpath.xmlBase}get?uri={{encode(hit.uri)}}">BioPAX</a></li>
						<li><a target="_blank" href ng-href="${cpath.xmlBase}get?uri={{encode(hit.uri)}}&format=BINARY_SIF">SIF</a></li>
						<li><a target="_blank" href ng-href="${cpath.xmlBase}get?uri={{encode(hit.uri)}}&format=EXTENDED_BINARY_SIF">Ext.SIF</a></li>
						<li><a target="_blank" href ng-href="${cpath.xmlBase}get?uri={{encode(hit.uri)}}&format=SBGN">SBGN</a></li>
						<li><a target="_blank" href ng-href="${cpath.xmlBase}get?uri={{encode(hit.uri)}}&format=GSEA">GSEA(gmt)</a></li>
						</ul>
					</td>
				</tr>
				</tbody>
				</table>
			</div>
		</div>
	  </div>

  </div>
	
	<jsp:include page="footer.jsp" />
	
</body>
</html>
