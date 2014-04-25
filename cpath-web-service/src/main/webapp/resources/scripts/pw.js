"use strict";

$(function(){ // document ready

    $("#find-form").submit(function() {
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
				//find pathways by keyword(s)
				$http.get('/find_pathway/' + 
						encodeURIComponent(kw)).success(function(data) 
				{
					$scope.hits = data;
				});	
			} else {
				//list top pathways (callback=JSON_CALLBACK is very important!)
				$http.get('/top_pathways').success(function(data) {
					$scope.hits = data;
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
	
});

