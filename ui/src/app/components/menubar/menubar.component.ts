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

  constructor( private userService: UserService, private router: Router, private timerService: BetterTimerService, private betterdb: BetterdbService, private route: ActivatedRoute) { }
        opened = true

        ngOnInit() {
        }


        DF(){
            return this.timerService.getDateFormatter;
        }

        isDebug(){
           return this.betterdb.getSettings().debug
        }

        specialDisabled(){
           return this.router.url.startsWith("/special")
        }
        gamesDisabled(){
           return this.router.url.startsWith("/games")
        }
        betsDisabled(){
            return this.router.url.startsWith("/bets")
        }
        usersDisabled(){
            return this.router.url.startsWith("/users")
        }
        registerDisabled(){
           return this.router.url.startsWith("/admin/register")
        }
        statsDisabled(){
            return this.router.url.startsWith("/statistics")
        }
        rulesDisabled(){
            return this.router.url.startsWith("/rules")
        }
        loginDisabled(){
          return this.router.url.startsWith("/login")
        }
        passwordDisabled(){
            return this.router.url.startsWith("/password")
        }
        mailDisabled(){
            return this.router.url.startsWith("/admin/mail")
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
