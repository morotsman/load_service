'use strict';

require([ 'angular', './load-service-dao' , 'flot'], function() {

	angular.module('loadService.lsNameValue', [])
	.directive('lsNameValue', ["$timeout", function($timeout) {
	  return {
		restrict: 'E',
		scope: {
			nameValues : "=",
			nameLabel : "@"
		},
	    templateUrl: "assets/javascripts/ls-name-value.template.html",
	    replace: true,
	    link: function(scope, element, attr) {   		
	    	scope.nameValues = scope.nameValues?scope.nameValues:[];
	    },
	    controller: ['$scope', function MyTabsController($scope) {
	    	$scope.deleteEntry = deleteEntry;
	    	$scope.addEntry = addEntry;
	    	
	    	function deleteEntry(index) {
	    		$scope.nameValues.splice(index,1);
	    	}
	    	
	    	function addEntry() {
	    		$scope.nameValues.push({"name" : "", "value": ""});
	    		//$timeout(function() {
	    		//	var id = '#name' + ($scope.nameValues.length-1);
	    			//$(id).trigger("click");
	    		    //angular.element(id).triggerHandler('click');
	    		//},1000);
	    	}
	    	
	    }]
	  };
	}]);
	
});