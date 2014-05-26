/*global require, requirejs */

'use strict';

requirejs.config({
  paths: {
    'angular': ['../lib/angularjs/angular'],
    'angular-route': ['../lib/angularjs/angular-route'],
    'restangular': ['../lib/restangular/restangular'],
    'underscore': ['../lib/underscorejs/underscore']
  },
  shim: {
    'angular': {
      exports : 'angular'
    },
    'angular-route': {
      deps: ['angular'],
      exports : 'angular'
    },
    'restangular': {
        deps: ["underscore", "angular"]
    }
  }
});

require(['angular', './controllers', './directives', './filters', './services', 'angular-route', 'underscore', 'restangular'],
  function(angular, controllers) {

    // Declare app level module which depends on filters, and services

    angular.module('myApp', ['myApp.filters', 'myApp.services', 'myApp.directives', 'ngRoute','restangular']).
      config(['$routeProvider', function($routeProvider) {
        $routeProvider.when('/users', {templateUrl: 'partials/users.html', controller: controllers.UsersCtrl});
        $routeProvider.when('/games', {templateUrl: 'partials/games.html', controller: controllers.GamesCtrl});
        $routeProvider.when('/user/:username', {templateUrl: 'partials/user.html', controller: controllers.UserCtrl});
        $routeProvider.otherwise({redirectTo: '/users'});
      }]);

    angular.bootstrap(document, ['myApp']);

});
