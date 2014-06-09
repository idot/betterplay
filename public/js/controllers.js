/*global define */

'use strict';

define(function() {

/* Controllers */

var controllers = {};

controllers.UsersCtrl = function($log, $scope, $filter, Restangular, $stateParams, ngTableParams ) {
	var queryUsers = Restangular.all('wm2014/api/users');
   
    
   
	queryUsers.getList().then(function(users){
	    $scope.allUsers = users;
		setupTable( users, ngTableParams, { 'username': 'asc'}, $scope, $filter );
	});
}

controllers.UsersCtrl.$inject = ['$log', '$scope', '$filter', 'Restangular', '$stateParams', 'ngTableParams' ];

controllers.GamesCtrl = function($log, $scope, $rootScope, $filter, Restangular, $stateParams, ngTableParams, selectFilter ) {
	var queryGames = Restangular.all('wm2014/api/games');
	
	$scope.openFilter = selectFilter.from(['open','closed']);
		
	$scope.levelFilter = selectFilter.from(['group','last16','quarter','semi','final','third']);	
		
	var transformGame = function(game){
	   game.gamenr = game.game.nr;
	   game.team1name = game.team1.name;
	   game.team2name = game.team2.name;	
	   game.openGame = $rootScope.betClosed(game.game.serverStart, $rootScope.currentTime) ? "closed" : "open" ;
	   game.levelname = game.level.name;
	   game.serverStart = game.game.serverStart;
	};
		
	queryGames.getList().then(function(games){
	    var allGames = games;
		$scope.allGames = _.each(allGames, function(g){ transformGame(g)});		
		setupTable( $scope.allGames, ngTableParams, { 'game.nr': 'asc'}, $scope, $filter );
    });
	  
}
controllers.GamesCtrl.$inject = ['$log', '$scope', '$rootScope', '$filter', 'Restangular', '$stateParams', 'ngTableParams', 'selectFilter'];


controllers.UserCtrl = function($log, $scope, $rootScope, $filter, Restangular, $stateParams, ngTableParams, toaster, selectFilter ) {
	$scope.stateParams = $stateParams;
	var queryUser = Restangular.one('wm2014/api/user', $scope.stateParams.username);
   
	$scope.openFilter = selectFilter.from(['open','closed']);
		
	$scope.levelFilter = selectFilter.from(['group','last16','quarter','semi','final','third']);	
		
	$scope.bettedFilter = selectFilter.from(['set','not']);	
   
    var transformGameBets = function(gb){
		gb.team1name = gb.game.team1.name;
		gb.team2name = gb.game.team2.name;
		gb.levelname = gb.game.level.name;
		gb.openGame = $rootScope.betClosed(gb.game.game.serverStart, $rootScope.currentTime) ? "closed" : "open";
		gb.betset = gb.bet.result.isSet ? "set" : "not";
		gb.serverStart = gb.game.game.serverStart;
		gb.gamenr = gb.game.game.nr;
    };
   
    queryUser.get().then(function(userWithSpAndGB){
		$scope.user = userWithSpAndGB.user;
		$scope.special = userWithSpAndGB.special;
		var gameBets = userWithSpAndGB.gameBets;
		$scope.gameBets = _.each(gameBets, function(gb){ transformGameBets(gb) });
		
		setupTable( $scope.gameBets, ngTableParams, { 'game.game.nr': 'asc'}, $scope, $filter );
    });
}
controllers.UserCtrl.$inject = ['$log', '$scope', '$rootScope', '$filter', 'Restangular', '$stateParams', 'ngTableParams', 'toaster', 'selectFilter'];



controllers.GameCtrl = function($log, $scope, $filter, Restangular, $stateParams, ngTableParams, selectFilter) {
	$scope.stateParams = $stateParams;
	var queryGame = Restangular.one('wm2014/api/game', $scope.stateParams.gamenr);
    	
	var transformBetsUser = function(bu){
	    bu.username = bu.user.username;	
	};
		
	queryGame.get().then(function(gwtWithBetsPerUser){
		$scope.gwt = gwtWithBetsPerUser.game;
		var betsUsers = gwtWithBetsPerUser.betsUsers;
		$scope.betsUsers = _.each(betsUsers, function(bu){ bu.username = bu.user.username; });
		setupTable( $scope.betsUsers, ngTableParams, { 'game.game.nr': 'asc'}, $scope, $filter );
	});

}
controllers.GameCtrl.$inject = ['$log', '$scope', '$filter', 'Restangular', '$stateParams', 'ngTableParams', 'selectFilter'];


controllers.SettingsCtrl = function($log, $scope, $rootScope, $stateParams, Restangular, toaster){
  $scope.stateParams = $stateParams;	
	
  $scope.opened = false;	
		
  $scope.DF = $rootScope.DF;
  
  $scope.setFormat = function(){
	  $rootScope.DF = $scope.DF;
  };
  
  $scope.updateTimeOnServer = function(time){
  	Restangular.all('wm2014/api/time').customPOST( { serverTime: time.getTime() } ).then(
	   function(success){
		    toaster.pop('success', "changed time", success);
	   })
   };	
  
	
  $scope.updateDate = function(){
      $rootScope.TIMEFROMSERVER = false;
	  var nm = moment($scope.global.date);
      var om = moment($rootScope.currentTime);
	  nm.hours(om.hours());
	  nm.minutes(om.minutes());
	  nm.seconds(om.seconds());
	  $rootScope.currentTime = nm.toDate();
	  $scope.updateTimeOnServer($rootScope.currentTime);
  };
  
  $scope.updateTime = function(){
      $rootScope.TIMEFROMSERVER = false;
	  var nm = moment($scope.global.time);
      var om = moment($rootScope.currentTime);
	  om.hours(nm.hours());
	  om.minutes(nm.minutes());
	  $rootScope.currentTime = om.toDate();
	  $scope.updateTimeOnServer($rootScope.currentTime);
  };
  
  
  $scope.resetTime = function(){
      Restangular.all('wm2014/api/time/reset').customPOST().then(
  	   function(success){
	 	    $rootScope.TIMEFROMSERVER = true;
	 	    $rootScope.updateTimeFromServer()
  		    toaster.pop('success', "reset time", success);
  	   })
  };
    
  
  $scope.global = { date : new Date($rootScope.currentTime.getTime()), time: new Date($rootScope.currentTime.getTime())};

  $scope.open = function($event){
	    $event.preventDefault();
	    $event.stopPropagation();
	    $scope.opened = true;
  };

  $scope.updateInterval = $rootScope.UPDATEINTERVAL;

  $scope.setUpdateinterval = function(){
	  $rootScope.UPDATEINTERVAL = $scope.updateInterval;
  };

}
controllers.SettingsCtrl.$inject = ['$log', '$scope','$rootScope','$stateParams', 'Restangular', 'toaster'];



controllers.LoginCtrl = function($log, $scope, $rootScope, $stateParams, Restangular, $state){
	$scope.stateParams = $stateParams;	
		
	$scope.username = "";
	$scope.password = "";
	
	$scope.login = function(){
		var credentials = { 'username': $scope.username, 'password': $scope.password };
	    Restangular.all("wm2014/login").post(credentials).then(
	      function(auth){ 
			  $rootScope.updateLogin(auth.user, auth["AUTH-TOKEN"]);	
			  if(auth.user.hadInstructions){  
			     $state.transitionTo("user.userBets", { username: $scope.username });
			  } else {
				 $state.transitionTo("user.specialBets", { username: $scope.username });
			  } 
	      }
	    );
    };

}
controllers.LoginCtrl.$inject = ['$log', '$scope', '$rootScope', '$stateParams', 'Restangular', '$state'];

controllers.RegisterUserCtrl = function($log, $scope, $rootScope, $stateParams, Restangular, $state, toaster){
	$scope.stateParams = $stateParams;	
	
	var queryUsers = Restangular.all('wm2014/api/users');	
	queryUsers.getList().then(function(users){
	    $scope.allUsers = _.map(users, function(u){ return u.username });
	});	
		
	$scope.setFields = function(){	
	    $scope.username = "";
	    $scope.password1 = "";
		$scope.password2 = "";
    	$scope.email = "";
    };
	
	$scope.uniqueUsername = function(username){
	   	var duplicated = _.find($scope.allUsers, function(u){ return u === username; });
		$log.debug("unique: "+username+" "+duplicated);
	    return !duplicated;			
	};
	
	$scope.signon = function(){
		    var pu = { 'username': $scope.username, 'password': $scope.password1, 'email': $scope.email };
	    	Restangular.all('wm2014/api/user/'+$scope.username).customPUT( pu ).then(
			function(success){
				toaster.pop('success', "registered "+$scope.username); //TODO: email
				$scope.setFields();
			}
		);
	};
	$scope.setFields();
}
controllers.RegisterUserCtrl.$inject = ['$log', '$scope', '$rootScope', '$stateParams', 'Restangular', '$state', 'toaster'];

controllers.EditUserCtrl = function($log, $scope, $rootScope, $stateParams, Restangular, $state, toaster){
	$scope.stateParams = $stateParams;
		
	
	$scope.refreshUser = function(){
	    var queryUser = Restangular.one('wm2014/api/userWithEmail');
        queryUser.get().then(function(userWithEmail){
			$scope.formUser = userWithEmail;
			$rootScope.updateLogin(userWithEmail);
		});
    }
    	
	$scope.pass = { word1: "", word2: ""};
		
	$scope.updatePassword = function(){
		    $log.debug('submitting new password: '+$scope.pass.word1);
		    var pu = { 'password': $scope.pass.word1 };
	    	Restangular.all('wm2014/api/user/'+$scope.formUser.username+'/password').customPOST( pu ).then(
			function(success){
				toaster.pop('success', "changed password");
				$scope.pass.word1 = "";
				$scope.pass.word2 = "";
			}
		);
	};
	
	
	$scope.updateDetails = function(){
	       $log.debug('updating details: ');
		   var u = { firstName: $scope.formUser.firstName, lastName: $scope.formUser.lastName, email: $scope.formUser.email, icontype: $scope.formUser.icontype };	
    	   Restangular.all('wm2014/api/user/'+$scope.formUser.username+'/details').customPOST( u ).then(
		     function(success){
			    toaster.pop('success', "updated user details");
                $scope.refreshUser();
		     }
		  );
	};
	
	$scope.refreshUser();
}
controllers.EditUserCtrl.$inject = ['$log', '$scope', '$rootScope', '$stateParams', 'Restangular', '$state', 'toaster'];

controllers.EditGameCtrl = function($log, $scope, $rootScope, $stateParams, Restangular, $state, toaster) {
    console.log("st: "+$stateParams);
	$scope.stateParams = $stateParams;
	console.log("sd: "+$scope.stateParams);
	var queryGame = Restangular.one('wm2014/api/game', $scope.stateParams.gamenr);
    
	queryGame.get().then(function(gwtWithBetsPerUser){
		$scope.gwt = gwtWithBetsPerUser.game;
	});
	

	$scope.submitResult = function(){
	    var queryBet = Restangular.all('wm2014/api/game/results').customPOST($scope.gwt.game).then(
		    function(success){
		        toaster.pop('success', "updated game", success);
		    }		
		);			
	};
}
controllers.EditGameCtrl.$inject = ['$log', '$scope', '$rootScope', '$stateParams', 'Restangular', '$state', 'toaster'];



controllers.BetCtrl = function($scope, $rootScope, Restangular, toaster){
	$scope.disabled = false;
	
	$scope.saveBet = function(bet){
		$scope.disabled = true;
	    var queryBet = Restangular.all('wm2014/api/bet/'+bet.id).customPOST(bet).then(
		    function(success){
				var game = success.game;
				var betold = success.betold;
				var betnew = success.betnew;
				var show = success.game.game.nr+": "+$scope.prettyBet(betold)+" -> "+$scope.prettyBet(betnew)
				toaster.pop('success', "updated bet ", show);
				bet['marked'] = false;
				$scope.disabled = false;
		    }		
		);			
	};

	$scope.markBet = function(bet){
	    bet['marked'] = true;	
	};

    $scope.withTime = $rootScope.currentTime;

   
	$scope.saveButton = function(bet){
		if(typeof bet.marked === "undefined" || bet.marked == false){
			return "btn btn-default btn-xs";
		}else{
			return "btn btn-warning btn-xs";
		}
	};

	$scope.prettyBet = function(bet){
		if(bet.result.isSet){
			return bet.result.goalsTeam1+":"+bet.result.goalsTeam2;
		}else{
			return "-:-"
		}
	}
	
}
controllers.BetCtrl.$inject = ['$scope','$rootScope', 'Restangular', 'toaster'];

controllers.UserSpecialBetsCtrl = function($log, $scope, $rootScope, $filter, $stateParams, Restangular, $state, toaster, ngTableParams, specialBetService ) {	
	     $scope.stateParams = $stateParams;
		 
         specialBetService.getSpecialBet($scope.betId, $scope.stateParams.username).then(function(success){	
				     // join must make to usable structure
				 $scope.user = success.user;
				 $scope.templatebets = success.templateBets;
				 if($rootScope.isOwner($scope.user.id) && ! $scope.user.hadInstructions){
					$scope.noInstructions = true;
				 	toaster.pop('info', "Welcome "+success.user.username+"!", "Please place special bets until start of the game.\n Have fun!")
				 }else{
				 	$scope.noInstructions = false;
				 }
			 }
		 );		 
		 		 
		 $scope.change = function(templatebet){
		     switch(templatebet.template.itemType){
		     	case "team": $state.transitionTo("user.specialBetsspecialTeams", { username: $scope.user.username, id: templatebet.bet.id });  break;
			    case "player": $state.transitionTo("user.specialBetsspecialPlayers", { username: $scope.user.username, id: templatebet.bet.id });  break;
			    default:  toaster.pop('error', "someting is wrong!", "could not decide if its bet for player or team. Please inform somebody by email");  
		     }
		 };
}
controllers.UserSpecialBetsCtrl.$inject = ['$log', '$scope', '$rootScope', '$filter', '$stateParams', 'Restangular', '$state', 'toaster', 'ngTableParams', 'specialBetService'];	



controllers.EditUserSpecialPlayerCtrl = function($log, $scope, $rootScope, $filter, $stateParams, Restangular, $state, toaster, ngTableParams, specialBetService ) {	
	     $scope.stateParams = $stateParams;
	     		 
		 $scope.betId = $stateParams.id;		 
		
		 specialBetService.getSpecialBet($scope.betId, $scope.stateParams.username).then(function(success){		 
				 $scope.user = success.user;
				 $scope.tb = success.templateBets;
		     }		 
		 );
		 
         Restangular.all('wm2014/api/players').getList().then(
			 function(success){
				 var forFilter = _.map(success, function(pt){ pt.name = pt.player.name; pt.tname = pt.team.name; return pt; });
				 $scope.playerWithTeams = forFilter;
				 setupTable( $scope.playerWithTeams, ngTableParams, { 'player.name': 'asc' }, $scope, $filter );
			 }
		 );		 
		 
		 $scope.selectPlayer = function(){
		 	specialBetService.saveSelected($scope.tb.bet, $scope.user, $scope.playerWithTeams);	
		 };
}
controllers.EditUserSpecialPlayerCtrl.$inject = ['$log', '$scope', '$rootScope', '$filter', '$stateParams', 'Restangular', '$state', 'toaster', 'ngTableParams','specialBetService'];	

controllers.EditUserSpecialTeamCtrl = function($log, $scope, $rootScope, $filter, $stateParams, Restangular, $state, toaster, ngTableParams, specialBetService ) {	
	     $scope.stateParams = $stateParams;
		 
		 $scope.betId = $stateParams.id;
        
		 specialBetService.getSpecialBet($scope.betId, $scope.stateParams.username).then(function(success){
			 $scope.user = success.user;
			 $scope.tb = success.templateBets;
	//		 specialBetService.specialBetStats(tb.template.id).then(
	//			 function(tb){
	//			   $scope.bets = tb.bets;	
	//		     }
	//		 );	 
		 });
		 
		 Restangular.all('wm2014/api/teams').getList().then(
			 function(success){
				 $scope.teams = success;
				 setupTable( $scope.teams, ngTableParams, { 'name': 'asc'}, $scope, $filter );
			 }
		 );		 
		 
		 $scope.selectTeam = function(){
	     	 specialBetService.saveSelected($scope.tb.bet, $scope.user, $scope.teams)
		 };
		 
		
		  
}
controllers.EditUserSpecialTeamCtrl.$inject = ['$log', '$scope', '$rootScope', '$filter', '$stateParams', 'Restangular', '$state', 'toaster', 'ngTableParams', 'specialBetService'];	


controllers.PlotSpecialBetsCtrl = function($scope, $stateParams, $state, specialBetStats, tid){
   //  $scope.stateParams = $stateParams; not applicable within a nested view
   if(typeof tid !== "undefined"){
	   $scope.templateId =  tid.templateId;
   }else{
	   //extract from parameters
   }
   
   specialBetStats.getStats($scope.templateId).then(function(tb){
         $scope.template = tb.template;
		 $scope.plotData = tb.data;  
   });
   
}
controllers.PlotSpecialBetsCtrl.$inject = ['$scope', '$stateParams', '$state', 'specialBetStats','tid'];



return controllers;

});





