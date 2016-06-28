<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<c:set var="xmlBaseLength" value="${fn:length(cpath.xmlBase)}" />

<!DOCTYPE html>
<html>
<head>
<link href="<spring:url value='/resources/css/jquery.dataTables.css'/>" rel="stylesheet">
<jsp:include page="head.jsp" />
<script src="<spring:url value='/resources/scripts/pw.js'/>"></script>
<title>cPath2::View  (${cpath.name})</title>
</head>

<body>
  <jsp:include page="header.jsp" />

  <div id="ng-app" ng-app="pcApp" ng-controller="PcController">
	
<%--	Current action/view: <em ng-bind="renderAction">Unknown</em>&nbsp;
		Sub-path: <em>{{ renderPath[ 1 ] }}</em>.

        When the route changes, we're going to be setting up the
        renderPath - an array of values that help define how the
        page is going to be rendered. We can use these values to
        conditionally show / load parts of the page.
--%>
	  <div ng-switch on="renderPath[ 0 ]">
		<!-- Users' Site Home Page -->
        <div ng-switch-when="home">
       		<h2>For All Users (non-programmers)</h2>
  			<div class="row"><div class="jumbotron">
				<blockquote><p>
			TODO...
				</p></blockquote>
  			</div></div>       		
  		    <div class="col-sm-4">  			
       		<div class="thumbnail">
      			<div class="caption">
        			<h3><a href="#/pw">Pathways</a></h3>
        			<p>Get the list of <a href="#/pw">top pathways</a>, or 
       				find pathways using a simple keyword(s), e.g., <a rel="nofollow" href="#/pw/brca2">brca2</a>,
       				<a rel="nofollow" href="#/pw/P51587">P51587</a>, 
       				or full-text query, such as <a rel="nofollow" href="#/pw/+response%20+alcohol">+response%20+alcohol</a>
       				(see also about <a href="<spring:url value='/home#search'/>">the search</a> web service command).
       				</p>
       			</div>
       		</div>
       		</div>
       		    		
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
						<li><a rel="nofollow" target="_blank" href ng-href="#/sif/{{encode(hit.uri)}}">SIF view</a></li><%--TODO--%>
						<li><a rel="nofollow" target="_blank" href ng-href="#/sbgn/{{encode(hit.uri)}}">SBGN view</a></li><%--TODO--%>
						<li><a rel="nofollow" target="_blank" href ng-href="{{hit.uri}}">resolve URI</a></li>
						<li><a rel="nofollow" target="_blank" href ng-href="${cpath.xmlBase}get?uri={{encode(hit.uri)}}">BioPAX</a></li>
						<li><a rel="nofollow" target="_blank" href ng-href="${cpath.xmlBase}get?uri={{encode(hit.uri)}}&format=SIF">to SIF</a></li>
						<li><a rel="nofollow" target="_blank" href ng-href="${cpath.xmlBase}get?uri={{encode(hit.uri)}}&format=TXT">to TXT (ext. SIF)</a></li>
						<li><a rel="nofollow" target="_blank" href ng-href="${cpath.xmlBase}get?uri={{encode(hit.uri)}}&format=SBGN">to SBGN</a></li>
						<li><a rel="nofollow" target="_blank" href ng-href="${cpath.xmlBase}get?uri={{encode(hit.uri)}}&format=GSEA">to GSEA</a></li>
						</ul>
					</td>
				</tr>
				</tbody>
				</table>
			</div>
		</div>
		<div ng-switch-when="sif">
			  <h2>SIF view</h2>
			  <div class="row">
				<%--TODO embed CytoscapeJs here--%>
			  </div>
		</div>
	  </div>

  </div>
	
	<jsp:include page="footer.jsp" />
	
</body>
</html>
