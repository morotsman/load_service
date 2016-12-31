/*global define */

'use strict';

require([ 'angular', './load-service-dao' ], function() {

	var controllers = angular.module('myApp.load-service-controller',
			[ 'myApp.load-service-dao' ]);

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
					delete plotData[getId("successful",resourceToDelete)];
					delete plotData[getId("failed",resourceToDelete)];
					delete plotData[getId("latancy",resourceToDelete)];			
				}
				
				function startSession(index) {
					loadServiceDao.createSession($scope.loadResourceList[index]).then(listLoadResources);
				}
				
				function stopSession(index) {
					loadServiceDao.deleteSession($scope.loadResourceList[index]).then(listLoadResources);
				}
				

				function getFailedChartOptions () {
					return {
						series: {shadowSize: 0},
						xaxis: {
							show: false
						},	
						legend: { position: "se"}
					}
				}		
				
				
				function getSucessfulChartOptions () {
					return {
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
					}
				}
				
				var plotData = {};
			
				
				
				function getPlotData(resource, type, data) {		
					var result = getHistoricData(type, resource);
					if(result.length > maxNumberOfStatistics) {
						result.shift();
					}
					
					var nextIndex = result[result.length-1]?(result[result.length-1][0] + 1):0; 
					
					result.push([nextIndex,data]);
					return result;
				}
				
				function updateSuccessfulPlot(resource, numberOfRequests, latancy) {
					var numbers = getPlotData(resource, "successful", numberOfRequests);
					var latancies = getPlotData(resource, "latancy", latancy);		
					
					var successfulDataset = [
					                         { label: "Req/s", data: numbers, points: { symbol: "triangle"}},
								             { label: "Latancy", data: latancies, points: { symbol: "triangle"}, yaxis:2}
								           ];
					
					var plot = $("#" + getId("successful",resource)).data("plot");
					if(plot) {
						plot.setData(successfulDataset)
						plot.setupGrid()
						plot.draw()
					}					
				}
				
				function updateFailedPlot(resource, numberOfRequests) {
					var failed = getPlotData(resource, "failed", numberOfRequests);
					
					var failedDataset = [
							               { label: "Req/s", data: failed, points: { symbol: "triangle"} }
							           ];
					
					var plot = $("#" + getId("failed", resource)).data("plot");
					if(plot) {
						plot.setData(failedDataset)
						plot.setupGrid()
						plot.draw()
					}	
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
						plotData[getId("latancy",resource)] = [];
						plotData[getId("failed",resource)] = [];
					}
					var successful = getHistoricData("successful", resource);
					var failed = getHistoricData("failed", resource);
					var latancy = getHistoricData("latancy", resource);
					
					var successfulDataset = [
					               { label: "Req/s", data: successful, points: { symbol: "triangle"}},
					               { label: "Latancy", data: latancy, points: { symbol: "triangle"}, yaxis:2}
					           ];
					var failedDataset = [
							               { label: "Req/s", data: failed, points: { symbol: "triangle"} }
							           ];
					
					$("#" + getId('successful', resource)).plot(successfulDataset, getSucessfulChartOptions()).data("plot");
					$("#" + getId('failed', resource)).plot(failedDataset, getFailedChartOptions()).data("plot")
					
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