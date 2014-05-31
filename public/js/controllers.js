/*global define */

'use strict';

define(function() {

/* Controllers */

var controllers = {};

controllers.UsersCtrl = function($scope, $filter, Restangular, $stateParams, ngTableParams ) {
	var queryUsers = Restangular.all('api/users');
   
	queryUsers.getList().then(function(users){
	    $scope.allUsers = users;
		setupTable( users, ngTableParams, { 'username': 'asc'}, $scope, $filter );
	});
}

controllers.UsersCtrl.$inject = ['$scope', '$filter', 'Restangular', '$stateParams', 'ngTableParams' ];

controllers.GamesCtrl = function($scope, $filter, Restangular, $stateParams, ngTableParams ) {
	var queryGames = Restangular.all('api/games');
		
	queryGames.getList().then(function(games){
	    $scope.allGames = games;
		setupTable( games, ngTableParams, { 'game.nr': 'asc'}, $scope, $filter );
    });
   
}
controllers.GamesCtrl.$inject = ['$scope', '$filter', 'Restangular', '$stateParams', 'ngTableParams'];


controllers.UserCtrl = function($scope, $filter, Restangular, $stateParams, ngTableParams ) {
	$scope.stateParams = $stateParams;
	var queryUser = Restangular.one('api/user', $scope.stateParams.username);
   
    queryUser.get().then(function(userWithSpAndGB){
		$scope.user = userWithSpAndGB.user;
		$scope.special = userWithSpAndGB.special;
		$scope.gameBets = userWithSpAndGB.gameBets;
		setupTable( $scope.gameBets, ngTableParams, { 'game.game.nr': 'asc'}, $scope, $filter );
    });
}
controllers.UserCtrl.$inject = ['$scope', '$filter', 'Restangular', '$stateParams', 'ngTableParams'];

controllers.GameCtrl = function($scope, $filter, Restangular, $stateParams, ngTableParams ) {
	$scope.stateParams = $stateParams;
	var queryGame = Restangular.one('api/game', $scope.stateParams.gamenr);
    
	queryGame.get().then(function(gwtWithBetsPerUser){
		$scope.gwt = gwtWithBetsPerUser.game;
		$scope.betsUsers = gwtWithBetsPerUser.betsUsers;
		console.log($scope.betsUsers.length);
		setupTable( $scope.betsUsers, ngTableParams, { 'game.game.nr': 'asc'}, $scope, $filter );
	});

}
controllers.GameCtrl.$inject = ['$scope', '$filter', 'Restangular', '$stateParams', 'ngTableParams'];


controllers.SettingsCtrl = function($scope,$rootScope,$stateParams){
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
controllers.SettingsCtrl.$inject = ['$scope','$rootScope','$stateParams'];



controllers.LoginCtrl = function($scope, $rootScope, $stateParams, Restangular, $state){
	$scope.stateParams = $stateParams;	
		
	$scope.username = "";
	$scope.password = "";
	
	$scope.login = function(){
		var credentials = { 'username': $scope.username, 'password': $scope.password };
	    Restangular.all("login").post(credentials).then(
	      function(auth){ 
			  $rootScope.loggedInUser = auth.user;
			  $rootScope.authtoken = auth["AUTH-TOKEN"];
			  Restangular.setDefaultHeaders({'X-AUTH-TOKEN': auth["AUTH-TOKEN"]});
			  $state.transitionTo("users");
	      },
	      function(err){
	        console.log("err "+err);
	      });
    };

}
controllers.LoginCtrl.$inject = ['$scope', '$rootScope', '$stateParams', 'Restangular', '$state'];

return controllers;

});





