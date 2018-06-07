import { Component, OnInit, Input } from '@angular/core';

import { BetterdbService } from '../../betterdb.service';
import { Game, Team, GameWithTeams } from '../../model/bet';
import { BetService } from '../../service/bet.service';
import { BetterTimerService } from '../../better-timer.service';


@Component({
  selector: 'game-view',
  templateUrl: './game-view.component.html',
  styleUrls: ['./game-view.component.css']
})
export class GameViewComponent implements OnInit {
  @Input() gwt: GameWithTeams
  @Input() allowedit: boolean
  @Input() fullcontent: boolean
  @Input() number: boolean

  private disabled = false
  private DF = this.timeService.getDateFormatter()



  gameClosed(): boolean {
    const start = this.gwt.game.serverStart
    return this.betService.betClosed(start)
  }

  prettyResult(): String {
    return "TODO"
  }
  allowEdit(): boolean {
    return true
  }

  constructor(private betterdb: BetterdbService, private betService: BetService, private timeService: BetterTimerService) { }



  ngOnInit() {
  }

}
