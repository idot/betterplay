/*global require, requirejs */

'use strict';

/**
* dependencies are declared manually based on huntc comment:
* https://github.com/huntc/angular-seed-play-1/commit/1c4dad20cd881c6045592aa4f055663b724c19ae#commitcomment-6434953
**/

requirejs.config({
  paths: {
	'underscore': ['../lib/underscorejs/underscore'],
	'moment': ['../lib/momentjs/min/moment.min'],
    'angular': ['../lib/angularjs/angular'],
	'angular-cookies': ['../lib/angularjs/angular-cookies'],
	'angular-animate': ['../lib/angularjs/angular-animate'],
    'restangular': ['../lib/restangular/restangular'],
    'angular-ui': ['../lib/angular-ui/angular-ui'],
    'angular-ui-bootstrap': ['../lib/angular-ui-bootstrap/ui-bootstrap-tpls'],
    'angular-ui-router': ['../lib/angular-ui-router/angular-ui-router'],
	'ng-table': ['../lib/ng-table/ng-table'],
	'angular-ui-utils': ['../lib/angular-ui-utils/ui-utils'],
	'toaster': ['toaster'],
	
  },
  shim: {
    'angular': {
      exports : 'angular'
    },
	'angular-cookies': {
		deps: ['angular']
	},
	'angular-animate': {
		deps: ['angular']
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
    },
	'angular-ui-utils': {
		deps: ['angular']
	},
	'toaster': {
		deps: ['angular-animate']
	}
  }
});



require(['moment','angular', './controllers', './directives', './filters', './services', 'underscore', 'angular-cookies', 'angular-animate', 'restangular','angular-ui','angular-ui-bootstrap','angular-ui-router', 'ng-table', 'angular-ui-utils', 'toaster'],
  function(moment, angular, controllers) {   
	  moment().format();
   
   angular.module('myApp', ['myApp.filters', 'myApp.services', 'myApp.directives', 'ngCookies', 'ngAnimate', 'restangular', 'ui', 'ui.bootstrap', 'ui.bootstrap.tabs', 'ui.bootstrap.datepicker', 'ui.bootstrap.timepicker', 'ui.router', 'ngTable','ui.utils', 'toaster'])
      .run([ '$rootScope', '$state', '$stateParams', '$timeout', '$cookies', 'Restangular', 'toaster',
         function ($rootScope, $state, $stateParams, $timeout, $cookies, Restangular, toaster){
			 Restangular.setErrorInterceptor(function(response, deferred, responseHandler) {
			    // if(response.status === 403) {
			        // refreshAccesstoken().then(function() {
			             // Repeat the request and then call the handlers the usual way.
			      //       $http(response.config).then(responseHandler, deferred.reject);
			    //         // Be aware that no request interceptors are called this way.
			    //     });

			   //      return false; // error handled
			   //  }
			     var errors = _.map(response.data, function(value, key){ return key + " " +value; });
				 var errorsum = errors.join("\n");
			     toaster.pop('error', "application error: "+response.statusText, errorsum);
                 console.log("custom error handler: "+response.status+" "+response.data);
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
			 		 
			 $rootScope.isDebug = function(){
			     if(typeof $rootScope.betterSettings !== "undefined"){
			     	 return $rootScope.betterSettings.debug;
			     }else{
					 return false;
			     }	
			 };		 
					 
			 $rootScope.logout = function(){
		         $rootScope.loggedInUser = { id: -1, username: "" };
				 $rootScope.authtoken = "";	
				 delete $cookies["AUTH-TOKEN"];	 
				 Restangular.setDefaultHeaders();	
			 };
			  		 
			 /**
			 * opening a new window looses all info in the new window
			 * we grab the cookie containing the auth token and reload the user
			 * if cookie not there logout => reset user to default
			 */
			 $rootScope.reauthenticate = function(){
				 if(typeof $rootScope.authtoken === "undefined" || $rootScope.authtoken == ""){
				 	var auth = $cookies["AUTH-TOKEN"];
					if(typeof auth !== "undefined"){
   				       $rootScope.authtoken = auth;
   			 	       Restangular.setDefaultHeaders({'X-AUTH-TOKEN': auth});
					   Restangular.one('api/userWithEmail').get().then(function(userWithEmail){
						       $rootScope.loggedInUser = userWithEmail;
					   });
					}	
				 }else{
				 	$rootScope.logout();
				 }
			 };
			 
			 $rootScope.reauthenticate();	  
			 
			 /**
			 * update user calls this function without auth
			 * 
			 **/
			 $rootScope.updateLogin = function(user, auth){
				 $rootScope.loggedInUser = user;
				 if( typeof auth !== "undefined" ) { 
				     $rootScope.authtoken = auth;
			 	     Restangular.setDefaultHeaders({'X-AUTH-TOKEN': auth});
				}
			 }

			 $rootScope.isAdmin = function(){
			     if(typeof $rootScope.loggedInUser === "undefined" || typeof $rootScope.loggedInUser.isAdmin === "undefined"){
					 return false;
				 }else{
				 	 return $rootScope.loggedInUser.isAdmin;					
				 }
			 };		 
			 
			 $rootScope.isOwner = function(userId){
				 if(typeof $rootScope.loggedInUser === "undefined" || typeof $rootScope.loggedInUser.id === "undefined"){
					 return false;
				 }else{
			         return $rootScope.loggedInUser.id === userId;	
			     }
			 };
			 
			 $rootScope.isLoggedIn = function(){
			     return typeof $rootScope.authtoken !== "undefined" && $rootScope.authtoken != "";
			 };
					 					 
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

	   		 $rootScope.timeLeft = function(serverStart, current){
	   		   //boolean true add in/ago
	   		   //negative values = ago
	   		   //positive values = in
			   var diff = (serverStart -  $rootScope.MSTOCLOSING) - current;
	   	       var s = moment.duration(diff, "milliseconds").humanize(true);
	   		   return s;	 
	   	     };
			 

	         $rootScope.betClosed = function(serverStart, current){
	            var diff = (serverStart -  $rootScope.MSTOCLOSING) - current;
				return diff < 0;	
	         };
			 
			 $rootScope.canBet = function(serverStart, current, bet){
				 var diff = (serverStart -  $rootScope.MSTOCLOSING) - current;
				 var owner = $rootScope.isOwner(bet.userId);
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
			  .state('admin.createGame', {
			  	  url: "/createGame",
				  templateUrl: 'partials/createGame.html',
				  controller: controllers.CreateGameCtrl
			  })
			  .state('admin.registerUser', {
				  url: "/registerUser",
				  templateUrl: 'partials/registerUser.html',
				  controller: controllers.RegisterUserCtrl
			  })
			  .state('editUser', {
			  	  url: "/user/:username/edit",
				  templateUrl: 'partials/editUser.html',
				  controller: controllers.EditUserCtrl				
			  })
			  .state('editGame', {
			  	  url: "/game/:gamenr/edit",
				  templateUrl: 'partials/editGame.html',
				  controller: controllers.EditGameCtrl
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
