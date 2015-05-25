<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>

<!DOCTYPE html>
<html>
<head>
	<link href='<c:url value="/resources/css/xeditable.css"/>' rel="stylesheet" />
	<jsp:include page="head.jsp" />
	<script src='<c:url value="/resources/scripts/xeditable.js"/>'></script>
	<script src='<c:url value="/resources/scripts/datasources.js"/>'></script>	
	<title>cPath2::Datasources</title>
</head>
<body>
<jsp:include page="header.jsp"/>

<div id="pathway_datasources" ng-app="dsApp" id="ng-app" ng-controller="DatasourcesController">

  <h2>Data Sources</h2>
  
  <security:authorize ifNotGranted="ROLE_ADMIN">
  <!-- the explanation below is not needed for Administrators -->
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
		the <a rel="nofollow" href='<c:url value="/log/TOTAL/stats"/>'>total number of requests</a> 
		minus <a rel="nofollow" href='<c:url value="/log/ERROR/stats"/>'>errors</a> will be fair estimate.
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
 </security:authorize>

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

<!-- for Admin users (detected by security:authorize), 
all the metadata's attributes are in-place editable, 
and there are action buttons (delete,save,upload, etc.), and input validation;
for regular users, - show the compact read-only summary of the data providers -->	
	<div ng-repeat="ds in datasources" class="row">
		 <security:authorize ifAnyGranted="ROLE_ADMIN">
			<div class="thumbnail">
      			<div class="caption">
        			<h3>
        				<img alt="provider's logo" class="datasource-logo" ng-src="{{ds.iconUrl}}" >&nbsp;{{ds.identifier}}		
    				</h3>
        			<div ng-class="{ 'has-error' : !ds.description }">
        			  <p>
        				<a href="#" editable-text="ds.description" e-required e-placeholder="Enter a description" ng-minlength="15">
        				{{ds.description || 'required (min. 15 symbols)'}}</a>
        				<span ng-show="!ds.description" class="help-block">required</span>
        			  </p>
        			</div>
       				<p ng-hide="ds.notPathwayData"><em>URI: </em>{{ds.uri  || 'empty'}}</p>
       				<div ng-class="{ 'has-error' : !ds.name[0] }">
       				  <p>
       					<em>display name: </em>
       					<a href="#" editable-text="ds.name[0]" e-required e-placeholder="Enter a display name" ng-minlength="3">
   						{{(ds.name[0] || 'required')}}</a>
   						<span ng-show="!ds.name[0]" class="help-block">required</span>
   					  </p>
   					</div>
       				<p><em>standard name: </em> <a href="#" editable-text="ds.name[1]" e-placeholder="Enter the standard name" ng-minlength="3">
   						{{ds.name[1] || 'empty'}}</a></p>
       				<p><em>synonym: </em> <a href="#" editable-text="ds.name[2]" e-placeholder="Enter a synonym">
   						{{ds.name[2] || 'empty'}}</a></p>
       				<div ng-class="{ 'has-error' : !ds.type }">
       				  <p>
       					<em>type: </em>
       					<a href="#" editable-select="ds.type" e-ng-options="v.value as v.value for v in dtypes" e-required>
       					{{ showType(ds) }}</a><span ng-show="!ds.type" class="help-block">required</span>
       				  </p>
       				</div>
       				<p><em>availability: </em><a href="#" editable-select="ds.availability" e-ng-options="v.value as v.value for v in dlicenses">
       					{{ showAvailability(ds) }}</a></p>
       				<div ng-class="{ 'has-error' : !ds.urlToHomepage }"><p><em>home URL: </em>
       					<a href="#" editable-url="ds.urlToHomepage" e-placeholder="Enter the homepage URL" e-required>
       					{{ds.urlToHomepage || 'empty'}}</a><span ng-show="!ds.urlToHomepage" class="help-block">required</span></p></div>
       				<p><em>data URL: </em>
       					<a href="#" editable-url="ds.urlToData" e-placeholder="Enter the data location">
       					{{ds.urlToData || 'empty'}}</a></p>
       				<div ng-class="{ 'has-error' : !ds.iconUrl }"><p><em>icon URL: </em>
       					<a href="#" editable-url="ds.iconUrl" e-placeholder="Enter the logo URL" e-required>
       					{{ds.iconUrl || 'empty'}}</a><span ng-show="!ds.iconUrl" class="help-block">required</span></p></div>
       				<p><em>cleaner class: </em>
       					<a href="#" editable-text="ds.cleanerClassname" 
       						e-placeholder="that implements cpath.importer.Cleaner interface" ng-minlength="15">
       					{{ds.cleanerClassname  || 'empty'}}</a>
       				</p>
       				<p><em>converter class: </em>
       					<a href="#" editable-text="ds.converterClassname" 
       						e-placeholder="that implements cpath.importer.Converter interface" ng-minlength="15">
       					{{ds.converterClassname  || 'empty'}}</a>
       				</p>
       				<p><em>PMID: </em><a href="#" editable-text="ds.pubmedId">{{ds.pubmedId || 'empty'}}</a> 
       				 	<a target="_blank" ng-href="http://identifiers.org/pubmed/{{ds.pubmedId}}">go to publicaion</a>
       				</p>
       				<p><em>data: </em>{{ (ds.uploaded) ? "uploaded as " + ds.dataArchiveName : "not uploaded" }}
       					<a rel="nofollow" ng-show="ds.uploaded" ng-href="admin/homedir/data/{{ds.identifier}}.zip"> (download), </a>&nbsp;
       					and {{ (ds.premerged) ? "premerged" : "not premerged yet" }}
       				</p>
        			 			
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
						
<!-- TODO (way later, but better do from console) to execute and monitor premerge/merge/index, etc. cPath2 admin tasks 
	from this web app is currently not as straighforward as it might seem due to: a single-user H2 database, Spring web context w/o biopax-validator, 
	not yet fully designed/implemented service methods for doing this, etc. -->
<!--         			<button class="btn btn-success" ng-click="executePremerge(ds)"  -->
<!--         				ng-disabled="! (ds.iconUrl && ds.urlToHomepage && ds.type && ds.name[0] && ds.description && ds.uploaded)">Premerge... </button>							 -->
      			</div>		
			</div>
		</security:authorize>

		<security:authorize ifNotGranted="ROLE_ADMIN">
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
       				  <em>Access summary: </em>
       				  <a target="_blank" rel="nofollow" class="alert-success" ng-href='<c:url value="/log/PROVIDER/{{ds.name[1] || ds.name[0]}}/stats"/>'>
       				  no. requests/users.</a>
       				</p>
       				<p>
       				  <em>Publication: </em>{{ds.citation}} 
       				  <a target="_blank" ng-href="http://identifiers.org/pubmed/{{ds.pubmedId}}">(PMID:{{ds.pubmedId}})</a>
       				</p>
       				<p><em>Availability: </em>{{ds.availability}}</p>
      			</div>		
			</div>
		  </security:authorize>
	</div>

  </div> <!-- ng-app wrapper -->

<jsp:include page="footer.jsp"/>

</body>
</html>
