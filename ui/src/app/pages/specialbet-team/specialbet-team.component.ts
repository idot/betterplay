import { Component, OnInit } from '@angular/core';

import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { Observable, EMPTY } from 'rxjs';
import { switchMap } from 'rxjs/operators';

import { NGXLogger } from 'ngx-logger';
import { UserService } from '../../service/user.service';
import { BetterdbService } from '../../betterdb.service';
import { Player, PlayerWithTeam, Team } from '../../model/bet';
import { MatTableDataSource, MatSort } from '@angular/material';
import { ViewChild } from '@angular/core';
import { User } from '../../model/user';
import { SpecialBet } from '../../model/specialbet';
import { BetService } from '../../service/bet.service';



@Component({
  selector: 'specialbet-team',
  templateUrl: './specialbet-team.component.html',
  styleUrls: ['./specialbet-team.component.css']
})
export class SpecialbetTeamComponent implements OnInit {
  specialBet$: Observable<SpecialBet>
  teams: MatTableDataSource<Team>

  private betTemplateId: number = 0
  private result: boolean = false

  constructor(private logger: NGXLogger, private userService: UserService, private betterdb: BetterdbService, private betService: BetService,  private route: ActivatedRoute, private router: Router) { }

  displayedColumns = ['teamflag','country', 'select']

  @ViewChild(MatSort) sort: MatSort

  select(team: Team, specialBet: SpecialBet){
    specialBet.bet.prediction = team.name
    const user = this.userService.getUser()
    if(user){
       if(this.result){
          this.betterdb.saveSpecialBetResult(user, specialBet)
       } else {
          this.betterdb.saveSpecialBetPrediction(user, specialBet)
       }
    }
    const un = user && user.username || ""
    this.router.navigate([`user/${un}/special`])
  }

  applyFilter(filterValue: string) {
     filterValue = filterValue.trim().toLowerCase() // MatTableDataSource defaults to lowercase matches
     this.teams.filter = filterValue
  }

  ngOnInit() {
      this.getSpecialBet()
      this.getTeams()
  }

  getSpecialBet(){
     this.specialBet$ = this.route.paramMap.pipe(
        switchMap((params: ParamMap) => {
           const username = params.get('username')
           const id = params.get('id')
           const setresult = params.get('result')
           if(setresult){
              this.result = true
           }
           if(id){
              this.logger.log("returned special bets")
              return this.betterdb.getSpecialBetForUser(this.userService.getUser(),parseInt(id))
           } else {
             throw Error(`could not parse id`)
           }
       })
    )
    this.logger.log("set special bets")
  }

  getTeams(){
    this.betterdb.getTeams().subscribe(teams => {
      this.teams = new MatTableDataSource(teams)
      this.teams.sort = this.sort
    })
  }


}
