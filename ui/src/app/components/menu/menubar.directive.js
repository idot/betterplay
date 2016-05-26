(function() {
  'use strict';

  angular
    .module('ui')
    .directive('betterMenuBar', betterMenuBar);

  /** @ngInject */
  function betterMenuBar() {
    var directive = {
      restrict: 'E',
      templateUrl: 'app/components/menu/menubar.html',
      scope: {
      },
      controller: MenuBarController,
      controllerAs: 'vm'
    };

    return directive;

     /** @ngInject */
    function MenuBarController($log, $scope, betterSettings , userService, $state, $mdMedia, $stateParams) {
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
          
        vm.large = $mdMedia('gt-xs');
        
      
               
    };
  }

})();