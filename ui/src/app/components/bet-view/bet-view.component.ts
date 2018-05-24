import { Component, OnInit, Input } from '@angular/core';
import {MatSnackBar} from '@angular/material';

import range from 'lodash/range';
import isNumber from 'lodash/isNumber'
import { BetterdbService } from '../../betterdb.service';
import { Bet, Result } from '../../model/bet';
import { BetService } from '../../service/bet.service';
import { ToastComponent } from '../toast/toast.component';


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


  change1 = false
  change2 = false

  disabled(): boolean {
      return ! this.enablePoints && this.betService.canBet(this.serverStart, this.bet)
  }

  changed1(){
    this.change1 = true
  }

  changed2(){
    this.change2 = true
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
    this.snackBar.openFromComponent(ToastComponent, { data: { message: "blabla", level: "error"}})
  //  this.snackBar.open("something", undefined, {
//        panelClass: ['toast-error']
//    })
  //  this.snackBar.openFromComponent(error, "", {
  //    duration: 2000,
  //    panelClass: ['blue-snackbar']
//    })

  }



}
