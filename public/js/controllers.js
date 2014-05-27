/*global define */

'use strict';

define(function() {

/* Controllers */

var controllers = {};

controllers.UsersCtrl = function($scope, $filter, Restangular, $stateParams, ngTableParams ) {
	var baseUsers = Restangular.all('api/users');

    $scope.tableParams = new ngTableParams({
        page: 1,            // show first page
        count: 10,          // count per page
        sorting: {
            name: 'asc'     // initial sorting
        }
    }, {
        total: 0,           // length of data
        getData: function($defer, params) {
	       baseUsers.getList().then(function(users){
               var data = users;
		       var orderedData = params.sorting() ? $filter('orderBy')(data, params.orderBy()) : data;
               $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
	       });
        }
    });
}

controllers.UsersCtrl.$inject = ['$scope', '$filter', 'Restangular', 'stateParams', 'ngTableParams' ];

controllers.GamesCtrl = function($scope, $filter, Restangular, $stateParams, ngTableParams ) {
	var baseGames = Restangular.all('api/games');
	
    $scope.tableParams = new ngTableParams({
        page: 1,            // show first page
        count: 10,          // count per page
        sorting: {
            name: 'asc'     // initial sorting
        }
    }, {
        total: 0,           // length of data
        getData: function($defer, params) {

	baseGames.getList().then(function(games){
		$scope.allGames = games;
	});
	
}
controllers.GamesCtrl.$inject = ['$scope', '$filter', 'Restangular', 'stateParams', 'ngTableParams'];


controllers.UserCtrl = function($scope, $filter, Restangular, $stateParams, ngTableParams ) {
	$scope.params = $stateParams;
	var baseUser = Restangular.one('api/user', $scope.params.username);
    
    $scope.tableParams = new ngTableParams({
        page: 1,            // show first page
        count: 10,          // count per page
        sorting: {
            name: 'asc'     // initial sorting
        }
    }, {
        total: 0,           // length of data
        getData: function($defer, params) {	

	baseUser.get().then(function(userWithSpAndGB){
		  $scope.gameBets = userWithSpAndGB.gameBets;
		  $scope.user = userWithSpAndGB.user;
		  $scope.specialBet = userWithSpAndGB.specialBet;
	});
}

controllers.UserCtrl.$inject = ['$scope', '$filter', 'Restangular', '$stateParams', 'ngTableParams'];

return controllers;

});
