import { Component, OnInit } from '@angular/core';

import { ActivatedRoute } from '@angular/router';

import { BetterdbService } from '../../betterdb.service';
import { BetterTimerService } from '../../better-timer.service';
import { UserService } from '../../service/user.service';
import { Router } from '@angular/router';

@Component({
  selector: 'menubar',
  templateUrl: './menubar.component.html',
  styleUrls: ['./menubar.component.css']
})
export class MenubarComponent implements OnInit {

  constructor(private userService: UserService, private router: Router, private timerService: BetterTimerService, private betterdb: BetterdbService, private route: ActivatedRoute) { }

        ngOnInit() {
        }


        DF(){
            return this.timerService.getDateFormatter;
        }

        isDebug(){
           return this.betterdb.getSettings().debug
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
            return false //false return $state.includes("statistics.plots");
        }
        loginDisabled(){
            return false //false return $state.includes("login");
        }
        mailDisabled(){
            return false
        }
        settingsDisabled(){
            return false
        }
        currentTime(){
            return this.timerService.getTime();
        }

        logout(){
           this.userService.logout()
           this.router.navigate([`/login`])
        }

        //vm.large = $mdMedia('gt-xs');

}
