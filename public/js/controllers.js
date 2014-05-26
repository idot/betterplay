/*global define */

'use strict';

define(function() {

/* Controllers */

var controllers = {};

controllers.MyCtrl1 = function($scope, Restangular) {
	var baseUsers = Restangular.all('api/users');

	baseUsers.getList().then(function(users){
		$scope.allUsers = users;
	});
	
	
}
controllers.MyCtrl1.$inject = ['$scope', 'Restangular'];

controllers.MyCtrl2 = function() {}
controllers.MyCtrl2.$inject = [];

return controllers;

});