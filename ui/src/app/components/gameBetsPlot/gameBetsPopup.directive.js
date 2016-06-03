(function() {
  'use strict';

  angular
    .module('ui')
    .directive('gameBetsPopup', gameBetsPopup);

  /** @ngInject */
  function gameBetsPopup() { 
           var directive = {
                 replace: true,
		 restrict: 'E',
                 scope:  {
                     gwt: '='
                 },
                 templateUrl: "app/components/gameBetsPlot/gameBetsPopup.html",
                 controller: GameBetsPopupController,
                 controllerAs: 'vm'  
            };
             
            return directive;
            

           /** @ngInject */
           function GameBetsPopupController($log, $mdDialog, gameBetStats, $timeout, $scope, _, $window) {
               var vm = this;
               vm.gwt = $scope.gwt;
     //          $log.error("POPUP: "+vm.gwt.game.id);
               vm.plotBets = function($event){
                   var parentEl = angular.element(document.body);          
                   alert = $mdDialog.show({
                          clickOutsideToClose: true,
                          parent: parentEl,
                          targetEvent: $event,
                          title: 'Attention',
                          template:   ' <md-dialog aria-label="stats"><md-dialog-content> '+
                                            ' <game-bets-plot gwt="gwt"></game-bets-plot>'+
                                            ' </md-dialog></md-dialog-content> ',
                          ok: 'Close',
                          locals: { gwt: vm.gwt },
                         controller: function($scope, $mdDialog, gwt) {
                               $scope.closeDialog = function() {
                                    $mdDialog.hide();
                               }
                              $scope.gwt = gwt;
                         }
                        });
                
               };
               
           }
  
  }
            
})(); 