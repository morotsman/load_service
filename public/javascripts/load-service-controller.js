/*global define */

'use strict';

require([ 'angular', './load-service-dao' ], function() {

	var controllers = angular.module('myApp.load-service-controller',
			[ 'myApp.load-service-dao' ]);

	controllers.controller('loadServiceCtrl', [ '$scope', 'loadServiceDao','$q','$timeout',
			function($scope, loadServiceDao, $q, $timeout) {
				var websocket = new WebSocket("ws://localhost:9001/ws");
				websocket.onmessage = updateStatistics

				$scope.newLoadResource = newLoadResource; 
				$scope.createLoadResource = createLoadResource;
				$scope.updateLoadResource = updateLoadResource;
				$scope.deleteLoadResource= deleteLoadResource;
				$scope.startSession = startSession;
				$scope.stopSession = stopSession;
				$scope.loadResourceList = [];
				$scope.showInfo = showInfo;
				$scope.getId = getId;
		
				activate();
				
				function activate() {
					listLoadResources();
				}
 
				function listLoadResources (){
					loadServiceDao.getLoadResources().then(function(services) {
						loadServiceDao.getLoadResourceDetails(services.data).then(function(loadServiceDetails){
							
							var result = [];
							for(var i = 0; i < loadServiceDetails.length; i++) {
								var details = loadServiceDetails[i].data;
								result.push({
									create: false,
									name: services.data[i],
									method: details.method,
									url: details.url,
									body: details.body,	
									status: details.status,
									numberOfRequestPerSecond: details.numberOfRequestPerSecond,
									maxTimeForRequestInMillis: details.maxTimeForRequestInMillis,
									currentSide: "flippable_front"
								})
							}
							$scope.loadResourceList = result;
							
						
						});
					})
				}
				
				
				function newLoadResource(){
					$scope.serviceUnderConstruction= true;
					$scope.loadResourceList.push({
						create: true,
						method: "GET",
						url: "",
						body: "",
						status: "Inactive",
						numberOfRequestPerSecond: "",
						currentSide: "flippable_front"
					});
				};
				
				function addResource() {
					$scope.serviceUnderConstruction = false;
				}
				
				function updateLoadResource(index) {
					var resource = $scope.loadResourceList[index];
					
					if(resource.status === 'Active') {
						loadServiceDao.deleteSession(resource).then(function(){
							loadServiceDao.updateLoadResource(resource).then(function() {
								loadServiceDao.createSession(resource).then(listLoadResources);							
							})
						})
					} else {
						loadServiceDao.updateLoadResource(resource).then(listLoadResources);
					}
					
					
				}
				
				
				function createLoadResource(index) {
					loadServiceDao.createLoadResource($scope.loadResourceList[index]).then(listLoadResources).then(addResource);
				}
				
				function deleteLoadResource(index) {
					var resourceToDelete = $scope.loadResourceList[index];
					if(!resourceToDelete.create) {
						unWatchStatistics(resourceToDelete);
						loadServiceDao.deleteLoadResource(resourceToDelete).then(listLoadResources);
					} else {
						addResource();
					}
					$scope.loadResourceList.splice(index,1);
					delete plotData["incoming" + resourceToDelete.method + resourceToDelete.path];
					delete plotData["completed" + resourceToDelete.method + resourceToDelete.path];
				}
				
				function startSession(index) {
					loadServiceDao.createSession($scope.loadResourceList[index]).then(listLoadResources);
				}
				
				function stopSession(index) {
					loadServiceDao.deleteSession($scope.loadResourceList[index]).then(listLoadResources);
				}
				

				function getChartOptions () {
					return {
						series: {shadowSize: 0},
						  xaxis: {
							  show: false
						  }				    
					}
				}	
				
				var plotData = {};
				
				function updatePlot(resource, numberOfRequests, eventType) {
					
					var data = getHistoricData(eventType, resource);
					if(data.length > 1000) {
						data.shift();
					}
					data.push([data.length, numberOfRequests]);
					
					var plot = $("#" + getId(eventType,resource)).data("plot");
					if(plot) {
						plot.setData([data])
						plot.setupGrid()
						plot.draw()
					}
					
				}
				
				function updateStatistics(msg) {
					console.log(msg.data);
					var data = JSON.parse(msg.data);
					var eventType = data.eventType
					var numberOfRequests = data.numberOfRequestsPerSecond;		
					updatePlot(data.resource, numberOfRequests,eventType);
				}
				
				
				
				
				function getHistoricData(eventType, resource) {	
					return plotData[getId(eventType,resource)];
				}
				
				function getId(type, resource) {
					return type + resource.method + resource.url.replace(/\//g, "").replace(/:/g, "");
				}
				
				function watchStatistics(resource) {
					
					websocket.send(JSON.stringify({action:"watch", resource: {method: resource.method, url: resource.url}}));
					if(!plotData[getId("successful",resource)]) {
						plotData[getId("successful",resource)] = [];
						plotData[getId("failed",resource)] = [];
					}
					var successful = getHistoricData("successful", resource);
					var failed = getHistoricData("failed", resource);
					
					var successfulDataset = [
					               { label: "Successful requests", data: successful, points: { symbol: "triangle"} }
					           ];
					var failedDataset = [
							               { label: "Failed requests", data: failed, points: { symbol: "triangle"} }
							           ];
					var chartOptions = getChartOptions();
					$("#" + getId('successful', resource)).plot(successfulDataset, chartOptions).data("plot");
					$("#" + getId('failed', resource)).plot(failedDataset, chartOptions).data("plot")
					
				}
				
				function unWatchStatistics(resource) {
					websocket.send(JSON.stringify({action:"unWatch", resource: {method: resource.method, url: resource.url}}));
				}

				function showInfo(index) {
					var resource = $scope.loadResourceList[index]; 
					if(resource.currentSide === "flippable_front") {
						resource.currentSide = "flippable_back";
						watchStatistics(resource);
					} else {
						resource.currentSide = "flippable_front";
					}
				}
				
				
			} ]);


	return controllers;

});