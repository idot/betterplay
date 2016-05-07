
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
        vm.game = $scope.gwt.game;
        vm.disabled = false;
        vm.DF = betterSettings.DF;
        vm.timeLeft = betterSettings.timeLeft;
        vm.gameClosed = betterSettings.betClosed(vm.game.serverStart);
        vm.resultSet = vm.game.result.isSet;
        vm.prettyResult = betterSettings.prettyResult(vm.game.result);
        vm.expand = $mdMedia('gt-xs') || $scope.fullcontent;
        vm.isAdmin = userService.isAdmin()
    } 
  }

})();

