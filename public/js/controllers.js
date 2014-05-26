/*global define */

'use strict';

define(function() {

/* Controllers */

var controllers = {};

controllers.UsersCtrl = function($scope, Restangular) {
	var baseUsers = Restangular.all('api/users');

	baseUsers.getList().then(function(users){
		$scope.allUsers = users;
	});
}

controllers.UsersCtrl.$inject = ['$scope', 'Restangular'];

controllers.GamesCtrl = function($scope, Restangular) {
	var baseGames = Restangular.all('api/games');
	
	baseGames.getList().then(function(games){
		$scope.allGames = games;
	});
	
}
controllers.GamesCtrl.$inject = ['$scope', 'Restangular'];


controllers.UserCtrl = function($scope, Restangular, $stateParams) {
	$scope.params = $stateParams;
	var baseUser = Restangular.one('api/user', $scope.params.username);
	
	baseUser.get().then(function(userWithSpAndGB){
		  $scope.gameBets = userWithSpAndGB.gameBets;
		  $scope.user = userWithSpAndGB.user;
		  $scope.specialBet = userWithSpAndGB.specialBet;
	});
}

controllers.UserCtrl.$inject = ['$scope', 'Restangular', '$stateParams'];

return controllers;

});