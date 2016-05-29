
(function() {
  'use strict';

  angular
    .module('ui')
    .directive('betterGameView', betterGameView);

  /** @ngInject */
  function betterGameView() {
    var directive = {
      restrict: 'E',
      templateUrl: 'app/components/game/game.html',
      scope: {
          fullcontent: '=fullcontent',
          allowedit: '=allowedit',
          gwt: '=gwt'
	//	    onSend: '&',      // Pass a reference to the method 
      },
      controller: GameViewController,
      controllerAs: 'vm'
    };

    return directive;

     /** @ngInject */
    function GameViewController($log, Restangular, toastr,  $scope, _, $mdMedia, betterSettings , userService) {
        var vm = this;
        vm.gwt = $scope.gwt;
        vm.disabled = false;
        vm.DF = betterSettings.DF;
        vm.timeLeft = betterSettings.timeLeft;

        vm.gameClosed =  function(){
            if(vm.gwt.game){
                return betterSettings.betClosed(vm.gwt.game.serverStart);
            } else { return false; }
        };
        vm.resultSet  = function(){
            if(vm.gwt.game){
               return vm.gwt.game.result.isSet;
            } else { return false; }
        }
        vm.prettyResult = function(){
              if(vm.gwt.game){
                  return betterSettings.prettyResult(vm.gwt.game.result);
              } else { return "-:-" }
        }
        vm.expand = $mdMedia('gt-xs') || $scope.fullcontent;
        vm.isAdmin = userService.isAdmin()
    } 
  }

})();

