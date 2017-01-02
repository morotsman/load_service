'use strict';

require([ 'angular', './load-service-dao' , 'flot'], function() {

	angular.module('loadService.loadServicePlot', [])
	.directive('loadServicePlot', ["$timeout", function($timeout) {
	  return {
		restrict: 'E',
		scope: {
			plotOptions : "=",
			data : "="	
		},
	    template: '<div></div>',
	    replace: true,
	    link: function(scope, element, attr) {   		
	    	$(element).plot(scope.data?scope.data:[], scope.plotOptions).data("plot");
	    	
	    	scope.$watch('data', function(newData) {
	    		console.log(newData);
	    		var plot = $(element).data("plot");
				if(plot) {
					plot.setData(newData)
					plot.setupGrid()
					plot.draw()
				}
	    	});
	    }
	  };
	}]);
	
});