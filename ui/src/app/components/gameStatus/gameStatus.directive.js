(function() {
  'use strict';

  angular
    .module('ui')
    .directive('betterGameStatus', betterGameStatus);

  /** @ngInject */
  function betterGameStatus() {
    var directive = {
      restrict: 'E',
      templateUrl: 'app/components/gameStatus/gameStatus.html',
      scope: {
          game: '=game'
	//	    onSend: '&',      // Pass a reference to the method 
      },
      controller: GameStatusController,
      controllerAs: 'vm',
      bindToController: true
    };

    return directive;

     /** @ngInject */
    function GameStatusController($log, betterSettings) {
        var vm = this;
        vm.disabled = false;
        
        
        vm.markBet = function(bet) {
            bet['marked'] = true;
        };

        vm.withTime = betterSettings.currentTime;

        vm.saveButton = function(bet) {
            if (typeof bet.marked === "undefined" || bet.marked == false) {
                return "btn btn-default btn-xs";
            } else {
                return "btn btn-warning btn-xs";
            }
        };

        vm.prettyBet = function(bet) {
            if (bet.result.isSet) {
                return bet.result.goalsTeam1 + ":" + bet.result.goalsTeam2;
            } else {
                return "-:-"
            }
        }
    } 
  }

})();

