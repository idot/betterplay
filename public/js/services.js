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
  .value('version', '0.1')
  .factory('specialBetService', function($state, Restangular, toaster){
	  return {
		 
		getSpecialBet: function(betId, username, callback){	 
 		     Restangular.one('wm2014/api/specialBets', username).get().then(
	 		     function(success){
					 var user = success.user;
	 				 var templatebets = success.templatebets;
	 				 var tb = _.filter(templatebets, function(b){ return b.bet.id == betId; })[0];
	 				 callback(user, tb);
	 		     }	
		)},	 
 		
	    saveSelected : function(bet, user, selectedList){
	         var selected = _.filter(selectedList, function(t){ return t.selected; })[0];
			 bet.prediction = selected.name;
		     Restangular.all('wm2014/api/specialBet').customPOST(bet).then(
			     function(success){	 
			          if(! user.hadInstructions){
		                      Restangular.all('wm2014/api/userhadinstructions').customPOST().then(
		                           function(success){
		                                 toaster.pop('success', "Congratulations!", success+ " Please don't forget to place all special bets until start of the games");
		                           }		
		                      );			  	
			 		   };
				       $state.transitionTo("user.specialBets", { username: user.username }); 
			 }); 		
	     }}; 
	 });
 


//  .factory('settingsFactory', function(Restangular){
//	  var settings = {
//		 Restangular.one('wm2014/api/settings').get().then(function(stings){
//		 	var betterSettings = stings;				
//		 });
//	  };
//	  return settings;
//  });
});