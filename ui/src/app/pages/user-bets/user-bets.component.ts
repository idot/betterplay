import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute, ParamMap } from '@angular/router';
import { switchMap } from 'rxjs/operators';
import { Observable, EMPTY } from 'rxjs';

import { NGXLogger } from 'ngx-logger';

import { BetterdbService } from '../../betterdb.service';
import { UserWithBets, GameBet } from '../../model/bet';
import { UserService } from '../../service/user.service';
import { FilterService } from '../../service/filter.service';
import filter from 'lodash/filter'


@Component({
  selector: 'user-bets',
  templateUrl: './user-bets.component.html',
  styleUrls: ['./user-bets.component.css']
})
export class UserBetsComponent implements OnInit {
  user$: Observable<UserWithBets>;
  JSON = JSON;

  constructor(private logger: NGXLogger, private route: ActivatedRoute, private betterdb: BetterdbService, private filterService: FilterService) { }

  filter(gameBets: GameBet[]){
     return filter(gameBets, function(g){ return this.filterService.filterGBL(g) })
  }

  ngOnInit() {
    this.user$ = this.route.paramMap.pipe(
        switchMap((params: ParamMap) => {
           const username = params.get('username');
           this.logger.debug(`extracted ${username}`)
           if(username){
             return this.betterdb.getBetsForUser(username)
           } else {
             return EMPTY
           }
       })
    )
  }

}
