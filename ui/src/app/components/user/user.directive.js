(function() {
  'use strict';

  angular
    .module('ui')
    .directive('betterUserView', betterUserView);

  /** @ngInject */
  function betterUserView() {
    var directive = {
      restrict: 'E',
      templateUrl: 'app/components/user/user.component.html',
      scope: {
           user: '=user',
           full: '=full'
      },
      controller: UserViewController,
      controllerAs: 'vm'
    };

    return directive;

     /** @ngInject */
    function UserViewController($log, Restangular, toastr, betterSettings, userService, $scope, _) {
        var vm = this;
        vm.badgecolor = function(){
            return betterSettings.badgecolour($scope.user.rank);
        };
    };
  }

})();