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
      controllerAs: 'vm',
      transclude: true
    };

    return directive;

     /** @ngInject */
    function MenuViewController($log, $scope, betterSettings , userService, $state, $mdMedia, $stateParams, $mdSidenav) {
        var vm = this;
        
        vm.large = $mdMedia('gt-xs');
         
        vm.openLeftMenu = function() {
           $mdSidenav('left').toggle();
        }; 
        
    };
  }

})();