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
          start: '=start',
          gameResult: '=gameResult'
      },
      controller: BetViewController,
      controllerAs: 'vm'
    };

    return directive;

     /** @ngInject */
    function BetViewController($log, Restangular, toastr, betterSettings, $scope, _) {
        var vm = this;
        vm.disabled = false;
        vm.disableSave = true;
        vm.saveStyle = {};
        vm.points = _.range(15);
        vm.withTime = betterSettings.currentTime;
        vm.canBet = betterSettings.canBet($scope.start, $scope.bet);
                
        vm.cloneBet = function(bet){
            var cloned = _.clone($scope.bet);
            cloned.result = _.clone($scope.bet.result);
            return cloned;
        }         
                
        //we set a result if there is none available because its hidden from the user; otherwise the user owns the bet 
        //so: 
        // we set strings if the result is not set yet to force errors on 1/2 submissions
        // we set other symbols
        function transformBet(bet){
            bet.marked = false;
            if(bet.result === undefined){
                bet.result = { 'goalsTeam1' :  "hidden", 'goalsTeam2' : "hidden", 'isSet' : false, 'points' : 'NA' };   
            } else {
                if(! bet.result.isSet){
                    if(vm.canBet){
                       bet.result.goalsTeam1 = "-";
                       bet.result.goalsTeam2 = "-";
                    } else {    
                       bet.result.goalsTeam1 = "closed!";
                       bet.result.goalsTeam2 = "closed!";
                    }
               }
            } 
        }
        
        
        transformBet($scope.bet);  
        vm.originalBet = vm.cloneBet($scope.bet);
        
        function checkSubmission(bet){
            var error = [];
            if(! _.isNumber(bet.result.goalsTeam1)){
                error.push("team1");
            } 
            if(! _.isNumber(bet.result.goalsTeam2)){
                error.push("team2");
            }    
            var errors = error.join()
            if(errors.length > 0){
               return "Please set "+errors+"!";   
            } else { return errors };            
        };
        
        vm.saveBet = function(bet) {
            var error = checkSubmission(bet);
            if(error.length > 0){
                  toastr.error(error);
                  return;                   
            }
            vm.disableSave = true;
            vm.disabled = true;
            vm.saveStyle = vm.saveStyleValue(bet);

            Restangular.all('em2016/api/bet/' + bet.id).customPOST(bet).then(
                function(success) {
                    var game = success.game;
                    var betold = success.betold;
                    var betnew = success.betnew;
                    var flag = function(team){
                        return "<span class='flag-icon flag-icon-"+team.short2+" flag-icon-squared'></span>";  
                    };
                    var g = flag(game.team1)+" : "+flag(game.team2)
                    var res = betterSettings.prettyResult(betold.result) + " -> " + betterSettings.prettyResult(betnew.result); 
                    toastr.success("<span>"+g+"     "+res+"</span>", "updated bet");
                    
                    vm.originalBet = betnew;
                    bet = betnew;
                    bet.marked = false;
                    vm.disabled = false;
                    vm.saveStyle = vm.saveStyleValue(bet);
                },
                function(error) { 
                    error.cancelGeneralHandler(); 
                    bet.result = _.clone(vm.originalBet.result);
                    bet.marked = false;
                    vm.disabled = false;
                    toastr.error(error.data.error.join(), "reverting bet to original");
                    vm.saveStyle = vm.saveStyleValue(bet);
                }
            );
        };
                
        vm.saveStyleValue = function(bet) {
            if(vm.disabled){
               return  { 'fill' : 'white' };
            }
            if(bet.viewable){
                if(bet.marked ){
                    if(checkSubmission(bet).length == 0) {
                        return { 'fill' : 'green' };
                    } else {
                        return { 'fill': 'yellow' };                        
                    }
                } else if(! bet.result.isSet){
                    return { 'fill': 'red' };
                } 
            }     
        };
          
     
        vm.disableSaveValue = function(bet) {
             if(checkSubmission(bet).length == 0){
                 return false;  
             } else {
                 return true;
             }
        };      
          
        vm.markBet = function(bet) {
            bet.marked = true;
            vm.disableSave = vm.disableSaveValue(bet);
            vm.saveStyle = vm.saveStyleValue(bet);
        };
    
        vm.saveButton = function(bet) {
            if(! bet.marked ) {
                return "";
            } else {
                return "md-raised";
            }
        };
   
        vm.saveStyle = vm.saveStyleValue(vm.originalBet);
        
        
   
    }  
     
  }
 
})();

