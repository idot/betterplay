import { Injectable } from '@angular/core';

import * as moment from 'moment';

import { UserService } from '../service/user.service';
import { BetterTimerService } from '../better-timer.service';
import { Bet } from '../model/bet';
import { BetterdbService } from '../betterdb.service';

@Injectable({
  providedIn: 'root'
})
export class BetService {

  constructor(private userService: UserService, private timeService: BetterTimerService, private betterdb: BetterdbService) {}

  //TODO:: property of Game!!
  //time before game start that bet closes
  private MSTOCLOSING = 60 * 60 * 1000 //in ms

  specialBetsOpen(): boolean {
     return ! this.betClosed(this.betterdb.getGamesStart())
  }

  betClosed(serverStart: Date): boolean {
      return this.diffToStart(serverStart) < 0
  }

  canBet(serverStart, bet): boolean {
      const diff = this.diffToStart(serverStart)
      const owner = this.userService.isOwner(bet.userId)
      return diff > 0 && owner
  }


  diffToStart(serverStart: Date): number {
     return (serverStart.getTime() - this.MSTOCLOSING) - this.timeService.getTimeMillis()
  }

  timeLeft(serverStart: Date): string {
     //boolean true add in/ago
     //negative values = ago
     //positive values = in
     const s = moment.duration(this.diffToStart(serverStart), "milliseconds").humanize(false)
     return s
  }

}
