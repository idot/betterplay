import { Component, OnInit } from '@angular/core';

import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { Observable, EMPTY } from 'rxjs';
import { switchMap } from 'rxjs/operators';

import { NGXLogger } from 'ngx-logger';
import { UserService } from '../../service/user.service';
import { BetterdbService } from '../../betterdb.service';
import { Player, PlayerWithTeam } from '../../model/bet';
import { MatTableDataSource, MatSort } from '@angular/material';
import { ViewChild } from '@angular/core';
import { User } from '../../model/user';
import { SpecialBet } from '../../model/specialbet';



@Component({
  selector: 'specialbet-player',
  templateUrl: './specialbet-player.component.html',
  styleUrls: ['./specialbet-player.component.css']
})
export class SpecialbetPlayerComponent implements OnInit {
  specialBet$: Observable<SpecialBet>
  pwts: MatTableDataSource<PlayerWithTeam>

  private betTemplateId: number = 0
  private result: boolean = false

  constructor(private logger: NGXLogger, private userService: UserService, private betterdb: BetterdbService, private route: ActivatedRoute, private router: Router) { }

  displayedColumns = ['teamflag','country', 'name', 'select']

  @ViewChild(MatSort) sort: MatSort

  select(player: Player, specialBet: SpecialBet){
    if(this.result){
        const user = this.userService.getUser()
        if(user){
            specialBet.bet.prediction = player.name
            this.betterdb.saveSpecialBetResult(user, specialBet)
        }
    } else {
       //this.betterdb.setSpecialBetPrediction(this.userService.getUser, pwt.player.name, this.betTemplateId)
    }
  }

  sorter(pwt: PlayerWithTeam, header: string): string | number {
    switch (header) {
      case 'name': {
         const nms = pwt.player.name.split(" ")
         if(nms.length >= 2){
           return nms[1]
         }
         return nms[0]
      }
      case 'country': return pwt.team.name;
      default: return pwt[header]
    }
  }

  filter(pwt: PlayerWithTeam, filter: string): boolean {
     return pwt.player.name.toLowerCase().indexOf(filter) >= 0 ||  pwt.team.name.toLowerCase().indexOf(filter) >= 0
  }


  applyFilter(filterValue: string) {
     filterValue = filterValue.trim().toLowerCase() // MatTableDataSource defaults to lowercase matches
     this.pwts.filter = filterValue
  }

  ngOnInit() {
      this.getSpecialBet()
      this.getPlayers()
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

  getPlayers(){
    this.betterdb.getPlayers().subscribe(pwts => {
      this.pwts = new MatTableDataSource(pwts)
      this.pwts.sort = this.sort
      this.pwts.sortingDataAccessor = this.sorter
      this.pwts.filterPredicate = this.filter
    })
  }


}
