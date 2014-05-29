/*global require, requirejs */

'use strict';

/**
* dependencies are declared manually based on huntc comment:
* https://github.com/huntc/angular-seed-play-1/commit/1c4dad20cd881c6045592aa4f055663b724c19ae#commitcomment-6434953
**/

requirejs.config({
  paths: {
	'moment': ['../lib/momentjs/min/moment.min'],
    'angular': ['../lib/angularjs/angular'],
    'restangular': ['../lib/restangular/restangular'],
    'underscore': ['../lib/underscorejs/underscore'],
    'angular-ui': ['../lib/angular-ui/angular-ui'],
    'angular-ui-bootstrap': ['../lib/angular-ui-bootstrap/ui-bootstrap'],
    'angular-ui-router': ['../lib/angular-ui-router/angular-ui-router'],
	'ng-table': ['../lib/ng-table/ng-table']
  },
  shim: {
    'angular': {
      exports : 'angular'
    },
    'restangular': {
        deps: ['underscore', 'angular']
    },
    'angular-ui': {
        deps: ['angular']
    },
    'angular-ui-bootstrap': {
        deps: ['angular-ui']
    },
    'angular-ui-router':{
        deps: ['angular-ui']
    },
    'ng-table':{
        deps: ['angular']
    }
  }
});



require(['moment','angular', './controllers', './directives', './filters', './services', 'underscore', 'restangular','angular-ui','angular-ui-bootstrap','angular-ui-router', 'ng-table'],
  function(moment, angular, controllers) {   
	  moment().format();
   
   angular.module('myApp', ['myApp.filters', 'myApp.services', 'myApp.directives', 'restangular', 'ui.router', 'ngTable'])
      .run([ '$rootScope', '$state', '$stateParams', '$timeout', 'Restangular',
         function ($rootScope, $state, $stateParams, $timeout, Restangular){
			 var baseTime = Restangular.one('api/time');
			 
			 var updateTimeFromServer = function(){ 
				    baseTime.get().then(function(currentTime){			 
	 	  	        $rootScope.startupTime = new Date(currentTime.serverTime);
				    $rootScope.currentTime = $rootScope.startupTime;
		         })
			 };
			
			 //time before game start that bet closes
			 //1 minute more than on server to prevent submission errors for users
			 var MSTOCLOSING = 61 * 60 * 1000; //in ms
			 
			 //time to update clock
			 var UPDATEINTERVAL = 1000; //in ms
			 
			 //reload clock from server 
			 var RESETTIMEDIFF = 5 * 60 * 1000; //in ms
			 
		     $rootScope.$state = $state;
             $rootScope.$stateParams = $stateParams;

	   		 $rootScope.timeLeft = function(serverTime, current){
	   		   //boolean true add in/ago
	   		   //negative values = ago
	   		   //positive values = in
			   var diff = (serverTime -  MSTOCLOSING) - current;
	   	       var s = moment.duration(diff, "milliseconds").humanize(true);
	   		   return s;	 
	   	     };
	
	         $rootScope.betClosed = function(serverTime, current){
	            var diff = (serverTime -  MSTOCLOSING) - current;
				return diff < 0;	
	         };
	         
		
	   	     $rootScope.onTimeout = function(){
	           mytimeout = $timeout($rootScope.onTimeout,UPDATEINTERVAL);
	   		   $rootScope.currentTime = new Date(new Date($rootScope.currentTime).getTime() + UPDATEINTERVAL);
	   	       var timerunning = $rootScope.currentTime.getTime() - $rootScope.startupTime.getTime();
			   if(timerunning > RESETTIMEDIFF){
			       updateTimeFromServer();  	
			   }
			 }	
			 
		     updateTimeFromServer()
	   	     var mytimeout = $timeout($rootScope.onTimeout,UPDATEINTERVAL);	 

      }])
	  .config(function($stateProvider, $urlRouterProvider){
		  
	   
      
        $urlRouterProvider.otherwise("/users"); //home should be 1. if logged in

	       $stateProvider
		      .state('users', {
		            url:  "/users",
		            templateUrl: 'partials/users.html',
		            controller: controllers.UsersCtrl
		      })
		      .state('games', {
		            url: "/games",
		            templateUrl: 'partials/games.html',
		            controller: controllers.GamesCtrl
		      })  
		      .state('user', {
		            url:  "/user/:username",
		            templateUrl: 'partials/user.html',
		            controller: controllers.UserCtrl
		      })
			  .state('game', {
				  url: "/game/:gamenr",
				  templateUrl: 'partials/game.html',
				  controller: controllers.GameCtrl
			  });
	//	.state('home', { home of user
	//		url: '/home',
	//		templateUrl: 'partials/home.html',
	//		controller: controllers.HomeCtrl
	//	})
    });



    angular.bootstrap(document, ['myApp']);


});
