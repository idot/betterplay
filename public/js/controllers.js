/*global define */

'use strict';

define(function() {

/* Controllers */

var controllers = {};

controllers.UsersCtrl = function($log, $scope, $filter, Restangular, $stateParams, ngTableParams ) {
	var queryUsers = Restangular.all('wm2014/api/users');
   
	queryUsers.getList().then(function(users){
	    $scope.allUsers = users;
		setupTable( users, ngTableParams, { 'username': 'asc'}, $scope, $filter );
	});
}

controllers.UsersCtrl.$inject = ['$log', '$scope', '$filter', 'Restangular', '$stateParams', 'ngTableParams' ];

controllers.GamesCtrl = function($log, $scope, $filter, Restangular, $stateParams, ngTableParams ) {
	var queryGames = Restangular.all('wm2014/api/games');
		
	queryGames.getList().then(function(games){
	    $scope.allGames = games;
		setupTable( games, ngTableParams, { 'game.nr': 'asc'}, $scope, $filter );
    });
	
   
}
controllers.GamesCtrl.$inject = ['$log', '$scope', '$filter', 'Restangular', '$stateParams', 'ngTableParams'];


controllers.UserCtrl = function($log, $scope, $filter, Restangular, $stateParams, ngTableParams, toaster ) {
	$scope.stateParams = $stateParams;
	var queryUser = Restangular.one('wm2014/api/user', $scope.stateParams.username);
   
    queryUser.get().then(function(userWithSpAndGB){
		$scope.user = userWithSpAndGB.user;
		$scope.special = userWithSpAndGB.special;
		$scope.gameBets = userWithSpAndGB.gameBets;
		setupTable( $scope.gameBets, ngTableParams, { 'game.game.nr': 'asc'}, $scope, $filter );
    });
	
}
controllers.UserCtrl.$inject = ['$log', '$scope', '$filter', 'Restangular', '$stateParams', 'ngTableParams', 'toaster'];

controllers.GameCtrl = function($log, $scope, $filter, Restangular, $stateParams, ngTableParams ) {
	$scope.stateParams = $stateParams;
	var queryGame = Restangular.one('wm2014/api/game', $scope.stateParams.gamenr);
    
	queryGame.get().then(function(gwtWithBetsPerUser){
		$scope.gwt = gwtWithBetsPerUser.game;
		$scope.betsUsers = gwtWithBetsPerUser.betsUsers;
		setupTable( $scope.betsUsers, ngTableParams, { 'game.game.nr': 'asc'}, $scope, $filter );
	});

}
controllers.GameCtrl.$inject = ['$log', '$scope', '$filter', 'Restangular', '$stateParams', 'ngTableParams'];


controllers.SettingsCtrl = function($log, $scope, $rootScope, $stateParams, Restangular, toaster){
  $scope.stateParams = $stateParams;	
	
  $scope.opened = false;	
		
  $scope.DF = $rootScope.DF;
  
  $scope.setFormat = function(){
	  $rootScope.DF = $scope.DF;
  };
  
  $scope.updateTimeOnServer = function(time){
  	Restangular.all('wm2014/api/time').customPOST( { serverTime: time.getTime() } ).then(
	   function(success){
		    toaster.pop('success', "changed time", success);
	   })
   };	
  
	
  $scope.updateDate = function(){
      $rootScope.TIMEFROMSERVER = false;
	  var nm = moment($scope.global.date);
      var om = moment($rootScope.currentTime);
	  nm.hours(om.hours());
	  nm.minutes(om.minutes());
	  nm.seconds(om.seconds());
	  $rootScope.currentTime = nm.toDate();
	  $scope.updateTimeOnServer($rootScope.currentTime);
  };
  
  $scope.updateTime = function(){
      $rootScope.TIMEFROMSERVER = false;
	  var nm = moment($scope.global.time);
      var om = moment($rootScope.currentTime);
	  om.hours(nm.hours());
	  om.minutes(nm.minutes());
	  $rootScope.currentTime = om.toDate();
	  $scope.updateTimeOnServer($rootScope.currentTime);
  };
  
  
  $scope.resetTime = function(){
      Restangular.all('wm2014/api/time/reset').customPOST().then(
  	   function(success){
	 	    $rootScope.TIMEFROMSERVER = true;
	 	    $rootScope.updateTimeFromServer()
  		    toaster.pop('success', "reset time", success);
  	   })
  };
    
  
  $scope.global = { date : new Date($rootScope.currentTime.getTime()), time: new Date($rootScope.currentTime.getTime())};

  $scope.open = function($event){
	    $event.preventDefault();
	    $event.stopPropagation();
	    $scope.opened = true;
  };

  $scope.updateInterval = $rootScope.UPDATEINTERVAL;

  $scope.setUpdateinterval = function(){
	  $rootScope.UPDATEINTERVAL = $scope.updateInterval;
  };

}
controllers.SettingsCtrl.$inject = ['$log', '$scope','$rootScope','$stateParams', 'Restangular', 'toaster'];



controllers.LoginCtrl = function($log, $scope, $rootScope, $stateParams, Restangular, $state){
	$scope.stateParams = $stateParams;	
		
	$scope.username = "";
	$scope.password = "";
	
	$scope.login = function(){
		var credentials = { 'username': $scope.username, 'password': $scope.password };
	    Restangular.all("wm2014/login").post(credentials).then(
	      function(auth){ 
			  $rootScope.updateLogin(auth.user, auth["AUTH-TOKEN"]);	
			  if(auth.user.hadInstructions){  
			     $state.transitionTo("user.userBets", { username: $scope.username });
			  } else {
				 $state.transitionTo("user.specialBets", { username: $scope.username });
			  } 
	      }
	    );
    };

}
controllers.LoginCtrl.$inject = ['$log', '$scope', '$rootScope', '$stateParams', 'Restangular', '$state'];

controllers.RegisterUserCtrl = function($log, $scope, $rootScope, $stateParams, Restangular, $state, toaster){
	$scope.stateParams = $stateParams;	
	
	var queryUsers = Restangular.all('wm2014/api/users');	
	queryUsers.getList().then(function(users){
	    $scope.allUsers = _.map(users, function(u){ return u.username });
	});	
		
	$scope.setFields = function(){	
	    $scope.username = "";
	    $scope.password1 = "";
		$scope.password2 = "";
    	$scope.email = "";
    };
	
	$scope.uniqueUsername = function(username){
	   	var duplicated = _.find($scope.allUsers, function(u){ return u === username; });
		$log.debug("unique: "+username+" "+duplicated);
	    return !duplicated;			
	};
	
	$scope.signon = function(){
		    var pu = { 'username': $scope.username, 'password': $scope.password1, 'email': $scope.email };
	    	Restangular.all('wm2014/api/user/'+$scope.username).customPUT( pu ).then(
			function(success){
				toaster.pop('success', "registered "+$scope.username); //TODO: email
				$scope.setFields();
			}
		);
	};
	$scope.setFields();
}
controllers.RegisterUserCtrl.$inject = ['$log', '$scope', '$rootScope', '$stateParams', 'Restangular', '$state', 'toaster'];

controllers.EditUserCtrl = function($log, $scope, $rootScope, $stateParams, Restangular, $state, toaster){
	$scope.stateParams = $stateParams;
		
	
	$scope.refreshUser = function(){
	    var queryUser = Restangular.one('wm2014/api/userWithEmail');
        queryUser.get().then(function(userWithEmail){
			$scope.formUser = userWithEmail;
			$rootScope.updateLogin(userWithEmail);
		});
    }
    	
	$scope.pass = { word1: "", word2: ""};
		
	$scope.updatePassword = function(){
		    $log.debug('submitting new password: '+$scope.pass.word1);
		    var pu = { 'password': $scope.pass.word1 };
	    	Restangular.all('wm2014/api/user/'+$scope.formUser.username+'/password').customPOST( pu ).then(
			function(success){
				toaster.pop('success', "changed password");
				$scope.pass.word1 = "";
				$scope.pass.word2 = "";
			}
		);
	};
	
	
	$scope.updateDetails = function(){
	       $log.debug('updating details: ');
		   var u = { firstName: $scope.formUser.firstName, lastName: $scope.formUser.lastName, email: $scope.formUser.email, icontype: $scope.formUser.icontype };	
    	   Restangular.all('wm2014/api/user/'+$scope.formUser.username+'/details').customPOST( u ).then(
		     function(success){
			    toaster.pop('success', "updated user details");
                $scope.refreshUser();
		     }
		  );
	};
	
	$scope.refreshUser();
}
controllers.EditUserCtrl.$inject = ['$log', '$scope', '$rootScope', '$stateParams', 'Restangular', '$state', 'toaster'];

controllers.EditGameCtrl = function($log, $scope, $rootScope, $stateParams, Restangular, $state, toaster) {
    console.log("st: "+$stateParams);
	$scope.stateParams = $stateParams;
	console.log("sd: "+$scope.stateParams);
	var queryGame = Restangular.one('wm2014/api/game', $scope.stateParams.gamenr);
    
	queryGame.get().then(function(gwtWithBetsPerUser){
		$scope.gwt = gwtWithBetsPerUser.game;
	});
	

	$scope.submitResult = function(){
	    var queryBet = Restangular.all('wm2014/api/game/results').customPOST($scope.gwt.game).then(
		    function(success){
		        toaster.pop('success', "updated game", success);
		    }		
		);			
	};
}
controllers.EditGameCtrl.$inject = ['$log', '$scope', '$rootScope', '$stateParams', 'Restangular', '$state', 'toaster'];



controllers.BetCtrl = function($scope, $rootScope, Restangular, toaster){
	$scope.disabled = false;
	
	$scope.saveBet = function(bet){
		$scope.disabled = true;
	    var queryBet = Restangular.all('wm2014/api/bet/'+bet.id).customPOST(bet).then(
		    function(success){
				var game = success.game;
				var betold = success.betold;
				var betnew = success.betnew;
				var show = success.game.game.nr+": "+$scope.prettyBet(betold)+" -> "+$scope.prettyBet(betnew)
				toaster.pop('success', "updated bet ", show);
				bet['marked'] = false;
				$scope.disabled = false;
		    }		
		);			
	};

	$scope.markBet = function(bet){
	    bet['marked'] = true;	
	};

    $scope.withTime = $rootScope.currentTime;

   
	$scope.saveButton = function(bet){
		if(typeof bet.marked === "undefined" || bet.marked == false){
			return "btn btn-default btn-xs";
		}else{
			return "btn btn-warning btn-xs";
		}
	};

	$scope.prettyBet = function(bet){
		if(bet.result.isSet){
			return bet.result.goalsTeam1+":"+bet.result.goalsTeam2;
		}else{
			return "-:-"
		}
	}
	
}
controllers.BetCtrl.$inject = ['$scope','$rootScope', 'Restangular', 'toaster'];

controllers.UserSpecialBetsCtrl = function($log, $scope, $rootScope, $filter, $stateParams, Restangular, $state, toaster, ngTableParams ) {	
	     $scope.stateParams = $stateParams;
		 
         Restangular.one('wm2014/api/user', $scope.stateParams.username).one('specialBets').get().then(
			 function(success){
			     // join must make to usable structure
				 $scope.user = success.user;
				 $scope.templatebets = success.templatebets;
				 $scope.noInstructions = ! $scope.user.hadInstructions;
			 }
		 );		 
		 		 
		 $scope.change = function(templatebet){
		     switch(templatebet.template.itemType){
		     	case "team": $state.transitionTo("user.specialBetsspecialTeams", { username: $scope.user.username, id: templatebet.bet.id });  break;
			    case "player": $state.transitionTo("user.specialBetsspecialPlayers", { username: $scope.user.username, id: templatebet.bet.id });  break;
			    default:  toaster.pop('error', "someting is wrong!", "could not decide if its bet for player or team. Please inform somebody by email");  
		     }
		 };
}
controllers.UserSpecialBetsCtrl.$inject = ['$log', '$scope', '$rootScope', '$filter', '$stateParams', 'Restangular', '$state', 'toaster', 'ngTableParams'];	



controllers.EditUserSpecialPlayerCtrl = function($log, $scope, $rootScope, $filter, $stateParams, Restangular, $state, toaster, ngTableParams, specialBetService ) {	
	     $scope.stateParams = $stateParams;
	     		 
		 $scope.betId = $stateParams.id;		 
				 
		 Restangular.one('wm2014/api/user', $scope.stateParams.username).one('specialBets').get().then(
		     function(success){
				 $scope.user = success.user;
				 var templatebets = success.templatebets;
				 var tb = _.filter(templatebets, function(b){ return b.bet.id == $scope.betId; })[0];
				 $scope.tb = tb;
		     }		 
		 );
		 
         Restangular.all('wm2014/api/players').getList().then(
			 function(success){
				 var forFilter = _.map(success, function(pt){ pt.name = pt.player.name; pt.tname = pt.team.name; return pt; });
				 $scope.playerWithTeams = forFilter;
				 setupTable( $scope.playerWithTeams, ngTableParams, { 'player.name': 'asc' }, $scope, $filter );
			 }
		 );		 
		 
		 $scope.selectPlayer = function(){
		 	specialBetService.saveSelected($scope.tb.bet, $scope.user, $scope.playerWithTeams);	
		 };
}
controllers.EditUserSpecialPlayerCtrl.$inject = ['$log', '$scope', '$rootScope', '$filter', '$stateParams', 'Restangular', '$state', 'toaster', 'ngTableParams','specialBetService'];	

controllers.EditUserSpecialTeamCtrl = function($log, $scope, $rootScope, $filter, $stateParams, Restangular, $state, toaster, ngTableParams, specialBetService ) {	
	     $scope.stateParams = $stateParams;
		 
		 $scope.betId = $stateParams.id;
		 
        
		 Restangular.one('wm2014/api/user', $scope.stateParams.username).one('specialBets').get().then(
		     function(success){
				 $scope.user = success.user;
				 var templatebets = success.templatebets;
				 var tb = _.filter(templatebets, function(b){ return b.bet.id == $scope.betId; })[0];
				 $scope.tb = tb;
		     }		 
		 );
		 
		 Restangular.all('wm2014/api/teams').getList().then(
			 function(success){
				 $scope.teams = success;
				 setupTable( $scope.teams, ngTableParams, { 'name': 'asc'}, $scope, $filter );
			 }
		 );		 
		 
		 $scope.selectTeam = function(){
	     	 specialBetService.saveSelected($scope.tb.bet, $scope.user, $scope.teams)
		 };
		  
}
controllers.EditUserSpecialTeamCtrl.$inject = ['$log', '$scope', '$rootScope', '$filter', '$stateParams', 'Restangular', '$state', 'toaster', 'ngTableParams', 'specialBetService'];	


controllers.PlotCtrl = function($scope){

     $scope.exampleData = [
             {
                  "key": "Series 1",
                  "values": [ [ 1025409600000 , 0] , [ 1028088000000 , -6.3382185140371] , [ 1030766400000 , -5.9507873460847] , [ 1033358400000 , -11.569146943813] , [ 1036040400000 , -5.4767332317425] , [ 1038632400000 , 0.50794682203014] , [ 1041310800000 , -5.5310285460542] , [ 1043989200000 , -5.7838296963382] , [ 1046408400000 , -7.3249341615649] , [ 1049086800000 , -6.7078630712489] , [ 1051675200000 , 0.44227126150934] , [ 1054353600000 , 7.2481659343222] , [ 1056945600000 , 9.2512381306992] , [ 1059624000000 , 11.341210982529] , [ 1062302400000 , 14.734820409020] , [ 1064894400000 , 12.387148007542] , [ 1067576400000 , 18.436471461827] , [ 1070168400000 , 19.830742266977] , [ 1072846800000 , 22.643205829887] , [ 1075525200000 , 26.743156781239] , [ 1078030800000 , 29.597478802228] , [ 1080709200000 , 30.831697585341] , [ 1083297600000 , 28.054068024708] , [ 1085976000000 , 29.294079423832] , [ 1088568000000 , 30.269264061274] , [ 1091246400000 , 24.934526898906] , [ 1093924800000 , 24.265982759406] , [ 1096516800000 , 27.217794897473] , [ 1099195200000 , 30.802601992077] , [ 1101790800000 , 36.331003758254] , [ 1104469200000 , 43.142498700060] , [ 1107147600000 , 40.558263931958] , [ 1109566800000 , 42.543622385800] , [ 1112245200000 , 41.683584710331] , [ 1114833600000 , 36.375367302328] , [ 1117512000000 , 40.719688980730] , [ 1120104000000 , 43.897963036919] , [ 1122782400000 , 49.797033975368] , [ 1125460800000 , 47.085993935989] , [ 1128052800000 , 46.601972859745] , [ 1130734800000 , 41.567784572762] , [ 1133326800000 , 47.296923737245] , [ 1136005200000 , 47.642969612080] , [ 1138683600000 , 50.781515820954] , [ 1141102800000 , 52.600229204305] , [ 1143781200000 , 55.599684490628] , [ 1146369600000 , 57.920388436633] , [ 1149048000000 , 53.503593218971] , [ 1151640000000 , 53.522973979964] , [ 1154318400000 , 49.846822298548] , [ 1156996800000 , 54.721341614650] , [ 1159588800000 , 58.186236223191] , [ 1162270800000 , 63.908065540997] , [ 1164862800000 , 69.767285129367] , [ 1167541200000 , 72.534013373592] , [ 1170219600000 , 77.991819436573] , [ 1172638800000 , 78.143584404990] , [ 1175313600000 , 83.702398665233] , [ 1177905600000 , 91.140859312418] , [ 1180584000000 , 98.590960607028] , [ 1183176000000 , 96.245634754228] , [ 1185854400000 , 92.326364432615] , [ 1188532800000 , 97.068765332230] , [ 1191124800000 , 105.81025556260] , [ 1193803200000 , 114.38348777791] , [ 1196398800000 , 103.59604949810] , [ 1199077200000 , 101.72488429307] , [ 1201755600000 , 89.840147735028] , [ 1204261200000 , 86.963597532664] , [ 1206936000000 , 84.075505208491] , [ 1209528000000 , 93.170105645831] , [ 1212206400000 , 103.62838083121] , [ 1214798400000 , 87.458241365091] , [ 1217476800000 , 85.808374141319] , [ 1220155200000 , 93.158054469193] , [ 1222747200000 , 65.973252382360] , [ 1225425600000 , 44.580686638224] , [ 1228021200000 , 36.418977140128] , [ 1230699600000 , 38.727678144761] , [ 1233378000000 , 36.692674173387] , [ 1235797200000 , 30.033022809480] , [ 1238472000000 , 36.707532162718] , [ 1241064000000 , 52.191457688389] , [ 1243742400000 , 56.357883979735] , [ 1246334400000 , 57.629002180305] , [ 1249012800000 , 66.650985790166] , [ 1251691200000 , 70.839243432186] , [ 1254283200000 , 78.731998491499] , [ 1256961600000 , 72.375528540349] , [ 1259557200000 , 81.738387881630] , [ 1262235600000 , 87.539792394232] , [ 1264914000000 , 84.320762662273] , [ 1267333200000 , 90.621278391889] , [ 1270008000000 , 102.47144881651] , [ 1272600000000 , 102.79320353429] , [ 1275278400000 , 90.529736050479] , [ 1277870400000 , 76.580859994531] , [ 1280548800000 , 86.548979376972] , [ 1283227200000 , 81.879653334089] , [ 1285819200000 , 101.72550015956] , [ 1288497600000 , 107.97964852260] , [ 1291093200000 , 106.16240630785] , [ 1293771600000 , 114.84268599533] , [ 1296450000000 , 121.60793322282] , [ 1298869200000 , 133.41437346605] , [ 1301544000000 , 125.46646042904] , [ 1304136000000 , 129.76784954301] , [ 1306814400000 , 128.15798861044] , [ 1309406400000 , 121.92388706072] , [ 1312084800000 , 116.70036100870] , [ 1314763200000 , 88.367701837033] , [ 1317355200000 , 59.159665765725] , [ 1320033600000 , 79.793568139753] , [ 1322629200000 , 75.903834028417] , [ 1325307600000 , 72.704218209157] , [ 1327986000000 , 84.936990804097] , [ 1330491600000 , 93.388148670744]]
     }];

}
controllers.PlotCtrl.$inject = ['$scope'];

return controllers;

});





