import { Component, OnInit } from '@angular/core';
import { NGXLogger } from 'ngx-logger';
import { ActivatedRoute } from '@angular/router';
import { BetterdbService } from '../../betterdb.service';
import { UserService } from '../../user/user.service';
import { switchMap } from 'rxjs/operators';
import { ParamMap } from '@angular/router';
import { Observable, EMPTY } from 'rxjs';
import { UserWithBets } from '../../model/bet';
import { BetterTimerService } from '../../better-timer.service';


@Component({
  selector: 'user-specialbets',
  templateUrl: './user-specialbets.component.html',
  styleUrls: ['./user-specialbets.component.css']
})
export class UserSpecialbetsComponent implements OnInit {
  user$: Observable<UserWithBets>;

  constructor(private logger: NGXLogger, private route: ActivatedRoute, private timeService: BetterTimerService, private userService: UserService, private betterdb: BetterdbService) { }

  

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
