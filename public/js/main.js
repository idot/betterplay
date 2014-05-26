/*global require, requirejs */

'use strict';

/**
* dependencies are declared manually based on huntc comment:
* https://github.com/huntc/angular-seed-play-1/commit/1c4dad20cd881c6045592aa4f055663b724c19ae#commitcomment-6434953
**/

requirejs.config({
  paths: {
    'angular': ['../lib/angularjs/angular'],
 //   'angular-route': ['../lib/angularjs/angular-route'],
    'restangular': ['../lib/restangular/restangular'],
    'underscore': ['../lib/underscorejs/underscore'],
    'angular-ui': ['../lib/angular-ui/angular-ui'],
    'angular-ui-bootstrap': ['../lib/angular-ui-bootstrap/ui-bootstrap'],
    'angular-ui-router': ['../lib/angular-ui-router/angular-ui-router']
  },
  shim: {
    'angular': {
      exports : 'angular'
    },
//    'angular-route': {
 //     deps: ['angular']
 //   },
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
    }
  }
});

require(['angular', './controllers', './directives', './filters', './services', 'underscore', 'restangular','angular-ui','angular-ui-bootstrap','angular-ui-router'],
  function(angular, controllers) {
   //  alert("init module");
   
 
   angular.module('myApp', ['myApp.filters', 'myApp.services', 'myApp.directives', 'restangular', 'ui.router']).
      config(function($stateProvider, $urlRouterProvider){
      
     $urlRouterProvider.otherwise("/users");

	  $stateProvider
		.state('users', {
		  url: "/users",
		  templateUrl: "partials/users.html",
		  controller: controllers.UsersCtrl
		})
		.state('games', {
		  url: "/games",
		  templateUrl: "partials/games.html",
		  controller: controllers.GamesCtrl
		})  
		.state('user', {
		  url: '/user/:username',
		  templateUrl: "partials/user.html",
		  controller: controllers.UserCtrl
		})
    });
      

    angular.bootstrap(document, ['myApp']);


});
