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


controllers.UserCtrl = function($log, $scope, $filter, Restangular, $stateParams, ngTableParams ) {
	$scope.stateParams = $stateParams;
	var queryUser = Restangular.one('api/user', $scope.stateParams.username);
   
    queryUser.get().then(function(userWithSpAndGB){
		$scope.user = userWithSpAndGB.user;
		$scope.special = userWithSpAndGB.special;
		$scope.gameBets = userWithSpAndGB.gameBets;
		setupTable( $scope.gameBets, ngTableParams, { 'game.game.nr': 'asc'}, $scope, $filter );
    });
}
controllers.UserCtrl.$inject = ['$log', '$scope', '$filter', 'Restangular', '$stateParams', 'ngTableParams'];

controllers.GameCtrl = function($log, $scope, $filter, Restangular, $stateParams, ngTableParams ) {
	$scope.stateParams = $stateParams;
	var queryGame = Restangular.one('api/game', $scope.stateParams.gamenr);
    
	queryGame.get().then(function(gwtWithBetsPerUser){
		$scope.gwt = gwtWithBetsPerUser.game;
		$scope.betsUsers = gwtWithBetsPerUser.betsUsers;
		setupTable( $scope.betsUsers, ngTableParams, { 'game.game.nr': 'asc'}, $scope, $filter );
	});

}
controllers.GameCtrl.$inject = ['$log', '$scope', '$filter', 'Restangular', '$stateParams', 'ngTableParams'];


controllers.SettingsCtrl = function($log, $scope, $rootScope, $stateParams){
  $scope.stateParams = $stateParams;	
	
  $scope.opened = false;	
	
  $scope.DF = $rootScope.DF;
  
  $scope.setFormat = function(){
	  $rootScope.DF = $scope.DF;
  };
	
  $scope.setDate = function(newDate){
      $rootScope.TIMEFROMSERVER = false;
	  var nm = moment(newDate);
      var om = currentTime;
	  nm.hours(om.hours());
	  nm.minutes(om.minutes());
	  nm.seconds(om.seconds());
	  $rootScope.currentTime = new Date(nm.getTime());
  };
  
  $scope.GLOBALDATE = new Date($rootScope.currentTime.getTime());

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
controllers.SettingsCtrl.$inject = ['$log', '$scope','$rootScope','$stateParams'];



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
			$scope.username = userWithEmail.username;
			$scope.firstName = userWithEmail.firstName;
			$scope.lastName = userWithEmail.lastName;
			$scope.email = userWithEmail.email;
			$scope.icontype = userWithEmail.icontype;
			$scope.iconurl = userWithEmail.iconurl;
			$rootScope.updateLogin(userWithEmail);
		});
    }
    	
	$scope.password1 = "";
    $scope.password2 = "";
		
	$scope.updatePassword = function(){
		    $log.debug('submitting new password: '+$scope.password1);
		    var pu = { 'password': $scope.password1 };
	    	Restangular.all('api/user/'+$scope.username+'/password').customPOST( pu ).then(
			function(success){
				toaster.pop('success', "changed password for "+$scope.username+"\n"+success);
				scope.password1 = "";
				scope.password2 = "";
			}
		);
	};
	
	
	$scope.updateDetails = function(){
	       $log.debug('updating details: ');
		   var u = { firstName: $scope.firstName, lastName: $scope.lastName, email: $scope.email, icontype: $scope.icontype };	
    	   Restangular.all('api/user/'+$scope.username+'/details').customPOST( u ).then(
		     function(success){
			    toaster.pop('success', "updated user details for"+$scope.username+"\n"+success);
                $scope.refreshUser();
		     }
		  );
	};
	
	$scope.refreshUser();
}
controllers.EditUserCtrl.$inject = ['$log', '$scope', '$rootScope', '$stateParams', 'Restangular', '$state', 'toaster'];




return controllers;

});





