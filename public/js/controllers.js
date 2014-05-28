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
			   params.total(data.length);
		       var orderedData = params.sorting() ? $filter('orderBy')(data, params.orderBy()) : data;
               $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
	       });
        }
    });
}

controllers.UsersCtrl.$inject = ['$scope', '$filter', 'Restangular', '$stateParams', 'ngTableParams' ];

controllers.GamesCtrl = function($scope, $filter, $timeout, Restangular, $stateParams, ngTableParams ) {
	var baseGames = Restangular.all('api/games');
	
	$scope.df = DATEFILTER;
		
    $scope.tableParams = new ngTableParams({
        page: 1,            // show first page
        count: 10,          // count per page
        sorting: {
            'game.nr': 'asc'     // initial sorting
        }
    }, {
        total: 0,           // length of data
        getData: function($defer, params) {
 	       baseGames.getList().then(function(games){
                var data = games;
				params.total(data.length);
 		        var orderedData = params.sorting() ? $filter('orderBy')(data, params.orderBy()) : data;
                $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
 	       });
		}
   });
   
}
controllers.GamesCtrl.$inject = ['$scope', '$filter', '$timeout', 'Restangular', '$stateParams', 'ngTableParams'];


controllers.UserCtrl = function($scope, $filter, Restangular, $stateParams, ngTableParams ) {
	$scope.stateParams = $stateParams;
	var baseUser = Restangular.one('api/user', $scope.stateParams.username);

    $scope.tableParams = new ngTableParams({
        page: 1,            // show first page
        count: 10,          // count per page
        sorting: {
           'username' : 'asc'     // initial sorting
        }
    }, {
        total: 0,           // length of data
        getData: function($defer, params) {	
        	baseUser.get().then(function(userWithSpAndGB){
				$scope.user = userWithSpAndGB.user;
				$scope.special = userWithSpAndGB.special;
				var data = userWithSpAndGB.gameBets;
				params.total(data.length);
 		        var orderedData = params.sorting() ? $filter('orderBy')(data, params.orderBy()) : data;
                $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
	        });
	    }
     });
}
controllers.UserCtrl.$inject = ['$scope', '$filter', 'Restangular', '$stateParams', 'ngTableParams'];

controllers.GameCtrl = function($scope, $filter, Restangular, $stateParams, ngTableParams ) {
	$scope.stateParams = $stateParams;
	var baseGame = Restangular.one('api/game', $scope.stateParams.gamenr);
    
    $scope.tableParams = new ngTableParams({
        page: 1,            // show first page
        count: 10,          // count per page
        sorting: {
            nr: 'asc'     // initial sorting
        }
    }, {
        total: 0,           // length of data
        getData: function($defer, params) {	
        	baseGame.get().then(function(gwtWithBetsPerUser){
				$scope.gwt = gwtWithBetsPerUser.game;
				var data = gwtWithBetsPerUser.betsUsers;
				params.total(data.length);
 		        var orderedData = params.sorting() ? $filter('orderBy')(data, params.orderBy()) : data;
                $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
	        });
	    }
     });
}
controllers.GameCtrl.$inject = ['$scope', '$filter', 'Restangular', '$stateParams', 'ngTableParams'];


return controllers;

});
