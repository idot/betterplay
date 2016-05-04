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
          game: '=game'
	//	    onSend: '&',      // Pass a reference to the method 
      },
      controller: GameViewController,
      controllerAs: 'vm'
    };

    return directive;

     /** @ngInject */
    function GameViewController($log, Restangular, toastr, betterSettings, $scope, _) {
        var vm = this;
        vm.game = $scope.game.game;
        vm.disabled = false;
        vm.DF = betterSettings.DF;
        vm.timeLeft = betterSettings.timeLeft;
        vm.gameClosed = betterSettings.betClosed(vm.game.serverStart);
        vm.resultSet = vm.game.result.isSet;
        vm.prettyResult = betterSettings.prettyResult(vm.game.result);
        
        
    } 
  }

})();

