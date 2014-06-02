/*global define */

'use strict';

define(['angular', 'controllers'], function(angular, controllers) {

/* Directives */

angular.module('myApp.directives', [])
  .directive('appVersion', ['version', function(version) {
    return function(scope, elm, attrs) {
      elm.text(version);
    };
  }])
  .directive('betterBet', ['$rootScope', function($rootScope){
	 return {
	      restrict: 'E',
		  scope: {
		    bet: '=bet',     // Bind the betBet to the object given
	//	    onSend: '&',      // Pass a reference to the method 
			start: '=start'   
		  },
	      templateUrl: 'partials/bet.html',
		  controller: controllers.BetCtrl
	 } 	
  }])
  .directive('teamFlag', function(){  //<span class="f32"><span class="flag ag"></span></span>
  	  return {
  	     replace: true,
		 restrict: 'E',
		 link: function(scope, element, attrs) {//"attrs.iso"
    	        var flag = '<span class="f32"><span class="flag '+attrs.iso+'"></span></span>';
   	            element.append(flag);
		 }
  	  }	
  })
  ;

});