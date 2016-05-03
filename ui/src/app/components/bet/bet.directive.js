(function() {
  'use strict';

  angular
    .module('ui')
    .directive('betterBetView', betterBetView);

  /** @ngInject */
  function betterBetView() {
    var directive = {
      restrict: 'E',
      templateUrl: 'app/components/bet/bet.html',
      scope: {
          bet: '=bet', 
	//	    onSend: '&',      // Pass a reference to the method 
          start: '=start'   
      },
      controller: BetController,
      controllerAs: 'vm'
    };

    return directive;

     /** @ngInject */
    function BetController($log, Restangular, toastr, betterSettings) {
        var vm = this;
        vm.disabled = false;
        
        vm.saveBet = function(bet) {
            vm.disabled = true;
            Restangular.all('em2016/api/bet/' + bet.id).customPOST(bet).then(
                function(success) {
                    var game = success.game;
                    var betold = success.betold;
                    var betnew = success.betnew;
                    var show = game.game.nr + ": " + vm.prettyBet(betold) + " -> " + vm.prettyBet(betnew)
                    toastr.info('success', "updated bet ", show);
                    bet['marked'] = false;
                    vm.disabled = false;
                }
            );
        };
        
        vm.canBet = function(start, bet) {
              return betterSettings.canBet(start, bet);
        };

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

