import { Component, OnInit, Input } from '@angular/core';

import range from 'lodash/range';

import { BetterdbService } from '../../betterdb.service';
import { Bet } from '../../model/bet';
import { BetterTimerService } from '../../better-timer.service';

@Component({
  selector: 'app-bet-view',
  templateUrl: './bet-view.component.html',
  styleUrls: ['./bet-view.component.css']
})
export class BetViewComponent implements OnInit {
  @Input() bet: Bet
  @Input() serverStart: Date
  enablePoints = true
  disableSave = true
  saveStyle = {}

  points = range(15)

  disabled(): boolean {
      return ! this.enablePoints && this.timerService.canBet(this.serverStart, this.bet)
  }


  constructor(private betterdb: BetterdbService, private timerService: BetterTimerService) { }

  ngOnInit() {
  }

}
