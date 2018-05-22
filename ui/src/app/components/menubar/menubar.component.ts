import { Component, OnInit } from '@angular/core';

import { ActivatedRoute } from '@angular/router';

import { BetterdbService } from '../../betterdb.service';
import { BetterTimerService } from '../../better-timer.service';
import { UserService } from '../../service/user.service';

@Component({
  selector: 'menubar',
  templateUrl: './menubar.component.html',
  styleUrls: ['./menubar.component.css']
})
export class MenubarComponent implements OnInit {

  constructor(private userService: UserService, private timerService: BetterTimerService, private betterdb: BetterdbService, private route: ActivatedRoute) { }

  ngOnInit() {
  }


        DF(){
            return this.timerService.getDateFormatter;
        }

        specialDisabled(){
          //  return $state.includes("**.specialBets.**")  &&  $stateParams.username !== undefined &&  $stateParams.username == vm.userService.loggedInUser.username;
          return false
        }
        gamesDisabled(){
            return false //return $state.includes("games");
        }
        betsDisabled(){
            return false//
            ///return $state.includes("user.userBets") &&  $stateParams.username !== undefined &&  $stateParams.username == vm.userService.loggedInUser.username;
        }
        usersDisabled(){
              return false//         return $state.includes("users");
        }
        registerDisabled(){
           return false//  return $state.includes("admin.registerUser");
        }
        statsDisabled(){
            return //false return $state.includes("statistics.plots");
        }
        loginDisabled(){
            return //false return $state.includes("login");
        }

        isDebug(){
            return false //return betterSettings.isDebug();
        }

        currentTime(){
            return this.timerService.getTime();
        }

        //vm.large = $mdMedia('gt-xs');

}
