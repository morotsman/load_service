/*global define */

'use strict';

define([ 'angular' ], function(angular) {

	var myModule = angular.module('myApp.load-service-dao', []);

	myModule.factory('loadServiceDao', [ '$http','$q', function($http,$q) {

		
		function getLoadResources() {
			return $http.get("load-resource");
		}
		
		
		function getLoadResourceDetails(loadResources) {
			return $q.all(loadResources.map(function(id){
				return getLoadResource(id);
			}));
		}
		
		function getLoadResource(name) {
			return $http.get("load-resource/" + name).then(function(loadResource) {
				loadResource.name = name;
				return loadResource;
			});
		}
		
		function mapLoadResource(loadResource) {
			return {
				numberOfRequestPerSecond: parseInt(loadResource.numberOfRequestPerSecond),
				url: loadResource.url,
				method: loadResource.method,
				maxTimeForRequestInMillis : parseInt(loadResource.maxTimeForRequestInMillis),
				body: loadResource.body
			};
		}
		
		function createLoadResource(loadResource) {
			return $http.post("load-resource", mapLoadResource(loadResource))
		}
		
		function deleteLoadResource(loadResource) {
			return $http.delete("load-resource/" + loadResource.name);
		}
		
		function updateLoadResource(loadResource) {
			return $http.put("load-resource/" + loadResource.name, mapLoadResource(loadResource));
		}

		
		return {
			getLoadResources : getLoadResources,
			getLoadResourceDetails : getLoadResourceDetails,
			createLoadResource : createLoadResource,
			deleteLoadResource : deleteLoadResource,
			updateLoadResource : updateLoadResource
		};


		
	} ]);

});