import { Component, OnInit } from '@angular/core';

import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { Observable, EMPTY } from 'rxjs';
import { switchMap } from 'rxjs/operators';

import { NGXLogger } from 'ngx-logger';
import { UserService } from '../../service/user.service';
import { BetterdbService } from '../../betterdb.service';
import { Player, PlayerWithTeam, Team } from '../../model/bet';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { ViewChild } from '@angular/core';
import { User } from '../../model/user';
import { SpecialBet } from '../../model/specialbet';
import { BetService } from '../../service/bet.service';
import { ToastComponent } from '../../components/toast/toast.component';



@Component({
  selector: 'specialbet-team',
  templateUrl: './specialbet-team.component.html',
  styleUrls: ['./specialbet-team.component.css']
})
export class SpecialbetTeamComponent implements OnInit {
  specialBet$: Observable<SpecialBet>
  teams: MatTableDataSource<Team>
  username = ""

  private betTemplateId: number = 0
  private result: boolean = false

  constructor(private logger: NGXLogger,  private snackBar: MatSnackBar, private userService: UserService, private betterdb: BetterdbService, private betService: BetService,  private route: ActivatedRoute, private router: Router) { }

  displayedColumns = ['teamflag','country', 'select']

  @ViewChild(MatSort) sort: MatSort



  select(team: Team, specialBet: SpecialBet){
    specialBet.bet.prediction = team.name
    const user = this.userService.getUser()
    if(user){
       if(this.result){
          this.betterdb.saveSpecialBetResult(user, specialBet).subscribe()
       } else {
          this.betterdb.saveSpecialBetPrediction(user, specialBet).subscribe(data => {
              if(data['error']){
                  this.snackBar.openFromComponent(ToastComponent, { data: { message: data['error'], level: "error"}})
              } else {
                const u = <User>data
                this.snackBar.openFromComponent(ToastComponent, { data: { message: "set special bet", level: "ok"}})
                user.hadInstructions = u.hadInstructions
                this.logger.debug(`instructions ${user.hadInstructions}`)
                this.router.navigate([`user/${user.username}/special`])
              }}
          )
       }
    } else {
      this.snackBar.openFromComponent(ToastComponent, { data: { message: "not logged in, please log in again", level: "error"}})
      if(this.username != ""){
          this.router.navigate([`user/${this.username}/special`])
      } else {
          this.router.navigate([`users`])
      }
    }
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
           if(username){
             this.username = username
           }
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
