/*global define */

'use strict';

require([ 'angular', './load-service-dao' ], function() {

	var controllers = angular.module('myApp.load-service-controller',
			[ 'myApp.load-service-dao' ]);

	controllers.controller('loadServiceCtrl', [ '$scope', 'loadServiceDao','$q',
			function($scope, loadServiceDao, $q) {
				//var websocket = new WebSocket("ws://localhost:9000/ws");
				//websocket.onmessage = updateStatistics

				$scope.newLoadResource = newLoadResource; 
				$scope.createLoadResource = createLoadResource;
				$scope.updateLoadResource = updateLoadResource;
				$scope.deleteLoadResource= deleteLoadResource;
				$scope.startSession = startSession;
				$scope.stopSession = stopSession;
				$scope.loadResourceList = [];
				$scope.showInfo = showInfo;
		
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
						numberOfRequestPerSecond: "",
						currentSide: "flippable_front"
					});
				};
				
				function addResource() {
					$scope.serviceUnderConstruction = false;
				}
				
				function updateLoadResource(index) {
					loadServiceDao.updateLoadResource($scope.loadResourceList[index]).then(listLoadResources).then(addResource);
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
					loadServiceDao.createSession($scope.loadResourceList[index]);
				}
				
				function stopSession(index) {
					loadServiceDao.deleteSession($scope.loadResourceList[index]);
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
				
				function updatePlot(method, path, numberOfRequests, eventType) {
					
					var data = getHistoricData(eventType, method, path);
					if(data.length > 1000) {
						data.shift();
					}
					data.push([data.length, numberOfRequests]);
					
					var plot = $("#" + eventType + method + path).data("plot");
					if(plot) {
						plot.setData([data])
						plot.setupGrid()
						plot.draw()
					}
					
				}
				
				function updateStatistics(msg) {
					var data = JSON.parse(msg.data);
					var method = data.resource.method;
					var path = data.resource.path;
					var eventType = data.eventType
					var numberOfRequests = data.numberOfRequestsPerSecond;
					
					updatePlot(method, path, numberOfRequests,eventType);
				}
				
				
				
				
				function getHistoricData(eventType, method, path) {	
					return plotData[eventType + method + path];
				}
				
				function watchStatistics(mock) {
					/*
					websocket.send(JSON.stringify({action:"watch", resource: {method: mock.method, path: mock.path}}));
					if(!plotData["incoming" + mock.method + mock.path]) {
						plotData["incoming" + mock.method + mock.path] = [];
						plotData["completed" + mock.method + mock.path] = [];
					}
					var incoming = getHistoricData("incoming", mock.method, mock.path);
					var completed = getHistoricData("completed", mock.method, mock.path);
					
					var incomingDataset = [
					               { label: "Incoming requests", data: incoming, points: { symbol: "triangle"} }
					           ];
					var completedDataset = [
							               { label: "Completed requests", data: completed, points: { symbol: "triangle"} }
							           ];
					var chartOptions = getChartOptions();
					$('#completed' + mock.method + mock.path).plot(completedDataset, chartOptions).data("plot");
					$('#incoming' + mock.method + mock.path).plot(incomingDataset, chartOptions).data("plot")
					*/
				}
				
				function unWatchStatistics(mock) {
					//websocket.send(JSON.stringify({action:"unWatch", resource: {method: mock.method, path: mock.path}}));
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