(function() {
        'use strict';

        angular
            .module('ui')
            .value('version', '0.1')
            .factory('specialBetService', function($state, Restangular, toastr, _, userService) {
                return {
                    getSpecialBet: function(betId, username) {
                        return Restangular.one('em2016/api/user', username).one('specialBets').get().then(
                            function(success) {
                                var user = success.user;
                                var tb = _.filter(success.templateBets, function(b) {
                                    return b.bet.id == betId;
                                })[0];
                                return {
                                    user: user,
                                    templateBet: tb
                                };
                            }
                        )
                    },

                    saveSelected: function(bet, user, selected, setresult) {
                        bet.prediction = selected.name;
                        var url = setresult ? 'em2016/api/specialBetResult' : 'em2016/api/specialBet';
                        Restangular.all(url).customPOST(bet).then(
                            function(success) {
                                if (!user.hadInstructions && ! setresult) {
                                    Restangular.all('em2016/api/userhadinstructions').customPOST().then(
                                        function(success) {
                                            toastr.success("You have placed your first special bet.\nPlease don't forget to place all special bets until start of the games.", "Congratulations" + user.username+"! ");
                                            userService.loggedInUser.hadInstructions = true;
                                        }
                                    );
                                }
                                $state.transitionTo("user.specialBets", {
                                    username: user.username
                                });
                            });
                    }
                };
            })
            .factory('specialBetStats', function(Restangular, _ ) {
                return {
                    getStats: function(templateName) {
                        return Restangular.one('em2016/api/statistics/specialBet', templateName).get().then(
                            function(success) {
                                var template = success.template;
                                var grouped = _.groupBy(success.predictions, function(b) {
                                    return b;
                                });
                                var bets = _.map(grouped, function(v, k) {
                                    var item = k.toString() == "" ? "undecided" : k.toString();
                                    var arr = { label: item, value: v.length };
                                    return arr;
                                });
                                var result = {
                                    template: template,
                                    data: [{
                                        key: template.name,
                                        values: bets
                                    }]
                                };
                                return result;
                            }
                        );
                    }
                };
            })
            .factory('gameBetStats', function(Restangular, _ ) {
                return {
                    getStats: function(gameId, team1, team2) {
                        return Restangular.one('em2016/api/statistics/game', gameId).get().then(
                            function(gameResults){
                                var getGoals = function(gameResults, goals, sign){
                                    var g = _.map(gameResults, function(b){ return b.isSet ? b[goals] : "X" });
                                    var gc = _.groupBy(g, function(b){ return b; });
                                    var gcm = [ { label: "NA",  value: 0 }];
                                    if(gc.length > 1){
                                        gcm = _.map(gc, function(k,v){ return { label: k, value: v.length * sign}});
                                    } else {
                                        gcm = [{ label: "X", value: gc["X"].length * sign}];
                                    }
                                    return gcm;
                                };
                                var g1 = getGoals(gameResults, "goalsTeam1", -1);
                                var g2 = getGoals(gameResults, "goalsTeam2",  1);
                                
                                var g1m = _.min(g1, function(o){ o.value });
                                var g2m = _.max(g2, function(o){ o.value });
                                var mx = _.max([g1m.value * -1, g2m.value]);
                                
                                
                                var gg1 = { key:  team1, "color": "#d62728", values: g1 };
                                var gg2 = { key:  team2, "color": "#1f77b4", values: g2 };
                                
                                var result = {
                                    data:  [gg1, gg2] ,
                                    max: mx    
                                };

                                return result;
                            }
                        );
                    }
                };
            })
            .service('betterSettings', function($log, Restangular, $timeout, toastr, moment, userService) {
                    var vm = this;
                    vm.startupTime = new Date();
                    vm.currentTime = new Date();

                    vm.settings = { //default
                          debug: false,
                          gamesStarts: new Date(),
                          recaptchasite: "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI"//default key no captcha
                    };

                    vm.getTime = function(){
                        return vm.currentTime;  
                    };
                    
                    vm.badgecolour = function(rank) {
                        switch (rank) {
                            case 1:
                                return "badge-gold";
                            case 2:
                                return "badge-silver";
                            case 3:
                                return "badge-bronze";
                            default:
                                return "badge-points";
                        }
                    };

                    //should we fetch the time from the server or take the interactively set time
                   var TIMEFROMSERVER = true;

                    //format for time display
                    vm.DF = 'd/M HH:mm';

                    //time before game start that bet closes
                    var MSTOCLOSING = 60 * 60 * 1000; //in ms

                    //time to update clock
                    var UPDATEINTERVAL = 1000 * 5; //in ms

                    //reload clock from server 
                    var RESETTIMEDIFF = 5 * 60 * 1000; //in ms

                    vm.timeLeft = function(serverStart) {
                        //boolean true add in/ago
                        //negative values = ago
                        //positive values = in
                        var diff = (serverStart - MSTOCLOSING) - vm.currentTime;
                        var s = moment.duration(diff, "milliseconds").humanize(false);
                        return s;
                    };
   
                    vm.specialBetOpen = function(bet){
                        return vm.canBet(vm.settings.gamesStarts, bet);
                    };

                    vm.specialBetsOpen = function(){
                        return ! vm.betClosed(vm.settings.gamesStarts);
                    };

                    vm.betClosed = function(serverStart) {
                        var diff = (serverStart - MSTOCLOSING) - vm.currentTime;
                        return diff < 0;
                    };

                    vm.canBet = function(serverStart, bet) {
                        var diff = (serverStart - MSTOCLOSING) - vm.currentTime;
                        var owner = userService.isOwner(bet.userId);
                        return diff > 0 && owner;
                    };

                    vm.onTimeout = function() {
                        vm.mytimeout = $timeout(vm.onTimeout, UPDATEINTERVAL);
                        vm.currentTime = new Date(new Date(vm.currentTime).getTime() + UPDATEINTERVAL);
                        var timerunning = vm.currentTime.getTime() - vm.startupTime.getTime();
                        if (timerunning > RESETTIMEDIFF && TIMEFROMSERVER) {
                            vm.updateTimeFromServer();
                        }
                    };

                    vm.updateSettings = function() {
                        Restangular.one('em2016/api/settings').get().then(function(settings) {
                            settings.gamesStarts = new Date(settings.gamesStarts);
                            vm.settings = settings;
                            $log.debug(settings);
                        })
                    };

                    vm.updateTimeFromServer = function() {
                        Restangular.one('em2016/api/time').get().then(function(currentTime) {
                            vm.startupTime = new Date(currentTime.serverTime);
                            vm.currentTime = vm.startupTime;
                            $log.debug("updated time from server: "+moment(vm.currentTime).format());
                        })
                    };

                    vm.updateDate = function(date) {
                        TIMEFROMSERVER = false;
                        var nm = moment(date);
                        var om = moment(currentTime);
                        nm.hours(om.hours());
                        nm.minutes(om.minutes());
                        nm.seconds(om.seconds());
                        vm.currentTime = nm.toDate();
                        vm.updateTimeOnServer(vm.currentTime);
                    };

                    vm.updateTime = function(time) {
                        TIMEFROMSERVER = false;
                        var nm = moment(time);
                        var om = moment(currentTime);
                        om.hours(nm.hours());
                        om.minutes(nm.minutes());
                        vm.currentTime = om.toDate();
                        vm.updateTimeOnServer(vm.currentTime);
                    };

                    vm.updateTimeOnServer = function(time) { //TODO fetch time from server after update, then update current time!
                        Restangular.all('em2016/api/time').customPOST({
                            serverTime: time.getTime()
                        }).then(
                            function(success) {
                                toastr.pop('success', "changed time", success);
                            })
                    };


                    vm.resetTime = function() {
                        Restangular.all('em2016/api/time/reset').customPOST().then(
                            function(success) {
                                vm.TIMEFROMSERVER = true;
                                vm.updateTimeFromServer()
                                toastr.pop('success', "reset time", success);
                            })
                    };

                    vm.prettyResult = function(result){
                        if (result.isSet) {
                                return result.goalsTeam1 + ":" + result.goalsTeam2;
                        } else {
                                return "-:-"
                        }  
                    };
                    
                    vm.updateSettings();
                    vm.updateTimeFromServer()
                    $timeout(vm.onTimeout, UPDATEINTERVAL);
                })

        
    .service('userService', function($log, Restangular, $cookies, $state, toastr, FileSaver, Blob) {
        var vm = this;
                
        $log.debug("created userservice");

        var NOUSER = {
            id: -1,
            username: ""
        };
        vm.loggedInUser = NOUSER;
        var authtoken = "";
                
        vm.filter = {
            bet : "all",
            level : "all",
            game: "all"
        };
        
        vm.identical = function(username){
             if (typeof vm.loggedInUser === "undefined" || typeof vm.loggedInUser.username === "undefined") {
                 return false;
             } else {
               return username == vm.loggedInUser.username;  
            }
        };

        vm.login = function(credentials) { //TODO move state back to controller by returning callback/future
            Restangular.all("em2016/api/login").post(credentials).then(
                function(auth) {
                   vm.updateLogin(auth.user, auth["AUTH-TOKEN"]);
                    if (auth.user.hadInstructions) {
               //        $state.transitionTo("admin.registerUser");
                        $state.transitionTo("user.userBets", {
                            username: vm.loggedInUser.username
                        });
                        
                    } else {
                        $state.transitionTo("user.specialBets", {
                            username: vm.loggedInUser.username
                            //http://benfoster.io/blog/ui-router-optional-parameters add new invisible parameter to display instructions popup in specialbets
                        });
                    }
                }
            );
        };

        vm.isOwner = function(id){
             if (typeof vm.loggedInUser === "undefined" || typeof vm.loggedInUser.id === "undefined") {
                return false;
             } else {
                return id == vm.loggedInUser.id;
             } 
        }

        vm.logout = function() {        
            Restangular.all('/em2016/api/logout').customPOST().then(function(result){
                 $log.info("logout without error"); 
            },function(error){
                error.cancelGeneralHandler();
                $log.info("logout with error"); 
            });
            authtoken = "";
            $cookies.remove("AUTH-TOKEN");
            vm.loggedInUser = NOUSER;
            Restangular.setDefaultHeaders();
            $state.transitionTo("login");      
        };

        /**
         * opening a new window looses all info in the new window
         * we grab the cookie containing the auth token and reload the user
         * if cookie not there logout => reset user to default
         */
        vm.reauthenticate = function() {
            $log.debug("reauthenticating")
            if (typeof authtoken === "undefined" || authtoken == "") {
                var auth = $cookies.get("AUTH-TOKEN");
                if (typeof auth !== "undefined") {
                    authtoken = auth;
                    Restangular.setDefaultHeaders({
                        'X-AUTH-TOKEN': auth
                    });
                    Restangular.one('em2016/api/userWithEmail').get().then(function(userWithEmail) {
                        vm.loggedInUser = userWithEmail;
                        vm.filter = userWithEmail.filterSettings;
                    });
                }
            } else {
               vm.logout();
            }
        };
                
        vm.saveFilter = function(){
            Restangular.all('em2016/api/user/filter').customPOST(vm.filter).then(
                function(userWithEmail) {
                    vm.filter = userWithEmail.filterSettings;
                },
                function(error) { 
                    error.cancelGeneralHandler(); 
                    toastr.error("could not save filter settings");
                }
            );   
        }

        /**
         * update user calls this function without auth
         * 
         **/
        vm.updateLogin = function(user, auth) {
            vm.loggedInUser = user;
            vm.filter = user.filterSettings;
            
            if (typeof auth !== "undefined") {
                authtoken = auth;
                Restangular.setDefaultHeaders({
                    'X-AUTH-TOKEN': auth
                });
            }
        };

        vm.isAdmin = function() {
            if (typeof vm.loggedInUser === "undefined" || typeof  vm.loggedInUser.isAdmin === "undefined") {
                return false;
            } else {
                return  vm.loggedInUser.isAdmin;
            }
        };

        vm.isLoggedIn = function() {
            var hasAuth = typeof  authtoken !== "undefined" && authtoken != "";
   /*
            if(hasAuth && (vm.loggedInUser === undefined || vm.loggedInUser == NOUSER)){ //we refresh the user if the user was lost, but cookie is still there
                Restangular.one('em2016/api/userWithEmail').get().then(function(userWithEmail) {
                    vm.loggedInUser = userWithEmail;
                    vm.filter = userWithEmail.filterSettings;
                    return true;
                }, function(err){
                    return false;
                }
               );
            } else {
*/
                return hasAuth ? true : false;
  //          }
        };

        vm.userHadInstructions = function() {
            if (typeof vm.loggedInUser === "undefined" || typeof vm.loggedInUser.hadInstructions === "undefined") {
                return false;
            } else {
                return vm.loggedInUser.hadInstructions;
            }
        };
        
        
        //DOES NOT WORK!!! BLOB is wrong 
        vm.getExcel = function(){
            var exUrl = vm.isLoggedIn() ? '/em2016/api/statistics/excel' : '/em2016/api/statistics/excelAnon'
            
            Restangular.setFullResponse(true).one(exUrl).get().then(function(result){
              //  $log.debug("got excel");
                var cd = result.headers()["content-disposition"];
                var filename = cd.split(" ")[1].split("=")[1];
                var ct = result.headers()["content-type"];
                var data = new Blob([result.data], { type: ct });
                FileSaver.saveAs(data, filename);
             //   return result;
            //})
             
            })      
        };
         
         
        vm.reauthenticate();

    });





})();