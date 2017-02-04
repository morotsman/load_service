/*global define */

'use strict';

define([ 'angular' ], function(angular) {

	var myModule = angular.module('loadService.load-service-dao', []);

	myModule.factory('loadServiceDao', [ '$http','$q', function($http,$q) {

		
		function getLoadResources() {
			return $http.get("load-resource");
		}
		
		
		function getLoadResourceDetails(loadResources) {
			return $q.all(loadResources.map(function(id){
				return getLoadResource(id);
			}));
		}
		
		function getLoadResource(id) {
			return $http.get("load-resource/" + id).then(function(loadResource) {
				loadResource.id = id;
				return loadResource;
			});
		}
		
		function mapLoadResource(loadResource) {
			return {
				numberOfRequestPerSecond: parseInt(loadResource.numberOfRequestPerSecond),
				url: loadResource.url,
				method: loadResource.method,
				maxTimeForRequestInMillis : parseInt(loadResource.maxTimeForRequestInMillis),
				body: loadResource.body,
				expectedResponseCode: loadResource.expectedResponseCode?loadResource.expectedResponseCode:undefined,
				expectedBody: loadResource.expectedBody?loadResource.expectedBody:undefined,
				headers: loadResource.headers?loadResource.headers:undefined,
				requestParameters: loadResource.requestParameters?loadResource.requestParameters:undefined,
				numberOfSendingSlots: loadResource.numberOfSendingSlots?parseInt(loadResource.numberOfSendingSlots): undefined,
				rampUpTimeInSeconds: loadResource.rampUpTimeInSeconds?parseInt(loadResource.rampUpTimeInSeconds): undefined,
				fromNumberOfRequestPerSecond: loadResource.fromNumberOfRequestPerSecond?parseInt(loadResource.fromNumberOfRequestPerSecond): 0
			};
		}
		
		function createLoadResource(loadResource) {
			return $http.post("load-resource", mapLoadResource(loadResource))
		}
		
		function deleteLoadResource(loadResource) {
			return $http.delete("load-resource/" + loadResource.id);
		}
		
		function updateLoadResource(loadResource) {
			console.log(loadResource.fromNumberOfRequestPerSecond + " " + loadResource.numberOfRequestPerSecond);
			return $http.put("load-resource/" + loadResource.id, mapLoadResource(loadResource));
		}
		
		function createSession(loadResource) {
			return $http.put("load-session/" + loadResource.id)
		}
		
		function deleteSession(loadResource) {
			return $http.delete("load-session/" + loadResource.id)
		}

		
		return {
			getLoadResource : getLoadResource,
			getLoadResources : getLoadResources,
			getLoadResourceDetails : getLoadResourceDetails,
			createLoadResource : createLoadResource,
			deleteLoadResource : deleteLoadResource,
			updateLoadResource : updateLoadResource,
			createSession: createSession,
			deleteSession: deleteSession
		};


		
	} ]);

});