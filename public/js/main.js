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
			 Restangular.setErrorInterceptor(function(response, deferred, responseHandler) {
			    // if(response.status === 403) {
			        // refreshAccesstoken().then(function() {
			             // Repeat the request and then call the handlers the usual way.
			      //       $http(response.config).then(responseHandler, deferred.reject);
			    //         // Be aware that no request interceptors are called this way.
			    //     });

			   //      return false; // error handled
			   //  }
                 console.log("custom error handler: "+response.status+" "+response.text);
			     return true; // error not handled
			 });
			 
			 var queryTime = Restangular.one('api/time');
			 Restangular.one('api/settings').get().then(function(settings){
			 	$rootScope.betterSettings = settings;				
			 });
			 
			 var updateTimeFromServer = function(){ 
				    queryTime.get().then(function(currentTime){			 
	 	  	        $rootScope.startupTime = new Date(currentTime.serverTime);
				    $rootScope.currentTime = $rootScope.startupTime;
		         })
			 };
			 		 
			 $rootScope.logout = function(){
		         $rootScope.loggedInUser = { userId: -1, username: "" };
				 $rootScope.authtoken = "";		 
				 Restangular.setDefaultHeaders();	
			 };
			 
			 $rootScope.logout();
					 
			 //should we fetch the time from the server or take the interactively set time
			 $rootScope.TIMEFROMSERVER = true;
			 
			 //format for time display
			 $rootScope.DF = 'MM/dd HH:mm:ss';
			
			 //time before game start that bet closes
			 //1 minute more than on server to prevent submission errors for users
			 $rootScope.MSTOCLOSING = 61 * 60 * 1000; //in ms
			 
			 //time to update clock
			 $rootScope.UPDATEINTERVAL = 1000; //in ms
			 
			 //reload clock from server 
			 $rootScope.RESETTIMEDIFF = 5 * 60 * 1000; //in ms
			 			 
		     $rootScope.$state = $state;
             $rootScope.$stateParams = $stateParams;

	   		 $rootScope.timeLeft = function(serverTime, current){
	   		   //boolean true add in/ago
	   		   //negative values = ago
	   		   //positive values = in
			   var diff = (serverTime -  $rootScope.MSTOCLOSING) - current;
	   	       var s = moment.duration(diff, "milliseconds").humanize(true);
	   		   return s;	 
	   	     };
			 
			 $rootScope.loggedIn = function(){
				 console.log("check.login!");
			   return $rootScope.authtoken != "";
			 };
	
	         $rootScope.betClosed = function(serverTime, current){
	            var diff = (serverTime -  $rootScope.MSTOCLOSING) - current;
				return diff < 0;	
	         };
			 
			 $rootScope.canBet = function(serverTime, current, user, bet){
			 	var diff = (serverTime -  $rootScope.MSTOCLOSING) - current;
				var owner = user.id == bet.userId;
				return diff > 0 && owner;
			 };
		
	   	     $rootScope.onTimeout = function(){
	           mytimeout = $timeout($rootScope.onTimeout,$rootScope.UPDATEINTERVAL);
	   		   $rootScope.currentTime = new Date(new Date($rootScope.currentTime).getTime() + $rootScope.UPDATEINTERVAL);
	   	       var timerunning = $rootScope.currentTime.getTime() - $rootScope.startupTime.getTime();
			   if(timerunning > $rootScope.RESETTIMEDIFF && $rootScope.TIMEFROMSERVER){
			       updateTimeFromServer();  	
			   }
			 }	
			 
		     updateTimeFromServer()
	   	     var mytimeout = $timeout($rootScope.onTimeout,$rootScope.UPDATEINTERVAL);	 

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
			  })
			  .state('settings', {
			  	  url: "/settings",
				  templateUrl: 'partials/settings.html',
				  controller: controllers.SettingsCtrl
			  })
			  .state('login', {
			  	  url: "/login",
				  templateUrl: 'partials/login.html',
				  controller: controllers.LoginCtrl
			  })
			  .state('logout', {
				  url: "/logout",
				  onEnter: function($rootScope, $state) {
					  $rootScope.logout();
					  $state.transitionTo("users");
				  }
			  })
			  .state('createGame', {
			  	  url: "/createGame",
				  templateUrl: 'partials/createGame.html',
				  controller: controllers.CreateGameCtrl
			  })
			  .state('registerUser', {
				  url: "/registerUser",
				  templateUrl: 'partials/registerUser.html',
				  controller: controllers.RegisterUserCtrl
			  })
		//	  .state('signon', {
		//		  url: "/signon/:token",
		//		  templateUrl: 'partials/signon.html',
		//		  controller: controllers.SignonCtrl
		//	  })		  
			  ;
			  
    });



    angular.bootstrap(document, ['myApp']);


});
