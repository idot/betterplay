(function() {
  'use strict';

  angular
    .module('ui')
    .directive('betterMenuView', betterMenuView);

  /** @ngInject */
  function betterMenuView() {
    var directive = {
      restrict: 'E',
      templateUrl: 'app/components/menu/menu.html',
      scope: {
      },
      controller: MenuViewController,
      controllerAs: 'vm'
    };

    return directive;

     /** @ngInject */
    function MenuViewController($log, $scope, betterSettings , userService) {
        var vm = this;
        vm.userService = userService;
        var DF = betterSettings.DF;
      
               
    };
  }

})();