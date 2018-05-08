import { Component, OnInit, Input } from '@angular/core';

import range from 'lodash/range';

import { BetterdbService } from '../../betterdb.service';
import { Bet } from '../../model/bet';

@Component({
  selector: 'app-bet-view',
  templateUrl: './bet-view.component.html',
  styleUrls: ['./bet-view.component.css']
})
export class BetViewComponent implements OnInit {
  @Input() bet: Bet;
  enablePoints = true;
  disableSave = true;
  saveStyle = {};

  points = range(15);

  disabled(bet: Bet): boolean {
   return false //  ! this.enablePoints && this.betterdb.canBet(bet);
  }


  constructor(private betterdb: BetterdbService) { }

  ngOnInit() {
  }

}
