(function() {
        'use strict';


        var fGame = function(item, filterGame, betterSettings){
             switch(filterGame) {
                case "open": return ! betterSettings.betClosed(item.game.serverStart);
                case "closed":  return betterSettings.betClosed(item.game.serverStart);
                default: return true; 
            }
        };

        var fBet = function(item, filterBet){
            switch(filterBet) {
                case "set": return item.bet.result.isSet;
                case "not set": return ! item.bet.result.isSet;
                default: return true; 
            }
        };
       
        var fLevel = function(item, filterLevel){
            if(filterLevel == "all"){
                return true;
            }
            return item.game.level.level == filterLevel;
        };

        var filterGBL = function(item, betterSettings, userService){
            return fGame(item, userService.filter.game, betterSettings) && fBet(item, userService.filter.bet) && fLevel(item, userService.filter.level);  
        };

        angular
        .module('ui')
        .filter('gbl', function($log, _, userService, betterSettings){
            return function(items){
                 return _.filter(items, function(item){
                    return filterGBL(item, betterSettings, userService);
                });   
            };
        })
})();
