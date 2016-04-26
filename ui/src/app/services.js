(function() {
        'use strict';

        angular
            .module('ui')
            .value('version', '0.1')
            .factory('specialBetService', function($state, Restangular, toaster, _) {
                return {
                    getSpecialBet: function(betId, username) {
                        return Restangular.one('wm2014/api/user', username).one('specialBets').get().then(
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

                    saveSelected: function(bet, user, selectedList) {
                        var selected = _.filter(selectedList, function(t) {
                            return t.selected;
                        })[0];
                        bet.prediction = selected.name;
                        Restangular.all('wm2014/api/specialBet').customPOST(bet).then(
                            function(success) {
                                if (!user.hadInstructions) {
                                    Restangular.all('wm2014/api/userhadinstructions').customPOST().then(
                                        function(success) {
                                            toaster.pop('success', "Congratulations " + user.username + "!", "You have placed your first special bet.\nPlease don't forget to place all special bets until start of the games.");
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
                    getStats: function(templateId) {
                        return Restangular.one('wm2014/api/specialBets', templateId).get().then(
                            function(success) {
                                var template = success.template;
                                var grouped = _.groupBy(success.bets, function(b) {
                                    return b.prediction;
                                });
                                var bets = _.map(grouped, function(v, k) {
                                    var item = k.toString() == "" ? "undecided" : k.toString();
                                    var arr = [item, v.length];
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
            .service('betterSettings', function(Restangular, $timeout, toaster, moment, userService) {
                   var vm = this;
                    var startupTime = new Date();
                    var currentTime = new Date();

                    //should we fetch the time from the server or take the interactively set time
                    var TIMEFROMSERVER = true;

                    //format for time display
                    var DF = 'MM/dd HH:mm';

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
                        var diff = (serverStart - MSTOCLOSING) - currentTime;
                        var s = moment.duration(diff, "milliseconds").humanize(true);
                        return s;
                    };

                    vm.betClosed = function(serverStart) {
                        var diff = (serverStart - MSTOCLOSING) - currentTime;
                        return diff < 0;
                    };

                    vm.canBet = function(serverStart, bet) {
                        var diff = (serverStart - MSTOCLOSING) - currentTime;
                        var owner = userService.isOwner(bet.userId);
                        return diff > 0 && owner;
                    };

                    vm.onTimeout = function() {
                        vm.mytimeout = $timeout(vm.onTimeout, UPDATEINTERVAL);
                        currentTime = new Date(new Date(currentTime).getTime() + UPDATEINTERVAL);
                        var timerunning = currentTime.getTime() - startupTime.getTime();
                        if (timerunning > RESETTIMEDIFF && TIMEFROMSERVER) {
                            vm.updateTimeFromServer();
                        }
                    };

                    vm.updateSettings = function() {
                        Restangular.one('wm2014/api/settings').get().then(function(settings) {
                            vm.settings = settings;
                        })
                    };

                    vm.updateTimeFromServer = function() {
                        Restangular.one('wm2014/api/time').get().then(function(currentTime) {
                            vm.startupTime = new Date(currentTime.serverTime);
                            vm.currentTime = vm.startupTime;
                        })
                    };

                    vm.updateDate = function(date) {
                        TIMEFROMSERVER = false;
                        var nm = moment(date);
                        var om = moment(currentTime);
                        nm.hours(om.hours());
                        nm.minutes(om.minutes());
                        nm.seconds(om.seconds());
                        currentTime = nm.toDate();
                        vm.updateTimeOnServer(currentTime);
                    };

                    vm.updateTime = function(time) {
                        TIMEFROMSERVER = false;
                        var nm = moment(time);
                        var om = moment(currentTime);
                        om.hours(nm.hours());
                        om.minutes(nm.minutes());
                        currentTime = om.toDate();
                        vm.updateTimeOnServer(currentTime);
                    };

                    vm.updateTimeOnServer = function(time) { //TODO fetch time from server after update, then update current time!
                        Restangular.all('em2016/api/time').customPOST({
                            serverTime: time.getTime()
                        }).then(
                            function(success) {
                                toaster.pop('success', "changed time", success);
                            })
                    };


                    vm.resetTime = function() {
                        Restangular.all('em2016/api/time/reset').customPOST().then(
                            function(success) {
                                vm.TIMEFROMSERVER = true;
                                vm.updateTimeFromServer()
                                toaster.pop('success', "reset time", success);
                            })
                    };

                    vm.updateTimeFromServer()
                    $timeout(vm.onTimeout, UPDATEINTERVAL);
                })

        
    .service('userService', function(Restangular, $cookies, $state) {
        var vm = this;

        var NOUSER = {
            id: -1,
            username: ""
        };
        var loggedInUser = NOUSER;
        var authtoken = "";

        vm.login = function(credentials) { //TODO move state back to controller by returning callback/future
            Restangular.all("em2016/login").post(credentials).then(
                function(auth) {
                    vm.updateLogin(auth.user, auth["AUTH-TOKEN"]);
                    if (auth.user.hadInstructions) {
                        $state.transitionTo("user.userBets", {
                            username: vm.username
                        });
                    } else {
                        $state.transitionTo("user.specialBets", {
                            username: vm.username
                        });
                    }
                }
            );
        };

        vm.isOwner = function(id){
             if (typeof loggedInUser === "undefined" || typeof loggedInUser.id === "undefined") {
                return false;
             } else {
                return id == loggedInUser.id;
             } 
        }

        vm.logout = function() {
            vm.loggedInUser = NOUSER;
            authtoken = "";
            $cookies.remove("AUTH-TOKEN");
            Restangular.setDefaultHeaders();
        };

        /**
         * opening a new window looses all info in the new window
         * we grab the cookie containing the auth token and reload the user
         * if cookie not there logout => reset user to default
         */
        vm.reauthenticate = function() {
            if (typeof authtoken === "undefined" || authtoken == "") {
                var auth = $cookies.get("AUTH-TOKEN");
                if (typeof auth !== "undefined") {
                    authtoken = auth;
                    Restangular.setDefaultHeaders({
                        'X-AUTH-TOKEN': auth
                    });
                    Restangular.one('wm2014/api/userWithEmail').get().then(function(userWithEmail) {
                        loggedInUser = userWithEmail;
                    });
                }
            } else {
               vm.logout();
            }
        };

        /**
         * update user calls this function without auth
         * 
         **/
        vm.updateLogin = function(user, auth) {
            loggedInUser = user;
            if (typeof auth !== "undefined") {
                authtoken = auth;
                Restangular.setDefaultHeaders({
                    'X-AUTH-TOKEN': auth
                });
            }
        };

        vm.isAdmin = function() {
            if (typeof loggedInUser === "undefined" || typeof loggedInUser.isAdmin === "undefined") {
                return false;
            } else {
                return loggedInUser.isAdmin;
            }
        };

        vm.isLoggedIn = function() {
            return typeof authtoken !== "undefined" && authtoken != "";
        };

        vm.userHadInstructions = function() {
            if (typeof loggedInUser === "undefined" || typeof loggedInUser.hadInstructions === "undefined") {
                return false;
            } else {
                return loggedInUser.hadInstructions;
            }
        };

        vm.reauthenticate();

    });





})();