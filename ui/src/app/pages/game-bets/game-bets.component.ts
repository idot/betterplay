import { Component, OnInit } from '@angular/core';
import { Observable, EMPTY } from 'rxjs';
import { GameWithBetsUsers } from '../../model/user';
import { BetterdbService } from '../../betterdb.service';
import { MatSnackBar } from '@angular/material';
import { ActivatedRoute } from '@angular/router';
import { switchMap } from 'rxjs/operators';
import { ParamMap } from '@angular/router';
import { NGXLogger } from 'ngx-logger';

@Component({
  selector: 'game-bets',
  templateUrl: './game-bets.component.html',
  styleUrls: ['./game-bets.component.css']
})
export class GameBetsComponent implements OnInit {
  game$: Observable<GameWithBetsUsers>

  constructor(private logger: NGXLogger, private betterdb: BetterdbService,  private route: ActivatedRoute, private snackBar: MatSnackBar) { }

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
       })
    )



  }

}
