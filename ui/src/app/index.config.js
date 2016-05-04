(function() {
    'use strict';

    angular
        .module('ui')
        .config(config);

    /** @ngInject */
    function config($logProvider, toastrConfig, $mdIconProvider) {
        // Enable log
        $logProvider.debugEnabled(true);

        // Set options third-party lib
        toastrConfig.allowHtml = true;
        toastrConfig.timeOut = 1200;
        toastrConfig.positionClass = 'toast-top-right';
        toastrConfig.preventDuplicates = false;
        toastrConfig.progressBar = true;
        
        
        $mdIconProvider.iconSet('content', 'bower_components/material-design-icons/sprites/svg-sprite/svg-sprite-content-symbol.svg', 10)
        
    }

})();