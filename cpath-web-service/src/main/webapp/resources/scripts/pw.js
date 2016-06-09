"use strict";

$(function(){ // document ready

    $("#find-button").click(function() {
    	var keyword = $("#keyword-text").val();   	
    	if(keyword)
    		window.location = "#/pw/" + encodeURIComponent(keyword);   	
        return false;
    });
    
});


// AngularJS app and controllers:

var pcApp = angular.module('pcApp', ['ngRoute']);

// configure routes (client-side)
pcApp.config(function( $routeProvider ) 
{
	$routeProvider
		.when(
			"/home",
			{ action: "home.default" }
		)
		.when(
			"/pw",
			{ action: "pw.default" }
		)
		.when(
			"/pw/:kw",
			{ action: "pw.find" }
		)
		.otherwise(
			{ redirectTo: "/home" }
		);
});

pcApp.controller('PcController', function($scope, $route, $routeParams, $http) {
	
	var render = function() {
		
		var renderAction = $route.current.action;
		if(!renderAction)
			return;	
		
		$scope.renderAction = renderAction;	
		
		var renderPath = renderAction.split( "." );
		$scope.renderPath = renderPath;
		
		if(renderPath[0] == "pw") {
			//get the query parameter, if any
			var kw = ($routeParams.kw || "");
			$("#keyword-text").val(kw);

			if(kw) {
				//find pathways (top hits, the first 'page' only) by keyword(s)
				$http.get('search.json?type=pathway&q=' + encodeURIComponent(kw))
					.success(function(data, status) {
					$scope.response = data;
					$scope.status = status;
				}).error(function(data, status) {
					$scope.status = status;
					$scope.errMsg = "Search by '" + kw + "' failed (" + status + ")";
					$scope.response = {};
				});	
			} else {
				//list top pathways
				$http.get('top_pathways.json').success(function(data, status) {
					$scope.response = data;
					$scope.status = status;
				}).error(function(data, status) {
					$scope.status = status;
					$scope.errMsg = "Failed to get 'top pathways' (" + status + ")";
					$scope.response = {};
					console.log(data);
				});	
			}
		}
		
	};
    
    // Listen for changes to the Route. When the route
    // changes, update view.
    $scope.$on(
        "$routeChangeSuccess",
        function( $currentRoute, $previousRoute ){
            // Update the rendering.
            render();
        }
    );
    

    $scope.encode = function(uri) {
        return window.encodeURIComponent(uri);
    };
	    
});

