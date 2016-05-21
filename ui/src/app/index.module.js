(function() {
    'use strict';

    angular
    .module('ui', ['ngAnimate', 'ngCookies', 'ngSanitize', 'ngMessages', 'ngAria', 'restangular', 'ui.router', 'ngMaterial', 'toastr', 'ngMdIcons', 'ngMaterialDatePicker', 'vcRecaptcha', 'md.data.table', 'ngFileSaver', 'nvd3'])
    .config(function($mdThemingProvider) {
        // Configure a dark theme with primary foreground yellow
        $mdThemingProvider.theme('docs-dark', 'default')
        .primaryPalette('indigo');
    //      .dark();
      })


})();
