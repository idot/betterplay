import { Component, OnInit } from '@angular/core';
import { BetterdbService } from '../../betterdb.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';
import { GameWithTeams } from '../../model/bet';
import { FilterService } from '../../service/filter.service';
import filter from 'lodash/filter'
import { BetService } from '../../service/bet.service';



@Component({
  selector: 'games',
  templateUrl: './games.component.html',
  styleUrls: ['./games.component.css']
})
export class GamesComponent implements OnInit {
  games$: Observable<GameWithTeams[]>

  constructor(private betterdb: BetterdbService, private snackBar: MatSnackBar, private filterService: FilterService, private betService: BetService) { }

  filterG(games: GameWithTeams[]){
     const fs = this.filterService
     const bs = this.betService
     return filter(games, function(g){ return fs.filterGL(g, bs) })
  }

  ngOnInit() {
    this.games$ = this.betterdb.getGames()
  }

}
