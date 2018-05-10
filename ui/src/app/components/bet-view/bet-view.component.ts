import { Component, OnInit, Input } from '@angular/core';

import range from 'lodash/range';

import { BetterdbService } from '../../betterdb.service';
import { Bet, Result } from '../../model/bet';
import { BetterTimerService } from '../../better-timer.service';

interface MarkableBet extends Bet {
   marked: boolean
}

@Component({
  selector: 'bet-view',
  templateUrl: './bet-view.component.html',
  styleUrls: ['./bet-view.component.css']
})
export class BetViewComponent implements OnInit {
  @Input() bet: Bet
  @Input() serverStart: Date
  @Input() gameresult: Result

  enablePoints = true
  disableSave = true
  saveStyle = {}

  points = range(15)
  markedBet = null


  disabled(): boolean {
      return ! this.enablePoints && this.timerService.canBet(this.serverStart, this.bet)
  }

  markBet(bet: Bet){
  //  bet.mark = true
  }

  constructor(private betterdb: BetterdbService, private timerService: BetterTimerService) { }

  ngOnInit() {
  }

}
