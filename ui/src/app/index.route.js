(function() {
    'use strict';

    angular
        .module('ui')
        .config(routerConfig);

    /** @ngInject */
    function routerConfig($stateProvider, $urlRouterProvider) {
        $urlRouterProvider.otherwise("/users");

        $stateProvider
            .state('users', {
                url: "/users",
                templateUrl: 'partials/users.html',
                controller: 'UsersCtrl',
                controllerAs: 'vm'
            })
            .state('games', {
                url: "/games",
                templateUrl: 'partials/games.html',
                controller: 'GamesCtrl',
                controllerAs: 'vm'
            })
            .state('user', {
                abstract: true,
                url: "/user",
                template: '<ui-view/>'
            })
            .state('user.userBets', {
                url: "/:username/bets",
                templateUrl: 'partials/userBets.html',
                controller: 'UserCtrl',
                controllerAs: 'vm'
            })
            .state('user.userEdit', {
                url: "/:username/edit",
                templateUrl: 'partials/userEdit.html',
                controller: 'EditUserCtrl',
                controllerAs: 'vm'
            })
            .state('user.specialBets', {
                url: "/:username/special",
                templateUrl: 'partials/userSpecialBets.html',
                controller: 'UserSpecialBetsCtrl',
                controllerAs: 'vm'
            })
            .state('user.specialBetsspecialPlayers', {
                url: "/:username/special/player/:id",
                templateUrl: 'partials/userSpecialBetPlayer.html',
                controller: 'EditUserSpecialPlayerCtrl',
                controllerAs: 'vm'
            })
            .state('user.specialBetsspecialTeams', {
                url: "/:username/special/team/:id",
                templateUrl: 'partials/userSpecialBetTeam.html',
                controller: 'EditUserSpecialTeamCtrl',
                controllerAs: 'vm'
            })
            .state('game', {
                abstract: true,
                url: "/game",
                template: '<ui-view/>'
            })
            .state('game.gameBets', {
                url: "/:gamenr/bets",
                templateUrl: 'partials/gameBets.html',
                controller: 'GameCtrl',
                controllerAs: 'vm'
            })
            .state('game.gameEdit', {
                url: "/:gamenr/edit",
                templateUrl: 'partials/gameEdit.html',
                controller: 'EditGameCtrl',
                controllerAs: 'vm'
            })
            .state('game.gameCreate', {
                url: "/create",
                templateUrl: 'partials/gameCreate.html',
                controller: 'CreateGameCtrl',
                controllerAs: 'vm'
            })
            .state('settings', {
                url: "/settings",
                templateUrl: 'partials/settings.html',
                controller: 'SettingsCtrl',
                controllerAs: 'vm'
            })
            .state('login', {
                url: "/login",
                templateUrl: 'partials/login.html',
                controller: 'LoginCtrl',
                controllerAs: 'vm'
            })
            .state('logout', {
                url: "/logout",
                onEnter: function($rootScope, $state) {
                    $rootScope.logout();
                    $state.transitionTo("users");
                }
            })
            .state('admin', {
                abstract: true,
                url: "/admin",
                template: '<ui-view/>'
            })
            .state('admin.createGame', {
                url: "/createGame",
                templateUrl: 'partials/gameCreate.html',
                controller: 'CreateGameCtrl',
                controllerAs: 'vm'
            })
            .state('admin.registerUser', {
                url: "/registerUser",
                templateUrl: 'partials/registerUser.html',
                controller: 'RegisterUserCtrl',
                controllerAs: 'vm'
            })
            .state('statistics', {
                abstract: true,
                url: "/statistics",
                template: '<ui-view/>'
            })
            .state('statistics.excel', {
                url: "/excel",
                templateUrl: 'partials/excel.html'
            })
            .state('statistics.plots', {
                url: "/plots",
                views: {
                    '': {
                        templateUrl: 'partials/specialPlots.html'
                    },
                    'mvp@statistics.plots': {
                        templateUrl: 'partials/plot.html',
                        controller: 'PlotSpecialBetsCtrl',
                        controllerAs: 'vm',
                        resolve: {
                            tid: function() {
                                return {
                                    templateId: "1"
                                };
                            }
                        }
                    },
                    'svp@statistics.plots': {
                        templateUrl: 'partials/plot.html',
                        controller: 'PlotSpecialBetsCtrl',
                        controllerAs: 'vm',
                        resolve: {
                            tid: function() {
                                return {
                                    templateId: "2"
                                };
                            }
                        }
                    },
                    'champion@statistics.plots': {
                        templateUrl: 'partials/plot.html',
                        controller: 'PlotSpecialBetsCtrl',
                        controllerAs: 'vm',
                        resolve: {
                            tid: function() {
                                return {
                                    templateId: "3"
                                };
                            }
                        }
                    }
                }
            });
    }

})();