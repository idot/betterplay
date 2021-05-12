import { Component, OnInit, Input } from '@angular/core';

import { BetterdbService } from '../../betterdb.service';
import { Game, Team, GameWithTeams, Result } from '../../model/bet';
import { BetService } from '../../service/bet.service';
import { BetterTimerService } from '../../better-timer.service';
import { ViewResult } from '../bet-view/bet-view.component';
import range from 'lodash/range';
import isNumber from 'lodash/isNumber'
import clone from 'lodash/clone'
import { UserService } from '../../service/user.service';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ToastComponent } from '../toast/toast.component';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { GameChartComponent } from '../game-chart/game-chart.component';

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

  private DF = this.timeService.getDateFormatter()

  result: ViewResult = { 'team1' :  "-", 'team2' : "-", 'isSet' : false }

  points = range(15)

  result2view(result: Result): ViewResult {
    return { team1: result.goalsTeam1, team2: result.goalsTeam2, isSet: result.isSet }
  }

  resultSet(){
    return this.result.isSet
  }

  viewwidth(){
    const width = this.fullcontent ? "350px" : "320px"
    return { 'width' :  width }
  }

  disabled(): boolean {
     return  ! this.userService.isAdmin() || ! this.gameClosed()
  }

  gameClosed(): boolean {
    const start = this.gwt.game.serverStart
    return this.betService.betClosed(start)
  }

  prettyResult(): string {
    if (this.gwt.game.result.isSet) {
            return this.gwt.game.result.goalsTeam1 + ":" + this.gwt.game.result.goalsTeam2
    } else {
            return "-:-"
    }
  }

  allowSetResult(): boolean {
     return ! this.disabled()
  }

  constructor(public dialog: MatDialog, private userService: UserService, private betterdb: BetterdbService, private betService: BetService, private timeService: BetterTimerService, public snackBar: MatSnackBar) { }

  checkSubmissionErrors(): string[] {
     const errors : string[] = []
     if(! isNumber(this.result.team1)){
        errors.push(" team1 ")
     }
     if(! isNumber(this.result.team2)){
        errors.push(" team2 ")
     }
     return errors
  }

  checkSubmission(): string {
     var errors = this.checkSubmissionErrors().join(",")
     if(errors.length > 0){
        return "Please set points for "+errors+"!";
     } else {
        return ""
     }
  }

  /// /api/game/:nr
  saveResult(){
      var error = this.checkSubmission()
      if(error != ""){
         this.snackBar.openFromComponent(ToastComponent, { data: { message: error, level: "error"}})
         return
      }
      const result: Result = { goalsTeam1: parseInt(this.result.team1.toString()), goalsTeam2: parseInt(this.result.team2.toString()), isSet: true }
      const game = clone(this.gwt.game)
      game.result = result
      this.betterdb.saveGameResult(game).pipe(
      //   tap(_ => this.logger.debug(`saving bet ${bet.id} ${bet.result}`)),
         catchError( (err, bet) => {
            return of({ error: err.message})
         })
      ).subscribe( response => {
        if(response['error']){
          this.snackBar.openFromComponent(ToastComponent, { data: { message: `could not update game\n ${response['error']}`, level: "error"}})
        }else{
           this.gwt.game = game
           this.snackBar.openFromComponent(ToastComponent, { data: { message: "updated game", level: "ok"}})
        }
      })
  }

  ngOnInit() {
    if(this.gwt.game.result.isSet){
       this.result = this.result2view(this.gwt.game.result)
    }
  }

  openChart(){
    this.betterdb.getGameStatistics(this.gwt.game.id, this.gwt.team1.name, this.gwt.team2.name).subscribe( data => {
      let dialogRef = this.dialog.open(GameChartComponent , {
            width: '320px',
            data: data
      })
    })
  }

}
