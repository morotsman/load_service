/*global define */

'use strict';

require([ 'angular', './load-service-dao'], function() {

	var controllers = angular.module('loadService.load-service-controller',
			[ 'loadService.load-service-dao' ]);

	controllers.controller('loadServiceCtrl', [ '$scope', 'loadServiceDao','$q','$timeout',
			function($scope, loadServiceDao, $q, $timeout) {
				var maxNumberOfStatistics = 100;
		
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
				
				$scope.failedChartOptions = {
						series: {shadowSize: 0},
						xaxis: {
							show: false
						},	
						legend: { position: "se"}
					}
				
				$scope.sucessfulChartOptions = {
						series: {shadowSize: 0},
						xaxis: {
							show: false
						},
						legend: { position: "se"},
						yaxes: [ { min: 0 }, {
							min:0,
							alignTicksWithAxis: 1,
							position: 1,
							tickFormatter: function (v, axis) {
								return v.toFixed(axis.tickDecimals) + "ms";
							}
						}]
					};				
				

		
				activate();
				
				function activate() {
					listLoadResources();
				}
				
				function getResource(id) {
					return $scope.loadResourceList.find(function(r) {
						return r.id === id;
					});
				}
				
				function getCurrentSide(id) {
					var current = getResource(id);
					if(current) {
						return current.currentSide;
					} else {
						return "flippable_front";
					}
					
				}
				
				function getCurrentPlotData(id) {
					var current = getResource(id);
					if(current) {
						return current.plotData;
					} else {
						return {};
					}
				}
				
				function backDisplayed(r) {
					return r.currentSide === "flippable_back";
				}
 
				function listLoadResources (){
					loadServiceDao.getLoadResources().then(function(services) {
						loadServiceDao.getLoadResourceDetails(services.data).then(function(loadServiceDetails){
							
							var result = [];
							for(var i = 0; i < loadServiceDetails.length; i++) {
								var details = loadServiceDetails[i].data;
								result.push({
									create: false,
									id: services.data[i],
									method: details.method,
									url: details.url,
									body: details.body,	
									status: details.status,
									numberOfRequestPerSecond: details.numberOfRequestPerSecond,
									maxTimeForRequestInMillis: details.maxTimeForRequestInMillis,
									currentSide: getCurrentSide(services.data[i]),
									plotData : getCurrentPlotData(services.data[i])
								});
							}
							$scope.loadResourceList = result;
							//$timeout(function(){
								//$scope.loadResourceList.filter(backDisplayed).forEach(watchStatistics);
							//},1);
							
						
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
				}
				
				function startSession(index) {
					loadServiceDao.createSession($scope.loadResourceList[index]).then(listLoadResources);
				}
				
				function stopSession(index) {
					loadServiceDao.deleteSession($scope.loadResourceList[index]).then(listLoadResources);
				}
				
				function updatePlot(plot, data) {
					var plotData = plot.data;
					var nextIndex = plotData[plotData.length-1]?(plotData[plotData.length-1][0] + 1):0; 
					plotData.push([nextIndex,data]);
				}
				
				function updateSuccessfulPlot(resource, numberOfRequests, latancy) {
					var currentData = getResource(resource.id).plotData.successfulPlotData;	
					updatePlot(currentData[0], numberOfRequests);
					updatePlot(currentData[1], latancy);
				}
				
				function updateFailedPlot(resource, numberOfRequests) {
					var currentData = getResource(resource.id).plotData.failedPlotData;	
					updatePlot(currentData[0], numberOfRequests);
				}
				
				function updateStatistics(msg) {
					var data = JSON.parse(msg.data);
					var eventType = data.eventType
					var numberOfRequests = data.numberOfRequestsPerSecond;
					var latancy = data.avargeLatancyInMillis;
					if(eventType === "successful") {
						updateSuccessfulPlot(data.resource, numberOfRequests, latancy);
					} else if(eventType === "failed") {
						updateFailedPlot(data.resource, numberOfRequests);
					}
					$scope.$apply();
				}	
				
				function watchStatistics(resource, index) {
					websocket.send(JSON.stringify({action:"watch", resource: {method: resource.method, url: resource.url, id: resource.id}}));
					
					var currentResource = getResource(resource.id); 
					
					if(!currentResource.plotData) {
						currentResource.plotData = {};
					}
					
					if(!currentResource.plotData.successfulPlotData) {
						currentResource.plotData.successfulPlotData = [{ label: "Req/s", data: []}, { label: "Latancy", data: [], yaxis:2}];
					}
					
					if(!currentResource.plotData.failedPlotData) {
						currentResource.plotData.failedPlotData = [{ label: "Req/s", data: [], points: { symbol: "triangle"} }]; 
					}
				}
				
				function unWatchStatistics(resource) {
					websocket.send(JSON.stringify({action:"unWatch", resource: {method: resource.method, url: resource.url, id: resource.id}}));
				}

				function showInfo(index) {
					var resource = $scope.loadResourceList[index]; 
					if(resource.currentSide === "flippable_front") {
						resource.currentSide = "flippable_back";
						watchStatistics(resource, index);
					} else {
						resource.currentSide = "flippable_front";
					}
				}
				
				
			} ]);


	return controllers;

});