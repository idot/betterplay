import { Component, OnInit } from '@angular/core';

import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { Observable, EMPTY } from 'rxjs';
import { switchMap } from 'rxjs/operators';

import { NGXLogger } from 'ngx-logger';
import { UserService } from '../../service/user.service';
import { BetterdbService } from '../../betterdb.service';
import { Player, PlayerWithTeam } from '../../model/bet';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { ViewChild } from '@angular/core';
import { User } from '../../model/user';
import { SpecialBet } from '../../model/specialbet';
import { BetService } from '../../service/bet.service';
import { ToastComponent } from '../../components/toast/toast.component';



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
  private username = ""

  constructor(private logger: NGXLogger, private userService: UserService, private betterdb: BetterdbService, private betService: BetService, private route: ActivatedRoute, private router: Router, private snackBar: MatSnackBar) { }

  displayedColumns = ['teamflag','country', 'name', 'select']

  @ViewChild(MatSort, {static: false}) sort: MatSort



  select(player: Player, specialBet: SpecialBet){
    specialBet.bet.prediction = player.name
    const user = this.userService.getUser()
    if(user){
       if(this.result){
          this.betterdb.saveSpecialBetResult(user, specialBet).subscribe()
       } else {
          this.betterdb.saveSpecialBetPrediction(user, specialBet).subscribe( data => {
              if(data['error']){
                this.snackBar.openFromComponent(ToastComponent, { data: { message: data['error'], level: "error"}})
              } else {
                const u = <User>data
                this.snackBar.openFromComponent(ToastComponent, { data: { message: "set special bet", level: "ok"}})
                user.hadInstructions = u.hadInstructions
                this.logger.debug(`instructions ${user.hadInstructions}`)
                this.router.navigate([`user/${user.username}/special`])
              }
            } 
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
           if(username){
             this.username = ""
           }
           const id = params.get('id')
           const setresult = params.get('result')
           if(setresult){
              this.result = true
           }
           if(id){
              return this.betterdb.getSpecialBetForUser(this.userService.getUser(),parseInt(id))
           } else {
             throw Error(`could not parse id`)
           }
       })
    )
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
