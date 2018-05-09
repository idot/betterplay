import { Component, OnInit, Input } from '@angular/core';

import { BetterdbService } from '../../betterdb.service';
import { Game, Team, GameWithTeams } from '../../model/bet';
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

  private disabled = false
  private DF = this.timerService.getDateFormatter()

  timeLeft = this.timerService.timeLeft

  constructor(private betterdb: BetterdbService, private timerService: BetterTimerService) { }



  ngOnInit() {
  }

}
