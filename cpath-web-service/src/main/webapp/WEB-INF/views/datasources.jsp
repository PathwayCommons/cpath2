<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>

<!DOCTYPE html>
<html>
<head>
	<link href="<c:url value="/resources/css/xeditable.css"/>" rel="stylesheet" />
	<jsp:include page="head.jsp" />
	<script src="<c:url value="/resources/scripts/xeditable.js"/>"></script>
	<script src="<c:url value="/resources/scripts/datasources.js"/>"></script>	
	<title>cPath2::Datasources</title>
</head>
<body>
<jsp:include page="header.jsp"/>

  <h2>Data Sources</h2>
  
  <security:authorize ifNotGranted="ROLE_ADMIN">
  <!-- the explanation below is not needed for Administrators -->
  <div class="row">
	<div class="jumbotron">
	<h3>Two categories</h3>
	<blockquote><p>
		<em><strong>Warehouse</strong></em> data (canonical molecules, ontologies) are converted 
		to BioPAX utility classes, such as <em>EntityReference, ControlledVocabulary, 
		EntityFeature</em> sub-classes, and saved as the initial BioPAX model, 
		which forms the foundation for integrating pathway data and for id-mapping.
		<em><strong>Pathway</strong></em> and binary interaction data (interactions, participants) are normalized  
		next and merged into the database as well as original reference molecules are replaced 
		with the corresponding BioPAX warehouse objects.
	</p></blockquote>
	</div>
 </div>
 </security:authorize>

  <div id="pathway_datasources" class="row" ng-app="dsApp" id="ng-app" ng-controller="DatasourcesController">
	
  <security:authorize ifAnyGranted="ROLE_ADMIN">
  <div class="row">
    <form name="fds" class="form-inline" ng-submit="newDatasource()" novalidate>
     <div class="form-group" ng-class="{ 'has-error' : fds.did.$invalid && fds.did.$dirty }">
  	  <label>Identifier:
   	  	<input class="form-control" size="40" type="text" name="did" required 
   	  		ng-model="newIdentifier" ng-minlength="3" ng-maxlength="40" ng-pattern="/^\w+$/" did-unique="unique"
   	   		placeholder="Enter identifier (no spaces, dashes)"/> <!-- 'did-unique' is a directive defined in the datasources.js -->
      </label>
     </div>
	 <button class="btn btn-primary" type="submit" ng-disabled="fds.did.$pristine || fds.did.$invalid">Add New</button>
   	  <div class="invalid help-block" ng-show="fds.$dirty && fds.$invalid">Error: 	
	  	<span ng-show="fds.did.$error.pattern">Contains non-word characters. </span>
	  	<span ng-show="fds.did.$error.minlength">Too short (<3). </span>
      	<span ng-show="fds.did.$error.maxlength">Too long (>40). </span>
      	<span ng-show="fds.did.$error.didunique">Not unique. </span>
      	<span ng-show="fds.did.$error.required">Required. </span>
      </div>	 
    </form>
  </div>	
  </security:authorize>	
	
	<div ng-repeat="ds in datasources">

<!-- for Administrators, all metadata's attributes are in-place editable, 
and there are action buttons (delete,save,upload, etc.), and input validation -->

		 <security:authorize ifAnyGranted="ROLE_ADMIN">
		  <div class="col-xs-12">
			<div class="thumbnail">
      			<div class="caption">
        			<h3>
        				<img alt="provider's logo" class="datasource-logo" ng-src="{{ds.iconUrl}}" >&nbsp;{{ds.identifier}}		
    				</h3>
        			<div ng-class="{ 'has-error' : !ds.description}">
        				<a href="#" editable-text="ds.description" e-required e-placeholder="Enter a description" ng-minlength="15">
        				{{ds.description || 'required (min. 15 symbols)'}}</a>
        				<span ng-show="!ds.description" class="help-block">required</span>
        			</div>
        			<ul>
        				<li ng-if="!ds.notPathwaydata"><em>URI: </em>{{ds.uri  || 'empty'}}</li>
        				<li><div ng-class="{ 'has-error' : !ds.name[0]}">
        					<em>display name: </em>
        					<a href="#" editable-text="ds.name[0]" e-required e-placeholder="Enter a display name" ng-minlength="3">
    						{{(ds.name[0] || 'required')}}</a>
    						<span ng-show="!ds.name[0]" class="help-block">required</span>
    						</div>
    					</li>
        				<li><em>standard name: </em> <a href="#" editable-text="ds.name[1]" e-placeholder="Enter the standard name" ng-minlength="3">
    						{{ds.name[1] || 'empty'}}</a></li>
        				<li><em>synonym: </em> <a href="#" editable-text="ds.name[2]" e-placeholder="Enter a synonym">
    						{{ds.name[2] || 'empty'}}</a></li>
        				<li><div ng-class="{ 'has-error' : !ds.type}"><em>type: </em>
        					<a href="#" editable-select="ds.type" e-ng-options="v.value as v.value for v in dtypes" e-required>
        					{{ showType(ds) }}</a><span ng-show="!ds.type" class="help-block">required</span></div></li>
        				<li><em>availability: </em><a href="#" editable-select="ds.availability" e-ng-options="v.value as v.value for v in dlicenses">
        					{{ showAvailability(ds) }}</a></li>
        				<li><div ng-class="{ 'has-error' : !ds.urlToHomepage}"><em>home URL: </em>
        					<a href="#" editable-url="ds.urlToHomepage" e-placeholder="Enter the homepage URL" e-required>
        					{{ds.urlToHomepage || 'empty'}}</a><span ng-show="!ds.urlToHomepage" class="help-block">required</span></div></li>
        				<li><em>data URL: </em>
        					<a href="#" editable-url="ds.urlToData" e-placeholder="Enter the data location">
        					{{ds.urlToData || 'empty'}}</a></li>
        				<li><div ng-class="{ 'has-error' : !ds.iconUrl}"><em>icon URL: </em>
        					<a href="#" editable-url="ds.iconUrl" e-placeholder="Enter the logo URL" e-required>
        					{{ds.iconUrl || 'empty'}}</a><span ng-show="!ds.iconUrl" class="help-block">required</span></div></li>
        				<li ng-if="!ds.notPathwaydata"><em>cleaner class: </em>
        					<a href="#" editable-text="ds.cleanerClassname" 
        						e-placeholder="that implements cpath.importer.Cleaner interface" ng-minlength="15">
        					{{ds.cleanerClassname  || 'empty'}}</a>
        				</li>
        				<li ng-if="!ds.notPathwaydata"><em>converter class: </em>
        					<a href="#" editable-text="ds.converterClassname" 
        						e-placeholder="that implements cpath.importer.Converter interface" ng-minlength="15">
        					{{ds.converterClassname  || 'empty'}}</a>
        				</li>
        				<li ng-if="!ds.notPathwaydata"><em>PMID: </em><a href="#" editable-text="ds.pubmedId">{{ds.pubmedId || 'empty'}}</a> 
        				 	<a target="_blank" ng-href="http://identifiers.org/pubmed/{{ds.pubmedId}}">go to publicaion</a>
        				</li>
        				<li><em>data: </em>{{ (ds.uploaded) ? "uploaded as " + ds.dataArchiveName : "not uploaded" }}
        					<a ng-show="ds.uploaded" ng-href="admin/homedir/data/{{ds.identifier}}.zip"> (download), </a>&nbsp;
        					and {{ (ds.premerged) ? "premerged" : "not premerged yet" }}
        				</li>
        			</ul> 
        			 			
        			<div class="btn btn-default btn-file" >
						<label>
							<input ng-disabled="!ds.identifier" type="file" accept=".zip" 
								file-model="myFile[ds.identifier]"/>Select a zip 
						</label>
						<!-- the 'file-model' is a directive defined in the datasources.js-->
					</div>
					
					<button class="btn btn-primary" ng-click="uploadDatafile(ds)" 
						ng-disabled="!myFile[ds.identifier]">{{ (ds.uploaded) ? "Replace" : "Upload" }} </button>		

        			<button class="btn btn-danger" ng-click="deleteDatasource($index)" 
        				ng-disabled="! (ds.iconUrl && ds.urlToHomepage && ds.type && ds.name[0] && ds.description)">Delete </button>
				
					<button class="btn btn-warning" ng-click="saveDatasource(ds)" 
						ng-disabled="! (ds.iconUrl && ds.urlToHomepage && ds.type && ds.name[0] && ds.description)">Save </button>
						
        			<button class="btn btn-success" ng-click="executePremerge(ds)" 
        				ng-disabled="! (ds.iconUrl && ds.urlToHomepage && ds.type && ds.name[0] && ds.description && ds.uploaded)">Premerge... </button>							
      			</div>		
			</div>
		  </div>
		 </security:authorize>
		 
<!-- for regular users, show the compact read-only summary of the data providers -->

		<security:authorize ifNotGranted="ROLE_ADMIN">
		  <div class="col-sm-6">
			<div class="thumbnail">
      			<div class="caption">
        			<h3>
        				<!--  ng-src="metadata/logo/{{ds.identifier}}" -->
						<img alt="provider's logo" class="datasource-logo" ng-src="{{ds.iconUrl}}" >&nbsp;
							<a ng-href='{{ds.urlToHomepage}}'>{{ds.name[1] || ds.name[0]}}</a>
        			</h3>
        			<p>{{ds.description}}</p>
        			<p ng-if="!ds.notPathwaydata">uri: <a ng-href="{{ds.uri}}">{{ds.uri}}</a></p>
        			<ul>
        			    <li><em>type: </em>{{ds.type}}</li>
        			    <li><em>display name: </em>{{(ds.name[0]) | lowercase}}</li>
        				<li><em>standard name: </em>{{ds.name[1]}}</li>
        				<li><em>synonym: </em>{{ds.name[2]}}</li>
        				<li><em>availability: </em>{{ds.availability}}</li>
        				<li ng-if="!ds.notPathwaydata">
        					<em>counts: </em><span class="badge">{{ds.numPathways}}</span> pathways, 
        				 	<span class="badge">{{ds.numInteractions}}</span> interactions, 
        				 	<span class="badge">{{ds.numPhysicalEntities}}</span> states
        				</li>
        				<li ng-if="!ds.notPathwaydata"><em>accessed: </em><span class="badge">{{ds.numAccessed}}</span></li>
        				<li ng-if="!ds.notPathwaydata">
        				  <a target="_blank" ng-href="http://identifiers.org/pubmed/{{ds.pubmedId}}">main publication</a>
        				</li>
        			</ul>
      			</div>		
			</div>
		  </div>
		  </security:authorize>
	</div>

  </div> <!-- ng-app -->
	
	<h3>Remark</h3>
	<p>The total number of times a cut of provider's data were successfully 
		served to a user (accessed) is incremented every time when a query 
		result (before converting from BioPAX to another format if requested) 
		contains biological entities from that original resource; or, for /search 
		or /traverse requests, the data source is referred to in the result; 
		or a particular file <a href='<c:url value="/downloads.html"/>'>is downloaded</a>.
		Access counts are not stored for the warehouse data sources, 
		for merely all users hit canonical proteins, small 
		molecules, ontology terms and identifiers on a regular basis; 
		thus the <a href='<c:url value="/log"/>'>total number of all requests</a> 
		minus errors will be fair estimate is this case. 
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

<jsp:include page="footer.jsp"/>

</body>
</html>
