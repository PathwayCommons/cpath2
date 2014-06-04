<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<!DOCTYPE html>
<html>
<head>
<jsp:include page="head.jsp" />
<script type="text/javascript" src="<c:url value="/resources/scripts/pw.js"/>"></script>
<title>cPath2::Client</title>
</head>
<body data-spy="scroll" data-target=".navbar">
	<jsp:include page="header.jsp" />

	<h2>Client</h2>
	
	  Current action/view: <em ng-bind="renderAction">Unknown</em>&nbsp;
	  Sub-path: <em>{{ renderPath[ 1 ] }}</em>.
	
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
			<div class="row" id="find-row">
				<div class="span9 offset3">
					<form id="find-form">
						<div class="span8">
							<input type="text" id="keyword-text" value="" 
								class="input-large span3"
								placeholder="Enter a keyword (e.g. MDM2)">
							<button class="btn btn-large btn-primary" 
								id="find-button">Find Pathways &raquo;</button>
						</div>
					</form>
				</div>
			</div>
			<hr>
			<div class="row">
				<div class="span10 offset1 documentation">
					<!-- a list to be filled by script-->
					<span ng-show="{{ response.numHits > 0 }}">Total hits: {{response.numHits}}</span>
					<span ng-show="{{ response.numHits > response.maxHitsPerPage }}">
						(only top {{response.maxHitsPerPage }} are listed)</span>			
					<table class="table table-striped table-bordered">
					<thead><tr><th>Name</th><th>Source</th><th>Organism</th></tr></thead>
					<tbody>
					<!-- iterate over hits array -->
					<tr ng-repeat="hit in hits">
						<!-- %TODO%  link to PCViz by URI, using http://www.pathwaycommons.org/pcviz/#pathway/encodedURI -->
						<td>{{hit.name}}</td><td>{{hit.dataSource}}</td><td>{{hit.organism}}</td>
					</tr>
					</tbody>
					</table>
					<p class="text-center">
						<a href="#" title="go back to the top" class="top-scroll">^top</a>
					</p>
				</div>
			</div>
		</div>
	  </div>
	
	<jsp:include page="footer.jsp" />
	
</body>
</html>
