(function() {
    'use strict';

    angular
        .module('ui')
        .config(routerConfig);

    /** @ngInject */
    function routerConfig($stateProvider, $urlRouterProvider) {
        $urlRouterProvider.otherwise("/user/@reload@/bets"); 
   
        
        $stateProvider
        .state('home', {
            url: "/",
            templateUrl: 'app/partials/users.html',
            controller: 'UsersController',
            controllerAs: 'main'
        }) 
        .state('login', {
            url: "/login",
            templateUrl: 'app/partials/login.html',            
            controller: 'LoginController',
            controllerAs: 'vm'
        })
        .state('users', {
                url: "/users",
                templateUrl: 'app/partials/users.html',
                controller: 'UsersController',
                controllerAs: 'vm'
            })
            .state('games', {
                url: "/games",
                templateUrl: 'app/partials/games.html',
                controller: 'GamesController',
                controllerAs: 'vm'
            })
            .state('user', {
                abstract: true,
                url: "/user",
                template: '<ui-view/>'
            })
            .state('user.userBets', {
                url: "/:username/bets",
                templateUrl: 'app/partials/userBets.html',
                controller: 'UserController',
                controllerAs: 'vm'
            })
            .state('user.userEdit', {
                url: "/:username/edit",
                templateUrl: 'app/partials/userEdit.html',
                controller: 'EditUserController',
                controllerAs: 'vm'
            }) //   http://localhost:9000/em2016/#/completeRegistration/123456789012345678901234567890123456
            .state('completeRegistration', {
                url: "/completeRegistration/:token",
                templateUrl: 'app/partials/password.html',
                controller: 'CompleteRegistrationController',
                controllerAs: 'vm'
            })
            .state('changePassword', { //same like completeRegistration
                url: "/changePassword/:token",
                templateUrl: 'app/partials/password.html',
                controller: 'CompleteRegistrationController',
                controllerAs: 'vm'
            })
            .state('passwordRequest', { 
                url: "/requestPassword",
                templateUrl: 'app/partials/passwordRequest.html',
                controller: 'ChangePasswordRequestController',
                controllerAs: 'vm'
            })
            .state('user.specialBets', {
                url: "/:username/special",
                templateUrl: 'app/partials/userSpecialBets.html',
                controller: 'UserSpecialBetsController',
                controllerAs: 'vm'
            })
            .state('user.specialBetsspecialPlayers', {
                url: "/:username/special/player/:id?setresult",
                templateUrl: 'app/partials/userSpecialBetPlayer.html',
                controller: 'EditUserSpecialPlayerController',
                controllerAs: 'vm'
            })
            .state('user.specialBetsspecialTeams', {
                url: "/:username/special/team/:id?setresult",
                templateUrl: 'app/partials/userSpecialBetTeam.html',
                controller: 'EditUserSpecialTeamController',
                controllerAs: 'vm'
            })
            .state('game', {
                abstract: true,
                url: "/game",
                template: '<ui-view/>'
            })
            .state('game.gameBets', {
                url: "/:gamenr/bets",
                templateUrl: 'app/partials/gameBets.html',
                controller: 'GameController',
                controllerAs: 'vm'
            })
            .state('game.gameEdit', {
                url: "/:gamenr/edit",
                templateUrl: 'app/partials/gameEdit.html',
                controller: 'EditGameController',
                controllerAs: 'vm'
            })
            .state('game.gameCreate', {
                url: "/create",
                templateUrl: 'app/partials/gameCreate.html',
                controller: 'CreateGameController',
                controllerAs: 'vm'
            })
            .state('logout', {
                url: "/logout",
                onEnter: function(userService) {
                    userService.logout();
                }
            })
            .state('admin', {
                abstract: true,
                url: "/admin",
                template: '<ui-view/>'
            })
            .state('admin.createGame', {
                url: "/createGame",
                templateUrl: 'app/partials/gameCreate.html',
                controller: 'CreateGameController',
                controllerAs: 'vm'
            })
            .state('admin.registerUser', {
                url: "/registerUser",
                templateUrl: 'app/partials/registerUser.html',
                controller: 'RegisterUserController',
                controllerAs: 'vm'
            })
            .state('admin.mail', {
                url: "/mail",
                templateUrl: 'app/partials/mail.html',
                controller: 'MailController',
                controllerAs: 'vm'
            })
            .state('admin.settings', {
                url: "/settings",
                templateUrl: 'app/partials/settings.html',
                controller: 'SettingsController',
                controllerAs: 'vm'
            })
            .state('statistics', {
                abstract: true,
                url: "/statistics",
                template: '<ui-view/>'
            })
            .state('statistics.excel', {
                url: "/excel",
                templateUrl: 'app/partials/excel.html'
            })
            .state('statistics.plots', {
                url: "/plots",
                templateUrl: 'app/partials/statistics.html',
                controller: 'PlotSpecialBetsController',
                controllerAs: 'vm'
            });
    }

})();
        
    
