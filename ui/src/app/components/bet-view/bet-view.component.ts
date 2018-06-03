import { Component, OnInit, Input } from '@angular/core';
import {MatSnackBar} from '@angular/material';

import range from 'lodash/range';
import isNumber from 'lodash/isNumber'
import clone from 'lodash/clone'
import { BetterdbService } from '../../betterdb.service';
import { Bet, Result } from '../../model/bet';
import { BetService } from '../../service/bet.service';
import { ToastComponent } from '../toast/toast.component';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';


export interface ViewResult {
  team1: number | string
  team2: number | string
  isSet: boolean
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

  result: ViewResult = { 'team1' :  "hidden", 'team2' : "hidden", 'isSet' : false }


  disabled(): boolean {
      return ! this.enablePoints && this.betService.canBet(this.serverStart, this.bet)
  }

  changed(){
      this.saveStyleValue()
  }

  constructor(private betterdb: BetterdbService, private betService: BetService, public snackBar: MatSnackBar) { }

  ngOnInit() {
     if(this.bet.result){
        if(this.bet.result.isSet){
            this.result = { team1: this.bet.result.goalsTeam1, team2: this.bet.result.goalsTeam2, isSet: this.bet.result.isSet }
        } else {
            if(this.betService.canBet(this.serverStart, this.bet)){
                this.result = { team1: '-', team2: '-', isSet: false }
            } else {
                this.result = { team1: 'hidden', team2: 'hidden', isSet: false }
            }
       }
     }
  }

  checkSubmission(): string {
     const error : string[] = []
     if(! isNumber(this.result.team1)){
        error.push(" team1 ")
     }
     if(! isNumber(this.result.team2)){
        error.push(" team2 ")
     }
     var errors = error.join(",")
     if(errors.length > 0){
        return "Please set points for "+errors+"!";
     } else {
        return ""
     }
  }

  saveBet(){
    console.log("saving")
    var error = this.checkSubmission()
    if(error != ""){
       this.snackBar.openFromComponent(ToastComponent, { data: { message: error, level: "error"}})
    }
    const result: Result = { goalsTeam1: parseInt(this.result.team1.toString()), goalsTeam2: parseInt(this.result.team2.toString()), isSet: true }
    const bet = clone(this.bet)
    bet.result = result
    this.betterdb.saveBet(bet).pipe(
    //   tap(_ => this.logger.debug(`saving bet ${bet.id} ${bet.result}`)),
       catchError( (err, bet) => {
          this.snackBar.openFromComponent(ToastComponent, { data: { message: "could not save bet", level: "error"}})
          return of({ error: err.message})
       })
    ).subscribe( result => {
         this.snackBar.openFromComponent(ToastComponent, { data: { message: "saved bet", level: "ok"}})
    })
  }

  saveStyleValue() {
      if(! this.betService.canBet(this.serverStart, this.bet)){
            return { 'fill' : 'rgba(0, 0, 0, 0)' };
      }
    //??  if(! this.enablePoints){
   //?          return  { 'fill' : 'white' };
  //??    }
      if(this.bet.viewable){
          if(this.betService.canBet(this.serverStart, this.bet)){
              if(this.checkSubmission() != "") {
                     return { 'fill' : 'green' };
              } else {
                  return { 'fill': 'yellow' };
              }
          } else if(! this.bet.result.isSet){
              return { 'fill': 'red' };
          }
      }
  }


}
