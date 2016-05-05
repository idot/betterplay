(function() {
    'use strict';

    angular
        .module('ui')
        .controller('UsersController', UsersController)
        .controller('GamesController', GamesController)
        .controller('UserController', UserController)
        .controller('GameController', GameController)
        .controller('SettingsController', SettingsController)
        .controller('LoginController', LoginController)
        .controller('RegisterUserController', RegisterUserController)
        .controller('EditUserController', EditUserController)
        .controller('UserSpecialBetsController', UserSpecialBetsController)
        .controller('EditUserSpecialPlayerController', EditUserSpecialPlayerController)
        .controller('EditUserSpecialTeamController', EditUserSpecialTeamController)
        .controller('CreateGameController', CreateGameController)
        .controller('PlotSpecialBetsController', PlotSpecialBetsController)
        .controller('ExcelController', ExcelController);

    /** @ngInject */
    function UsersController($log, $filter, Restangular, betterSettings) {
        var vm = this;
        vm.DF = betterSettings.DF;
        vm.allUsers = [];
        vm.dtoptions = {
            scrollbarV: false
        };

        // vm.getUsers = getUsers;

        var queryUsers = Restangular.all('em2016/api/users');

        activate();

        vm.badgecolour = function(rank) {
            switch (rank) {
                case 1:
                    return "badge-gold";
                case 2:
                    return "badge-silver";
                case 3:
                    return "badge-bronze";
                default:
                    return "";
            }
        };

        function getUsers() {
            queryUsers.getList().then(function(users) {
                vm.allUsers = users;
                $log.debug(users);
            });
        }


        function activate() {
            getUsers();
        }

    }

     /** @ngInject */
    function GamesController($log, $filter, Restangular, $stateParams, _, betterSettings) {
        var vm = this;
        vm.DF = betterSettings.DF;
        vm.allGames = [];


        var queryGames = Restangular.all('em2016/api/games');

        vm.openFilter = ['open', 'closed'];

        vm.levelFilter = ['group', 'last16', 'quarter', 'semi', 'final', 'third'];

        activate();

        function transformGame(game) {
            game.gamenr = game.game.nr;
            game.team1name = game.team1.name;
            game.team2name = game.team2.name;
            game.openGame = betterSettings.betClosed(game.game.serverStart) ? "closed" : "open";
            game.levelname = game.level.name;
            game.serverStart = game.game.serverStart;
        }

        function getGames() {
            queryGames.getList().then(function(games) {
                var allGames = games;
                vm.allGames = _.each(allGames, function(g) {
                    transformGame(g)
                });
            })
        }

        vm.prettyGame = function(game) {
            if (game.result.isSet) {
                return game.result.goalsTeam1 + ":" + game.result.goalsTeam2
            } else {
                return "-:-"
            }
        }

        function activate() {
            getGames();
        }

    }
    /** @ngInject */
    function UserController($log, $filter, Restangular, $stateParams, toastr, _, betterSettings, gblFilter) {
        var vm = this;
        vm.stateParams = $stateParams;
        vm.user = [];
        vm.special = [];
        vm.gameBets = [];
        vm.DF = betterSettings.DF;
        vm.getTime = betterSettings.getTime;
        vm.allGameBets = [];
        
        var queryUser = Restangular.one('em2016/api/user', vm.stateParams.username);
     
        vm.filterChanged = function(){
            vm.gameBets = gblFilter(vm.allGameBets);
        }

        function getUser() {
            queryUser.get().then(function(userWithSpAndGB) {
                vm.user = userWithSpAndGB.user;
                vm.special = userWithSpAndGB.special;
                var gameBets = userWithSpAndGB.gameBets;
                vm.allGameBets = gameBets;
                vm.gameBets = gblFilter(gameBets);
            })
        }

        activate();

        function activate() {
            getUser();
        }
    }
    
     /** @ngInject */
    function GameController($log, $filter, Restangular, $stateParams, _) {
        var vm = this;
        vm.stateParams = $stateParams;

        var queryGame = Restangular.one('em2016/api/game', vm.stateParams.gamenr);


        activate();

        function getGame() {
            queryGame.get().then(function(gwtWithBetsPerUser) {
                vm.gwt = gwtWithBetsPerUser.game;
                var betsUsers = gwtWithBetsPerUser.betsUsers;
                vm.betsUsers = _.each(betsUsers, function(bu) {
                    bu.username = bu.user.username;
                });
            })
        }

        function activate() {
            getGame();
        }
    }

     /** @ngInject */
    function SettingsController($log, $stateParams, Restangular, toastr, moment, betterSettings) {
        var vm = this;
        vm.stateParams = $stateParams;

        vm.DF = betterSettings.DF;
        vm.updateInterval = betterSettings.UPDATEINTERVAL;
        //separating current date and time for datepicker and timepicker
        vm.date = new Date(betterSettings.currentTime.getTime());
        vm.time = new Date(betterSettings.currentTime.getTime());

        vm.setFormat = function() {
            betterSettings.DF = vm.DF;
        };

        vm.setUpdateinterval = function() {
            betterSettings.UPDATEINTERVAL = vm.updateInterval;
        };

        vm.updateTime = function() {
            betterSettings.updateTime(vm.time);
        };

        vm.updateDate = function() {
            betterSettings.updateDate(vm.date);
        };

    }

    /** @ngInject */
    function LoginController($log, $stateParams, Restangular, $state, userService) {
        var vm = this;
        vm.stateParams = $stateParams;

        vm.username = "";
        vm.password = "";

        vm.login = function() {
            $log.debug("login: "+vm.username+" *******");
            var credentials = {
                'username': vm.username,
                'password': vm.password
            };
            userService.login(credentials);
        };
    }

     /** @ngInject */
    function RegisterUserController($log, $stateParams, Restangular, $state, toastr, _) {
        var vm = this;
        vm.stateParams = $stateParams;

        vm.allUsers = [];

        var queryUsers = Restangular.all('em2016/api/users');


        vm.setFields = function() {
            vm.username = "";
            vm.password1 = "";
            vm.password2 = "";
            vm.email = "";
        };

        vm.getUsers = function() {
            queryUsers.getList().then(function(users) {
                vm.allUsers = _.map(users, function(u) {
                    return u.username
                });
            });
        }

        vm.uniqueUsername = function(username) {
            var duplicated = _.find(vm.allUsers, function(u) {
                return u === username;
            });
            $log.debug("unique: " + username + " " + duplicated);
            return !duplicated;
        };

        vm.signon = function() {
            var pu = {
                'username': vm.username,
                'password': vm.password1,
                'email': vm.email
            };
            Restangular.all('em2016/api/user/' + vm.username).customPUT(pu).then(
                function(success) {
                    toastr.pop('success', "registered " + vm.username); //TODO: email
                    vm.setFields();
                }
            );
        };

        function activate() {
            vm.setFields();
            vm.getUsers();
        }

        activate();
    }

     /** @ngInject */
    function EditUserController($log, $stateParams, Restangular, $state, toastr, userService) {
        var vm = this;
        vm.stateParams = $stateParams;
        vm.formUser = {};

        vm.refreshUser = function() {
            var queryUser = Restangular.one('em2016/api/userWithEmail');
            queryUser.get().then(function(userWithEmail) {
                vm.formUser = userWithEmail;
                userService.updateLogin(userWithEmail);
            });
        }

        vm.pass = {
            word1: "",
            word2: ""
        };

        vm.updatePassword = function() {
            $log.debug('submitting new password: ' + vm.pass.word1);
            var pu = {
                'password': vm.pass.word1
            };
            Restangular.all('em2016/api/user/' + vm.formUser.username + '/password').customPOST(pu).then(
                function(success) {
                    toastr.pop('success', "changed password");
                    vm.pass.word1 = "";
                    vm.pass.word2 = "";
                }
            );
        };


        vm.updateDetails = function() {
            $log.debug('updating details: ');
            var u = {
                firstName: vm.formUser.firstName,
                lastName: vm.formUser.lastName,
                email: vm.formUser.email,
                icontype: vm.formUser.icontype
            };
            Restangular.all('em2016/api/user/' + vm.formUser.username + '/details').customPOST(u).then(
                function(success) {
                    toastr.pop('success', "updated user details");
                    vm.refreshUser();
                }
            );
        };

        activate();

        function activate() {
            vm.refreshUser();
        }
    }
    
   

    /** @ngInject */
    function UserSpecialBetsController($log, $filter, $stateParams, Restangular, $state, toastr, betterSettings, userService) {
        var vm = this;
        vm.stateParams = $stateParams;
        vm.user = {};
        vm.templateBets = {};
        vm.noInstructions = true;
        vm.DF = betterSettings.DF;

        function getUserBets() {
            Restangular.one('em2016/api/user', vm.stateParams.username).one('specialBets').get().then(
                function(success) {
                    vm.user = success.user;
                    vm.templateBets = success.templateBets;
                    if (userService.isOwner(vm.user.id) && ! userService.userHadInstructions()) {
                        vm.noInstructions = true;
                        toastr.info('info', "Welcome " + success.user.username + "!", "Please place special bets until start of the game.\n Have fun!")
                    } else {
                        vm.noInstructions = false;
                    }
                }
            );
        }

        vm.change = function(templatebet) {
            switch (templatebet.template.itemType) {
                case "team":
                    $state.transitionTo("user.specialBetsspecialTeams", {
                        username: vm.user.username,
                        id: templatebet.bet.id
                    });
                    break;
                case "player":
                    $state.transitionTo("user.specialBetsspecialPlayers", {
                        username: vm.user.username,
                        id: templatebet.bet.id
                    });
                    break;
                default:
                    toastr.pop('error', "someting is wrong!", "could not decide if its bet for player or team. Please inform somebody by email");
            }
        };

        activate();

        function activate() {
            getUserBets();
        }
    }

     /** @ngInject */
    function EditUserSpecialPlayerController($log, $filter, $stateParams, Restangular, $state, toastr, _, specialBetService) {
        var vm = this;
        vm.stateParams = $stateParams;
        vm.betId = $stateParams.id;
        vm.user = {};
        vm.tb = {};

        specialBetService.getSpecialBet(vm.betId, vm.stateParams.username).then(function(success) {
            vm.user = success.user;
            vm.tb = success.templateBet;
        });

        Restangular.all('em2016/api/players').getList().then(
            function(success) {
                var forFilter = _.map(success, function(pt) {
                    pt.name = pt.player.name;
                    pt.tname = pt.team.name;
                    return pt;
                });
                vm.playerWithTeams = forFilter;
            }
        );

        vm.selectPlayer = function() {
            specialBetService.saveSelected(vm.tb.bet, vm.user, vm.playerWithTeams);
        };
    }


     /** @ngInject */
    function EditUserSpecialTeamController($log, $filter, $stateParams, Restangular, $state, toastr, specialBetService) {
        var vm = this;
        vm.stateParams = $stateParams;
        vm.betId = $stateParams.id;
        vm.user = {};
        vm.tb = {};

        specialBetService.getSpecialBet(vm.betId, vm.stateParams.username).then(function(success) {
            vm.user = success.user;
            vm.tb = success.templateBet;
        });

        Restangular.all('em2016/api/teams').getList().then(
            function(success) {
                vm.teams = success;
            }
        );

        vm.selectTeam = function() {
            specialBetService.saveSelected(vm.tb.bet, vm.user, vm.teams)
        };
    }

     /** @ngInject */
    function CreateGameController($log, $filter, $stateParams, Restangular, $state, toastr, moment) {
        var vm = this;
        vm.stateParams = $stateParams;

        Restangular.all('em2016/api/teams').getList().then(
            function(success) {
                vm.teams = success;
                vm.team1 = vm.teams[0];
                vm.team2 = vm.teams[0];
            }
        );

        Restangular.all('em2016/api/levels').getList().then(
            function(success) {
                vm.levels = success;
                vm.level = vm.levels[0];
            }
        );

        vm.servero = false;
        vm.localo = false;

        vm.opens = function($event) {
            $event.preventDefault();
            $event.stopPropagation();
            vm.servero = true;
        };

        vm.openl = function($event) {
            $event.preventDefault();
            $event.stopPropagation();
            vm.localo = true;
        };

        var toDateTime = function(d, t) {
            var md = moment(d);
            var mt = moment(t);
            md.hours(mt.hours());
            md.minutes(mt.minutes());
            return md.toDate();
        };

        var now = function() {
            var md = new Date();
            var m = moment(md);
            m.hours(18);
            m.minutes(0);
            return {
                date: m.toDate(),
                time: m.toDate()
            };
        };

        vm.server = now();
        vm.local = now();
        vm.disabled = false;

        vm.submit = function() {
            vm.disabled = true;
            var server = toDateTime(vm.server.date, vm.server.time);
            var local = toDateTime(vm.local.date, vm.local.time);
            var createdGame = {
                serverStart: server.getTime(),
                localStart: local.getTime(),
                team1: vm.team1.name,
                team2: vm.team2.name,
                level: vm.level.level
            };
            Restangular.all('em2016/api/game').customPOST(createdGame).then(
                function(success) {
                    toastr.pop('success', "created game", success);
                    vm.disabled = false;
                }
            );
        };
    }

     /** @ngInject */
    function PlotSpecialBetsController($stateParams, $state, specialBetStats, tid) {
        var vm = this;

        //  $scope.stateParams = $stateParams; not applicable within a nested view
        if (typeof tid !== "undefined") {
            vm.templateId = tid.templateId;
        } else {
            //extract from parameters
        }

        specialBetStats.getStats(vm.templateId).then(function(tb) {
            vm.template = tb.template;
            vm.plotData = tb.data;
        });

    }

     /** @ngInject */
    function ExcelController($stateParams, $state, Restangular, $http, $document) {
        var vm = this;

        vm.filename = ""

        vm.upload = function() {
            fd = $document.getElementById('file').files[0],
                r = new FileReader();
            r.onloadend = function(e) {
                var data = e.target.result;
                var blob = new Blob([data.content], {
                    type: 'application/xls',
                    filename: vm.filename
                });
                $http.post('/data/fileupload', fd, {
                    transformRequest: angular.identity,
                    headers: {
                        'Content-Type': undefined
                    }
                });
            }
            r.readAsArrayBuffer()
        };
    }


})();