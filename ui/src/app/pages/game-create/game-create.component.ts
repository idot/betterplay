import { Component, OnInit } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { UserService } from '../../service/user.service';
import { BetterdbService, CreatedGame } from '../../betterdb.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';

import sortBy from 'lodash/sortBy';
import range from 'lodash/range';
import { Team, Level, GameWithTeams, Game } from '../../model/bet';
import { Observable } from 'rxjs';
import { ToastComponent } from '../../components/toast/toast.component';



@Component({
  selector: 'game-create',
  templateUrl: './game-create.component.html',
  styleUrls: ['./game-create.component.css']
})
export class GameCreateComponent implements OnInit {

  constructor(private userService: UserService, public betterdb: BetterdbService, private snackBar: MatSnackBar, private router: Router) { }

  startView = 'month'
  startDate = new Date()
  date = new Date()
  hours = range(0, 23)
  minutes = range(0, 60)

  hour = 12
  minute = 0

  team1: Team
  team2: Team
  level: Level

  levels$: Observable<Level[]>
  teams$: Observable<Team[]>

  ngOnInit() {
     this.levels$ = this.betterdb.getLevels()
     this.teams$ = this.betterdb.getTeams()
  }

  getTime(): Date {
     const cdate = this.date
     cdate.setHours(this.hour, this.minute)
     return cdate
  }

  sortedTeams(teams: Team[]): Team[] {
     return sortBy(teams, function(team: Team){ return team.name })
  }

  getGame(): CreatedGame {
    return {
      team1: this.team1.name,
      team2: this.team2.name,
      level: this.level.level,
      localStart: this.getTime(),
      serverStart: this.getTime()
    }
  }

  create(){
    const game = this.getGame()
    this.betterdb.createGame(game).subscribe( result => {
      if(result['error']){
          this.snackBar.openFromComponent(ToastComponent, { data: { message: result['error'], level: "error"}})
      } else {
          this.snackBar.openFromComponent(ToastComponent, { data: { message: "created game, but please check again if everything is correct", level: "ok"}})
          this.router.navigate([`/game/${result.gwt.game.nr}`])
      }
    })
  }

}
