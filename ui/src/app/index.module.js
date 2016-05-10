(function() {
    'use strict';

    angular
    .module('ui', ['ngAnimate', 'ngCookies', 'ngSanitize', 'ngMessages', 'ngAria', 'restangular', 'ui.router', 'ngMaterial', 'toastr', 'ngMdIcons'])
    .config(function($mdThemingProvider) {
        // Configure a dark theme with primary foreground yellow
        $mdThemingProvider.theme('docs-dark', 'default')
        .primaryPalette('indigo');
    //      .dark();
      })


})();
