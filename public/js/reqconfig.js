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
