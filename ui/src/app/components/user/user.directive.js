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
           user: '=user'
      },
      controller: UserViewController,
      controllerAs: 'vm'
    };

    return directive;

     /** @ngInject */
    function UserViewController($log, Restangular, toastr, betterSettings, userService, $scope, _) {
        //var vm = this;
        //vm.user = $scope.user;
        //vm.iconUrl =  $scope.user.iconurl + '&s=80';
      
        
    };
  }

})();