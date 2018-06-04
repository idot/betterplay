import { Component, OnInit } from '@angular/core';
import { NGXLogger } from 'ngx-logger';
import { ActivatedRoute } from '@angular/router';
import { Router } from '@angular/router';

import { switchMap } from 'rxjs/operators';
import { ParamMap } from '@angular/router';
import { Observable, EMPTY } from 'rxjs';

import { BetterdbService } from '../../betterdb.service';
import { UserService } from '../../service/user.service';
import { UserWithBets } from '../../model/bet';
import { SpecialBet } from '../../model/specialbet';
import { BetService } from '../../service/bet.service';
import { User } from '../../model/user';
import { BetterTimerService } from '../../better-timer.service';




export interface Element {
  name: string;
  position: number;
  weight: number;
  symbol: string;
}

@Component({
  selector: 'user-specialbets',
  templateUrl: './user-specialbets.component.html',
  styleUrls: ['./user-specialbets.component.css']
})
export class UserSpecialbetsComponent implements OnInit {
  user$: Observable<UserWithBets>

  constructor(private logger: NGXLogger, private route: ActivatedRoute, private router: Router, private betService: BetService, private userService: UserService, private betterdb: BetterdbService, private timeService: BetterTimerService) { }

  private displayColumnsBeforeGameStartAnon = ['description', 'reward', 'prediction']
  private displayedColumnsBeforeGameStart = ['description', 'reward', 'prediction', 'setPrediction']
  private displayedColumnsAfterGameStart = ['description', 'reward', 'result', 'prediction', 'points']
  private displayedColumnsAfterGameStartAdmin = ['description', 'reward', 'result', 'prediction', 'points']

  displayedColumns: string[] = this.displayColumnsBeforeGameStartAnon



setDisplayedColumns(username: string){
     this.displayedColumns = this.displayColumnsBeforeGameStartAnon
     if(this.userService.isIdentical(username)){
         if(this.betService.specialBetsOpen()){
             this.displayedColumns = this.displayedColumnsBeforeGameStart
         } else {
            if(this.userService.isAdmin()){
                this.displayedColumns = this.displayedColumnsAfterGameStartAdmin
            } else {
                this.displayedColumns = this.displayedColumnsAfterGameStart
            }
         }
     }
}

canBet(username: string): boolean {
    return this.userService.isIdentical(username) && this.betService.specialBetsOpen()
}

createUrl(user: User, bet: SpecialBet, result: boolean): string {
    //const id = result ? bet.template.id : bet.bet.id
    const id = bet.bet.id
    return `user/${user.username}/special/${bet.template.itemType}/${id}`
}

setResult(user: User, bet: SpecialBet){
      this.router.navigate([this.createUrl(user, bet, true),{ result : 'true' }])
}

setPrediction(user: User, bet: SpecialBet){
     this.router.navigate([this.createUrl(user, bet, false)])
}

ngOnInit() {
    this.user$ = this.route.paramMap.pipe(
        switchMap((params: ParamMap) => {
           const username = params.get('username')
           this.logger.debug(`extracted ${username}`)
           if(username){
               this.setDisplayedColumns(username)
             return this.betterdb.getBetsForUser(username)
           } else {
             return EMPTY
           }
       })
    )
  }

}
