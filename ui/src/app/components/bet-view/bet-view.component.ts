import { Component, OnInit, Input } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

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

  submitting = false

  points = range(15)

  result: ViewResult = { 'team1' :  "hidden", 'team2' : "hidden", 'isSet' : false }


  disabled(): boolean {
       return ! this.submitting && ! this.betService.canBet(this.serverStart, this.bet)
  }

  constructor(private betterdb: BetterdbService, private betService: BetService, public snackBar: MatSnackBar) { }

  result2view(result: Result): ViewResult {
    return { team1: result.goalsTeam1, team2: result.goalsTeam2, isSet: result.isSet }
  }

  ngOnInit() {
     if(this.bet.result){
        if(this.bet.result.isSet){
            this.result = this.result2view(this.bet.result)
        } else if(this.betService.canBet(this.serverStart, this.bet)){
                this.result = { team1: '-', team2: '-', isSet: false }
            }
        }
  }


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

  saveBet(){
    console.log("saving")
    var error = this.checkSubmission()
    if(error != ""){
       this.snackBar.openFromComponent(ToastComponent, { data: { message: error, level: "error"}})
       return
    }
    this.submitting = true
    const result: Result = { goalsTeam1: parseInt(this.result.team1.toString()), goalsTeam2: parseInt(this.result.team2.toString()), isSet: true }
    const bet = clone(this.bet)
    bet.result = result
    this.betterdb.saveBet(bet).pipe(
    //   tap(_ => this.logger.debug(`saving bet ${bet.id} ${bet.result}`)),
       catchError( (err, bet) => {
          return of({ error: err.message})
       })
    ).subscribe( response => {
         if(response['error']){
           this.snackBar.openFromComponent(ToastComponent, { data: { message: `could not save bet\n ${response['error']}`, level: "error"}})
         }else{
            this.result.isSet = true
            const betnew = response['betnew']
            //const betold = result['betold']
            if(betnew){
              this.result = this.result2view(betnew.result)
            }
            this.bet.result = result
            this.snackBar.openFromComponent(ToastComponent, { data: { message: "saved bet", level: "ok"}})
         }
         this.submitting = false
    })
  }

  saveStyleValue(): string {
     if(this.submitting){
       return "disabled"
     }
     if(! this.betService.canBet(this.serverStart, this.bet)){
       return "disabled"
     } else {
         if(this.bet.viewable){
              if(this.result.isSet){
                 return "changable"
              } else {
                const errors = this.checkSubmissionErrors().length
                switch(errors){
                  case 0: return "savable"
                  case 1: return "imperfect"
                  case 2: return "necessary"
                  default: return "necessary"
                }
              }
         } else {
            return "disabled"
         }
      }
    }
}
