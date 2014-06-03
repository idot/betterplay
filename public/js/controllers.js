/*global define */

'use strict';

define(function() {

/* Controllers */

var controllers = {};

controllers.UsersCtrl = function($log, $scope, $filter, Restangular, $stateParams, ngTableParams ) {
	var queryUsers = Restangular.all('api/users');
   
	queryUsers.getList().then(function(users){
	    $scope.allUsers = users;
		setupTable( users, ngTableParams, { 'username': 'asc'}, $scope, $filter );
	});
}

controllers.UsersCtrl.$inject = ['$log', '$scope', '$filter', 'Restangular', '$stateParams', 'ngTableParams' ];

controllers.GamesCtrl = function($log, $scope, $filter, Restangular, $stateParams, ngTableParams ) {
	var queryGames = Restangular.all('api/games');
		
	queryGames.getList().then(function(games){
	    $scope.allGames = games;
		setupTable( games, ngTableParams, { 'game.nr': 'asc'}, $scope, $filter );
    });
	
   
}
controllers.GamesCtrl.$inject = ['$log', '$scope', '$filter', 'Restangular', '$stateParams', 'ngTableParams'];


controllers.UserCtrl = function($log, $scope, $filter, Restangular, $stateParams, ngTableParams, toaster ) {
	$scope.stateParams = $stateParams;
	var queryUser = Restangular.one('api/user', $scope.stateParams.username);
   
    queryUser.get().then(function(userWithSpAndGB){
		$scope.user = userWithSpAndGB.user;
		$scope.special = userWithSpAndGB.special;
		$scope.gameBets = userWithSpAndGB.gameBets;
		setupTable( $scope.gameBets, ngTableParams, { 'game.game.nr': 'asc'}, $scope, $filter );
    });
	
		/** duplicated with GameCtr **/
	$scope.saveBet = function(bet){
	    var queryBet = Restangular.all('api/bet/'+bet.id).customPOST(bet).then(
		    function(success){
				var game = success.game;
				var betold = success.betold;
				var betnew = success.betnew;
				var show = success.game.game.nr+": "+$scope.prettyBet(betold)+" -> "+$scope.prettyBet(betnew)
				toaster.pop('success', "updated bet ", show);
				bet['marked'] = false;
		    }		
		);			
	};
	
	$scope.markBet = function(bet){
	    bet['marked'] = true;	
	};
	
	$scope.saveButton = function(bet){
		if(typeof bet.marked === "undefined" || bet.marked == false){
			return "btn btn-default btn-xs";
		}else{
			return "btn btn-warning btn-xs";
		}
	};
	
		/** duplicated with GameCtr **/
	$scope.prettyBet = function(bet){
		if(bet.result.isSet){
			return bet.result.goalsTeam1+":"+bet.result.goalsTeam2;
		}else{
			return "-:-"
		}
	}
	
}
controllers.UserCtrl.$inject = ['$log', '$scope', '$filter', 'Restangular', '$stateParams', 'ngTableParams', 'toaster'];

controllers.GameCtrl = function($log, $scope, $filter, Restangular, $stateParams, ngTableParams ) {
	$scope.stateParams = $stateParams;
	var queryGame = Restangular.one('api/game', $scope.stateParams.gamenr);
    
	queryGame.get().then(function(gwtWithBetsPerUser){
		$scope.gwt = gwtWithBetsPerUser.game;
		$scope.betsUsers = gwtWithBetsPerUser.betsUsers;
		setupTable( $scope.betsUsers, ngTableParams, { 'game.game.nr': 'asc'}, $scope, $filter );
	});
	
	/** duplicated with UserCtr **/
	$scope.updateBet = function(bet){
	    var queryBet = Restangular.all('api/bet/'+bet.id).customPOST(bet).then(
		    function(success){
	//	        toaster.pop('success', "updated bet "+success);
		    }		
		);			
	};
	
    /** duplicated with UserCtr **/
	$scope.prettyBet = function(bet){
		if(bet.result.isSet){
			return bet.result.goalsTeam1+":"+bet.result.goalsTeam2;
		}else{
			return "-:-"
		}
	}


}
controllers.GameCtrl.$inject = ['$log', '$scope', '$filter', 'Restangular', '$stateParams', 'ngTableParams'];


controllers.SettingsCtrl = function($log, $scope, $rootScope, $stateParams, Restangular, toaster){
  $scope.stateParams = $stateParams;	
	
  $scope.opened = false;	
		
  $scope.DF = $rootScope.DF;
  
  $scope.setFormat = function(){
	  $rootScope.DF = $scope.DF;
  };
  
  $scope.updateTimeOnServer = function(time){
  	Restangular.all('api/time').customPOST( { serverTime: time.getTime() } ).then(
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
      Restangular.all('api/time/reset').customPOST().then(
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
	    Restangular.all("login").post(credentials).then(
	      function(auth){ 
			  $rootScope.updateLogin(auth.user, auth["AUTH-TOKEN"]);
			  $state.transitionTo("users");
	      }
	    );
    };

}
controllers.LoginCtrl.$inject = ['$log', '$scope', '$rootScope', '$stateParams', 'Restangular', '$state'];

controllers.RegisterUserCtrl = function($log, $scope, $rootScope, $stateParams, Restangular, $state, toaster){
	$scope.stateParams = $stateParams;	
	
	var queryUsers = Restangular.all('api/users');	
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
	    	Restangular.all('api/user/'+$scope.username).customPUT( pu ).then(
			function(success){
				toaster.pop('success', "registered "+$scope.username, "e-mail is on its way to "+$scope.email);
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
	    var queryUser = Restangular.one('api/userWithEmail');
        queryUser.get().then(function(userWithEmail){
			$scope.formUser = userWithEmail;
			$rootScope.updateLogin(userWithEmail);
		});
    }
    	
	$scope.pass = { word1: "", word2: ""};
		
	$scope.updatePassword = function(){
		    $log.debug('submitting new password: '+$scope.pass.word1);
		    var pu = { 'password': $scope.pass.word1 };
	    	Restangular.all('api/user/'+$scope.formUser.username+'/password').customPOST( pu ).then(
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
    	   Restangular.all('api/user/'+$scope.formUser.username+'/details').customPOST( u ).then(
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
	var queryGame = Restangular.one('api/game', $scope.stateParams.gamenr);
    
	queryGame.get().then(function(gwtWithBetsPerUser){
		$scope.gwt = gwtWithBetsPerUser.game;
	});
	

	$scope.submitResult = function(){
	    var queryBet = Restangular.all('api/game/results').customPOST($scope.gwt.game).then(
		    function(success){
		        toaster.pop('success', "updated game", success);
		    }		
		);			
	};
}
controllers.EditGameCtrl.$inject = ['$log', '$scope', '$rootScope', '$stateParams', 'Restangular', '$state', 'toaster'];



controllers.BetResultCtrl = function($scope){
	$scope.value = "myscopevalue";
	
	
}
controllers.BetResultCtrl.$inject = ['$scope'];

return controllers;

});





