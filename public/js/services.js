/*global define */

'use strict';




/**
* initialSorting should be a javascript object like:
* { 'game.nr': 'asc'}
*
*/
var setupTable = function(collection, ngTableParams, initialSorting, $scope, $filter){
    $scope.tableParams = new ngTableParams({
        page: 1,                 // show first page
        count: 15,               // count per page
        sorting: initialSorting  // initial sorting
    }, {
        total: 0,           // length of data
        getData: function($defer, params) {	
			var data = collection;
	        var orderedData = params.sorting() ? $filter('orderBy')(collection, params.orderBy()) : collection;
			orderedData = params.filter ? $filter('filter')(orderedData, params.filter()) : orderedData; 
			params.total(orderedData.length);
			var slice = orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count())
			$defer.resolve(slice);
	    }
     });
 };

/** global constants **/

define(['angular'], function(angular) {

/* Services */
// Demonstrate how to register services
// In this case it is a simple value service.
angular.module('myApp.services', [])
  .value('version', '0.1');
//  .factory('settingsFactory', function(Restangular){
//	  var settings = {
//		 Restangular.one('api/settings').get().then(function(stings){
//		 	var betterSettings = stings;				
//		 });
//	  };
//	  return settings;
//  });
});