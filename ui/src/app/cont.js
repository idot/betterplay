(function() {
    'use strict';

    angular
        .module('ui')
        .controller('XController', XController)
    ;

    /** @ngInject */
    function XController($log) {
        var vm = this;

        $log.error("created XController!!!!!!!!!!!!!!!!!!!!!!!!!");
        vm.allUsers = "xy";
    

    }
    
})();