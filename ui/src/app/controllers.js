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
        .controller('ExcelController', ExcelController)
        .controller("CompleteRegistrationController", CompleteRegistrationController)
        .controller("ChangePasswordRequestController", ChangePasswordRequestController);

    /** @ngInject */
    function UsersController($log, $filter, Restangular, betterSettings, userService) {
        var vm = this;
        vm.allUsers = [];
        
        var queryUsers = Restangular.all('em2016/api/users');

        activate();

        function getUsers() {
            queryUsers.getList().then(function(users) {
                vm.allUsers = users;
            });
        }

        function activate() {
            getUsers();
        }

    }

     /** @ngInject */
    function GamesController($log, $filter, Restangular, $stateParams, _, betterSettings, userService) {
        var vm = this;
        vm.DF = betterSettings.DF;
        vm.allGames = [];
       
        var queryGames = Restangular.all('em2016/api/games');

        activate();


        function getGames() {
            queryGames.getList().then(function(games) {
                vm.allGames = games;
            })
        }

        function activate() {
            getGames();
        }

    }
    
    /** @ngInject */
    function UserController($log, $filter, Restangular, $stateParams, toastr, _, betterSettings, gblFilter, userService, $state) {
        var vm = this;
        vm.stateParams = $stateParams;
        vm.user = {};
        vm.special = [];
        vm.gameBets = [];
        vm.DF = betterSettings.DF;
        vm.getTime = betterSettings.getTime;
        vm.allGameBets = [];
       
        var queryUserName = vm.stateParams.username;
        if(vm.stateParams.username && vm.stateParams.username == "@reload@"){
            queryUserName = userService.loggedInUser.username;
            if(queryUserName === undefined ||  queryUserName == ""){
                 $state.go("login");
            }else{
                 $state.go("user.userBets", {  username: queryUserName });
            }
        };
        
        var queryUser = Restangular.one('em2016/api/user',  queryUserName);
     
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
    function GameController($log, $filter, $mdDialog, Restangular, $stateParams, _, userService) {
        var vm = this;
        vm.stateParams = $stateParams;
        vm.betsUsers = [];
        vm.gwt = {};
 
        var queryGame = Restangular.one('em2016/api/game', vm.stateParams.gamenr);


        activate();

        function getGame() {
            queryGame.get().then(function(gwtWithBetsPerUser) {
                vm.gwt = gwtWithBetsPerUser.game;
                vm.betsUsers = gwtWithBetsPerUser.betsUsers;
            })
        }
 /*
        vm.plotBets = function(){
            alert = $mdDialog.alert({
                   title: 'Attention',
                   template: ' <game-bets-plot id="id"></game-bets-plot>',
                   ok: 'Close',
                   locals: { id: vm.gwt.game.id }
                 });
                 $mdDialog
                   .show( alert )
                   .finally(function() {
                     alert = undefined;
              });
            
        }
*/
    /*    vm.plotBets = function(){
            
            
            
        }*/        
       

        function activate() {
            getGame();
        }
    }

     /** @ngInject */
    function SettingsController($log, $stateParams, Restangular, toastr, moment, betterSettings, userService) {
        var vm = this;
        vm.stateParams = $stateParams;
        vm.userService = userService;
        vm.mailpassword = "";

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
        
        vm.submitPassword = function(){
            var message = {
                'password': vm.mailpassword
            };
            Restangular.all('/em2016/api/mailpassword').customPOST(message).then(function(result){
                toastr.info("updated mail password "+result);       
            });        
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
    function RegisterUserController($log, $stateParams, Restangular, $state, toastr, _, $scope, userService) {
        var vm = this;
        vm.allUsers = [];
        vm.username = "";   
        vm.email = "";
        vm.firstname = "";
        vm.lastname = "";


       var queryUsers = Restangular.all('em2016/api/users');

        vm.getUsers = function() {
            queryUsers.getList().then(function(users) {
                vm.allUsers = _.map(users, function(u) {
                    return u.username
                });
            });
        }
        
        vm.comparePasswords = function(form){
            var identical = vm.password1 == vm.password2
            form.password2.$setValidity('identical', identical);        
        };

        vm.uniqueUsername = function(form) {
            var duplicated = _.find(vm.allUsers, function(u) {
                return u === vm.username;
            });
            form.username.$setValidity('unique', ! duplicated);
        };

        vm.signon = function() {
            if(vm.password1 != vm.password2){
                 toastr.error("passwords don't match!");
                 vm.password1 = "";
                 vm.password2 = "";
                 return;
            }
            var pu = {
                'username': vm.username,
                'password': vm.password1,
                'firstname': vm.firstname,
                'lastname': vm.lastname,
                'email': vm.email
            };
            Restangular.all('em2016/api/user/create').customPUT(pu).then(
                function(success) {
                    toastr.success("registered", vm.username);
                    $state.reload();
                }
            );
        };

        function activate() {
            vm.getUsers();
        }

        activate();
    }

     /** @ngInject */
    function EditUserController($log, $stateParams, Restangular, $state, toastr, userService) {
        var vm = this;
        vm.stateParams = $stateParams;
        vm.password1 = "";
        vm.password2 = "";
        
        vm.user = {};
        
        vm.icontype = "";
        vm.showname = false;
        vm.email = "";
        vm.institute = "NA";
  
        vm.userIdentical = function(){
             return userService.identical(vm.stateParams.username);
        };
  
        vm.refreshUser = function() {
            var queryUser = Restangular.one('em2016/api/userWithEmail');
            queryUser.get().then(function(userWithEmail) {
                vm.icontype = userWithEmail.icontype;
                vm.showname = userWithEmail.showName;
                vm.email = userWithEmail.email;
              //TODO  vm.institute = userWithEmail.institute;
                vm.user = userWithEmail;
                userService.updateLogin(userWithEmail);
            });
        }

        vm.comparePasswords = function(form){
            var identical = vm.password1 == vm.password2
            form.password2.$setValidity('identical', identical);        
        };
       
        vm.changePassword = function() {
            if(vm.password1 != vm.password2){
                 toastr.error("passwords don't match!");
                 vm.password1 = "";
                 vm.password2 = "";
                 return;
            }
            var pu = {
                'password': vm.password1
            };
            Restangular.all('em2016/api/user/password').customPOST(pu).then(
                function(success) {
                    toastr.success("changed password");
                    $state.reload();
                }
            );
        };

        vm.changeDetails = function() {
            var u = {
                email: vm.email,
                icontype: vm.icontype,
                institute: vm.institute,
                showname: vm.showname
            };
            Restangular.all('em2016/api/user/details').customPOST(u).then(
                function(success) {
                    toastr.success("updated user details");
                    vm.refreshUser();
                }
            );
        };
        
        vm.updateName = function() {
            $log.debug('updating details: ');
            var u = {
                id: vm.form.User.username,
                firstName: vm.formUser.firstName,
                lastName: vm.formUser.lastName,
            };
            Restangular.all('em2016/api/user/details').customPOST(u).then(
                function(success) {
                    toastr.success('success', "updated user details");
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
    function UserSpecialBetsController($log, $filter, $stateParams, Restangular, $state, toastr, betterSettings, userService, $mdDialog, $mdMedia) {
        var vm = this;
        vm.stateParams = $stateParams;
        vm.user = {};
        vm.templateBets = {};
        vm.noInstructions = true;
        vm.DF = betterSettings.DF;
        vm.setresult = false;
        
        vm.isIdentical = function(){
            return vm.user.username && userService.loggedInUser.username == vm.user.username;  
        };
     
   
        vm.canBet = function(bet){
              return betterSettings.specialBetOpen(bet.bet);  
        };
        
        vm.specialBetsOpen = function(){
              return betterSettings.specialBetsOpen();  
        };
    
        function getUserBets() {
            Restangular.one('em2016/api/user', vm.stateParams.username).one('specialBets').get().then(
                function(success) {
                    vm.user = success.user;
                    vm.templateBets = success.templateBets;
                    if (userService.isOwner(vm.user.id) && ! userService.userHadInstructions()) {
                        vm.noInstructions = true;
                        toastr.info("Please place special bets before start of the games.\n Have fun!", "Welcome "+success.user.username, { timeOut: 5500 });
                    } else {
                        vm.noInstructions = false;
                    }
                }
            );
        }

        vm.change = function(templatebet) {
            var params = {
                username: vm.user.username,
                id: templatebet.bet.id,
            };
            if(vm.setresult){
           //   it would be better to have the warning here, but the dialog is non blocking
           //  vm.showConfirm(templatebet);
                params.setresult = true;  
            };
            
            switch (templatebet.template.itemType) {
                case "team":
                    $state.transitionTo("user.specialBetsspecialTeams", params);
                    break;
                case "player":
                    $state.transitionTo("user.specialBetsspecialPlayers", params);
                    break;
                default:
                    toastr.error("could not decide if its bet for player or team.\nPlease inform administrators by email", "someting is wrong!");
            }
        };

        vm.showConfirm = function() {
            if(! vm.setresult){
                return;
            }
            // Appending dialog to document.body to cover sidenav in docs app
            var confirm = $mdDialog.confirm()
                  .title('You are setting the result')
                  .textContent('Maybe you only want to bet?')
                  .ariaLabel('set result')
                  .ok('set result')
                  .cancel('cancel');
            $mdDialog.show(confirm).then(function() {
                vm.setresult = true;
            }, function() {
                 vm.setresult = false;
            });
          };

        activate();
        
        function activate() {
            getUserBets();
        }
    }

     /** @ngInject */
    function EditUserSpecialPlayerController($log, $filter, $stateParams, Restangular, $state, toastr, specialBetService, betterSettings) {
        var vm = this;
        vm.stateParams = $stateParams;
        vm.betId = $stateParams.id;
        
        if($stateParams.setresult){
            vm.setresult = true;
        }else{
            vm.setresult = false;
        }
        
        vm.user = {};
        vm.tb = {};
        vm.playersWithTeams = [];

        vm.specialBetsOpen = function(){
              return betterSettings.specialBetsOpen();  
        };

        specialBetService.getSpecialBet(vm.betId, vm.stateParams.username).then(function(success) {
            vm.user = success.user;
            vm.tb = success.templateBet;
        });

        Restangular.all('em2016/api/players').getList().then(
            function(success) {
                vm.playersWithTeams = success;
            }
        );

        vm.select = function(player) {
            specialBetService.saveSelected(vm.tb.bet, vm.user, player.player, vm.setresult);
        };
    }


     /** @ngInject */
    function EditUserSpecialTeamController($log, $filter, $stateParams, Restangular, $state, toastr, specialBetService, betterSettings) {
        var vm = this;
        vm.stateParams = $stateParams;
        vm.betId = $stateParams.id;
        vm.user = {};
        vm.tb = {};
        vm.allTeams = {};
        
        if($stateParams.setresult){
            vm.setresult = true;
        }else{
            vm.setresult = false;
        }
        
        vm.specialBetsOpen = function(){
              return betterSettings.specialBetsOpen();  
        };

        specialBetService.getSpecialBet(vm.betId, vm.stateParams.username).then(function(success) {
            vm.user = success.user;
            vm.tb = success.templateBet;
        });

        Restangular.all('em2016/api/teams').getList().then(
            function(success) {
                vm.allTeams = success;
            }
        );

        vm.select = function(team) {
            specialBetService.saveSelected(vm.tb.bet, vm.user, team, vm.setresult);
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
                    toastr.success('success', "created game", success);
                    vm.disabled = false;
                }
            );
        };
    }

     /** @ngInject */
    function PlotSpecialBetsController($stateParams, $state, specialBetStats, Restangular, userService) {
        var vm = this;
    
        vm.userService = userService;
        vm.getExcel = function(){
            vm.userService.getExcel();
        };
    }

     /** @ngInject */
    function ExcelController($stateParams, $state, Restangular, $document) {
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
      //          $http.post('/data/fileupload', fd, {
        //            transformRequest: angular.identity,
        //            headers: {
        //                'Content-Type': undefined
    //                }
      //          });
            }
            r.readAsArrayBuffer()
        };
    }
 
     /** @ngInject */
    function ChangePasswordRequestController($stateParams, $state, Restangular, toastr, betterSettings) {
        var vm = this;
        vm.recaptchasite = betterSettings.settings.recaptchasite;
        vm.email = "";
        
        vm.setResponse = function(response) {
            if(vm.email == ""){
                toastr.error("please enter email!");
                return;
            }
            var message = {
                email: vm.email,
                response: response
            };
            
            Restangular.all('em2016/api/changePasswordRequest').customPOST(message).then(
                function(success) {
                    toastr.info('success', "updated user details");
                    vm.refreshUser();
                }
            );
       };
    }
 
 
 
     /** @ngInject */
    function CompleteRegistrationController($log, Restangular, toastr, betterSettings, userService, $scope, _, $stateParams, $state) {
        var vm = this;
        
        vm.password1 = "";
        vm.password2 = "";
        vm.username = $stateParams.username;
        vm.token = $stateParams.token;
        vm.newPassword = $state.includes("newPassword");
        
        vm.comparePasswords = function(form){
            var identical = vm.password1 == vm.password2;
            form.password2.$setValidity('identical', identical);     
        };
        
        vm.submit = function() {
            if(vm.password1 != vm.password2){
                 toastr.error("passwords don't match!");
                 vm.password1 = "";
                 vm.password2 = "";
                 return;
            }
            var pu = {
                'password': vm.password1,
                'token': vm.token
            };
            Restangular.all('em2016/api/tokenPassword').customPUT(pu).then(
                function(auth) {
                    userService.updateLogin(auth.user, auth["AUTH-TOKEN"]);
                    if(vm.newPassword){
                          toastr.success(auth.user.username, "changed password");
                          $state.transitionTo("user.userBets", {
                              username: auth.user.username
                          });
                    } else {
                          toastr.success(auth.user.username, "welcome");
                          $state.transitionTo("user.specialBets", {
                              username: auth.user.username
                              //http://benfoster.io/blog/ui-router-optional-parameters add new invisible parameter to display instructions popup in specialbets
                          });
                    }
                }
            );
        };
                
    };

})();