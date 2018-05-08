import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute, ParamMap } from '@angular/router';
import { switchMap } from 'rxjs/operators';
import { Observable, EMPTY } from 'rxjs';

import { NGXLogger } from 'ngx-logger';

import { BetterdbService } from '../betterdb.service';
import { UserWithBets } from '../model/bet';



@Component({
  selector: 'app-user-bets',
  templateUrl: './user-bets.component.html',
  styleUrls: ['./user-bets.component.css']
})
export class UserBetsComponent implements OnInit {
  user$: Observable<UserWithBets>;
  JSON = JSON;

  constructor(private logger: NGXLogger, private route: ActivatedRoute, private betterdb: BetterdbService) { }

  ngOnInit() {
    this.user$ = this.route.paramMap.pipe(
        switchMap((params: ParamMap) => {
           const username = params.get('username');
           this.logger.error(`extracted ${username}`)
           if(username){
             return this.betterdb.getBetsForUser(username)
           } else {
             return EMPTY
           }
       })
    )
  }

}
