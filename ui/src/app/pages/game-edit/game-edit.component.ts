import { Component, OnInit } from '@angular/core';
import { BetterdbService } from '../../betterdb.service';
import { BetService } from '../../service/bet.service';
import { UserService } from '../../service/user.service';
import { ViewResult } from '../../components/bet-view/bet-view.component';
import { Result, GameWithTeams } from '../../model/bet';
import { Input } from '@angular/core';
import range from 'lodash/range';
import isNumber from 'lodash/isNumber'
import { MatSnackBar } from '@angular/material';
import { ToastComponent } from '../../components/toast/toast.component';
import { GameWithBetsUsers } from '../../model/user';
import { Observable, EMPTY } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { switchMap } from 'rxjs/operators';
import { ParamMap } from '@angular/router';
import { NGXLogger } from 'ngx-logger';

@Component({
  selector: 'game-edit',
  templateUrl: './game-edit.component.html',
  styleUrls: ['./game-edit.component.css']
})
export class GameEditComponent implements OnInit {
  game$: Observable<GameWithBetsUsers> //loading too much, but its there already

  constructor(private logger: NGXLogger,  private route: ActivatedRoute, private betterdb: BetterdbService, private betService: BetService, private userService: UserService, public snackBar: MatSnackBar) { }

  ngOnInit() {
     this.game$ = this.route.paramMap.pipe(
        switchMap((params: ParamMap) => {
           const gamenr = params.get('gamenr')
           this.logger.debug(`extracted ${gamenr}`)
           if(gamenr){
             return this.betterdb.getGameWithBetsForUsers(parseInt(gamenr))
           } else {
             //TODO: maybe redirect
             return EMPTY
           }
       }))
  }




}
