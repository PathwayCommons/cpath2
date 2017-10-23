"use strict";

var dsApp = angular.module('dsApp', ['ngRoute']);

dsApp.service('MyPubmed', ['$http', function ($http) {
	
	var euroPmcUrlPrefix = "http://www.ebi.ac.uk/europepmc/webservices/rest/search/query=EXT_ID:";
	var euroPmcUrlSuffix = "&format=json&callback=JSON_CALLBACK";
	
	/* get the publication summary from the Europe PubMed web service 
	 * by PMID (ds.pubmedId), using JSONP; nicely format the citation data;
	 * assign the resulting string to a new field in the ds */
    this.updateCitation = function(ds){
        $http.jsonp(euroPmcUrlPrefix+ds.pubmedId+euroPmcUrlSuffix)
        	.success(function(data){
        		var res = data.resultList.result[0];
        		var cite = res.authorString + " " +  res.title
        			+ " " + res.journalTitle + ". " + res.pubYear 
        			+ ";" + res.journalVolume 
        			+ "(" + res.issue + "):" + res.pageInfo;
        		
//        		console.log(res.pmid + ": " + res.title);      		
        		ds.citation = cite;
        	})
        	.error(function(data, status){
        		console.log(status + ' - ' + data.responseText);
        	});
    };
}]);


dsApp.controller('DatasourcesController', function($scope, $http, $filter, MyPubmed) {
// data for a quick off-line test	
//	$scope.datasources = [
//	  {"identifier" : "pid", "iconUrl" : "http://pathwaycommons.github.io/cpath2/logos/nci_nature.png", "description" : "NCI_Nature"},
//	  {"identifier" : "psp", "iconUrl" : "http://pathwaycommons.github.io/cpath2/logos/psp.png", "description" : "PhosphoSitePlus"},
//	  {"identifier" : "chebi", "iconUrl" : "http://pathwaycommons.github.io/cpath2/logos/chebi.png", "description" : "ChEBI SDF"},
//	];
	
	$http.get('metadata/datasources').success(function(datasources) {
		
		$scope.datasources = datasources;
		
		for(var i=0; i<datasources.length; i++) {
			//get citation by PMID (ds.pubmedId) using PubMed web service:
			MyPubmed.updateCitation(datasources[i]);
		}
		
	});	
	
	//cPath2 Metadata types and license options
	$scope.dtypes = [
	                   {value: 'WAREHOUSE'},
	                   {value: 'BIOPAX'},
	                   {value: 'PSI_MI'},
	                   {value: 'PSI_MITAB'},
	                   {value: 'MAPPING'}
	                  ];
	
	$scope.dlicenses = [
	                   {value: 'free'},
	                   {value: 'academic'},
	                   {value: 'purchase'}
	                  ];	
	
	$scope.showType = function(ds) {
	    var selected = $filter('filter')($scope.dtypes, {value: ds.type});
	    return (ds.type && selected.length) ? selected[0].value : 'Null';
	};
	
	$scope.showAvailability = function(ds) {
	    var selected = $filter('filter')($scope.dlicenses, {value: ds.availability});
	    return (ds.availability && selected.length) ? selected[0].value : 'Null';
	};	
	
	//makes a unique set of lower case strings
	$scope.uniqueStrings = function(strings) {
		var i, len=strings.length, out=[], h={};
		for (i=0;i<len;i++) {
			h[strings[i].toLowerCase()]=0;
		}
		for (i in h) {
			out.push(i);
		}
		return out;
	};
	
});
