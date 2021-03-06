/*global require, requirejs */

'use strict';

requirejs.config({
  paths: {
    'angular': ['../lib/angularjs/angular'],
    'jquery': ['../lib/jquery/jquery.min'],
    'flot': ['../lib/flot/jquery.flot'],
    'toastr': ['../lib/angular-toastr/angular-toastr.tpls'],
    'angular-ui': ['../lib/angular-ui-bootstrap/ui-bootstrap-tpls']
  },
  shim: {
	'angular': {
		exports : 'angular',
		
    },
    'jquery': {
        exports : 'jquery',
        deps: ['angular']
     },
     'flot': {
         deps: ['jquery']
     },
     'toastr' : {
    	 deps: ['angular']
     },
     'angular-ui' : {
    	 deps: ['angular']
     }
  }
});

require(['angular','flot','./load-service-controller', './load-service-plot-directive', 'toastr', 'angular-ui', './load-service-name-value-directive'],
  function(angular, flot,controllers) {

    // Declare app level module which depends on filters, and services

    var app = angular.module('loadService', [ 'loadService.load-service-controller', 'loadService.loadServicePlot', 'toastr', 'ui.bootstrap', 'loadService.lsNameValue']);
    
    var $html = angular.element(document.getElementsByTagName('html')[0]);

	angular.element().ready(function() {
		$html.addClass('ng-app');
		angular.bootstrap($html, [app['name']]);
	});    

});