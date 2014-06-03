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
  .directive('ngBetresult', function(){
	 return {
	      restrict: 'E',
	      require: '^ngModel',
		  scope: {
		    ngModel: '=',     // Bind the ngModel to the object given
		    onSend: '&',      // Pass a reference to the method 
		    fromName: '@'     // Store the string associated by fromName
		  },
	      templateUrl: 'partials/betresult.html',
		  controller: controllers.BetResultCtrl
	 } 	
  })
  ;

});