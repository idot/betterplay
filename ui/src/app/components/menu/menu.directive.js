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
    function MenuViewController($log, $scope, betterSettings , userService, $state, $stateParams) {
        var vm = this;
        vm.userService = userService;
        var DF = betterSettings.DF;
        
        vm.specialDisabled = function(){
            return $state.includes("**.specialBets.**")  &&  $stateParams.username !== undefined &&  $stateParams.username == vm.userService.loggedInUser.username;  
        };
        vm.gamesDisabled = function(){
            return $state.includes("games");  
        };
        vm.betsDisabled = function(){
            return $state.includes("user.userBets") &&  $stateParams.username !== undefined &&  $stateParams.username == vm.userService.loggedInUser.username;  
        };
        vm.usersDisabled = function(){
            return $state.includes("users");  
        };
        vm.registerDisabled = function(){
            return $state.includes("admin.registerUser");  
        };       
        vm.statsDisabled = function(){
            return $state.includes("statistics.plots");  
        };       
        vm.loginDisabled = function(){
            return $state.includes("login");  
        };
               
    };
  }

})();